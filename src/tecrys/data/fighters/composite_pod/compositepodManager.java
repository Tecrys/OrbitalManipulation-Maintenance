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

public class compositepodManager implements AdvanceableListener {

    public final ShipAPI mothership;
    private boolean isWeaponSwappedcomposite = false;

    public IntervalUtil timer = new IntervalUtil(3F, 20F);
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
    //todo need to give the sprite for these guys a second look, too bright vs the xyphos equivalent
    //todo need to do something for if the index per wing goes over 3 lol
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
                float angle = VectorUtils.getAngle(dronepos, mousepos);

                for (WeaponAPI dronewep : droneweps) {
                    if (dronewep.getSlot().getId().equals("compositeslot") || dronewep.getSlot().getId().equals("omm_laser")) {

//                        WeaponGroupAPI Group = FIGHTER.getWeaponGroupFor(weapon);
                        ShipAPI player = Global.getCombatEngine().getPlayerShip();
                        CombatEngineAPI engine = Global.getCombatEngine();
                        {

                            if (player == this.mothership && !drone.isLanding() && !drone.isLiftingOff() && dronewep.getSlot().getId().equals("omm_laser") 
                                    ) {
                                dronewep.getAnimation().setFrame(01);

                                //MagicRender.singleframe(sprite, dronewep.getLocation(), size, dronewep.getCurrAngle(), Color.WHITE, false, CombatEngineLayers.FIGHTERS_LAYER);
                            }
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
                                drone.giveCommand(ShipCommand.FIRE, mousepos, 0);           //clicky left drone shooty
                            }
                            if (Keyboard.isKeyDown(KEY_R)) {
                                drone.setShipTarget(this.mothership.getShipTarget());           //clicky left drone shooty
                            }
                            if (Mouse.isButtonDown(2) && !player.getFluxTracker().isOverloadedOrVenting() && (dronewep.getType() == MISSILE) && dronewep.getSlot().getId().equals("compositeslot")) {
                                drone.giveCommand(ShipCommand.FIRE, mousepos, 0);           //clicky left drone shooty
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
        deadDroneInterval.advance(amount);
        if (deadDroneInterval.intervalElapsed()) {
            for (ShipAPI drone : new ArrayList<>(dronescomposite)) {
                //Global.getCombatEngine().addFloatingText(drone.getLocation(), drone.getWing().getWingMembers().indexOf(drone) + ", " + drone.getWing().getSourceShip().getAllWings().indexOf(drone.getWing()), 20f, Color.BLUE, drone, 1f, 1f);
                if (!drone.isAlive() || drone.isHulk() || !Global.getCombatEngine().isEntityInPlay(drone)) {
                    dronescomposite.remove(drone);
                }
                this.timer.randomize();                                                        //randomize interval to stagger drone refit

                this.timer.advance(amount);
                if (!this.timer.intervalElapsed()) {
                    return;
                }
                if (this.isWeaponSwappedcomposite) {
                    drone.setShipAI(null);
                    return;
                }
                if (!this.isWeaponSwappedcomposite) {

                    if (this.mothership != null) {
                        List<FighterWingAPI> list1 = this.mothership.getAllWings();
                        for (FighterWingAPI fighterWingAPI : list1) {
                            if (!fighterWingAPI.getWingId().equals("omm_compositepod_wing")) {
                                continue;
                            }

                            {
                                drone = fighterWingAPI.getLeader();
                                drone.resetDefaultAI();
                                MutableShipStatsAPI mutableShipStatsAPI = drone.getMutableStats();
                                ShipVariantAPI shipVariantAPI = mutableShipStatsAPI.getVariant().clone();
                                drone.getFleetMember().setVariant(shipVariantAPI, false, true);
                                mutableShipStatsAPI.getVariant().clearSlot("compositeslot");
                                if (this.mothership.getVariant().getWeaponSpec("compositeslot") != null) {
                                    mutableShipStatsAPI.getVariant().addWeapon("compositeslot", this.mothership.getVariant().getWeaponId("compositeslot"));
                                    mutableShipStatsAPI.getVariant().getWeaponSpec("compositeslot").addTag("FIRE_WHEN_INEFFICIENT");
                                    fighterWingAPI.orderReturn(drone);

                                    this.isWeaponSwappedcomposite = true;

                                }

                            }
                        }
                    }
                }
            }
        }
    }
}
