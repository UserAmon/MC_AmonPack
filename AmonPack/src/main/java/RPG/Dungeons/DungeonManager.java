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
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DungeonManager implements Listener {

    private static DungeonManager instance;
    private final Map<String, Dungeon> templates = new HashMap<>();
    private final Map<World, DungeonInstance> activeInstances = new HashMap<>();
    private final Map<UUID, Long> dungeonConsumableChargingPlayers = new HashMap<>();
    private final Map<UUID, Integer> dungeonConsumableChargingTasks = new HashMap<>();

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
                            DungeonCustomItem customItem = new DungeonCustomItem(key, mat, nameStr, lore, itype, modelId);
                            if (itype.equalsIgnoreCase("consumable") || itype.equalsIgnoreCase("consumables")) {
                                String mode = config.getString(ipath + ".use-mode", "PPM");
                                if (config.contains(ipath + ".tryb-uzycia")) {
                                    mode = config.getString(ipath + ".tryb-uzycia");
                                }
                                String effType = config.getString(ipath + ".effect", "");
                                if (config.contains(ipath + ".efekt")) {
                                    effType = config.getString(ipath + ".efekt");
                                }
                                int dur = config.getInt(ipath + ".duration", 0);
                                if (config.contains(ipath + ".czas-trwania")) {
                                    dur = config.getInt(ipath + ".czas-trwania");
                                }
                                double val = config.getDouble(ipath + ".value", 0.0);
                                if (config.contains(ipath + ".wartosc")) {
                                    val = config.getDouble(ipath + ".wartosc");
                                }
                                customItem.setUseMode(mode);
                                customItem.setEffectType(effType);
                                customItem.setDuration(dur);
                                customItem.setEffectValue(val);
                            }
                            boolean isRet = config.getBoolean(ipath + ".is-returning", config.getBoolean(ipath + ".is_returning", config.getBoolean(ipath + ".IsReturning", false)));
                            customItem.setReturning(isRet);
                            customItems.put(key, customItem);
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
                                            if (map.containsKey("x") && map.containsKey("y") && map.containsKey("z")) {
                                                cond.setX(asDouble(map.get("x")));
                                                cond.setY(asDouble(map.get("y")));
                                                cond.setZ(asDouble(map.get("z")));
                                                if (map.containsKey("yaw")) cond.setYaw((float) asDouble(map.get("yaw")));
                                                if (map.containsKey("pitch")) cond.setPitch((float) asDouble(map.get("pitch")));
                                                cond.setHasCoords(true);
                                            }
                                            if (map.containsKey("freeze")) {
                                                cond.setFreezePlayers((Boolean) map.get("freeze"));
                                            }
                                            break;
                                        case DYNAMIC_PATH:
                                            cond = new DungeonCondition(DungeonCondition.ConditionType.DYNAMIC_PATH);
                                            if (map.containsKey("min")) {
                                                Map<String, Object> minMap = (Map<String, Object>) map.get("min");
                                                cond.setMinX(asDouble(minMap.get("x")));
                                                cond.setMinY(asDouble(minMap.get("y")));
                                                cond.setMinZ(asDouble(minMap.get("z")));
                                            } else {
                                                cond.setMinX(asDouble(map.get("min-x")));
                                                cond.setMinY(asDouble(map.get("min-y")));
                                                cond.setMinZ(asDouble(map.get("min-z")));
                                            }
                                            if (map.containsKey("max")) {
                                                Map<String, Object> maxMap = (Map<String, Object>) map.get("max");
                                                cond.setMaxX(asDouble(maxMap.get("x")));
                                                cond.setMaxY(asDouble(maxMap.get("y")));
                                                cond.setMaxZ(asDouble(maxMap.get("z")));
                                            } else {
                                                cond.setMaxX(asDouble(map.get("max-x")));
                                                cond.setMaxY(asDouble(map.get("max-y")));
                                                cond.setMaxZ(asDouble(map.get("max-z")));
                                            }
                                            if (map.containsKey("start")) {
                                                Map<String, Object> sMap = (Map<String, Object>) map.get("start");
                                                cond.setStartX(asDouble(sMap.get("x")));
                                                cond.setStartY(asDouble(sMap.get("y")));
                                                cond.setStartZ(asDouble(sMap.get("z")));
                                                cond.setHasStartLoc(true);
                                            } else if (map.containsKey("start-x")) {
                                                cond.setStartX(asDouble(map.get("start-x")));
                                                cond.setStartY(asDouble(map.get("start-y")));
                                                cond.setStartZ(asDouble(map.get("start-z")));
                                                cond.setHasStartLoc(true);
                                            }
                                            if (map.containsKey("end")) {
                                                Map<String, Object> eMap = (Map<String, Object>) map.get("end");
                                                cond.setEndX(asDouble(eMap.get("x")));
                                                cond.setEndY(asDouble(eMap.get("y")));
                                                cond.setEndZ(asDouble(eMap.get("z")));
                                                cond.setHasEndLoc(true);
                                            } else if (map.containsKey("end-x")) {
                                                cond.setEndX(asDouble(map.get("end-x")));
                                                cond.setEndY(asDouble(map.get("end-y")));
                                                cond.setEndZ(asDouble(map.get("end-z")));
                                                cond.setHasEndLoc(true);
                                            }
                                            if (map.containsKey("reveal-x")) cond.setRevealX(asDouble(map.get("reveal-x")));
                                            if (map.containsKey("reveal-y")) cond.setRevealY(asDouble(map.get("reveal-y")));
                                            if (map.containsKey("reveal-z")) cond.setRevealZ(asDouble(map.get("reveal-z")));
                                            if (map.containsKey("reveal-radius")) cond.setRevealRadius(asDouble(map.get("reveal-radius")));
                                            if (map.containsKey("reshuffle-interval-seconds")) cond.setReshuffleIntervalSeconds(asInt(map.get("reshuffle-interval-seconds")));
                                            if (map.containsKey("safe-block-material")) {
                                                Material m = DungeonCondition.parseMaterial((String) map.get("safe-block-material"));
                                                if (m != null) cond.setSafeBlockMaterial(m);
                                            }
                                            if (map.containsKey("crumble-block-material")) {
                                                Material m = DungeonCondition.parseMaterial((String) map.get("crumble-block-material"));
                                                if (m != null) cond.setCrumbleBlockMaterial(m);
                                            }
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
                                            if (map.containsKey("mob-display-name")) {
                                                cond.setMobDisplayName((String) map.get("mob-display-name"));
                                            }
                                            break;
                                        case INTERACT_BLOCK_WITH_ITEM:
                                            String bMatStr = (String) map.getOrDefault("block-material", "");
                                            Material bMat = DungeonCondition.parseMaterial(bMatStr);
                                            String itemMatStr = (String) map.getOrDefault("item-material", "");
                                            Material iMat = DungeonCondition.parseMaterial(itemMatStr);
                                            cond = new DungeonCondition(
                                                asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")),
                                                bMat, iMat, (String) map.get("item-display-name")
                                            );
                                            if (iMat == null && !itemMatStr.isEmpty()) {
                                                cond.setCustomItemId(itemMatStr);
                                            }
                                            if (map.containsKey("radius")) {
                                                cond.setRadius(asDouble(map.get("radius")));
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
                                            boolean isRet = map.containsKey("is-returning") ? Boolean.parseBoolean(String.valueOf(map.get("is-returning")))
                                                          : (map.containsKey("is_returning") ? Boolean.parseBoolean(String.valueOf(map.get("is_returning")))
                                                          : (map.containsKey("IsReturning") ? Boolean.parseBoolean(String.valueOf(map.get("IsReturning"))) : false));
                                            cond.setReturning(isRet);
                                            break;
                                        case SHIELDED:
                                            cond = new DungeonCondition(
                                                DungeonCondition.ConditionType.SHIELDED,
                                                (String) map.get("mob-name"), (String) map.get("item"), asInt(map.get("amount")),
                                                asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z"))
                                            );
                                            cond.setShieldType((String) map.getOrDefault("shield-type", "throw"));
                                            if (map.containsKey("mob-display-name")) {
                                                cond.setMobDisplayName((String) map.get("mob-display-name"));
                                            }
                                            break;
                                        case DROP_ON_DEATH:
                                            cond = new DungeonCondition(
                                                (String) map.get("mob-name"), (String) map.get("item"), asDouble(map.get("chance"))
                                            );
                                            if (map.containsKey("mob-display-name")) {
                                                cond.setMobDisplayName((String) map.get("mob-display-name"));
                                            }
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
                                            cond.setRadius(asDouble(map.getOrDefault("radius", 0.0)));
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
                                            if (map.containsKey("mob-display-name")) {
                                                cond.setMobDisplayName((String) map.get("mob-display-name"));
                                            }
                                            break;
                                        case COLLECT_POINTS:
                                            cond = new DungeonCondition(DungeonCondition.ConditionType.COLLECT_POINTS);
                                            cond.setRadius(asDouble(map.getOrDefault("radius", 3.0)));
                                            cond.setTimeRequired(asInt(map.getOrDefault("time", 60)));
                                            List<Location> pts = new ArrayList<>();
                                            List<?> rawPoints = (List<?>) map.get("points");
                                            if (rawPoints != null) {
                                                for (Object pObj : rawPoints) {
                                                    if (pObj instanceof String) {
                                                        String[] parts = ((String) pObj).split(",");
                                                        if (parts.length >= 3) {
                                                            double px = Double.parseDouble(parts[0].trim());
                                                            double py = Double.parseDouble(parts[1].trim());
                                                            double pz = Double.parseDouble(parts[2].trim());
                                                            pts.add(new Location(null, Math.floor(px) + 0.5, Math.floor(py), Math.floor(pz) + 0.5));
                                                        }
                                                    } else if (pObj instanceof Map) {
                                                        Map<String, Object> pMap = (Map<String, Object>) pObj;
                                                        double px = asDouble(pMap.get("x"));
                                                        double py = asDouble(pMap.get("y"));
                                                        double pz = asDouble(pMap.get("z"));
                                                        pts.add(new Location(null, Math.floor(px) + 0.5, Math.floor(py), Math.floor(pz) + 0.5));
                                                    }
                                                }
                                            }
                                            cond.setPoints(pts);
                                            break;
                                         case REBUILD:
                                             cond = new DungeonCondition(DungeonCondition.ConditionType.REBUILD);
                                             if (map.containsKey("start-x")) {
                                                 cond.setStartX(asDouble(map.get("start-x")));
                                                 cond.setStartY(asDouble(map.get("start-y")));
                                                 cond.setStartZ(asDouble(map.get("start-z")));
                                                 cond.setHasStartLoc(true);
                                             } else if (map.containsKey("x1")) {
                                                 cond.setStartX(asDouble(map.get("x1")));
                                                 cond.setStartY(asDouble(map.get("y1")));
                                                 cond.setStartZ(asDouble(map.get("z1")));
                                                 cond.setHasStartLoc(true);
                                             }
                                             if (map.containsKey("end-x")) {
                                                 cond.setEndX(asDouble(map.get("end-x")));
                                                 cond.setEndY(asDouble(map.get("end-y")));
                                                 cond.setEndZ(asDouble(map.get("end-z")));
                                                 cond.setHasEndLoc(true);
                                             } else if (map.containsKey("x2")) {
                                                 cond.setEndX(asDouble(map.get("x2")));
                                                 cond.setEndY(asDouble(map.get("y2")));
                                                 cond.setEndZ(asDouble(map.get("z2")));
                                                 cond.setHasEndLoc(true);
                                             }
                                             double rMinX = map.containsKey("min-x") ? asDouble(map.get("min-x")) : (cond.hasStartLoc() ? Math.min(cond.getStartX(), cond.getEndX()) : 0);
                                             double rMaxX = map.containsKey("max-x") ? asDouble(map.get("max-x")) : (cond.hasStartLoc() ? Math.max(cond.getStartX(), cond.getEndX()) : 0);
                                             double rMinY = map.containsKey("min-y") ? asDouble(map.get("min-y")) : (cond.hasStartLoc() ? Math.min(cond.getStartY(), cond.getEndY()) : 0);
                                             double rMaxY = map.containsKey("max-y") ? asDouble(map.get("max-y")) : (cond.hasStartLoc() ? Math.max(cond.getStartY(), cond.getEndY()) : 0);
                                             double rMinZ = map.containsKey("min-z") ? asDouble(map.get("min-z")) : (cond.hasStartLoc() ? Math.min(cond.getStartZ(), cond.getEndZ()) : 0);
                                             double rMaxZ = map.containsKey("max-z") ? asDouble(map.get("max-z")) : (cond.hasStartLoc() ? Math.max(cond.getStartZ(), cond.getEndZ()) : 0);
                                             cond.setMinX(rMinX);
                                             cond.setMaxX(rMaxX);
                                             cond.setMinY(rMinY);
                                             cond.setMaxY(rMaxY);
                                             cond.setMinZ(rMinZ);
                                             cond.setMaxZ(rMaxZ);
                                             if (map.containsKey("material")) {
                                                 cond.setRebuildMaterial(DungeonCondition.parseMaterial((String) map.get("material")));
                                             }
                                             if (map.containsKey("clear-field")) {
                                                 cond.setClearField((Boolean) map.get("clear-field"));
                                             }
                                             if (map.containsKey("direction")) {
                                                 cond.setRebuildDirection((String) map.get("direction"));
                                             }
                                             break;

                                         case HIT_TARGETS:
                                             cond = new DungeonCondition(DungeonCondition.ConditionType.HIT_TARGETS);
                                             cond.setTargetHp(asDouble(map.getOrDefault("hp", 20.0)));
                                             cond.setTargetSpeed(asDouble(map.getOrDefault("speed", 0.12)));
                                             double defOff = asDouble(map.getOrDefault("offset", 3.0));
                                             cond.setTargetOffsetX(asDouble(map.getOrDefault("offset-x", defOff)));
                                             cond.setTargetOffsetY(asDouble(map.getOrDefault("offset-y", Math.min(2.0, defOff))));
                                             cond.setTargetOffsetZ(asDouble(map.getOrDefault("offset-z", defOff)));
                                             cond.setTargetType((String) map.getOrDefault("target-type", "ORB"));
                                             cond.setTargetParticle((String) map.getOrDefault("particle", "FLAME"));

                                             List<Location> tPts = new ArrayList<>();
                                             List<Double> tHpList = new ArrayList<>();
                                             List<Double> tSpdList = new ArrayList<>();

                                             List<?> rawTargets = map.containsKey("targets") ? (List<?>) map.get("targets") : (List<?>) map.get("points");
                                             if (rawTargets != null) {
                                                 for (Object tObj : rawTargets) {
                                                     if (tObj instanceof String) {
                                                         String[] parts = ((String) tObj).split(",");
                                                         if (parts.length >= 3) {
                                                             double px = Double.parseDouble(parts[0].trim());
                                                             double py = Double.parseDouble(parts[1].trim());
                                                             double pz = Double.parseDouble(parts[2].trim());
                                                             tPts.add(new Location(null, px, py, pz));
                                                             tHpList.add(cond.getTargetHp());
                                                             tSpdList.add(cond.getTargetSpeed());
                                                         }
                                                     } else if (tObj instanceof Map) {
                                                         Map<String, Object> tMap = (Map<String, Object>) tObj;
                                                         double px = asDouble(tMap.get("x"));
                                                         double py = asDouble(tMap.get("y"));
                                                         double pz = asDouble(tMap.get("z"));
                                                         tPts.add(new Location(null, px, py, pz));
                                                         tHpList.add(asDouble(tMap.getOrDefault("hp", cond.getTargetHp())));
                                                         tSpdList.add(asDouble(tMap.getOrDefault("speed", cond.getTargetSpeed())));
                                                     }
                                                 }
                                             }
                                             cond.setPoints(tPts);
                                             cond.setTargetCustomHpList(tHpList);
                                             cond.setTargetCustomSpeedList(tSpdList);
                                             break;
                                    }
                                    if (cond != null) {
                                        if (map.containsKey("x")) cond.setXList(asDoubleList(map.get("x")));
                                        if (map.containsKey("y")) cond.setYList(asDoubleList(map.get("y")));
                                        if (map.containsKey("z")) cond.setZList(asDoubleList(map.get("z")));
                                        List<DungeonEffect> onComp = new ArrayList<>();
                                        List<Map<?, ?>> onCompList = (List<Map<?, ?>>) map.get("oncomplete");
                                        if (onCompList != null) {
                                            for (Map<?, ?> rawEff : onCompList) {
                                                DungeonEffect eff = parseSingleEffect((Map<String, Object>) rawEff);
                                                if (eff != null) onComp.add(eff);
                                            }
                                        }
                                        cond.setOnCompleteEffects(onComp);

                                        List<DungeonEffect> succEff = new ArrayList<>();
                                        List<Map<?, ?>> succList = (List<Map<?, ?>>) map.get("success");
                                        if (succList != null) {
                                            for (Map<?, ?> rawEff : succList) {
                                                DungeonEffect eff = parseSingleEffect((Map<String, Object>) rawEff);
                                                if (eff != null) succEff.add(eff);
                                            }
                                        }
                                        cond.setSuccessEffects(succEff);

                                        List<DungeonEffect> failEff = new ArrayList<>();
                                        List<Map<?, ?>> failList = (List<Map<?, ?>>) map.get("fail");
                                        if (failList != null) {
                                            for (Map<?, ?> rawEff : failList) {
                                                DungeonEffect eff = parseSingleEffect((Map<String, Object>) rawEff);
                                                if (eff != null) failEff.add(eff);
                                            }
                                        }
                                        cond.setFailEffects(failEff);

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
                        List<String> epool = new ArrayList<>();
                        if (config.contains(path + ".pool")) {
                            epool.addAll(config.getStringList(path + ".pool"));
                        }
                        List<List<String>> epoolLists = new ArrayList<>();
                        if (config.contains(path + ".pool_lists")) {
                            List<?> rawList = config.getList(path + ".pool_lists");
                            if (rawList != null) {
                                for (Object item : rawList) {
                                    if (item instanceof List) {
                                        List<String> sub = new ArrayList<>();
                                        for (Object subItem : (List<?>) item) {
                                            if (subItem != null) {
                                                sub.add(subItem.toString().trim());
                                            }
                                        }
                                        if (!sub.isEmpty()) epoolLists.add(sub);
                                    } else if (item instanceof String) {
                                        List<String> sub = new ArrayList<>();
                                        for (String part : ((String) item).split(",")) {
                                            String trimmed = part.trim();
                                            if (!trimmed.isEmpty()) {
                                                sub.add(trimmed);
                                            }
                                        }
                                        if (!sub.isEmpty()) epoolLists.add(sub);
                                    }
                                }
                            }
                        }
                        int maxMobs = config.getInt(path + ".max_mobs", 0);
                        boolean leaveMobs = config.getBoolean(path + ".leave_mobs", false) || config.getBoolean(path + ".leaveMobs", false);

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
                                Integer checkInterval = map.containsKey("check-interval") ? ((Number) map.get("check-interval")).intValue() : null;
                                Integer delay = map.containsKey("delay") ? ((Number) map.get("delay")).intValue() : null;
                                platforms.add(new DungeonPlatform(px1, py1, pz1, px2, py2, pz2, mat, inverted, testMode, requirement, checkInterval, delay));
                            }
                        }

                        Double guideX = null;
                        Double guideY = null;
                        Double guideZ = null;
                        double guideRadius = 5.0;
                        boolean hasGuide = false;

                        if (config.contains(path + ".guide") && config.isConfigurationSection(path + ".guide")) {
                            guideX = asDouble(config.get(path + ".guide.x"));
                            guideY = asDouble(config.get(path + ".guide.y"));
                            guideZ = asDouble(config.get(path + ".guide.z"));
                            guideRadius = config.getDouble(path + ".guide.radius", 5.0);
                            hasGuide = true;
                        } else if (config.contains(path + ".target") && config.isConfigurationSection(path + ".target")) {
                            guideX = asDouble(config.get(path + ".target.x"));
                            guideY = asDouble(config.get(path + ".target.y"));
                            guideZ = asDouble(config.get(path + ".target.z"));
                            guideRadius = config.getDouble(path + ".target.radius", 5.0);
                            hasGuide = true;
                        } else if (config.contains(path + ".objective") && config.isConfigurationSection(path + ".objective")) {
                            guideX = asDouble(config.get(path + ".objective.x"));
                            guideY = asDouble(config.get(path + ".objective.y"));
                            guideZ = asDouble(config.get(path + ".objective.z"));
                            guideRadius = config.getDouble(path + ".objective.radius", 5.0);
                            hasGuide = true;
                        } else if (config.contains(path + ".guide-x")) {
                            guideX = asDouble(config.get(path + ".guide-x"));
                            guideY = asDouble(config.get(path + ".guide-y"));
                            guideZ = asDouble(config.get(path + ".guide-z"));
                            guideRadius = config.getDouble(path + ".guide-radius", 5.0);
                            hasGuide = true;
                        } else if (config.contains(path + ".target-x")) {
                            guideX = asDouble(config.get(path + ".target-x"));
                            guideY = asDouble(config.get(path + ".target-y"));
                            guideZ = asDouble(config.get(path + ".target-z"));
                            guideRadius = config.getDouble(path + ".target-radius", 5.0);
                            hasGuide = true;
                        } else if (config.contains(path + ".objective-x")) {
                            guideX = asDouble(config.get(path + ".objective-x"));
                            guideY = asDouble(config.get(path + ".objective-y"));
                            guideZ = asDouble(config.get(path + ".objective-z"));
                            guideRadius = config.getDouble(path + ".objective-radius", 5.0);
                            hasGuide = true;
                        } else if (config.contains(path + ".x") && config.contains(path + ".y") && config.contains(path + ".z")) {
                            guideX = asDouble(config.get(path + ".x"));
                            guideY = asDouble(config.get(path + ".y"));
                            guideZ = asDouble(config.get(path + ".z"));
                            guideRadius = config.getDouble(path + ".radius", 5.0);
                            hasGuide = true;
                        }

                        encounters.put(encId, new Encounter(encId, desc, conditions, effects, next, exclude, reqClears, encAfterClears, etitle, epool, epoolLists, platforms, maxMobs, leaveMobs, guideX, guideY, guideZ, guideRadius, hasGuide));
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
                        Integer checkInterval = map.containsKey("check-interval") ? ((Number) map.get("check-interval")).intValue() : null;
                        Integer delay = map.containsKey("delay") ? ((Number) map.get("delay")).intValue() : null;
                        globalPlatforms.add(new DungeonPlatform(px1, py1, pz1, px2, py2, pz2, mat, inverted, testMode, requirement, checkInterval, delay));
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
        DungeonWorldManager.initPools();
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

        DungeonEffect eff = null;
        switch (eType) {
            case SEND_MESSAGE:
                eff = new DungeonEffect((String) map.get("message"));
                break;
            case TELEPORT_PLAYERS:
                eff = new DungeonEffect(
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z"))
                );
                break;
            case SPAWN_MOB:
                eff = new DungeonEffect(
                    (String) map.get("mob-name"), asInt(map.get("amount")), asInt(map.get("level")),
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), asDouble(map.get("range"))
                );
                break;
            case OPEN_DOOR:
            case CLOSE_DOOR:
                Material doorMat = Material.getMaterial((String) map.getOrDefault("material", "STONE"));
                eff = new DungeonEffect(
                    eType, asDouble(map.get("x1")), asDouble(map.get("y1")), asDouble(map.get("z1")),
                    asDouble(map.get("x2")), asDouble(map.get("y2")), asDouble(map.get("z2")), doorMat
                );
                break;
            case GIVE_READY_COMPASS:
                eff = new DungeonEffect(DungeonEffect.EffectType.GIVE_READY_COMPASS);
                break;
            case SPAWN_CHEST:
                eff = new DungeonEffect(
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), (String) map.getOrDefault("chest-type", "ROGUELITE_CHEST")
                );
                eff.setBlessingType((String) map.getOrDefault("blessing_type", "Chest_General"));
                eff.setSlotsCount(asInt(map.getOrDefault("slots", 3)));
                break;
            case COMPLETE_DUNGEON:
                eff = new DungeonEffect(DungeonEffect.EffectType.COMPLETE_DUNGEON);
                break;
            case SPAWN_UNTIL:
                eff = new DungeonEffect(
                    DungeonEffect.EffectType.SPAWN_UNTIL,
                    (String) map.get("mob-name"), asInt(map.get("amount")), asInt(map.get("level")),
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), asDouble(map.get("range")),
                    asInt(map.get("interval"))
                );
                break;
            case KNOCKBACK:
                eff = new DungeonEffect(
                    DungeonEffect.EffectType.KNOCKBACK, "", 0, 0,
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), 0.0, 0
                );
                break;
            case PULL:
                eff = new DungeonEffect(
                    DungeonEffect.EffectType.PULL, "", asInt(map.get("amount")), 0,
                    asDouble(map.get("x")), asDouble(map.get("y")), asDouble(map.get("z")), 0.0, 0
                );
                break;
            case DAMAGE:
                eff = new DungeonEffect(
                    DungeonEffect.EffectType.DAMAGE, "", asInt(map.get("amount")), 0,
                    0.0, 0.0, 0.0, 0.0, 0
                );
                break;
            case FORCE_FAIL:
                eff = new DungeonEffect(DungeonEffect.EffectType.FORCE_FAIL);
                break;
            case GIVE_ITEM:
                eff = new DungeonEffect(
                    DungeonEffect.EffectType.GIVE_ITEM,
                    (String) map.get("item"),
                    asInt(map.getOrDefault("amount", 1))
                );
                break;
            case SHIELD_REMOVE:
                eff = new DungeonEffect(DungeonEffect.EffectType.SHIELD_REMOVE);
                eff.setRange(asDouble(map.getOrDefault("radius", 5.0)));
                break;
        }
        if (eff != null) {
            if (map.containsKey("x")) eff.setXList(asDoubleList(map.get("x")));
            if (map.containsKey("y")) eff.setYList(asDoubleList(map.get("y")));
            if (map.containsKey("z")) eff.setZList(asDoubleList(map.get("z")));
        }
        return eff;
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

        run.registerBossDeath(victim.getUniqueId());

        if (run.isEnemyMarked(victim.getUniqueId())) {
            Player marker = run.getMarkerPlayer(victim.getUniqueId());
            if (marker != null && marker.isOnline()) {
                double maxHP = marker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                double curHP = marker.getHealth();
                marker.setHealth(Math.min(maxHP, curHP + 4.0));
                marker.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 20, 0));
                marker.playSound(marker.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                marker.getWorld().spawnParticle(org.bukkit.Particle.HEART, marker.getLocation().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0.0);

                if (run.isWaterMarked(victim.getUniqueId())) {
                    for (Player other : run.getOnlinePlayers()) {
                        if (!run.isPlayerSpectator(other)) {
                            double oMax = other.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                            double oCur = other.getHealth();
                            other.setHealth(Math.min(oMax, oCur + 4.0));
                            other.playSound(other.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                            other.getWorld().spawnParticle(org.bukkit.Particle.HEART, other.getLocation().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0.0);
                        }
                    }
                }
            }
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        String name = victim.getName();
        String cleanName = ChatColor.stripColor(name);
        run.onMobKill(victim.getType().name());
        run.onMobKill(cleanName);

        Encounter encounter = run.getActiveEncounter();
        if (encounter != null) {
            for (DungeonCondition condition : encounter.getConditions()) {
                if (condition.getType() == DungeonCondition.ConditionType.DROP_ON_DEATH) {
                    String targetName = (condition.getMobDisplayName() != null && !condition.getMobDisplayName().isEmpty()) ? condition.getMobDisplayName() : condition.getMobName();
                    String cleanTarget = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', targetName));
                    if (cleanName.equalsIgnoreCase(cleanTarget) || victim.getType().name().equalsIgnoreCase(cleanTarget)) {
                        double chance = condition.getChance();
                        if (new Random().nextDouble() * 100 <= chance) {
                            DungeonCustomItem customItem = run.getTemplate().getCustomItems().get(condition.getCustomItemId());
                            if (customItem != null) {
                                ItemStack dropStack = customItem.toItemStack();
                                Player targetPlayer = victim.getKiller();
                                if (targetPlayer == null || !targetPlayer.isOnline() || run.isPlayerSpectator(targetPlayer)) {
                                    for (Player p : run.getOnlinePlayers()) {
                                        if (!run.isPlayerSpectator(p)) {
                                            targetPlayer = p;
                                            break;
                                        }
                                    }
                                }
                                if (targetPlayer != null) {
                                    HashMap<Integer, ItemStack> leftover = targetPlayer.getInventory().addItem(dropStack);
                                    if (!leftover.isEmpty()) {
                                        for (ItemStack left : leftover.values()) {
                                            world.dropItemNaturally(targetPlayer.getLocation(), left);
                                        }
                                    }
                                    targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                                    targetPlayer.sendMessage(ChatColor.GOLD + "[Dungeon] Otrzymano przedmiot: " + (dropStack.getItemMeta() != null && dropStack.getItemMeta().hasDisplayName() ? dropStack.getItemMeta().getDisplayName() : dropStack.getType().name()));
                                } else {
                                    world.dropItemNaturally(victim.getLocation(), dropStack);
                                }
                                condition.setDropOnDeathTriggered(true);
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
            if (stats != null && stats.getBlessingLevel("WATER_STAFF") >= 3 && stats.hasWeaponInInventory(killer, "WATER_STAFF")) {
                com.projectkorra.projectkorra.BendingPlayer bKiller = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(killer);
                if (bKiller != null && bKiller.hasElement(com.projectkorra.projectkorra.Element.getElement("Fire"))) {
                    double currentDur = stats.getWeaponDurability("WATER_STAFF");
                    stats.setWeaponDurability("WATER_STAFF", currentDur + 10.0);
                }
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onShieldedEnemyDamageLowest(EntityDamageEvent event) {
        DungeonInstance run = activeInstances.get(event.getEntity().getWorld());
        if (run != null && run.isShieldedEnemy(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
            event.setDamage(0.0);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onShieldedEnemyDamageHighest(EntityDamageEvent event) {
        DungeonInstance run = activeInstances.get(event.getEntity().getWorld());
        if (run != null && run.isShieldedEnemy(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
            event.setDamage(0.0);
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

        Player attacker = null;
        boolean isProjectile = false;
        org.bukkit.entity.Entity arrowEntity = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
                isProjectile = true;
                arrowEntity = proj;
            }
        }

        if (attacker != null) {
            if (run.handleTargetHit(event.getEntity(), attacker, event.getDamage())) {
                event.setCancelled(true);
                return;
            }
            DungeonPlayerStats stats = run.getPlayerStats(attacker);
            if (stats != null) {
                double damage = event.getDamage();
                boolean isPhysical = event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK 
                    || event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK;

                org.bukkit.configuration.file.FileConfiguration cfg = AmonPackPlugin.getDungeonConfig();
                
                boolean isBow = false;
                boolean isGlove = false;
                boolean isBlueSpirit = false;
                boolean isMaiDaggers = false;
                boolean isEarthMace = false;
                boolean isWindSickle = false;
                boolean isWaterStaff = false;

                if (isProjectile && arrowEntity != null && arrowEntity.hasMetadata("drawing_factor")) {
                    isBow = true;
                } else if (!isProjectile && isPhysical) {
                    ItemStack held = attacker.getInventory().getItemInMainHand();
                    if (held != null && held.hasItemMeta()) {
                        String tag = held.getItemMeta().getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                            org.bukkit.persistence.PersistentDataType.STRING
                        );
                        if ("AMON_GLOVE".equals(tag)) {
                            isGlove = true;
                        } else if ("BLUE_SPIRIT_SWORDS".equals(tag)) {
                            isBlueSpirit = true;
                        } else if ("MAI_DAGGERS".equals(tag)) {
                            isMaiDaggers = true;
                        } else if ("EARTH_MACE".equals(tag)) {
                            isEarthMace = true;
                        } else if ("WIND_SICKLE".equals(tag)) {
                            isWindSickle = true;
                        } else if ("WATER_STAFF".equals(tag)) {
                            isWaterStaff = true;
                        }
                    }
                }

                java.util.Random rnd = new java.util.Random();
                boolean landsCrit = false;
                double critMultiplier = stats.getPCritDmg();

                if (isBow) {
                    double baseBowDmg = 3.0;
                    if (cfg != null) baseBowDmg = cfg.getDouble("blessings.POUHAI_BOW.base-damage", 3.0);

                    double drawingFactor = arrowEntity.getMetadata("drawing_factor").get(0).asDouble();
                    boolean isThird = arrowEntity.hasMetadata("third_shot");

                    damage = baseBowDmg * drawingFactor;

                    if (isThird) {
                        int bowLvl = stats.getBlessingLevel("POUHAI_BOW");
                        if (event.getEntity() instanceof LivingEntity) {
                            LivingEntity vic = (LivingEntity) event.getEntity();
                            
                            if (bowLvl >= 2) {
                                for (org.bukkit.entity.Entity ent : vic.getNearbyEntities(6.0, 6.0, 6.0)) {
                                    if (ent instanceof LivingEntity && !ent.getUniqueId().equals(attacker.getUniqueId()) && !ent.getUniqueId().equals(vic.getUniqueId())) {
                                        LivingEntity le = (LivingEntity) ent;
                                        org.bukkit.util.Vector pullDir = vic.getLocation().toVector().subtract(le.getLocation().toVector());
                                        if (pullDir.lengthSquared() > 0.01) {
                                            pullDir.normalize();
                                            pullDir.setY(0.25);
                                            le.setVelocity(pullDir.multiply(1.1));
                                        }

                                        Location start = le.getLocation().add(0, 1.0, 0);
                                        Location end = vic.getLocation().add(0, 1.0, 0);
                                        double distance = start.distance(end);
                                        Vector delta = end.toVector().subtract(start.toVector()).normalize();
                                        for (double d = 0; d < distance; d += 0.5) {
                                            Location point = start.clone().add(delta.clone().multiply(d));
                                            point.getWorld().spawnParticle(Particle.PORTAL, point, 1, 0, 0, 0, 0);
                                            point.getWorld().spawnParticle(Particle.CLOUD, point, 1, 0.05, 0.05, 0.05, 0);
                                        }
                                    }
                                }
                                vic.getWorld().playSound(vic.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
                            }

                            if (bowLvl >= 3) {
                                com.projectkorra.projectkorra.BendingPlayer bAttacker = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(attacker);
                                if (bAttacker != null && bAttacker.hasElement(com.projectkorra.projectkorra.Element.getElement("Fire"))) {
                                    vic.setFireTicks(80);
                                    for (org.bukkit.entity.Entity ent : vic.getNearbyEntities(6.0, 6.0, 6.0)) {
                                        if (ent instanceof LivingEntity && !ent.getUniqueId().equals(attacker.getUniqueId())) {
                                            ent.setFireTicks(80);
                                        }
                                    }
                                    vic.getWorld().spawnParticle(Particle.FLAME, vic.getLocation().add(0, 1.0, 0), 20, 0.5, 0.5, 0.5, 0.1);
                                    vic.getWorld().playSound(vic.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
                                }
                            }
                        }
                    }
                } else if (isGlove) {
                    double baseGloveDmg = 1.0;
                    if (cfg != null) baseGloveDmg = cfg.getDouble("blessings.AMON_GLOVE.base-damage", 1.0);

                    damage = baseGloveDmg;

                    if (event.getEntity() instanceof LivingEntity) {
                        LivingEntity vic = (LivingEntity) event.getEntity();
                        int amonLvl = stats.getBlessingLevel("AMON_GLOVE");
                        
                        com.projectkorra.projectkorra.BendingPlayer bAttacker = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(attacker);
                        boolean isEarth = bAttacker != null && bAttacker.hasElement(com.projectkorra.projectkorra.Element.getElement("Earth"));

                        org.bukkit.util.Vector knockDir = vic.getLocation().toVector().subtract(attacker.getLocation().toVector());
                        knockDir.setY(0.25);
                        double strength = (amonLvl >= 2 && isEarth) ? 1.4 : 0.8;
                        if (knockDir.lengthSquared() > 0.01) {
                            vic.setVelocity(knockDir.normalize().multiply(strength));
                        }

                        if (amonLvl >= 2 && isEarth) {
                            vic.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
                        }

                        int maxCharge = 5;
                        if (amonLvl == 2) maxCharge = 4;
                        else if (amonLvl >= 3) maxCharge = 3;

                        int curCharge = stats.getAmonGloveCharge();
                        if (curCharge < maxCharge) {
                            curCharge++;
                            stats.setAmonGloveCharge(curCharge);
                            stats.setAmonGloveLastChangeTime(System.currentTimeMillis());
                            run.updateGloveBossBar(attacker, stats);
                            attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 0.7f + curCharge * 0.2f);
                        }

                        if (curCharge >= maxCharge) {
                            stats.setAmonGloveCharge(0);
                            run.updateGloveBossBar(attacker, stats);
                            damage = baseGloveDmg * 1.5;
                            attacker.playSound(attacker.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.8f);
                            attacker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, vic.getLocation().add(0, 1.0, 0), 20, 0.3, 0.3, 0.3, 0.1);

                            triggerAmonChainLightning(attacker, vic, baseGloveDmg * 1.0, amonLvl >= 3);
                        }
                    }
                } else if (isBlueSpirit) {
                    double baseDaggerDmg = 2.0;
                    if (cfg != null) baseDaggerDmg = cfg.getDouble("blessings.BLUE_SPIRIT_SWORDS.base-damage", 2.0);

                    damage = baseDaggerDmg;

                    if (event.getEntity() instanceof LivingEntity) {
                        LivingEntity vic = (LivingEntity) event.getEntity();
                        int maiLvl = stats.getBlessingLevel("BLUE_SPIRIT_SWORDS");

                        UUID lastTarget = stats.getLastMaiHitTarget();
                        UUID curTarget = vic.getUniqueId();
                        boolean forceCrit = false;
                        if (lastTarget == null || !lastTarget.equals(curTarget)) {
                            forceCrit = true;
                        }
                        stats.setLastMaiHitTarget(curTarget);

                        long lastHitTime = stats.getLastMaiHitTime();
                        boolean idleCritDamageBoost = false;
                        if (maiLvl >= 2 && System.currentTimeMillis() - lastHitTime >= 5000) {
                            idleCritDamageBoost = true;
                        }
                        stats.setLastMaiHitTime(System.currentTimeMillis());

                        double critRate = stats.getPCritRate();
                        com.projectkorra.projectkorra.BendingPlayer bAttacker = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(attacker);
                        boolean isFire = bAttacker != null && bAttacker.hasElement(com.projectkorra.projectkorra.Element.getElement("Fire"));
                        if (maiLvl >= 3 && isFire) {
                            critRate += 0.25;
                        }

                        if (forceCrit || rnd.nextDouble() < critRate) {
                            landsCrit = true;
                            if (idleCritDamageBoost) {
                                critMultiplier += 0.50;
                            }
                        }

                        if (landsCrit) {
                            if (maiLvl >= 3) {
                                if (rnd.nextDouble() < 0.33) {
                                    double maxHP = attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                                    double curHP = attacker.getHealth();
                                    attacker.setHealth(Math.min(maxHP, curHP + 2.0));
                                    attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0.0);
                                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                                }
                                if (isFire) {
                                    vic.setFireTicks(60);
                                }
                            }
                        }
                    }
                } else if (isMaiDaggers) {
                    double baseDaggerDmg = 1.0;
                    if (cfg != null) baseDaggerDmg = cfg.getDouble("blessings.MAI_DAGGERS.base-damage", 1.0);

                    damage = baseDaggerDmg;

                    double curDur = stats.getWeaponDurability("MAI_DAGGERS");
                    stats.setWeaponDurability("MAI_DAGGERS", curDur + 33.0);
                } else if (isEarthMace) {
                    double earthBase = 3.0;
                    if (cfg != null) earthBase = cfg.getDouble("blessings.EARTH_MACE.base-damage", 3.0);

                    damage = earthBase;

                    if (event.getEntity() instanceof LivingEntity) {
                        LivingEntity vic = (LivingEntity) event.getEntity();
                        int maceLvl = stats.getBlessingLevel("EARTH_MACE");
                        double dur = stats.getWeaponDurability("EARTH_MACE");

                        if (dur >= 100.0) {
                            stats.setWeaponDurability("EARTH_MACE", 5.0);
                            double scale = (maceLvl >= 2) ? 2.5 : 2.0;
                            damage = earthBase * scale;

                            vic.getWorld().spawnParticle(Particle.CRIT, vic.getLocation().add(0, 1.0, 0), 30, 0.3, 0.3, 0.3, 0.2);
                            try {
                                vic.getWorld().spawnParticle(Particle.BLOCK, vic.getLocation(), 40, 0.5, 0.5, 0.5, 0.1, Material.COARSE_DIRT.createBlockData());
                            } catch (Exception ignored) {}
                            vic.getWorld().playSound(vic.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 0.8f);

                            if (maceLvl >= 3) {
                                com.projectkorra.projectkorra.BendingPlayer bAttacker = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(attacker);
                                if (bAttacker != null && bAttacker.hasElement(com.projectkorra.projectkorra.Element.getElement("Air"))) {
                                    attacker.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
                                    attacker.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0));
                                    Abilities.Bending.SoundAbility.HandleDamage(attacker, vic, 10.0);
                                }
                            }
                        }
                    }
                } else if (isWindSickle) {
                    double sickleBase = 1.0;
                    if (cfg != null) sickleBase = cfg.getDouble("blessings.WIND_SICKLE.base-damage", 1.0);

                    int sickleLvl = stats.getBlessingLevel("WIND_SICKLE");
                    double dur = stats.getWeaponDurability("WIND_SICKLE");
                    double scaling = 1.0;

                    if (sickleLvl >= 3) {
                        if (dur >= 100.0) {
                            com.projectkorra.projectkorra.BendingPlayer bAttacker = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(attacker);
                            boolean isFire = bAttacker != null && bAttacker.hasElement(com.projectkorra.projectkorra.Element.getElement("Fire"));
                            if (isFire && event.getEntity() instanceof LivingEntity && ((LivingEntity) event.getEntity()).getFireTicks() > 0) {
                                scaling = 3.0;
                            } else {
                                scaling = 2.5;
                            }
                        }
                        else if (dur >= 75.0) scaling = 2.125;
                        else if (dur >= 50.0) scaling = 1.75;
                        else if (dur >= 25.0) scaling = 1.375;
                    } else {
                        if (dur >= 100.0) scaling = 2.0;
                        else if (dur >= 75.0) scaling = 1.75;
                        else if (dur >= 50.0) scaling = 1.5;
                        else if (dur >= 25.0) scaling = 1.25;
                    }
                    damage = sickleBase * scaling;
                } else if (isWaterStaff) {
                    double waterBase = 1.0;
                    if (cfg != null) waterBase = cfg.getDouble("blessings.WATER_STAFF.base-damage", 1.0);

                    int waterLvl = stats.getBlessingLevel("WATER_STAFF");
                    if (waterLvl >= 2) {
                        com.projectkorra.projectkorra.BendingPlayer bAttacker = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(attacker);
                        if (bAttacker != null) {
                            int count = 0;
                            for (int i = 1; i <= 9; i++) {
                                String abilityName = bAttacker.getAbilities().get(i);
                                if (abilityName != null) {
                                    com.projectkorra.projectkorra.ability.CoreAbility ability = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(abilityName);
                                    if (ability != null) {
                                        if (ability instanceof com.projectkorra.projectkorra.ability.HealingAbility 
                                            || ability instanceof com.projectkorra.projectkorra.ability.BloodAbility
                                            || (ability.getElement() != null && (ability.getElement().getName().equalsIgnoreCase("Healing") || ability.getElement().getName().equalsIgnoreCase("Blood")))) {
                                            count++;
                                        }
                                    }
                                }
                            }
                            double multiplier = 1.0 + (0.50 * count);
                            if (multiplier > 3.0) {
                                multiplier = 3.0;
                            }
                            damage = waterBase * multiplier;
                        } else {
                            damage = waterBase;
                        }

                        if (event.getEntity() instanceof LivingEntity) {
                            LivingEntity vic = (LivingEntity) event.getEntity();
                            for (org.bukkit.entity.Entity ent : vic.getNearbyEntities(4.0, 4.0, 4.0)) {
                                if (ent instanceof LivingEntity && !ent.getUniqueId().equals(attacker.getUniqueId()) && !ent.getUniqueId().equals(vic.getUniqueId())) {
                                    LivingEntity le = (LivingEntity) ent;
                                    org.bukkit.util.Vector pushDir = le.getLocation().toVector().subtract(attacker.getLocation().toVector());
                                    if (pushDir.lengthSquared() > 0.01) {
                                        pushDir.normalize();
                                    } else {
                                        pushDir = new org.bukkit.util.Vector(0, 0, 1);
                                    }
                                    pushDir.setY(0.25);
                                    le.setVelocity(pushDir.multiply(0.8));
                                }
                            }
                            vic.getWorld().spawnParticle(org.bukkit.Particle.SPLASH, vic.getLocation().add(0, 1.0, 0), 20, 0.4, 0.4, 0.4, 0.1);
                        }
                    } else {
                        damage = waterBase;
                    }
                }

                damage = stats.calculateOutgoingDamage(damage);

                if (isBow) {
                    boolean isThird = arrowEntity.hasMetadata("third_shot");
                    if (isThird) {
                        try {
                            event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.ARMOR, 0.0);
                        } catch (Exception ignored) {}
                    } else {
                        if (rnd.nextDouble() < stats.getPCritRate()) {
                            damage *= stats.getPCritDmg();
                            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                            if (event.getEntity() instanceof LivingEntity) {
                                LivingEntity vic = (LivingEntity) event.getEntity();
                                vic.getWorld().spawnParticle(Particle.CRIT, vic.getLocation().add(0, 1.0, 0), 10, 0.2, 0.2, 0.2, 0.15);
                            }
                        }
                    }
                } else if (isGlove) {
                    if (rnd.nextDouble() < stats.getPCritRate()) {
                        damage *= stats.getPCritDmg();
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                        if (event.getEntity() instanceof LivingEntity) {
                            LivingEntity vic = (LivingEntity) event.getEntity();
                            vic.getWorld().spawnParticle(Particle.CRIT, vic.getLocation().add(0, 1.0, 0), 10, 0.2, 0.2, 0.2, 0.15);
                        }
                    }
                } else if (isBlueSpirit) {
                    if (landsCrit) {
                        damage *= critMultiplier;
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                        if (event.getEntity() instanceof LivingEntity) {
                            LivingEntity vic = (LivingEntity) event.getEntity();
                            vic.getWorld().spawnParticle(Particle.CRIT, vic.getLocation().add(0, 1.0, 0), 10, 0.2, 0.2, 0.2, 0.15);
                        }
                    }
                } else if (isEarthMace || isWindSickle || isWaterStaff || isMaiDaggers) {
                    if (rnd.nextDouble() < stats.getPCritRate()) {
                        damage *= stats.getPCritDmg();
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                        if (event.getEntity() instanceof LivingEntity) {
                            LivingEntity vic = (LivingEntity) event.getEntity();
                            vic.getWorld().spawnParticle(Particle.CRIT, vic.getLocation().add(0, 1.0, 0), 10, 0.2, 0.2, 0.2, 0.15);
                        }
                    }
                    if (event.getEntity() instanceof LivingEntity) {
                        LivingEntity vic = (LivingEntity) event.getEntity();
                        DungeonBlessingManager.handlePoison(attacker, vic, stats);
                    }
                } else {
                    if (isPhysical) {
                        if (rnd.nextDouble() < stats.getPCritRate()) {
                            damage *= stats.getPCritDmg();
                            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                            if (event.getEntity() instanceof LivingEntity) {
                                LivingEntity vic = (LivingEntity) event.getEntity();
                                vic.getWorld().spawnParticle(Particle.CRIT, vic.getLocation().add(0, 1.0, 0), 10, 0.2, 0.2, 0.2, 0.15);
                            }
                        }
                        if (event.getEntity() instanceof LivingEntity) {
                            LivingEntity vic = (LivingEntity) event.getEntity();
                            DungeonBlessingManager.handlePoison(attacker, vic, stats);
                        }
                    } else {
                        if (rnd.nextDouble() < stats.getMCritRate()) {
                            damage *= stats.getMCritDmg();
                            attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.8f);
                            if (event.getEntity() instanceof LivingEntity) {
                                LivingEntity vic = (LivingEntity) event.getEntity();
                                vic.getWorld().spawnParticle(Particle.GLOW, vic.getLocation().add(0, 1.0, 0), 1, 0.0, 0.0, 0.0, 0.0);
                                vic.getWorld().spawnParticle(Particle.ENCHANTED_HIT, vic.getLocation().add(0, 1.0, 0), 8, 0.2, 0.2, 0.2, 0.15);
                            }
                        }
                    }
                }

                event.setDamage(damage);

                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity vic = (LivingEntity) event.getEntity();
                    DungeonBlessingManager.handleCombustion(attacker, vic, stats, event);
                }

                DungeonBlessingManager.handleAdrenaline(attacker, event, stats);

                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity vic = (LivingEntity) event.getEntity();
                    DungeonBlessingManager.handleFlaming(attacker, vic, stats);
                    DungeonBlessingManager.handleLifesteal(attacker, vic, event.getDamage(), stats);
                    DungeonBlessingManager.handleKnockback(attacker, vic, stats);
                }
            }
        }

        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            DungeonPlayerStats stats = run.getPlayerStats(victim);
            if (stats != null) {
                if (event.getDamager() instanceof LivingEntity) {
                    LivingEntity livingDamager = (LivingEntity) event.getDamager();
                    boolean countered = DungeonBlessingManager.handleCounter(victim, livingDamager, event.getDamage(), stats, event);
                    if (countered) {
                        return;
                    }
                }
                double baseDamage = event.getDamage();
                if (stats.getBlessingLevel("EARTH_MACE") > 0 && stats.hasWeaponInInventory(victim, "EARTH_MACE")) {
                    double curDur = stats.getWeaponDurability("EARTH_MACE");
                    stats.setWeaponDurability("EARTH_MACE", curDur + baseDamage * 12.5);
                }

                double def = stats.getDefBoost();
                if (stats.getBlessingLevel("EARTH_MACE") > 0 && stats.hasWeaponInInventory(victim, "EARTH_MACE")) {
                    def *= 2.0;
                }
                double reduction = def <= 0 ? 0.0 : def / (def + 50.0);
                double newDmg = baseDamage * (1.0 - reduction);
                event.setDamage(newDmg);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;

        if (run.isPlayerSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItem();

        if (item != null && event.getAction().name().startsWith("RIGHT_CLICK")) {
            DungeonPlayerStats stats = run.getPlayerStats(player);
            if (stats != null) {
                int sickleLvl = stats.getBlessingLevel("WIND_SICKLE");
                if (sickleLvl >= 2) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                        org.bukkit.NamespacedKey nkey = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type");
                        if (pdc.has(nkey, org.bukkit.persistence.PersistentDataType.STRING) && "WIND_SICKLE".equals(pdc.get(nkey, org.bukkit.persistence.PersistentDataType.STRING))) {
                            double dur = stats.getWeaponDurability("WIND_SICKLE");
                            if (dur > 90.0) {
                                event.setCancelled(true);
                                triggerWindSickleSkill(player, stats, sickleLvl, run);
                                return;
                            }
                        }
                    }
                }

                int maiLvl = stats.getBlessingLevel("MAI_DAGGERS");
                if (maiLvl > 0) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                        org.bukkit.NamespacedKey nkey = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type");
                        if (pdc.has(nkey, org.bukkit.persistence.PersistentDataType.STRING) && "MAI_DAGGERS".equals(pdc.get(nkey, org.bukkit.persistence.PersistentDataType.STRING))) {
                            double dur = stats.getWeaponDurability("MAI_DAGGERS");
                            if (dur >= 100.0) {
                                event.setCancelled(true);
                                stats.setWeaponDurability("MAI_DAGGERS", 0.0);
                                run.updateVisualDurability(item, 0.0);
                                triggerMaiDaggersThrow(player, stats, maiLvl, run);
                                return;
                            }
                        }
                    }
                }
            }
        }

        if (item != null && item.getType() == Material.BOW && event.getAction().name().startsWith("RIGHT_CLICK")) {
            DungeonPlayerStats stats = run.getPlayerStats(player);
            if (stats != null && stats.getBlessingLevel("POUHAI_BOW") > 0) {
                stats.setPouhaiBowPullStartTime(System.currentTimeMillis());
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type");
                    if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING) && "POUHAI_BOW".equals(pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING))) {
                        if (!player.getInventory().contains(Material.ARROW)) {
                            ItemStack virtualArrow = new ItemStack(Material.ARROW, 1);
                            org.bukkit.inventory.meta.ItemMeta arrowMeta = virtualArrow.getItemMeta();
                            if (arrowMeta != null) {
                                arrowMeta.setDisplayName(ChatColor.GRAY + "Virtual Arrow");
                                org.bukkit.persistence.PersistentDataContainer arrowPdc = arrowMeta.getPersistentDataContainer();
                                org.bukkit.NamespacedKey vKey = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_virtual_arrow");
                                arrowPdc.set(vKey, org.bukkit.persistence.PersistentDataType.STRING, "true");
                                virtualArrow.setItemMeta(arrowMeta);
                            }
                            player.getInventory().addItem(virtualArrow);
                            player.updateInventory();
                        }
                    }
                }
            }
        }

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
                    if (customItem != null && ("consumable".equalsIgnoreCase(customItem.getType()) || "consumables".equalsIgnoreCase(customItem.getType()))) {
                        event.setCancelled(true);
                        if ("PPM".equalsIgnoreCase(customItem.getUseMode())) {
                            int newAmt = item.getAmount() - 1;
                            if (newAmt > 0) {
                                item.setAmount(newAmt);
                            } else {
                                player.getInventory().setItemInMainHand(null);
                            }
                            applyConsumableEffect(player, customItem, run);
                        }
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
                        if (!condition.isMet(run) && condition.isMetInteract(block.getLocation(), block.getType(), item, run)) {
                            if (item != null && item.getAmount() > 0 && condition.isRequiredItems()) {
                                int newAmt = item.getAmount() - 1;
                                if (newAmt > 0) {
                                    item.setAmount(newAmt);
                                } else {
                                    player.getInventory().setItemInMainHand(null);
                                }
                            }
                            
                            for (DungeonEffect effect : condition.getOnCompleteEffects()) {
                                effect.execute(run);
                            }

                            event.setCancelled(true);

                            boolean allMet = true;
                            for (DungeonCondition cond : encounter.getConditions()) {
                                if (!cond.isMet(run)) {
                                    allMet = false;
                                    break;
                                }
                            }
                            if (allMet && !encounter.getConditions().isEmpty()) {
                                run.transitionToNext();
                            }
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

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() == Material.ARROW && clickedItem.hasItemMeta()) {
            org.bukkit.persistence.PersistentDataContainer pdc = clickedItem.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey vKey = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_virtual_arrow");
            if (pdc.has(vKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                event.setCancelled(true);
                event.getWhoClicked().getInventory().remove(clickedItem);
                if (event.getWhoClicked() instanceof Player) {
                    ((Player) event.getWhoClicked()).updateInventory();
                }
                return;
            }
        }

        if (event.getSlot() == 40 || event.getRawSlot() == 45) {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (off != null && off.hasItemMeta()) {
                String tag = off.getItemMeta().getPersistentDataContainer().get(
                    new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                    PersistentDataType.STRING
                );
                if ("BLUE_SPIRIT_SWORDS_OFFHAND".equals(tag) || "MAI_DAGGERS_OFFHAND".equals(tag) || "WIND_SICKLE_OFFHAND".equals(tag)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        org.bukkit.Bukkit.getScheduler().runTask(AmonPackPlugin.plugin, () -> {
            checkAndSwapBlueSpiritSwords(player, player.getInventory().getItemInMainHand());
            checkAndSwapNewMaiDaggers(player, player.getInventory().getItemInMainHand());
        });

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
                        stats.addBoundDungeonSkill(option.key);
                        if (branch != null) {
                            branch.getTemporaryAbilities().add(option.key);
                            
                            com.projectkorra.projectkorra.Element skillElement = null;
                            org.bukkit.configuration.file.FileConfiguration skillTreeConfig = AmonPackPlugin.getSkillTreeConfig();
                            if (skillTreeConfig != null && skillTreeConfig.getConfigurationSection("AmonPack.Tree") != null) {
                                for (String elName : skillTreeConfig.getConfigurationSection("AmonPack.Tree").getKeys(false)) {
                                    com.projectkorra.projectkorra.Element pkEl = com.projectkorra.projectkorra.Element.getElement(elName);
                                    if (pkEl != null) {
                                        RPG.Levels.BendingTree.ElementTree tree = AmonPackPlugin.levelsBending.GetElement(pkEl);
                                        if (tree != null) {
                                            for (RPG.Levels.BendingTree.SkillTree_Ability ability : tree.getAbilities()) {
                                                if (ability.getName().equalsIgnoreCase(option.key)) {
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
                        player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.YELLOW + "Odblokowano ruch: " + option.key + " na czas tego dungeonu!");
                        break;
                        
                    case STAT:
                        try {
                            double val = Double.parseDouble(option.value);
                            applyUniversalStat(stats, option.key, val, player);
                        } catch (NumberFormatException e) {
                        }
                        break;
                        
                    case BLESSING:
                        if (!stats.hasBlessing(option.key)) {
                            stats.addActiveBlessing(option.key);
                            stats.upgradeBlessing(option.key);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.LIGHT_PURPLE + "Zdobyles Blogoslawienstwo: " + option.key + " (Poziom 1)!");
                        } else {
                            stats.upgradeBlessing(option.key);
                            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.LIGHT_PURPLE + "Ulepszyles Blogoslawienstwo: " + option.key + " do poziomu " + stats.getBlessingLevel(option.key) + "!");
                        }
                        if (option.key.equals("WATER_STAFF")) {
                            int level = stats.getBlessingLevel("WATER_STAFF");
                            if (level == 1) {
                                stats.addHpBoost(4.0);
                                stats.addMCritRate(0.10);
                            } else if (level == 2) {
                                for (Player p : run.getOnlinePlayers()) {
                                    DungeonPlayerStats ps = run.getPlayerStats(p);
                                    if (ps != null) {
                                        ps.addMCritRate(0.10);
                                    }
                                }
                            } else if (level == 3) {
                                stats.addHpBoost(4.0);
                                stats.addMCritRate(0.10);
                                com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
                                if (bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Earth"))) {
                                    stats.addHpBoost(-12.0);
                                }
                            }
                            stats.applyStatsToPlayer(player);
                        } else if (option.key.equals("EARTH_MACE")) {
                            int level = stats.getBlessingLevel("EARTH_MACE");
                            if (level == 3) {
                                com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
                                if (bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Earth"))) {
                                    stats.addRegenLevel(1);
                                }
                            }
                        }
                        if (option.key.equals("POUHAI_BOW") || option.key.equals("AMON_GLOVE") || option.key.equals("MAI_DAGGERS") || option.key.equals("BLUE_SPIRIT_SWORDS") || option.key.equals("EARTH_MACE") || option.key.equals("WIND_SICKLE") || option.key.equals("WATER_STAFF")) {
                            giveOrUpdateLegendaryWeapon(player, stats, option.key);
                        }
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

    private List<Double> asDoubleList(Object o) {
        List<Double> list = new ArrayList<>();
        if (o instanceof List) {
            for (Object obj : (List<?>) o) {
                if (obj instanceof Number) {
                    list.add(((Number) obj).doubleValue());
                }
            }
        } else if (o instanceof Number) {
            list.add(((Number) o).doubleValue());
        }
        return list;
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
        event.setCancelled(true);
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

    @EventHandler
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (activeInstances.containsKey(player.getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (activeInstances.containsKey(player.getWorld())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    public void throwProjectile(Player player, DungeonCustomItem customItem, DungeonInstance run) {
        World world = player.getWorld();
        Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector velocity = player.getEyeLocation().getDirection().normalize().multiply(0.8);

        org.bukkit.entity.Item thrownItem = world.dropItem(startLoc, customItem.toItemStack());
        thrownItem.setPickupDelay(32767);
        thrownItem.setGravity(false);
        thrownItem.setVelocity(velocity);

        world.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (ticks > 100 || thrownItem.isDead() || run.isFinished()) {
                    boolean shouldReturn = customItem.isReturning() || run.hasReturningThrowCondition();
                    if (shouldReturn && player.isOnline() && !run.isFinished()) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(customItem.toItemStack());
                        if (!leftover.isEmpty()) {
                            for (ItemStack left : leftover.values()) {
                                world.dropItemNaturally(player.getLocation(), left);
                            }
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                        player.sendMessage(ChatColor.YELLOW + "[Dungeon] Nie trafiono w cel! Przedmiot wrócił do ekwipunku.");
                    }
                    thrownItem.remove();
                    cancel();
                    return;
                }

                currentLoc.add(velocity);
                velocity.setY(velocity.getY() - 0.025);
                
                thrownItem.teleport(currentLoc);
                thrownItem.setVelocity(velocity);

                world.spawnParticle(Particle.CRIT, currentLoc, 3, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.DUST, currentLoc, 2, 0.05, 0.05, 0.05, 0.01, new Particle.DustOptions(Color.ORANGE, 0.8f));

                if (currentLoc.getBlock().getType().isSolid()) {
                    boolean hitTarget = run.registerProjectileHitCoord(currentLoc);
                    world.playSound(currentLoc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
                    world.spawnParticle(Particle.BLOCK, currentLoc, 15, 0.2, 0.2, 0.2, Material.STONE.createBlockData());
                    if (!hitTarget) {
                        boolean shouldReturn = customItem.isReturning() || run.hasReturningThrowCondition();
                        if (shouldReturn && player.isOnline() && !run.isFinished()) {
                            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(customItem.toItemStack());
                            if (!leftover.isEmpty()) {
                                for (ItemStack left : leftover.values()) {
                                    world.dropItemNaturally(player.getLocation(), left);
                                }
                            }
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                            player.sendMessage(ChatColor.YELLOW + "[Dungeon] Nie trafiono w cel! Przedmiot wrócił do ekwipunku.");
                        }
                    }
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

    @org.bukkit.event.EventHandler
    public void onPlayerRegen(org.bukkit.event.entity.EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            DungeonInstance run = activeInstances.get(player.getWorld());
            if (run != null) {
                org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
                if (reason == org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.SATIATED 
                    || reason == org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.REGEN) {
                    event.setCancelled(true);
                    return;
                }
                DungeonPlayerStats stats = run.getPlayerStats(player);
                if (stats != null) {
                    DungeonBlessingManager.handleHeal(player, event, stats);
                }
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onEntityCombustByEntity(org.bukkit.event.entity.EntityCombustByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            DungeonInstance run = activeInstances.get(player.getWorld());
            if (run != null) {
                if (event.getCombuster() != null && !(event.getCombuster() instanceof Player)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void giveItemSafely(Player player, ItemStack item) {
        if (player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            player.getInventory().setItemInMainHand(item);
        } else {
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            }
        }
    }

    public void giveOrUpdateLegendaryWeapon(Player player, DungeonPlayerStats stats, String key) {
        org.bukkit.configuration.file.FileConfiguration cfg = AmonPackPlugin.getDungeonConfig();
        org.bukkit.configuration.ConfigurationSection sec = cfg != null ? cfg.getConfigurationSection("blessings." + key) : null;
        if (sec != null) {
            Material mat = Material.getMaterial(sec.getString("material", "WOODEN_SWORD"));
            int modelId = sec.getInt("custom-model-id", 0);
            String dispName = sec.getString("display-name", key);
            List<String> loreLines = sec.getStringList("lore");
            int level = stats.getBlessingLevel(key);

            ItemStack item = new ItemStack(mat == null ? Material.WOODEN_SWORD : mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', dispName + " &e(Poziom " + level + ")"));
                List<String> coloredLore = new ArrayList<>();
                for (String line : loreLines) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', "&7Poziom: &a" + level));
                meta.setLore(coloredLore);
                if (modelId > 0) {
                    meta.setCustomModelData(modelId);
                }
                meta.getPersistentDataContainer().set(
                    new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                    PersistentDataType.STRING,
                    key
                );
                item.setItemMeta(meta);
            }

            ItemStack existing = null;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.hasItemMeta()) {
                    String tag = invItem.getItemMeta().getPersistentDataContainer().get(
                        new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                        PersistentDataType.STRING
                    );
                    if (key.equals(tag)) {
                        existing = invItem;
                        break;
                    }
                }
            }

            if (existing != null) {
                existing.setType(item.getType());
                existing.setItemMeta(item.getItemMeta());
                player.sendMessage(ChatColor.GREEN + "[Dungeons] Zaktualizowano " + dispName + " do Poziomu " + level + " w Twoim ekwipunku!");
            } else {
                giveItemSafely(player, item);
                player.sendMessage(ChatColor.GREEN + "[Dungeons] Otrzymales " + dispName + " (Poziom " + level + ")!");
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onPlayerItemHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;
        removeVirtualArrows(player);

        DungeonPlayerStats stats = run.getPlayerStats(player);
        if (stats == null) return;

        ItemStack newHeld = player.getInventory().getItem(event.getNewSlot());
        checkAndSwapBlueSpiritSwords(player, newHeld);
        checkAndSwapNewMaiDaggers(player, newHeld);
        checkAndSwapWindSickle(player, newHeld);
    }

    @org.bukkit.event.EventHandler
    public void onBowShoot(org.bukkit.event.entity.EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;

        DungeonPlayerStats stats = run.getPlayerStats(player);
        if (stats == null) return;

        int lvl = stats.getBlessingLevel("POUHAI_BOW");
        if (lvl <= 0) return;

        org.bukkit.inventory.ItemStack bow = event.getBow();
        if (bow == null || bow.getType() != Material.BOW) return;

        org.bukkit.inventory.meta.ItemMeta meta = bow.getItemMeta();
        if (meta == null) return;

        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type");
        if (!pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING) 
            || !"POUHAI_BOW".equals(pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING))) {
            return;
        }

        long start = stats.getPouhaiBowPullStartTime();
        double sec = (System.currentTimeMillis() - start) / 1000.0;
        if (sec < 0.1 || sec > 30.0) {
            sec = 0.5;
        }

        stats.setPouhaiBowShotCount(stats.getPouhaiBowShotCount() + 1);
        boolean isThird = stats.getPouhaiBowShotCount() % 3 == 0;
        double maxSec = (isThird && lvl >= 3) ? 6.0 : 4.0;
        double factor = 1.0 + Math.min(sec, maxSec) * 0.25;

        org.bukkit.entity.Entity arrow = event.getProjectile();
        arrow.setMetadata("drawing_factor", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, factor));
        arrow.setMetadata("bow_owner_uuid", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, player.getUniqueId().toString()));
        if (arrow instanceof org.bukkit.entity.AbstractArrow) {
            ((org.bukkit.entity.AbstractArrow) arrow).setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
        }
        if (isThird) {
            arrow.setMetadata("third_shot", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, true));
            if (arrow instanceof org.bukkit.entity.AbstractArrow) {
                ((org.bukkit.entity.AbstractArrow) arrow).setPierceLevel(10);
            }
        }
    }

    public void checkAndSwapBlueSpiritSwords(Player player, ItemStack held) {
        boolean isSpirit = false;
        if (held != null && held.getType() == Material.STONE_SWORD && held.hasItemMeta()) {
            String tag = held.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                PersistentDataType.STRING
            );
            if ("BLUE_SPIRIT_SWORDS".equals(tag)) {
                isSpirit = true;
            }
        }

        if (isSpirit) {
            ItemStack off = player.getInventory().getItemInOffHand();
            boolean already = false;
            if (off != null && off.hasItemMeta()) {
                String tag = off.getItemMeta().getPersistentDataContainer().get(
                    new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                    PersistentDataType.STRING
                );
                if ("BLUE_SPIRIT_SWORDS_OFFHAND".equals(tag)) {
                    already = true;
                }
            }

            if (!already) {
                ItemStack oldOff = player.getInventory().getItemInOffHand();
                if (oldOff != null && oldOff.getType() != Material.AIR) {
                    player.setMetadata("blue_spirit_offhand_backup", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, oldOff));
                } else {
                    player.setMetadata("blue_spirit_offhand_backup", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, new ItemStack(Material.AIR)));
                }

                ItemStack duplicate = held.clone();
                duplicate.setAmount(1);
                ItemMeta meta = duplicate.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lLewy Miecz Niebieskiego Ducha"));
                    meta.getPersistentDataContainer().set(
                        new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                        PersistentDataType.STRING,
                        "BLUE_SPIRIT_SWORDS_OFFHAND"
                    );
                    duplicate.setItemMeta(meta);
                }
                player.getInventory().setItemInOffHand(duplicate);
            }
        } else {
            if (player.hasMetadata("blue_spirit_offhand_backup")) {
                ItemStack backup = null;
                for (org.bukkit.metadata.MetadataValue val : player.getMetadata("blue_spirit_offhand_backup")) {
                    if (val.getOwningPlugin().equals(AmonPackPlugin.plugin)) {
                        backup = (ItemStack) val.value();
                        break;
                    }
                }
                player.removeMetadata("blue_spirit_offhand_backup", AmonPackPlugin.plugin);
                if (backup != null && backup.getType() != Material.AIR) {
                    player.getInventory().setItemInOffHand(backup);
                } else {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }
    }

    public void checkAndSwapNewMaiDaggers(Player player, ItemStack held) {
        boolean isMai = false;
        if (held != null && held.getType() == Material.GOLDEN_SWORD && held.hasItemMeta()) {
            String tag = held.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                PersistentDataType.STRING
            );
            if ("MAI_DAGGERS".equals(tag)) {
                isMai = true;
            }
        }

        if (isMai) {
            ItemStack off = player.getInventory().getItemInOffHand();
            boolean already = false;
            if (off != null && off.hasItemMeta()) {
                String tag = off.getItemMeta().getPersistentDataContainer().get(
                    new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                    PersistentDataType.STRING
                );
                if ("MAI_DAGGERS_OFFHAND".equals(tag)) {
                    already = true;
                }
            }

            if (!already) {
                ItemStack oldOff = player.getInventory().getItemInOffHand();
                if (oldOff != null && oldOff.getType() != Material.AIR) {
                    player.setMetadata("mai_daggers_offhand_backup", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, oldOff));
                } else {
                    player.setMetadata("mai_daggers_offhand_backup", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, new ItemStack(Material.AIR)));
                }

                ItemStack duplicate = held.clone();
                duplicate.setAmount(1);
                ItemMeta meta = duplicate.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lLewy Sztylet Mai"));
                    meta.getPersistentDataContainer().set(
                        new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                        PersistentDataType.STRING,
                        "MAI_DAGGERS_OFFHAND"
                    );
                    duplicate.setItemMeta(meta);
                }
                player.getInventory().setItemInOffHand(duplicate);
            }
        } else {
            if (player.hasMetadata("mai_daggers_offhand_backup")) {
                ItemStack backup = null;
                for (org.bukkit.metadata.MetadataValue val : player.getMetadata("mai_daggers_offhand_backup")) {
                    if (val.getOwningPlugin().equals(AmonPackPlugin.plugin)) {
                        backup = (ItemStack) val.value();
                        break;
                    }
                }
                player.removeMetadata("mai_daggers_offhand_backup", AmonPackPlugin.plugin);
                if (backup != null && backup.getType() != Material.AIR) {
                    player.getInventory().setItemInOffHand(backup);
                } else {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }
    }

    private void triggerAmonChainLightning(Player attacker, LivingEntity source, double lightningDamage, boolean applyStun) {
        List<LivingEntity> hitList = new ArrayList<>();
        hitList.add(source);

        LivingEntity current = source;
        for (int i = 0; i < 5; i++) {
            LivingEntity next = null;
            double bestDist = 36.0;
            for (org.bukkit.entity.Entity ent : current.getNearbyEntities(6.0, 6.0, 6.0)) {
                if (ent instanceof LivingEntity && !ent.getUniqueId().equals(attacker.getUniqueId())) {
                    LivingEntity target = (LivingEntity) ent;
                    int hits = java.util.Collections.frequency(hitList, target);
                    if (hits < 2) {
                        double dist = current.getLocation().distanceSquared(target.getLocation());
                        if (dist < bestDist) {
                            bestDist = dist;
                            next = target;
                        }
                    }
                }
            }

            if (next != null) {
                hitList.add(next);
                next.damage(lightningDamage, attacker);

                Location start = current.getLocation().add(0, 1.0, 0);
                Location end = next.getLocation().add(0, 1.0, 0);
                double distance = start.distance(end);
                Vector delta = end.toVector().subtract(start.toVector()).normalize();
                for (double d = 0; d < distance; d += 0.4) {
                    Location point = start.clone().add(delta.clone().multiply(d));
                    point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);
                }

                next.getWorld().playSound(next.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 1.8f);

                if (applyStun) {
                    next.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 9));
                    next.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 20, 0));
                    next.getWorld().spawnParticle(Particle.GLOW, next.getLocation().add(0, 1.0, 0), 1, 0, 0, 0, 0);
                }
                current = next;
            } else {
                break;
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onPlayerItemDamage(org.bukkit.event.player.PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run != null) {
            event.setCancelled(true);
        }
    }

    public void checkAndSwapWindSickle(Player player, ItemStack held) {
        boolean isSickle = false;
        if (held != null && held.getType() == Material.IRON_SWORD && held.hasItemMeta()) {
            String tag = held.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                PersistentDataType.STRING
            );
            if ("WIND_SICKLE".equals(tag)) {
                isSickle = true;
            }
        }

        if (isSickle) {
            ItemStack off = player.getInventory().getItemInOffHand();
            boolean already = false;
            if (off != null && off.hasItemMeta()) {
                String tag = off.getItemMeta().getPersistentDataContainer().get(
                    new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                    PersistentDataType.STRING
                );
                if ("WIND_SICKLE_OFFHAND".equals(tag)) {
                    already = true;
                }
            }

            if (!already) {
                ItemStack oldOff = player.getInventory().getItemInOffHand();
                if (oldOff != null && oldOff.getType() != Material.AIR) {
                    player.setMetadata("sickle_offhand_backup", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, oldOff));
                } else {
                    player.setMetadata("sickle_offhand_backup", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, new ItemStack(Material.AIR)));
                }

                ItemStack duplicate = held.clone();
                duplicate.setAmount(1);
                ItemMeta meta = duplicate.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lLewy Sierp Wiatru"));
                    meta.getPersistentDataContainer().set(
                        new NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type"),
                        PersistentDataType.STRING,
                        "WIND_SICKLE_OFFHAND"
                    );
                    duplicate.setItemMeta(meta);
                }
                player.getInventory().setItemInOffHand(duplicate);
            }
        } else {
            if (player.hasMetadata("sickle_offhand_backup")) {
                ItemStack backup = null;
                for (org.bukkit.metadata.MetadataValue val : player.getMetadata("sickle_offhand_backup")) {
                    if (val.getOwningPlugin().equals(AmonPackPlugin.plugin)) {
                        backup = (ItemStack) val.value();
                        break;
                    }
                }
                player.removeMetadata("sickle_offhand_backup", AmonPackPlugin.plugin);
                if (backup != null && backup.getType() != Material.AIR) {
                    player.getInventory().setItemInOffHand(backup);
                } else {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }
    }

    private void triggerWindSickleSkill(Player player, DungeonPlayerStats stats, int lvl, DungeonInstance run) {
        stats.setWeaponDurability("WIND_SICKLE", 5.0);

        com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
        boolean isWater = bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Water"));

        int speedAmplifier = 0;
        if (lvl >= 3 && isWater) {
            speedAmplifier = 1;
        }

        org.bukkit.potion.PotionEffect speedEffect = new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 100, speedAmplifier);
        org.bukkit.potion.PotionEffect jumpEffect = new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 100, 0);

        double healAmount = 0.0;
        if (lvl >= 3) {
            healAmount = isWater ? 4.0 : 2.0;
        }

        for (Player p : run.getOnlinePlayers()) {
            if (!run.isPlayerSpectator(p)) {
                p.addPotionEffect(speedEffect);
                p.addPotionEffect(jumpEffect);
                if (healAmount > 0.0) {
                    double maxHP = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    double curHP = p.getHealth();
                    p.setHealth(Math.min(maxHP, curHP + healAmount));
                    p.getWorld().spawnParticle(org.bukkit.Particle.HEART, p.getLocation().add(0, 1.5, 0), 5, 0.2, 0.2, 0.2, 0.0);
                }
            }
        }

        double soundStacks = (lvl >= 3) ? 20.0 : 10.0;
        for (org.bukkit.entity.Entity ent : player.getNearbyEntities(6.0, 6.0, 6.0)) {
            if (ent instanceof LivingEntity && !ent.getUniqueId().equals(player.getUniqueId())) {
                LivingEntity victim = (LivingEntity) ent;

                org.bukkit.util.Vector knockDir = victim.getLocation().toVector().subtract(player.getLocation().toVector());
                if (knockDir.lengthSquared() > 0) {
                    knockDir.normalize();
                } else {
                    knockDir = new org.bukkit.util.Vector(0, 0, 1);
                }
                knockDir.setY(0.35);
                victim.setVelocity(knockDir.multiply(1.2));

                Abilities.Bending.SoundAbility.HandleDamage(player, victim, soundStacks);
            }
        }

        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation().add(0, 1.0, 0), 30, 1.5, 0.5, 1.5, 0.1);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.5f);
        player.sendMessage(org.bukkit.ChatColor.GREEN + "[Sierp Wiatru] Wyzwolono fale powietrza!");
    }

    public void applyUniversalStat(DungeonPlayerStats stats, String statName, double val, Player player) {
        String name = statName.toUpperCase();
        if (name.equals("HP")) {
            stats.addHpBoost(val);
            stats.applyStatsToPlayer(player);
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.RED + "Zwiekszono statystyke: +" + val + " Maksymalnego HP!");
        } else if (name.equals("DEF")) {
            stats.addDefBoost(val);
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.BLUE + "Zwiekszono statystyke: +" + val + " Obrony (DEF)!");
        } else if (name.equals("DMG")) {
            stats.addDmgMultiplier(val);
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.GOLD + "Zwiekszono statystyke: +" + (int)(val * 100) + "% Zadawanych Obrazen!");
        } else if (name.equals("SPEED")) {
            stats.addSpeedBoost(val);
            stats.applyStatsToPlayer(player);
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.YELLOW + "Zwiekszono statystyke: Predkosc Ruchu!");
        } else if (name.equals("P_CRIT_RATE")) {
            stats.addPCritRate(val);
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.RED + "Zwiekszono statystyke: +" + (int)(val * 100) + "% Szansy na Fizyczny Kryt!");
        } else if (name.equals("P_CRIT_DMG")) {
            stats.addPCritDmg(val);
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.RED + "Zwiekszono statystyke: +" + (int)(val * 100) + "% Mnoznika Fizycznego Kryta!");
        } else if (name.equals("M_CRIT_RATE")) {
            stats.addMCritRate(val);
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.RED + "Zwiekszono statystyke: +" + (int)(val * 100) + "% Szansy na Magiczny Kryt!");
        } else if (name.equals("M_CRIT_DMG")) {
            stats.addMCritDmg(val);
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.RED + "Zwiekszono statystyke: +" + (int)(val * 100) + "% Mnoznika Magicznego Kryta!");
        } else if (name.equals("REGEN")) {
            stats.addRegenLevel((int) val);
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.GREEN + "Zwiekszono statystyke: +" + (int) val + " poziomu Regeneracji!");
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        java.io.File backupsFolder = new java.io.File(AmonPackPlugin.plugin.getDataFolder(), "backups");
        java.io.File file = new java.io.File(backupsFolder, player.getUniqueId().toString() + ".yml");
        if (file.exists()) {
            DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            DungeonInstance.clearPlayerTemporaryStatsAndAbilities(player);
            org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(20.0);
                if (player.getHealth() > 20.0) {
                    player.setHealth(20.0);
                }
            }
            org.bukkit.attribute.AttributeInstance speedAttr = player.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(0.2);
            }
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            player.sendMessage(org.bukkit.ChatColor.RED + "Twój survivalowy ekwipunek został bezpiecznie przywrócony!");
        }
    }

    @EventHandler
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        World world = event.getEntity().getWorld();
        DungeonInstance run = activeInstances.get(world);
        if (run != null) {
            if (!(event.getEntity() instanceof Player)) {
                run.registerSpawnedMob(event.getEntity().getUniqueId());
            }
        }
    }

    public void removeVirtualArrows(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && stack.getType() == Material.ARROW && stack.hasItemMeta()) {
                org.bukkit.persistence.PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
                org.bukkit.NamespacedKey vKey = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_virtual_arrow");
                if (pdc.has(vKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
        player.updateInventory();
    }

    public void triggerMaiDaggersThrow(Player player, DungeonPlayerStats stats, int level, DungeonInstance run) {
        World world = player.getWorld();
        ItemStack held = player.getInventory().getItemInMainHand();
        ItemStack daggersStack = held != null ? held.clone() : new ItemStack(Material.GOLDEN_SWORD);
        daggersStack.setAmount(1);

        org.bukkit.configuration.file.FileConfiguration cfg = AmonPackPlugin.getDungeonConfig();
        final double baseDmg = (cfg != null) ? cfg.getDouble("blessings.MAI_DAGGERS.base-damage", 1.0) : 1.0;

        com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
        final boolean isFire = bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Fire"));
        final boolean isWater = bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Water"));

        org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().clone();
        org.bukkit.util.Vector right = new org.bukkit.util.Vector(-dir.getZ(), 0.0, dir.getX()).normalize();

        List<org.bukkit.util.Vector> velocities = new ArrayList<>();
        if (level >= 3) {
            velocities.add(dir.clone().add(right.clone().multiply(-0.15)).normalize().multiply(0.8));
            velocities.add(dir.clone().normalize().multiply(0.8));
            velocities.add(dir.clone().add(right.clone().multiply(0.15)).normalize().multiply(0.8));
        } else {
            velocities.add(dir.clone().add(right.clone().multiply(-0.15)).normalize().multiply(0.8));
            velocities.add(dir.clone().add(right.clone().multiply(0.15)).normalize().multiply(0.8));
        }

        world.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0f, 1.5f);

        for (org.bukkit.util.Vector vel : velocities) {
            final Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
            final org.bukkit.entity.Item thrownItem = world.dropItem(startLoc, daggersStack);
            thrownItem.setPickupDelay(32767);
            thrownItem.setGravity(false);
            thrownItem.setVelocity(vel);

            final org.bukkit.util.Vector finalVel = vel.clone();

            new BukkitRunnable() {
                int ticks = 0;
                Location currentLoc = startLoc.clone();
                org.bukkit.util.Vector velocity = finalVel.clone();

                @Override
                public void run() {
                    if (ticks > 100 || thrownItem.isDead() || run.isFinished()) {
                        thrownItem.remove();
                        cancel();
                        return;
                    }

                    currentLoc.add(velocity);
                    velocity.setY(velocity.getY() - 0.02);

                    thrownItem.teleport(currentLoc);
                    thrownItem.setVelocity(velocity);

                    world.spawnParticle(Particle.CRIT, currentLoc, 3, 0.05, 0.05, 0.05, 0.01);

                    if (currentLoc.getBlock().getType().isSolid()) {
                        world.playSound(currentLoc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.2f);
                        world.spawnParticle(Particle.BLOCK, currentLoc, 8, 0.1, 0.1, 0.1, Material.GOLD_BLOCK.createBlockData());
                        thrownItem.remove();
                        cancel();
                        return;
                    }

                    for (org.bukkit.entity.Entity entity : thrownItem.getNearbyEntities(0.6, 0.6, 0.6)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity living = (LivingEntity) entity;
                            double multi = (level >= 2) ? 2.0 : 1.5;
                            if (level >= 2 && isWater) {
                                multi = 1.0;
                            }
                            double finalDmg = baseDmg * multi;
                            living.damage(finalDmg, player);

                            living.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 80, 1));
                            living.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 80, 0));

                            long durMs = (level >= 3) ? 7000L : 5000L;
                            boolean waterMark = (level >= 2 && isWater);
                            run.markEnemy(living, player, durMs, waterMark);

                            if (isFire) {
                                int fireSec = (level >= 3) ? 7 : 5;
                                living.setFireTicks(fireSec * 20);
                            }

                            world.playSound(currentLoc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.4f);
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

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        DungeonInstance run = activeInstances.get(event.getBlock().getWorld());
        if (run != null) {
            if (run.isBuildingAllowed() || AmonPackPlugin.BuildingOnArenas) {
                return;
            }
            if (run.handleZoneBlockBreak(event.getPlayer(), event.getBlock())) {
                event.setDropItems(false);
                for (ItemStack drop : event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand())) {
                    java.util.Map<Integer, ItemStack> leftover = event.getPlayer().getInventory().addItem(drop);
                    for (ItemStack rem : leftover.values()) {
                        event.getBlock().getWorld().dropItemNaturally(event.getPlayer().getLocation(), rem);
                    }
                }
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        DungeonInstance run = activeInstances.get(event.getBlock().getWorld());
        if (run != null) {
            if (run.isBuildingAllowed() || AmonPackPlugin.BuildingOnArenas) {
                return;
            }
            if (run.handleZoneBlockPlace(event.getPlayer(), event.getBlock())) {
                return;
            }
            event.setCancelled(true);
        }
    }

    public void applyConsumableEffect(Player player, DungeonCustomItem customItem, DungeonInstance run) {
        String effect = customItem.getEffectType();
        double value = customItem.getEffectValue();
        int duration = customItem.getDuration();
        if (effect == null || effect.isEmpty()) return;

        DungeonPlayerStats stats = run.getPlayerStats(player);

        if ("heal".equalsIgnoreCase(effect)) {
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(maxHealth, player.getHealth() + value));
            player.sendMessage(ChatColor.GREEN + "[Dungeons] Uleczono o " + value + " HP!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        } else if ("speed".equalsIgnoreCase(effect)) {
            if (stats != null) {
                stats.addSpeedBoost(value);
                stats.applyStatsToPlayer(player);
                player.sendMessage(ChatColor.GREEN + "[Dungeons] Aktywowano Speed Boost +" + value + " na " + duration + " sekund!");
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (run.getOnlinePlayers().contains(player)) {
                            DungeonPlayerStats s = run.getPlayerStats(player);
                            if (s != null) {
                                s.addSpeedBoost(-value);
                                s.applyStatsToPlayer(player);
                                player.sendMessage(ChatColor.RED + "[Dungeons] Efekt Speed Boost wygasl!");
                            }
                        }
                    }
                }.runTaskLater(AmonPackPlugin.plugin, duration * 20L);
            }
        } else if ("jumpboost".equalsIgnoreCase(effect) || "jump_boost".equalsIgnoreCase(effect)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, duration * 20, (int) value - 1));
            player.sendMessage(ChatColor.GREEN + "[Dungeons] Aktywowano Jump Boost " + (int) value + " na " + duration + " sekund!");
        } else if ("damage_boost".equalsIgnoreCase(effect) || "strength".equalsIgnoreCase(effect)) {
            if (stats != null) {
                stats.addDmgMultiplier(value);
                player.sendMessage(ChatColor.GREEN + "[Dungeons] Aktywowano Damage Boost +" + (int)(value * 100) + "% na " + duration + " sekund!");
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (run.getOnlinePlayers().contains(player)) {
                            DungeonPlayerStats s = run.getPlayerStats(player);
                            if (s != null) {
                                s.addDmgMultiplier(-value);
                                player.sendMessage(ChatColor.RED + "[Dungeons] Efekt Damage Boost wygasl!");
                            }
                        }
                    }
                }.runTaskLater(AmonPackPlugin.plugin, duration * 20L);
            }
        } else if ("hp_boost".equalsIgnoreCase(effect) || "health_boost".equalsIgnoreCase(effect)) {
            if (stats != null) {
                stats.addHpBoost(value);
                stats.applyStatsToPlayer(player);
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                player.setHealth(Math.min(maxHealth, player.getHealth() + value));
                player.sendMessage(ChatColor.GREEN + "[Dungeons] Aktywowano HP Boost +" + value + " na " + duration + " sekund!");
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (run.getOnlinePlayers().contains(player)) {
                            DungeonPlayerStats s = run.getPlayerStats(player);
                            if (s != null) {
                                s.addHpBoost(-value);
                                s.applyStatsToPlayer(player);
                                player.sendMessage(ChatColor.RED + "[Dungeons] Efekt HP Boost wygasl!");
                            }
                        }
                    }
                }.runTaskLater(AmonPackPlugin.plugin, duration * 20L);
            }
        } else if ("crit_rate_boost".equalsIgnoreCase(effect)) {
            if (stats != null) {
                stats.addPCritRate(value);
                stats.addMCritRate(value);
                player.sendMessage(ChatColor.GREEN + "[Dungeons] Zwiekszono szanse na krytyk o " + (int)(value * 100) + "% na " + duration + " sekund!");
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (run.getOnlinePlayers().contains(player)) {
                            DungeonPlayerStats s = run.getPlayerStats(player);
                            if (s != null) {
                                s.addPCritRate(-value);
                                s.addMCritRate(-value);
                                player.sendMessage(ChatColor.RED + "[Dungeons] Efekt szansy na krytyk wygasl!");
                            }
                        }
                    }
                }.runTaskLater(AmonPackPlugin.plugin, duration * 20L);
            }
        }
    }

    @EventHandler
    public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        DungeonInstance run = activeInstances.get(player.getWorld());
        if (run == null) return;
        if (run.isPlayerSpectator(player)) return;

        if (event.isSneaking()) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null) return;
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            org.bukkit.persistence.PersistentDataContainer pdc = meta != null ? meta.getPersistentDataContainer() : null;
            if (pdc != null) {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_item_id");
                if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                    String itemId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                    DungeonCustomItem customItem = run.getTemplate().getCustomItems().get(itemId);
                    if (customItem != null && ("consumable".equalsIgnoreCase(customItem.getType()) || "consumables".equalsIgnoreCase(customItem.getType()))) {
                        if ("SNEAK".equalsIgnoreCase(customItem.getUseMode()) || "SHIFT".equalsIgnoreCase(customItem.getUseMode())) {
                            dungeonConsumableChargingPlayers.put(player.getUniqueId(), System.currentTimeMillis());
                            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, new Runnable() {
                                @Override
                                public void run() {
                                    if (!player.isSneaking() || !player.isOnline()) {
                                        cancelCharging(player);
                                        return;
                                    }
                                    ItemStack currentItem = player.getInventory().getItemInMainHand();
                                    if (currentItem == null) {
                                        cancelCharging(player);
                                        return;
                                    }
                                    org.bukkit.inventory.meta.ItemMeta currentMeta = currentItem.getItemMeta();
                                    org.bukkit.persistence.PersistentDataContainer cPdc = currentMeta != null ? currentMeta.getPersistentDataContainer() : null;
                                    if (cPdc == null || !cPdc.has(key, org.bukkit.persistence.PersistentDataType.STRING) || !itemId.equalsIgnoreCase(cPdc.get(key, org.bukkit.persistence.PersistentDataType.STRING))) {
                                        cancelCharging(player);
                                        return;
                                    }
                                    long chargeTime = 2500;
                                    long startTime = dungeonConsumableChargingPlayers.get(player.getUniqueId());
                                    long elapsed = System.currentTimeMillis() - startTime;
                                    if (elapsed >= chargeTime) {
                                        player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0);
                                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f);
                                    } else {
                                        player.spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0);
                                    }
                                }
                            }, 0L, 5L);
                            dungeonConsumableChargingTasks.put(player.getUniqueId(), taskId);
                        }
                    }
                }
            }
        } else {
            if (dungeonConsumableChargingPlayers.containsKey(player.getUniqueId())) {
                long startTime = dungeonConsumableChargingPlayers.get(player.getUniqueId());
                int taskId = dungeonConsumableChargingTasks.remove(player.getUniqueId());
                Bukkit.getScheduler().cancelTask(taskId);
                dungeonConsumableChargingPlayers.remove(player.getUniqueId());

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    org.bukkit.persistence.PersistentDataContainer pdc = meta != null ? meta.getPersistentDataContainer() : null;
                    if (pdc != null) {
                        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_item_id");
                        if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                            String itemId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                            DungeonCustomItem customItem = run.getTemplate().getCustomItems().get(itemId);
                            if (customItem != null && ("consumable".equalsIgnoreCase(customItem.getType()) || "consumables".equalsIgnoreCase(customItem.getType()))) {
                                long chargeTime = 2500;
                                if (System.currentTimeMillis() - startTime >= chargeTime) {
                                    int newAmt = item.getAmount() - 1;
                                    if (newAmt > 0) {
                                        item.setAmount(newAmt);
                                    } else {
                                        player.getInventory().setItemInMainHand(null);
                                    }
                                    applyConsumableEffect(player, customItem, run);
                                } else {
                                    player.sendMessage(ChatColor.RED + "Rytual przerwany!");
                                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void cancelCharging(Player player) {
        if (dungeonConsumableChargingPlayers.containsKey(player.getUniqueId())) {
            if (dungeonConsumableChargingTasks.containsKey(player.getUniqueId())) {
                int taskId = dungeonConsumableChargingTasks.remove(player.getUniqueId());
                Bukkit.getScheduler().cancelTask(taskId);
            }
            dungeonConsumableChargingPlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onItemHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        cancelCharging(event.getPlayer());
    }

    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        cancelCharging(event.getPlayer());
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cancelCharging(event.getPlayer());
    }
}
