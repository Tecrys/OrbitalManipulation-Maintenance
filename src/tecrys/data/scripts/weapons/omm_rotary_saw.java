package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;

public class omm_rotary_saw implements EveryFrameWeaponEffectPlugin {

    private SpriteAPI sprite = Global.getSettings().getSprite("misc", "omm_rotarysaw"); // sprite itself
    // probably ok to put this here as we only render one at a time per beam anyway
    private Vector2f size = new Vector2f(sprite.getWidth(), sprite.getHeight());
    private CombatEntityAPI projectile = null;
    private IntervalUtil Interval = new IntervalUtil(0.0f, 0.1f);
    private Vector2f beamend;
    private float sawangle; // angle to render sprite att
    private float sawSpinRate = 3800f; // deg/sec saw spins at
    private CombatEntityAPI projectile2 = null;
    private float bright = 255;
    private boolean firing = false;
    private float fade = 255;
    private Color FADE_COLOR;


    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        sawangle += amount * sawSpinRate; //
        if (sawangle > 360f) sawangle = 0f;
        this.Interval.advance(amount);
        for (BeamAPI beam : weapon.getBeams()) {
//        beamend =    Misc.interpolateVector(beam.getWeapon().getLocation(), beam.getRayEndPrevFrame(), 0.95f);
            beamend = MathUtils.getPointOnCircumference(beam.getRayEndPrevFrame(), 8,weapon.getCurrAngle()+180);

//            beamend = beam.getRayEndPrevFrame();
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

            MagicRender.singleframe(sprite, // spriteapi
                    beamend, // beam ray end
                    size, // sprite dimensions
                    sawangle, // current angle to render sprite at
                    Color.WHITE,
                    false,
                    CombatEngineLayers.BELOW_SHIPS_LAYER); // layer to render sprite on
            firing = true;
            bright = beam.getBrightness();}
        }
        if (weapon.getAmmoTracker().getAmmo() < 1) {firing = false;}
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
        }

        if (projectile2 != null && !projectile2.isExpired()) {
            fade -= (int) amount ^ 2;
            FADE_COLOR = new Color(255, 255, 255, (int) Math.max(0f, Math.min(255, fade)));
            MagicRender.singleframe(sprite, // spriteapi
                    projectile2.getLocation(), // beam ray end
                    size, // sprite dimensions
                    sawangle, // current angle to render sprite at
                    Color.WHITE,
                    false,
                    CombatEngineLayers.BELOW_SHIPS_LAYER); // layer to render sprite on}


        }
    }
}