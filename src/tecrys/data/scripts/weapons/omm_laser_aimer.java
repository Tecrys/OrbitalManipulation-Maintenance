package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import tecrys.data.scripts.weapons.ai.omm_laser_mirv_autofire;

import java.util.List;

public class omm_laser_aimer implements EveryFrameWeaponEffectPlugin {

    WeaponAPI main, sub;
    private boolean weaponsInitialized = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        ShipAPI ship = weapon.getShip();
        if (!ship.isAlive()) return;

        if (!weaponsInitialized) {
            for (WeaponAPI ww : ship.getAllWeapons()) {
                String slotId = ww.getSlot().getId();
                if ("laser_web_slot".equals(slotId)) {
                    main = ww;
                } else if ("laser_web_slot2".equals(slotId)) {
                    sub = ww;
                }
            }
            weaponsInitialized = true;
        }

        if (main == null || sub == null) return;

        ShipAPI host = ship.getDroneSource();
        if (host == null || !host.isAlive()) return;

        List<ShipAPI> drones = host.getDeployedDrones();
        if (drones == null || drones.isEmpty()) return;

        int curIndex = drones.indexOf(ship);
        if (curIndex < 0) {
            main.setForceNoFireOneFrame(true);
            sub.setForceNoFireOneFrame(true);
            return;
        }

        int max = drones.size() - 1;
        int offset = 11;

        int nextIndex = curIndex + offset;
        if (nextIndex > max) {
            nextIndex -= (max + 1);
        }
        int prevIndex = curIndex + 4;
        if (prevIndex > max) {
            prevIndex -= (max + 1);
        }

        try {
            ShipAPI next = drones.get(nextIndex);
            ShipAPI prev = drones.get(prevIndex);

            if (next != null && next.isAlive() && prev != null && prev.isAlive()) {
                Vector2f p1 = prev.getLocation();
                Vector2f p2 = next.getLocation();

                WeaponGroupAPI mainGroup = ship.getWeaponGroupFor(main);
                if (mainGroup != null) {
                    AutofireAIPlugin plugin = mainGroup.getAutofirePlugin(main);
                    if (plugin instanceof omm_laser_mirv_autofire) {
                        ((omm_laser_mirv_autofire) plugin).setTarget(p2);
                    }
                }

                WeaponGroupAPI subGroup = ship.getWeaponGroupFor(sub);
                if (subGroup != null) {
                    AutofireAIPlugin plugin = subGroup.getAutofirePlugin(sub);
                    if (plugin instanceof omm_laser_mirv_autofire) {
                        ((omm_laser_mirv_autofire) plugin).setTarget(p1);
                    }
                }
            } else {
                main.setForceNoFireOneFrame(true);
                sub.setForceNoFireOneFrame(true);
            }
        } catch (Exception ex) {
            main.setForceNoFireOneFrame(true);
            sub.setForceNoFireOneFrame(true);
        }
    }
}