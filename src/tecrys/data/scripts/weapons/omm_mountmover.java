package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

import java.util.List;
import java.util.Objects;

public class omm_mountmover implements EveryFrameWeaponEffectPlugin {

    ShipAPI Substation = null;
    String wID = null;
    String sID = null;

    ShipAPI bottom_left_algae = null;
    WeaponSlotAPI wbottom_left_algae = null;
    ShipAPI bottom_right_algae = null;
    WeaponSlotAPI wbottom_right_algae = null;
    ShipAPI bottom = null;
    WeaponSlotAPI wbottom = null;
    ShipAPI right = null;
    WeaponSlotAPI wright = null;
    ShipAPI left = null;
    WeaponSlotAPI wleft = null;
    ShipAPI right_algae = null;
    WeaponSlotAPI wright_algae = null;
    ShipAPI left_algae = null;
    WeaponSlotAPI wleft_algae = null;


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }


            ShipAPI ship = weapon.getShip();
        ship.ensureClonedStationSlotSpec();
        List<ShipAPI> modules = ship.getChildModulesCopy();


        if(modules.equals(null) || modules.isEmpty())
        {
            return;
        }


        for (ShipAPI s : modules) {
            s.ensureClonedStationSlotSpec();

            if(s.equals(null)){
                continue;
            }
            if (s.getStationSlot().getId().equals("WS 000")) Substation = s;

            if (s.getStationSlot().getId().equals("bottom_left_algae")) bottom_left_algae = s;

            if (s.getStationSlot().getId().equals("bottom_right_algae")) bottom_right_algae = s;

            if (s.getStationSlot().getId().equals("bottom")) bottom = s;

            if (s.getStationSlot().getId().equals("right")) right = s;

            if (s.getStationSlot().getId().equals("left")) left = s;

            if (s.getStationSlot().getId().equals("right_algae")) right_algae = s;

            if (s.getStationSlot().getId().equals("left_algae")) left_algae = s;

            List<WeaponSlotAPI> weaponslots = Substation.getHullSpec().getAllWeaponSlotsCopy();




            if(weaponslots.equals(null) || weaponslots.isEmpty())
            {
                return;
            }

            for (WeaponSlotAPI w : weaponslots) {
                wID = w.getId();
                sID = s.getStationSlot().getId();

if (bottom_left_algae != null && bottom_left_algae.getStationSlot().getId().equals(wID)) {
wbottom_left_algae = w;
if (w != null) {
    bottom_left_algae.getLocation().set(wbottom_left_algae.getLocation());
    bottom_left_algae.setFacing(wbottom_left_algae.getAngle());
}}
                if (bottom_right_algae != null && Objects.equals(bottom_right_algae.getStationSlot().getId(), wID)) {
                    wbottom_right_algae = w;
                    if (w != null) {
                    bottom_right_algae.getLocation().set(w.getLocation());
                    bottom_right_algae.setFacing(w.getAngle());
                }}
                if (bottom != null && Objects.equals(bottom.getStationSlot().getId(), wID)) {
                    wbottom = w;
                    if (w != null) {
                    bottom.getLocation().set(w.getLocation());
                    bottom.setFacing(w.getAngle());
                }}
                if (right != null && Objects.equals(right.getStationSlot().getId(), wID)) {
                    wright = w;
                    if (w != null) {
                    right.getLocation().set(w.getLocation());
                    right.setFacing(w.getAngle());
                }}
                if (left != null && Objects.equals(left.getStationSlot().getId(), wID)) {
                    wleft = w;
                    if (w != null) {
                    left.getLocation().set(w.getLocation());
                    left.setFacing(w.getAngle());
                }}
                if (right_algae != null && Objects.equals(right_algae.getStationSlot().getId(), wID)) {
                    wright_algae = w;
                    if (w != null) {
                    right_algae.getLocation().set(w.getLocation());
                    right_algae.setFacing(w.getAngle());
                }}
                if (left_algae != null && Objects.equals(left_algae.getStationSlot().getId(), wID)) {
                    wleft_algae = w;
                    if (w != null) {
                    left_algae.getLocation().set(w.getLocation());
                    left_algae.setFacing(w.getAngle());
                }}
            }

        }



    }
    
}
