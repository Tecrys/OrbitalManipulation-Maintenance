package tecrys.data.shipsystems.scripts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.DroneStrikeStatsAIInfoProvider;
import com.fs.starfarer.api.input.InputEventAPI;
import org.dark.shaders.distortion.DistortionAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.Misc;

public class omm_laserwebmirv extends BaseShipSystemScript {



    protected EveryFrameCombatPlugin createTemporalShellBuffPlugin(final ShipAPI target, final float timeEffect) {
        return new BaseEveryFrameCombatPlugin() {
            float elapsed = 0f;
            int state = 0;
            float inc = 0;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused()) {
                    return;
                }
                boolean player = target == Global.getCombatEngine().getPlayerShip();

            }
        };
    }

}








