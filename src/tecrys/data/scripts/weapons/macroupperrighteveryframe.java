package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class macroupperrighteveryframe implements EveryFrameWeaponEffectPlugin {

    float index = 0f;
    IntervalUtil interval = new IntervalUtil(0.01f, 0.01f);
    ShipAPI target = null;
    MissileAPI missiletarget = null;
    int grabindex = 0;

    private static org.apache.log4j.Logger log = Global.getLogger(macroupperrighteveryframe.class);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();

        WeaponAPI arm = null;

        WeaponAPI forearm = null;

        WeaponAPI hand = null;

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("omm_macro_shoulder_right")) {
                arm = w;
            }

            if (w.getSlot().getId().equals("omm_macro_arm_right")) {
                forearm = w;
            }

            if (w.getSlot().getId().equals("omm_macro_hand_right")) {
                hand = w;
            }
        }
        if (ship.isHulk() || !ship.isAlive()){
            return;
        }
        interval.advance(amount);

        if(interval.intervalElapsed() && arm.getSlot() != null && forearm.getSlot() != null && hand.getSlot() != null) {
            Vector2f targloc = null;

            Vector2f armabsloc = VectorUtils.rotateAroundPivot(new Vector2f(arm.getSlot().getLocation().getX() + ship.getLocation().getX(), arm.getSlot().getLocation().getY() + ship.getLocation().getY()), ship.getLocation(), ship.getFacing());
            if(target != null){
                if(MathUtils.getDistance(target.getLocation(),armabsloc)>280f){
                    target = null;
                }else{
                    targloc = target.getLocation();
                }
            }else{
                if (AIUtils.getNearestEnemy(ship) != null) {
                    for(ShipAPI e:AIUtils.getNearbyEnemies(ship,1000f)){
                        if(e.isFighter() && MathUtils.getDistance(e.getLocation(),armabsloc)<280f){
                            targloc = e.getLocation();
                            target = e;
                        }
                    }
                }
                if (AIUtils.getNearbyEnemyMissiles(ship, index) != null) {
                    for (MissileAPI m : AIUtils.getNearbyEnemyMissiles(ship, 1000f)) {
                        if (m.isArmed() && MathUtils.getDistance(m.getLocation(), armabsloc) < 280f) {
                            targloc = m.getLocation();
                            missiletarget = m;
                        }
                    }
                }
            }


            if(targloc == null) {
                if (arm != null) {

                    float dangle = (3600f - 49.5f + ship.getFacing()) % 360f;
                    float currangle = (3600f + arm.getCurrAngle()) % 360f;

                    if (MathUtils.getShortestRotation(currangle + 3f, dangle) > 0f) {
                        arm.setCurrAngle(currangle + 3f);
                    } else if (MathUtils.getShortestRotation(currangle - 3f, dangle) < 0f) {
                        arm.setCurrAngle(currangle - 3f);
                    }

                }

                if (forearm != null) {
                    forearm.getSlot().getLocation().set(VectorUtils.rotateAroundPivot(new Vector2f(arm.getSlot().getLocation().getX() + 110f, arm.getSlot().getLocation().getY()), arm.getSlot().getLocation(), arm.getCurrAngle() - ship.getFacing()));

                    float dangle = (3600f + 22.5f + ship.getFacing()) % 360f;
                    float currangle = (3600f + forearm.getCurrAngle()) % 360f;

                    if (MathUtils.getShortestRotation(currangle + 3f, dangle) > 0f) {
                        forearm.setCurrAngle(currangle + 3f);
                    } else if (MathUtils.getShortestRotation(currangle - 3f, dangle) < 0f) {
                        forearm.setCurrAngle(currangle - 3f);
                    }
                }

            } else {

                if (arm != null) {
                    Vector2f slotabsloc = VectorUtils.rotateAroundPivot(new Vector2f(arm.getSlot().getLocation().getX() + ship.getLocation().getX(), arm.getSlot().getLocation().getY() + ship.getLocation().getY()), ship.getLocation(), ship.getFacing());
                    float dist = MathUtils.clamp(MathUtils.getDistance(slotabsloc, targloc), 0f, 220f) * 0.5f;

                    float targangle = (float) (Math.asin((MathUtils.clamp(dist, 0f, 106f) / 106f))) * 57.2958f;

                    float dangle = (3600f + MathUtils.clamp(VectorUtils.getAngle(slotabsloc, targloc) + 90f - targangle, -10f + ship.getFacing(), 110f + ship.getFacing())) % 360f;
                    float currangle = (3600f + arm.getCurrAngle()) % 360f;

                    if (MathUtils.getShortestRotation(currangle + 3f, dangle) > 0f) {
                        arm.setCurrAngle(currangle + 3f);
                    } else if (MathUtils.getShortestRotation(currangle - 3f, dangle) < 0f) {
                        arm.setCurrAngle(currangle - 3f);
                    }

                }

                if (forearm != null) {
                    forearm.getSlot().getLocation().set(VectorUtils.rotateAroundPivot(new Vector2f(arm.getSlot().getLocation().getX() + 110f, arm.getSlot().getLocation().getY()), arm.getSlot().getLocation(), arm.getCurrAngle() - ship.getFacing()));

                    //float offset = 0f;

                    Vector2f slotabsloc = VectorUtils.rotateAroundPivot(new Vector2f(forearm.getSlot().getLocation().getX() + ship.getLocation().getX(), forearm.getSlot().getLocation().getY() + ship.getLocation().getY()), ship.getLocation(), ship.getFacing());
                    float dist = MathUtils.clamp(MathUtils.getDistance(slotabsloc, targloc), 0f, 220f) * 0.5f;

                    float targangle = (float) (Math.asin((MathUtils.clamp(dist, 0f, 110f) / 110f))) * 57.2958f;

                    float dangle = VectorUtils.getAngle(slotabsloc, targloc);
                    float currangle = forearm.getCurrAngle();

                    if (MathUtils.getShortestRotation(currangle + 3f, dangle) > 0f) {
                        forearm.setCurrAngle(currangle + 3f);
                    } else if (MathUtils.getShortestRotation(currangle - 3f, dangle) < 0f) {
                        forearm.setCurrAngle(currangle - 3f);
                    }
                }

            }
            if (hand != null) {
                hand.getSlot().getLocation().set(VectorUtils.rotateAroundPivot(new Vector2f(forearm.getSlot().getLocation().getX() + 110f, forearm.getSlot().getLocation().getY() - 4f), forearm.getSlot().getLocation(), forearm.getCurrAngle() - ship.getFacing()));

                Vector2f slotabsloc = VectorUtils.rotateAroundPivot(new Vector2f(hand.getSlot().getLocation().getX() + ship.getLocation().getX(), hand.getSlot().getLocation().getY() + ship.getLocation().getY()), ship.getLocation(), ship.getFacing());
if(target != null){
                if(MathUtils.getDistance(slotabsloc,target.getLocation())<10f){
                    while (target.getHullLevel()>0){
                        engine.applyDamage(target, target.getLocation(), 100f, DamageType.FRAGMENTATION, 0, true, false, ship);
                    }
                    for (int i = 0; i < 10; i++) {
                        target.splitShip();
                    }
                    target=null;
                }
                //float offset = 0f;

                hand.setCurrAngle(forearm.getCurrAngle());
            }
if(missiletarget != null){
                if(MathUtils.getDistance(slotabsloc,missiletarget.getLocation())<10f){
                    while (missiletarget.getHullLevel()>0){
                        engine.applyDamage(missiletarget, missiletarget.getLocation(), 100f, DamageType.FRAGMENTATION, 0, true, false, ship);
                    }
                    for (int i = 0; i < 10; i++) {
                        missiletarget.explode();
                    }
                    missiletarget=null;
                }
                //float offset = 0f;

                hand.setCurrAngle(forearm.getCurrAngle());
            }
            }
        }
    }
}

