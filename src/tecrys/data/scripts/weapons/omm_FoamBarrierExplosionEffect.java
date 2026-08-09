package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;

public class omm_FoamBarrierExplosionEffect implements ProximityExplosionEffect, OnHitEffectPlugin {

    public static float IMPACT_VOLUME_MULT = 0f;

    @Override
    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        if (originalProjectile == null) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI source = originalProjectile.getSource();
        Vector2f spawnLocation = new Vector2f(originalProjectile.getLocation());

        spawnFoamDrone(engine, source, spawnLocation);
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {
        if (projectile == null || point == null) return;

        ShipAPI source = projectile.getSource();
        Vector2f spawnLocation = new Vector2f(point);

        spawnFoamDrone(engine, source, spawnLocation);
    }

    private void spawnFoamDrone(CombatEngineAPI engine, ShipAPI source, Vector2f spawnLocation) {
        if (engine == null || source == null || spawnLocation == null) return;

        float randomAngle = MathUtils.getRandomNumberInRange(0f, 360f);

        // Spawn the FX drone silently on the firing ship's side
        CombatFleetManagerAPI fleetManager = engine.getFleetManager(source.getOwner());
        fleetManager.setSuppressDeploymentMessages(true);

        ShipAPI drone = fleetManager.spawnShipOrWing("omm_foam_drone", spawnLocation, randomAngle, 0f);
        fleetManager.setSuppressDeploymentMessages(false);

        if (drone != null) {
            drone.getVelocity().set(0f, 0f);
            drone.setCollisionClass(CollisionClass.SHIP);
            drone.setExplosionScale(0f);
            drone.setHulkChanceOverride(0f);
            drone.setImpactVolumeMult(IMPACT_VOLUME_MULT);
            drone.getArmorGrid().clearComponentMap(); // no damage to weapons/engines
            drone.setNoDamagedExplosions(true);
            drone.setSpawnDebris(false);
            drone.setBeingIgnored(true);
            drone.setMass(1f);

            drone.setShipCollisionSoundOverride("omm_nosound");
            drone.setAsteroidCollisionSoundOverride("omm_nosound");
            drone.getMutableStats().getMaxSpeed().modifyMult("foam_barrier", 0f);
            drone.getMutableStats().getAcceleration().modifyMult("foam_barrier", 0f);

            // --- COMBAT END SAFEGUARDS ---
            drone.setDrone(true); // Flags entity as an auxiliary drone
            drone.setShipAI(null); // Prevents AI tactical processing
            // -----------------------------

            omm_FoamBarrierEntityPlugin visualPlugin = new omm_FoamBarrierEntityPlugin(drone, randomAngle);
            engine.addLayeredRenderingPlugin(visualPlugin);

        }
    }
}