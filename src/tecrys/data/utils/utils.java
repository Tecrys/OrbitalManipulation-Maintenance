package tecrys.data.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.List;

//original script by ruddygreat
public class utils {

    public static <T> T getFirstListenerOfClass(ShipAPI ship, Class listenerClass) {

        if (!ship.hasListenerOfClass(listenerClass)) {
            return null;
        }

        Object listener = ship.getListeners(listenerClass).get(0);

        return (T) listener;
    }

    public static boolean playerHasCommodity(String id) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            return false;
        }
        List<CargoStackAPI> playerCargoStacks = playerFleet.getCargo().getStacksCopy();

        for (CargoStackAPI cargoStack : playerCargoStacks) {
            if (cargoStack.isCommodityStack() && cargoStack.getCommodityId().equals(id) && cargoStack.getSize() > 0) {
                return true;
            }
        }

        return false;
    }

    public static void removePlayerCommodity(String id) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            return;
        }
        List<CargoStackAPI> playerCargoStacks = playerFleet.getCargo().getStacksCopy();

        for (CargoStackAPI cargoStack : playerCargoStacks) {
            if (cargoStack.isCommodityStack() && cargoStack.getCommodityId().equals(id)) {
                cargoStack.subtract(1);
                if (cargoStack.getSize() <= 0) {
                    playerFleet.getCargo().removeStack(cargoStack);
                }
                return;
            }
        }
    }

    public static void addPlayerCommodity(String commodityId, int amount) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            return;
        }
        CargoAPI playerFleetCargo = playerFleet.getCargo();
        if (commodityId.equals("fuel")) {
            playerFleetCargo.addFuel(amount);
        } else if (commodityId.equals("supplies")) {
            playerFleetCargo.addSupplies(amount);
        } else if (commodityId.equals("crew")) {
            playerFleetCargo.removeCrew(amount);
        } else {
            playerFleetCargo.addCommodity(commodityId, amount);
        }
    }
}
