package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.HashMap;
import java.util.Map;

public class omm_plasmacutter_anim implements EveryFrameWeaponEffectPlugin {
    // Default to 15 frames per second
    private IntervalUtil ShotInterval = null;
    private IntervalUtil ReloadWait = new IntervalUtil(0.1f, 0.1f);
    private float timeSinceLastFrame, timeBetweenFrames = 1.5f / 60f;
    private final Map pauseFrames = new HashMap();
    private int curFrame = 0, pausedFor = 0;
    private boolean isFiring = false;
    private boolean hasShot = false;
    private int curFramerewind = 0;
    private float beamlength = 0;
    private float FireRate;
    private boolean RunOnce = false;

    protected void setFramesPerSecond(float fps) {
        timeBetweenFrames = 1.0f / fps;
    }

    protected void pauseOnFrame(int frame, int pauseFor) {
        pauseFrames.put(frame, pauseFor);
    }

    private void incFrame(AnimationAPI anim) {
        if (pauseFrames.containsKey(curFrame)) {
            if (pausedFor < (Integer) pauseFrames.get(curFrame)) {
                pausedFor++;
                return;
            } else {
                pausedFor = 0;
            }
        }

        curFrame = Math.min(curFrame + 1, anim.getNumFrames() - 1);
        curFramerewind = Math.max(curFrame - 1, 0);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getAnimation() == null) {
            return;
        }

        FireRate = weapon.getAmmoTracker().getAmmoPerSecond();

        AnimationAPI anim = weapon.getAnimation();
        anim.setFrame(curFrame);
        for (BeamAPI beam : weapon.getBeams()) {

            if (this.ShotInterval.intervalElapsed() && weapon.getAmmoTracker().getAmmo() > 0) {

                engine.spawnProjectile(
                        weapon.getShip(),
                        weapon,
                        "miningblaster",
                        beam.getFrom(),
                        weapon.getCurrAngle(),
                        null);
//    isReadyToFire = false;
                hasShot = true;
            }
        }

        if (hasShot) {

            weapon.getAmmoTracker().deductOneAmmo();
            hasShot = false;
        }

        if (weapon.getAmmoTracker().getAmmo() < 1) {
            weapon.setForceNoFireOneFrame(true);
        }
        if (isFiring && weapon.getAmmoTracker().getAmmo() > 0) {
            timeSinceLastFrame += amount;

            while (timeSinceLastFrame >= timeBetweenFrames) {
                timeSinceLastFrame -= timeBetweenFrames;
                incFrame(anim);
            }

            anim.setFrame(curFrame);
            if (curFrame == anim.getNumFrames() - 1) {
                isFiring = false;
            }
        }
        if (weapon.isFiring()) {

            if (!RunOnce) {
                this.ShotInterval = new IntervalUtil(FireRate, FireRate);
                RunOnce = true;
            }
            this.ShotInterval.advance(amount);

        }
        if (weapon.isFiring() && weapon.getChargeLevel() >= 0.5f
        ) {
            isFiring = true;
            incFrame(anim);
            anim.setFrame(curFrame);
        } else {
            curFrame = 0;
            anim.setFrame(curFrame);
        }

    }
}