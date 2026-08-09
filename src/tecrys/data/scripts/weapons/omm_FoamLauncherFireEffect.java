package tecrys.data.scripts.weapons;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.awt.Color;

import com.fs.starfarer.api.AnimationAPI;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

public class omm_FoamLauncherFireEffect extends BaseCombatLayeredRenderingPlugin implements OnFireEffectPlugin,
        OnHitEffectPlugin,
        EveryFrameWeaponEffectPlugin {

    // --- SWIVEL ARC CONFIGURATION ---
    private static final float TOTAL_ARC_DEGREES = 8f; // Sweeps from -12 to +12 degrees
    private int currentShotIndex = 0;
    private boolean sweepLeftToRight = true;
    // --------------------------------

    public omm_FoamLauncherFireEffect() {
    }

    protected String getLoopId() {
        return "cryoflamer_loop";
    }

    protected void playImpactSound(ApplyDamageResultAPI damageResult, Vector2f point, Vector2f vel) {
        Misc.playSound(damageResult, point, vel,
                "cryoflamer_hit_shield_light",
                "cryoflamer_hit_shield_solid",
                "cryoflamer_hit_shield_heavy",
                "cryoflamer_hit_light",
                "cryoflamer_hit_solid",
                "cryoflamer_hit_heavy");
    }

    protected String getParticleSpriteCat() {
        return "misc";
    }
    protected String getParticleSpriteKey() {
        return "nebula_particles";
    }
    protected float getParticleScale() {
        return 1.1f;
    }
    protected float getParticleScaleIncreaseRateMult() {
        return 1.4f;
    }
    protected int getNumParticles() {
        return 10;
    }
    protected float getThresholdDist() {
        return 20f;
    }

    protected List<omm_FoamLauncherFireEffect> trails;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        AnimationAPI anim = weapon.getAnimation();
        if (anim == null) {
            return;
        }

        // Freeze standard frame-by-frame auto-playback
        anim.pause();

        // Safety checks for ammo usage
        if (!weapon.usesAmmo() || weapon.getMaxAmmo() <= 0) {
            return;
        }

        int numFrames = anim.getNumFrames();
        if (numFrames <= 1) {
            return;
        }

        int currentAmmo = weapon.getAmmo();
        int maxAmmo = weapon.getMaxAmmo();

        // Calculate fraction of ammo consumed (0.0 = full ammo, 1.0 = completely empty)
        float ammoDepletedRatio = 1.0f - ((float) currentAmmo / (float) maxAmmo);

        // Clamp value between 0.0 and 1.0 to prevent out-of-bounds on ammo boosts/regen
        ammoDepletedRatio = Math.max(0.0f, Math.min(1.0f, ammoDepletedRatio));

        // Map depleted ratio to frame index [0 to numFrames - 1]
        int targetFrame = Math.round(ammoDepletedRatio * (numFrames - 1));
        targetFrame = Math.min(numFrames - 1, Math.max(0, targetFrame));

        // Update current animation frame
        anim.setFrame(targetFrame);

        if (trails == null) return;

        Iterator<omm_FoamLauncherFireEffect> iter = trails.iterator();
        while (iter.hasNext()) {
            if (iter.next().isExpired()) iter.remove();
        }

        // --- SWIVEL RESET ON COOLDOWN ---
        if (!weapon.isFiring() && weapon.getCooldownRemaining() > 0.1f) {
            if (currentShotIndex > 0) {
                currentShotIndex = 0;
                sweepLeftToRight = !sweepLeftToRight; // Alternate sweep direction on next burst
            }
        }
        // --------------------------------

        // --- EVERY-FRAME BARREL SWEEP LOCK ---
        // Locks the turret barrel angle every frame while firing so Starsector's aiming AI
        // cannot drag the barrel back toward center between individual shots.
//        if (weapon.isFiring()) {
//            int burstSize = Math.max(1, weapon.getSpec().getBurstSize());
//            float progress = (burstSize <= 1) ? 0.5f : ((float) currentShotIndex / (float) (burstSize - 1));
//            if (!sweepLeftToRight) {
//                progress = 1.0f - progress;
//            }
//            float angleOffset = -(TOTAL_ARC_DEGREES * 0.5f) + (TOTAL_ARC_DEGREES * progress);
//            weapon.setCurrAngle(weapon.getCurrAngle() + angleOffset);
//        }
        // -------------------------------------

        // Sound loop playback
        if (weapon.getShip() != null) {
            float maxRange = weapon.getRange();
            ShipAPI ship = weapon.getShip();
            Vector2f com = new Vector2f();
            float weight = 0f;
            float totalDist = 0f;
            Vector2f source = weapon.getLocation();
            for (omm_FoamLauncherFireEffect curr : trails) {
                if (curr.proj != null) {
                    Vector2f.add(com, curr.proj.getLocation(), com);
                    weight += curr.proj.getBrightness();
                    totalDist += Misc.getDistance(source, curr.proj.getLocation());
                }
            }
            if (weight > 0.1f) {
                com.scale(1f / weight);
                float volume = Math.min(weight, 1f);
                if (trails.size() > 0) {
                    totalDist /= (float) trails.size();
                    float mult = totalDist / Math.max(maxRange, 1f);
                    mult = 1f - mult;
                    if (mult > 1f) mult = 1f;
                    if (mult < 0f) mult = 0f;
                    mult = (float) Math.sqrt(mult);
                    volume *= mult;
                }
                Global.getSoundPlayer().playLoop(getLoopId(), ship, 1f, volume, com, ship.getVelocity());
            }
        }

        float numIter = 1f;
        amount /= numIter;
        float thresholdDist = getThresholdDist();

        // Drag along the previous projectile so the stream bends naturally without jagged steps
        for (int i = 0; i < numIter; i++) {
            for (omm_FoamLauncherFireEffect trail : trails) {
                if (trail.prev != null && !trail.prev.isExpired() && Global.getCombatEngine().isEntityInPlay(trail.prev)) {
                    float dist1 = Misc.getDistance(trail.prev.getLocation(), trail.proj.getLocation());
                    if (dist1 < trail.proj.getProjectileSpec().getLength() * 1f) {
                        float maxSpeed = trail.prev.getMoveSpeed() * 0.5f;
                        float e = trail.prev.getElapsed();
                        float t = 0.5f;
                        if (e > t) {
                            maxSpeed *= Math.max(0.25f, 1f - (e - t) * 0.5f);
                        }
                        if (dist1 < thresholdDist && e > t) {
                            maxSpeed *= dist1 / thresholdDist;
                        }

                        Vector2f driftTo = Misc.closestPointOnLineToPoint(trail.proj.getLocation(), trail.proj.getTailEnd(), trail.prev.getLocation());
                        float dist = Misc.getDistance(driftTo, trail.prev.getLocation());
                        Vector2f diff = Vector2f.sub(driftTo, trail.prev.getLocation(), new Vector2f());
                        diff = Misc.normalise(diff);
                        diff.scale(Math.min(dist, maxSpeed * amount));
                        Vector2f.add(trail.prev.getLocation(), diff, trail.prev.getLocation());
                        Vector2f.add(trail.prev.getTailEnd(), diff, trail.prev.getTailEnd());
                    }
                }
            }
        }



    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Color color = getParticleColor();

        Vector2f vel = new Vector2f();
        if (target instanceof ShipAPI) {
            vel.set(target.getVelocity());
        }

        float size = Math.max(45f, projectile.getProjectileSpec().getWidth() * 1.5f);
        float sizeMult = Misc.getHitGlowSize(100f, projectile.getDamage().getBaseDamage(), damageResult) / 100f;
        float dur = 1f;
        float rampUp = 0f;
        Color c = Misc.scaleAlpha(color, projectile.getBrightness());
        engine.addNebulaParticle(point, vel, size, 5f + 3f * sizeMult,
                rampUp, 0f, dur, c);

        playImpactSound(damageResult, point, vel);
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (projectile == null || weapon == null || engine == null) return;

        // --- 1. APPLY SWEEPING ARC SWIVEL ---
        int burstSize = weapon.getSpec().getBurstSize();
        if (burstSize < 1) burstSize = 1;

        float progress = (burstSize <= 1) ? 0.5f : ((float) currentShotIndex / (float) (burstSize - 1));
        if (!sweepLeftToRight) {
            progress = 1.0f - progress;
        }

        float angleOffset = -(TOTAL_ARC_DEGREES * 0.5f) + (TOTAL_ARC_DEGREES * progress);
        float newFacing = projectile.getFacing() + angleOffset;

        projectile.setFacing(newFacing);
        float speed = projectile.getVelocity().length();
        float rad = (float) Math.toRadians(newFacing);
        projectile.getVelocity().set(
                (float) Math.cos(rad) * speed,
                (float) Math.sin(rad) * speed
        );

        // Barrel angle is now authoritatively locked every frame in advance(),
        // but setting it here ensures zero single-frame visual delay on shot creation.
       // weapon.setCurrAngle(weapon.getCurrAngle() + angleOffset);

        currentShotIndex++;
        if (currentShotIndex >= burstSize) {
            currentShotIndex = 0;
            sweepLeftToRight = !sweepLeftToRight;
        }
        // ------------------------------------

        // --- 2. SPAWN CRYOFFLAMER STREAM TRAIL ---
        String prevKey = "foam_prev_" + weapon.getShip().getId() + "_" + weapon.getSlot().getId();
        DamagingProjectileAPI prev = (DamagingProjectileAPI) engine.getCustomData().get(prevKey);

        omm_FoamLauncherFireEffect trail = createTrail(projectile, prev);
        CombatEntityAPI e = engine.addLayeredRenderingPlugin(trail);
        e.getLocation().set(projectile.getLocation());

        engine.getCustomData().put(prevKey, projectile);

        if (trails == null) {
            trails = new ArrayList<omm_FoamLauncherFireEffect>();
        }
        trails.add(0, trail);
    }

    protected omm_FoamLauncherFireEffect createTrail(DamagingProjectileAPI projectile, DamagingProjectileAPI prev) {
        return new omm_FoamLauncherFireEffect(projectile, prev);
    }

    public static class ParticleData {
        public SpriteAPI sprite;
        public Vector2f offset = new Vector2f();
        public Vector2f vel = new Vector2f();
        public float scale = 1f;
        public DamagingProjectileAPI proj;
        public float scaleIncreaseRate = 1f;
        public float turnDir = 1f;
        public float angle = 1f;
        public FaderUtil fader;

        public ParticleData(DamagingProjectileAPI proj, omm_FoamLauncherFireEffect effect) {
            this.proj = proj;
            sprite = Global.getSettings().getSprite(effect.getParticleSpriteCat(), effect.getParticleSpriteKey());

            float i = Misc.random.nextInt(4);
            float j = Misc.random.nextInt(4);
            sprite.setTexWidth(0.25f);
            sprite.setTexHeight(0.25f);
            sprite.setTexX(i * 0.25f);
            sprite.setTexY(j * 0.25f);

            // Normal blend so it looks like dense foam instead of glowing light
            sprite.setNormalBlend();

            angle = (float) Math.random() * 360f;

            float maxDur = proj.getWeapon().getRange() / proj.getWeapon().getProjectileSpeed();
            scaleIncreaseRate = 3.2f / maxDur;
            scaleIncreaseRate *= effect.getParticleScaleIncreaseRateMult();
            scale = effect.getParticleScale();

            turnDir = Math.signum((float) Math.random() - 0.5f) * 60f * (float) Math.random();

            float driftDir = (float) Math.random() * 360f;
            vel = Misc.getUnitVectorAtDegreeAngle(driftDir);
            vel.scale(proj.getProjectileSpec().getWidth() / maxDur * 0.33f);

            fader = new FaderUtil(0f, 0.25f, 0.5f);
            fader.fadeIn();
        }

        public void advance(float amount) {
            scale += scaleIncreaseRate * amount;
            if (scale < 1f) {
                scale += scaleIncreaseRate * amount * 1f;
            }

            offset.x += vel.x * amount;
            offset.y += vel.y * amount;

            angle += turnDir * amount;

            fader.advance(amount);
        }
    }

    protected List<ParticleData> particles = new ArrayList<ParticleData>();
    protected DamagingProjectileAPI proj;
    protected DamagingProjectileAPI prev;
    protected float baseFacing = 0f;

    public omm_FoamLauncherFireEffect(DamagingProjectileAPI proj, DamagingProjectileAPI prev) {
        this.proj = proj;
        this.prev = prev;

        baseFacing = proj.getFacing();

        int num = getNumParticles();
        for (int i = 0; i < num; i++) {
            particles.add(new ParticleData(proj, this));
        }

        float length = proj.getProjectileSpec().getLength();
        float width = Math.max(5f, proj.getProjectileSpec().getWidth() * 1.5f);

        float index = 0;
        for (ParticleData p : particles) {
            float f = index / (particles.size() - 1);
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);
            dir.scale(length * f);
            Vector2f.add(p.offset, dir, p.offset);

            p.offset = Misc.getPointWithinRadius(p.offset, width * 0.5f);
            index++;
        }
    }

    public float getRenderRadius() {
        return 300f;
    }

    protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return layers;
    }

    @Override
    public void init(CombatEntityAPI entity) {
        super.init(entity);
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;

        entity.getLocation().set(proj.getLocation());

        for (ParticleData p : particles) {
            p.advance(amount);
        }
    }

    @Override
    public boolean isExpired() {
        return proj.isExpired() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public Color getParticleColor() {
        return new Color(240, 210, 145, 100);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float x = entity.getLocation().x;
        float y = entity.getLocation().y;

        Color color = getParticleColor();
        float b = proj.getBrightness();
        b *= viewport.getAlphaMult();

        for (ParticleData p : particles) {
            float size = Math.max(8f, proj.getProjectileSpec().getWidth() * 1.5f);
            size *= p.scale;

            float alphaMult = 1f;
            Vector2f offset = p.offset;
            float diff = Misc.getAngleDiff(baseFacing, proj.getFacing());
            if (Math.abs(diff) > 0.1f) {
                offset = Misc.rotateAroundOrigin(offset, diff);
            }
            Vector2f loc = new Vector2f(x + offset.x, y + offset.y);

            p.sprite.setAngle(p.angle);
            p.sprite.setSize(size, size);
            p.sprite.setAlphaMult(b * alphaMult * p.fader.getBrightness());
            p.sprite.setColor(color);
            p.sprite.renderAtCenter(loc.x, loc.y);
        }
    }
}