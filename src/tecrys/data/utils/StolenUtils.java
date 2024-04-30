package tecrys.data.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;

public class StolenUtils {
  static final float SAFE_DISTANCE = 600.0F;
  
  static final float DEFAULT_DAMAGE_WINDOW = 3.0F;
  
  static final Map<ShipAPI.HullSize, Float> baseOverloadTimes = new HashMap<>();
  
  static {
    baseOverloadTimes.put(ShipAPI.HullSize.FIGHTER, Float.valueOf(10.0F));
    baseOverloadTimes.put(ShipAPI.HullSize.FRIGATE, Float.valueOf(4.0F));
    baseOverloadTimes.put(ShipAPI.HullSize.DESTROYER, Float.valueOf(6.0F));
    baseOverloadTimes.put(ShipAPI.HullSize.CRUISER, Float.valueOf(8.0F));
    baseOverloadTimes.put(ShipAPI.HullSize.CAPITAL_SHIP, Float.valueOf(10.0F));
    baseOverloadTimes.put(ShipAPI.HullSize.DEFAULT, Float.valueOf(6.0F));
  }
  
  public static Vector2f getMidpoint(Vector2f from, Vector2f to, float d) {
    d *= 2.0F;
    return new Vector2f((from.x * (2.0F - d) + to.x * d) / 2.0F, (from.y * (2.0F - d) + to.y * d) / 2.0F);
  }
  
  public static Vector2f toRelative(CombatEntityAPI entity, Vector2f point) {
    Vector2f retVal = new Vector2f((ReadableVector2f)point);
    Vector2f.sub(retVal, entity.getLocation(), retVal);
    VectorUtils.rotate(retVal, -entity.getFacing(), retVal);
    return retVal;
  }
  
  public static Vector2f toAbsolute(CombatEntityAPI entity, Vector2f point) {
    Vector2f retVal = new Vector2f((ReadableVector2f)point);
    VectorUtils.rotate(retVal, entity.getFacing(), retVal);
    Vector2f.add(retVal, entity.getLocation(), retVal);
    return retVal;
  }
  
  public static void blink(Vector2f at) {
    Global.getCombatEngine().addHitParticle(at, new Vector2f(), 30.0F, 1.0F, 0.1F, Color.RED);
  }
  
  public static List<ShipAPI> getShipsOnSegment(Vector2f from, Vector2f to) {
    float distance = MathUtils.getDistance(from, to);
    Vector2f center = new Vector2f();
    center.x = (from.x + to.x) / 2.0F;
    center.y = (from.y + to.y) / 2.0F;
    List<ShipAPI> list = new ArrayList<>();
    for (ShipAPI s : CombatUtils.getShipsWithinRange(center, distance / 2.0F)) {
      if (CollisionUtils.getCollisionPoint(from, to, (CombatEntityAPI)s) != null)
        list.add(s); 
    } 
    return list;
  }
  
  public static ShipAPI getFirstShipOnSegment(Vector2f from, Vector2f to, CombatEntityAPI exception) {
    ShipAPI winner = null;
    float record = Float.MAX_VALUE;
    for (ShipAPI s : getShipsOnSegment(from, to)) {
      if (s == exception)
        continue; 
      float dist2 = MathUtils.getDistanceSquared((CombatEntityAPI)s, from);
      if (dist2 < record) {
        record = dist2;
        winner = s;
      } 
    } 
    return winner;
  }
  
  public static ShipAPI getFirstNonFighterOnSegment(Vector2f from, Vector2f to, CombatEntityAPI exception) {
    ShipAPI winner = null;
    float record = Float.MAX_VALUE;
    for (ShipAPI s : getShipsOnSegment(from, to)) {
      if (s == exception || 
        s.isFighter())
        continue; 
      float dist2 = MathUtils.getDistanceSquared((CombatEntityAPI)s, from);
      if (dist2 < record) {
        record = dist2;
        winner = s;
      } 
    } 
    return winner;
  }
  
