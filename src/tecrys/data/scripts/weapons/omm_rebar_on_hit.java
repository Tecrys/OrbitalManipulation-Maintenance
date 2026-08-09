package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.List;

public class omm_rebar_on_hit implements OnHitEffectPlugin {

    private static final float SHIELD_PIERCE_CHANCE = 0.3f; // 30% chance to pierce shields
    private static final float STUCK_DURATION = 10.0f;       // Stays stuck for 10 seconds
    private static final float EMP_DAMAGE_MULT = 2.0f;       // 100% increased EMP damage taken
    private static final String STAT_ID_PREFIX = "omm_rebar_stuck_";

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (engine == null || target == null || !(target instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) target;

        Vector2f hitPoint = point != null ? new Vector2f(point) : new Vector2f(projectile.getLocation());

        // Check if hitting an active shield
        ShieldAPI shield = ship.getShield();
        boolean isHittingShield = shield != null && shield.isOn() && shield.isWithinArc(hitPoint);

        if (isHittingShield) {
            // Roll for the 30% shield-pierce chance
            if (Math.random() >= SHIELD_PIERCE_CHANCE) {
                // Shield blocks normally; do not stick
                return;
            }

            // Find where it would hit on the hull beneath the shield
            Vector2f shieldHitPoint = new Vector2f(hitPoint);
            Vector2f vel = projectile.getVelocity();
            if (vel.length() == 0) {
                vel = VectorUtils.rotate(new Vector2f(0f, 100f), projectile.getFacing());
            }
            Vector2f lineEnd = new Vector2f(shieldHitPoint.x + vel.x * 5f, shieldHitPoint.y + vel.y * 5f);
            Vector2f hullPoint = CollisionUtils.getCollisionPoint(shieldHitPoint, lineEnd, ship);
            if (hullPoint == null) {
                hullPoint = CollisionUtils.getNearestPointOnBounds(projectile.getLocation(), ship);
            }
            if (hullPoint != null) {
                hitPoint = hullPoint;
            } else {
                hitPoint = new Vector2f(ship.getLocation());
            }

            // Spawn 5 staggered bright blue explosions from shield impact down to the hull attachment over 0.7 seconds
            engine.addPlugin(new ShieldPierceExplosionPlugin(
                    engine,
                    shieldHitPoint,
                    hitPoint,
                    new Color(120, 210, 255, 255),
                    38f,
                    0.5f
            ));
        } else {
            // Ensure hitPoint snaps cleanly onto the hull bounds
            Vector2f boundsPoint = CollisionUtils.getNearestPointOnBounds(hitPoint, ship);
            if (boundsPoint != null) {
                hitPoint = boundsPoint;
            }
        }

        // Attach a new independent stuck rebar script plugin for this specific hit location
        RebarStuckScript stuckScript = new RebarStuckScript(ship, hitPoint, projectile.getFacing());
        engine.addPlugin(stuckScript);
    }

    // Helper plugin to handle 5 staggered sequential explosions on shield pierce over 0.7 seconds
    public static class ShieldPierceExplosionPlugin extends BaseEveryFrameCombatPlugin {
        private final CombatEngineAPI engine;
        private final Vector2f start;
        private final Vector2f end;
        private final Color color;
        private final float size;
        private final float duration;
        private float elapsed = 0f;
        private int stage = 0;

        public ShieldPierceExplosionPlugin(CombatEngineAPI engine, Vector2f start, Vector2f end, Color color, float size, float duration) {
            this.engine = engine;
            this.start = start;
            this.end = end;
            this.color = color;
            this.size = size;
            this.duration = duration;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine == null || engine.isPaused()) return;
            elapsed += amount;

