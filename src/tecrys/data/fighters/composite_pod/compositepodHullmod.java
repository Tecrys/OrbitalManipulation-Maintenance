package tecrys.data.fighters.composite_pod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import tecrys.data.utils.utils;

public class compositepodHullmod extends BaseHullMod {

    public static Logger log = Global.getLogger(compositepodHullmod.class);

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

                compositepodManager manager = utils.getFirstListenerOfClass(source, compositepodManager.class);
                if (manager == null) {
                    manager = new compositepodManager(source);
                    source.addListener(manager);
                }

                manager.dronescomposite.add(ship);
            }

//            for (WeaponGroupAPI group : ship.getWeaponGroupsCopy()) {
 //               if (!group.isAutofiring()) {
 //                   ship.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, ship.getWeaponGroupsCopy().indexOf(group));
//                }
 //           }
            Global.getCombatEngine().addPlugin(new compositepodAI(ship, source));
            ship.setShipAI(null);

            ship.setCustomData(key, true);
            ship.getCustomData().put(key, true);
        }
    }
}
