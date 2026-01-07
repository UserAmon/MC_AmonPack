package Mechanics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import methods_plugins.AmonPackPlugin;
import java.io.File;

public class BossScrollManager implements Listener {

    private static BossScrollManager instance;
    private FileConfiguration config;
    private Map<UUID, BossSession> activeBosses = new HashMap<>();
    private Map<Location, LootChest> activeChests = new HashMap<>();

    public BossScrollManager() {
        instance = this;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, AmonPackPlugin.plugin);
    }

    public static BossScrollManager getInstance() {
        if (instance == null) {
            new BossScrollManager();
        }
        return instance;
    }

    private void loadConfig() {
        config = AmonPackPlugin.getConfigs_menager().getBoss_Config();
    }

    public void reloadConfig() {
        loadConfig();
    }

    public void summonBoss(Location location, String bossId) {

        System.out.println(ChatColor.RED + "Boss configuration not found: " + bossId);

        String mmName = config.getString("Bosses." + bossId + ".MythicMobName");
        String type = config.getString("Bosses." + bossId + ".Type");
        int count = type.equalsIgnoreCase("GROUP") ? config.getInt("Bosses." + bossId + ".GroupSize") : 1;

        BossSession session = new BossSession(bossId, count);

        for (int i = 0; i < count; i++) {
            Location loc = location.clone().add((Math.random() - 0.5) * 5, 0, (Math.random() - 0.5) * 5);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mm m spawn " + mmName + " 1 " + loc.getWorld().getName()
                    + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ());

            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double radius = 5;
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                        if (e instanceof LivingEntity && !activeBosses.containsKey(e.getUniqueId())) {
                            activeBosses.put(e.getUniqueId(), session);
                            session.addEntity(e.getUniqueId());
                            break;
                        }
                    }
                }
            }.runTaskLater(AmonPackPlugin.plugin, 5L); // Wait 5 ticks for spawn
        }

        System.out.println(ChatColor.GREEN + "Ritual complete! " + config.getString("Bosses." + bossId + ".DisplayName")
                + ChatColor.GREEN + " has been summoned!");
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (activeBosses.containsKey(event.getEntity().getUniqueId())) {
            if (event.getDamager() instanceof Player player) {
                BossSession session = activeBosses.get(event.getEntity().getUniqueId());
                session.addDamage(player.getUniqueId(), event.getFinalDamage());
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (activeBosses.containsKey(event.getEntity().getUniqueId())) {
            BossSession session = activeBosses.remove(event.getEntity().getUniqueId());
            session.removeEntity(event.getEntity().getUniqueId());

            if (session.isFinished()) {
                spawnLootChest(event.getEntity().getLocation(), session);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
            if (activeChests.containsKey(event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
                LootChest chest = activeChests.get(event.getClickedBlock().getLocation());
                chest.open(event.getPlayer());
            }
        }
    }

    private void spawnLootChest(Location loc, BossSession session) {
        Block block = loc.getBlock();
        block.setType(Material.CHEST);

        LootChest chest = new LootChest(session);
        activeChests.put(block.getLocation(), chest);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeChests.containsKey(block.getLocation())) {
                    activeChests.remove(block.getLocation());
                    block.setType(Material.AIR);
                    block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.5, 0.5,
                            0.5, 0);
                }
            }
        }.runTaskLater(AmonPackPlugin.plugin, 1200L);

        Bukkit.broadcastMessage(ChatColor.GOLD + "The Boss has been defeated! A loot chest has appeared!");
    }


    private class BossSession {
        String bossId;
        int remainingEntities;
        Map<UUID, Double> damageMap = new HashMap<>();
        List<UUID> entityIds = new ArrayList<>();

        public BossSession(String bossId, int count) {
            this.bossId = bossId;
            this.remainingEntities = count;
        }

        public void addEntity(UUID uuid) {
            entityIds.add(uuid);
        }

        public void addDamage(UUID player, double damage) {
            damageMap.put(player, damageMap.getOrDefault(player, 0.0) + damage);
        }

        public void removeEntity(UUID uuid) {
            remainingEntities--;
        }

        public boolean isFinished() {
            return remainingEntities <= 0;
        }
    }

    private class LootChest {
        BossSession session;
        Map<UUID, List<ItemStack>> playerLoot = new HashMap<>();

        public LootChest(BossSession session) {
            this.session = session;
            generateLoot();
        }

        private void generateLoot() {
            List<Map<?, ?>> lootTable = config.getMapList("Bosses." + session.bossId + ".LootTable");

            for (UUID playerUuid : session.damageMap.keySet()) {
                List<ItemStack> items = new ArrayList<>();
                // Simple logic: Give loot based on chance.
                // Could scale with damage, but for now just give loot to everyone who
                // participated.

                for (Map<?, ?> entry : lootTable) {
                    double chance = (Double) entry.get("Chance");
                    if (Math.random() <= chance) {
                        String matName = (String) entry.get("Material");
                        int amount = (Integer) entry.get("Amount");
                        items.add(new ItemStack(Material.valueOf(matName), amount));
                    }
                }
                playerLoot.put(playerUuid, items);
            }
        }

        public void open(Player player) {
            if (playerLoot.containsKey(player.getUniqueId())) {
                Inventory inv = Bukkit.createInventory(null, 27, "Boss Loot");
                for (ItemStack item : playerLoot.get(player.getUniqueId())) {
                    inv.addItem(item);
                }
                player.openInventory(inv);
                playerLoot.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "You have claimed your loot!");
            } else {
                player.sendMessage(
                        ChatColor.RED + "You did not participate in this fight or have already claimed your loot.");
            }
        }
    }
}
