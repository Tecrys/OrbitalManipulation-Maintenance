package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class omm_rotary_saw_sub implements OnHitEffectPlugin {

    private SpriteAPI sprite = Global.getSettings().getSprite("misc", "omm_rotarysaw"); // sprite itself
    // probably ok to put this here as we only render one at a time per beam anyway
    private Vector2f size = new Vector2f(sprite.getWidth(), sprite.getHeight());
    private CombatEntityAPI projectile = null;
    private IntervalUtil Interval = new IntervalUtil(0.f, 0.01f);
    private Vector2f beamend;
    private float sawangle; // angle to render sprite att
    private float sawSpinRate = 3800f; // deg/sec saw spins at
    private CombatEntityAPI projectile2 = null;
    private float bright = 255;
    private boolean firing = false;
    private float fade = 255;
    private  Color FADE_COLOR;

    @Override
    public void onHit(DamagingProjectileAPI saw, CombatEntityAPI combatEntityAPI, Vector2f vector2f, boolean shieldHit, ApplyDamageResultAPI applyDamageResultAPI, CombatEngineAPI engine) {
      if  (saw.didDamage() && !shieldHit) {

//saw.getProjectileSpec().set
          ;engine.spawnMuzzleFlashOrSmoke(saw.getSource(),
                 CollisionUtils.getNearestPointOnBounds(saw.getLocation(),saw.getDamageTarget()),
//            saw.getLocation(),
            Global.getSettings().getWeaponSpec("omm_saw_flak"),
                  VectorUtils.getAngle(saw.getLocation(),saw.getDamageTarget().getLocation()) +110
//                  saw.getFacing()+70
          );}
          if  (shieldHit) {

//saw.getProjectileSpec().set
//              ;engine.spawnMuzzleFlashOrSmoke(saw.getSource(),
//                      saw.getLocation(),
////            saw.getLocation(),
//                      Global.getSettings().getWeaponSpec("omm_rotarysaw_sub_sub"),
//                      VectorUtils.getAngle(saw.getLocation(),saw.getDamageTarget().getLocation()) +70
////                  saw.getFacing()+70
//              );
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
   ;
//          engine.spawnProjectile(
//                  null,
//                  null,
//                  "omm_saw_flak",
//                  CollisionUtils.getNearestPointOnBounds(saw.getLocation(),saw.getDamageTarget()),
//                  0f,
//                  null);
      }
    }
}