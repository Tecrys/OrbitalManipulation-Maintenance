package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.input.InputEventAPI;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class FreitagCorporation_Sand_Hopper_DefPlatform implements EveryFrameWeaponEffectPlugin {

    private boolean isDeployed = false;
    private float tracker = 0;
    private final float trackermax = 1f;
    private final float launchPlatformAnyways = 30;
    private float trackerBis = 0;

    //private CombatEntityAPI anchor;
    private SpriteAPI sprite;
    private Vector2f size;

    // The weapon who run is the head.
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused()) {
            return;
        }
        if (isDeployed) {
            return;
        }
//        if (sprite == null) {
//            displaySprite();
//        }
//        MagicRender.singleframe(sprite, weapon.getShip().getLocation(), size, weapon.getShip().getFacing() - 90, Color.WHITE, false, CombatEngineLayers.BELOW_SHIPS_LAYER);

        tracker += amount;

        if (tracker > trackermax) {
            tracker -= trackermax;

            ShipAPI ship = weapon.getShip();
            for (ShipAPI module : ship.getChildModulesCopy()) {
                module.setCurrentCR(ship.getCurrentCR());
            if (ship != null && ship.isAlive()) {
                if (ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.REACHED_WAYPOINT)) {
                    // Global.getCombatEngine().getCombatUI().addMessage(0, "REACHED_WAYPOINT");
                    module.setStationSlot(null);
                    isDeployed = true;
                    return;
                }
                trackerBis += trackermax;
                if (trackerBis >= launchPlatformAnyways) {
                    module.setStationSlot(null);
                    isDeployed = true;
                }
                else {
                    for (BattleObjectiveAPI objective : Global.getCombatEngine().getObjectives()) {
                        // Global.getCombatEngine().getCombatUI().addMessage(0, objective.getImportance()+"");
                        if (MathUtils.isWithinRange(ship, objective, 300)) {
                            //weapon.usesAmmo();
                            module.setStationSlot(null);
                            isDeployed = true;
                            return;
                        }
                    }
                }
            }
        }}
    }}
