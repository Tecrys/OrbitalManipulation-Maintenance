package tecrys.data.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class DamageReportManagerV1 {
  private static final String DamageReportManagerKey = "DamageReportManagerV1";
  
  private static final boolean dcrEnabled = Global.getSettings().getModManager().isModEnabled("DetailedCombatResults");
  
  private static final Logger log = Global.getLogger(DamageReportManagerV1.class);
  
  private static List<Object[]> getDamageReportStream() {
    Map<String, Object> customData = Global.getCombatEngine().getCustomData();
    Object raw = customData.get("DamageReportManagerV1");
    if (raw == null) {
      raw = new ArrayList(200);
      customData.put("DamageReportManagerV1", raw);
    } 
    if (!(raw instanceof List))
      throw new RuntimeException("Unknown class for CustomDataKey: 'DamageReportManagerV1' class: '" + raw.getClass() + "'"); 
    List<Object[]> ret = (List<Object[]>)raw;
    if (ret.size() > 1000)
      ret.clear(); 
    return ret;
  }
  
  public static void addDamageClarification(float shipDamage, float empDamage, DamageType damageType, CombatEntityAPI source, CombatEntityAPI target, String weaponName) {
    try {
      if (dcrEnabled)
        getDamageReportStream().add(new Object[] { Float.valueOf(shipDamage), Float.valueOf(empDamage), damageType, source, target, weaponName }); 
    } catch (Exception e) {
      log.warn("Error adding damage report", e);
    } 
  }
  
  public static void addDamageClarification(float shipDamage, float empDamage, DamagingProjectileAPI projectile) {
    try {
      addDamageClarification(shipDamage, empDamage, projectile.getDamageType(), (CombatEntityAPI)projectile.getSource(), projectile.getDamageTarget(), projectile.getWeapon().getDisplayName());
    } catch (Exception e) {
      log.warn("Error adding damage report", e);
    } 
  }
  
  public static void addDamageReport(float armorDamage, float hullDamage, float empDamage, float shieldDamage, DamageType damageType, CombatEntityAPI source, CombatEntityAPI target, String weaponName) {
    try {
      if (dcrEnabled)
        getDamageReportStream().add(new Object[] { Float.valueOf(armorDamage), Float.valueOf(hullDamage), Float.valueOf(empDamage), Float.valueOf(shieldDamage), damageType, source, target, weaponName }); 
    } catch (Exception e) {
      log.warn("Error adding damage report", e);
    } 
  }
  
  public static void addDamageReport(float armorDamage, float hullDamage, float empDamage, float shieldDamage, DamagingProjectileAPI projectile) {
    try {
      addDamageReport(armorDamage, hullDamage, empDamage, shieldDamage, projectile.getDamageType(), (CombatEntityAPI)projectile.getSource(), projectile.getDamageTarget(), projectile.getWeapon().getDisplayName());
    } catch (Exception e) {
      log.warn("Error adding damage report", e);
    } 
  }
  
  public static void addDamageReport(float armorDamage, float hullDamage, float empDamage, float shieldDamage, BeamAPI beam) {
    try {
      addDamageReport(armorDamage, hullDamage, empDamage, shieldDamage, beam.getDamage().getType(), (CombatEntityAPI)beam.getSource(), beam.getDamageTarget(), beam.getWeapon().getDisplayName());
    } catch (Exception e) {
      log.warn("Error adding damage report", e);
    } 
  }
}
