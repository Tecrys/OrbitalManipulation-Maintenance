package tecrys.data.scripts.weapons;


import com.fs.starfarer.api.combat.*;




public class omm_biosphere implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused() || weapon.getShip() == null || weapon.getShip().getOriginalOwner() == -1) return;

        weapon.setCurrAngle(weapon.getShip().getFacing()- weapon.getShip().getFacing()+90f);
    }
}



