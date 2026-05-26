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

    // Unlocked and bound dungeon abilities for this run
    private final List<String> boundDungeonSkills = new ArrayList<>();
    private final List<String> activeBlessings = new ArrayList<>();

    public DungeonPlayerStats(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
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

        // Reset Max Health
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) {
                player.setHealth(20.0);
            }
        }

        // Reset Speed
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
}
