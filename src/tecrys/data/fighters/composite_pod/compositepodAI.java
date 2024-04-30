package tecrys.data.fighters.composite_pod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.input.InputEventAPI;
import tecrys.data.utils.pidController;
import tecrys.data.utils.utils;

import java.util.List;

public class compositepodAI extends BaseEveryFrameCombatPlugin {

    private final pidController controller;
    private final ShipAPI mothership;
    private final ShipAPI drone;

    public compositepodAI(ShipAPI drone, ShipAPI mothership) {

        this.drone = drone;
        this.mothership = mothership;
        this.controller = new pidController(10f, 3f, 3f, 1f);

    }

    //todo replace these with a shipAIPlugin that copies the fighter's original AI & delegates the normal operation of the ship to them
    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        if (Global.getCombatEngine().isPaused()) return;
        compositepodManager manager = utils.getFirstListenerOfClass(mothership, compositepodManager.class);
        if (manager == null) return;

        //always keep the shield up


        controller.move(manager.getDesiredPosition(drone), drone);
      controller.rotate(manager.getDesiredFacing(drone), drone);

        if (!manager.dronescomposite.contains(drone)) {
            Global.getCombatEngine().removePlugin(this);
            Global.getCombatEngine().removeEntity(drone);
        }
    }
}
