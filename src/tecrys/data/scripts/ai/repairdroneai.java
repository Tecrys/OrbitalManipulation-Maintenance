package tecrys.data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import tecrys.data.utils.DamageReportManagerV1;
import tecrys.data.utils.StolenUtils;
import java.awt.Color;
import java.util.Random;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.Point;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

public class repairdroneai extends BaseShipAI {
  private static final float REPAIR_RANGE = 35.0F;
  
  private static final float REPAIR_HULL = 6.0F;
  
  private static final float REPAIR_ARMOR = 0.24F;
  
  private static final float FLUX_PER_MX_PERFORMED = 1.0F;
  
  private final Color SPARK_COLOR = new Color(255,195,0,100);
  
  private static final String SPARK_SOUND_ID = "system_emp_emitter_loop";
  
  private static final float SPARK_CHANCE = 0.67F;
  
  private static final float SPARK_SPEED_MULTIPLIER = 500.0F;
  
  private static final float SPARK_VOLUME = 1.0F;
  
  private static final float SPARK_PITCH = 1.0F;
  
  private final ShipwideAIFlags flags = new ShipwideAIFlags();
  
  private final ShipAIConfig config = new ShipAIConfig();
  
  private ShipAPI carrier;
  
  ShipAPI target;
  
  Vector2f targetOffset;
  
  Random rng = new Random();
  
  ArmorGridAPI armorGrid;
  
  float maxArmorInCell;
  
  float cellSize;
  
  int gridWidth;
  
  int gridHeight;
  
  int cellCount;
  
  boolean shouldRepair = false;
  
  boolean returning = false;
  
  boolean spark = false;
  
  float targetFacingOffset = Float.MIN_VALUE;
  
  float range = 4000.0F;
  
  private final IntervalUtil interval = new IntervalUtil(0.25F, 0.33F);
  
  private final IntervalUtil countdown = new IntervalUtil(4.0F, 4.0F);
  
  public repairdroneai(ShipAPI ship) {
    super(ship);
  }
  
  public void advance(float amount) {
    if (this.carrier == null)
      init(); 
    if (this.ship.isLanding()) {
      this.countdown.advance(amount);
      if (this.countdown.intervalElapsed()) {
        this.ship.getWing().getSource().land(this.ship);
        return;
      } 
    } 
    this.interval.advance(amount);
    if (this.interval.intervalElapsed()) {
      super.advance(amount);
      if (this.target == null)
        return; 
      if (this.shouldRepair) {
        repairArmorAndHull();
      } else if (this.returning && !this.ship.isLanding() && MathUtils.getDistance((CombatEntityAPI)this.ship, (CombatEntityAPI)this.carrier) < this.carrier.getCollisionRadius() / 3.0F) {
        this.ship.beginLandingAnimation(this.carrier);
      } 
    } 
    goToDestination();
  }
  
  public boolean needsRefit() {
    return (this.ship.getFluxTracker().getFluxLevel() >= 1.0F);
  }
  
  public void cancelCurrentManeuver() {}
  
  public void evaluateCircumstances() {
    if (this.carrier == null || !this.carrier.isAlive()) {
      StolenUtils.destroy((CombatEntityAPI)this.ship);
      return;
    } 
    setTarget(chooseTarget());
    if (this.returning) {
      this.targetOffset = StolenUtils.toRelative((CombatEntityAPI)this.target, this.ship.getWing().getSource().getLandingLocation(this.ship));
    } else {
      do {
        this.targetOffset = MathUtils.getRandomPointInCircle(this.target.getLocation(), this.target.getCollisionRadius());
      } while (!CollisionUtils.isPointWithinBounds(this.targetOffset, (CombatEntityAPI)this.target));
      this.targetOffset = StolenUtils.toRelative((CombatEntityAPI)this.target, this.targetOffset);
      this.armorGrid = this.target.getArmorGrid();
      this.maxArmorInCell = this.armorGrid.getMaxArmorInCell();
      this.cellSize = this.armorGrid.getCellSize();
      this.gridWidth = (this.armorGrid.getGrid()).length;
      this.gridHeight = (this.armorGrid.getGrid()[0]).length;
      this.cellCount = this.gridWidth * this.gridHeight;
    } 
    if ((this.target.getPhaseCloak() == null || !this.target.getPhaseCloak().isOn()) && !this.returning && (
      
      StolenUtils.getArmorPercent(this.target) < 1.0F || this.target.getHullLevel() < 0.98F) && 
      MathUtils.getDistance((CombatEntityAPI)this.ship, (CombatEntityAPI)this.target) < 35.0F) {
      this.shouldRepair = true;
    } else {
      this.shouldRepair = false;
    } 
  }
  