  public static ShipAPI getFirstShipOnSegment(Vector2f from, Vector2f to) {
    return getFirstShipOnSegment(from, to, null);
  }
  
  public static ShipAPI getShipInLineOfFire(WeaponAPI weapon) {
    Vector2f endPoint = weapon.getLocation();
    endPoint.x = (float)(endPoint.x + Math.cos(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange());
    endPoint.y = (float)(endPoint.y + Math.sin(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange());
    return getFirstShipOnSegment(weapon.getLocation(), endPoint, (CombatEntityAPI)weapon.getShip());
  }
  
  public static float getArmorPercent(ShipAPI ship) {
    float acc = 0.0F;
    int width = (ship.getArmorGrid().getGrid()).length;
    int height = (ship.getArmorGrid().getGrid()[0]).length;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++)
        acc += ship.getArmorGrid().getArmorFraction(x, y); 
    } 
    return acc / (width * height);
  }
  
  public static void setArmorPercentage(ShipAPI ship, float armorPercent) {
    ArmorGridAPI armorGrid = ship.getArmorGrid();
    armorPercent = Math.min(1.0F, Math.max(0.0F, armorPercent));
    for (int x = 0; x < (armorGrid.getGrid()).length; x++) {
      for (int y = 0; y < (armorGrid.getGrid()[0]).length; y++)
        armorGrid.setArmorValue(x, y, armorGrid.getMaxArmorInCell() * armorPercent); 
    } 
  }
  
  public static void destroy(CombatEntityAPI entity) {
    Global.getCombatEngine().applyDamage(entity, entity.getLocation(), entity
        .getMaxHitpoints() * 10.0F, DamageType.HIGH_EXPLOSIVE, 0.0F, true, true, entity);
  }
  
  public static float estimateIncomingDamage(ShipAPI ship) {
    return estimateIncomingDamage(ship, 3.0F);
  }
  
  public static float estimateIncomingDamage(ShipAPI ship, float damageWindowSeconds) {
    float accumulator = 0.0F;
    accumulator += estimateIncomingBeamDamage(ship, damageWindowSeconds);
    for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {
      if (proj.getOwner() == ship.getOwner())
        continue; 
      Vector2f endPoint = new Vector2f((ReadableVector2f)proj.getVelocity());
      endPoint.scale(damageWindowSeconds);
      Vector2f.add(endPoint, proj.getLocation(), endPoint);
      if (ship.getShield() == null || !ship.getShield().isWithinArc(proj.getLocation())) {
        if (!CollisionUtils.getCollides(proj.getLocation(), endPoint, new Vector2f((ReadableVector2f)ship
              .getLocation()), ship.getCollisionRadius()))
          continue; 
        accumulator += proj.getDamageAmount() + proj.getEmpAmount();
      } 
    } 
    return accumulator;
  }
  
  public static float estimateAllIncomingDamage(ShipAPI ship) {
    return estimateIncomingDamage(ship, 3.0F);
  }
  
  public static float estimateIncomingBeamDamage(ShipAPI ship, float damageWindowSeconds) {
    float accumulator = 0.0F;
    for (BeamAPI beam : Global.getCombatEngine().getBeams()) {
      if (beam.getDamageTarget() != ship)
        continue; 
      float dps = beam.getWeapon().getDerivedStats().getDamageOver30Sec() / 30.0F;
      float emp = beam.getWeapon().getDerivedStats().getEmpPerSecond();
      accumulator += (dps + emp) * damageWindowSeconds;
    } 
    return accumulator;
  }
  
