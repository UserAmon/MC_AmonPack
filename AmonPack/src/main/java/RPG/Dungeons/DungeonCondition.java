package RPG.Dungeons;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DungeonCondition {

    public enum ConditionType {
        ALL_PLAYERS_READY,          // Wszyscy gracze zatwierdzili gotowosc kompasem
        PLAYER_ENTER_AREA,          // Gracz wszedł w dany okrag/obszar
        KILL_MOBS,                  // Pokonanie wyznaczonej liczby mobow o danej nazwie
        INTERACT_BLOCK_WITH_ITEM    // PPM na dany blok trzymajac konkretny przedmiot
    }

    private final ConditionType type;
    
    // PLAYER_ENTER_AREA & INTERACT_BLOCK_WITH_ITEM fields
    private double x, y, z;
    private double radius;

    // KILL_MOBS fields
    private String mobName;
    private int amount;

    // INTERACT_BLOCK_WITH_ITEM fields
    private Material blockMaterial;
    private Material itemMaterial;
    private String itemDisplayName;

    public DungeonCondition(ConditionType type) {
        this.type = type;
    }

    // Constructor for PLAYER_ENTER_AREA
    public DungeonCondition(double x, double y, double z, double radius) {
        this.type = ConditionType.PLAYER_ENTER_AREA;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    // Constructor for KILL_MOBS
    public DungeonCondition(String mobName, int amount) {
        this.type = ConditionType.KILL_MOBS;
        this.mobName = mobName;
        this.amount = amount;
    }

    // Constructor for INTERACT_BLOCK_WITH_ITEM
    public DungeonCondition(double x, double y, double z, Material blockMaterial, Material itemMaterial, String itemDisplayName) {
        this.type = ConditionType.INTERACT_BLOCK_WITH_ITEM;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockMaterial = blockMaterial;
        this.itemMaterial = itemMaterial;
        this.itemDisplayName = itemDisplayName;
    }

    /**
     * Checks if this condition is satisfied inside a running DungeonInstance.
     */
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

            default:
                return false;
        }
    }

    /**
     * Specialized check for block interaction condition.
     */
    public boolean isMetInteract(Location blockLoc, Material clickedBlock, ItemStack heldItem) {
        if (type != ConditionType.INTERACT_BLOCK_WITH_ITEM) return false;

        // Check distance/location
        Location targetLoc = new Location(blockLoc.getWorld(), x, y, z);
        if (blockLoc.distanceSquared(targetLoc) > 1.5) return false;

        // Check block material
        if (blockMaterial != null && clickedBlock != blockMaterial) return false;

        // Check held item material
        if (itemMaterial != null) {
            if (heldItem == null || heldItem.getType() != itemMaterial) return false;
            
            // Check item display name (if specified)
            if (itemDisplayName != null) {
                if (!heldItem.hasItemMeta() || heldItem.getItemMeta().getDisplayName() == null) return false;
                
                String cleanMetaName = ChatColor.stripColor(heldItem.getItemMeta().getDisplayName());
                String cleanTargetName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', itemDisplayName));
                
                if (!cleanMetaName.equalsIgnoreCase(cleanTargetName)) return false;
            }
        } else {
            // If itemMaterial is null, player should click with empty hand or anything
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
}
