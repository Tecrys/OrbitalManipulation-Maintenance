package tecrys.data.fighters.pddrone_pod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.magiclib.util.PIDController;
import tecrys.data.utils.utils;

import java.util.List;

public class pddronepodAI extends BaseEveryFrameCombatPlugin {

    private final PIDController controller;
    private final ShipAPI mothership;
    private final ShipAPI drone;

    public pddronepodAI(ShipAPI drone, ShipAPI mothership) {

        this.drone = drone;
        this.mothership = mothership;
        this.controller = new PIDController(20f, 2f, 6f, 0.5f);

    }

    //todo replace these with a shipAIPlugin that copies the fighter's original AI & delegates the normal operation of the ship to them
    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        if (Global.getCombatEngine().isPaused()) return;
        pddroneManager manager = utils.getFirstListenerOfClass(mothership, pddroneManager.class);
        if (manager == null) return;



        if (!manager.drones.contains(drone)) {
            Global.getCombatEngine().removePlugin(this);
            Global.getCombatEngine().removeEntity(drone);
        }
    }
}