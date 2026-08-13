package tecrys.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class omm_FoamBarrierExplosionEffect implements ProximityExplosionEffect, OnHitEffectPlugin {

    public static float IMPACT_VOLUME_MULT = 0f;

    @Override
    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        if (originalProjectile == null) return;
        spawnFoamDrone(Global.getCombatEngine(), originalProjectile.getSource(), new Vector2f(originalProjectile.getLocation()));
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {
        if (projectile == null || point == null) return;
        spawnFoamDrone(engine, projectile.getSource(), new Vector2f(point));
    }

    private void spawnFoamDrone(CombatEngineAPI engine, ShipAPI source, Vector2f spawnLocation) {
        if (engine == null || source == null || spawnLocation == null) return;

        float randomAngle = MathUtils.getRandomNumberInRange(0f, 360f);

        ShipHullSpecAPI spec = Global.getSettings().getHullSpec("omm_foam_drone");
        ShipVariantAPI variant = Global.getSettings().createEmptyVariant("omm_foam_drone_empty", spec);

        // Bypassing Fleet Manager entirely to prevent UI ghosting
        ShipAPI drone = engine.createFXDrone(variant);

        if (drone != null) {
            drone.setLayer(CombatEngineLayers.BELOW_SHIPS_LAYER);
            drone.setOwner(source.getOwner());
            drone.getLocation().set(spawnLocation);
            drone.setFacing(randomAngle);

            drone.getVelocity().set(0f, 0f);
            drone.addTag("fx_drone");
            drone.setCollisionClass(CollisionClass.SHIP);
            drone.setExplosionScale(0f);
            drone.setHulkChanceOverride(0f);
            drone.setImpactVolumeMult(IMPACT_VOLUME_MULT);
            drone.getArmorGrid().clearComponentMap();
            drone.setNoDamagedExplosions(true);
            drone.setSpawnDebris(false);
            drone.setBeingIgnored(true);
            drone.setMass(1f);

            drone.setShipCollisionSoundOverride("omm_nosound");
            drone.setAsteroidCollisionSoundOverride("omm_nosound");
            drone.getMutableStats().getMaxSpeed().modifyMult("foam_barrier", 0f);
            drone.getMutableStats().getAcceleration().modifyMult("foam_barrier", 0f);

            drone.setDrone(true);
            drone.setShipAI(null);

            engine.addEntity(drone);

            omm_FoamBarrierEntityPlugin visualPlugin = new omm_FoamBarrierEntityPlugin(drone, randomAngle);
            engine.addLayeredRenderingPlugin(visualPlugin);
        }
    }
}