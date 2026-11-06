package RPG.BattleRoyale.GroundLoot;

import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.BattleRoyaleArena;
import RPG.BattleRoyale.Loot.BattleRoyaleLootManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroundLootManager {

    public static final NamespacedKey KEY_GROUND_LOOT = new NamespacedKey(AmonPackPlugin.plugin, "br_ground_loot");
    public static final NamespacedKey KEY_LOOT_CAT = new NamespacedKey(AmonPackPlugin.plugin, "br_ground_loot_cat");

    public static class GroundLootPoint {
        private final Location location;
        private final String category; // np. GUNS, AMMO, MEDICAL, FOOD, MELEE, MAGIC, RANDOM

        public GroundLootPoint(Location location, String category) {
            this.location = location;
            this.category = category;
        }

        public Location getLocation() { return location; }
        public String getCategory() { return category; }
    }

    private final List<GroundLootPoint> configuredPoints = new ArrayList<>();
    private final Map<UUID, GroundLootPoint> activeFrames = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Rejestruje nowy punkt ground loot i zapisuje w pamięci.
     */
    public void addPoint(Location location, String category) {
        Location blockLoc = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        configuredPoints.removeIf(p -> p.getLocation().equals(blockLoc));
        configuredPoints.add(new GroundLootPoint(blockLoc, category.toUpperCase()));
    }

    public boolean removePoint(Location location) {
        Location blockLoc = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return configuredPoints.removeIf(p -> p.getLocation().equals(blockLoc) || p.getLocation().distanceSquared(location) < 1.5);
    }

    public List<GroundLootPoint> getConfiguredPoints() {
        return Collections.unmodifiableList(configuredPoints);
    }

    /**
     * Aktywuje i spawnuje przedmioty na ziemi przy starcie meczu.
     */
    public void spawnGroundLoot(World world, BattleRoyaleArena arena, BattleRoyaleLootManager lootManager) {
        cleanAllFrames(world);
        activeFrames.clear();

        for (GroundLootPoint point : configuredPoints) {
            Location loc = point.getLocation();
            Location spawnLoc = new Location(world, loc.getX() + 0.5, loc.getY() + 1.0, loc.getZ() + 0.5);

            // Sprawdź czy blok poniżej istnieje
            Block blockBelow = spawnLoc.clone().subtract(0, 1, 0).getBlock();
            if (blockBelow.getType().isAir()) {
                spawnLoc.subtract(0, 1, 0);
            }

            try {
                // Spawnowanie niewidzialnego ItemFrame leżącego płasko na podłodze
                ItemFrame frame = world.spawn(spawnLoc, ItemFrame.class, f -> {
                    f.setVisible(false);
                    f.setFixed(true);
                    f.setFacingDirection(BlockFace.UP, true);
                    f.getPersistentDataContainer().set(KEY_GROUND_LOOT, PersistentDataType.BYTE, (byte) 1);
                    f.getPersistentDataContainer().set(KEY_LOOT_CAT, PersistentDataType.STRING, point.getCategory());
                });

                // Generowanie losowego przedmiotu
                ItemStack item = generateItemForCategory(point.getCategory(), arena, lootManager);
                if (item != null) {
                    frame.setItem(item, false);
                    frame.setRotation(Rotation.values()[random.nextInt(Rotation.values().length)]);
                    activeFrames.put(frame.getUniqueId(), point);
                }
            } catch (Exception e) {
                // Fallback na ścianę jeśli UP nie działa w tym miejscu
                try {
                    ItemFrame frame = world.spawn(spawnLoc, ItemFrame.class, f -> {
                        f.setVisible(false);
                        f.setFixed(true);
                        f.getPersistentDataContainer().set(KEY_GROUND_LOOT, PersistentDataType.BYTE, (byte) 1);
                    });
                    ItemStack item = generateItemForCategory(point.getCategory(), arena, lootManager);
                    if (item != null) {
                        frame.setItem(item, false);
                        activeFrames.put(frame.getUniqueId(), point);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Wywoływane gdy gracz podnosi przedmiot z ziemi (kliknięcie lub podejście).
     */
    public boolean handlePickup(ItemFrame frame, Player player) {
        if (frame == null || !frame.isValid()) return false;
        if (!activeFrames.containsKey(frame.getUniqueId()) &&
                !frame.getPersistentDataContainer().has(KEY_GROUND_LOOT, PersistentDataType.BYTE)) {
            return false;
        }

        ItemStack item = frame.getItem();
        if (item != null && !item.getType().isAir()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            if (leftover.isEmpty()) {
                // Całość zebrana
                frame.setItem(null, false);
                activeFrames.remove(frame.getUniqueId());
                player.playSound(frame.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                frame.getWorld().spawnParticle(Particle.CRIT, frame.getLocation().add(0, 0.2, 0), 6, 0.2, 0.1, 0.2, 0.1);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "[BattleRoyale] Twój ekwipunek jest pełny!");
            }
        }
        return false;
    }

    /**
     * Sprawdza proximity graczy do leżących na ziemi przedmiotów (automatyczne podnoszenie).
     */
    public void checkProximityPickups(List<Player> players) {
        if (activeFrames.isEmpty() || players.isEmpty()) return;

        for (Map.Entry<UUID, GroundLootPoint> entry : new HashMap<>(activeFrames).entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof ItemFrame frame) || !frame.isValid()) {
                activeFrames.remove(entry.getKey());
                continue;
            }

            ItemStack is = frame.getItem();
            if (is == null || is.getType().isAir()) {
                activeFrames.remove(entry.getKey());
                continue;
            }

            Location frameLoc = frame.getLocation();
            for (Player p : players) {
                if (p != null && p.isOnline() && !p.isDead()) {
                    if (p.getWorld().equals(frameLoc.getWorld()) && p.getLocation().distanceSquared(frameLoc) < 1.8) {
                        handlePickup(frame, p);
                        break;
                    }
                }
            }
        }
    }

    private ItemStack generateItemForCategory(String category, BattleRoyaleArena arena, BattleRoyaleLootManager lootManager) {
        BattleRoyaleLootManager.LootCategory cat;
        switch (category.toUpperCase()) {
            case "GUNS": cat = BattleRoyaleLootManager.LootCategory.GUN; break;
            case "AMMO": cat = BattleRoyaleLootManager.LootCategory.AMMO; break;
            case "MEDICAL": cat = random.nextBoolean() ? BattleRoyaleLootManager.LootCategory.BANDAGE : BattleRoyaleLootManager.LootCategory.CURE; break;
            case "FOOD": cat = BattleRoyaleLootManager.LootCategory.FOOD; break;
            case "MELEE": cat = BattleRoyaleLootManager.LootCategory.VANILLA; break;
            case "UPGRADE": cat = BattleRoyaleLootManager.LootCategory.UPGRADE_KIT; break;
            default:
                BattleRoyaleLootManager.LootCategory[] all = BattleRoyaleLootManager.LootCategory.values();
                cat = all[random.nextInt(all.length)];
                break;
        }

        BattleRoyaleLootManager.LootEntry entry = new BattleRoyaleLootManager.LootEntry(cat, 1.0, 1, 1, "");
        if (cat == BattleRoyaleLootManager.LootCategory.AMMO) {
            entry = new BattleRoyaleLootManager.LootEntry(cat, 1.0, 6, 16, "lead_bullet");
        } else if (cat == BattleRoyaleLootManager.LootCategory.GUN) {
            entry = new BattleRoyaleLootManager.LootEntry(cat, 1.0, 1, 1, "flintlock_pistol");
        } else if (cat == BattleRoyaleLootManager.LootCategory.BANDAGE) {
            entry = new BattleRoyaleLootManager.LootEntry(cat, 1.0, 1, 3, "");
        }
        return lootManager.generateItem(entry, arena);
    }

    public void cleanAllFrames(World world) {
        if (world == null) return;
        for (Entity e : world.getEntitiesByClass(ItemFrame.class)) {
            if (e.getPersistentDataContainer().has(KEY_GROUND_LOOT, PersistentDataType.BYTE)) {
                e.remove();
            }
        }
        activeFrames.clear();
    }

    public static ItemStack createAdminWand() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Różdżka Budowy Mapy BR");
            meta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "PPM na blok: " + ChatColor.WHITE + "Tworzy leżący na ziemi Ground Loot",
                    ChatColor.RED + "LPM na blok: " + ChatColor.WHITE + "Usuwa Ground Loot / Skrzynię tematyczną",
                    ChatColor.GRAY + "Kucnij + PPM: " + ChatColor.AQUA + "Zmienia kategorię lootu"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void loadFromConfig(FileConfiguration config) {
        configuredPoints.clear();
        ConfigurationSection sec = config.getConfigurationSection("arena.ground-loot");
        if (sec == null) return;

        String worldName = config.getString("arena.world-name", "hungergames_arena");
        World w = Bukkit.getWorld(worldName);

        for (String key : sec.getKeys(false)) {
            double x = sec.getDouble(key + ".x");
            double y = sec.getDouble(key + ".y");
            double z = sec.getDouble(key + ".z");
            String cat = sec.getString(key + ".category", "RANDOM");
            Location loc = new Location(w, x, y, z);
            configuredPoints.add(new GroundLootPoint(loc, cat));
        }
    }

    public void saveToConfig(FileConfiguration config, File file) {
        config.set("arena.ground-loot", null);
        int idx = 1;
        for (GroundLootPoint p : configuredPoints) {
            String path = "arena.ground-loot.point_" + idx;
            config.set(path + ".x", p.getLocation().getX());
            config.set(path + ".y", p.getLocation().getY());
            config.set(path + ".z", p.getLocation().getZ());
            config.set(path + ".category", p.getCategory());
            idx++;
        }
        try {
            config.save(file);
        } catch (IOException e) {
            AmonPackPlugin.plugin.getLogger().warning("[BattleRoyale] Błąd zapisu ground-loot: " + e.getMessage());
        }
    }

    public void shiftPoints(int dx, int dy, int dz, World targetWorld) {
        List<GroundLootPoint> shifted = new ArrayList<>();
        for (GroundLootPoint p : configuredPoints) {
            Location old = p.getLocation();
            World w = targetWorld != null ? targetWorld : old.getWorld();
            Location newLoc = new Location(w, old.getBlockX() + dx, old.getBlockY() + dy, old.getBlockZ() + dz);
            shifted.add(new GroundLootPoint(newLoc, p.getCategory()));
        }
        configuredPoints.clear();
        configuredPoints.addAll(shifted);
    }
}
