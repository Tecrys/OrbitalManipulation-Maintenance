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

    // --- CONFIGURABLE IDLE SAW POSITION ---
    // Forward / Backward offset from the weapon mount when idle (positive = further out down barrel)
    private static final float X_OFFSET = 15f;
    // Left / Right offset across the weapon mount when idle
    private static final float Y_OFFSET = 0f;

    private float sawSpinRate = 3800f; // deg/sec saw spins at
    private static final CombatEngineLayers RENDER_LAYER = CombatEngineLayers.CONTRAILS_LAYER;
    // -------------------------------------

    private SpriteAPI sprite = Global.getSettings().getSprite("misc", "omm_rotarysaw");
    private Vector2f size = new Vector2f(sprite.getWidth(), sprite.getHeight());
    private CombatEntityAPI projectile = null;
    private IntervalUtil Interval = new IntervalUtil(0.0f, 0.1f);
    private Vector2f beamend = new Vector2f();
    private Vector2f proj2Pos = new Vector2f(); // Smooth interpolation tracker for projectile2
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

        // When out of ammo, completely block shooting and weapon saw rendering,
        // allowing any active flying-away projectile2 to finish its animation cleanly.
        if (!hasAmmo) {
            weapon.setForceNoFireOneFrame(true);
            firing = false;

            if (projectile2 != null && !projectile2.isExpired()) {
                sawangle += amount * sawSpinRate;
                if (sawangle > 360f) sawangle -= 360f;

                if (justSpawnedProj2) {
                    proj2Pos.set(beamend);
                    justSpawnedProj2 = false;
                } else {
                    Vector2f targetLoc = projectile2.getLocation();
                    proj2Pos.x += (targetLoc.x - proj2Pos.x) * 0.7f;
                    proj2Pos.y += (targetLoc.y - proj2Pos.y) * 0.7f;
                }

                fade -= (int) amount ^ 2;
                FADE_COLOR = new Color(255, 255, 255, (int) Math.max(0f, Math.min(255, fade)));
                MagicRender.singleframe(
                        sprite,
                        proj2Pos,
                        size,
                        sawangle,
                        Color.WHITE,
                        false,
                        RENDER_LAYER
                );
            }
            return;
        }

        // 1. Always spin the saw blade continuously
        sawangle += amount * sawSpinRate;
        if (sawangle > 360f) sawangle -= 360f;

        this.Interval.advance(amount);

        boolean isFiringNow = false;

        // 2. Check for active beams when firing
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

        // 3. If NOT firing, render the spinning saw in place under the weapon mount position
        if (!isFiringNow) {
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

        // 4. Trigger release/tap flying-away projectile from the last known beamend position
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
        }

        if (projectile2 != null && !projectile2.isExpired()) {
            if (justSpawnedProj2) {
                proj2Pos.set(beamend);
                justSpawnedProj2 = false;
            } else {
                Vector2f targetLoc = projectile2.getLocation();
                proj2Pos.x += (targetLoc.x - proj2Pos.x) * 0.7f;
                proj2Pos.y += (targetLoc.y - proj2Pos.y) * 0.7f;
            }

            fade -= (int) amount ^ 2;
            FADE_COLOR = new Color(255, 255, 255, (int) Math.max(0f, Math.min(255, fade)));
            MagicRender.singleframe(
                    sprite,
                    proj2Pos,
                    size,
                    sawangle,
                    Color.WHITE,
                    false,
                    RENDER_LAYER
            );
        }
    }
}