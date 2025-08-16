package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

//original script from Secrets of the Frontier by Inventor Raccoon

public class omm_laser_mirv implements ProximityExplosionEffect {

    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        ArrayList<ShipAPI> leaders = new ArrayList<ShipAPI>();

        String variantId = "omm_laser_mirv_proxy";

        CombatFleetManagerAPI fleetManager = Global.getCombatEngine().getFleetManager(originalProjectile.getOwner());
        boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
        fleetManager.setSuppressDeploymentMessages(true);

        ShipAPI leader = fleetManager.spawnShipOrWing(variantId,
                originalProjectile.getLocation(),
                originalProjectile.getFacing(),
                0f);
//leader.setDrone(true);
     //   leader.setOriginalOwner(originalProjectile.getWeapon().getShip().getOriginalOwner());
leader.setHullSize(ShipAPI.HullSize.FIGHTER);
        for (ShipAPI drones : leader.getDeployedDrones()) {
          // drones.setOriginalOwner(originalProjectile.getWeapon().getShip().getOriginalOwner());

        }

        fleetManager.setSuppressDeploymentMessages(wasSuppressed);


            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                float timer = 0;
                // time until drone self-destructs
                final float max = 7f + ((float) Math.random());
                // drone begins with 50% damage resistance (from IR smoke), fading over this time
                final float resistanceTime = 1.5f;
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (timer <= resistanceTime) {
                        leader.getMutableStats().getHullDamageTakenMult().modifyMult("omm_laser_mirv", (timer * (0.5f / resistanceTime)) + 0.5f);
                        leader.getMutableStats().getArmorDamageTakenMult().modifyMult("omm_laser_mirv", (timer * (0.5f / resistanceTime)) + 0.5f);
                        leader.getMutableStats().getShieldDamageTakenMult().modifyMult("omm_laser_mirv", (timer * (0.5f / resistanceTime)) + 0.5f);
                    } else {
                        leader.getMutableStats().getHullDamageTakenMult().unmodify("omm_laser_mirv");
                        leader.getMutableStats().getArmorDamageTakenMult().unmodify("omm_laser_mirv");
                        leader.getMutableStats().getShieldDamageTakenMult().unmodify("omm_laser_mirv");
                    }
                    if (Global.getCombatEngine().isPaused()) return;
                    timer += amount * Global.getCombatEngine().getTimeMult().getModifiedValue();
                    if (timer >= max) {
                        Global.getCombatEngine().removeEntity(leader);

                        Global.getCombatEngine().removePlugin(this);
                    }
                }
            });
       

        // deploy a few decoy flares
//        for (int i = 0; i < 5; i++) {
//            Vector2f flareLoc = Misc.getPointWithinRadius(explosion.getLocation(), 50);
//            Global.getCombatEngine().spawnProjectile(
//                    explosion.getSource(),
//                    null,
//                    "flarelauncher2",
//                    flareLoc,
//                    Misc.getAngleInDegrees(explosion.getLocation(), flareLoc),
//                    null
//            );
//        }
//
//        // IR smoke visual
//        for (int i = 0; i < 18; i++) {
//            float dur = 2f + (float) Math.random();
//            Vector2f loc = new Vector2f(explosion.getLocation());
//            loc = Misc.getPointWithinRadius(loc, 100f);
//            float s = 275f * (0.25f + (float) Math.random() * 0.25f);
//            Global.getCombatEngine().addNebulaParticle(loc, explosion.getVelocity(), s, 1.5f, 0.1f, 0f, dur, new Color(35, 35, 35));
//        }
    }
}



