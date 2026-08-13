package tecrys.data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import static com.fs.starfarer.api.combat.ShipSystemAPI.SystemState.ACTIVE;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

public class FreitagCorporation_Manipulator extends BaseShipSystemScript {

    private static boolean graphicsLibEnabled = Global.getSettings().getModManager().isModEnabled("shaderLib");
    private static Map<ShipAPI.HullSize, Float> mag = new HashMap<>();

    static {
        mag.put(ShipAPI.HullSize.FIGHTER, 0.15f);
        mag.put(ShipAPI.HullSize.FRIGATE, 0.15f);
        mag.put(ShipAPI.HullSize.DESTROYER, 0.15f);
        mag.put(ShipAPI.HullSize.CRUISER, 0.33f);
        mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.33f);
    }
    protected Object STATUSKEY1 = new Object();

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id);
        } else {
            stats.getMaxSpeed().modifyFlat(id, 400f * effectLevel);
            stats.getAcceleration().modifyFlat(id, 300f * effectLevel);
        }

        float mult = mag.get(ShipAPI.HullSize.CRUISER);
        if (stats.getVariant() != null) {
            mult = mag.get(stats.getVariant().getHullSize());
        }

        stats.getShieldDamageTakenMult().modifyMult(id, 1f - (0.9f * effectLevel));
        stats.getShieldArcBonus().modifyFlat(id, 100f * effectLevel);

        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        }
        if (ship == null) {
            return;
        }
        if (player) {
            ShipSystemAPI system = ship.getSystem();
            if (system != null) {
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                        system.getSpecAPI().getIconSpriteName(), system.getDisplayName(),
                        "90% less shield damage taken", false);
            }
        }
        if (ship.getSystem().getCooldownRemaining() < 0.2f) {
            return;
        }

        List<ShipAPI> targetsToRepulse = AIUtils.getNearbyEnemies(ship, 50f);
        boolean containATrueTarget = false;
        for (ShipAPI target : targetsToRepulse) {
            if (!target.isFighter()) {
                containATrueTarget = true;
                break;
            }
        }

        if (containATrueTarget) {
            Vector2f positionRepulse = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), ship.getFacing());

            for (ShipAPI target2 : targetsToRepulse) {
                Vector2f dir = new Vector2f();
                Vector2f.sub(target2.getLocation(), positionRepulse, dir);

                if (dir.lengthSquared() > 0) {
                    dir.normalise();
                } else {
                    dir.set(1f, 0f);
                }

                // Repulse target
                float targetForce = target2.getMass() * 150f;
                CombatUtils.applyForce(target2, dir, targetForce);

                // Repulse user's ship in the opposite direction
                Vector2f oppositeDir = new Vector2f(-dir.x, -dir.y);
                float shipForce = ship.getMass() * 150f;
                CombatUtils.applyForce(ship, oppositeDir, shipForce);
            }

            for (int i = 0; i < 5; i++) {
                Global.getCombatEngine().spawnEmpArcVisual(positionRepulse, null, MathUtils.getRandomPointOnCircumference(positionRepulse, 10), null, 10, Color.cyan, Color.BLUE);
            }
            if (graphicsLibEnabled) {
                CustomRippleDistortion(positionRepulse, new Vector2f(0, 0), 200, 3f, false, 0f, 360f, 0.5f, 0f, 0.5f, 0.5f, 1f, 0f);
            }
            if (ship.getSystem().getState().equals(ACTIVE)) {
                ship.getSystem().deactivate();
            }
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getShieldArcBonus().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("increased engine power", false);
        }
        if (index == 1) {
            return new StatusData("shield absorbs 10x damage", false);
        }
        return null;
    }

    public static void CustomRippleDistortion(Vector2f loc, Vector2f vel, float size, float intensity, boolean flip, float angle, float arc, float edgeSmooth, float fadeIn, float last, float fadeOut, float growthTime, float shrinkTime) {
        RippleDistortion ripple = new RippleDistortion(loc, vel);
        ripple.setIntensity(intensity);
        ripple.setSize(size);
        ripple.setArc(angle - arc / 2, angle + arc / 2);
        if (edgeSmooth != 0) {
            ripple.setArcAttenuationWidth(edgeSmooth);
        }
        ripple.flip(flip);
        if (fadeIn != 0) {
            ripple.fadeInIntensity(fadeIn);
        }
        ripple.setLifetime(last);
        if (fadeOut != 0) {
            ripple.setAutoFadeIntensityTime(fadeOut);
        }
        if (growthTime != 0) {
            ripple.fadeInSize(growthTime);
        }
        if (shrinkTime != 0) {
            ripple.setAutoFadeSizeTime(shrinkTime);
        }
        ripple.setFrameRate(60);
        DistortionShader.addDistortion(ripple);
    }
}