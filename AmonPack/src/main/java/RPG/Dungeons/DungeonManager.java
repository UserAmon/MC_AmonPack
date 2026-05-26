package RPG.Dungeons;

import RPG.Levels.BendingTree.PlayerBendingBranch;
import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DungeonManager implements Listener {

    private static DungeonManager instance;
    private final Map<String, Dungeon> templates = new HashMap<>();
    private final Map<World, DungeonInstance> activeInstances = new HashMap<>();
    
    public DungeonManager() {
        instance = this;
        loadTemplates();
        startUpdateTask();
    }

    public static DungeonManager getInstance() {
        return instance;
    }

    /**
     * Scans and loads all dungeon template YAML scripts from plugins/AmonPack/dungeons/
     */
    public void loadTemplates() {
        templates.clear();
        File dungeonsFolder = new File(AmonPackPlugin.plugin.getDataFolder(), "dungeons");
        if (!dungeonsFolder.exists()) {
            dungeonsFolder.mkdirs();
        }

        File[] files = dungeonsFolder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) return;

        for (File file : files) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = file.getName().replace(".yml", "").replace(".yaml", "");
                
                String name = config.getString("name", id);
                String schematic = config.getString("schematic", id + ".schem");
                
                // Read spawn & paste locations
                Vector pasteLoc = new Vector(
                    config.getDouble("paste-location.x", 0),
                    config.getDouble("paste-location.y", 60),
                    config.getDouble("paste-location.z", 0)
                );
                Vector spawnLoc = new Vector(
                    config.getDouble("spawn-location.x", 0),
                    config.getDouble("spawn-location.y", 61),
                    config.getDouble("spawn-location.z", 0)
                );

                // Exit location
                String exitWorld = config.getString("exit-location.world", "world");
                Vector exitLoc = new Vector(
                    config.getDouble("exit-location.x", 0),
                    config.getDouble("exit-location.y", 64),
                    config.getDouble("exit-location.z", 0)
                );

                String initialEncounter = config.getString("initial-encounter", "ENC1");

                // Parse Rewards
                int xp = config.getInt("rewards.dungeon-xp", 0);
                double money = config.getDouble("rewards.money", 0.0);
                List<String> commands = config.getStringList("rewards.commands");
                List<ItemStack> items = new ArrayList<>();
                
                ConfigurationSection itemsSection = config.getConfigurationSection("rewards.items");
                if (itemsSection != null) {
                    // Alternative standard list structure
                    for (String key : itemsSection.getKeys(false)) {
                        String path = "rewards.items." + key;
                        Material mat = Material.getMaterial(config.getString(path + ".material", ""));
                        if (mat != null) {
                            int amount = config.getInt(path + ".amount", 1);
                            ItemStack item = new ItemStack(mat, amount);
                            ItemMeta meta = item.getItemMeta();
                            if (meta != null) {
                                String dName = config.getString(path + ".display-name");
                                if (dName != null) {
                                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', dName));
                                }
                                List<String> lore = config.getStringList(path + ".lore");
                                if (lore != null && !lore.isEmpty()) {
                                    List<String> coloredLore = new ArrayList<>();
                                    for (String line : lore) {
                                        coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                                    }
                                    meta.setLore(coloredLore);
                                }
                                item.setItemMeta(meta);
                            }
                            items.add(item);
                        }
                    }
                } else {
                    // Try parsing as simple list of itemstacks if available (standard Bukkit list)
                    List<?> simpleItems = config.getList("rewards.items");
                    if (simpleItems != null) {
                        for (Object obj : simpleItems) {
                            if (obj instanceof ItemStack) {
                                items.add((ItemStack) obj);
                            }
                        }
                    }
                }

                Dungeon.DungeonRewards rewards = new Dungeon.DungeonRewards(xp, money, commands, items);

                // Parse Encounters
                Map<String, Encounter> encounters = new HashMap<>();
                ConfigurationSection encSection = config.getConfigurationSection("encounters");
                if (encSection != null) {
                    for (String encId : encSection.getKeys(false)) {
                        String path = "encounters." + encId;
                        String desc = config.getString(path + ".description", "");
                        
                        // Parse Conditions
                        List<DungeonCondition> conditions = new ArrayList<>();
                        List<Map<?, ?>> condList = config.getMapList(path + ".conditions");
                        for (Map<?, ?> rawMap : condList) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) rawMap;
                            String typeStr = (String) map.get("type");
                            if (typeStr != null) {
                                DungeonCondition.ConditionType cType = DungeonCondition.ConditionType.valueOf(typeStr);
                                switch (cType) {
                                    case ALL_PLAYERS_READY:
                                        conditions.add(new DungeonCondition(DungeonCondition.ConditionType.ALL_PLAYERS_READY));
                                        break;
                                    case PLAYER_ENTER_AREA:
                                        conditions.add(new DungeonCondition(
                                            asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), asDouble(map.get("radius"))
                                        ));
                                        break;
                                    case KILL_MOBS:
                                        conditions.add(new DungeonCondition(
                                            (String) map.get("mob-name"), asInt(map.get("amount"))
                                        ));
                                        break;
                                    case INTERACT_BLOCK_WITH_ITEM:
                                        Material bMat = Material.getMaterial((String) map.getOrDefault("block-material", ""));
                                        Material iMat = Material.getMaterial((String) map.getOrDefault("item-material", ""));
                                        conditions.add(new DungeonCondition(
                                            asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")),
                                            bMat, iMat, (String) map.get("item-display-name")
                                        ));
                                        break;
                                }
                            }
                        }

                        // Parse Effects
                        List<DungeonEffect> effects = new ArrayList<>();
                        List<Map<?, ?>> effList = config.getMapList(path + ".effects");
                        for (Map<?, ?> rawMap : effList) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) rawMap;
                            String typeStr = (String) map.get("type");
                            if (typeStr != null) {
                                DungeonEffect.EffectType eType = DungeonEffect.EffectType.valueOf(typeStr);
                                switch (eType) {
                                    case SEND_MESSAGE:
                                        effects.add(new DungeonEffect((String) map.get("message")));
                                        break;
                                    case TELEPORT_PLAYERS:
                                        effects.add(new DungeonEffect(
                                            asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z"))
                                        ));
                                        break;
                                    case SPAWN_MOB:
                                        effects.add(new DungeonEffect(
                                            (String) map.get("mob-name"), asInt(map.get("amount")), asInt(map.get("level")),
                                            asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), asDouble(map.get("range"))
                                        ));
                                        break;
                                    case OPEN_DOOR:
                                    case CLOSE_DOOR:
                                        Material doorMat = Material.getMaterial((String) map.getOrDefault("material", "STONE"));
                                        effects.add(new DungeonEffect(
                                            eType, asDouble(map.get("x1")), asDouble(map.get("y1")), asDouble(map.get("z1")),
                                            asDouble(map.get("x2")), asDouble(map.get("y2")), asDouble(map.get("z2")), doorMat
                                        ));
                                        break;
                                    case GIVE_READY_COMPASS:
                                        effects.add(new DungeonEffect(DungeonEffect.EffectType.GIVE_READY_COMPASS));
                                        break;
                                    case SPAWN_CHEST:
                                        effects.add(new DungeonEffect(
                                            asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), (String) map.get("type")
                                        ));
                                        break;
                                    case COMPLETE_DUNGEON:
                                        effects.add(new DungeonEffect(DungeonEffect.EffectType.COMPLETE_DUNGEON));
                                        break;
                                }
                            }
                        }

                        // Parse Next Encounters
                        List<String> next = new ArrayList<>();
                        if (config.isList(path + ".next")) {
                            next.addAll(config.getStringList(path + ".next"));
                        } else {
                            String singleNext = config.getString(path + ".next");
                            if (singleNext != null && !singleNext.isEmpty()) {
                                next.add(singleNext);
                            }
                        }

                        encounters.put(encId, new Encounter(encId, desc, conditions, effects, next));
                    }
                }

                List<String> allowedStats = config.getStringList("loot-chest.allowed-stats");
                List<String> allowedBlessings = config.getStringList("loot-chest.allowed-blessings");

                Dungeon dungeon = new Dungeon(id, name, schematic, pasteLoc, spawnLoc, exitWorld, exitLoc, initialEncounter, encounters, rewards, allowedStats, allowedBlessings);
                templates.put(id.toLowerCase(), dungeon);
                System.out.println("[Dungeons] Pomyslnie wczytano szablon lochu: " + id);
            } catch (Exception e) {
                System.err.println("[Dungeons] Blad podczas parsowania pliku " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Starts a new dungeon instance for a party.
     */
    public boolean startDungeon(String dungeonId, List<Player> party) {
        Dungeon template = templates.get(dungeonId.toLowerCase());
        if (template == null) {
            party.get(0).sendMessage(ChatColor.RED + "Dungeon o podanym ID nie istnieje!");
            return false;
        }

        DungeonInstance run = new DungeonInstance(template, party);
        activeInstances.put(run.getWorld(), run);
        
        run.start();
        return true;
    }

    /**
     * Periodic task to update all active dungeon instances.
     */
    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<DungeonInstance> active = new ArrayList<>(activeInstances.values());
                for (DungeonInstance run : active) {
                    try {
                        run.update();
                        
                        // Unregister if finished
                        if (run.isFinished()) {
                            activeInstances.remove(run.getWorld());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 20L); // Update every 1 second
    }

    /**
     * Safely unloads and cleans up all active runs (e.g. on plugin disable/reload).
     */
    public void cleanupAll() {
        List<DungeonInstance> active = new ArrayList<>(activeInstances.values());
        for (DungeonInstance run : active) {
            run.cleanup();
        }
        activeInstances.clear();
    }

    // ==========================================
    // SPIGOT LISTENERS & EVENTS
    // ==========================================

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        World world = victim.getWorld();
        
        DungeonInstance run = activeInstances.get(world);
        if (run == null) return;

        // 1. Log kill to active encounter
        // Match either type or custom display name
        String name = victim.getName();
        String cleanName = ChatColor.stripColor(name);
        run.onMobKill(victim.getType().name());
        run.onMobKill(cleanName);

        // 2. Handle Vampirism blessing
        Player killer = victim.getKiller();
        if (killer != null) {
            DungeonPlayerStats stats = run.getPlayerStats(killer);
            DungeonBlessingManager.handleVampirism(killer, victim, stats);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;

        DungeonPlayerStats stats = run.getPlayerStats(player);
        
        // Handle Dodge blessing
        DungeonBlessingManager.handleDodge(player, event, stats);
    }

    @EventHandler
    public void onPlayerDealsTakesDamage(EntityDamageByEntityEvent event) {
        World world = event.getEntity().getWorld();
        DungeonInstance run = activeInstances.get(world);
        if (run == null) return;

        // Case 1: Player deals damage to mob/entity
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            DungeonPlayerStats stats = run.getPlayerStats(attacker);
            if (stats != null) {
                // Apply DMG multiplier
                double newDmg = stats.calculateOutgoingDamage(event.getDamage());
                event.setDamage(newDmg);
                
                // Handle Adrenaline blessing
                DungeonBlessingManager.handleAdrenaline(attacker, event, stats);
            }
        }

        // Case 2: Player takes damage from mob/entity
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            DungeonPlayerStats stats = run.getPlayerStats(victim);
            if (stats != null) {
                // Apply DEF mitigation
                double newDmg = stats.calculateIncomingDamage(event.getDamage());
                event.setDamage(newDmg);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;

        // 1. Clicked block checks
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            
            // Check if is Loot Chest
            if (run.isRegisteredLootChest(block.getLocation())) {
                event.setCancelled(true);
                DungeonPlayerStats stats = run.getPlayerStats(player);
                
                // Open GUI
                DungeonLootChest gui = new DungeonLootChest(player, stats, run.getTemplate());
                player.openInventory(gui.getInventory());
                
                // Break block with cool effect and remove chest so it is one-time use
                block.setType(Material.AIR);
                block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, Material.CHEST.createBlockData());
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
                run.removeLootChest(block.getLocation());
                return;
            }

            // Check if matches block interact condition for active encounter
            Encounter encounter = run.getActiveEncounter();
            if (encounter != null) {
                for (DungeonCondition condition : encounter.getConditions()) {
                    if (condition.getType() == DungeonCondition.ConditionType.INTERACT_BLOCK_WITH_ITEM) {
                        ItemStack item = event.getItem();
                        if (condition.isMetInteract(block.getLocation(), block.getType(), item)) {
                            // If item is key (or held), let's consume it!
                            if (item != null && item.getAmount() > 0) {
                                int newAmt = item.getAmount() - 1;
                                if (newAmt > 0) {
                                    item.setAmount(newAmt);
                                } else {
                                    player.getInventory().setItemInMainHand(null);
                                }
                            }
                            
                            // Trigger transition
                            run.transitionToNext();
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }

        // 2. Clicked item checks (Compass / Chest)
        ItemStack item = event.getItem();
        if (item != null && event.getAction().name().startsWith("RIGHT_CLICK")) {
            if (item.getType() == Material.COMPASS && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Gotowy?")) {
                event.setCancelled(true);
                run.setPlayerReady(player, !run.areAllPlayersReady());
            } else if (item.getType() == Material.CHEST && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Menu Umiejętności")) {
                event.setCancelled(true);
                // Open global skills bending list
                AmonPackPlugin.levelsBending.OpenBendingSkillMenu(player.getName());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;

        // Validate if custom chest selection inventory
        if (event.getInventory().getHolder() instanceof DungeonLootChest) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            DungeonLootChest gui = (DungeonLootChest) event.getInventory().getHolder();
            DungeonLootChest.RewardOption option = gui.getOptions().get(slot);
            
            if (option != null) {
                DungeonPlayerStats stats = run.getPlayerStats(player);
                PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
                
                // Apply reward
                switch (option.type) {
                    case SKILL:
                        stats.addBoundDungeonSkill(option.value);
                        if (branch != null) {
                            branch.getTemporaryAbilities().add(option.value);
                        }
                        player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.YELLOW + "Odblokowano ruch: " + option.value + " na czas tego dungeonu!");
                        break;
                        
                    case STAT_HP:
                        try {
                            double hpVal = Double.parseDouble(option.value);
                            stats.addHpBoost(hpVal);
                            stats.applyStatsToPlayer(player);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.RED + "Zwiekszono statystyke: +" + hpVal + " Maksymalnego HP!");
                        } catch (NumberFormatException e) {
                            stats.addHpBoost(4.0);
                            stats.applyStatsToPlayer(player);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.RED + "Zwiekszono statystyke: +4 Maksymalnego HP!");
                        }
                        break;
                        
                    case STAT_DEF:
                        try {
                            double defVal = Double.parseDouble(option.value);
                            stats.addDefBoost(defVal);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.BLUE + "Zwiekszono statystyke: +" + defVal + " Obrony (DEF)!");
                        } catch (NumberFormatException e) {
                            stats.addDefBoost(10.0);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.BLUE + "Zwiekszono statystyke: +10 Obrony (DEF)!");
                        }
                        break;
                        
                    case STAT_DMG:
                        try {
                            double dmgVal = Double.parseDouble(option.value);
                            stats.addDmgMultiplier(dmgVal);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.GOLD + "Zwiekszono statystyke: +" + (int)(dmgVal * 100) + "% Zadawanych Obrazen!");
                        } catch (NumberFormatException e) {
                            stats.addDmgMultiplier(0.15);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.GOLD + "Zwiekszono statystyke: +15% Zadawanych Obrazen!");
                        }
                        break;
                        
                    case STAT_SPEED:
                        try {
                            double speedVal = Double.parseDouble(option.value);
                            stats.addSpeedBoost(speedVal);
                            stats.applyStatsToPlayer(player);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.YELLOW + "Zwiekszono statystyke: Predkosc Ruchu!");
                        } catch (NumberFormatException e) {
                            stats.addSpeedBoost(0.02);
                            stats.applyStatsToPlayer(player);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.YELLOW + "Zwiekszono statystyke: +10% Predkosci Ruchu!");
                        }
                        break;
                        
                    case BLESSING:
                        stats.addActiveBlessing(option.value);
                        player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.LIGHT_PURPLE + "Zdobyles Blogoslawienstwo: " + option.value + "!");
                        break;
                }
                
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run != null) {
            run.ejectPlayer(player);
        }
    }

    // ==========================================
    // UTILITY HELPER METHODS
    // ==========================================

    private double asDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        return 0;
    }

    private int asInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return 0;
    }

    public DungeonInstance getActiveInstance(Player player) {
        return activeInstances.get(player.getWorld());
    }
}
