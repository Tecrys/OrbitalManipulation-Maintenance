package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class omm_plasmacutter_anim implements EveryFrameWeaponEffectPlugin {
    private IntervalUtil ShotInterval = new IntervalUtil(0.1f, 0.1f);
    private float timeSinceLastFrame, timeBetweenFrames = 1.5f / 60f;
    private final Map<Integer, Integer> pauseFrames = new HashMap<>();
    private int curFrame = 0, pausedFor = 0;
    private boolean hasShot = false;
    private float FireRate;
    private boolean RunOnce = false;

    // State tracking for the 0.7s stop threshold and smooth animation reversal
    private float notFiringDuration = 0f;
    private boolean isReversing = false;
    private static final float REVERSE_DELAY = 0.7f;

    private static class PendingShot {
        Vector2f from;
        float angle;
        float delay;
        public PendingShot(Vector2f from, float angle, float delay) {
            this.from = new Vector2f(from);
            this.angle = angle;
            this.delay = delay;
        }
    }
    private final List<PendingShot> pendingShots = new ArrayList<>();
    private static final float SPAWN_DELAY = 0.2f;

    protected void setFramesPerSecond(float fps) {
        timeBetweenFrames = 1.0f / fps;
    }

    protected void pauseOnFrame(int frame, int pauseFor) {
        pauseFrames.put(frame, pauseFor);
    }

    private void incFrame(AnimationAPI anim) {
        if (pauseFrames.containsKey(curFrame)) {
            if (pausedFor < pauseFrames.get(curFrame)) {
                pausedFor++;
                return;
            } else {
                pausedFor = 0;
            }
        }

        curFrame = Math.min(curFrame + 1, anim.getNumFrames() - 1);
    }

    private void decFrame(AnimationAPI anim) {
        curFrame = Math.max(curFrame - 1, 0);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getAnimation() == null) {
            return;
        }

        FireRate = 1f / weapon.getAmmoTracker().getAmmoPerSecond();

        if (!RunOnce) {
            ShotInterval = new IntervalUtil(FireRate, FireRate);
            RunOnce = true;
        }

        AnimationAPI anim = weapon.getAnimation();

        for (BeamAPI beam : weapon.getBeams()) {
            beam.getDamage().setDamage(0f);

            if (ShotInterval.intervalElapsed() && weapon.getAmmoTracker().getAmmo() > 0) {
                // Play sound immediately when the interval elapses
                Global.getSoundPlayer().playSound("omm_plasma_cutter_fire", 1f, 1f, beam.getFrom(), weapon.getShip().getVelocity());

                // Queue the projectile spawn for after the short delay
                pendingShots.add(new PendingShot(beam.getFrom(), weapon.getCurrAngle(), SPAWN_DELAY));
            }
        }

        // Restored original ammo deduction mechanism via hasShot flag
        if (hasShot) {
            weapon.getAmmoTracker().deductOneAmmo();
            hasShot = false;
        }

        // --- PROJECTILE SPAWNING & RANGE SYNC ---
        List<PendingShot> finishedShots = new ArrayList<>();
        boolean needsSpawn = false;

        for (PendingShot shot : pendingShots) {
            shot.delay -= amount;
            if (shot.delay <= 0f) {
                needsSpawn = true;
            }
        }

        if (needsSpawn) {
            WeaponSpecAPI subSpec = Global.getSettings().getWeaponSpec("omm_plasma_cutter_sub");
            if (subSpec != null) {
                float oldRange = subSpec.getMaxRange();
                try {
                    // Fetch the correct generic stat bonuses applying to the sub-weapon
                    StatBonus rangeBonus = null;
                    if (subSpec.getType() == WeaponAPI.WeaponType.ENERGY) {
                        rangeBonus = weapon.getShip().getMutableStats().getEnergyWeaponRangeBonus();
                    } else if (subSpec.getType() == WeaponAPI.WeaponType.BALLISTIC) {
                        rangeBonus = weapon.getShip().getMutableStats().getBallisticWeaponRangeBonus();
                    } else if (subSpec.getType() == WeaponAPI.WeaponType.MISSILE) {
                        rangeBonus = weapon.getShip().getMutableStats().getMissileWeaponRangeBonus();
                    }

                    // Mathematically calculate the inverse of the engine's flat and mult generic stats
                    float effectiveAtZero = rangeBonus != null ? rangeBonus.computeEffective(0f) : 0f;
                    float effectiveAtOne = rangeBonus != null ? rangeBonus.computeEffective(1f) : 1f;
                    float mult = effectiveAtOne - effectiveAtZero;

                    // Force the base range so that when the engine multiplies it out, it matches the parent beam perfectly
                    float requiredBaseRange = (weapon.getRange() - effectiveAtZero) / Math.max(0.0001f, mult);
                    subSpec.setMaxRange(requiredBaseRange);

                    for (PendingShot shot : pendingShots) {
                        if (shot.delay <= 0f) {
                            hasShot = true;
                            engine.spawnMuzzleFlashOrSmoke(weapon.getShip(),
                                    shot.from,
                                    subSpec,
                                    shot.angle
                            );
                            engine.spawnProjectile(
                                    weapon.getShip(),
                                    weapon,
                                    "omm_plasma_cutter_sub",
                                    shot.from,
                                    shot.angle,
                                    null);
                            finishedShots.add(shot);
                        }
                    }
                } finally {
                    // Always safely revert the global spec to prevent interference with other ships
                    subSpec.setMaxRange(oldRange);
                }
            }
        }
        pendingShots.removeAll(finishedShots);
        // ----------------------------------------

        if (weapon.getAmmoTracker().getAmmo() < 1) {
            weapon.setForceNoFireOneFrame(true);
        }

        // Animation state machine: forward advance when firing, reverse when out of ammo or stopped > 0.7s
        boolean isFiringCondition = weapon.isFiring() && weapon.getChargeLevel() >= 0.5f && weapon.getAmmoTracker().getAmmo() > 0;

        if (isFiringCondition) {
            notFiringDuration = 0f;
            isReversing = false;

            timeSinceLastFrame += amount;
            while (timeSinceLastFrame >= timeBetweenFrames) {
                timeSinceLastFrame -= timeBetweenFrames;
                incFrame(anim);
            }
        } else {
            // Determine if reverse should be triggered
            if (weapon.getAmmoTracker().getAmmo() < 1) {
                isReversing = true;
            } else {
                notFiringDuration += amount;
                if (notFiringDuration >= REVERSE_DELAY) {
                    isReversing = true;
                }
            }

            if (isReversing) {
                timeSinceLastFrame += amount;
                while (timeSinceLastFrame >= timeBetweenFrames) {
                    timeSinceLastFrame -= timeBetweenFrames;
                    decFrame(anim);
                }
            } else {
                // Within the 0.7s grace period after stopping, hold current frame
                timeSinceLastFrame = 0f;
            }
        }

        anim.setFrame(curFrame);

        if (weapon.isFiring()) {
            ShotInterval.advance(amount);
        }
    }
}