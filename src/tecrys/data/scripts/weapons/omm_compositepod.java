//package tecrys.data.scripts.weapons;
//
//import com.fs.starfarer.api.Global;
//import com.fs.starfarer.api.combat.CombatEngineAPI;
//import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
//import com.fs.starfarer.api.combat.FighterWingAPI;
//import com.fs.starfarer.api.combat.MutableShipStatsAPI;
//import com.fs.starfarer.api.combat.ShipAPI;
//import com.fs.starfarer.api.combat.ShipCommand;
//import com.fs.starfarer.api.combat.ShipVariantAPI;
//import com.fs.starfarer.api.combat.WeaponAPI;
//import static com.fs.starfarer.api.combat.WeaponAPI.WeaponType.MISSILE;
//import com.fs.starfarer.api.combat.WeaponGroupAPI;
//import com.fs.starfarer.api.util.IntervalUtil;
//import java.util.List;
//import org.lazywizard.lazylib.MathUtils;
//import org.lazywizard.lazylib.VectorUtils;
//import org.lwjgl.input.Mouse;
//import org.lwjgl.util.vector.Vector2f;
//
//public class omm_compositepod implements EveryFrameWeaponEffectPlugin {
//
//
//    private boolean isWeaponSwappedcomposite = false;
//    private ShipAPI SHIP;
//    private ShipAPI FIGHTER;
//    public IntervalUtil timer = new IntervalUtil(3F, 20F);
//
//    @Override
//    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
//        this.SHIP = weapon.getShip();
//        if (engine.isPaused()) {
//            return;
//        }
//
//
//        List<WeaponGroupAPI> weapons = this.SHIP.getWeaponGroupsCopy();
//        List<WeaponAPI> list = this.SHIP.getAllWeapons();
//        List<FighterWingAPI> dronewings = this.SHIP.getAllWings();
//        if (this.SHIP.getOriginalOwner() == 0 || this.SHIP.getOriginalOwner() == 1) { //check for refit screen
//
//            for (FighterWingAPI fighterWingAPI : dronewings) {
//                if (!fighterWingAPI.getWingId().equals("omm_compositepod_wing")) {   //name is the built-in drone wing
//                    continue;
//                }
//                Vector2f mousepos = this.SHIP.getMouseTarget();
//                FIGHTER = fighterWingAPI.getLeader();
//                List<WeaponAPI> droneweps = FIGHTER.getAllWeapons();
//                Vector2f dronepos = FIGHTER.getLocation();
//                float angle = VectorUtils.getAngle(dronepos, mousepos);
//
//                for (WeaponAPI dronewep : droneweps) {
//                    if (dronewep.getSlot().getId().equals("compositeslot")|| dronewep.getSlot().getId().equals("omm_laser")) {
//
//                        WeaponGroupAPI Group = FIGHTER.getWeaponGroupFor(weapon);
//                        FIGHTER.getVariant().assignUnassignedWeapons();
//                        float diff = MathUtils.getShortestRotation(dronewep.getCurrAngle(), angle);
//                        float maxVel = amount * dronewep.getTurnRate();
//                        diff = MathUtils.clamp(diff, -maxVel, maxVel);
//                        dronewep.setCurrAngle(diff + dronewep.getCurrAngle());     //aims the drone weapon
//
//                        float diffdrone = MathUtils.getShortestRotation(FIGHTER.getFacing(), angle);
//                        float maxVeldrone = amount * FIGHTER.getMaxTurnRate();
//                        diffdrone = MathUtils.clamp(diffdrone, -maxVeldrone, maxVeldrone);
//                        FIGHTER.setFacing(diffdrone + FIGHTER.getFacing());        //sets facing of the drone
//
// 
//                        
//                        ShipAPI player = Global.getCombatEngine().getPlayerShip();
//                        {if (player == this.SHIP && !this.FIGHTER.isLanding() && !this.FIGHTER.isLiftingOff() && dronewep.getSlot().getId().equals("omm_laser")) {
//
//                            dronewep.getAnimation().setFrame(01);
//                            //MagicRender.singleframe(sprite, dronewep.getLocation(), size, dronewep.getCurrAngle(), Color.WHITE, false, CombatEngineLayers.FIGHTERS_LAYER);
//
//                        }
//}
//                        if (dronewep.getSlot().getId().equals("compositeslot")) {
//                            if (Mouse.isButtonDown(0) && !player.getFluxTracker().isOverloadedOrVenting() && (dronewep.getType() != MISSILE)) {
//                                this.FIGHTER.giveCommand(ShipCommand.FIRE, mousepos, 0);           //clicky left drone shooty
//                            }
//                            if (Mouse.isButtonDown(2) && !player.getFluxTracker().isOverloadedOrVenting() && (dronewep.getType() == MISSILE)) {
//                                this.FIGHTER.giveCommand(ShipCommand.FIRE, mousepos, 0);           //clicky left drone shooty
//                            }
//                        }
//                        if (player != this.SHIP) {
//                            for (WeaponGroupAPI group : FIGHTER.getWeaponGroupsCopy()){
//                            this.FIGHTER.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, FIGHTER.getWeaponGroupsCopy().indexOf(group));       
//                            }
//                        if (dronewep.getAnimation() != null)
//                            dronewep.getAnimation().setFrame(00);
//                        }
//                        for (WeaponAPI weaponAPI : list) {
////                if (weaponAPI.getId().equals("omm_weaponpoddeco")) {                  //decorative looks like drone
////                    weaponAPI.getSprite().setColor(new Color(255, 255, 255, 0));
////                }
//                            if (weaponAPI.getSlot().getId().equals("compositeslot")) {                //slot has same name as on drone !important!
////                    weaponAPI.getSprite().setColor(new Color(255, 255, 255, 0));
//                            }
//                            for (int i = 0; i < weapons.size(); i++) {
//                                if (weaponAPI.getSlot().getId().equals("compositeslot")) {
//                                    weaponAPI.disable(true);
//                                    this.SHIP.removeWeaponFromGroups(weaponAPI);                   //removes the weapons swap "interface" from weapon groups
//                                }
//                            }
////                if (weaponAPI.getBarrelSpriteAPI() != null) {
////                    weaponAPI.getBarrelSpriteAPI().setColor(new Color(255, 255, 255, 0));
////                }
//                        }
//                        continue;
//                    }
//                }
//
//            }
//        }
//
//        this.timer.randomize();                                                        //randomize interval to stagger drone refit
//
//        this.timer.advance(amount);
//        if (!this.timer.intervalElapsed()) {
//            return;
//        }
//        if (isWeaponSwappedcomposite) {
//                                        this.FIGHTER.setShipAI(null);
//            return;
//        }
//        if (!isWeaponSwappedcomposite) {
//
//            if (this.SHIP != null) {
//                List<FighterWingAPI> list1 = this.SHIP.getAllWings();
//                for (FighterWingAPI fighterWingAPI : list1) {
//                    if (!fighterWingAPI.getWingId().equals("omm_compositepod_wing")) {
//                        continue;
//                    }
//
//                    {
//                        this.FIGHTER = fighterWingAPI.getLeader();
//                        this.FIGHTER.resetDefaultAI();
//                        MutableShipStatsAPI mutableShipStatsAPI = this.FIGHTER.getMutableStats();
//                        ShipVariantAPI shipVariantAPI = mutableShipStatsAPI.getVariant().clone();
//                        this.FIGHTER.getFleetMember().setVariant(shipVariantAPI, false, true);
//                        mutableShipStatsAPI.getVariant().clearSlot("compositeslot");
//                        if (this.SHIP.getVariant().getWeaponSpec("compositeslot") != null) {
//                            mutableShipStatsAPI.getVariant().addWeapon("compositeslot", this.SHIP.getVariant().getWeaponId("compositeslot"));
//                            mutableShipStatsAPI.getVariant().getWeaponSpec("compositeslot").addTag("FIRE_WHEN_INEFFICIENT");
//                            fighterWingAPI.orderReturn(this.FIGHTER);
//
//                            this.isWeaponSwappedcomposite = true;
//
//                        }
//
//                    }
//                }
//            }
//        }
//    }
//
//
//
//}
