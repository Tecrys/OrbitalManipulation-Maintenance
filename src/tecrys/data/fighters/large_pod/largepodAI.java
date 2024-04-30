package tecrys.data.fighters.large_pod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import tecrys.data.utils.pidController;
import tecrys.data.utils.utils;

import java.util.List;

public class largepodAI extends BaseEveryFrameCombatPlugin {

    private final pidController controller;
    private final ShipAPI mothership;
    private final ShipAPI drone;

    public largepodAI(ShipAPI drone, ShipAPI mothership) {

        this.drone = drone;
        this.mothership = mothership;
        this.controller = new pidController(20f, 2f, 6f, 0.5f);

    }

    //todo replace these with a shipAIPlugin that copies the fighter's original AI & delegates the normal operation of the ship to them
    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        if (Global.getCombatEngine().isPaused()) return;
        largepodManager manager = utils.getFirstListenerOfClass(mothership, largepodManager.class);
        if (manager == null) return;

        controller.move(manager.getDesiredPosition(drone), drone);
        controller.rotate(manager.getDesiredFacing(drone), drone);

        if (!manager.drones.contains(drone)) {
            Global.getCombatEngine().removePlugin(this);
            Global.getCombatEngine().removeEntity(drone);
        }
    }
}