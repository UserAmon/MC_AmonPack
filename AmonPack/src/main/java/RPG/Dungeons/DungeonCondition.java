package RPG.Dungeons;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class DungeonCondition {

    public enum ConditionType {
        ALL_PLAYERS_READY,
        PLAYER_ENTER_AREA,
        KILL_MOBS,
        INTERACT_BLOCK_WITH_ITEM,
        ZONE,
        THROW_AT,
        THROW_AT_ENEMY,
        DROP_ON_DEATH
    }

    private final ConditionType type;
    
    private double x, y, z;
    private double radius;

    private String mobName;
    private int amount;

    private Material blockMaterial;
    private Material itemMaterial;
    private String itemDisplayName;

    private String customItemId;
    private double chance;
    private int timeRequired;
    private List<DungeonEffect> onCompleteEffects = new ArrayList<>();

    public DungeonCondition(ConditionType type) {
        this.type = type;
    }

    public DungeonCondition(double x, double y, double z, double radius) {
        this.type = ConditionType.PLAYER_ENTER_AREA;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    public DungeonCondition(String mobName, int amount) {
        this.type = ConditionType.KILL_MOBS;
        this.mobName = mobName;
        this.amount = amount;
    }

    public DungeonCondition(double x, double y, double z, Material blockMaterial, Material itemMaterial, String itemDisplayName) {
        this.type = ConditionType.INTERACT_BLOCK_WITH_ITEM;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockMaterial = blockMaterial;
        this.itemMaterial = itemMaterial;
        this.itemDisplayName = itemDisplayName;
    }

    public DungeonCondition(double x, double y, double z, double radius, int timeRequired) {
        this.type = ConditionType.ZONE;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.timeRequired = timeRequired;
    }

    public DungeonCondition(ConditionType type, double x, double y, double z, double radius, String customItemId, int amount) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.customItemId = customItemId;
        this.amount = amount;
    }

    public DungeonCondition(ConditionType type, String mobName, String customItemId, int amount, double x, double y, double z) {
        this.type = type;
        this.mobName = mobName;
        this.customItemId = customItemId;
        this.amount = amount;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public DungeonCondition(String mobName, String customItemId, double chance) {
        this.type = ConditionType.DROP_ON_DEATH;
        this.mobName = mobName;
        this.customItemId = customItemId;
        this.chance = chance;
    }

    public boolean isMet(DungeonInstance instance) {
        switch (type) {
            case ALL_PLAYERS_READY:
                return instance.areAllPlayersReady();

            case PLAYER_ENTER_AREA:
                Location center = new Location(instance.getWorld(), x, y, z);
                double radiusSq = radius * radius;
                for (Player player : instance.getOnlinePlayers()) {
                    if (player.getLocation().distanceSquared(center) <= radiusSq) {
                        return true;
                    }
                }
                return false;

            case KILL_MOBS:
                int currentKills = instance.getKilledMobsCount(mobName);
                return currentKills >= amount;

            case ZONE:
                return instance.getZoneProgress(this) >= timeRequired;

            case THROW_AT:
                return instance.getThrowHits(this) >= amount;

            case THROW_AT_ENEMY:
                return instance.getThrowHits(this) >= amount;

            case DROP_ON_DEATH:
                return true;

            default:
                return false;
        }
    }

    public boolean isMetInteract(Location blockLoc, Material clickedBlock, ItemStack heldItem) {
        if (type != ConditionType.INTERACT_BLOCK_WITH_ITEM) return false;

        Location targetLoc = new Location(blockLoc.getWorld(), x, y, z);
        if (blockLoc.distanceSquared(targetLoc) > 1.5) return false;

        if (blockMaterial != null && clickedBlock != blockMaterial) return false;

        if (itemMaterial != null) {
            if (heldItem == null || heldItem.getType() != itemMaterial) return false;
            
            if (itemDisplayName != null) {
                if (!heldItem.hasItemMeta() || heldItem.getItemMeta().getDisplayName() == null) return false;
                
                String cleanMetaName = ChatColor.stripColor(heldItem.getItemMeta().getDisplayName());
                String cleanTargetName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', itemDisplayName));
                
                if (!cleanMetaName.equalsIgnoreCase(cleanTargetName)) return false;
            }
        } else if (customItemId != null) {
            if (heldItem == null) return false;
            org.bukkit.persistence.PersistentDataContainer pdc = heldItem.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(Plugin.AmonPackPlugin.plugin, "dungeon_item_id");
            if (!pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) return false;
            String itemId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
            if (!customItemId.equalsIgnoreCase(itemId)) return false;
        } else {
            if (heldItem != null && !heldItem.getType().isAir()) return false;
        }

        return true;
    }

    public ConditionType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getRadius() {
        return radius;
    }

    public String getMobName() {
        return mobName;
    }

    public int getAmount() {
        return amount;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public String getItemDisplayName() {
        return itemDisplayName;
    }

    public String getCustomItemId() {
        return customItemId;
    }

    public double getChance() {
        return chance;
    }

    public int getTimeRequired() {
        return timeRequired;
    }

    public List<DungeonEffect> getOnCompleteEffects() {
        return onCompleteEffects;
    }

    public void setOnCompleteEffects(List<DungeonEffect> onCompleteEffects) {
        this.onCompleteEffects = onCompleteEffects;
    }

    public void setCustomItemId(String customItemId) {
        this.customItemId = customItemId;
    }
}
