//package tecrys.data.fighters.composite_pod;
//
//import com.fs.starfarer.api.Global;
//import com.fs.starfarer.api.combat.*;
//
//import static com.fs.starfarer.api.combat.WeaponAPI.WeaponType.MISSILE;
//
//import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
//import com.fs.starfarer.api.loading.WeaponSpecAPI;
//import com.fs.starfarer.api.util.IntervalUtil;
//import org.lazywizard.lazylib.MathUtils;
//import org.lazywizard.lazylib.VectorUtils;
//import org.lazywizard.lazylib.combat.AIUtils;
//import org.lazywizard.lazylib.combat.CombatUtils;
//import org.lwjgl.util.vector.Vector2f;
//
//import java.awt.Color;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//import org.lwjgl.input.Keyboard;
//import static org.lwjgl.input.Keyboard.KEY_R;
//import org.lwjgl.input.Mouse;
//import tecrys.data.utils.OMMSettings;
//
//public class compositepodManager implements AdvanceableListener {
//
//    public final ShipAPI mothership;
//    private boolean isWeaponSwappedcomposite = false;
//
//    public IntervalUtil timer = new IntervalUtil(0F, 5F);
//    public final ArrayList<ShipAPI> dronescomposite = new ArrayList<>();
//    public final ArrayList<FighterWingAPI> relevantWings = new ArrayList<>();
//
//    private final IntervalUtil deadDroneInterval = new IntervalUtil(0.2f, 0.2f);
//    private boolean wasSelectedLastFrame = false;
//    private boolean firstFrame = true;
//
//    public compositepodManager(ShipAPI mothership) {
//        this.mothership = mothership;
//
//        for (FighterWingAPI wing : this.mothership.getAllWings()) {
//            if (wing.getSpec().getId().equals("omm_compositepod_wing")) {
//                relevantWings.add(wing);
//            }
//        }
//    }
//
//    float angleFromBasepos = 150f;
//    float distBetweenClusters = 210f;
//    float distFromClusterCenterToDrone = 30f;
//
//    public Vector2f getDesiredPosition(ShipAPI drone) {
//        int wingIndex = relevantWings.indexOf(drone.getWing());
//        int indexInWing = drone.getWing().getWingMembers().indexOf(drone);
//        int oddOrEvenNoOfWings = relevantWings.size() % 2;
//        float anglebetweenDrones = 360f / drone.getWing().getSpec().getNumFighters();
//
//        if (mothership.getHullSpec().getHullId().startsWith("omm_sandhopper")) {
//            angleFromBasepos = 42f;
//            distBetweenClusters = 62f;
//            distFromClusterCenterToDrone = 30f;
//        }
//
//        // Nathantia positioning: Inner wings
//        if (mothership.getHullSpec().getHullId().startsWith("omm_nathantia")) {
//            angleFromBasepos = 145f;
//            distBetweenClusters = 230f;
//            distFromClusterCenterToDrone = 30f;
//        }
//
//        if (mothership.isPullBackFighters()) {
//            Vector2f basePos = MathUtils.getPointOnCircumference(mothership.getShieldCenterEvenIfNoShield(), mothership.getShieldRadiusEvenIfNoShield(), mothership.getFacing());
//            if (oddOrEvenNoOfWings == 0) {
//                Vector2f clusterCenter;
//                if (wingIndex % 2 == 0) {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * wingIndex), mothership.getFacing() + angleFromBasepos);
//                } else {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * (wingIndex - 1)), mothership.getFacing() - angleFromBasepos);
//                }
//                return MathUtils.getPointOnCircumference(clusterCenter, distFromClusterCenterToDrone, mothership.getFacing() + (indexInWing * anglebetweenDrones));
//            } else {
//                Vector2f clusterCenter;
//                if (wingIndex == 0) {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, 0, mothership.getFacing() + angleFromBasepos);
//                } else if (wingIndex % 2 == 0) {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * (wingIndex - 1)) * 2f, mothership.getFacing() + angleFromBasepos);
//                } else {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * wingIndex) * 2f, mothership.getFacing() - angleFromBasepos);
//                }
//                return MathUtils.getPointOnCircumference(clusterCenter, distFromClusterCenterToDrone, mothership.getFacing() + (indexInWing * anglebetweenDrones));
//            }
//        } else {
//            float desiredFacing = VectorUtils.getAngle(mothership.getShieldCenterEvenIfNoShield(), mothership.getMouseTarget());
//            Vector2f basePos = MathUtils.getPointOnCircumference(mothership.getShieldCenterEvenIfNoShield(), mothership.getShieldRadiusEvenIfNoShield(), desiredFacing);
//            if (oddOrEvenNoOfWings == 0) {
//                Vector2f clusterCenter;
//                if (wingIndex % 2 == 0) {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * wingIndex), desiredFacing + angleFromBasepos);
//                } else {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * (wingIndex - 1)), desiredFacing - angleFromBasepos);
//                }
//                return MathUtils.getPointOnCircumference(clusterCenter, distFromClusterCenterToDrone, desiredFacing + (indexInWing * anglebetweenDrones));
//            } else {
//                Vector2f clusterCenter;
//                if (wingIndex == 0) {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, 0, desiredFacing + angleFromBasepos);
//                } else if (wingIndex % 2 == 0) {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * (wingIndex - 1)) * 2f, desiredFacing + angleFromBasepos);
//                } else {
//                    clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * wingIndex) * 2f, desiredFacing - angleFromBasepos);
//                }
//                return MathUtils.getPointOnCircumference(clusterCenter, distFromClusterCenterToDrone, desiredFacing + (indexInWing * anglebetweenDrones));
//            }
//        }
//    }
//
//    public float getDesiredFacing(ShipAPI drone) {
//        CombatEngineAPI engine = Global.getCombatEngine();
//        if (engine != null) {
//            boolean isPlayerFlagship = (engine.getPlayerShip() == this.mothership);
//            boolean isPlayerControlled = isPlayerFlagship && engine.isUIAutopilotOn();
//
//            WeaponAPI mWep = null;
//            for (WeaponAPI w : mothership.getAllWeapons()) {
//                if (w.getSlot().getId().equals("compositeslot")) {
//                    mWep = w;
//                    break;
//                }
//            }
//
//            if (mWep != null) {
//                WeaponGroupAPI mGroup = mothership.getWeaponGroupFor(mWep);
//                boolean isSelected = isPlayerFlagship && (this.mothership.getSelectedGroupAPI() == mGroup);
//
//                if (isPlayerControlled && isSelected) {
//                    return VectorUtils.getAngle(drone.getLocation(), mothership.getMouseTarget());
//                }
//            }
//
//            if (mothership.areAnyEnemiesInRange() && AIUtils.getNearestEnemy(mothership) != null) {
//                if (!AIUtils.getNearestEnemy(mothership).getHullSize().equals(ShipAPI.HullSize.FIGHTER)) {
//                    return VectorUtils.getAngle(drone.getLocation(), AIUtils.getNearestEnemy(mothership).getLocation());
//                }
//            }
//            if (mothership.isPullBackFighters() && !mothership.areAnyEnemiesInRange()) {
//                return mothership.getFacing();
//            }
//        }
//        return VectorUtils.getAngle(drone.getLocation(), mothership.getMouseTarget());
//    }
//
//    @Override
//    public void advance(float amount) {
//        ShipAPI ship = this.mothership;
//        CombatEngineAPI engine = Global.getCombatEngine();
//
//        WeaponAPI mothershipWep = null;
//        for (WeaponAPI w : ship.getAllWeapons()) {
//            if (w.getSlot().getId().equals("compositeslot")) {
//                mothershipWep = w;
//                break;
//            }
//        }
//
//        if (ship.isAlive() && mothershipWep != null) {
//
//            mothershipWep.ensureClonedSpec();
//            mothershipWep.getSpec().setMaxRange(0.01f);
//            mothershipWep.setForceNoFireOneFrame(true);
//
//            WeaponGroupAPI mothershipGroup = ship.getWeaponGroupFor(mothershipWep);
//            boolean isPlayerFlagship = (engine.getPlayerShip() == this.mothership);
//            boolean isPlayerControlled = isPlayerFlagship && engine.isUIAutopilotOn();
//            boolean isCurrentlySelected = isPlayerFlagship && (this.mothership.getSelectedGroupAPI() == mothershipGroup);
//
//            if (firstFrame) {
//                wasSelectedLastFrame = isCurrentlySelected;
//                firstFrame = false;
//            }
//
//            List<FighterWingAPI> wings = relevantWings;
//
//            int currentAmmo = 0;
//            int maxAmmoSpec = 0;
//            float currentReload = 0f;
//            boolean foundAmmo = false;
//
//            for (FighterWingAPI wing : wings) {
//                if (wing == null || wing.getWingMembers() == null) continue;
//                if (!wing.getWingId().equals("omm_compositepod_wing")) continue;
//                for (ShipAPI fighter : wing.getWingMembers()) {
//                    if (fighter == null || !fighter.isAlive()) continue;
//                    for (WeaponAPI w : fighter.getAllWeapons()) {
//                        if (w.getSlot().getId().equals("compositeslot") && w.usesAmmo()) {
//                            foundAmmo = true;
//                            AmmoTrackerAPI tracker = w.getAmmoTracker();
//                            if (tracker != null && tracker.getAmmo() >= currentAmmo) {
//                                currentAmmo = tracker.getAmmo();
//                                maxAmmoSpec = w.getSpec().getMaxAmmo();
//                                currentReload = tracker.getReloadProgress();
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (foundAmmo && mothershipWep.getAmmoTracker() != null) {
//                mothershipWep.getSpec().setMaxAmmo(maxAmmoSpec);
//                mothershipWep.getAmmoTracker().setAmmo(currentAmmo);
//                mothershipWep.getAmmoTracker().setReloadProgress(currentReload);
//            }
//
//            for (FighterWingAPI wing : wings) {
//                if (wing == null || wing.getWingMembers() == null || wing.getWingMembers().isEmpty()) continue;
//                if (!wing.getWingId().equals("omm_compositepod_wing")) continue;
//
//                for (int i = 0; i < wing.getWingMembers().size(); i++) {
//                    ShipAPI fighter = wing.getWingMembers().get(i);
//                    if (fighter == null || !fighter.isAlive() || fighter.getAllWeapons().isEmpty()) continue;
//
//                    WeaponAPI droneWep = null;
//                    WeaponAPI laswep = null;
//                    for (WeaponAPI w : fighter.getAllWeapons()) {
//                        if (w.getSlot().getId().equals("compositeslot")) droneWep = w;
//                        if (w.getSlot().getId().equals("omm_laser")) laswep = w;
//                    }
//
//                    if (droneWep != null) {
//                        WeaponGroupAPI droneGroup = fighter.getWeaponGroupFor(droneWep);
//
//                        if (mothershipGroup != null && droneGroup != null) {
//
//                            boolean shouldAutofire;
//                            if (isPlayerControlled) {
//                                shouldAutofire = mothershipGroup.isAutofiring() && !isCurrentlySelected;
//                            } else {
//                                shouldAutofire = true;
//                            }
//
//                            if (droneGroup.isAutofiring() != shouldAutofire) {
//                                int groupIndex = fighter.getWeaponGroupsCopy().indexOf(droneGroup);
//                                fighter.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, groupIndex);
//                            }
//
//                            if (isCurrentlySelected && !wasSelectedLastFrame) {
//                                droneWep.beginSelectionFlash();
//                                if (laswep != null) laswep.beginSelectionFlash();
//                                fighter.addAfterimage(new Color(255, 255, 255, 175), 0f, 0f, 0f, 0f, 0f, 0f, 0.1f, 0.3f, true, true, true);
//                            }
//
//                            if (isPlayerControlled && isCurrentlySelected && !engine.isPaused()) {
//                                Vector2f mousePos = this.mothership.getMouseTarget();
//                                float angleToMouse = VectorUtils.getAngle(fighter.getLocation(), mousePos);
//
//                                if (laswep != null && !fighter.isLanding() && !fighter.isLiftingOff()) {
//                                    laswep.setForceFireOneFrame(true);
//                                    laswep.setCurrAngle(angleToMouse);
//                                    laswep.ensureClonedSpec();
//
//                                    float exactEffectiveRange = droneWep.getRange();
//                                    laswep.getSpec().setMaxRange(exactEffectiveRange);
//                                }
//
//                                float diff = MathUtils.getShortestRotation(fighter.getFacing(), angleToMouse);
//                                float maxTurn = fighter.getMaxTurnRate() * amount;
//                                fighter.setFacing(fighter.getFacing() + MathUtils.clamp(diff, -maxTurn, maxTurn));
//                                droneWep.setCurrAngle(angleToMouse);
//
//                                if (!engine.isUIShowingDialog() && !engine.getCombatUI().isShowingCommandUI()) {
//                                    boolean shouldFire = false;
//
//                                    if (droneWep.getType() == WeaponAPI.WeaponType.MISSILE) {
//                                        if (OMMSettings.missile_key == 0) {
//                                            if (Mouse.isButtonDown(2) || Mouse.isButtonDown(0)) shouldFire = true;
//                                        } else {
//                                            if (Keyboard.isKeyDown(OMMSettings.missile_key) || Mouse.isButtonDown(0)) shouldFire = true;
//                                        }
//                                    } else {
//                                        if (Mouse.isButtonDown(0)) shouldFire = true;
//                                    }
//
//                                    if (shouldFire && !this.mothership.getFluxTracker().isOverloadedOrVenting()) {
//                                        int groupIndex = fighter.getWeaponGroupsCopy().indexOf(droneGroup);
//                                        fighter.giveCommand(ShipCommand.FIRE, mousePos, groupIndex);
//                                        droneWep.setForceFireOneFrame(true);
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    if (this.mothership.getFluxTracker().isOverloaded()) {
//                        if (!fighter.getFluxTracker().isOverloaded()) {
//                            fighter.getFluxTracker().forceOverload(this.mothership.getFluxTracker().getOverloadTimeRemaining());
//                        }
//                    } else if (fighter.getFluxTracker().isOverloaded()) {
//                        fighter.getFluxTracker().stopOverload();
//                    }
//
//                    if (this.mothership.getFluxTracker().isVenting() && droneWep != null) {
//                        droneWep.setForceNoFireOneFrame(true);
//                    }
//
//                    MutableShipStatsAPI stats = fighter.getMutableStats();
//                    ShipVariantAPI OGVariant = stats.getVariant().clone();
//                    ShipVariantAPI newVariant = stats.getVariant().clone();
//
//                    String str = (String) engine.getCustomData().get("omm_compositedroneWeaponId" + this.mothership.getId());
//                    if (str == null)
//                        str = "No weapon";
//
//                    if (!fighter.getAllWeapons().get(0).getId().equals(str)) {
//                        fighter.resetDefaultAI();
//                        if (ship.getVariant().getWeaponSpec("compositeslot") != null) {
//                            engine.getCustomData().put("omm_compositedroneWeaponId" + this.mothership.getId(), this.mothership.getVariant().getWeaponId("compositeslot"));
//                            stats.getVariant().setOriginalVariant(null);
//                            fighter.getFleetMember().setVariant(newVariant, true, true);
//
//                            ship.syncWeaponDecalsWithArmorDamage();
//                            this.timer.randomize();
//
//                            this.timer.advance(amount);
//                            if (!this.timer.intervalElapsed()) {
//                                return;
//                            }
//                            stats.getVariant().clearSlot("compositeslot");
//                            stats.getVariant().addWeapon("compositeslot", this.mothership.getVariant().getWeaponId("compositeslot"));
//                            stats.getVariant().getWeaponSpec("compositeslot").addTag("FIRE_WHEN_INEFFICIENT");
//
//                            wing.orderReturn(fighter);
//                        }
//                    } else {
//                        fighter.setShipAI(null);
//                    }
//                }
//            }
//
//            wasSelectedLastFrame = isCurrentlySelected;
//        }
//    }
//}