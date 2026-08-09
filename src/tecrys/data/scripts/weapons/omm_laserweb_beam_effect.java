package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;

public class omm_laserweb_beam_effect implements BeamEffectPlugin {

    private IntervalUtil DamageInterval = new IntervalUtil(0.1f, 0.2f);
    private boolean wasZero = true;


    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {

        CombatEntityAPI target = beam.getDamageTarget();
        if (((target instanceof ShipAPI)
        || target instanceof MissileAPI)
        ) {         this.DamageInterval.advance(amount);
            if (DamageInterval.intervalElapsed()) {
                Global.getCombatEngine().applyDamage(target, target.getLocation(), beam.getDamage().getDamage()/1, DamageType.ENERGY, (beam.getDamage().getDamage() / 3f)/1, false, false, beam.getWeapon());
            }
        }
//			Global.getSoundPlayer().playLoop("system_emp_emitter_loop",
//											 beam.getDamageTarget(), 1.5f, beam.getBrightness() * 0.5f,
//											 beam.getTo(), new Vector2f());
   }
}
