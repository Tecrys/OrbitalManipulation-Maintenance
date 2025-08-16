package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

//original script from Secrets of the Frontier by Inventor Raccoon

public class omm_chaff_flak implements ProximityExplosionEffect {



    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        if (Global.getCombatEngine().isPaused()) return;
        // IR smoke visual
        for (int i = 0; i < 18; i++) {
            float dur = 2f + (float) Math.random();
            Vector2f loc = new Vector2f(explosion.getLocation());
            loc = Misc.getPointWithinRadius(loc, 100f);
            float s = 275f * (0.25f + (float) Math.random() * 0.25f);
            Global.getCombatEngine().addNebulaParticle(loc, explosion.getVelocity(), s, 1.5f, 0.1f, 0f, dur, new Color(35, 35, 35));
        }

        ArrayList<CombatEntityAPI> TargetsHit = new ArrayList<CombatEntityAPI>();

        List<ShipAPI> Targets = CombatUtils.getShipsWithinRange(explosion.getLocation(), 150f);
        List<MissileAPI> Missiles = CombatUtils.getMissilesWithinRange(explosion.getLocation(), 150f);

        for (MissileAPI Missile : Missiles) {
            if (Missile.getOwner() != originalProjectile.getOwner()) {
                Global.getCombatEngine().applyDamage(Missile, Missile.getLocation(), 20, DamageType.FRAGMENTATION, 20f, false, false, explosion);
            }
        }

        for (ShipAPI Target : Targets) {
            if (Target.getOriginalOwner() != originalProjectile.getOwner()) {
                Global.getCombatEngine().applyDamage(Target, Target.getLocation(), 0, DamageType.HIGH_EXPLOSIVE, 100f, false, false, explosion);
            }

            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                float timer = 0;
                final float max = 10f + ((float) Math.random());
                final float resistanceTime = 0.05f;
                private SpriteAPI sprite;




                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (Global.getCombatEngine().isPaused()) return;
                    if (timer <= max && Target instanceof ShipAPI && ((ShipAPI) Target).getOriginalOwner() != originalProjectile.getOwner()) {
                        ((ShipAPI) Target).getMutableStats().getEnergyWeaponDamageMult().modifyMult("omm_chaff_flak", timer * resistanceTime + 0.2f);
                        ((ShipAPI) Target).getMutableStats().getBeamWeaponDamageMult().modifyMult("omm_chaff_flak", timer * resistanceTime + 0.2f);
                        ((ShipAPI) Target).getMutableStats().getBeamWeaponRangeBonus().modifyMult("omm_chaff_flak", timer * resistanceTime + 0.2f);
                        float resttime = max - timer ;

                        for (WeaponAPI wep :Target.getAllWeapons()){
                            if (wep.isBeam() || wep.getDamageType().equals(DamageType.ENERGY)){

                                wep.setGlowAmount(5f, new Color(255, 255, 255, 5));

                            }
                        }
                        //Global.getCombatEngine().addFloatingTextAlways(Target.getLocation(), "Chaff"+ Math.round(resttime),10f, Color.YELLOW, Target, 1f, 1f, 0.01f, 0f, 0f, 1f); //Debugging
                    }
                    if (timer <= max && Target instanceof ShipAPI && ((ShipAPI) Target).getOriginalOwner() == originalProjectile.getOwner()) {
                        ((ShipAPI) Target).getMutableStats().getBeamDamageTakenMult().modifyMult("omm_chaff_flak", timer * resistanceTime + 0.2f);
                        ((ShipAPI) Target).getMutableStats().getEnergyDamageTakenMult().modifyMult("omm_chaff_flak", timer * resistanceTime + 0.2f);
                        ((ShipAPI) Target).getMutableStats().getEmpDamageTakenMult().modifyMult("omm_chaff_flak", timer * resistanceTime + 0.2f);
                        float resttime = max - timer ;
                      //  Target.setJitter(this, new Color(255, 255, 255, 10), 1f, 25, 0f, 7f);
                      //  Global.getCombatEngine().addFloatingTextAlways(Target.getLocation(), "Chaff"+ Math.round(resttime),10f, Color.GREEN, Target, 1f, 1f, 0.01f, 0f, 0f, 1f); //Debugging
                        for (WeaponAPI wep :Target.getAllWeapons()){
                                             wep.setGlowAmount(5f, new Color(255, 255, 255, 5));
                        }
                    }
                    if (timer >= (max-0.1f)){

                        for (WeaponAPI wep :Target.getAllWeapons()){

                            wep.setGlowAmount(0f, new Color(0, 0, 0, 0));

                    }}
                    else {
                        if (Target instanceof ShipAPI) {
                            ((ShipAPI) Target).getMutableStats().getEnergyWeaponDamageMult().unmodify("omm_chaff_flak");
                            ((ShipAPI) Target).getMutableStats().getBeamWeaponDamageMult().unmodify("omm_chaff_flak");
                            ((ShipAPI) Target).getMutableStats().getBeamWeaponRangeBonus().unmodify("omm_chaff_flak");
                            ((ShipAPI) Target).getMutableStats().getBeamDamageTakenMult().unmodify("omm_chaff_flak");
                            ((ShipAPI) Target).getMutableStats().getEnergyDamageTakenMult().unmodify("omm_chaff_flak");
                            ((ShipAPI) Target).getMutableStats().getEmpDamageTakenMult().unmodify("omm_chaff_flak");

                        }
                    }
                    if (Global.getCombatEngine().isPaused()) return;
                    timer += amount * Global.getCombatEngine().getTimeMult().getModifiedValue();
                    if (timer >= max) {
                        Global.getCombatEngine().removePlugin(this);
                    }
                }
            });
    }


    }
}



