package tecrys.data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import tecrys.data.shipsystems.scripts.FreitagCorporation_FluxEater;

public class FreitagCorporation_FluxEaterAI implements ShipSystemAIScript {

    // Defines the maximum allowable flux level (0.0 to 1.0) after system use
    private static final float MAX_FLUX_PERCENTAGE = 0.85f;

    // Defines the minimum required flux level (0.0 to 1.0) on the target ally
    private static final float TARGET_MIN_FLUX_PERCENTAGE = 0.50f;

    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;

    private float tracker = 0;
    private float trackermax = 0.2f;

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        tracker += amount;

        if (tracker > trackermax) {
            tracker -= trackermax;

            if (ship.getFluxTracker().isOverloadedOrVenting()) {
                return;
            }

            // Prevents AI from activating the system if the transferred flux pushes it over the threshold
            float projectedFlux = ship.getFluxTracker().getCurrFlux() + FreitagCorporation_FluxEater.FLUX_TRANSFER_AMOUNT;
            if ((projectedFlux / ship.getFluxTracker().getMaxFlux()) > MAX_FLUX_PERCENTAGE) {
                return;
            }

            ShipAPI target2 = FreitagCorporation_FluxEater.getAvailableAllyTarget(ship);
            if (target2 == null) {
                return;
            }

            // Prevents AI from activating the system if the target ally is below the minimum flux threshold
            if ((target2.getFluxTracker().getCurrFlux() / target2.getFluxTracker().getMaxFlux()) <= TARGET_MIN_FLUX_PERCENTAGE) {
                return;
            }

            if (!system.isOn()) {
                if (AIUtils.canUseSystemThisFrame(ship)) {
                    ship.useSystem();
                }
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
        this.tracker = 0;
        this.trackermax = 0.2f;
    }
}