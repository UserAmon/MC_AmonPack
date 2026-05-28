package RPG.Dungeons;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DungeonPlayerStats {
    private final UUID playerUUID;
    private final String playerName;

    // Combat Stats (Rogue-lite)
    private double hpBoost = 0;      // Extra max health (half-hearts, e.g., 2.0 = 1 heart)
    private double defBoost = 0;     // Defense points (reduces incoming damage)
    private double dmgMultiplier = 1.0; // Damage multiplier (default 1.0 = 100%)
    private double speedBoost = 0;   // Added to default speed (standard walk speed is 0.2)

    private double pCritRate = 0.05;
    private double pCritDmg = 1.5;
    private double mCritRate = 0.05;
    private double mCritDmg = 1.5;
    private int regenLevel = 0;
    private final java.util.Map<String, Integer> blessingLevels = new java.util.HashMap<>();
    private double earthMaceDurability = 5.0;
    private double windSickleDurability = 5.0;
    private double waterStaffDurability = 99.0;

    private final List<String> boundDungeonSkills = new ArrayList<>();
    private final List<String> activeBlessings = new ArrayList<>();

    private UUID lastMaiHitTarget = null;
    private long lastMaiHitTime = 0;
    private int amonGloveCharge = 0;
    private transient org.bukkit.boss.BossBar amonGloveBar = null;
    private long amonGloveLastChangeTime = 0;
    private long pouhaiBowPullStartTime = 0;
    private int pouhaiBowShotCount = 0;

    public DungeonPlayerStats(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }

    public UUID getLastMaiHitTarget() { return lastMaiHitTarget; }
    public void setLastMaiHitTarget(UUID target) { this.lastMaiHitTarget = target; }
    public long getLastMaiHitTime() { return lastMaiHitTime; }
    public void setLastMaiHitTime(long time) { this.lastMaiHitTime = time; }
    public int getAmonGloveCharge() { return amonGloveCharge; }
    public void setAmonGloveCharge(int charge) { this.amonGloveCharge = charge; }
    public org.bukkit.boss.BossBar getAmonGloveBar() { return amonGloveBar; }
    public void setAmonGloveBar(org.bukkit.boss.BossBar bar) { this.amonGloveBar = bar; }
    public long getAmonGloveLastChangeTime() { return amonGloveLastChangeTime; }
    public void setAmonGloveLastChangeTime(long time) { this.amonGloveLastChangeTime = time; }
    public long getPouhaiBowPullStartTime() { return pouhaiBowPullStartTime; }
    public void setPouhaiBowPullStartTime(long time) { this.pouhaiBowPullStartTime = time; }
    public int getPouhaiBowShotCount() { return pouhaiBowShotCount; }
    public void setPouhaiBowShotCount(int count) { this.pouhaiBowShotCount = count; }

    public void cleanupGloveBar(Player player) {
        if (amonGloveBar != null) {
            if (player != null) {
                amonGloveBar.removePlayer(player);
            }
            amonGloveBar.removeAll();
            amonGloveBar.setVisible(false);
            amonGloveBar = null;
        }
    }

    /**
     * Applies the current speed and HP stats to the Spigot player attributes.
     */
    public void applyStatsToPlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        // Apply Max Health attribute
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double newMax = 20.0 + hpBoost;
            maxHealthAttr.setBaseValue(newMax);
            
            // If current health is above new max, scale it down, otherwise keep current
            if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
        }

        // Apply Movement Speed attribute
        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            double newSpeed = 0.2 + speedBoost;
            speedAttr.setBaseValue(newSpeed);
        }
    }

    /**
     * Resets the player's attributes to standard values upon leaving.
     */
    public void resetAttributes(Player player) {
        if (player == null || !player.isOnline()) return;

        cleanupGloveBar(player);

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) {
                player.setHealth(20.0);
            }
        }

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.2);
        }
    }

    /**
     * Calculates final incoming damage after defense reduction:
     * Damage = BaseDamage * (1 - DEF / (DEF + 50))
     */
    public double calculateIncomingDamage(double baseDamage) {
        if (defBoost <= 0) return baseDamage;
        double reduction = defBoost / (defBoost + 50.0);
        return baseDamage * (1.0 - reduction);
    }

    /**
     * Calculates final outgoing damage after applying damage multiplier:
     * Damage = BaseDamage * dmgMultiplier
     */
    public double calculateOutgoingDamage(double baseDamage) {
        return baseDamage * dmgMultiplier;
    }

    // Getters and Setters for stats
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public double getHpBoost() {
        return hpBoost;
    }

    public void addHpBoost(double amount) {
        this.hpBoost += amount;
    }

    public double getDefBoost() {
        return defBoost;
    }

    public void addDefBoost(double amount) {
        this.defBoost += amount;
    }

    public double getDmgMultiplier() {
        return dmgMultiplier;
    }

    public void addDmgMultiplier(double amount) {
        this.dmgMultiplier += amount;
    }

    public double getSpeedBoost() {
        return speedBoost;
    }

    public void addSpeedBoost(double amount) {
        this.speedBoost += amount;
    }

    public List<String> getBoundDungeonSkills() {
        return boundDungeonSkills;
    }

    public void addBoundDungeonSkill(String skill) {
        if (!boundDungeonSkills.contains(skill)) {
            boundDungeonSkills.add(skill);
        }
    }

    public List<String> getActiveBlessings() {
        return activeBlessings;
    }

    public void addActiveBlessing(String blessing) {
        if (!activeBlessings.contains(blessing)) {
            activeBlessings.add(blessing);
        }
    }

    public boolean hasBlessing(String blessing) {
        return activeBlessings.contains(blessing);
    }

    public double getPCritRate() {
        return pCritRate;
    }

    public void addPCritRate(double amount) {
        this.pCritRate += amount;
    }

    public double getPCritDmg() {
        return pCritDmg;
    }

    public void addPCritDmg(double amount) {
        this.pCritDmg += amount;
    }

    public double getMCritRate() {
        return mCritRate;
    }

    public void addMCritRate(double amount) {
        this.mCritRate += amount;
    }

    public double getMCritDmg() {
        return mCritDmg;
    }

    public void addMCritDmg(double amount) {
        this.mCritDmg += amount;
    }

    public int getRegenLevel() {
        return regenLevel;
    }

    public void addRegenLevel(int amount) {
        this.regenLevel += amount;
    }

    public int getBlessingLevel(String blessing) {
        return blessingLevels.getOrDefault(blessing.toUpperCase(), 0);
    }

    public void upgradeBlessing(String blessing) {
        String key = blessing.toUpperCase();
        blessingLevels.put(key, getBlessingLevel(key) + 1);
    }

    public double getWeaponDurability(String key) {
        String upper = key.toUpperCase();
        if ("EARTH_MACE".equals(upper)) return earthMaceDurability;
        if ("WIND_SICKLE".equals(upper)) return windSickleDurability;
        if ("WATER_STAFF".equals(upper)) return waterStaffDurability;
        return 0;
    }

    public void setWeaponDurability(String key, double val) {
        String upper = key.toUpperCase();
        double clamped = Math.max(0.0, Math.min(100.0, val));
        if ("EARTH_MACE".equals(upper)) earthMaceDurability = clamped;
        if ("WIND_SICKLE".equals(upper)) windSickleDurability = clamped;
        if ("WATER_STAFF".equals(upper)) waterStaffDurability = clamped;
    }

    public boolean hasWeaponInInventory(Player player, String key) {
        if (player == null) return false;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                org.bukkit.NamespacedKey nkey = new org.bukkit.NamespacedKey(Plugin.AmonPackPlugin.plugin, "dungeon_weapon_type");
                if (pdc.has(nkey, org.bukkit.persistence.PersistentDataType.STRING)) {
                    if (key.equals(pdc.get(nkey, org.bukkit.persistence.PersistentDataType.STRING))) {
                        return true;
                    }
                }
            }
        }
        org.bukkit.inventory.ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.hasItemMeta()) {
            org.bukkit.persistence.PersistentDataContainer pdc = off.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey nkey = new org.bukkit.NamespacedKey(Plugin.AmonPackPlugin.plugin, "dungeon_weapon_type");
            if (pdc.has(nkey, org.bukkit.persistence.PersistentDataType.STRING)) {
                if (key.equals(pdc.get(nkey, org.bukkit.persistence.PersistentDataType.STRING))) {
                    return true;
                }
            }
        }
        return false;
    }
}
