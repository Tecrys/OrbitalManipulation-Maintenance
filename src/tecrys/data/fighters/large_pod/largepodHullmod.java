package tecrys.data.fighters.large_pod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import tecrys.data.utils.utils;

public class largepodHullmod extends BaseHullMod {

    public static Logger log = Global.getLogger(largepodHullmod.class);

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

                largepodManager manager = utils.getFirstListenerOfClass(source, largepodManager.class);
                if (manager == null) {
                    manager = new largepodManager(source);
                    source.addListener(manager);
                }

                manager.drones.add(ship);
            }

//            for (WeaponGroupAPI group : ship.getWeaponGroupsCopy()) {
 //               if (!group.isAutofiring()) {
 //                   ship.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, ship.getWeaponGroupsCopy().indexOf(group));
//                }
 //           }
            Global.getCombatEngine().addPlugin(new largepodAI(ship, source));
            ship.setShipAI(null);

            ship.setCustomData(key, true);
            ship.getCustomData().put(key, true);
        }
    }
}