  Point computeArmorCellToRepair() {
    if (this.gridWidth <= 0 || this.gridHeight <= 0)
      return null; 
    for (int i = 0; i < 1 + this.cellCount / 5; i++) {
      int x = this.rng.nextInt(this.gridWidth);
      int y = this.rng.nextInt(this.gridHeight);
      if (this.armorGrid.getArmorValue(x, y) < this.maxArmorInCell)
        return new Point(x, y); 
    } 
    return null;
  }
  
  void repairArmorAndHull() {
    float totalHullRepaired = 0.0F;
    this.spark = true;
    this.ship.getFluxTracker().setCurrFlux(this.ship.getFluxTracker().getCurrFlux() + 1.0F);
    float bonus = 1.0F;
    if (this.target.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
      bonus = 4.0F;
    } else if (this.target.getHullSize() == ShipAPI.HullSize.CRUISER) {
      bonus = 3.0F;
    } else if (this.target.getHullSize() == ShipAPI.HullSize.DESTROYER) {
      bonus = 2.0F;
    } 
    if (this.target.getHullLevel() < 0.99F) {
      totalHullRepaired = 6.0F * bonus;
      this.target.setHitpoints(this.target.getHitpoints() + totalHullRepaired);
      float overage = this.target.getHitpoints() - this.target.getMaxHitpoints();
      if (overage > 0.0F) {
        totalHullRepaired -= overage;
        this.target.setHitpoints(this.target.getMaxHitpoints());
      } 
    } 
    float totalArmorRepaired = 0.0F;
    Point cellToFix = computeArmorCellToRepair();
    if (cellToFix != null)
      for (int x = cellToFix.getX() - 1; x <= cellToFix.getX() + 1; x++) {
        if (x >= 0 && x < this.gridWidth)
          for (int y = cellToFix.getY() - 1; y <= cellToFix.getY() + 1; y++) {
            if (y >= 0 && y < this.gridHeight)
              if (this.armorGrid.getArmorValue(x, y) < this.maxArmorInCell) {
                float cellValue = this.armorGrid.getArmorValue(x, y);
                float armorRepairAmount = 0.24F * bonus;
                this.armorGrid.setArmorValue(x, y, cellValue + armorRepairAmount);
                float overage = this.armorGrid.getArmorValue(x, y) - this.maxArmorInCell;
                if (overage > 0.0F) {
                  armorRepairAmount -= overage;
                  this.armorGrid.setArmorValue(x, y, this.maxArmorInCell);
                } 
                totalArmorRepaired += armorRepairAmount;
              }  
          }  
      }  
    if (totalArmorRepaired + totalHullRepaired > 1.0F) {
      Global.getCombatEngine().addFloatingDamageText(this.target.getLocation(), totalArmorRepaired + totalHullRepaired, Color.GREEN, (CombatEntityAPI)this.target, (CombatEntityAPI)this.ship.getWing().getSourceShip());
      DamageReportManagerV1.addDamageReport(-totalArmorRepaired, -totalHullRepaired, 0.0F, 0.0F, DamageType.OTHER, (CombatEntityAPI)this.ship.getWing().getSourceShip(), (CombatEntityAPI)this.target, "Maltese Repair Drone");
    } 
  }
  
  ShipAPI chooseTarget() {
    if (needsRefit()) {
      this.returning = true;
      return this.carrier;
    } 
    this.returning = false;
    if (this.carrier.getShipTarget() != null && this.carrier
      .getOwner() == this.carrier.getShipTarget().getOwner() && 
      !this.carrier.getShipTarget().isDrone() && 
      !this.carrier.getShipTarget().isFighter())
      return this.carrier.getShipTarget(); 
    ShipAPI mostWounded = this.carrier;
    float mostDamage = 4.0F;
    for (ShipAPI s : Global.getCombatEngine().getShips()) {
      float d = MathUtils.getDistance((CombatEntityAPI)this.carrier, (CombatEntityAPI)s);
      if (!s.isFighter() && !s.isDrone() && !s.isHulk() && d <= this.range) {
        float currDamage = StolenUtils.getArmorPercent(s) + s.getHullLevel();
        float priority = d / 1000.0F;
        if (s.isStationModule())
          priority++; 
        if (s.getOwner() == this.carrier.getOwner() && currDamage < 1.98F && 
          mostDamage > currDamage + priority / 2.0F) {
          mostDamage = currDamage + priority / 2.0F;
          mostWounded = s;
        } 
      } 
    } 
    return mostWounded;
  }
  
