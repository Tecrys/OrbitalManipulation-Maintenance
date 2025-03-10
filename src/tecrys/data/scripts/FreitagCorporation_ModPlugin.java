package tecrys.data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exerelin.campaign.SectorManager;
import lunalib.lunaSettings.LunaSettings;
import tecrys.data.scripts.world.ommGen;
import tecrys.data.scripts.ai.repairdroneai;

import tecrys.data.utils.OMMSettings;


public class FreitagCorporation_ModPlugin extends BaseModPlugin {

    public static final boolean isExerelin;

    static {
        boolean foundExerelin;
        if (Global.getSettings().getModManager().isModEnabled("nexerelin")) {
            foundExerelin = true;
        } else {
            foundExerelin = false;
        }
        isExerelin = foundExerelin;
    }

  public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
    switch (ship.getHullSpec().getHullId()) {
      case "omm_nanodrone":
        return new PluginPick(new repairdroneai(ship), CampaignPlugin.PickPriority.MOD_GENERAL);
    } 
//    if (ship.getHullSpec().getHullId().startsWith("edshipyard_wurg_"))
//      return new PluginPick(new WurgandalModuleShipAI(ship), CampaignPlugin.PickPriority.MOD_GENERAL); 
    return super.pickShipAI(member, ship);
  }
    public void onApplicationLoad(){

        LunaSettings.addSettingsListener(new OMMSettings());
    }

    @Override
    public void onNewGame() {
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");

        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
                    if (Global.getSector() == null) {
            return;
        }
            new ommGen().generate(Global.getSector());
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        //if we are in random sector, none of this code is run
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if(!haveNexerelin || SectorManager.getManager().isCorvusMode()){

            if (Global.getSector().getEntityById("freitag_hq") == null) { //star id
                new ommGen().generate(Global.getSector());
            }
            if (Global.getSector().getEconomy().getMarket("eldfell").getSubmarket("freitag_submarket") == null)
            {
                MarketAPI market = Global.getSector().getEconomy().getMarket("eldfell"); //to get the market
                if (market != null) {
                    market.addSubmarket("freitag_submarket"); //add the submarket into the market
                }
            }
            if (Global.getSector().getEconomy().getMarket("new_maxios").getSubmarket("freitag_submarket") == null)
            {
                MarketAPI market2 = Global.getSector().getEconomy().getMarket("new_maxios"); //to get the market
                if (market2 != null) {
                    market2.addSubmarket("freitag_submarket"); //add the submarket into the market
                }
            }
            if (Global.getSector().getEconomy().getMarket("ilm").getSubmarket("freitag_submarket") == null)
            {
                MarketAPI market3 = Global.getSector().getEconomy().getMarket("ilm"); //to get the market
                if (market3 != null) {
                    market3.addSubmarket("freitag_submarket"); //add the submarket into the market
                }
            }
        }
    }
}
