package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class omm_trapdoor implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {


    public Map<WeaponAPI.WeaponSize, Float> flareSize(){
        return new HashMap<WeaponAPI.WeaponSize, Float>(){{
            put(WeaponAPI.WeaponSize.LARGE, 100f);
            put(WeaponAPI.WeaponSize.MEDIUM, 60f);
            put(WeaponAPI.WeaponSize.SMALL, 30f);
        }};
    }
    public Map<WeaponAPI.WeaponSize, Float> offsets(){
        return new HashMap<WeaponAPI.WeaponSize, Float>(){{
            put(WeaponAPI.WeaponSize.LARGE, 9f);
            put(WeaponAPI.WeaponSize.MEDIUM, 6f);
            put(WeaponAPI.WeaponSize.SMALL, 3f);
        }};
    }

    public Map<WeaponAPI.WeaponSize, Float> decay(){
        return new HashMap<WeaponAPI.WeaponSize, Float>(){{
            put(WeaponAPI.WeaponSize.LARGE, 1/1.6f);
            put(WeaponAPI.WeaponSize.MEDIUM, 1/1.2f);
            put(WeaponAPI.WeaponSize.SMALL, 1/0.7f);
        }};
    }


    public Map<WeaponAPI.WeaponSize, Float> volume(){
        return new HashMap<WeaponAPI.WeaponSize, Float>(){{
            put(WeaponAPI.WeaponSize.LARGE, 1f);
            put(WeaponAPI.WeaponSize.MEDIUM, 0.6f);
            put(WeaponAPI.WeaponSize.SMALL, 0.3f);
        }};
    }

    public Map<WeaponAPI.WeaponSize, Float> pitch(){
        return new HashMap<WeaponAPI.WeaponSize, Float>(){{
            put(WeaponAPI.WeaponSize.LARGE, 1f);
            put(WeaponAPI.WeaponSize.MEDIUM, 1.4f);
            put(WeaponAPI.WeaponSize.SMALL, 2f);
        }};
    }


    private float sizeChange = 0.5f;

    private float centerCover;
    private float height;
    private boolean runOnce = false;


    private float charge = 0f;
    private float decayRate = 0f;

    private float turnrate =0f;


    public WeaponAPI weapon;

    private float prev =0f;
    public void advance(float amount, WeaponAPI weapon, float level, CombatEngineAPI engine){
    }


    @Override
    public final void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(!runOnce) {
            if (weapon.getBarrelSpriteAPI() != null) {
                centerCover = weapon.getBarrelSpriteAPI().getCenterY();
            height = weapon.getBarrelSpriteAPI().getHeight();
            decayRate = 1f / weapon.getCooldown();
            turnrate = weapon.getTurnRate();
            this.weapon = weapon;
            runOnce = true;
        }
        }


        if(weapon.getSlot().isHidden())return;
        if(engine.isPaused())return;
        if(weapon.isDisabled())return;
        if(!weapon.getShip().isAlive()){
            //weapon.getAnimation().setFrameRate(0);
            return;
        }
        if (weapon.getBarrelSpriteAPI() == null) return;
        weapon.setTurnRateOverride(0.1f*turnrate + charge*0.9f*turnrate);
        float l2 = 1-(1-charge)*(1-charge);
        float o1 = offsets().get(weapon.getSize());
        SpriteAPI s1 = weapon.getBarrelSpriteAPI();
        SpriteAPI s2 = weapon.getSprite();
        s1.setCenterY(centerCover+o1*l2);
        //s2.setCenterY(centerBase-o1*l2);
        s1.setHeight(height*((1-sizeChange) + sizeChange*(1-charge)  ));

        float c1 = 0.2f;
        int c2 = (int)(255*((1f-c1)+(c1*(1f-charge))));
        Color c3 = new Color(c2,c2,c2,255);
        s1.setColor(c3);


        advance(amount, weapon, charge, engine);



        if(isTargetValid(weapon)){
            if(charge<1f){
                charge+=decay().get(weapon.getSize())*amount;
            }else {charge=1f;}
        }else {
            if(charge>0){
                charge-=decay().get(weapon.getSize())*amount;
            }else {charge=0f;}
            if(charge-decay().get(weapon.getSize())*amount<0){
                charge=0f;
            }


            float a1 = weapon.getSlot().getAngle()+weapon.getShip().getFacing();
            float a2 = weapon.getCurrAngle();
            float dif = MathUtils.getShortestRotation(a2, a1);
            float step = amount*weapon.getTurnRate();
            float dir = Math.signum(dif);
            float a3 = a2+dir*step;

            if(Math.abs(dif)<=step){
                a3 = a1;
            }
            if(charge<=0){
                //a3 = a1;
            }
            weapon.setCurrAngle(a3);
        }

        if(charge<0.9f){
            weapon.setForceNoFireOneFrame(true);
        }



    }
    private static boolean isTargetValid(WeaponAPI weapon){

        float minArc =30f;
        ShipAPI ship = weapon.getShip();
        boolean result = false;
        Vector2f targetLoc = null;
        Vector2f mouseTarget = ship.getMouseTarget();

        WeaponGroupAPI grp = ship.getWeaponGroupFor(weapon);

        if(mouseTarget!=null && grp!=null && grp == weapon.getShip().getSelectedGroupAPI()){
            targetLoc=mouseTarget;

        }
        if (grp!=null && grp != weapon.getShip().getSelectedGroupAPI()){
            AutofireAIPlugin aiplug = grp.getAutofirePlugin(weapon);
            if(aiplug!=null){
                Vector2f aiLoc = aiplug.getTarget();
                if(aiLoc!=null){
                    targetLoc=aiLoc;
                }
            }
        }
        if(targetLoc!=null){
            float targetAngle = VectorUtils.getAngle(weapon.getLocation(), targetLoc);
            float dif = MathUtils.getShortestRotation(weapon.getArcFacing()+ship.getFacing(), targetAngle);
            float arc = weapon.getSlot().getArc();
            if(arc<minArc){arc=minArc;}
            if(Math.abs(dif)<=arc/2f && MathUtils.getDistance(weapon.getLocation(), targetLoc)<=weapon.getRange()){
                result = true;
            }
        }


        return result;
    }


    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {

    }
}
