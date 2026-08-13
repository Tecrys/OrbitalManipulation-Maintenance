package tecrys.data.fighters;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;
import tecrys.data.utils.OMMSettings;

import java.awt.Color;
import java.util.*;

public class omm_DroneSwarmManager implements AdvanceableListener, WeaponRangeModifier {

    public final ShipAPI mothership;
    private boolean firstFrame = true;

    private final Map<String, Boolean> wasSelectedLastFrameMap = new HashMap<>();

    // Performance Caches
    private final Map<String, String> cooldownKeys = new HashMap<>();
    private final Map<String, String> globalQueueKeys = new HashMap<>();
    private final List<String> validModsToCopy = new ArrayList<>();
    private String validModsString = "";
    private final IntervalUtil refitInterval = new IntervalUtil(0.25f, 0.35f);

    public static class PodConfig {
        public final String wingId;
        public final String slotId;
        public final float defaultAngle;
        public final float defaultDistBetweenClusters;
        public final float defaultDistFromCenter;

        public PodConfig(String wingId, String slotId, float defaultAngle, float defaultDistBetweenClusters, float defaultDistFromCenter) {
            this.wingId = wingId;
            this.slotId = slotId;
            this.defaultAngle = defaultAngle;
            this.defaultDistBetweenClusters = defaultDistBetweenClusters;
            this.defaultDistFromCenter = defaultDistFromCenter;
        }

        public PositionParams getParams(ShipAPI mothership) {
            float angle = defaultAngle;
            float distClusters = defaultDistBetweenClusters;
            float distCenter = defaultDistFromCenter;

            String hullId = mothership.getHullSpec().getHullId();

            if (wingId.equals("omm_synergypod_wing")) {
                if (hullId.startsWith("omm_koura")) {
                    angle = 42f; distClusters = 62f; distCenter = 30f;
                } else if (hullId.startsWith("omm_nathantia")) {
                    angle = 140f; distClusters = 340f; distCenter = 30f;
                }
            } else if (wingId.equals("omm_smallpod_wing")) {
                if (hullId.startsWith("omm_koura")) {
                    angle = 100f; distClusters = 100f; distCenter = 30f;
                }
            } else if (wingId.equals("omm_compositepod_wing")) {
                if (hullId.startsWith("omm_sandhopper")) {
                    angle = 42f; distClusters = 62f; distCenter = 30f;
                } else if (hullId.startsWith("omm_nathantia")) {
                    angle = 110f; distClusters = 180f; distCenter = 30f;
                }
            } else if (wingId.equals("omm_largepod_wing")) {
                if (hullId.startsWith("omm_nathantia")) {
                    angle = 135f; distClusters = 240f; distCenter = 30f;
                }
            }

            return new PositionParams(angle, distClusters, distCenter);
        }
    }

    public static class PositionParams {
        public final float angleFromBasepos;
        public final float distBetweenClusters;
        public final float distFromClusterCenterToDrone;

        public PositionParams(float angleFromBasepos, float distBetweenClusters, float distFromClusterCenterToDrone) {
            this.angleFromBasepos = angleFromBasepos;
            this.distBetweenClusters = distBetweenClusters;
            this.distFromClusterCenterToDrone = distFromClusterCenterToDrone;
        }
    }

    private static final List<PodConfig> POD_CONFIGS = new ArrayList<>();
    private static final Set<String> ALL_DUMMY_SLOT_IDS = new HashSet<>();
    private static final Set<String> EXCLUDED_HULLMOD_TAGS = new HashSet<>(Arrays.asList(
            "shields", "phase", "civ_package", "hide_in_codex", "defensive",
            "engines", "special", "req_spaceport", "codex_require_related",
            "dmod", "threat", "fragment", "shrouded", "no_drop", "no_drop_salvage"
    ));

