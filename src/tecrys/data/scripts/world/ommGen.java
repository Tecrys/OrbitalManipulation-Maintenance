package tecrys.data.scripts.world;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import tecrys.data.scripts.world.subsidiaries.FreitagHQ;

public class ommGen implements SectorGeneratorPlugin {

    @Override
    public void generate(SectorAPI sector) {
//        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("freitag_corporation");
//        initFactionRelationships(sector);
        FactionAPI independent = sector.getFaction(Factions.INDEPENDENT); 
        FactionAPI freitagcorporation = sector.getFaction("freitag_corporation");
        freitagcorporation.setRelationship(independent.getId(), RepLevel.COOPERATIVE);  
        (new FreitagHQ()).generate(sector);
    }
}
