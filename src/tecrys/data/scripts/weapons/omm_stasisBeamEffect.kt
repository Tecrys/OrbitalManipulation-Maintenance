package tecrys.data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import org.lazywizard.lazylib.CollisionUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.sqrt

class omm_stasisBeamEffect : BeamEffectPlugin {
    companion object {
        const val DURATION = 5f
        const val EFFECT_MULT = 0f
        const val EFFECT_ID = "omm_stasisBeamEffect"
        const val DATA_KEY = "omm_stasis_script_active"
        const val SHIELD_PIERCE_CHANCE = 0.1f // 10% chance
        const val PIERCE_CHECK_INTERVAL = 1.0f // Try once every 1 second of sustained beam contact on a shield

        // Tracks interval timers for active beams hitting shields
        private val beamTimers = mutableMapOf<BeamAPI, Float>()
    }

    override fun advance(amount: Float, engine: CombatEngineAPI, beam: BeamAPI) {
        val target = beam.damageTarget as? ShipAPI

        // If the beam is inactive, invisible, or not hitting a ship, clean up and exit
        if (beam.brightness <= 0f || target == null || !target.isAlive) {
            beamTimers.remove(beam)
            return
        }

        // If the target is already under stasis, don't spam shield pierce checks
        if (target.customData.containsKey(DATA_KEY)) {
            beamTimers.remove(beam)
            return
        }

        // Check if the beam is currently hitting an active shield
        val shield = target.shield
        val hitPoint = beam.to
        val isHittingShield = shield != null && shield.isOn && shield.isWithinArc(hitPoint)

        if (isHittingShield) {
            // Accumulate time for the pierce check interval instead of rolling every single frame
            val elapsed = (beamTimers[beam] ?: 0f) + amount
            if (elapsed < PIERCE_CHECK_INTERVAL) {
                beamTimers[beam] = elapsed
                return
            }
            // Interval reached, reset timer for the next check window
            beamTimers[beam] = 0f

            // Roll the 10% chance
            if (Math.random() > SHIELD_PIERCE_CHANCE.toDouble()) {
                return // Failed pierce check
            }

            // Successful shield pierce! Visually render the piercing arc once
            val hullPoint = CollisionUtils.getNearestPointOnBounds(beam.from, target) ?: target.location
            engine.spawnEmpArcVisual(
                hitPoint,
                null,
                hullPoint,
                target,
                beam.width,
                beam.coreColor,
                beam.fringeColor
            )
        } else {
            // Hitting armor/hull directly or no shield active; clear the shield timer
            beamTimers.remove(beam)
        }

        // Apply or refresh the stasis effect
        val existingScript = target.customData[DATA_KEY] as? omm_stasisCombatScript
        if (existingScript != null) {
            existingScript.remainingDuration = DURATION
        } else {
            val newScript = omm_stasisCombatScript(target, engine)
            target.customData[DATA_KEY] = newScript
            engine.addPlugin(newScript)
        }
    }

    inner class omm_stasisCombatScript(private val ship: ShipAPI, private val engine: CombatEngineAPI?) : BaseEveryFrameCombatPlugin() {
        var remainingDuration = DURATION
        private var effectLevel = 0.1f
        val JITTER_COLOR: Color = Color(90, 165, 255, 55)
        val JITTER_UNDER_COLOR: Color = Color(90, 165, 255, 155)

        private val statsToModify = ship.mutableStats.run {
            listOf(
                acceleration, turnAcceleration, maxTurnRate, ballisticRoFMult, energyRoFMult, missileRoFMult,
                weaponDamageTakenMult, empDamageTakenMult, hullDamageTakenMult, armorDamageTakenMult,
                shieldDamageTakenMult, maxSpeed, crewLossMult, weaponRangeThreshold, weaponRangeMultPastThreshold
            )
        }

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            remainingDuration -= amount
            if (remainingDuration <= 0f || !ship.isAlive || engine == null || !engine.isEntityInPlay(ship)) {
                statsToModify.forEach { it.unmodify(EFFECT_ID) }
                ship.customData.remove(DATA_KEY)
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
            val jitterRangeBonus = 0f

            jitterLevel = sqrt(jitterLevel.toDouble()).toFloat()
            effectLevel *= effectLevel
            ship.setJitter(this, JITTER_COLOR, 1f, 3, 0f, jitterRangeBonus)
            ship.setJitterUnder(this, JITTER_UNDER_COLOR, 1f, 25, 0f, 7f + jitterRangeBonus)
        }
    }
}