            if (stage == 0) {
                // 1st explosion at 0% (shield impact)
                engine.spawnExplosion(start, new Vector2f(), color, size, duration);
                stage = 1;
            } else if (stage == 1 && elapsed >= 0.12f) {
                // 2nd explosion at 25% along the path
                Vector2f pos = new Vector2f(start.x + (end.x - start.x) * 0.25f, start.y + (end.y - start.y) * 0.25f);
                engine.spawnExplosion(pos, new Vector2f(), color, size, duration);
                stage = 2;
            } else if (stage == 2 && elapsed >= 0.24f) {
                // 3rd explosion at 50% along the path
                Vector2f pos = new Vector2f(start.x + (end.x - start.x) * 0.5f, start.y + (end.y - start.y) * 0.5f);
                engine.spawnExplosion(pos, new Vector2f(), color, size, duration);
                stage = 3;
            } else if (stage == 3 && elapsed >= 0.36f) {
                // 4th explosion at 75% along the path
                Vector2f pos = new Vector2f(start.x + (end.x - start.x) * 0.75f, start.y + (end.y - start.y) * 0.75f);
                engine.spawnExplosion(pos, new Vector2f(), color, size, duration);
                stage = 4;
            } else if (stage == 4 && elapsed >= 0.48f) {
                // 5th explosion at 100% where the rod attaches to the hull
                engine.spawnExplosion(end, new Vector2f(), color, size, duration);
                engine.removePlugin(this);
            }
        }
    }

    public static class RebarStuckScript extends BaseEveryFrameCombatPlugin {
        private final ShipAPI ship;
        private float remainingDuration = STUCK_DURATION;
        private final Vector2f localPos;
        private final float relativeAngle;
        private final SpriteAPI rebarSprite;
        private final SpriteAPI glowSprite;
        private final String uniqueStatId;
        private float timeToNextArc = 3.0f;

        public RebarStuckScript(ShipAPI ship, Vector2f worldHitPoint, float projectileFacing) {
            this.ship = ship;
            this.rebarSprite = Global.getSettings().getSprite("misc", "omm_redhot_rebar");

            // Optional glow sprite asset (make sure to define "omm_redhot_rebar_glow" in settings.json if used, or it falls back safely)
            SpriteAPI tempGlow = null;
            try {
                tempGlow = Global.getSettings().getSprite("misc", "omm_redhot_rebar_glow");
            } catch (Exception ignored) {}
            this.glowSprite = tempGlow;

            this.uniqueStatId = STAT_ID_PREFIX + System.currentTimeMillis() + "_" + Math.random();

            // Calculate local position offset and angle relative to the target ship's core/facing
            Vector2f rel = new Vector2f(worldHitPoint.x - ship.getLocation().x, worldHitPoint.y - ship.getLocation().y);
            this.localPos = VectorUtils.rotate(rel, -ship.getFacing());
            this.relativeAngle = projectileFacing - ship.getFacing() - 90f;

            // Apply increased EMP vulnerability stat for this specific stuck rebar
            ship.getMutableStats().getEmpDamageTakenMult().modifyMult(uniqueStatId, EMP_DAMAGE_MULT);
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || ship == null || !ship.isAlive() || !engine.isEntityInPlay(ship)) {
                cleanup();
                return;
            }

            // Pause check: completely freeze logic and effects while the game is paused
            if (engine.isPaused()) {
                return;
            }

            remainingDuration -= amount;
            if (remainingDuration <= 0f) {
                cleanup();
                return;
            }

            // Update absolute world position dynamically using a cloned offset vector to prevent drift
            Vector2f currentAbsPos = new Vector2f(ship.getLocation());
            Vector2f rotatedOffset = VectorUtils.rotate(new Vector2f(localPos), ship.getFacing());
            Vector2f.add(currentAbsPos, rotatedOffset, currentAbsPos);

            float currentAngle = ship.getFacing() + relativeAngle;

            // Slower, heavy, blindingly bright white/yellow-hot core pulse calculation
            float timeElapsed = STUCK_DURATION - remainingDuration;
            float pulse = 0.8f + 0.2f * (float) Math.sin(timeElapsed * 1.5f);

            int red = 255;
            int green = (int) (210 + 45 * pulse);
            int blue = (int) (140 + 115 * pulse);
            Color blindingWhiteHotColor = new Color(red, green, blue, 255);

            // 1. Render the solid opaque rebar base
            if (rebarSprite != null) {
                MagicRender.singleframe(
                        rebarSprite,
                        currentAbsPos,
                        new Vector2f(rebarSprite.getWidth(), rebarSprite.getHeight()),
                        currentAngle,
                        blindingWhiteHotColor,
                        false
                );
            }

            // 2. Layer an additive glow sprite on top for a convincing luminous bloom effect
            if (glowSprite != null) {
                MagicRender.singleframe(
                        glowSprite,
                        currentAbsPos,
                        new Vector2f(glowSprite.getWidth(), glowSprite.getHeight()),
                        currentAngle,
                        new Color(255, 180, 100, 220),
                        true
                );
            }

            // Infrequent blue visual EMP arcs jumping from the furthest tip of the rebar back to a close local point on the ship hull
            timeToNextArc -= amount;
            if (timeToNextArc <= 0f) {
                timeToNextArc = (float) Math.random() * 1.5f + 0.5f;

                if (Math.random() < 0.7f) {
                    // Calculate the tip of the rebar sticking furthest out of the hull
                    Vector2f rebarTip = MathUtils.getPointOnCircumference(currentAbsPos, rebarSprite.getHeight() * 0.5f, currentAngle - 90f);
                    // Pick a local destination point close to the rebar base so it doesn't arc too far
                    Vector2f localHullDest = MathUtils.getRandomPointInCircle(currentAbsPos, 60f);

                    engine.spawnEmpArcVisual(
                            rebarTip,
                            null,
                            localHullDest,
                            ship,
                            10f,
                            new Color(0, 120, 255, 255),   // Electric blue fringe
                            new Color(180, 230, 255, 255)  // Cyan/white core
                    );
                }
            }
        }

        private void cleanup() {
            if (ship != null) {
                ship.getMutableStats().getEmpDamageTakenMult().unmodify(uniqueStatId);
            }
            Global.getCombatEngine().removePlugin(this);
        }
    }
}