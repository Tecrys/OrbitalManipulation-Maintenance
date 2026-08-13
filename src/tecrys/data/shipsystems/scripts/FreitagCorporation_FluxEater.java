package tecrys.data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class FreitagCorporation_FluxEater extends BaseShipSystemScript {

    protected static float FLUX_RANGE = 1500f;
    public static float FLUX_CONVERTED_TIME = 1f;

    // Defines the flat amount of flux to transfer, replacing the base system cost
    public static float FLUX_TRANSFER_AMOUNT = 2500f;

    protected static float FLUX_MIN_CONVERTED = 0.6f;
    protected static String FLUX_STAT_ID = "FreitagCorporation_FluxEater_Stat";

    public static final Color JITTER_COLOR = new Color(200, 200, 25, 75);
    public static final Color JITTER_UNDER_COLOR = new Color(200, 200, 25, 155);

    public static final Color EMP_COLOR = new Color( 255, 251, 0, 45);
    public static final Color EMP_FRINGE_COLOR = new Color( 185, 255, 0, 30);

    public static float getRange(ShipAPI ship) {
        if (ship == null) {
            return FLUX_RANGE;
        }
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(FLUX_RANGE);
    }

    public static ShipAPI getAvailableAllyTarget(ShipAPI ship) {
        ShipAPI target = null;
        float min = 0.0f;
        List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, getRange(ship));
        for (ShipAPI ally : allies) {
            if (ally.isFighter() || !ally.isAlive()) {
                continue;
            }
            if (ally.getFluxTracker().getCurrFlux() > FLUX_TRANSFER_AMOUNT * FLUX_MIN_CONVERTED && ally.getFluxTracker().getCurrFlux() / ally.getFluxTracker().getMaxFlux() > min) {
                min = ally.getFluxTracker().getCurrFlux() / ally.getFluxTracker().getMaxFlux();
                target = ally;
            }
        }
        return target;
    }

    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        float jitterLevel = effectLevel;
        if (state == ShipSystemStatsScript.State.OUT) {
            jitterLevel *= jitterLevel;
        }
        float maxRangeBonus = 15f;
        float jitterRangeBonus = jitterLevel * maxRangeBonus;
        ship.setJitterUnder(this, EMP_FRINGE_COLOR, jitterLevel, 11, 0f, 3f + jitterRangeBonus);
        ship.setJitter(this, EMP_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);

        String firedKey = "omm_flux_eater_fired_" + ship.getId();

        if (state == ShipSystemStatsScript.State.IN) {
            ship.getCustomData().remove(firedKey);
        } else if (effectLevel >= 1f) {

            if (!ship.getCustomData().containsKey(firedKey)) {
                ship.getCustomData().put(firedKey, true);

                ShipAPI target = getAvailableAllyTarget(ship);

                if (target != null) {
                    Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, ship.getLocation(), ship.getVelocity());

                    float fluxToRemove = FLUX_TRANSFER_AMOUNT;
                    if (fluxToRemove < 0) {
                        fluxToRemove = 0;
                    }

                    float time = FLUX_CONVERTED_TIME;
                    float fluxPerSec = fluxToRemove / time;

                    // Passes the caster ship as an argument to process the flux gain
                    Global.getCombatEngine().addPlugin(createJitterPlugin(ship, target, time / 2f, fluxPerSec));
                }
            }
        }
    }

    protected EveryFrameCombatPlugin createJitterPlugin(final ShipAPI ship, final ShipAPI target, final float timeEffect, final float fluxPerSec) {
        return new BaseEveryFrameCombatPlugin() {
            float elapsed = 0f;
            int state = 0;
            float arcTimer = 0f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused()) {
                    return;
                }
                elapsed += amount;
                arcTimer += amount;

                if (elapsed > timeEffect) {
                    state++;
                    elapsed -= timeEffect;
                }

                float fluxThisFrame = fluxPerSec * amount;
                target.getFluxTracker().decreaseFlux(fluxThisFrame);
                ship.getFluxTracker().increaseFlux(fluxThisFrame, false);

                // Spawns multiple arcs evenly over the transfer duration
                if (arcTimer > 0.1f) {
                    arcTimer -= 0.1f;
                    Vector2f shippos = ship.getLocation();
                    Vector2f EMPtarget = MathUtils.getPointOnCircumference(target.getLocation(), target.getCollisionRadius(), target.getFacing());
                    Global.getCombatEngine().spawnEmpArcVisual(shippos, null, MathUtils.getRandomPointOnCircumference(EMPtarget, 1f), null, 10f, EMP_FRINGE_COLOR, EMP_COLOR);
                }

                if (state >= 2) {
                    target.getMutableStats().getFluxDissipation().unmodifyFlat(FLUX_STAT_ID);
                    Global.getCombatEngine().removePlugin(this);
                    return;
                }

                float jitterLevel;
                if (state == 0) {
                    jitterLevel = elapsed / timeEffect;
                } else {
                    jitterLevel = (timeEffect - elapsed) / timeEffect;
                    jitterLevel *= jitterLevel;
                }

                float maxRangeBonus = 25f;
                float jitterRangeBonus = jitterLevel * maxRangeBonus;

                target.setJitterUnder(target, EMP_FRINGE_COLOR, jitterLevel, 11, 0f, 3f + jitterRangeBonus);
                target.setJitter(target, EMP_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);
            }
        };
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) {
            return null;
        }
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) {
            return null;
        }

        ShipAPI target = getAvailableAllyTarget(ship);
        if (target != null) {
            return "READY";
        } else {
            return "OUT OF TARGET";
        }
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return getAvailableAllyTarget(ship) != null;
    }
}