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
        FORCE_FAIL,
        GIVE_ITEM,
        SHIELD_REMOVE,
        MINING_ZONE,
        BUILDING_ZONE
    }

    private final EffectType type;

    private String message;

    private double x, y, z;
    private List<ItemStack> givenTools = new ArrayList<>();
    private List<Double> xList = new ArrayList<>();
    private List<Double> yList = new ArrayList<>();
    private List<Double> zList = new ArrayList<>();

    private double x1, y1, z1, x2, y2, z2;
    private Material material;

    private String mobName;
    private int amount = 1;
    private int level = 1;
    private double range = 0.0;

    private String chestType;
    private int interval = 5;
    private String itemId;
    private String blessingType = "Chest_General";
    private int slotsCount = 3;

    public DungeonEffect(EffectType type) {
        this.type = type;
    }

    public DungeonEffect(EffectType type, String itemId, int amount) {
        this.type = type;
        this.itemId = itemId;
        this.amount = amount;
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
                Location tpLoc = getResolvedLocation(instance);
                for (Player player : instance.getOnlinePlayers()) {
                    player.teleport(tpLoc);
                }
                break;

            case SPAWN_MOB:
                Random rand = new Random();
                Location mobLoc = getResolvedLocation(instance);
                for (int i = 0; i < amount; i++) {
                    Encounter activeEnc = instance.getActiveEncounter();
                    if (activeEnc != null && activeEnc.getMaxMobs() > 0) {
                        int currentMobs = 0;
                        for (org.bukkit.entity.Entity ent : instance.getWorld().getEntities()) {
                            if (ent instanceof org.bukkit.entity.LivingEntity && !(ent instanceof Player)) {
                                currentMobs++;
                            }
                        }
                        if (currentMobs >= activeEnc.getMaxMobs()) {
                            break;
                        }
                    }
                    double rx = mobLoc.getX() + (range > 0 ? (rand.nextDouble() * range * 2 - range) : 0);
                    double rz = mobLoc.getZ() + (range > 0 ? (rand.nextDouble() * range * 2 - range) : 0);
                    
                    String command = "mm mobs spawn -s " + mobName + ":" + level + " 1 " +
                                     instance.getWorld().getName() + "," + rx + "," + mobLoc.getY() + "," + rz;
                    
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
                    double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    player.setHealth(maxHealth);
                    player.setFoodLevel(20);
                }
                break;

            case SPAWN_CHEST:
                Location chestLoc = getResolvedLocation(instance);
                Block block = chestLoc.getBlock();
                block.setType(Material.CHEST);
                
                instance.registerLootChest(block.getLocation(), chestType == null ? "ROGUELITE_CHEST" : chestType, blessingType, slotsCount);
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
                        Encounter enc = instance.getActiveEncounter();
                        ConsoleCommandSender cmdConsole = Bukkit.getServer().getConsoleSender();
                        Random spawnRand = new Random();
                        Location spawnLoc = getResolvedLocation(instance);
                        for (int i = 0; i < amount; i++) {
                            if (enc != null && enc.getMaxMobs() > 0) {
                                int currentMobs = 0;
                                for (org.bukkit.entity.Entity ent : instance.getWorld().getEntities()) {
                                    if (ent instanceof org.bukkit.entity.LivingEntity && !(ent instanceof Player)) {
                                        currentMobs++;
                                    }
                                }
                                if (currentMobs >= enc.getMaxMobs()) {
                                    break;
                                }
                            }
                            double rx = spawnLoc.getX() + (range > 0 ? (spawnRand.nextDouble() * range * 2 - range) : 0);
                            double rz = spawnLoc.getZ() + (range > 0 ? (spawnRand.nextDouble() * range * 2 - range) : 0);
                            String command = "mm mobs spawn -s " + mobName + ":" + level + " 1 " +
                                             instance.getWorld().getName() + "," + rx + "," + spawnLoc.getY() + "," + rz;
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
                Location pullLoc = getResolvedLocation(instance);
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
            case GIVE_ITEM:
                int giveQty = amount > 0 ? amount : 1;
                ItemStack stackToGive = null;
                DungeonCustomItem customItem = instance.getTemplate().getCustomItems().get(itemId);
                if (customItem != null) {
                    stackToGive = customItem.toItemStack();
                    stackToGive.setAmount(giveQty);
                } else {
                    Material matToGive = Material.getMaterial(itemId.toUpperCase());
                    if (matToGive != null) {
                        stackToGive = new ItemStack(matToGive, giveQty);
                    }
                }
                if (stackToGive != null) {
                    for (Player player : instance.getOnlinePlayers()) {
                        if (!instance.isPlayerSpectator(player)) {
                            ItemStack copy = stackToGive.clone();
                            if (player.getInventory().getItemInMainHand().getType().isAir()) {
                                player.getInventory().setItemInMainHand(copy);
                            } else {
                                player.getInventory().addItem(copy);
                            }
                        }
                    }
                }
                break;

            case SHIELD_REMOVE:
                Location removeLoc = getResolvedLocation(instance);
                double rad = range > 0.0 ? range : 5.0;
                instance.removeShieldsInArea(removeLoc, rad);
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

    public void setRange(double range) {
        this.range = range;
    }

    public String getChestType() {
        return chestType;
    }

    public int getInterval() {
        return interval;
    }

    public String getItemId() {
        return itemId;
    }

    public String getBlessingType() {
        return blessingType;
    }

    public void setBlessingType(String blessingType) {
        this.blessingType = blessingType;
    }

    public int getSlotsCount() {
        return slotsCount;
    }

    public void setSlotsCount(int slotsCount) {
        this.slotsCount = slotsCount;
    }

    public List<Double> getXList() {
        return xList;
    }

    public void setXList(List<Double> xList) {
        this.xList = xList;
        if (!xList.isEmpty()) {
            this.x = xList.get(0);
        }
    }

    public List<Double> getYList() {
        return yList;
    }

    public void setYList(List<Double> yList) {
        this.yList = yList;
        if (!yList.isEmpty()) {
            this.y = yList.get(0);
        }
    }

    public List<Double> getZList() {
        return zList;
    }

    public void setZList(List<Double> zList) {
        this.zList = zList;
        if (!zList.isEmpty()) {
            this.z = zList.get(0);
        }
    }

    public List<ItemStack> getGivenTools() { return givenTools; }
    public void setGivenTools(List<ItemStack> givenTools) { this.givenTools = givenTools; }

    public Location getResolvedLocation(DungeonInstance instance) {
        return instance.getResolvedLocation(this);
    }
}