  public static float estimateIncomingMissileDamage(ShipAPI ship) {
    float accumulator = 0.0F;
    for (MissileAPI missileAPI : Global.getCombatEngine().getMissiles()) {
      MissileAPI missileAPI1 = missileAPI;
      if (missileAPI1.getOwner() == ship.getOwner())
        continue; 
      float safeDistance = 600.0F + ship.getCollisionRadius();
      float threat = missileAPI1.getDamageAmount() + missileAPI1.getEmpAmount();
      if (ship.getShield() != null && ship.getShield().isWithinArc(missileAPI1.getLocation()))
        continue; 
      accumulator = (float)(accumulator + threat * Math.max(0.0D, Math.min(1.0D, Math.pow((1.0F - MathUtils.getDistance((CombatEntityAPI)missileAPI1, (CombatEntityAPI)ship) / safeDistance), 2.0D))));
    } 
    return accumulator;
  }
  
  public static float getHitChance(DamagingProjectileAPI proj, CombatEntityAPI target) {
    float estTimeTilHit = MathUtils.getDistance(target, proj.getLocation()) / Math.max(1.0F, proj.getWeapon().getProjectileSpeed());
    Vector2f estTargetPosChange = new Vector2f((target.getVelocity()).x * estTimeTilHit, (target.getVelocity()).y * estTimeTilHit);
    float estFacingChange = target.getAngularVelocity() * estTimeTilHit;
    Vector2f projVelocity = proj.getVelocity();
    target.setFacing(target.getFacing() + estFacingChange);
    Vector2f.add(target.getLocation(), estTargetPosChange, target.getLocation());
    projVelocity.scale(estTimeTilHit * 3.0F);
    Vector2f.add(projVelocity, proj.getLocation(), projVelocity);
    Vector2f estHitLoc = CollisionUtils.getCollisionPoint(proj.getLocation(), projVelocity, target);
    target.setFacing(target.getFacing() - estFacingChange);
    Vector2f.add(target.getLocation(), (Vector2f)estTargetPosChange.scale(-1.0F), target.getLocation());
    if (estHitLoc == null)
      return 0.0F; 
    return 1.0F;
  }
  
  public static float getHitChance(WeaponAPI weapon, CombatEntityAPI target) {
    float estTimeTilHit = MathUtils.getDistance(target, weapon.getLocation()) / Math.max(1.0F, weapon.getProjectileSpeed());
    Vector2f estTargetPosChange = new Vector2f((target.getVelocity()).x * estTimeTilHit, (target.getVelocity()).y * estTimeTilHit);
    float estFacingChange = target.getAngularVelocity() * estTimeTilHit;
    double theta = weapon.getCurrAngle() * 0.017453292519943295D;
    Vector2f projVelocity = new Vector2f((float)Math.cos(theta) * weapon.getProjectileSpeed() + (weapon.getShip().getVelocity()).x, (float)Math.sin(theta) * weapon.getProjectileSpeed() + (weapon.getShip().getVelocity()).y);
    target.setFacing(target.getFacing() + estFacingChange);
    Vector2f.add(target.getLocation(), estTargetPosChange, target.getLocation());
    projVelocity.scale(estTimeTilHit * 3.0F);
    Vector2f.add(projVelocity, weapon.getLocation(), projVelocity);
    Vector2f estHitLoc = CollisionUtils.getCollisionPoint(weapon.getLocation(), projVelocity, target);
    target.setFacing(target.getFacing() - estFacingChange);
    Vector2f.add(target.getLocation(), (Vector2f)estTargetPosChange.scale(-1.0F), target.getLocation());
    if (estHitLoc == null)
      return 0.0F; 
    return 1.0F;
  }
  
  public static float getFPWorthOfSupport(ShipAPI ship, float range) {
    float retVal = 0.0F;
    for (Iterator<ShipAPI> iter = AIUtils.getNearbyAllies((CombatEntityAPI)ship, range).iterator(); iter.hasNext(); ) {
      ShipAPI ally = iter.next();
      if (ally == ship)
        continue; 
      float colDist = ship.getCollisionRadius() + ally.getCollisionRadius();
      float distance = Math.max(0.0F, MathUtils.getDistance((CombatEntityAPI)ship, (CombatEntityAPI)ally) - colDist);
      float maxRange = Math.max(1.0F, range - colDist);
      retVal += getFPStrength(ally) * (1.0F - distance / maxRange);
    } 
    return retVal;
  }
  
