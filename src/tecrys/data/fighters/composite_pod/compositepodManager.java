package tecrys.data.fighters.composite_pod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import static com.fs.starfarer.api.combat.WeaponAPI.WeaponType.MISSILE;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;
import static org.lwjgl.input.Keyboard.KEY_R;
import org.lwjgl.input.Mouse;
import tecrys.data.utils.OMMSettings;

public class compositepodManager implements AdvanceableListener {

    public final ShipAPI mothership;
    private boolean isWeaponSwappedcomposite = false;

    public IntervalUtil timer = new IntervalUtil(0F, 5F);
    public final ArrayList<ShipAPI> dronescomposite = new ArrayList<>(); //list of all drones
    public final ArrayList<FighterWingAPI> relevantWings = new ArrayList<>(); //the list of sarissa wings

    private final IntervalUtil deadDroneInterval = new IntervalUtil(0.2f, 0.2f);

    public compositepodManager(ShipAPI mothership) {
        this.mothership = mothership;

        for (FighterWingAPI wing : mothership.getAllWings()) {
            if (wing.getSpec().getId().equals("omm_compositepod_wing")) {
                relevantWings.add(wing);
            }
        }
    }

    float angleFromBasepos = 150f;
    float distBetweenClusters = 210f;
    float distFromClusterCenterToDrone = 30f;

