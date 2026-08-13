package tecrys.data.scripts.hullmods;


import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;


public class omm_fxdronehm extends BaseHullMod {




    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {


        if (ship.getShipAI() != null && ship.getShipAI().getConfig() != null) {
            ship.addTag("fx_drone");
            ship.setExplosionScale(0f);
            ship.setHulkChanceOverride(0f);
            ship.getArmorGrid().clearComponentMap(); // no damage to weapons/engines
            ship.setNoDamagedExplosions(true);
            ship.setSpawnDebris(false);
            ship.setBeingIgnored(true);
            ship.setMass(1f);

            ship.setShipCollisionSoundOverride("omm_nosound");
            ship.setAsteroidCollisionSoundOverride("omm_nosound");


        }
    }

}
