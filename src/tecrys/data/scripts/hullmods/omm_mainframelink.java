package tecrys.data.scripts.hullmods;


import com.fs.starfarer.api.combat.*;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;


public class omm_mainframelink extends BaseHullMod {


    private float slotX;
    private float slotY;

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod("act_as_combat_ship").modifyFlat(id, 1.0F);
    }

    public void advanceInCombat(ShipAPI ship, Float amount) {


        if (ship.getShipAI() != null && ship.getShipAI().getConfig() != null) {
            ShipAIConfig config = ship.getShipAI().getConfig();

            config.alwaysStrafeOffensively = true;
            config.backingOffWhileNotVentingAllowed = false;
            config.turnToFaceWithUndamagedArmor = true;

            ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
            ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING);
            if (ship.getHullSize() == ship.getHullSize().FRIGATE) {
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.HARASS_MOVE_IN);
            }

            if (ship.getFleetMember() == null)
                return;

            if (ship.getFleetMember().isFrigate() || ship.getFleetMember().isDestroyer()) {
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET);
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.ESCORT_OTHER_SHIP);
            }

        }
    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

}
//    private static final float HULL_RESISTANCE = 10.0f; // Value is an assumption based on the Kotlin code
//
//    @Override
//    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
//        switch (index) {
//            case 0:
//                return ((int) HULL_RESISTANCE) + "%";
//            case 1:
//                return "Neural Interface";
//            default:
//                return null;
//        }
//    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();


        tooltip.addSectionHeading("Proprietary Asset Protection Measures", Alignment.MID, opad);
        tooltip.addPara(
                "Freitag Ships cannot be recovered by opposing forces.", opad
        );
        tooltip.addSectionHeading("Drone Formations", Alignment.MID, opad);
        tooltip.addPara(
                "By pressing %s you can change formation and behaviour of your weapon drones.", opad, h,
                "Toggle Fighter Engage/Regroup (default Z)"
        );        tooltip.addPara(
                "By default drones will face the closest enemy larger than a fighter, in Engage Mode they will change orbit position depending on cursor location.", opad
        );

        tooltip.addSectionHeading("Drone Weapon Groups", Alignment.MID, opad);
        tooltip.addPara(
                "Drones will mirror the autofire selection of your weapon groups." +
                        "Turning on Autopilot will most likely change this selection." +
                        "You can toggle autofire in combat by pressing %s.", opad, h,
                "CTRL + Weapon Group Number"
        );
        tooltip.addSectionHeading("Flux affecting and range increasing hullmods", Alignment.MID, opad);
        tooltip.addSectionHeading("are shared between mothership and drones", Alignment.MID, 0f);
    }
}
