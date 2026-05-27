package RPG.Dungeons;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static Plugin.AmonPackPlugin.FastEasyStack;

public class DungeonEffect {

    public enum EffectType {
        SEND_MESSAGE,          // Send message to all players in the instance
        TELEPORT_PLAYERS,      // Teleport players to specific coordinates
        SPAWN_MOB,             // Spawn MythicMob/vanilla mob
        OPEN_DOOR,             // Set block cuboid to AIR
        CLOSE_DOOR,            // Set block cuboid to Material
        GIVE_READY_COMPASS,    // Give the ready compass prep item
        SPAWN_CHEST,           // Spawns a physical loot chest
        COMPLETE_DUNGEON       // Completes the dungeon and triggers rewards
    }

    private final EffectType type;

    // SEND_MESSAGE fields
    private String message;

    // TELEPORT_PLAYERS & SPAWN_MOB & SPAWN_CHEST fields
    private double x, y, z;

    // OPEN_DOOR & CLOSE_DOOR fields
    private double x1, y1, z1, x2, y2, z2;
    private Material material;

    // SPAWN_MOB fields
    private String mobName;
    private int amount = 1;
    private int level = 1;
    private double range = 0.0;

    // SPAWN_CHEST fields
    private String chestType; // "ROGUELITE_CHEST" or custom

    public DungeonEffect(EffectType type) {
        this.type = type;
    }

    // Constructor for SEND_MESSAGE
    public DungeonEffect(String message) {
        this.type = EffectType.SEND_MESSAGE;
        this.message = message;
    }

    // Constructor for TELEPORT_PLAYERS
    public DungeonEffect(double x, double y, double z) {
        this.type = EffectType.TELEPORT_PLAYERS;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Constructor for SPAWN_MOB
    public DungeonEffect(String mobName, int amount, int level, double x, double y, double z, double range) {
        this.type = EffectType.SPAWN_MOB;
        this.mobName = mobName;
        this.amount = amount;
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.range = range;
    }

    // Constructor for OPEN_DOOR / CLOSE_DOOR
    public DungeonEffect(EffectType type, double x1, double y1, double z1, double x2, double y2, double z2, Material material) {
        this.type = type;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.material = material;
    }

    // Constructor for SPAWN_CHEST
    public DungeonEffect(double x, double y, double z, String chestType) {
        this.type = EffectType.SPAWN_CHEST;
        this.x = x;
        this.y = y;
        this.z = z;
        this.chestType = chestType;
    }

    /**
     * Executes this effect in the context of the active DungeonInstance.
     */
    public void execute(DungeonInstance instance) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        
        switch (type) {
            case SEND_MESSAGE:
                String formattedMsg = ChatColor.translateAlternateColorCodes('&', message);
                instance.broadcast(formattedMsg);
                break;

            case TELEPORT_PLAYERS:
                Location tpLoc = new Location(instance.getWorld(), x, y, z);
                for (Player player : instance.getOnlinePlayers()) {
                    player.teleport(tpLoc);
                }
                break;

            case SPAWN_MOB:
                Random rand = new Random();
                for (int i = 0; i < amount; i++) {
                    double rx = x + (range > 0 ? (rand.nextDouble() * range * 2 - range) : 0);
                    double rz = z + (range > 0 ? (rand.nextDouble() * range * 2 - range) : 0);
                    
                    // Construct MythicMobs console command spawn command
                    // mm mobs spawn -s [Name]:[Lvl] 1 [World],[X],[Y],[Z]
                    String command = "mm mobs spawn -s " + mobName + ":" + level + " 1 " +
                                     instance.getWorld().getName() + "," + rx + "," + y + "," + rz;
                    
                    Bukkit.dispatchCommand(console, command);
                }
                break;

            case OPEN_DOOR:
                manipulateBlocks(instance.getWorld(), Material.AIR);
                break;

            case CLOSE_DOOR:
                manipulateBlocks(instance.getWorld(), material == null ? Material.STONE : material);
                break;

            case GIVE_READY_COMPASS:
                ItemStack compass = FastEasyStack(Material.COMPASS, ChatColor.RED + "Gotowy?");
                ItemStack skillChest = FastEasyStack(Material.CHEST, ChatColor.GREEN + "Menu Umiejętności");
                for (Player player : instance.getOnlinePlayers()) {
                    // Remove existing first to prevent duplicate
                    player.getInventory().remove(Material.COMPASS);
                    ItemStack[] contents = player.getInventory().getContents();
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack is = contents[i];
                        if (is != null && is.getType() == Material.CHEST && is.hasItemMeta() && is.getItemMeta().getDisplayName().contains("Menu Umiejętności")) {
                            player.getInventory().setItem(i, null);
                        }
                    }
                    player.getInventory().addItem(compass);
                    player.getInventory().addItem(skillChest);
                }
                break;

            case SPAWN_CHEST:
                Location chestLoc = new Location(instance.getWorld(), x, y, z);
                Block block = chestLoc.getBlock();
                block.setType(Material.CHEST);
                
                // Track this chest block in the instance as an interactive loot chest
                instance.registerLootChest(block.getLocation(), chestType == null ? "ROGUELITE_CHEST" : chestType);
                instance.preGenerateChestGuis(block.getLocation());
                break;

            case COMPLETE_DUNGEON:
                instance.completeDungeon();
                break;
        }
    }

    /**
     * Fills a cuboid volume with the specified block material.
     */
    private void manipulateBlocks(org.bukkit.World world, Material mat) {
        int minX = (int) Math.min(x1, x2);
        int minY = (int) Math.min(y1, y2);
        int minZ = (int) Math.min(z1, z2);
        int maxX = (int) Math.max(x1, x2);
        int maxY = (int) Math.max(y1, y2);
        int maxZ = (int) Math.max(z1, z2);

        for (int sx = minX; sx <= maxX; sx++) {
            for (int sy = minY; sy <= maxY; sy++) {
                for (int sz = minZ; sz <= maxZ; sz++) {
                    world.getBlockAt(sx, sy, sz).setType(mat);
                }
            }
        }
    }

    public EffectType getType() {
        return type;
    }

    public String getMessage() {
        return message;
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

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getZ1() {
        return z1;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    public double getZ2() {
        return z2;
    }

    public Material getMaterial() {
        return material;
    }

    public String getMobName() {
        return mobName;
    }

    public int getAmount() {
        return amount;
    }

    public int getLevel() {
        return level;
    }

    public double getRange() {
        return range;
    }

    public String getChestType() {
        return chestType;
    }
}
