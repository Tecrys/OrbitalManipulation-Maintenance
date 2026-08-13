package tecrys.data.fighters;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import tecrys.data.utils.utils;

public class omm_DroneSwarmHullmod extends BaseHullMod {

    public static Logger log = Global.getLogger(omm_DroneSwarmHullmod.class);

    static {
        log.setLevel(Level.DEBUG);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.getWing() == null) return;
        ShipAPI source = ship.getWing().getSourceShip();
        if (source == null) return;

        String key = "omm_swarm_AI_assigned_" + source.getId();
        boolean aiAssigned = ship.getCustomData().containsKey(key);

        if (!aiAssigned) {
            omm_DroneSwarmManager manager = utils.getFirstListenerOfClass(source, omm_DroneSwarmManager.class);
            if (manager == null) {
                manager = new omm_DroneSwarmManager(source);
                source.addListener(manager);
            }

            // Excludes the PD drone from receiving the PID controller so it retains vanilla AI
            if (!ship.getWing().getSpec().getId().equals("omm_pddrone_wing")) {
                Global.getCombatEngine().addPlugin(new omm_DroneSwarmAI(ship, source));
                ship.setShipAI(null);
            }

            ship.setCustomData(key, true);
        }
    }
}