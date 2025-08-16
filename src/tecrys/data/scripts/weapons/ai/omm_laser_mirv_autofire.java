package tecrys.data.scripts.weapons.ai;


import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class omm_laser_mirv_autofire implements AutofireAIPlugin {
    @Override
    public void advance(float amount) {

    }
    private Vector2f target;
    private WeaponAPI weapon;

    public void setTarget(Vector2f target) {
        this.target = target;
    }

    @Override
    public boolean shouldFire() {
        return target!=null;
    }

    @Override
    public void forceOff() {

    }

    @Override
    public Vector2f getTarget() {
        try{
            Float dir = VectorUtils.getAngle(weapon.getLocation(), target);
            weapon.setCurrAngle(dir);
        }catch (Exception ex){}
        return target;
    }

    @Override
    public ShipAPI getTargetShip() {
        return null;
    }

    @Override
    public WeaponAPI getWeapon() {
        return weapon;
    }

    @Override
    public MissileAPI getTargetMissile() {
        return null;
    }

    public omm_laser_mirv_autofire(WeaponAPI weapon) {
        this.weapon = weapon;
    }
}