  public static float getFPWorthOfHostility(ShipAPI ship, float range) {
    float retVal = 0.0F;
    for (Iterator<ShipAPI> iter = AIUtils.getNearbyEnemies((CombatEntityAPI)ship, range).iterator(); iter.hasNext(); ) {
      ShipAPI enemy = iter.next();
      float colDist = ship.getCollisionRadius() + enemy.getCollisionRadius();
      float distance = Math.max(0.0F, MathUtils.getDistance((CombatEntityAPI)ship, (CombatEntityAPI)enemy) - colDist);
      float maxRange = Math.max(1.0F, range - colDist);
      retVal += getFPStrength(enemy) * (1.0F - distance / maxRange);
    } 
    return retVal;
  }
  
  public static float getStrengthInArea(Vector2f at, float range) {
    float retVal = 0.0F;
    for (ShipAPI ship : CombatUtils.getShipsWithinRange(at, range))
      retVal += getFPStrength(ship); 
    return retVal;
  }
  
  public static float getStrengthInArea(Vector2f at, float range, int owner) {
    float retVal = 0.0F;
    for (ShipAPI ship : CombatUtils.getShipsWithinRange(at, range)) {
      if (ship.getOwner() == owner)
        retVal += getFPStrength(ship); 
    } 
    return retVal;
  }
  
  public static float getFPStrength(ShipAPI ship) {
    DeployedFleetMemberAPI member = Global.getCombatEngine().getFleetManager(ship.getOwner()).getDeployedFleetMember(ship);
    return (member == null || member.getMember() == null) ? 
      0.0F : 
      member.getMember().getMemberStrength();
  }
  
  public static float getFP(ShipAPI ship) {
    DeployedFleetMemberAPI member = Global.getCombatEngine().getFleetManager(ship.getOwner()).getDeployedFleetMember(ship);
    return (member == null || member.getMember() == null) ? 
      0.0F : 
      member.getMember().getFleetPointCost();
  }
  
  public static float getBaseOverloadDuration(ShipAPI ship) {
    return ((Float)baseOverloadTimes.get(ship.getHullSize())).floatValue();
  }
  
  public static float estimateOverloadDurationOnHit(ShipAPI ship, float damage, DamageType type) {
    if (ship.getShield() == null)
      return 0.0F; 
    float fluxDamage = damage * type.getShieldMult() * ship.getMutableStats().getShieldAbsorptionMult().getModifiedValue();
    fluxDamage += ship.getFluxTracker().getCurrFlux() - ship
      .getFluxTracker().getMaxFlux();
    if (fluxDamage <= 0.0F)
      return 0.0F; 
    return Math.min(15.0F, getBaseOverloadDuration(ship) + fluxDamage / 25.0F);
  }
  
  public static float getLifeExpectancy(ShipAPI ship) {
    float damage = estimateIncomingDamage(ship);
    return (damage <= 0.0F) ? 3600.0F : (ship.getHitpoints() / damage);
  }
  
  public static void createSmoothFlare(CombatEngineAPI engine, ShipAPI origin, Vector2f point, float thickness, float length, float angle, Color fringeColor, Color coreColor) {
    for (int i = 1; i < length / 50.0F; i++) {
      point.x = (float)(point.x + FastTrig.cos((angle * 3.1415927F / 180.0F)));
      point.y = (float)(point.y + FastTrig.sin((angle * 3.1415927F / 180.0F)));
      engine.spawnEmpArc(origin, point, null, (CombatEntityAPI)new SimpleEntity(point), DamageType.FRAGMENTATION, 0.0F, 0.0F, 10.0F, null, 25.0F, new Color(fringeColor.getRed(), fringeColor.getGreen(), fringeColor.getBlue(), Math.min(255, (int)(thickness * fringeColor.getAlpha() / 128.0F))), coreColor);
    } 
  }
}
