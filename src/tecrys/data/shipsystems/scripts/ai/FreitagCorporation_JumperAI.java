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

public class FreitagCorporation_JumperAI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private float RANGEOFBURN = 500f;
    private float EVALUATION_RADIUS = 800f;

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

            if (ship.getFluxTracker().isOverloadedOrVenting() || ship.getEngineController().isDisabled() || target == null) {
                return;
            }

            boolean targetToManipulate = !this.isWithinArc(target, 150f) && this.isWithinArc(target, RANGEOFBURN);

            if (!system.isOn() && targetToManipulate) {

                int enemyFrigates = 0;
                int enemyDestroyers = 0;
                int enemyCruisers = 0;
                int enemyCapitals = 0;
                int friendlyShips = 0;

                List<ShipAPI> enemies = AIUtils.getNearbyEnemies(target, EVALUATION_RADIUS);
                for (ShipAPI e : enemies) {
                    if (e.isFighter() || e.isDrone() || !e.isAlive()) continue;

                    switch (e.getHullSize()) {
                        case FRIGATE:
                            enemyFrigates++;
                            break;
                        case DESTROYER:
                            enemyDestroyers++;
                            break;
                        case CRUISER:
                            enemyCruisers++;
                            break;
                        case CAPITAL_SHIP:
                            enemyCapitals++;
                            break;
                        default:
                            break;
                    }
                }

                List<ShipAPI> allies = AIUtils.getNearbyAllies(target, EVALUATION_RADIUS);
                for (ShipAPI a : allies) {
                    if (a.isFighter() || a.isDrone() || !a.isAlive() || a == ship) continue;
                    friendlyShips++;
                }

                if (target.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP || enemyCapitals > 0) {
                    if (friendlyShips < 3) {
                        return;
                    }
                }

                if (enemyFrigates > 3 || enemyDestroyers > 2 || enemyCruisers > 1) {
                    return;
                }

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