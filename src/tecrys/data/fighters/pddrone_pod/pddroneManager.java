package tecrys.data.fighters.pddrone_pod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class pddroneManager implements AdvanceableListener {


    public final ShipAPI mothership;

    public IntervalUtil timer = new IntervalUtil(0F, 5F);
    private boolean isWeaponSwapped = false;

    public final ArrayList<ShipAPI> drones = new ArrayList<>(); //list of all drones
    public final ArrayList<FighterWingAPI> relevantWings = new ArrayList<>(); //the list of sarissa wings
    public pddroneManager(ShipAPI mothership) {
        this.mothership = mothership;

        for (FighterWingAPI wing : mothership.getAllWings()) {
            if (wing.getSpec().getId().equals("omm_pddrone_wing")) {
                relevantWings.add(wing);
            }
        }
    }
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
    public void advance(float amount)
    {
        ShipAPI ship = this.mothership;
        //CombatEngineAPI engine = Global.getCombatEngine();
        WeaponAPI wep = null;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("pdslot"))
                wep = w;
        }


        if (ship.isAlive() && wep != null) {
            //CombatEngineAPI engine = Global.getCombatEngine();
            List<FighterWingAPI> wings = relevantWings;

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
                    CombatEngineAPI engine = Global.getCombatEngine();

                    String str = (String) Global.getCombatEngine().getCustomData().get("omm_pddroneWeaponId" + this.mothership.getId());
                    if (str == null)
                        str = "No weapon";

                    //if (engine.getPlayerShip() == ship)
                        //Global.getCombatEngine().maintainStatusForPlayerShip("PDDrones", "graphics/ui/icons/icon_repair_refit.png", "Drone Weaponry", str + " installed. ", true);
                    if (!fighter.getAllWeapons().get(2).getId().equals(str)) {
                        if (ship.getVariant().getWeaponSpec("pdslot") != null) {
                            Global.getCombatEngine().getCustomData().put("omm_pddroneWeaponId" + this.mothership.getId(), this.mothership.getVariant().getWeaponId("pdslot"));
                            stats.getVariant().setOriginalVariant(null);
                            fighter.getFleetMember().setVariant(newVariant, true, true);
                            wep.disable(true);

                            this.timer.randomize();                                                        //randomize interval to stagger drone refit

                            this.timer.advance(amount);
                            if (!this.timer.intervalElapsed()) {
                                return;
                            }
                            stats.getVariant().clearSlot("pdslot");
                            stats.getVariant().addWeapon("pdslot", this.mothership.getVariant().getWeaponId("pdslot"));
                            stats.getVariant().getWeaponSpec("pdslot").addTag("FIRE_WHEN_INEFFICIENT");
                            ship.removeWeaponFromGroups(wep);


                            wing.orderReturn(fighter);
                            //isWeaponSwapped = true;

                        }
                    }
                }//

            }
        }
    }




}







