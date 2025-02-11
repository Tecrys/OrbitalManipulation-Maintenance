package tecrys.data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exerelin.campaign.SectorManager;
import lunalib.lunaSettings.LunaSettings;
import tecrys.data.scripts.world.subsidiaries.FreitagHQ;

public class ommGen implements SectorGeneratorPlugin {

    @Override
    public void generate(SectorAPI sector) {
//        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("freitag_corporation");
//        initFactionRelationships(sector);
        FactionAPI independent = sector.getFaction(Factions.INDEPENDENT); 
        FactionAPI freitagcorporation = sector.getFaction("freitag_corporation");
        freitagcorporation.setRelationship(independent.getId(), RepLevel.COOPERATIVE);
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        boolean ommFaction = LunaSettings.getBoolean("OMM", "omm_faction");
        boolean indiesGetOmmTech = LunaSettings.getBoolean("OMM","omm_indie_tech_share");

        if(!ommFaction){
            freitagcorporation.setShowInIntelTab(false);
        }

        //TODO when OMM adds more things add to this list
        if(indiesGetOmmTech){
            FactionAPI independents = Global.getSector().getFaction(Factions.INDEPENDENT);
            for(String ship:freitagcorporation.getKnownShips())
                independents.addKnownShip(ship,false);
        }

        if(!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            (new FreitagHQ()).generate(sector, ommFaction);
        }

    }
}
