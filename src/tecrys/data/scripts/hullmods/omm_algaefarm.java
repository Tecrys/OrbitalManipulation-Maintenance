package tecrys.data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.impl.hullmods.MilitarizedSubsystems;
import org.lazywizard.lazylib.MathUtils;
import tecrys.data.utils.utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class omm_algaefarm extends BaseHullMod {

    private static final Map crew = new HashMap();

    static {
        crew.put(HullSize.DEFAULT, 2);
        crew.put(HullSize.FIGHTER, 2);
        crew.put(HullSize.FRIGATE, 2);
        crew.put(HullSize.DESTROYER, 7);
        crew.put(HullSize.CRUISER, 14);
        crew.put(HullSize.CAPITAL_SHIP, 24);
    }
    private static final Map soy = new HashMap();

    static {
        soy.put(HullSize.DEFAULT, 1);
        soy.put(HullSize.FIGHTER, 1);
        soy.put(HullSize.FRIGATE, 1);
        soy.put(HullSize.DESTROYER, 3);
        soy.put(HullSize.CRUISER, 6);
        soy.put(HullSize.CAPITAL_SHIP, 10);
    }

    private static final Map fuel = new HashMap();

    static {
        fuel.put(HullSize.DEFAULT, 1);
        fuel.put(HullSize.FIGHTER, 1);
        fuel.put(HullSize.FRIGATE, 1);
        fuel.put(HullSize.DESTROYER, 2);
        fuel.put(HullSize.CRUISER, 3);
        fuel.put(HullSize.CAPITAL_SHIP, 6);
    }
    //This above is kinda important, you have to define HullSize.FIGHTER and HullSize.DEFAULT because for some reason people are spawning old precursor fighters and the mod is randomly summoning these cringe gargoyles and CTDing the game. If you don't want them to get the bonus, I would just set it to 0f or something...

    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        //Checks if the fleet is real and belongs to the player.
        if (member.getFleetData() == null) {
            return;
        }
        if (member.getFleetData().getFleet() == null) {
            return;
        }
        if (member.getFleetData().getFleet().getStarSystem() == null) {
            return;
        }
        if (member.getFleetData().getFleet().getStarSystem().getStar() == null) {
            return;
        }

        if (member.getFleetData().getFleet().isInHyperspace()) {
            return;
        }
        if (member.getFleetData().getFleet().getStarSystem() != null && !member.isMothballed() && member.getFleetData().getFleet().getStarSystem().getStar().isNormalStar()) {

            if (member.getFleetData() != null && member.getFleetData().getFleet() != null && member.getFleetData().getFleet().equals(Global.getSector().getPlayerFleet())) {
                if (!Global.getSector().getPersistentData().containsKey(member.getId() + "algaetimecheck")) {
                    Global.getSector().getPersistentData().put(member.getId() + "algaetimecheck", Global.getSector().getClock().getTimestamp());
                }
                float timeelapsed = Global.getSector().getClock().getElapsedDaysSince((long) Global.getSector().getPersistentData().get(member.getId() + "algaetimecheck"));
                if (timeelapsed >= 1f && timeelapsed <= 2f) {
                    if (member.getFleetData().getFleet().getCargo().getSupplies() >= 1 && member.getFleetData().getFleet().getCargo().getFuel() >= 1) {
                        utils.addPlayerCommodity("fuel", (int) fuel.get(member.getVariant().getHullSize()));
                        utils.addPlayerCommodity("supplies", (int) soy.get(member.getVariant().getHullSize()));
                        Global.getSector().getPersistentData().put(member.getId() + "algaetimecheck", Global.getSector().getClock().getTimestamp());
                    }
                    if (member.getFleetData().getFleet().getCargo().getSupplies() < 1 || member.getFleetData().getFleet().getCargo().getFuel() < 1) {
                        Global.getSector().getCampaignUI().addMessage("Some crew members went missing");
                        utils.addPlayerCommodity("crew", (int) crew.get(member.getVariant().getHullSize()));
                        utils.addPlayerCommodity("fuel", (int) fuel.get(member.getVariant().getHullSize()));
                        utils.addPlayerCommodity("supplies", (int) soy.get(member.getVariant().getHullSize()));
                        Global.getSector().getPersistentData().put(member.getId() + "algaetimecheck", Global.getSector().getClock().getTimestamp());
                    }
                } else if (timeelapsed > 2f) {
                    Global.getSector().getPersistentData().put(member.getId() + "algaetimecheck", Global.getSector().getClock().getTimestamp());
                }
            }
        }
    }

    public String getDescriptionParam(int index, HullSize hullSize) {

        return null;

    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        java.util.List<WeaponAPI> decos = ship.getAllWeapons();
        for (WeaponAPI deco : decos) {
            if (deco.getSlot().getId().equals("algaepods")) {
                if (ship.getOriginalOwner() == -1) {
                    deco.getAnimation().setFrame(00);
                } else {
                    deco.getAnimation().setFrame(01);
                }
            }
        }
    }

    //Oh these are cool colors below introduced in 0.95a, to match with your tech type and stuff. Just nice to have!
    public Color getBorderColor() {
        return new Color(255, 255, 255, 100);
    }

    public Color getNameColor() {
        return new Color(255, 166, 0, 255);
    }
}
