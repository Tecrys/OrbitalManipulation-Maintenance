package tecrys.data.fighters;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.PIDController;
import tecrys.data.utils.utils;

import java.util.List;

public class omm_DroneSwarmAI extends BaseEveryFrameCombatPlugin {

    private final PIDController controller;
    private final ShipAPI mothership;
    private final ShipAPI drone;

    public omm_DroneSwarmAI(ShipAPI drone, ShipAPI mothership) {
        this.drone = drone;
        this.mothership = mothership;
        this.controller = new PIDController(10f, 3f, 3f, 1f);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine().isPaused()) return;

        if (drone == null || !drone.isAlive()) {
            Global.getCombatEngine().removePlugin(this);
            return;
        }

        if (mothership == null || !mothership.isAlive()) return;

        if (drone.isLanding() || drone.isLiftingOff() || drone.getCustomData().containsKey("omm_ordered_return")) {
            return;
        }

        omm_DroneSwarmManager manager = utils.getFirstListenerOfClass(mothership, omm_DroneSwarmManager.class);
        if (manager == null) return;

        // --- ANTI-DRAG & PHYSICS SYNC ---

        // 1. Time Dilation Sync
        // Cleanly applies and removes the time modifier so it doesn't linger after Temporal Shell ends.
        float msTimeMult = mothership.getMutableStats().getTimeMult().getModifiedValue();
        if (msTimeMult != 1f) {
            drone.getMutableStats().getTimeMult().modifyMult("omm_time_sync", msTimeMult);
        } else {
            drone.getMutableStats().getTimeMult().unmodify("omm_time_sync");
        }

        // 2. Proportional Speed Sync (Double-Dip Fix)
        // By dividing the mothership's global velocity by its time multiplier, we calculate its true "local" speed.
        // This prevents the PID controller from going insane and oscillating when both buffs are active at once.
        float localSpeed = mothership.getVelocity().length() / Math.max(0.1f, msTimeMult);

        drone.getMutableStats().getMaxSpeed().modifyFlat("omm_speed_sync", localSpeed);
        drone.getMutableStats().getAcceleration().modifyFlat("omm_accel_sync", localSpeed);
        drone.getMutableStats().getDeceleration().modifyFlat("omm_accel_sync", localSpeed);

        Vector2f desiredPos = manager.getDesiredPosition(drone);

        // 3. Teleport Snapping
        if (MathUtils.getDistance(drone.getLocation(), desiredPos) > 400f) {
            drone.getLocation().set(desiredPos.x, desiredPos.y);
            drone.getVelocity().set(mothership.getVelocity().x, mothership.getVelocity().y);
        }
        // --------------------------------

        controller.move(desiredPos, drone);
        controller.rotate(manager.getDesiredFacing(drone), drone);
    }
}