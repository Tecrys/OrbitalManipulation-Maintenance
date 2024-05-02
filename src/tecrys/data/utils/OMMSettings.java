package tecrys.data.utils;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;


public class OMMSettings implements LunaSettingsListener {



        public static int missile_key = LunaSettings.getInt("OMM", "omm_missile_key");

    //Gets called whenever settings are saved in the campaign or the main menu.
    @Override
    public void settingsChanged(String modID) {
        missile_key = LunaSettings.getInt("OMM", "omm_missile_key");
    }
}