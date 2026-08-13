package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class omm_rotary_saw implements EveryFrameWeaponEffectPlugin {

    // --- CONFIGURABLE SAW SETTINGS ---
    private static final float X_OFFSET = 15f;
    private static final float Y_OFFSET = 0f;

    // How many seconds it takes for the saw to fade out once the missile fizzles
    private static final float FADE_TIME_SECONDS = 4f;

    private float sawSpinRate = 3800f; // deg/sec saw spins at
    private static final CombatEngineLayers RENDER_LAYER = CombatEngineLayers.CONTRAILS_LAYER;
    // -------------------------------------

    private SpriteAPI sprite = Global.getSettings().getSprite("misc", "omm_rotarysaw");
    private Vector2f size = new Vector2f(sprite.getWidth(), sprite.getHeight());
    private CombatEntityAPI projectile = null;
    private IntervalUtil Interval = new IntervalUtil(0.0f, 0.1f);
    private Vector2f beamend = new Vector2f();
    private Vector2f proj2Pos = new Vector2f();
    private boolean justSpawnedProj2 = false;
    private float sawangle = 0f;
    private CombatEntityAPI projectile2 = null;
    private float bright = 255f;
    private boolean firing = false;
    private float fade = 255f;
    private Color FADE_COLOR;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon == null) return;
        weapon.getSpec().setNoImpactSounds(true);

        // 1. Always spin the saw blade continuously, regardless of firing or ammo state
        sawangle += amount * sawSpinRate;
        if (sawangle > 360f) sawangle -= 360f;

        // Render the weapon's own sprite on top of the original weapon every frame
        SpriteAPI weaponSprite = weapon.getSprite();
        if (weaponSprite != null) {
            MagicRender.singleframe(
                    weaponSprite,
                    weapon.getLocation(),
                    new Vector2f(weaponSprite.getWidth(), weaponSprite.getHeight()),
                    weapon.getCurrAngle(),
                    Color.WHITE,
                    false,
                    CombatEngineLayers.ABOVE_SHIPS_LAYER
            );
        }

        boolean hasAmmo = weapon.getAmmoTracker() == null || weapon.getAmmoTracker().getAmmo() > 0;

        // 2. Out of ammo block
        if (!hasAmmo) {
            weapon.setForceNoFireOneFrame(true);
            firing = false;
            // Still render the flying projectile if one was fired right before ammo ran out
            renderFlyingSaw(amount, engine);
            return;
        }

        this.Interval.advance(amount);
        boolean isFiringNow = false;

        // 3. Check for active beams when firing
        for (BeamAPI beam : weapon.getBeams()) {
            beam.getDamage().setForceHardFlux(true);
            beamend = MathUtils.getPointOnCircumference(beam.getRayEndPrevFrame(), 8f, weapon.getCurrAngle() + 180f);

            if (weapon.getAmmoTracker().getAmmo() < 1) {
                weapon.setForceNoFireOneFrame(true);
            }
            if (weapon.isFiring() && weapon.getAmmoTracker().getAmmo() > 0) {
                if (Interval.intervalElapsed()) {
                    projectile = engine.spawnProjectile(
                            beam.getSource(),
                            beam.getWeapon(),
                            "omm_rotarysaw_sub",
                            beamend,
                            beam.getWeapon().getCurrAngle(),
                            null);
                    weapon.getAmmoTracker().deductOneAmmo();
                }
            }

            isFiringNow = true;
            firing = true;
            bright = beam.getBrightness();

            // Render spinning saw at beam tip while firing
            MagicRender.singleframe(
                    sprite,
                    beamend,
                    size,
                    sawangle,
                    Color.WHITE,
                    false,
                    RENDER_LAYER
            );
        }

        // 4. If NOT firing, render the spinning saw in place under the weapon mount position
        if (!isFiringNow && weapon.getRange() > 0f && weapon.getAmmoTracker().getAmmo() > 0f) {
            Vector2f idleLoc = new Vector2f(weapon.getLocation());
            if (X_OFFSET != 0f || Y_OFFSET != 0f) {
                Vector2f offsetVec = VectorUtils.rotate(new Vector2f(X_OFFSET, Y_OFFSET), weapon.getCurrAngle());
                Vector2f.add(idleLoc, offsetVec, idleLoc);
            }
            MagicRender.singleframe(
                    sprite,
                    idleLoc,
                    size,
                    sawangle,
                    Color.WHITE,
                    false,
                    RENDER_LAYER
            );
        }

        if (weapon.getAmmoTracker().getAmmo() < 1) {
            firing = false;
        }

        // 5. Trigger release/tap flying-away projectile from the last known beamend position
        if ((!weapon.isFiring() && firing)
                || (weapon.isFiring() && firing && weapon.getAmmoTracker().getAmmo() < 2 && weapon.getAmmoTracker().getAmmo() > 0)) {
            projectile2 = engine.spawnProjectile(
                    weapon.getShip(),
                    weapon,
                    "omm_rotarysaw_sub_sub",
                    beamend,
                    weapon.getCurrAngle(),
                    null);
            weapon.getAmmoTracker().deductOneAmmo();
            firing = false;

            proj2Pos.set(beamend);
            justSpawnedProj2 = true;
            fade = 255f; // Ensures the fade tracker is fully reset on a fresh launch
        }

        renderFlyingSaw(amount, engine);
    }

    // HELPER METHOD: Handles drawing the homing missile sprite and fading it when it fizzles
    private void renderFlyingSaw(float amount, CombatEngineAPI engine) {
        if (projectile2 == null || projectile2.isExpired() || !engine.isEntityInPlay(projectile2)) {
            projectile2 = null; // Clean up
            return;
        }

        boolean exploded = false;
        boolean fadingOut = false;

        // Check DamagingProjectile logic
        if (projectile2 instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI dp = (DamagingProjectileAPI) projectile2;
            if (dp.didDamage()) exploded = true;
            if (dp.isFading()) fadingOut = true;
        }

        // Check Missile specific logic
        if (projectile2 instanceof MissileAPI) {
            MissileAPI missile = (MissileAPI) projectile2;
            if (missile.getHitpoints() <= 0f) exploded = true;
            if (missile.isFizzling()) fadingOut = true; // Triggers exactly when engines shut off
        }

        // If it hit a ship or was shot down by PD, instantly stop rendering the saw
        if (exploded) {
            projectile2 = null;
            return;
        }

        // Smooth position interpolation
        if (justSpawnedProj2) {
            proj2Pos.set(beamend);
            justSpawnedProj2 = false;
        } else {
            Vector2f targetLoc = projectile2.getLocation();
            proj2Pos.x += (targetLoc.x - proj2Pos.x) * 0.7f;
            proj2Pos.y += (targetLoc.y - proj2Pos.y) * 0.7f;
        }

        // If the missile's engine has stopped, begin standard time-based fading
        if (fadingOut) {
            fade -= (255f / FADE_TIME_SECONDS) * amount;
        } else {
            fade = 255f; // Keep completely solid while flying actively
        }

        // Only render if it hasn't completely faded out yet
        if (fade > 0) {
            FADE_COLOR = new Color(255, 255, 255, (int) Math.max(0f, Math.min(255, fade)));
            MagicRender.singleframe(
                    sprite,
                    proj2Pos,
                    size,
                    sawangle,
                    FADE_COLOR,
                    false,
                    RENDER_LAYER
            );
        }
    }
}