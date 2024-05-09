package tecrys.data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.AdditionalBerthing;
import com.fs.starfarer.api.impl.hullmods.ExpandedCargoHolds;
import com.fs.starfarer.api.impl.hullmods.MilitarizedSubsystems;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class omm_factory_override extends BaseHullMod {

    private static final String HULLMOD_ID = "omm_factory_override";
    private static final Map coom = new HashMap();

    static {
        coom.put(HullSize.FIGHTER, 100f);
        coom.put(HullSize.FRIGATE, 100f);
        coom.put(HullSize.DESTROYER, 100f);
        coom.put(HullSize.CRUISER, 100f);
        coom.put(HullSize.CAPITAL_SHIP, 100f);
        coom.put(HullSize.DEFAULT, 100f);
    }
    //This above is kinda important, you have to define HullSize.FIGHTER and HullSize.DEFAULT because for some reason people are spawning old precursor fighters and the mod is randomly summoning these cringe gargoyles and CTDing the game. If you don't want them to get the bonus, I would just set it to 0f or something...
    private static float NEGATIVE_PERCENT = 50;


    private static final int LOGISTICS_HULLMOD_SLOTS_BONUS = 1;



    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        //stats.getDynamic().getMod(Stats.MAX_LOGISTICS_HULLMODS_MOD).modifyFlat(id, LOGISTICS_HULLMOD_SLOTS_BONUS);
        boolean sMod = isSMod(stats);

        if (stats.getVariant().hasHullMod("militarized_subsystems")) {

            stats.getMinCrewMod().modifyPercent(id, -NEGATIVE_PERCENT);
        }
        if (stats.getVariant().hasHullMod("auxiliary_fuel_tanks")) {
            if (!sMod && stats.getVariant() != null && stats.getVariant().hasHullMod(HullMods.CIVGRADE) && !stats.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS)) {
                stats.getSuppliesPerMonth().modifyPercent("omm_factory_override1", -(NEGATIVE_PERCENT/2));
            }
        }
        if (stats.getVariant().hasHullMod("additional_berthing")) {
            if (!sMod && stats.getVariant() != null && stats.getVariant().hasHullMod(HullMods.CIVGRADE) && !stats.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS)) {
                stats.getSuppliesPerMonth().modifyPercent("omm_factory_override2", -(NEGATIVE_PERCENT/2));
            }
        }
        if (stats.getVariant().hasHullMod("expanded_cargo_holds")) {
            if (!sMod && stats.getVariant() != null && stats.getVariant().hasHullMod(HullMods.CIVGRADE) && !stats.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS)) {
                stats.getSuppliesPerMonth().modifyPercent("omm_factory_override3", -(NEGATIVE_PERCENT/2));
            }
        }

    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
//        if (!"freitag_corporation".equals(Misc.getCommissionFactionId())) {
//            ship.getVariant().removeMod("omm_factory_override");
//        }
//        if (ship.getVariant().hasHullMod("CHM_commission")) {
//            ship.getVariant().removeMod("CHM_commission");
//        }
    }

    public String getDescriptionParam(int index, HullSize hullSize) {

        if (index == 0) {
            return "Militarized Subsystems";
        }
        if (index == 1) {
            return "" + ((Float) NEGATIVE_PERCENT).intValue() + "%";
        }
        if (index == 2) {
            return "Additional Berthing";
        }
        if (index == 3) {
            return "Auxiliary Fuel Tanks";
        }
        if (index == 4) {
            return "Expanded Cargo Holds";
        }
        if (index == 5) {
            return "" + ((Float) NEGATIVE_PERCENT).intValue() + "%";
        }

        return null;

    }

    //Oh these are cool colors below introduced in 0.95a, to match with your tech type and stuff. Just nice to have!
    public Color getBorderColor() {
        return new Color(255, 255, 255, 100);
    }

    public Color getNameColor() {
        return new Color(255, 166, 0, 255);
    }
}