  void setTarget(ShipAPI t) {
    if (this.target == t)
      return; 
    this.target = t;
    this.ship.setShipTarget(t);
  }
  
  void goToDestination() {
    Vector2f to = StolenUtils.toAbsolute((CombatEntityAPI)this.target, this.targetOffset);
    float distance = MathUtils.getDistance((CombatEntityAPI)this.ship, to);
    if (this.shouldRepair && 
      distance < 100.0F) {
      float f = (1.0F - distance / 100.0F) * 0.2F;
      (this.ship.getLocation()).x = (to.x * f + (this.ship.getLocation()).x * (2.0F - f)) / 2.0F;
      (this.ship.getLocation()).y = (to.y * f + (this.ship.getLocation()).y * (2.0F - f)) / 2.0F;
      (this.ship.getVelocity()).x = ((this.target.getVelocity()).x * f + (this.ship.getVelocity()).x * (2.0F - f)) / 2.0F;
      (this.ship.getVelocity()).y = ((this.target.getVelocity()).y * f + (this.ship.getVelocity()).y * (2.0F - f)) / 2.0F;
    } 
    if (this.shouldRepair && distance < 35.0F) {
                Global.getSoundPlayer().playLoop(SPARK_SOUND_ID, this.target, 1.0F, 0.5F, this.target.getLocation(), this.target.getVelocity());
      if (this.spark) {

        if (this.targetFacingOffset == Float.MIN_VALUE) {
          this.targetFacingOffset = this.ship.getFacing() - this.target.getFacing();
        } else {
          this.ship.setFacing(MathUtils.clampAngle(this.targetFacingOffset + this.target.getFacing()));
        } 
        if (Math.random() < 0.6700000166893005D) {
          Vector2f loc = new Vector2f((ReadableVector2f)this.ship.getLocation());
          loc.x += this.cellSize * 0.5F - this.cellSize * (float)Math.random();
          loc.y += this.cellSize * 0.5F - this.cellSize * (float)Math.random();
          Vector2f vel = new Vector2f((ReadableVector2f)this.ship.getVelocity());
          vel.x = (float)(vel.x + (Math.random() - 0.5D) * 500.0D);
          vel.y = (float)(vel.y + (Math.random() - 0.5D) * 500.0D);
          MagicLensFlare.createSharpFlare(
              Global.getCombatEngine(), this.ship, loc, 5.0F, 100.0F, 0.0F, this.SPARK_COLOR, Color.white);
        } 
      } 
      this.spark = false;
    } else {
      float distToCarrier = (float)(MathUtils.getDistanceSquared(this.carrier.getLocation(), this.ship.getLocation()) / Math.pow(this.target.getCollisionRadius(), 2.0D));
      if ((this.target == this.carrier && distToCarrier < 1.0F) || this.ship.isLanding()) {
        float f = 30.0F - Math.min(1.0F, distToCarrier);
        if (!this.returning)
          f *= 0.1F; 
        turnToward(this.target.getFacing());
        (this.ship.getLocation()).x = (to.x * f * 0.1F + (this.ship.getLocation()).x * (2.0F - f * 0.1F)) / 2.0F;
        (this.ship.getLocation()).y = (to.y * f * 0.1F + (this.ship.getLocation()).y * (2.0F - f * 0.1F)) / 2.0F;
        (this.ship.getVelocity()).x = ((this.target.getVelocity()).x * f + (this.ship.getVelocity()).x * (2.0F - f)) / 2.0F;
        (this.ship.getVelocity()).y = ((this.target.getVelocity()).y * f + (this.ship.getVelocity()).y * (2.0F - f)) / 2.0F;
      } else {
        this.targetFacingOffset = Float.MIN_VALUE;
        float angleDif = MathUtils.getShortestRotation(this.ship.getFacing(), VectorUtils.getAngle(this.ship.getLocation(), to));
        if (Math.abs(angleDif) < 30.0F) {
          accelerate();
        } else {
          turnToward(to);
          decelerate();
        } 
        strafeToward(to);
      } 
    } 
  }
  
  public ShipwideAIFlags getAIFlags() {
    return this.flags;
  }
  
  public void setDoNotFireDelay(float amount) {}
  
  public ShipAIConfig getConfig() {
    return this.config;
  }
  
  public void init() {
    this.carrier = this.ship.getWing().getSourceShip();
    this.target = this.carrier;
    this.targetOffset = StolenUtils.toRelative((CombatEntityAPI)this.carrier, this.carrier.getLocation());
    this.range = this.ship.getWing().getRange();
  }
}
