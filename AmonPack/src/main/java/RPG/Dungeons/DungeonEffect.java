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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import static Plugin.AmonPackPlugin.FastEasyStack;

public class DungeonEffect {

    public enum EffectType {
        SEND_MESSAGE,
        TELEPORT_PLAYERS,
        SPAWN_MOB,
        OPEN_DOOR,
        CLOSE_DOOR,
        GIVE_READY_COMPASS,
        SPAWN_CHEST,
        COMPLETE_DUNGEON,
        SPAWN_UNTIL,
        KNOCKBACK,
        PULL,
        DAMAGE,
        FORCE_FAIL
    }

    private final EffectType type;

    private String message;

    private double x, y, z;

    private double x1, y1, z1, x2, y2, z2;
    private Material material;

    private String mobName;
    private int amount = 1;
    private int level = 1;
    private double range = 0.0;

    private String chestType;
    private int interval = 5;

    public DungeonEffect(EffectType type) {
        this.type = type;
    }

    public DungeonEffect(String message) {
        this.type = EffectType.SEND_MESSAGE;
        this.message = message;
    }

    public DungeonEffect(double x, double y, double z) {
        this.type = EffectType.TELEPORT_PLAYERS;
        this.x = x;
        this.y = y;
        this.z = z;
    }

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

    public DungeonEffect(double x, double y, double z, String chestType) {
        this.type = EffectType.SPAWN_CHEST;
        this.x = x;
        this.y = y;
        this.z = z;
        this.chestType = chestType;
    }

    public DungeonEffect(EffectType type, String mobName, int amount, int level, double x, double y, double z, double range, int interval) {
        this.type = type;
        this.mobName = mobName;
        this.amount = amount;
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.range = range;
        this.interval = interval;
    }

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
                
                instance.registerLootChest(block.getLocation(), chestType == null ? "ROGUELITE_CHEST" : chestType);
                instance.preGenerateChestGuis(block.getLocation());
                break;

            case COMPLETE_DUNGEON:
                instance.completeDungeon();
                break;

            case SPAWN_UNTIL:
                final DungeonEffect self = this;
                org.bukkit.scheduler.BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (instance.isFinished() || instance.getActiveEncounter() == null || !instance.getActiveEncounter().getEffects().contains(self)) {
                            cancel();
                            return;
                        }
                        ConsoleCommandSender cmdConsole = Bukkit.getServer().getConsoleSender();
                        Random spawnRand = new Random();
                        for (int i = 0; i < amount; i++) {
                            double rx = x + (range > 0 ? (spawnRand.nextDouble() * range * 2 - range) : 0);
                            double rz = z + (range > 0 ? (spawnRand.nextDouble() * range * 2 - range) : 0);
                            String command = "mm mobs spawn -s " + mobName + ":" + level + " 1 " +
                                             instance.getWorld().getName() + "," + rx + "," + y + "," + rz;
                            Bukkit.dispatchCommand(cmdConsole, command);
                        }
                    }
                }.runTaskTimer(Plugin.AmonPackPlugin.plugin, 0L, interval * 20L);
                instance.addSpawnUntilTaskId(task.getTaskId());
                break;

            case KNOCKBACK:
                Vector kbVec = new Vector(x, y, z);
                for (Player player : instance.getOnlinePlayers()) {
                    if (!instance.isPlayerSpectator(player)) {
                        player.setVelocity(kbVec);
                    }
                }
                break;

            case PULL:
                Location pullLoc = new Location(instance.getWorld(), x, y, z);
                double force = amount > 0 ? amount : 1.0;
                for (Player player : instance.getOnlinePlayers()) {
                    if (!instance.isPlayerSpectator(player)) {
                        Location pLoc = player.getLocation();
                        Vector dir = pullLoc.toVector().subtract(pLoc.toVector());
                        if (dir.lengthSquared() > 0.25) {
                            dir.normalize().multiply(force);
                            player.setVelocity(dir);
                        }
                    }
                }
                break;

            case DAMAGE:
                double dmgVal = amount > 0 ? amount : 2.0;
                for (Player player : instance.getOnlinePlayers()) {
                    if (!instance.isPlayerSpectator(player)) {
                        player.damage(dmgVal);
                    }
                }
                break;

            case FORCE_FAIL:
                new org.bukkit.scheduler.BukkitRunnable() {
                    private int ticks = 0;
                    @Override
                    public void run() {
                        if (instance.isFinished()) {
                            cancel();
                            return;
                        }
                        ticks++;
                        List<Player> playersToDamage = new ArrayList<>();
                        for (Player p : instance.getOnlinePlayers()) {
                            if (!instance.isPlayerSpectator(p)) {
                                playersToDamage.add(p);
                            }
                        }
                        if (playersToDamage.isEmpty() || ticks > 10) {
                            for (Player p : instance.getOnlinePlayers()) {
                                if (!instance.isPlayerSpectator(p)) {
                                    p.setHealth(0.0);
                                }
                            }
                            instance.broadcast(ChatColor.RED + "[Dungeons] Dungeon zakończył się sromotną klęską!");
                            instance.cleanup();
                            cancel();
                            return;
                        }
                        for (Player p : playersToDamage) {
                            double maxHp = p.getMaxHealth();
                            double currentHp = p.getHealth();
                            double damagePerSec = maxHp / 10.0;
                            double nextHp = Math.max(0.0, currentHp - damagePerSec);
                            p.setHealth(nextHp);
                            if (nextHp <= 0.0) {
                                p.damage(99999.0);
                            }
                            int amp = ticks / 2;
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, amp));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, amp));
                            Location pLoc = p.getLocation();
                            p.getWorld().spawnParticle(Particle.LARGE_SMOKE, pLoc, ticks * 5, 0.5, 1.0, 0.5, 0.05);
                            p.getWorld().spawnParticle(Particle.DUST, pLoc.add(0, 1, 0), ticks * 10, 0.5, 0.5, 0.5, new Particle.DustOptions(Color.RED, 1.5f));
                        }
                    }
                }.runTaskTimer(Plugin.AmonPackPlugin.plugin, 0L, 20L);
                break;
        }
    }

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

    public int getInterval() {
        return interval;
    }
}
