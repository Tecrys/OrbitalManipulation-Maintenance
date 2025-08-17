package tecrys.data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.sqrt


class omm_stasisOnHitEffect(): OnHitEffectPlugin {
    companion object{
        const val DURATION = 5f
        const val EFFECT_MULT = 0f
        const val EFFECT_ID = "omm_stasisOnHitEffect"
    }

    protected val Interval = IntervalUtil(0.0f, 0.1f)
    private var remainingDuration = DURATION

    override fun onHit(
        projectile: DamagingProjectileAPI?,
        target: CombatEntityAPI?,
        point: Vector2f?,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI?,
        engine: CombatEngineAPI?
    ) {
        if(shieldHit) return

        if (projectile?.didDamage() == true) {

        val ship = target as? ShipAPI ?: return
        engine ?: return
        engine.addPlugin(omm_stasisOnHitScript(ship, engine))}
    }

    inner class omm_stasisOnHitScript(private var ship: ShipAPI, private var engine: CombatEngineAPI?): BaseEveryFrameCombatPlugin() {

        private var effectLevel = 0.1f
        val JITTER_COLOR: Color = Color(90, 165, 255, 55)

        val JITTER_UNDER_COLOR: Color = Color(90, 165, 255, 155)
        private val sprite: SpriteAPI = Global.getSettings().getSprite("misc", "omm_stasis") // sprite itself

        // probably ok to put this here as we only render one at a time per beam anyway
        private val size = Vector2f(ship.spriteAPI.height, ship.spriteAPI.width)


        protected val Interval = IntervalUtil(0.3f, 0.3f)
        public var cloud = false
        private val statsToModify = ship.mutableStats.run {
            listOf(acceleration, turnAcceleration, maxTurnRate, ballisticRoFMult, energyRoFMult, missileRoFMult, weaponDamageTakenMult,
                empDamageTakenMult, hullDamageTakenMult, armorDamageTakenMult, shieldDamageTakenMult, maxSpeed, crewLossMult,
                weaponRangeThreshold, weaponRangeMultPastThreshold)
        }
        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {


            this.Interval.advance(amount)
            remainingDuration -= amount
            if(remainingDuration <= 0f){
                statsToModify.forEach { it.unmodify(EFFECT_ID) }

                engine?.removePlugin(this)
                return
            }

            statsToModify.forEach { it.modifyMult(EFFECT_ID, EFFECT_MULT) }
            ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK)
            ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM)
            ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX)
            ship.blockCommandForOneFrame(ShipCommand.FIRE)
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE)
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS)
            var jitterLevel: Float = effectLevel * amount
            var jitterRangeBonus = 0f
            val maxRangeBonus = 10f

            jitterLevel = sqrt(jitterLevel.toDouble()).toFloat()
            effectLevel *= effectLevel
            ship.setJitter(this, JITTER_COLOR, 1f, 3, 0f, 0 + jitterRangeBonus);
            ship.setJitterUnder(this, JITTER_UNDER_COLOR, 1f, 25, 0f, 7f + jitterRangeBonus);

//            MagicRender.singleframe(
//                sprite,  // spriteapi
//                ship.shieldCenterEvenIfNoShield,  // beam ray end
//                size,  // sprite dimensions
//                ship.facing,  // current angle to render sprite at
//                Color (255,255,255,10),
//                false,
//                CombatEngineLayers.ABOVE_SHIPS_LAYER
//            )
        }
        fun kill() {

            engine?.removePlugin(this)
        }
    }


}