//if (arm != null) {
//                //float offset = 0f;
//
//                Vector2f slotabsloc = VectorUtils.rotateAroundPivot(new Vector2f(arm.getSlot().getLocation().getX()+ship.getLocation().getX(), arm.getSlot().getLocation().getY()+ship.getLocation().getY()),ship.getLocation(), ship.getFacing());
//                float dist = MathUtils.clamp(MathUtils.getDistance(slotabsloc,targloc),0f,225f)*0.5f;
//
//                float targangle = (float) (Math.asin((MathUtils.clamp(dist,0f,106f)/106f)))*57.2958f;
//
//                float dangle = (3600f+MathUtils.clamp(VectorUtils.getAngle(slotabsloc,targloc)+90f - targangle,-10f+ship.getFacing(), 110f+ship.getFacing()))%360f;
//                float currangle =  (3600f+arm.getCurrAngle())%360f;
//
//                if(MathUtils.getShortestRotation(currangle+3f,dangle)>0f){
//                    arm.setCurrAngle(currangle+3f);
//                }else if(MathUtils.getShortestRotation(currangle-3f,dangle)<0f){
//                    arm.setCurrAngle(currangle-3f);
//                }
//
//            }
//
//            if (forearm != null) {
//                forearm.getSlot().getLocation().set(VectorUtils.rotateAroundPivot(new Vector2f(arm.getSlot().getLocation().getX()+106f, arm.getSlot().getLocation().getY()), arm.getSlot().getLocation(), arm.getCurrAngle() - ship.getFacing()));
//
//                //float offset = 0f;
//
//                Vector2f slotabsloc = VectorUtils.rotateAroundPivot(new Vector2f(arm.getSlot().getLocation().getX()+ship.getLocation().getX(), arm.getSlot().getLocation().getY()+ship.getLocation().getY()),ship.getLocation(), ship.getFacing());
//                float dist = MathUtils.clamp(MathUtils.getDistance(slotabsloc,targloc),0f,225f)*0.5f;
//
//                float targangle = (float) (Math.asin((MathUtils.clamp(dist,0f,119f)/119f)))*57.2958f;
//
//                float dangle = (3600f+VectorUtils.getAngle(slotabsloc,targloc)+90f - targangle)%360f;
//                float currangle =  (3600f+forearm.getCurrAngle())%360f;
//
//                if(MathUtils.getShortestRotation(currangle+3f,dangle)>0f){
//                    forearm.setCurrAngle(currangle+3f);
//                }else if(MathUtils.getShortestRotation(currangle-3f,dangle)<0f){
//                    forearm.setCurrAngle(currangle-3f);
//                }
//            }
//
//            if (hand != null) {
//                hand.getSlot().getLocation().set(VectorUtils.rotateAroundPivot(new Vector2f(forearm.getSlot().getLocation().getX() + 120f, forearm.getSlot().getLocation().getY() - 4f), forearm.getSlot().getLocation(), forearm.getCurrAngle() - ship.getFacing()));
//
//                //float offset = 0f;
//
//                hand.setCurrAngle(forearm.getCurrAngle());
//            }