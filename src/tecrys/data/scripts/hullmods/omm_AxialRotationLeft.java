package tecrys.data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class omm_AxialRotationLeft extends BaseHullMod {

	public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}
	
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	}


	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return true;
	}


	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		if (ship.getParentStation() == null) return;
		ship.setFacing(ship.getParentStation().getFacing()* (-1f));
//		ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
	}
	

	
	
}
