package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class omm_FoamBarrierEntityPlugin extends BaseCombatLayeredRenderingPlugin implements DamageTakenModifier {

    private final ShipAPI drone;

    // Lifetime and scaling configuration
    private final float maxLifetime = 7.0f;
    private float elapsed = 0f;
    private final float growthDuration = 1.5f;
    private final float shrinkDuration = 0.35f;
    private final float startScale = 0.05f;
    private float currentScale = startScale;

    // Per-element randomized size variation fields
    private final float sizeMultiplier;
    private final float baseDiameter;
    private final float pulseAmplitude;

    // Smooth pulsing animation fields
    private final float pulseSpeed;
    private final float pulsePhase;

    // Sprite grid selection (4x4 grid = 16 sprites)
    private SpriteAPI sprite;
    private final int spriteIndex;
    private float currentAngle;

    // Nebula particle emission tracking
    private float particleTimer = 0f;
    private final float particleInterval = 0.25f;

    // Attachment tracking (Generalized to CombatEntityAPI so it supports ships AND asteroids)
    private CombatEntityAPI attachedTarget = null;
    private Vector2f localOffset = null;
    private float angleOffset = 0f;

    // Damage tracking without triggering engine ship explosions or floating numbers
    private final float maxFoamHp = 200f;
    private final float bufferHp = 200f;
    private float damageTaken = 0f;

    public omm_FoamBarrierEntityPlugin(ShipAPI drone, float angle) {
        this.drone = drone;
        this.entity = drone;

        // True random rotation per element
        this.currentAngle = MathUtils.getRandomNumberInRange(0f, 360f);
        this.drone.setFacing(this.currentAngle);
        this.spriteIndex = MathUtils.getRandomNumberInRange(0, 15);

        // --- ENHANCED PER-ELEMENT SIZE VARIATION ---
        this.sizeMultiplier = MathUtils.getRandomNumberInRange(0.6f, 1.4f);
        this.baseDiameter = MathUtils.getRandomNumberInRange(42f, 60f);
        this.pulseAmplitude = MathUtils.getRandomNumberInRange(3f, 7f);
        // -------------------------------------------

        this.pulseSpeed = MathUtils.getRandomNumberInRange(1.5f, 3.0f);
        this.pulsePhase = MathUtils.getRandomNumberInRange(0f, (float) Math.PI * 2f);

        this.drone.setOwner(100);
        this.drone.setMaxHitpoints(bufferHp);
        this.drone.setHitpoints(bufferHp);
        this.drone.addListener(this);

        initSprite();

        spawnNebulaParticle();
        spawnNebulaParticle();
    }

    private void initSprite() {
        sprite = Global.getSettings().getSprite("graphics/weapons/omm_foam.png");
        if (sprite != null) {
            int col = spriteIndex % 4;
            int row = spriteIndex / 4;
            float cellSize = 0.25f;

            sprite.setTexWidth(cellSize);
            sprite.setTexHeight(cellSize);
            sprite.setTexX(col * cellSize);
            sprite.setTexY(row * cellSize);
            sprite.setAngle(currentAngle);

            sprite.setColor(new Color(210, 200, 180, 255));
        } else {
            Global.getLogger(omm_FoamBarrierEntityPlugin.class).error("Failed to load foam sprite: graphics/weapons/omm_foam.png");
        }
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;

        if (drone == null || !Global.getCombatEngine().isEntityInPlay(drone)) {
            Global.getCombatEngine().removeEntity(entity);
            return;
        }

        elapsed += amount;

        float shrinkStartTime = maxLifetime - shrinkDuration;

        if (elapsed < growthDuration) {
            float progress = elapsed / growthDuration;
            float easeOut = 1f - ((1f - progress) * (1f - progress) * (1f - progress));
            currentScale = startScale + (1f - startScale) * easeOut;
        } else if (elapsed >= shrinkStartTime) {
            float shrinkProgress = (elapsed - shrinkStartTime) / shrinkDuration;
            shrinkProgress = Math.min(1f, Math.max(0f, shrinkProgress));
            currentScale = 1.0f - (shrinkProgress * shrinkProgress);
        } else {
            currentScale = 1.0f;
        }

        if (attachedTarget == null) {
            drone.getVelocity().set(0f, 0f);
            checkForTouch();
        } else {
            boolean isAlive = true;
            if (attachedTarget instanceof ShipAPI) {
                isAlive = ((ShipAPI) attachedTarget).isAlive();
            }

            if (!isAlive || !Global.getCombatEngine().isEntityInPlay(attachedTarget)) {
                attachedTarget = null;
            } else {
                updateAttachedPosition();
            }
        }

        if (currentScale > 0.1f) {
            particleTimer += amount;
            if (particleTimer >= particleInterval) {
                particleTimer -= particleInterval;
                spawnNebulaParticle();
            }
        }

        boolean destroyedByDamage = damageTaken >= maxFoamHp;

        if (destroyedByDamage || elapsed >= maxLifetime) {
            // Clean removal without fleet UI ghosting
            Global.getCombatEngine().removeEntity(drone);
            Global.getCombatEngine().removeEntity(entity);
        }
    }

    private void spawnNebulaParticle() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || drone == null) return;

        Vector2f loc = new Vector2f(drone.getLocation());
        loc.x += MathUtils.getRandomNumberInRange(-20f, 20f) * currentScale;
        loc.y += MathUtils.getRandomNumberInRange(-20f, 20f) * currentScale;

        Vector2f vel = new Vector2f(drone.getVelocity());
        vel.x += MathUtils.getRandomNumberInRange(-18f, 18f);
        vel.y += MathUtils.getRandomNumberInRange(-18f, 18f);

        float size = MathUtils.getRandomNumberInRange(45f, 65f) * currentScale * sizeMultiplier;
        float endSizeMult = 1.4f;
        float rampUp = 0.2f;
        float rampDown = 0.5f;
        float duration = MathUtils.getRandomNumberInRange(1.1f, 1.5f);

        Color color = new Color(180, 145, 85, 125);

        engine.addNebulaParticle(loc, vel, size, endSizeMult, rampUp, rampDown, duration, color);
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (damage == null) return null;

        damageTaken += damage.getDamage();

        damage.setDamage(0f);
        damage.setFluxComponent(0f);

        return null;
    }

    private void checkForTouch() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship == drone || !ship.isAlive()) continue;

            boolean isOtherFoam = "foam_drone".equals(ship.getHullSpec().getHullId());
            boolean isShipTarget = !ship.isDrone() && !ship.isFighter() && ship.getHullSize() != ShipAPI.HullSize.FIGHTER;

            if (!isOtherFoam && !isShipTarget) continue;

            if (tryAttachToEntity(ship, isOtherFoam)) return;
        }

        for (CombatEntityAPI asteroid : engine.getAsteroids()) {
            if (asteroid == null || !engine.isEntityInPlay(asteroid)) continue;
            if (tryAttachToEntity(asteroid, false)) return;
        }
    }

    private boolean tryAttachToEntity(CombatEntityAPI entity, boolean isOtherFoam) {
        Vector2f closestPoint = CollisionUtils.getNearestPointOnBounds(drone.getLocation(), entity);
        if (closestPoint == null) closestPoint = entity.getLocation();

        float dist = MathUtils.getDistance(drone.getLocation(), closestPoint);

        float touchThreshold = isOtherFoam
                ? (14f * currentScale * sizeMultiplier)
                : (18f * currentScale * sizeMultiplier);

        if (dist <= touchThreshold) {
            attachToTarget(entity);
            return true;
        }
        return false;
    }

    private void attachToTarget(CombatEntityAPI target) {
        attachedTarget = target;
        angleOffset = currentAngle - target.getFacing();

        float dx = drone.getLocation().x - target.getLocation().x;
        float dy = drone.getLocation().y - target.getLocation().y;
        float rad = (float) Math.toRadians(-target.getFacing());
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        localOffset = new Vector2f(
                dx * cos - dy * sin,
                dx * sin + dy * cos
        );

        drone.setCollisionClass(CollisionClass.FIGHTER);
        drone.setMass(0.0001f);
    }

    private void updateAttachedPosition() {
        float rad = (float) Math.toRadians(attachedTarget.getFacing());
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float worldX = attachedTarget.getLocation().x + (localOffset.x * cos - localOffset.y * sin);
        float worldY = attachedTarget.getLocation().y + (localOffset.x * sin + localOffset.y * cos);

        drone.getLocation().set(worldX, worldY);
        drone.getVelocity().set(attachedTarget.getVelocity());
        currentAngle = attachedTarget.getFacing() + angleOffset;
        drone.setFacing(currentAngle);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (drone == null || sprite == null) return;

        float sinePulse = (float) Math.sin(elapsed * pulseSpeed + pulsePhase);

        float baseSize = (baseDiameter + sinePulse * pulseAmplitude) * sizeMultiplier;
        float scaledSize = baseSize * currentScale;

        sprite.setSize(scaledSize, scaledSize);
        sprite.setAngle(currentAngle);
        sprite.renderAtCenter(drone.getLocation().x, drone.getLocation().y);
    }

    @Override
    public float getRenderRadius() {
        return 99999999f;
    }

    public Enum<CombatEngineLayers> getActiveLayer() {
        return CombatEngineLayers.BELOW_SHIPS_LAYER;
    }
}