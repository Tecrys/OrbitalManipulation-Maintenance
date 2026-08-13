package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

//original script from Secrets of the Frontier by Inventor Raccoon

public class omm_chaff_flak implements ProximityExplosionEffect {

    public void onExplosion(final DamagingProjectileAPI explosion, final DamagingProjectileAPI originalProjectile) {
        if (Global.getCombatEngine().isPaused()) return;

        // IR smoke visual
        for (int i = 0; i < 18; i++) {
            float dur = 2f + (float) Math.random();
            Vector2f loc = new Vector2f(explosion.getLocation());
            loc = Misc.getPointWithinRadius(loc, 100f);
            float s = 275f * (0.25f + (float) Math.random() * 0.25f);
            Global.getCombatEngine().addNebulaParticle(loc, explosion.getVelocity(), s, 1.5f, 0.1f, 0f, dur, new Color(35, 35, 35));
        }

        // --- NEW: LINGERING CLOUD AOE PLUGIN ---
        // Handles the persistent chance to fizzle missiles and overload fighters touching the cloud
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float cloudTimer = 0f;
            private final float cloudMaxDuration = 3.0f; // Represents the visual lifetime of the nebula particles
            private final Vector2f cloudLoc = new Vector2f(explosion.getLocation());

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused()) return;

                float timeMult = Global.getCombatEngine().getTimeMult().getModifiedValue();
                cloudTimer += amount * timeMult;

                if (cloudTimer >= cloudMaxDuration) {
                    Global.getCombatEngine().removePlugin(this);
                    return;
                }

                // Fizzle hostile missiles (approx 50% chance per second while in the cloud)
                float fizzleChance = 0.50f * amount * timeMult;
                for (MissileAPI missile : CombatUtils.getMissilesWithinRange(cloudLoc, 125f)) {
                    if (missile.getOwner() != originalProjectile.getOwner() && !missile.isFading() && !missile.isFizzling()) {
                        if (Math.random() < fizzleChance) {
                            missile.flameOut();
                        }
                    }
                }

                // Overload hostile fighters (approx 50% chance per second while in the cloud)
                float overloadChance = 0.50f * amount * timeMult;
                for (ShipAPI ship : CombatUtils.getShipsWithinRange(cloudLoc, 125f)) {
                    if (ship.getOwner() != originalProjectile.getOwner() && ship.isFighter() && ship.isAlive()) {

                            if (Math.random() < overloadChance) {
                                ship.getFluxTracker().forceOverload(5f); // Triggers the visual sparks and overload status

                                // Fighters ignore standard flux shutdown mechanics.
                                // We must apply a massive burst of EMP damage bypassing shields to physically disable their weapons and engines.
                                Global.getCombatEngine().applyDamage(
                                        ship,
                                        ship.getLocation(),
                                        0f,                  // Hull damage
                                        DamageType.OTHER,    // Replaced DamageType.EMP with a valid enum
                                        2000f,               // EMP damage
                                        true,                // Bypass shields
                                        false,               // Does not deal soft damage
                                        explosion            // Source
                                );
                            }

                    }
                }
            }
        });
        // ---------------------------------------


        // Instant fragmentation damage and lingering debuffs for targets caught in the initial blast
        List<ShipAPI> Targets = CombatUtils.getShipsWithinRange(explosion.getLocation(), 150f);
        List<MissileAPI> Missiles = CombatUtils.getMissilesWithinRange(explosion.getLocation(), 150f);

        for (MissileAPI Missile : Missiles) {
            if (Missile.getOwner() != originalProjectile.getOwner()) {
                Global.getCombatEngine().applyDamage(Missile, Missile.getLocation(), 20, DamageType.FRAGMENTATION, 20f, false, false, explosion);
            }
        }

        for (final ShipAPI Target : Targets) {
            if (Target.getOriginalOwner() != originalProjectile.getOwner()) {
                Global.getCombatEngine().applyDamage(Target, Target.getLocation(), 20, DamageType.FRAGMENTATION, 20f, false, false, explosion);
            }

            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                float timer = 0;
                final float max = 5f + ((float) Math.random());
                final float resistanceTime = 0.05f;

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (Global.getCombatEngine().isPaused()) return;

                    timer += amount * Global.getCombatEngine().getTimeMult().getModifiedValue();

                    // Cleanup state: unmodify stats properly when the timer expires or target dies
                    if (timer >= max || !Target.isAlive()) {
                        Target.getMutableStats().getEnergyWeaponDamageMult().unmodify("omm_chaff_flak");
                        Target.getMutableStats().getBeamWeaponDamageMult().unmodify("omm_chaff_flak");
                        Target.getMutableStats().getBeamWeaponRangeBonus().unmodify("omm_chaff_flak");
                        Target.getMutableStats().getBeamDamageTakenMult().unmodify("omm_chaff_flak");
                        Target.getMutableStats().getEnergyDamageTakenMult().unmodify("omm_chaff_flak");
                        Target.getMutableStats().getEmpDamageTakenMult().unmodify("omm_chaff_flak");

                        for (WeaponAPI wep : Target.getAllWeapons()) {
                            wep.setGlowAmount(0f, new Color(0, 0, 0, 0));
                        }

                        Global.getCombatEngine().removePlugin(this);
                        return;
                    }

                    // Active state: apply modifiers
                    float effectMult = (timer * resistanceTime) + 0.2f;

                    if (Target.getOriginalOwner() != originalProjectile.getOwner()) {
                        Target.getMutableStats().getEnergyWeaponDamageMult().modifyMult("omm_chaff_flak", effectMult);
                        Target.getMutableStats().getBeamWeaponDamageMult().modifyMult("omm_chaff_flak", effectMult);
                        Target.getMutableStats().getBeamWeaponRangeBonus().modifyMult("omm_chaff_flak", effectMult);

                        for (WeaponAPI wep : Target.getAllWeapons()) {
                            if (wep.isBeam() || wep.getDamageType().equals(DamageType.ENERGY)) {
                                wep.setGlowAmount(5f, new Color(255, 255, 255, 5));
                            }
                        }
                    } else {
                        Target.getMutableStats().getBeamDamageTakenMult().modifyMult("omm_chaff_flak", effectMult);
                        Target.getMutableStats().getEnergyDamageTakenMult().modifyMult("omm_chaff_flak", effectMult);
                        Target.getMutableStats().getEmpDamageTakenMult().modifyMult("omm_chaff_flak", effectMult);

                        for (WeaponAPI wep : Target.getAllWeapons()) {
                            wep.setGlowAmount(5f, new Color(255, 255, 255, 5));
                        }
                    }
                }
            });
        }
    }
}