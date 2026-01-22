package tecrys.data.scripts.weapons;


import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;


public class omm_shadowturner implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused() || weapon.getShip() == null  || weapon.getShip().getOriginalOwner() == -1) return;

        weapon.setCurrAngle((weapon.getShip().getParentStation().getFacing()));
    }
}