    //each wing groups up into a triangle
    //odd no. of wings has one in center + 2 on each side, even no. of wings has non in center & all on sides
    //if in engage mode, stick closer to shield & rotate to face shield facing
    public Vector2f getDesiredPosition(ShipAPI drone) {

        int wingIndex = relevantWings.indexOf(drone.getWing());
        int indexInWing = drone.getWing().getWingMembers().indexOf(drone);
        int oddOrEvenNoOfWings = relevantWings.size() % 2;
        float anglebetweenDrones = 360f / drone.getWing().getSpec().getNumFighters();

        if (mothership.getHullSpec().getHullId().startsWith("omm_sandhopper")) {
            angleFromBasepos = 42f;
            distBetweenClusters = 62f;
            distFromClusterCenterToDrone = 30f;
        }
        if (mothership.getHullSpec().getHullId().startsWith("omm_nathantia")) {
            angleFromBasepos = 130f;
            distBetweenClusters = 140f;
            distFromClusterCenterToDrone = 30f;
        }
        List<WeaponGroupAPI> weapons = this.mothership.getWeaponGroupsCopy();
        List<WeaponAPI> list = this.mothership.getAllWeapons();
        List<FighterWingAPI> dronewings = this.mothership.getAllWings();
        if (this.mothership.getOriginalOwner() == 0 || this.mothership.getOriginalOwner() == 1) { //check for refit screen

            for (FighterWingAPI fighterWingAPI : dronewings) {
                if (!fighterWingAPI.getWingId().equals("omm_compositepod_wing")) {   //name is the built-in drone wing
                    continue;
                }
                Vector2f mousepos = this.mothership.getMouseTarget();
                drone = fighterWingAPI.getLeader();
                List<WeaponAPI> droneweps = drone.getAllWeapons();
                Vector2f dronepos = drone.getLocation();
                WeaponAPI comwep = null;
                WeaponAPI laswep = null;
                float angle = VectorUtils.getAngle(dronepos, mousepos);

                for (WeaponAPI dronewep : droneweps) {
                    if (dronewep.getSlot().getId().equals("compositeslot") || dronewep.getSlot().getId().equals("omm_laser")) {
                        {
                            if (dronewep.getSlot().getId().equals("compositeslot"))
                                comwep = dronewep;
                        } {
                            if (dronewep.getSlot().getId().equals("omm_laser"))
                                laswep = dronewep;
                        }
//                        WeaponGroupAPI Group = FIGHTER.getWeaponGroupFor(weapon);
                        ShipAPI player = Global.getCombatEngine().getPlayerShip();
                        CombatEngineAPI engine = Global.getCombatEngine();
                        {

                            if (player == this.mothership && !drone.isLanding() && !drone.isLiftingOff() && dronewep.getSlot().getId().equals("omm_laser") && comwep != null
                                    ) {
                                //dronewep.getAnimation().setFrame(01);
                                // dronewep.getSprite().setHeight(synwep.getRange()*2);
                                // dronewep.getSprite().setCenterY(synwep.getRange());
                                laswep.setForceFireOneFrame(true);
                                laswep.ensureClonedSpec();
                                laswep.getSpec().setMaxRange(comwep.getRange()-((comwep.getRange()/100)*20));  }
                            if (dronewep.getAnimation() != null && !engine.isUIAutopilotOn()){
                                dronewep.getAnimation().setFrame(00);}
                        }
                        for (WeaponGroupAPI group : drone.getWeaponGroupsCopy()) {
                            if ((!group.isAutofiring() && player != this.mothership) || (!group.isAutofiring() && this.mothership.equals(player) && !engine.isUIAutopilotOn())) {
                                drone.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, drone.getWeaponGroupsCopy().indexOf(group));
                            } else if (group.isAutofiring() && this.mothership.equals(player) && engine.isUIAutopilotOn()) {
                                drone.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, drone.getWeaponGroupsCopy().indexOf(group));
                            }
                        }
                        if (this.mothership.equals(player) &&  engine.isUIAutopilotOn()) {
                            drone.getVariant().assignUnassignedWeapons();
                            float diff = MathUtils.getShortestRotation(dronewep.getCurrAngle(), angle);
                            float maxVel = dronewep.getTurnRate();
                            diff = MathUtils.clamp(diff, -maxVel, maxVel);
                            dronewep.setCurrAngle(diff + dronewep.getCurrAngle());     //aims the drone weapon

                            float diffdrone = MathUtils.getShortestRotation(drone.getFacing(), angle);
                            float maxVeldrone = drone.getMaxTurnRate();
                            diffdrone = MathUtils.clamp(diffdrone, -maxVeldrone, maxVeldrone);
                            drone.setFacing(diffdrone + drone.getFacing());        //sets facing of the drone
                            if (Mouse.isButtonDown(0) && !player.getFluxTracker().isOverloadedOrVenting() && (dronewep.getType() != MISSILE) && dronewep.getSlot().getId().equals("compositeslot")) {
                                comwep.setForceFireOneFrame(true);           //clicky left drone shooty
                            }
                            if (Keyboard.isKeyDown(KEY_R)) {
                                drone.setShipTarget(this.mothership.getShipTarget());           //clicky left drone shooty
                            }
                            if ( OMMSettings.missile_key == 0 && Mouse.isButtonDown(2) && !player.getFluxTracker().isOverloadedOrVenting() && (dronewep.getType() == MISSILE) && dronewep.getSlot().getId().equals("compositeslot")) {
                                comwep.setForceFireOneFrame(true);            //clicky left drone shooty
                            }
                                else if (Keyboard.isKeyDown(OMMSettings.missile_key) && !player.getFluxTracker().isOverloadedOrVenting() && (dronewep.getType() == MISSILE) && dronewep.getSlot().getId().equals("compositeslot")) {
                                comwep.setForceFireOneFrame(true);
                            }
                        }
                        if (this.mothership.getFluxTracker().isOverloaded()) {
                            float OverloadTime = this.mothership.getFluxTracker().getOverloadTimeRemaining();
                            drone.getFluxTracker().forceOverload(OverloadTime);
                        }
                                                else if (!this.mothership.getFluxTracker().isOverloaded()){
                            drone.getFluxTracker().stopOverload();
                        }
                        if (player != this.mothership) {
//                            for (WeaponGroupAPI group : FIGHTER.getWeaponGroupsCopy()){
//                            this.FIGHTER.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, FIGHTER.getWeaponGroupsCopy().indexOf(group));       
//                            }
                            if (dronewep.getAnimation() != null) {
                                dronewep.getAnimation().setFrame(00);
                            }
                        }
                        for (WeaponAPI weaponAPI : list) {
//                if (weaponAPI.getId().equals("omm_weaponpoddeco")) {                  //decorative looks like drone
//                    weaponAPI.getSprite().setColor(new Color(255, 255, 255, 0));
//                }
                            if (weaponAPI.getSlot().getId().equals("compositeslot")) {                //slot has same name as on drone !important!
//                    weaponAPI.getSprite().setColor(new Color(255, 255, 255, 0));
                            }
                            for (int i = 0; i < weapons.size(); i++) {
                                if (weaponAPI.getSlot().getId().equals("compositeslot")) {
                                    weaponAPI.disable(true);
                                    this.mothership.removeWeaponFromGroups(weaponAPI);                   //removes the weapons swap "interface" from weapon groups
                                }
                            }
//                if (weaponAPI.getBarrelSpriteAPI() != null) {
//                    weaponAPI.getBarrelSpriteAPI().setColor(new Color(255, 255, 255, 0));
//                }
                        }
                        continue;
                    }
                }

            }
        }
        //todo better idea for defensive formation?
        if (mothership.isPullBackFighters()) {//defensive formation
            Vector2f basePos = MathUtils.getPointOnCircumference(mothership.getShieldCenterEvenIfNoShield(), mothership.getShieldRadiusEvenIfNoShield(), mothership.getFacing());
            if (oddOrEvenNoOfWings == 0) { //even number of wings, have none in the center
                Vector2f clusterCenter;//the center of this wing's cluster
                if (wingIndex % 2 == 0) { //wing index is even, go to the left
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * wingIndex), mothership.getFacing() + angleFromBasepos);
                } else { //else, go to the right
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * (wingIndex - 1)), mothership.getFacing() - angleFromBasepos);
                }
                return MathUtils.getPointOnCircumference(clusterCenter, distFromClusterCenterToDrone, mothership.getFacing() + (indexInWing * anglebetweenDrones));
            } else { //odd number of wings
                Vector2f clusterCenter;//the center of this wing's cluster
                if (wingIndex == 0) {
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, 0, mothership.getFacing() + angleFromBasepos);
                } else if (wingIndex % 2 == 0) { //wing index is even, go to the left
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * (wingIndex - 1)) * 2f, mothership.getFacing() + angleFromBasepos);
                } else { //else, go to the right
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * wingIndex) * 2f, mothership.getFacing() - angleFromBasepos);
                }
                return MathUtils.getPointOnCircumference(clusterCenter, distFromClusterCenterToDrone, mothership.getFacing() + (indexInWing * anglebetweenDrones));
            }
        } else { //offensive formation
            float desiredFacing = VectorUtils.getAngle(mothership.getShieldCenterEvenIfNoShield(), mothership.getMouseTarget());
            Vector2f basePos = MathUtils.getPointOnCircumference(mothership.getShieldCenterEvenIfNoShield(), mothership.getShieldRadiusEvenIfNoShield(), desiredFacing);
            if (oddOrEvenNoOfWings == 0) { //even number of wings, have none in the center
                Vector2f clusterCenter;//the center of this wing's cluster
                if (wingIndex % 2 == 0) { //wing index is even, go to the left
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * wingIndex), desiredFacing + angleFromBasepos);
                } else { //else, go to the right
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * (wingIndex - 1)), desiredFacing - angleFromBasepos);
                }
                return MathUtils.getPointOnCircumference(clusterCenter, distFromClusterCenterToDrone, desiredFacing + (indexInWing * anglebetweenDrones));
            } else { //odd number of wings
                Vector2f clusterCenter;//the center of this wing's cluster
                if (wingIndex == 0) {
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, 0, desiredFacing + angleFromBasepos);
                } else if (wingIndex % 2 == 0) { //wing index is even, go to the left
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * (wingIndex - 1)) * 2f, desiredFacing + angleFromBasepos);
                } else { //else, go to the right
                    clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * wingIndex) * 2f, desiredFacing - angleFromBasepos);
                }
                return MathUtils.getPointOnCircumference(clusterCenter, distFromClusterCenterToDrone, desiredFacing + (indexInWing * anglebetweenDrones));
            }
        }
    }

    public float getDesiredFacing(ShipAPI drone) {

        if (mothership.isPullBackFighters()) {//defensive formation
            return mothership.getFacing();
        } else { //offsenive formation
            return VectorUtils.getAngle(mothership.getShieldCenterEvenIfNoShield(), mothership.getMouseTarget());
        }
    }

    //remove drones if they're dead or otherwise gone
    @Override
    public void advance(float amount) {
        ShipAPI ship = this.mothership;
        //CombatEngineAPI engine = Global.getCombatEngine();
        WeaponAPI wep = null;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("compositeslot"))
                wep = w;
        }


        if (ship.isAlive() && wep != null) {
            //CombatEngineAPI engine = Global.getCombatEngine();
            List<FighterWingAPI> wings = relevantWings;

            for (FighterWingAPI wing : wings) {
                if (wing == null || wing.getWingMembers() == null || wing.getWingMembers().isEmpty())
                    continue;

                ShipAPI fighter = wing.getLeader();
                if (fighter == null)
                    continue;

                for (int i = 0; i < wing.getWingMembers().size(); i++) {
                    fighter = wing.getWingMembers().get(i);
                    if (fighter == null)
                        continue;
                    if (fighter.getAllWeapons().isEmpty())
                        continue;
                    if (!wing.getWingId().equals("omm_compositepod_wing")) {
                        continue;
                    }
                    MutableShipStatsAPI stats = fighter.getMutableStats();
                    ShipVariantAPI OGVariant = stats.getVariant().clone();
                    ShipVariantAPI newVariant = stats.getVariant().clone();
                    CombatEngineAPI engine = Global.getCombatEngine();

                    String str = (String) Global.getCombatEngine().getCustomData().get("omm_compositedroneWeaponId" + this.mothership.getId());
                    if (str == null)
                        str = "No weapon";

                    //if (engine.getPlayerShip() == ship)
                        //Global.getCombatEngine().maintainStatusForPlayerShip("SynergyDrones", "graphics/ui/icons/icon_repair_refit.png", "Drone Weaponry", str + " installed. ", true);
                        if (!fighter.getAllWeapons().get(0).getId().equals(str)) {
                            fighter.resetDefaultAI();
                            if (ship.getVariant().getWeaponSpec("compositeslot") != null) {
                                Global.getCombatEngine().getCustomData().put("omm_compositedroneWeaponId" + this.mothership.getId(), this.mothership.getVariant().getWeaponId("compositeslot"));
                                stats.getVariant().setOriginalVariant(null);
                                fighter.getFleetMember().setVariant(newVariant, true, true);
                                wep.disable(true);

                                this.timer.randomize();                                                        //randomize interval to stagger drone refit

                                this.timer.advance(amount);
                                if (!this.timer.intervalElapsed()) {
                                    return;
                                }
                                stats.getVariant().clearSlot("compositeslot");
                                stats.getVariant().addWeapon("compositeslot", this.mothership.getVariant().getWeaponId("compositeslot"));
                                stats.getVariant().getWeaponSpec("compositeslot").addTag("FIRE_WHEN_INEFFICIENT");
                                ship.removeWeaponFromGroups(wep);


                                wing.orderReturn(fighter);
                                //isWeaponSwapped = true;

                            }
                        } else fighter.setShipAI(null);
                }//

            }
        }
    }

}