    static {
        POD_CONFIGS.add(new PodConfig("omm_synergypod_wing", "synergyslot", 130f, 140f, 30f));
        POD_CONFIGS.add(new PodConfig("omm_smallpod_wing", "smlhybridslot", 130f, 140f, 30f));
        POD_CONFIGS.add(new PodConfig("omm_compositepod_wing", "compositeslot", 150f, 210f, 30f));
        POD_CONFIGS.add(new PodConfig("omm_largepod_wing", "largeslot", -320f, -320f, 60f));
        POD_CONFIGS.add(new PodConfig("omm_missilepod_wing", "smlmissileslot", 270f, 80f, 30f));
        POD_CONFIGS.add(new PodConfig("omm_pddrone_wing", "pdslot", 0f, 0f, 0f));

        for (PodConfig config : POD_CONFIGS) {
            ALL_DUMMY_SLOT_IDS.add(config.slotId);
        }
    }

    public omm_DroneSwarmManager(ShipAPI mothership) {
        this.mothership = mothership;
        String msId = mothership.getFleetMember() != null ? mothership.getFleetMember().getId() : mothership.getId();

        for (PodConfig config : POD_CONFIGS) {
            wasSelectedLastFrameMap.put(config.slotId, false);
            cooldownKeys.put(config.slotId, "omm_cooldown_" + msId + "_" + config.slotId);
            globalQueueKeys.put(config.slotId, config.wingId.equals("omm_pddrone_wing") ? "omm_global_pd_queue" : "omm_global_next_refit_time_" + config.slotId);
        }
    }

    private List<FighterWingAPI> getRelevantWings(String wingId) {
        List<FighterWingAPI> wings = new ArrayList<>();
        for (FighterWingAPI wing : mothership.getAllWings()) {
            if (wing.getSpec().getId().equals(wingId)) {
                wings.add(wing);
            }
        }
        return wings;
    }

