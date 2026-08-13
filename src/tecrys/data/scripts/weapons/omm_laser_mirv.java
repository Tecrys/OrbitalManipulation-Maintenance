package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.List;

public class omm_laser_mirv implements ProximityExplosionEffect, OnFireEffectPlugin {

    @Override
    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        spawnPayload(originalProjectile);
    }

    @Override
    public void onFire(final DamagingProjectileAPI projectile, WeaponAPI weapon, final CombatEngineAPI engine) {
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (engine.isPaused()) return;

                if (projectile.getCustomData().containsKey("omm_payload_spawned")) {
                    engine.removePlugin(this);
                    return;
                }

                if (projectile.isFading()) {
                    spawnPayload(projectile);
                    engine.removeEntity(projectile);
                    engine.removePlugin(this);
                    return;
                }

                if (!engine.isEntityInPlay(projectile)) {
                    engine.removePlugin(this);
                }
            }
        });
    }

    private void spawnPayload(DamagingProjectileAPI projectile) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (projectile.getCustomData().containsKey("omm_payload_spawned")) return;
        projectile.setCustomData("omm_payload_spawned", true);

        String variantId = "omm_laser_mirv_proxy";
        ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
        if (variant == null) {
            variant = Global.getSettings().createEmptyVariant(variantId + "_empty", Global.getSettings().getHullSpec(variantId));
        }

        // Bypassing Fleet Manager entirely to prevent UI ghosting
        final ShipAPI leader = engine.createFXDrone(variant);

        if (leader != null) {
            leader.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            leader.setOwner(projectile.getOwner());
            leader.getLocation().set(projectile.getLocation());
            leader.setFacing(projectile.getFacing());
            leader.setHullSize(ShipAPI.HullSize.FIGHTER);
            leader.addTag("fx_drone");

            if (projectile.getSource() != null) {
                leader.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP, 100000f, projectile.getSource());
            }

            engine.addEntity(leader);
        }

        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            private float timer = 0f;
            private final float max = 7f + (float) Math.random();
            private final float resistanceTime = 1.5f;
            private boolean cleanedUp = false;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                CombatEngineAPI engine = Global.getCombatEngine();
                if (engine.isPaused() || leader == null || cleanedUp) return;

                float timeMult = engine.getTimeMult().getModifiedValue();
                timer += amount * timeMult;

                // FX Drones lack standard AI. We must manually force it to activate its drone system.
                if (leader.getSystem() != null && leader.getSystem().getState() == ShipSystemAPI.SystemState.IDLE) {
                    leader.useSystem();
                }

                if (timer <= resistanceTime) {
                    float mult = (timer * (0.5f / resistanceTime)) + 0.5f;
                    MutableShipStatsAPI stats = leader.getMutableStats();
                    stats.getHullDamageTakenMult().modifyMult("omm_laser_mirv", mult);
                    stats.getArmorDamageTakenMult().modifyMult("omm_laser_mirv", mult);
                    stats.getShieldDamageTakenMult().modifyMult("omm_laser_mirv", mult);
                } else {
                    MutableShipStatsAPI stats = leader.getMutableStats();
                    stats.getHullDamageTakenMult().unmodify("omm_laser_mirv");
                    stats.getArmorDamageTakenMult().unmodify("omm_laser_mirv");
                    stats.getShieldDamageTakenMult().unmodify("omm_laser_mirv");
                }

                if (timer >= max || !leader.isAlive()) {
                    cleanedUp = true;

                    // Clean up child laser drones silently
                    if (leader.getDeployedDrones() != null) {
                        for (ShipAPI drone : new ArrayList<>(leader.getDeployedDrones())) {
                            if (drone != null) {
                                engine.removeEntity(drone);
                            }
                        }
                    }

                    // Clean FX drone removal without UI ghosting
                    engine.removeEntity(leader);
                    engine.removePlugin(this);
                }
            }
        });
    }
}