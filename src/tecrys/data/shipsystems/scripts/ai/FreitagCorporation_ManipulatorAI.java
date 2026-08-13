package tecrys.data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import java.awt.geom.Line2D;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class FreitagCorporation_ManipulatorAI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private float RANGEOFBURN = 1800f;
    private float CLUSTER_RADIUS = 1000f;

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

            if (ship.getFluxTracker().isOverloadedOrVenting() || ship.getEngineController().isDisabled()) {
                return;
            }

            List<ShipAPI> enemies = AIUtils.getNearbyEnemies(ship, RANGEOFBURN + CLUSTER_RADIUS);

            int maxClusterSize = 0;
            ShipAPI clusterCenter = null;

            // 1. Identify the center of the largest enemy cluster
            for (ShipAPI e1 : enemies) {
                if (e1.isFighter() || e1.isDrone() || !e1.isAlive()) continue;

                int currentClusterSize = 0;
                for (ShipAPI e2 : enemies) {
                    if (e2.isFighter() || e2.isDrone() || !e2.isAlive()) continue;

                    if (MathUtils.getDistance(e1, e2) <= CLUSTER_RADIUS) {
                        currentClusterSize++;
                    }
                }

                if (currentClusterSize > maxClusterSize) {
                    maxClusterSize = currentClusterSize;
                    clusterCenter = e1;
                }
            }

            if (clusterCenter == null) {
                return;
            }

            // 2. Find the nearest enemy within that largest cluster
            ShipAPI idealTarget = null;
            float minDistance = Float.MAX_VALUE;

            for (ShipAPI e : enemies) {
                if (e.isFighter() || e.isDrone() || !e.isAlive()) continue;

                if (MathUtils.getDistance(e, clusterCenter) <= CLUSTER_RADIUS) {
                    float distToShip = MathUtils.getDistance(ship, e);
                    if (distToShip < minDistance) {
                        minDistance = distToShip;
                        idealTarget = e;
                    }
                }
            }

            if (idealTarget == null) {
                return;
            }

            // Overwrite the ship's current target to force the AI to aim at the ideal cluster target
            if (ship.getShipTarget() != idealTarget) {
                ship.setShipTarget(idealTarget);
            }

            // 3. Verify the route is clear of allied ships
            boolean allyInWay = false;
            List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, RANGEOFBURN);

            for (ShipAPI ally : allies) {
                if (ally == ship || ally.isFighter() || ally.isDrone() || !ally.isAlive()) continue;

                float distToLine = (float) Line2D.ptSegDist(
                        ship.getLocation().x, ship.getLocation().y,
                        idealTarget.getLocation().x, idealTarget.getLocation().y,
                        ally.getLocation().x, ally.getLocation().y
                );

                // Check distance against combined collision bounds plus a small safety margin
                if (distToLine < (ally.getCollisionRadius() + ship.getCollisionRadius() + 25f)) {
                    allyInWay = true;
                    break;
                }
            }

            if (allyInWay) {
                return;
            }

            // 4. Check if aligned and activate
            boolean targetToManipulate = !this.isWithinArc(idealTarget, 300f) && this.isWithinArc(idealTarget, RANGEOFBURN);

            if (!system.isOn() && targetToManipulate) {
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

    public boolean isWithinArc(CombatEntityAPI entity, float range) {
        float arc = 5;
        Vector2f entityTarget = entity.getLocation();
        Vector2f wep = ship.getLocation();
        Vector2f endArcLeft = MathUtils.getPointOnCircumference(wep, range, ship.getFacing() - arc);
        Vector2f endArcRight = MathUtils.getPointOnCircumference(wep, range, ship.getFacing() + arc);
        float radSquared = entity.getCollisionRadius() * entity.getCollisionRadius();

        return (Line2D.ptSegDistSq(wep.x, wep.y, endArcLeft.x, endArcLeft.y, entityTarget.x, entityTarget.y) <= radSquared)
                || (Line2D.ptSegDistSq(wep.x, wep.y, endArcRight.x, endArcRight.y, entityTarget.x, entityTarget.y) <= radSquared);
    }
}