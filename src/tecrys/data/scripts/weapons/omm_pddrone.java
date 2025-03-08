package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import java.util.List;

public class omm_pddrone implements EveryFrameWeaponEffectPlugin {



    private boolean isWeaponSwapped = false;

    public boolean isBattling ()
    {
        boolean check = false;
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatUIAPI ui = engine.getCombatUI();

        if (ui != null) {
            check = true;
        }

        return check;
    }

    @Override
    public void advance ( float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        ShipAPI ship = weapon.getShip();
        //CombatEngineAPI engine = Global.getCombatEngine();
        WeaponAPI wep = null;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("pdslot"))
                wep = w;
        }
        //Refit screen check
        if (ship.getOriginalOwner() == -1) {
            weapon.getSprite().setColor(new Color(255, 255, 255, 255));
            if (weapon.getBarrelSpriteAPI() != null)
                weapon.getBarrelSpriteAPI().setColor(new Color(255, 255, 255, 255));
        } else {

            if (weapon != null)
                weapon.getSprite().setColor(new Color(255, 255, 255, 0));
        }

        if (ship.isAlive() && wep != null) {
            //CombatEngineAPI engine = Global.getCombatEngine();
            List<FighterWingAPI> wings = ship.getAllWings();

            for (FighterWingAPI wing : wings) {
                if (wing == null || wing.getWingMembers() == null || wing.getWingMembers().isEmpty())
                    continue;

                ShipAPI fighter = wing.getLeader();
                if (fighter == null)
                    continue;

                for (int i = 0; i < wing.getWingMembers().size(); i++) {
                    fighter = wing.getWingMembers().get(i);
                    if (fighter == null)
                        continue;
                    if (fighter.getAllWeapons().isEmpty())
                        continue;
                    if (!wing.getWingId().equals("omm_pddrone_wing")) {
                        continue;
                    }
                    MutableShipStatsAPI stats = fighter.getMutableStats();
                    ShipVariantAPI OGVariant = stats.getVariant().clone();
                    ShipVariantAPI newVariant = stats.getVariant().clone();

                    String str = (String) Global.getCombatEngine().getCustomData().get("omm_pddroneWeaponId" + ship.getId());
                    if (str == null)
                        str = "No weapon";
                    if (engine.getPlayerShip() == ship)
                        Global.getCombatEngine().maintainStatusForPlayerShip("PDDrone", "graphics/ui/icons/icon_repair_refit.png", "PDDrone Weapon", str + " installed. ", true);
                    if (!fighter.getAllWeapons().get(0).getId().equals(str)) {
                        if (ship.getVariant().getWeaponSpec("pdslot") != null) {
                            Global.getCombatEngine().getCustomData().put("omm_pddroneWeaponId" + ship.getId(), ship.getVariant().getWeaponId("pdslot"));
                            //stats.getVariant().setOriginalVariant(null);
                            fighter.getFleetMember().setVariant(newVariant, true, true);
                            wep.disable(true);


                            stats.getVariant().clearSlot("pdslot");
                            stats.getVariant().addWeapon("pdslot", ship.getVariant().getWeaponId("pdslot"));
                            stats.getVariant().getWeaponSpec("pdslot").addTag("FIRE_WHEN_INEFFICIENT");
                            //ship.removeWeaponFromGroups(wep);


                            wing.orderReturn(fighter);
                            //isWeaponSwapped = true;

                        }
                    }
                }//

            }
        }
    }
}







