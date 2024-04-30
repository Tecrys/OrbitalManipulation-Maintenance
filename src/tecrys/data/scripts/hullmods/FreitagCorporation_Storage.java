package tecrys.data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import java.util.List;

public class FreitagCorporation_Storage extends BaseHullMod {

    private static final float STORAGE_ADDITIONAL_BERTHING = 500f;
    private static final float STORAGE_AUXILIARY_FUEL_TANKS = 500f;
    private static final float STORAGE_EXPANDED_CARGO_HOLDS = 400f;

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getVariant() != null) {
            if (stats.getVariant().hasHullMod(HullMods.AUXILIARY_FUEL_TANKS)) {
                stats.getFuelMod().modifyFlat(id, STORAGE_AUXILIARY_FUEL_TANKS);
                stats.getDynamic().getMod(Stats.MAX_LOGISTICS_HULLMODS_MOD).modifyFlat(id, 1);
            } else if (stats.getVariant().hasHullMod(HullMods.EXPANDED_CARGO_HOLDS)) {
                stats.getCargoMod().modifyFlat(id, STORAGE_EXPANDED_CARGO_HOLDS);
                stats.getDynamic().getMod(Stats.MAX_LOGISTICS_HULLMODS_MOD).modifyFlat(id, 1);

            } else if (stats.getVariant().hasHullMod(HullMods.ADDITIONAL_BERTHING)) {
                stats.getMaxCrewMod().modifyFlat(id, STORAGE_ADDITIONAL_BERTHING);
                stats.getDynamic().getMod(Stats.MAX_LOGISTICS_HULLMODS_MOD).modifyFlat(id, 1);

            }
        }

    }
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
                List<WeaponAPI> decos = ship.getAllWeapons();
                                for (WeaponAPI deco : decos) {
                    if (deco.getSlot().getId().equals("SesarmaDeco")) {
                                if (ship.getVariant() != null) {
                        if (ship.getVariant().hasHullMod(HullMods.AUXILIARY_FUEL_TANKS)){
                           deco.getAnimation().setFrame(01); 
                        }
                                                                        if (ship.getVariant().hasHullMod(HullMods.ADDITIONAL_BERTHING)){
                           deco.getAnimation().setFrame(02); 
                        }
                                                if (ship.getVariant().hasHullMod(HullMods.EXPANDED_CARGO_HOLDS)){
                           deco.getAnimation().setFrame(03); 
                        }
                        }
                                }
                    }
                                }
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) {
            return "" + ((Float) STORAGE_AUXILIARY_FUEL_TANKS).intValue();
        }
        if (index == 1) {
            return "" + ((Float) STORAGE_EXPANDED_CARGO_HOLDS).intValue();
        }
        if (index == 2) {
            return "" + ((Float) STORAGE_ADDITIONAL_BERTHING).intValue();
        }
        return null;
    }

}
