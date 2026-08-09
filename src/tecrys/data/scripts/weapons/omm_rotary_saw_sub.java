package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;

public class omm_rotary_saw_sub implements OnHitEffectPlugin {

    private SpriteAPI sprite = Global.getSettings().getSprite("misc", "omm_rotarysaw");
    private Vector2f size = new Vector2f(sprite.getWidth(), sprite.getHeight());

    @Override
    public void onHit(DamagingProjectileAPI saw, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI applyDamageResultAPI, CombatEngineAPI engine) {
        if (engine == null || target == null) return;
        saw.getProjectileSpec().setNoImpactSounds(true);
        saw.getWeapon().getSpec().setNoImpactSounds(true);
        // 1. Determine looping sound ID based on shield vs armor/hull hit
        // (Make sure to define these sound IDs in your mod's sounds.json)
        String soundId = shieldHit ? "omm_saw_shield_loop" : "omm_saw_armor_loop";

        // 2. Add a short-lived every-frame plugin to sustain the playLoop for 0.4 seconds
        engine.addPlugin(new SawHitLoopSoundPlugin(target, soundId, 0.4f));

        if (saw.didDamage() && !shieldHit) {
            engine.spawnMuzzleFlashOrSmoke(
                    saw.getSource(),
                    CollisionUtils.getNearestPointOnBounds(saw.getLocation(), target),
                    Global.getSettings().getWeaponSpec("omm_saw_flak"),
                    VectorUtils.getAngle(saw.getLocation(), target.getLocation()) + 110f
            );
        }

        if (shieldHit) {
            engine.addSwirlyNebulaParticle(
                    saw.getLocation(),
                    new Vector2f(),
                    13,
                    5,
                    3,
                    1000,
                    0.3f,
                    new Color(39, 245, 241, 210),
                    false
            );
        }
    }

    // Helper inner plugin to sustain the playLoop every frame for the specified duration
    private static class SawHitLoopSoundPlugin extends BaseEveryFrameCombatPlugin {
        private final CombatEntityAPI target;
        private final String soundId;
        private float duration;

        public SawHitLoopSoundPlugin(CombatEntityAPI target, String soundId, float duration) {
            this.target = target;
            this.soundId = soundId;
            this.duration = duration;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            duration -= amount;
            if (duration <= 0f || target == null || !Global.getCombatEngine().isEntityInPlay(target)) {
                Global.getCombatEngine().removePlugin(this);
                return;
            }

            // Play loop every frame to sustain the sound smoothly
            Global.getSoundPlayer().playLoop(
                    soundId,
                    target,
                    1.0f, // Pitch
                    1.0f, // Volume
                    target.getLocation(),
                    target.getVelocity()
            );
        }
    }
}