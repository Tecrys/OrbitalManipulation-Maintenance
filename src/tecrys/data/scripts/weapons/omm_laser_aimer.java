package tecrys.data.scripts.weapons;


import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import tecrys.data.scripts.weapons.ai.omm_laser_mirv_autofire;

import java.util.List;

public class omm_laser_aimer implements EveryFrameWeaponEffectPlugin {

    WeaponAPI main, sub;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        ShipAPI ship = weapon.getShip();
        if (!ship.isAlive()) return;
        if (main == null) {
            for (WeaponAPI ww : ship.getAllWeapons()) {
                if (ww.getSlot().getId().equals("laser_web_slot")) {
                    main = ww;
                }
                if (ww.getSlot().getId().equals("laser_web_slot2")) {
                    sub = ww;
                }
            }
        }

        ShipAPI host = ship.getDroneSource();
        if (host == null) return;
        if (!host.isAlive()) return;
//        if (!engine.isEntityInPlay(host)) return;
        List<ShipAPI> drones = host.getDeployedDrones();
        int max = drones.size() - 1;
        int offset = max / 3;
        offset = 11;

        int curIndex = 0;

        try {
            curIndex = drones.indexOf(ship);

            //engine.addFloatingText(ship.getLocation(), curIndex+"", 20, Color.WHITE, ship, 0,0);
            // baseSingleframeRender(new tooltipRender(curIndex+"", Color.RED, m.aa(Color.ORANGE, 0.5f)),ship.getLocation(), 0, new Vector2f(100, 30));
            int nextIndex = curIndex + offset;
            if (nextIndex > max) {
                nextIndex -= (max + 1);
            }
            int prevIndex = curIndex + 4;
            if (prevIndex > max) {
                prevIndex -= (max + 1);
            }
            ShipAPI next = drones.get(nextIndex);
            ShipAPI prev = drones.get(prevIndex);
            Vector2f p1 = prev.getLocation();
            Vector2f p2 = next.getLocation();
            //main.setForceFireOneFrame(true);
            //sub.setForceFireOneFrame(true);
            ((omm_laser_mirv_autofire) ship.getWeaponGroupFor(main).getAutofirePlugin(main)).setTarget(p2);
            ((omm_laser_mirv_autofire) ship.getWeaponGroupFor(sub).getAutofirePlugin(sub)).setTarget(p1);
        } catch (Exception ex) {
            main.setForceNoFireOneFrame(true);
            sub.setForceNoFireOneFrame(true);
        }
    }
}