    public Vector2f getDesiredPosition(ShipAPI drone) {
        PodConfig config = getConfigForDrone(drone);
        if (config == null) return drone.getLocation();

        List<FighterWingAPI> relevantWings = getRelevantWings(config.wingId);
        PositionParams params = config.getParams(mothership);

        int wingIndex = relevantWings.indexOf(drone.getWing());
        int indexInWing = drone.getWing().getWingMembers().indexOf(drone);
        int oddOrEvenNoOfWings = relevantWings.size() % 2;
        float anglebetweenDrones = 360f / drone.getWing().getSpec().getNumFighters();

        float angleFromBasepos = params.angleFromBasepos;
        float distBetweenClusters = params.distBetweenClusters;
        float distFromClusterCenterToDrone = params.distFromClusterCenterToDrone;

        float facing = mothership.isPullBackFighters() ?
                mothership.getFacing() :
                VectorUtils.getAngle(mothership.getShieldCenterEvenIfNoShield(), mothership.getMouseTarget());

        Vector2f basePos = MathUtils.getPointOnCircumference(
                mothership.getShieldCenterEvenIfNoShield(),
                mothership.getShieldRadiusEvenIfNoShield(),
                facing
        );

        Vector2f clusterCenter;
        if (oddOrEvenNoOfWings == 0) {
            if (wingIndex % 2 == 0) {
                clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * wingIndex), facing + angleFromBasepos);
            } else {
                clusterCenter = MathUtils.getPointOnCircumference(basePos, distBetweenClusters + (distBetweenClusters * (wingIndex - 1)), facing - angleFromBasepos);
            }
        } else {
            if (wingIndex == 0) {
                clusterCenter = MathUtils.getPointOnCircumference(basePos, 0, facing + angleFromBasepos);
            } else if (wingIndex % 2 == 0) {
                clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * (wingIndex - 1)) * 2f, facing + angleFromBasepos);
            } else {
                clusterCenter = MathUtils.getPointOnCircumference(basePos, (distBetweenClusters * wingIndex) * 2f, facing - angleFromBasepos);
            }
        }

        return MathUtils.getPointOnCircumference(clusterCenter, distFromClusterCenterToDrone, facing + (indexInWing * anglebetweenDrones));
    }

    public float getDesiredFacing(ShipAPI drone) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            boolean isPlayerFlagship = (engine.getPlayerShip() == this.mothership);
            boolean isPlayerControlled = isPlayerFlagship && engine.isUIAutopilotOn();

            PodConfig config = getConfigForDrone(drone);
            if (config != null) {
                WeaponAPI mWep = getMothershipWeaponForSlot(config.slotId);
                if (mWep != null) {
                    WeaponGroupAPI mGroup = mothership.getWeaponGroupFor(mWep);
                    boolean isSelected = isPlayerFlagship && (this.mothership.getSelectedGroupAPI() == mGroup);

                    if (isPlayerControlled && isSelected) {
                        return VectorUtils.getAngle(drone.getLocation(), mothership.getMouseTarget());
                    }
                }
            }

            if (mothership.areAnyEnemiesInRange() && AIUtils.getNearestEnemy(mothership) != null) {
                if (!AIUtils.getNearestEnemy(mothership).getHullSize().equals(ShipAPI.HullSize.FIGHTER)) {
                    return VectorUtils.getAngle(drone.getLocation(), AIUtils.getNearestEnemy(mothership).getLocation());
                }
            }
            if (mothership.isPullBackFighters() && !mothership.areAnyEnemiesInRange()) {
                return mothership.getFacing();
            }
        }
        return VectorUtils.getAngle(drone.getLocation(), mothership.getMouseTarget());
    }

    @Override
    public void advance(float amount) {
        ShipAPI ship = this.mothership;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!ship.isAlive() || engine == null) return;

        if (firstFrame) {
            List<String> allMothershipMods = new ArrayList<>();
            allMothershipMods.addAll(ship.getVariant().getNonBuiltInHullmods());
            allMothershipMods.addAll(ship.getVariant().getSMods());

            for (String mod : allMothershipMods) {
                HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(mod);
                if (modSpec != null && !modSpec.isHiddenEverywhere()) {
                    boolean hasExcludedTag = false;
                    for (String tag : EXCLUDED_HULLMOD_TAGS) {
                        if (modSpec.hasTag(tag)) {
                            hasExcludedTag = true;
                            break;
                        }
                    }
                    if (!hasExcludedTag) {
                        validModsToCopy.add(mod);
                    }
                }
            }
            validModsString = validModsToCopy.toString();
            firstFrame = false;
        }

        refitInterval.advance(amount);
        boolean runRefitChecks = refitInterval.intervalElapsed();

        boolean isPlayerFlagship = (engine.getPlayerShip() == this.mothership);
        boolean isPlayerControlled = isPlayerFlagship && engine.isUIAutopilotOn();

        for (PodConfig config : POD_CONFIGS) {

            boolean isVanillaPD = config.wingId.equals("omm_pddrone_wing");

            WeaponAPI mothershipWep = getMothershipWeaponForSlot(config.slotId);
            if (mothershipWep == null) continue;

            mothershipWep.ensureClonedSpec();
            mothershipWep.getSpec().setMaxRange(0f);
            mothershipWep.setForceNoFireOneFrame(true);

            WeaponGroupAPI mothershipGroup = ship.getWeaponGroupFor(mothershipWep);
            boolean isCurrentlySelected = isPlayerFlagship && (this.mothership.getSelectedGroupAPI() == mothershipGroup);

            if (runRefitChecks) {
                wasSelectedLastFrameMap.put(config.slotId, isCurrentlySelected);
            }

            String expectedWepId = ship.getVariant().getWeaponId(config.slotId);
            if (expectedWepId == null) expectedWepId = "No weapon";
            String expectedState = expectedWepId + "_" + validModsString;

            List<FighterWingAPI> wings = getRelevantWings(config.wingId);

            int currentAmmo = 0;
            int maxAmmoSpec = 0;
            float currentReload = 0f;
            boolean foundAmmo = false;

            for (FighterWingAPI wing : wings) {
                if (wing == null || wing.getWingMembers() == null) continue;
                for (ShipAPI fighter : wing.getWingMembers()) {
                    if (fighter == null || !fighter.isAlive()) continue;
                    for (WeaponAPI w : fighter.getAllWeapons()) {
                        if (w.getSlot().getId().equals(config.slotId) && w.usesAmmo()) {
                            foundAmmo = true;
                            AmmoTrackerAPI tracker = w.getAmmoTracker();
                            if (tracker != null && tracker.getAmmo() >= currentAmmo) {
                                currentAmmo = tracker.getAmmo();
                                maxAmmoSpec = w.getSpec().getMaxAmmo();
                                currentReload = tracker.getReloadProgress();
                            }
                        }
                    }
                }
            }

            if (foundAmmo && mothershipWep.getAmmoTracker() != null) {
                mothershipWep.getSpec().setMaxAmmo(maxAmmoSpec);
                mothershipWep.getAmmoTracker().setAmmo(currentAmmo);
                mothershipWep.getAmmoTracker().setReloadProgress(currentReload);
            }

            for (FighterWingAPI wing : wings) {
                if (wing == null || wing.getWingMembers() == null || wing.getWingMembers().isEmpty()) continue;

                for (int i = 0; i < wing.getWingMembers().size(); i++) {
                    ShipAPI fighter = wing.getWingMembers().get(i);
                    if (fighter == null || !fighter.isAlive() || fighter.getAllWeapons().isEmpty()) continue;

                    if (fighter.isLiftingOff() || fighter.isLanding()) {
                        continue;
                    }

                    WeaponAPI droneWep = null;
                    WeaponAPI laswep = null;
                    for (WeaponAPI w : fighter.getAllWeapons()) {
                        if (w.getSlot() != null && w.getSlot().getId() != null) {
                            if (w.getSlot().getId().equals(config.slotId)) droneWep = w;
                            if (w.getSlot().getId().equals("omm_laser")) laswep = w;
                        }
                    }

                    if (runRefitChecks) {
                        String currentState = (String) fighter.getCustomData().get("omm_fighter_state");

                        if (currentState == null) {
                            String currentWepId = droneWep != null ? droneWep.getId() : "No weapon";
                            Collection<String> currentMods = new ArrayList<>(fighter.getVariant().getNonBuiltInHullmods());
                            boolean hullmodsMatch = currentMods.size() == validModsToCopy.size() && currentMods.containsAll(validModsToCopy);

                            if (currentWepId.equals(expectedWepId) && hullmodsMatch) {
                                currentState = expectedState;
                                fighter.getCustomData().put("omm_fighter_state", expectedState);
                            }
                        }

                        if (!expectedState.equals(currentState)) {
                            if (fighter.getCustomData().containsKey("omm_ordered_return")) {
                                fighter.resetDefaultAI();
                                continue;
                            }

                            float currentTime = engine.getTotalElapsedTime(false);
                            String cooldownKey = cooldownKeys.get(config.slotId);
                            Float bayCooldown = (Float) engine.getCustomData().get(cooldownKey);

                            String globalQueueKey = globalQueueKeys.get(config.slotId);
                            Float nextRefitTime = (Float) engine.getCustomData().get(globalQueueKey);
                            if (nextRefitTime == null) nextRefitTime = 0f;

                            if ((bayCooldown != null && currentTime < bayCooldown) || currentTime < nextRefitTime) {
                                if (!isVanillaPD) fighter.setShipAI(null);
                            } else {
                                fighter.resetDefaultAI();

                                if (isVanillaPD) {
                                    engine.getCustomData().put(globalQueueKey, currentTime + MathUtils.getRandomNumberInRange(1.0f, 2.5f));
                                    engine.getCustomData().put(cooldownKey, currentTime + 2.0f);
                                } else {
                                    engine.getCustomData().put(globalQueueKey, currentTime + MathUtils.getRandomNumberInRange(3.0f, 6.0f));
                                    engine.getCustomData().put(cooldownKey, currentTime + 4.0f);
                                }

                                MutableShipStatsAPI stats = fighter.getMutableStats();
                                ShipVariantAPI newVariant = stats.getVariant().clone();

                                stats.getVariant().setOriginalVariant(null);
                                if (fighter.getFleetMember() != null) {
                                    fighter.getFleetMember().setVariant(newVariant, true, true);
                                }

                                ship.syncWeaponDecalsWithArmorDamage();

                                stats.getVariant().clearSlot(config.slotId);
                                if (!expectedWepId.equals("No weapon")) {
                                    stats.getVariant().addWeapon(config.slotId, expectedWepId);
                                    stats.getVariant().getWeaponSpec(config.slotId).addTag("FIRE_WHEN_INEFFICIENT");
                                }

                                for (String existingMod : new ArrayList<>(stats.getVariant().getNonBuiltInHullmods())) {
                                    stats.getVariant().removeMod(existingMod);
                                }
                                for (String mod : validModsToCopy) {
                                    stats.getVariant().addMod(mod);
                                }

                                fighter.getCustomData().put("omm_fighter_state", expectedState);
                                fighter.getCustomData().remove("omm_wep_group_index");

                                wing.orderReturn(fighter);
                                fighter.getCustomData().put("omm_ordered_return", true);

                                continue;
                            }
                        } else {
                            if (!isVanillaPD) fighter.setShipAI(null);
                            fighter.getCustomData().remove("omm_ordered_return");
                        }
                    } else {
                        if (fighter.getCustomData().containsKey("omm_ordered_return")) {
                            continue;
                        }
                    }

                    if (isVanillaPD) {
                        continue;
                    }

                    // --- COMBAT EXECUTION ---
                    if (droneWep != null) {

                        fighter.getMutableStats().getBallisticWeaponRangeBonus().unmodifyFlat("omm_range_failsafe");
                        fighter.getMutableStats().getEnergyWeaponRangeBonus().unmodifyFlat("omm_range_failsafe");
                        fighter.getMutableStats().getMissileWeaponRangeBonus().unmodifyFlat("omm_range_failsafe");

                        float baseRange = droneWep.getSpec().getMaxRange();
                        float targetMothershipRange = baseRange;
                        MutableShipStatsAPI mStats = this.mothership.getMutableStats();

                        if (droneWep.getType() == WeaponAPI.WeaponType.BALLISTIC) {
                            targetMothershipRange = mStats.getBallisticWeaponRangeBonus().computeEffective(targetMothershipRange);
                        } else if (droneWep.getType() == WeaponAPI.WeaponType.ENERGY) {
                            targetMothershipRange = mStats.getEnergyWeaponRangeBonus().computeEffective(targetMothershipRange);
                        } else if (droneWep.getType() == WeaponAPI.WeaponType.MISSILE) {
                            targetMothershipRange = mStats.getMissileWeaponRangeBonus().computeEffective(targetMothershipRange);
                        }

                        if (droneWep.isBeam()) {
                            targetMothershipRange = mStats.getBeamWeaponRangeBonus().computeEffective(targetMothershipRange);
                        }

                        float currentDroneRange = droneWep.getRange();
                        float rangeDiff = targetMothershipRange - currentDroneRange;

                        if (Math.abs(rangeDiff) > 0.1f) {
                            if (droneWep.getType() == WeaponAPI.WeaponType.BALLISTIC) {
                                fighter.getMutableStats().getBallisticWeaponRangeBonus().modifyFlat("omm_range_failsafe", rangeDiff, "Mothership Sync Failsafe");
                            } else if (droneWep.getType() == WeaponAPI.WeaponType.ENERGY) {
                                fighter.getMutableStats().getEnergyWeaponRangeBonus().modifyFlat("omm_range_failsafe", rangeDiff, "Mothership Sync Failsafe");
                            } else if (droneWep.getType() == WeaponAPI.WeaponType.MISSILE) {
                                fighter.getMutableStats().getMissileWeaponRangeBonus().modifyFlat("omm_range_failsafe", rangeDiff, "Mothership Sync Failsafe");
                            }
                        }

                        WeaponGroupAPI droneGroup = fighter.getWeaponGroupFor(droneWep);

                        if (mothershipGroup != null && droneGroup != null) {

                            boolean shouldAutofire = isPlayerControlled ? (mothershipGroup.isAutofiring() && !isCurrentlySelected) : true;

                            Integer groupIndex = (Integer) fighter.getCustomData().get("omm_wep_group_index");
                            if (groupIndex == null) {
                                groupIndex = fighter.getWeaponGroupsCopy().indexOf(droneGroup);
                                fighter.getCustomData().put("omm_wep_group_index", groupIndex);
                            }

                            if (droneGroup.isAutofiring() != shouldAutofire) {
                                fighter.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, groupIndex);
                            }

                            boolean wasSelected = fighter.getCustomData().containsKey("omm_was_selected");

                            if (isCurrentlySelected && !wasSelected) {
                                droneWep.beginSelectionFlash();
                                if (laswep != null) laswep.beginSelectionFlash();
                                fighter.addAfterimage(new Color(255, 255, 255, 175), 0f, 0f, 0f, 0f, 0f, 0f, 0.1f, 0.3f, true, true, true);
                                fighter.getCustomData().put("omm_was_selected", true);
                            } else if (!isCurrentlySelected && wasSelected) {
                                fighter.getCustomData().remove("omm_was_selected");
                            }

                            if (isPlayerControlled && isCurrentlySelected && !engine.isPaused()) {
                                Vector2f mousePos = this.mothership.getMouseTarget();
                                float angleToMouse = VectorUtils.getAngle(fighter.getLocation(), mousePos);

                                if (laswep != null) {
                                    laswep.setForceFireOneFrame(true);
                                    laswep.setCurrAngle(angleToMouse);
                                    laswep.ensureClonedSpec();

                                    float targetRange = droneWep.getRange();
                                    laswep.getSpec().setMaxRange(0f);
                                    float flatBonus = laswep.getRange();

                                    laswep.getSpec().setMaxRange(100f);
                                    float multiplier = (laswep.getRange() - flatBonus) / 100f;
                                    if (multiplier <= 0.001f) multiplier = 1f;

                                    float correctedBaseRange = (targetRange - flatBonus) / multiplier;
                                    laswep.getSpec().setMaxRange(Math.max(0f, correctedBaseRange));
                                }

                                float diff = MathUtils.getShortestRotation(fighter.getFacing(), angleToMouse);
                                float maxTurn = fighter.getMaxTurnRate() * amount;
                                fighter.setFacing(fighter.getFacing() + MathUtils.clamp(diff, -maxTurn, maxTurn));
                                droneWep.setCurrAngle(angleToMouse);

                                if (!engine.isUIShowingDialog() && !engine.getCombatUI().isShowingCommandUI()) {
                                    boolean shouldFire = false;

                                    if (droneWep.getType() == WeaponAPI.WeaponType.MISSILE) {
                                        if (OMMSettings.missile_key == 0) {
                                            if (Mouse.isButtonDown(2) || Mouse.isButtonDown(0)) shouldFire = true;
                                        } else {
                                            if (Keyboard.isKeyDown(OMMSettings.missile_key) || Mouse.isButtonDown(0)) shouldFire = true;
                                        }
                                    } else {
                                        if (Mouse.isButtonDown(0)) shouldFire = true;
                                    }

                                    if (shouldFire && !this.mothership.getFluxTracker().isOverloadedOrVenting()) {
                                        fighter.giveCommand(ShipCommand.FIRE, mousePos, groupIndex);
                                        droneWep.setForceFireOneFrame(true);
                                    }
                                }
                            }
                        }
                    }

                    if (this.mothership.getFluxTracker().isOverloaded()) {
                        if (!fighter.getFluxTracker().isOverloaded()) {
                            fighter.getFluxTracker().forceOverload(this.mothership.getFluxTracker().getOverloadTimeRemaining());
                        }
                    } else if (fighter.getFluxTracker().isOverloaded()) {
                        fighter.getFluxTracker().stopOverload();
                    }

                    if (this.mothership.getFluxTracker().isVenting() && droneWep != null) {
                        droneWep.setForceNoFireOneFrame(true);
                    }
                }
            }
        }
    }

    private WeaponAPI getMothershipWeaponForSlot(String slotId) {
        for (WeaponAPI w : mothership.getAllWeapons()) {
            if (w.getSlot().getId().equals(slotId)) {
                return w;
            }
        }
        return null;
    }

    private PodConfig getConfigForDrone(ShipAPI drone) {
        if (drone.getWing() == null) return null;
        String wingId = drone.getWing().getSpec().getId();
        for (PodConfig config : POD_CONFIGS) {
            if (config.wingId.equals(wingId)) {
                return config;
            }
        }
        return null;
    }

    @Override
    public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
        return 0f;
    }

    @Override
    public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
        if (weapon.getSlot() != null && ALL_DUMMY_SLOT_IDS.contains(weapon.getSlot().getId())) {
            return 0f;
        }
        return 1f;
    }

    @Override
    public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
        if (weapon.getSlot() != null && ALL_DUMMY_SLOT_IDS.contains(weapon.getSlot().getId())) {
            return -100000f;
        }
        return 0f;
    }
}