package tecrys.data.fighters.synergy_pod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import tecrys.data.utils.utils;

public class synergypodHullmod extends BaseHullMod {

    public static Logger log = Global.getLogger(synergypodHullmod.class);

    static {
        log.setLevel(Level.DEBUG);
   }

    String key = "RGMD_realAIAssigned";

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        boolean aiAssigned = ship.getCustomData().containsKey(key);

        if (!aiAssigned) {
            if (ship.getWing() == null) return;
            ShipAPI source = ship.getWing().getSourceShip();
            if (source != null) {

                synergypodManager manager = utils.getFirstListenerOfClass(source, synergypodManager.class);
                if (manager == null) {
                    manager = new synergypodManager(source);
                    source.addListener(manager);
                }

                manager.drones.add(ship);
            }
//                        ShipAPI player = Global.getCombatEngine().getPlayerShip();
//            for (WeaponGroupAPI group : ship.getWeaponGroupsCopy()) {
//                if (group.isAutofiring() && ship.equals(player)) {
//                    ship.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, ship.getWeaponGroupsCopy().indexOf(group));
//                }
//                            else    if (!group.isAutofiring()) {
//                    ship.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, ship.getWeaponGroupsCopy().indexOf(group));
//                }
            
            Global.getCombatEngine().addPlugin(new synergypodAI(ship, source));
            ship.setShipAI(null);

            ship.setCustomData(key, true);
            ship.getCustomData().put(key, true);
        }
    }
}
