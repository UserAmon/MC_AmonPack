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
import org.bukkit.Color;
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

    public Map<String, Dungeon> getTemplates() {
        return templates;
    }
    
    public DungeonManager() {
        instance = this;
        loadTemplates();
        startUpdateTask();
    }

    public static DungeonManager getInstance() {
        return instance;
    }

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

                String exitWorld = config.getString("exit-location.world", "world");
                Vector exitLoc = new Vector(
                    config.getDouble("exit-location.x", 0),
                    config.getDouble("exit-location.y", 64),
                    config.getDouble("exit-location.z", 0)
                );

                String initialEncounter = config.getString("initial-encounter", "ENC1");

                int xp = config.getInt("rewards.dungeon-xp", 0);
                double money = config.getDouble("rewards.money", 0.0);
                List<String> commands = config.getStringList("rewards.commands");
                List<ItemStack> items = new ArrayList<>();
                
                ConfigurationSection itemsSection = config.getConfigurationSection("rewards.items");
                if (itemsSection != null) {
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

                Map<String, DungeonCustomItem> customItems = new HashMap<>();
                ConfigurationSection itemsSec = config.getConfigurationSection("items");
                if (itemsSec != null) {
                    for (String key : itemsSec.getKeys(false)) {
                        String ipath = "items." + key;
                        Material mat = Material.getMaterial(config.getString(ipath + ".material", ""));
                        if (mat != null) {
                            String nameStr = config.getString(ipath + ".name", key);
                            if (config.contains(ipath + ".nazwa")) {
                                nameStr = config.getString(ipath + ".nazwa");
                            }
                            List<String> lore = config.getStringList(ipath + ".lore");
                            if (config.contains(ipath + ".opis")) {
                                lore = config.getStringList(ipath + ".opis");
                            }
                            String itype = config.getString(ipath + ".type", "");
                            if (config.contains(ipath + ".typ")) {
                                itype = config.getString(ipath + ".typ");
                            }
                            int modelId = config.getInt(ipath + ".custom-model-id", 0);
                            customItems.put(key, new DungeonCustomItem(key, mat, nameStr, lore, itype, modelId));
                        }
                    }
                }

                Map<String, Encounter> encounters = new HashMap<>();
                ConfigurationSection encSection = config.getConfigurationSection("encounters");
                if (encSection != null) {
                    for (String encId : encSection.getKeys(false)) {
                        String path = "encounters." + encId;
                        String desc = config.getString(path + ".description", "");
                        
                        List<DungeonCondition> conditions = new ArrayList<>();
                        List<Map<?, ?>> condList = config.getMapList(path + ".conditions");
                        for (Map<?, ?> rawMap : condList) {
                            Map<String, Object> map = (Map<String, Object>) rawMap;
                            String typeStr = (String) map.get("type");
                            if (typeStr != null) {
                                String normType = typeStr.toUpperCase().replace(" ", "_").replace("!", "");
                                DungeonCondition.ConditionType cType = null;
                                try {
                                    cType = DungeonCondition.ConditionType.valueOf(normType);
                                } catch (IllegalArgumentException ex) {
                                }
                                if (cType != null) {
                                    List<DungeonEffect> failEffs = new ArrayList<>();
                                    List<?> rawFail = (List<?>) map.get("fail-effects");
                                    if (rawFail != null) {
                                        for (Object rawObj : rawFail) {
                                            if (rawObj instanceof Map) {
                                                DungeonEffect eff = parseSingleEffect((Map<String, Object>) rawObj);
                                                if (eff != null) {
                                                    failEffs.add(eff);
                                                }
                                            }
                                        }
                                    }

                                    List<DungeonEffect> successEffs = new ArrayList<>();
                                    List<?> rawSuccess = (List<?>) map.get("success-effects");
                                    if (rawSuccess != null) {
                                        for (Object rawObj : rawSuccess) {
                                            if (rawObj instanceof Map) {
                                                DungeonEffect eff = parseSingleEffect((Map<String, Object>) rawObj);
                                                if (eff != null) {
                                                    successEffs.add(eff);
                                                }
                                            }
                                        }
                                    }

                                    DungeonCondition cond = null;
                                    switch (cType) {
                                        case ALL_PLAYERS_READY:
                                            cond = new DungeonCondition(DungeonCondition.ConditionType.ALL_PLAYERS_READY);
                                            break;
                                        case PLAYER_ENTER_AREA:
                                            cond = new DungeonCondition(
                                                asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), asDouble(map.get("radius"))
                                            );
                                            cond.setRequiredAllPlayers(map.containsKey("required_all_players") ? (Boolean) map.get("required_all_players") : false);
                                            break;
                                        case KILL_MOBS:
                                            cond = new DungeonCondition(
                                                (String) map.get("mob-name"), asInt(map.get("amount"))
                                            );
                                            break;
                                        case INTERACT_BLOCK_WITH_ITEM:
                                            Material bMat = Material.getMaterial((String) map.getOrDefault("block-material", ""));
                                            String itemMatStr = (String) map.getOrDefault("item-material", "");
                                            Material iMat = Material.getMaterial(itemMatStr);
                                            cond = new DungeonCondition(
                                                asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")),
                                                bMat, iMat, (String) map.get("item-display-name")
                                            );
                                            if (iMat == null && !itemMatStr.isEmpty()) {
                                                cond.setCustomItemId(itemMatStr);
                                            }
                                            cond.setRequiredItems(map.containsKey("required_items") ? (Boolean) map.get("required_items") : true);
                                            break;
                                        case ZONE:
                                            cond = new DungeonCondition(
                                                asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")),
                                                asDouble(map.get("radius")), asInt(map.get("time"))
                                            );
                                            break;
                                        case THROW_AT:
                                            cond = new DungeonCondition(
                                                DungeonCondition.ConditionType.THROW_AT,
                                                asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")),
                                                asDouble(map.get("radius")), (String) map.get("item"), asInt(map.get("amount"))
                                            );
                                            break;
                                        case THROW_AT_ENEMY:
                                            cond = new DungeonCondition(
                                                DungeonCondition.ConditionType.THROW_AT_ENEMY,
                                                (String) map.get("mob-name"), (String) map.get("item"), asInt(map.get("amount")),
                                                asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z"))
                                            );
                                            break;
                                        case DROP_ON_DEATH:
                                            cond = new DungeonCondition(
                                                (String) map.get("mob-name"), (String) map.get("item"), asDouble(map.get("chance"))
                                            );
                                            break;
                                        case PERIODIC_CHECK:
                                            cond = new DungeonCondition(DungeonCondition.ConditionType.PERIODIC_CHECK);
                                            cond.setInterval(asInt(map.getOrDefault("interval", 5)));
                                            cond.setRequirement((String) map.getOrDefault("requirement", ""));
                                            cond.setOnce(map.containsKey("once") ? (Boolean) map.get("once") : false);
                                            break;
                                        case LOOKING_AT:
                                            cond = new DungeonCondition(DungeonCondition.ConditionType.LOOKING_AT);
                                            cond.setX(asDouble(map.get("x")));
                                            cond.setY(asDouble(map.get("y")));
                                            cond.setZ(asDouble(map.get("z")));
                                            cond.setInterval(asInt(map.getOrDefault("interval", 5)));
                                            break;
                                        case ALIVE:
                                            cond = new DungeonCondition(DungeonCondition.ConditionType.ALIVE);
                                            cond.setX(asDouble(map.get("x")));
                                            cond.setY(asDouble(map.get("y")));
                                            cond.setZ(asDouble(map.get("z")));
                                            cond.setRadius(asDouble(map.getOrDefault("radius", 20.0)));
                                            cond.setMobName((String) map.get("mob-name"));
                                            cond.setAmount(asInt(map.getOrDefault("amount", 1)));
                                            break;
                                    }
                                    if (cond != null) {
                                        List<DungeonEffect> onComp = new ArrayList<>();
                                        List<?> rawOnComp = (List<?>) map.get("oncomplete");
                                        if (rawOnComp != null) {
                                            for (Object rawObj : rawOnComp) {
                                                if (rawObj instanceof Map) {
                                                    DungeonEffect eff = parseSingleEffect((Map<String, Object>) rawObj);
                                                    if (eff != null) {
                                                        onComp.add(eff);
                                                    }
                                                }
                                            }
                                        }
                                        cond.setOnCompleteEffects(onComp);
                                        cond.setFailEffects(failEffs);
                                        cond.setSuccessEffects(successEffs);
                                        conditions.add(cond);
                                    }
                                }
                            }
                        }

                        List<DungeonEffect> effects = new ArrayList<>();
                        List<Map<?, ?>> effList = config.getMapList(path + ".effects");
                        for (Map<?, ?> rawMap : effList) {
                            Map<String, Object> map = (Map<String, Object>) rawMap;
                            DungeonEffect eff = parseSingleEffect(map);
                            if (eff != null) {
                                effects.add(eff);
                            }
                        }

                        List<String> next = new ArrayList<>();
                        if (config.isList(path + ".next")) {
                            next.addAll(config.getStringList(path + ".next"));
                        } else {
                            String singleNext = config.getString(path + ".next");
                            if (singleNext != null && !singleNext.isEmpty()) {
                                next.add(singleNext);
                            }
                        }

                        List<String> exclude = config.getStringList(path + ".exclude");
                        int reqClears = config.getInt(path + ".req_clears", 0);
                        String encAfterClears = config.getString(path + ".enc_after_clears", "");
                        String etitle = config.getString(path + ".title", "");
                        List<String> epool = config.getStringList(path + ".pool");

                        List<List<String>> epoolLists = new ArrayList<>();
                        List<?> rawPoolLists = config.getList(path + ".pool_lists");
                        if (rawPoolLists != null) {
                            for (Object entry : rawPoolLists) {
                                if (entry instanceof List) {
                                    List<String> inner = new ArrayList<>();
                                    for (Object item : (List<?>) entry) {
                                        if (item != null) {
                                            inner.add(item.toString().trim());
                                        }
                                    }
                                    if (!inner.isEmpty()) {
                                        epoolLists.add(inner);
                                    }
                                } else if (entry instanceof String) {
                                    List<String> inner = new ArrayList<>();
                                    for (String part : ((String) entry).split(",")) {
                                        String trimmed = part.trim();
                                        if (!trimmed.isEmpty()) {
                                            inner.add(trimmed);
                                        }
                                    }
                                    if (!inner.isEmpty()) {
                                        epoolLists.add(inner);
                                    }
                                }
                            }
                        }

                        List<DungeonPlatform> platforms = new ArrayList<>();
                        List<Map<?, ?>> platList = config.getMapList(path + ".platforms");
                        if (platList != null) {
                            for (Map<?, ?> rawMap : platList) {
                                Map<String, Object> map = (Map<String, Object>) rawMap;
                                double px1 = asDouble(map.get("x1"));
                                double py1 = asDouble(map.get("y1"));
                                double pz1 = asDouble(map.get("z1"));
                                double px2 = asDouble(map.get("x2"));
                                double py2 = asDouble(map.get("y2"));
                                double pz2 = asDouble(map.get("z2"));
                                Material mat = Material.getMaterial((String) map.getOrDefault("material", "STONE"));
                                boolean inverted = map.containsKey("inverted") ? (Boolean) map.get("inverted") : false;
                                String testMode = (String) map.getOrDefault("test-mode", "GLOBAL");
                                String requirement = (String) map.getOrDefault("requirement", "");
                                platforms.add(new DungeonPlatform(px1, py1, pz1, px2, py2, pz2, mat, inverted, testMode, requirement));
                            }
                        }

                        encounters.put(encId, new Encounter(encId, desc, conditions, effects, next, exclude, reqClears, encAfterClears, etitle, epool, epoolLists, platforms));
                    }
                }

                List<DungeonPlatform> globalPlatforms = new ArrayList<>();
                List<Map<?, ?>> globalPlatList = config.getMapList("platforms");
                if (globalPlatList != null) {
                    for (Map<?, ?> rawMap : globalPlatList) {
                        Map<String, Object> map = (Map<String, Object>) rawMap;
                        double px1 = asDouble(map.get("x1"));
                        double py1 = asDouble(map.get("y1"));
                        double pz1 = asDouble(map.get("z1"));
                        double px2 = asDouble(map.get("x2"));
                        double py2 = asDouble(map.get("y2"));
                        double pz2 = asDouble(map.get("z2"));
                        Material mat = Material.getMaterial((String) map.getOrDefault("material", "STONE"));
                        boolean inverted = map.containsKey("inverted") ? (Boolean) map.get("inverted") : false;
                        String testMode = (String) map.getOrDefault("test-mode", "GLOBAL");
                        String requirement = (String) map.getOrDefault("requirement", "");
                        globalPlatforms.add(new DungeonPlatform(px1, py1, pz1, px2, py2, pz2, mat, inverted, testMode, requirement));
                    }
                }

                List<String> allowedStats = config.getStringList("loot-chest.allowed-stats");
                List<String> allowedBlessings = config.getStringList("loot-chest.allowed-blessings");

                Dungeon dungeon = new Dungeon(id, name, schematic, pasteLoc, spawnLoc, exitWorld, exitLoc, initialEncounter, encounters, rewards, allowedStats, allowedBlessings, customItems, globalPlatforms);
                templates.put(id.toLowerCase(), dungeon);
                System.out.println("[Dungeons] Pomyslnie wczytano szablon lochu: " + id);
            } catch (Exception e) {
                System.err.println("[Dungeons] Blad podczas parsowania pliku " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private DungeonEffect parseSingleEffect(Map<String, Object> map) {
        String typeStr = (String) map.get("type");
        if (typeStr == null) return null;
        
        String normType = typeStr.toUpperCase().replace(" ", "_").replace("!", "");
        DungeonEffect.EffectType eType = null;
        try {
            eType = DungeonEffect.EffectType.valueOf(normType);
        } catch (IllegalArgumentException ex) {
        }
        if (eType == null) return null;

        switch (eType) {
            case SEND_MESSAGE:
                return new DungeonEffect((String) map.get("message"));
            case TELEPORT_PLAYERS:
                return new DungeonEffect(
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z"))
                );
            case SPAWN_MOB:
                return new DungeonEffect(
                    (String) map.get("mob-name"), asInt(map.get("amount")), asInt(map.get("level")),
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), asDouble(map.get("range"))
                );
            case OPEN_DOOR:
            case CLOSE_DOOR:
                Material doorMat = Material.getMaterial((String) map.getOrDefault("material", "STONE"));
                return new DungeonEffect(
                    eType, asDouble(map.get("x1")), asDouble(map.get("y1")), asDouble(map.get("z1")),
                    asDouble(map.get("x2")), asDouble(map.get("y2")), asDouble(map.get("z2")), doorMat
                );
            case GIVE_READY_COMPASS:
                return new DungeonEffect(DungeonEffect.EffectType.GIVE_READY_COMPASS);
            case SPAWN_CHEST:
                return new DungeonEffect(
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), (String) map.getOrDefault("chest-type", "ROGUELITE_CHEST")
                );
            case COMPLETE_DUNGEON:
                return new DungeonEffect(DungeonEffect.EffectType.COMPLETE_DUNGEON);
            case SPAWN_UNTIL:
                return new DungeonEffect(
                    DungeonEffect.EffectType.SPAWN_UNTIL,
                    (String) map.get("mob-name"), asInt(map.get("amount")), asInt(map.get("level")),
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), asDouble(map.get("range")),
                    asInt(map.get("interval"))
                );
            case KNOCKBACK:
                return new DungeonEffect(
                    DungeonEffect.EffectType.KNOCKBACK, "", 0, 0,
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), 0.0, 0
                );
            case PULL:
                return new DungeonEffect(
                    DungeonEffect.EffectType.PULL, "", asInt(map.get("amount")), 0,
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), 0.0, 0
                );
            case DAMAGE:
                return new DungeonEffect(
                    DungeonEffect.EffectType.DAMAGE, "", asInt(map.get("amount")), 0,
                    0.0, 0.0, 0.0, 0.0, 0
                );
            case FORCE_FAIL:
                return new DungeonEffect(DungeonEffect.EffectType.FORCE_FAIL);
        }
        return null;
    }

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

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<DungeonInstance> active = new ArrayList<>(activeInstances.values());
                for (DungeonInstance run : active) {
                    try {
                        run.update();
                        
                        if (run.isFinished()) {
                            activeInstances.remove(run.getWorld());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 20L);
    }

    public void cleanupAll() {
        List<DungeonInstance> active = new ArrayList<>(activeInstances.values());
        for (DungeonInstance run : active) {
            run.cleanup();
        }
        activeInstances.clear();
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        World world = victim.getWorld();
        
        DungeonInstance run = activeInstances.get(world);
        if (run == null) return;

        String name = victim.getName();
        String cleanName = ChatColor.stripColor(name);
        run.onMobKill(victim.getType().name());
        run.onMobKill(cleanName);

        Encounter encounter = run.getActiveEncounter();
        if (encounter != null) {
            for (DungeonCondition condition : encounter.getConditions()) {
                if (condition.getType() == DungeonCondition.ConditionType.DROP_ON_DEATH) {
                    if (cleanName.equalsIgnoreCase(condition.getMobName()) || victim.getType().name().equalsIgnoreCase(condition.getMobName())) {
                        double chance = condition.getChance();
                        if (new Random().nextDouble() * 100 <= chance) {
                            DungeonCustomItem customItem = run.getTemplate().getCustomItems().get(condition.getCustomItemId());
                            if (customItem != null) {
                                world.dropItemNaturally(victim.getLocation(), customItem.toItemStack());
                            }
                        }
                    }
                }
            }
        }

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

        if (run.isPlayerSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        if (player.getHealth() - event.getFinalDamage() <= 0.0) {
            event.setCancelled(true);
            player.setHealth(20.0);
            run.setPlayerSpectator(player, true);
            return;
        }

        DungeonPlayerStats stats = run.getPlayerStats(player);
        DungeonBlessingManager.handleDodge(player, event, stats);
    }

    @EventHandler
    public void onPlayerDealsTakesDamage(EntityDamageByEntityEvent event) {
        World world = event.getEntity().getWorld();
        DungeonInstance run = activeInstances.get(world);
        if (run == null) return;

        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity victim = (LivingEntity) event.getEntity();
            if (run.isShieldedEnemy(victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            DungeonPlayerStats stats = run.getPlayerStats(attacker);
            if (stats != null) {
                double newDmg = stats.calculateOutgoingDamage(event.getDamage());
                event.setDamage(newDmg);
                DungeonBlessingManager.handleAdrenaline(attacker, event, stats);
            }
        }

        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            DungeonPlayerStats stats = run.getPlayerStats(victim);
            if (stats != null) {
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

        if (run.isPlayerSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItem();

        if (item != null && event.getAction().name().startsWith("RIGHT_CLICK")) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            org.bukkit.persistence.PersistentDataContainer pdc = meta != null ? meta.getPersistentDataContainer() : null;
            if (pdc != null) {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_item_id");
                if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                    String itemId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                    DungeonCustomItem customItem = run.getTemplate().getCustomItems().get(itemId);
                    if (customItem != null && "throwable".equalsIgnoreCase(customItem.getType())) {
                        event.setCancelled(true);
                        int newAmt = item.getAmount() - 1;
                        if (newAmt > 0) {
                            item.setAmount(newAmt);
                        } else {
                            player.getInventory().setItemInMainHand(null);
                        }
                        throwProjectile(player, customItem, run);
                        return;
                    }
                }
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            
            if (run.isRegisteredLootChest(block.getLocation())) {
                event.setCancelled(true);
                if (run.hasClaimedChest(player, block.getLocation())) {
                    player.sendMessage(ChatColor.RED + "[Dungeons] Juz odebrales swoja nagrode z tej skrzyni!");
                    return;
                }
                
                DungeonLootChest gui = run.getChestGuiForPlayer(block.getLocation(), player);
                player.openInventory(gui.getInventory());
                return;
            }

            Encounter encounter = run.getActiveEncounter();
            if (encounter != null) {
                for (DungeonCondition condition : encounter.getConditions()) {
                    if (condition.getType() == DungeonCondition.ConditionType.INTERACT_BLOCK_WITH_ITEM) {
                        if (condition.isMetInteract(block.getLocation(), block.getType(), item)) {
                            if (item != null && item.getAmount() > 0) {
                                int newAmt = item.getAmount() - 1;
                                if (newAmt > 0) {
                                    item.setAmount(newAmt);
                                } else {
                                    player.getInventory().setItemInMainHand(null);
                                }
                            }
                            
                            run.transitionToNext();
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }

        if (item != null && event.getAction().name().startsWith("RIGHT_CLICK")) {
            if (item.getType() == Material.COMPASS && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Gotowy?")) {
                event.setCancelled(true);
                run.setPlayerReady(player, !run.isPlayerReady(player));
            } else if (item.getType() == Material.CHEST && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Menu Umiejętności")) {
                event.setCancelled(true);
                AmonPackPlugin.levelsBending.OpenDungeonSkillMenu(player.getName());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;

        if (event.getInventory().getHolder() instanceof DungeonLootChest) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            DungeonLootChest gui = (DungeonLootChest) event.getInventory().getHolder();
            DungeonLootChest.RewardOption option = gui.getOptions().get(slot);
            
            if (option != null) {
                DungeonPlayerStats stats = run.getPlayerStats(player);
                PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
                
                switch (option.type) {
                    case SKILL:
                        stats.addBoundDungeonSkill(option.value);
                        if (branch != null) {
                            branch.getTemporaryAbilities().add(option.value);
                            
                            com.projectkorra.projectkorra.Element skillElement = null;
                            org.bukkit.configuration.file.FileConfiguration skillTreeConfig = AmonPackPlugin.getSkillTreeConfig();
                            if (skillTreeConfig != null && skillTreeConfig.getConfigurationSection("AmonPack.Tree") != null) {
                                for (String elName : skillTreeConfig.getConfigurationSection("AmonPack.Tree").getKeys(false)) {
                                    com.projectkorra.projectkorra.Element pkEl = com.projectkorra.projectkorra.Element.getElement(elName);
                                    if (pkEl != null) {
                                        RPG.Levels.BendingTree.ElementTree tree = AmonPackPlugin.levelsBending.GetElement(pkEl);
                                        if (tree != null) {
                                            for (RPG.Levels.BendingTree.SkillTree_Ability ability : tree.getAbilities()) {
                                                if (ability.getName().equalsIgnoreCase(option.value)) {
                                                    skillElement = pkEl;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (skillElement != null) break;
                                }
                            }
                            
                            if (skillElement != null) {
                                com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
                                if (bPlayer != null) {
                                    if (!bPlayer.hasElement(skillElement)) {
                                        bPlayer.getElements().add(skillElement);
                                        branch.getTemporaryElements().add(skillElement);
                                        player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.LIGHT_PURPLE + "Przyznano Ci tymczasowy żywioł/subżywioł: " + skillElement.getName() + " na czas tego dungeonu!");
                                    }
                                    com.projectkorra.projectkorra.Element parentElement = AmonPackPlugin.ElementBasedOnSubElement(skillElement);
                                    if (parentElement != null && !bPlayer.hasElement(parentElement)) {
                                        bPlayer.getElements().add(parentElement);
                                        branch.getTemporaryElements().add(parentElement);
                                        player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.LIGHT_PURPLE + "Przyznano Ci tymczasowy żywioł główny: " + parentElement.getName() + " na czas tego dungeonu!");
                                    }
                                }
                            }
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
                run.markChestClaimed(player, gui.getChestLocation());
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

    public boolean isWorldInUse(String worldName) {
        for (World world : activeInstances.keySet()) {
            if (world.getName().equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        World world = Bukkit.getWorld(worldName);
        if (world != null && !world.getPlayers().isEmpty()) {
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;

        if (run.isPlayerSpectator(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                event.setTo(from);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;

        if (run.isPlayerSpectator(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;

        if (run.isPlayerSpectator(player)) {
            event.setCancelled(true);
        }
    }

    public void throwProjectile(Player player, DungeonCustomItem customItem, DungeonInstance run) {
        World world = player.getWorld();
        Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector velocity = player.getEyeLocation().getDirection().normalize().multiply(1.2);

        org.bukkit.entity.Item thrownItem = world.dropItem(startLoc, customItem.toItemStack());
        thrownItem.setPickupDelay(32767);
        thrownItem.setGravity(false);
        thrownItem.setVelocity(new Vector(0, 0, 0));

        world.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (ticks > 100 || thrownItem.isDead() || run.isFinished()) {
                    thrownItem.remove();
                    cancel();
                    return;
                }

                currentLoc.add(velocity);
                velocity.setY(velocity.getY() - 0.04);
                
                thrownItem.teleport(currentLoc);

                world.spawnParticle(Particle.CRIT, currentLoc, 3, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.DUST, currentLoc, 2, 0.05, 0.05, 0.05, 0.01, new Particle.DustOptions(Color.ORANGE, 0.8f));

                if (currentLoc.getBlock().getType().isSolid()) {
                    run.registerProjectileHitCoord(currentLoc);
                    world.playSound(currentLoc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
                    world.spawnParticle(Particle.BLOCK, currentLoc, 15, 0.2, 0.2, 0.2, Material.STONE.createBlockData());
                    thrownItem.remove();
                    cancel();
                    return;
                }

                for (Entity entity : thrownItem.getNearbyEntities(0.6, 0.6, 0.6)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity living = (LivingEntity) entity;
                        if (run.isShieldedEnemy(living.getUniqueId())) {
                            run.registerShieldedEnemyHit(living, customItem);
                        } else {
                            living.damage(4.0, player);
                        }
                        world.playSound(currentLoc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        thrownItem.remove();
                        cancel();
                        return;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }
}
