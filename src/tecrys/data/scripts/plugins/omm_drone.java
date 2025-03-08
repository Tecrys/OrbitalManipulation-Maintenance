package tecrys.data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE;

public class omm_drone implements EveryFrameCombatPlugin {

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void init(CombatEngineAPI engine) {
        droneWeaponPerShip = new HashMap<>();
    }

    private Map<ShipAPI, WeaponAPI> droneWeaponPerShip = new HashMap<>();

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) {
            return;
        }

        for (ShipAPI ship : engine.getShips()) {

                if (ship != null && ship.isAlive()) {

                    boolean canEndCombat = true; //default to assuming combat will end
                    for (ShipAPI other : engine.getShips()) {
                        if (other.getOwner() != ship.getOwner()) continue; //only check allies
                        if (other.getHullSpec().getHullId().equals("omm_octocorallia") || other.getHullSpec().getHullId().equals("omm_algaepod")
                                || other.getHullSpec().getHullId().equals("omm_nanodrone") || other.getHullSpec().getHullId().equals("FreitagCorporation_ShieldDrone")
                                || other.getHullSpec().getHullId().equals("omm_pddrone") || other.getHullSpec().getHullId().equals("omm_weaponpod")
                                || other.getHullSpec().getHullId().equals("omm_synergypod") || other.getHullSpec().getHullId().equals("omm_missilepod")
                                || other.getHullSpec().getHullId().equals("omm_medmissilepod") || other.getHullSpec().getHullId().equals("omm_compositepod")
                                || other.getHullSpec().getHullId().equals("omm_smallpod") || other.getHullSpec().getHullId().equals("omm_ballisticpod")
                                || other.getHullSpec().getHullId().equals("omm_largepod"))
                            continue; //if it's the same hull id, ignore it
                        canEndCombat = false; //else, combat shouldn't end
                    }

                    if (canEndCombat) {
                        engine.endCombat(1);
                    } //end combat
                }
            for (FighterWingAPI fighterWing : ship.getAllWings()){
            for (ShipAPI WingMembers : fighterWing.getWingMembers()) {
                if (ship.getHullLevel() >= 10) {
                    engine.applyDamage(WingMembers, WingMembers.getLocation(), 1000, DamageType.ENERGY, 0, true, false, ship);
                }}}
            if (!ship.isAlive() || !ship.getHullSpec().getHullId().startsWith("omm_")) {
                continue;
            }
            if (ship.getOriginalOwner() == 1) {
                ship.getFleetMember().getVariant().getHints().add(UNBOARDABLE);
            } else {
                ship.getFleetMember().getVariant().getHints().remove(UNBOARDABLE);
            }

            for (WeaponAPI weap : ship.getAllWeapons()) {
                if (ship.isFighter()) {
                    break;
                }
                if (!droneWeaponPerShip.containsKey(ship)) {
                    droneWeaponPerShip.put(ship, weap);
                }                    //WEAPON interface for drones
                if (weap.getId().equals("omm_weaponpoddeco") //decorative looks like drone
                        || weap.getId().equals("omm_pddronedeco")
                        || weap.getId().equals("omm_smallpoddeco")
                        || weap.getId().equals("omm_synergypoddeco")
                        || weap.getId().equals("omm_missilepoddeco")
                        || weap.getId().equals("omm_compositepoddeco")
                        || weap.getId().equals("omm_largepoddeco")
                        || weap.getId().equals("omm_ballisticpoddeco")) {

                    weap.getSprite().setSize(0, 0);
                }
                if (weap.getSlot().getId().equals("droneslot") //slot has same name as on drone !important!
                        || weap.getSlot().getId().equals("pdslot")
                        || weap.getSlot().getId().equals("smlhybridslot")
                        || weap.getSlot().getId().equals("synergyslot")
                        || weap.getSlot().getId().equals("smlmissileslot")
                        || weap.getSlot().getId().equals("compositeslot")
                        || weap.getSlot().getId().equals("largeslot")
                        || weap.getSlot().getId().equals("ballisticslot")) {

                    weap.getSprite().setSize(0, 0);

                   // weap.disable(true);
                    //ship.removeWeaponFromGroups(weap);                           //removes the weapons swap "interface" from weapon groups

                    if (weap.getUnderSpriteAPI() != null) {
                        weap.getUnderSpriteAPI().setSize(0, 0);
                    }
                    if (weap.getBarrelSpriteAPI() != null) {
                        weap.getBarrelSpriteAPI().setSize(0, 0);
                    }
                }

                //Weapon Stuff, swapping and such
                for (FighterWingAPI fighterWing : ship.getAllWings()) {
                    if (!fighterWing.getWingId().equals("omm_pddrone_wing")
                            || !fighterWing.getWingId().equals("omm_weaponpod_wing")
                            || !fighterWing.getWingId().equals("omm_smallpod_wing")
                            || !fighterWing.getWingId().equals("omm_synergypod_wing")
                            || !fighterWing.getWingId().equals("omm_missilepod_wing")
                            || !fighterWing.getWingId().equals("omm_compositepod_wing")
                            || !fighterWing.getWingId().equals("omm_largepod_wing")
                            || !fighterWing.getWingId().equals("omm_ballisticpod_wing")) {
                        ShipAPI fighter = fighterWing.getLeader();
                        fighter.getMutableStats().getBallisticWeaponRangeBonus().modifyMult(this.getClass().toString(), ship.getMutableStats().getBallisticWeaponRangeBonus().computeEffective(1f));
                        fighter.getMutableStats().getEnergyWeaponRangeBonus().modifyMult(this.getClass().toString(), ship.getMutableStats().getEnergyWeaponRangeBonus().computeEffective(1f));
                        fighter.getMutableStats().getBeamWeaponRangeBonus().modifyFlat(this.getClass().toString(), ship.getMutableStats().getBeamWeaponRangeBonus().computeEffective(1f));
                        continue;
                    }

                    ShipAPI fighter = fighterWing.getLeader();
                    for (WeaponAPI dronewep : fighter.getAllWeapons()) {
                        if (!dronewep.getSlot().getId().equals("pdslot")
                                || !dronewep.getSlot().getId().equals("droneslot")
                                || !dronewep.getSlot().getId().equals("smlhybridslot")
                                || !dronewep.getSlot().getId().equals("synergyslot")
                                || !dronewep.getSlot().getId().equals("smlmissileslot")
                                || !dronewep.getSlot().getId().equals("compositeslot")
                                || !dronewep.getSlot().getId().equals("largeslot")
                                || !dronewep.getSlot().getId().equals("ballisticslot")) {
                            continue;
                        }


                        WeaponAPI weapon = droneWeaponPerShip.get(ship);
                        if (weapon != null) {
                            weapon.getLocation().set(dronewep.getLocation());

                            //         weapon.repair();
                            weapon.getSprite().setColor(new Color(1f, 1f, 1f, 1f));
                            if (weapon.getBarrelSpriteAPI() != null) {
                                weapon.getBarrelSpriteAPI().setColor(new Color(255, 255, 255, 255));
                            }

                            WeaponGroupAPI group = ship.getWeaponGroupFor(weapon);
                            if (group != null) {
                                group.removeWeapon(0);

                                fighter.getWeaponGroupFor(dronewep).addWeaponAPI(weapon);
                                fighter.getVariant().assignUnassignedWeapons();
                            }
                        }

                        //aims the drone weapon
                    }

                    //get enemies in range
                    List<ShipAPI> enemies = AIUtils.getNearbyEnemies(ship, 2000f);

                    //REFIT AI
                    if (fighter.getHullLevel() < 0.9f && enemies.isEmpty() && !fighterWing.isReturning(fighter)) {
                        fighterWing.orderReturn(fighter);
                    }
                }
            }




            if (!ship.isAlive() || ship.isHulk()) {

                return;
            }
            if (ship.getFluxTracker().isOverloadedOrVenting()) {
                return;
            }
            FluxTrackerAPI carrierFlux = ship.getFluxTracker();
            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing() == null) {
                    continue;
                }
                for (ShipAPI fighter : bay.getWing().getWingMembers()) {
                    if (!bay.getWing().getWingId().equals("FreitagCorporation_ShieldDrone_wing")) {
                        transferFlux(fighter, carrierFlux);
                    }

                }
                for (FighterWingAPI.ReturningFighter returning : bay.getWing().getReturning()) {
                    transferFlux(returning.fighter, carrierFlux);
                }
            }
        }

    }

    private static void transferFlux(ShipAPI fighter, FluxTrackerAPI carrierFlux) {
        if (!fighter.isAlive() || fighter.isHulk()) {
            return;
        }

        if (fighter.isPhased()) {
            return;
        }
        FluxTrackerAPI fighterFlux = fighter.getFluxTracker();
        if (fighterFlux.isOverloadedOrVenting()) {
            return;
        }
        float hard = fighterFlux.getHardFlux();
        float soft = fighterFlux.getCurrFlux() - hard;
        carrierFlux.increaseFlux(hard, true);
        fighterFlux.setHardFlux(0.0F);
        if (carrierFlux.increaseFlux(soft, false)) {
            fighterFlux.setCurrFlux(0.0F);
        } else {
            fighterFlux.setCurrFlux(soft);
        }
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport
    ) {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport
    ) {

    }

}
