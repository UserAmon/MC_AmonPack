package RPG.BattleRoyale.Keys;

import Plugin.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zarządza systemem kluczy i zamkniętych drzwi żelaznych (IRON_DOOR).
 * Obsługuje otwieranie drzwi za pomocą kluczy, konfigurację drzwi oraz lokalizację dla nawigacji cząsteczkowej.
 */
public class KeyManager {

    public static final NamespacedKey KEY_KEY_TYPE = new NamespacedKey(AmonPackPlugin.plugin, "br_key_type");

    // Zarejestrowane drzwi: Lokalizacja dolnego bloku drzwi -> KeyType
    private final Map<Location, KeyType> lockedDoors = new ConcurrentHashMap<>();
    // Drzwi otwarte w bieżącym meczu
    private final Set<Location> unlockedDoors = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private boolean consumeOnUse = false;

    public void loadFromConfig(FileConfiguration config) {
        lockedDoors.clear();
        unlockedDoors.clear();
        consumeOnUse = config.getBoolean("keys.consume-on-use", false);

        List<Map<?, ?>> list = config.getMapList("keys.locked-doors");
        if (list != null) {
            for (Map<?, ?> entry : list) {
                String typeStr = (String) entry.get("key");
                KeyType type = KeyType.fromString(typeStr);
                if (type == null) continue;

                String worldName = (String) entry.get("world");
                double x = ((Number) entry.get("x")).doubleValue();
                double y = ((Number) entry.get("y")).doubleValue();
                double z = ((Number) entry.get("z")).doubleValue();

                org.bukkit.World w = org.bukkit.Bukkit.getWorld(worldName != null ? worldName : "hungergames_arena");
                Location loc = new Location(w, x, y, z);
                lockedDoors.put(normalizeDoorLocation(loc), type);
            }
        }
    }

    public ItemStack createKey(KeyType type) {
        if (type == null) type = KeyType.POLICE_STATION;

        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getColor() + "" + ChatColor.BOLD + "🔑 " + type.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Klucz dostępu do: " + type.getColor() + type.getLocationName());
            lore.add(ChatColor.DARK_GRAY + "Otwiera zamknięte żelazne drzwi w budynku.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Trzymaj w dłoni, aby cząsteczki na ziemi");
            lore.add(ChatColor.YELLOW + "zaprowadziły cię do wejścia!");
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(KEY_KEY_TYPE, PersistentDataType.STRING, type.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_KEY_TYPE, PersistentDataType.STRING);
    }

    public KeyType getKeyType(ItemStack item) {
        if (!isKey(item)) return null;
        String typeStr = item.getItemMeta().getPersistentDataContainer().get(KEY_KEY_TYPE, PersistentDataType.STRING);
        return KeyType.fromString(typeStr);
    }

    public void registerDoor(Location loc, KeyType type) {
        if (loc == null || type == null) return;
        lockedDoors.put(normalizeDoorLocation(loc), type);
    }

    public void removeDoor(Location loc) {
        if (loc == null) return;
        lockedDoors.remove(normalizeDoorLocation(loc));
        unlockedDoors.remove(normalizeDoorLocation(loc));
    }

    public boolean isLockedDoor(Block block) {
        if (block == null || block.getType() != Material.IRON_DOOR) return false;
        Location norm = normalizeDoorBlock(block);
        return lockedDoors.containsKey(norm) && !unlockedDoors.contains(norm);
    }

    public KeyType getRequiredKey(Block block) {
        if (block == null) return null;
        Location norm = normalizeDoorBlock(block);
        return lockedDoors.get(norm);
    }

    /**
     * Obsługuje kliknięcie PPM na żelazne drzwi.
     * Zwraca true jeśli zdarzenie powinno zostać anulowane/obsłużone.
     */
    public boolean handleDoorInteract(Player player, Block doorBlock, ItemStack handItem) {
        if (doorBlock == null || doorBlock.getType() != Material.IRON_DOOR) return false;

        Location norm = normalizeDoorBlock(doorBlock);
        KeyType required = lockedDoors.get(norm);

        // Jeśli drzwi nie są zarejestrowane jako zamknięte na klucz
        if (required == null) return false;

        // Jeśli zostały już odblokowane w tym meczu
        if (unlockedDoors.contains(norm)) {
            toggleDoorState(doorBlock);
            return true;
        }

        // Gracz klika drzwiami
        KeyType heldKey = getKeyType(handItem);
        if (heldKey == required) {
            // Sukces: otwieramy drzwi!
            unlockedDoors.add(norm);
            toggleDoorState(doorBlock);

            player.sendMessage(ChatColor.GREEN + "[Klucze] 🔓 Otworzono: " + required.getColor() + required.getLocationName() + ChatColor.GREEN + "!");
            player.playSound(doorBlock.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 1.0f);
            player.playSound(doorBlock.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.8f, 1.5f);

            if (consumeOnUse && handItem != null) {
                handItem.setAmount(handItem.getAmount() - 1);
            }
            return true;
        } else {
            // Porażka: brak klucza lub zły klucz
            player.sendMessage(ChatColor.RED + "[Klucze] 🔒 Te drzwi są zamknięte na klucz! Wymagany: "
                    + required.getColor() + required.getDisplayName());
            player.playSound(doorBlock.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 0.8f);
            return true;
        }
    }

    private void toggleDoorState(Block doorBlock) {
        if (doorBlock.getBlockData() instanceof Door door) {
            door.setOpen(!door.isOpen());
            doorBlock.setBlockData(door);
        }
    }

    /**
     * Zwraca lokalizację najbliższych ZAMKNIĘTYCH drzwi dla danego typu klucza.
     */
    public Location findNearestLockedDoor(Location fromLoc, KeyType type) {
        if (fromLoc == null || type == null || lockedDoors.isEmpty()) return null;

        Location best = null;
        double minDstSq = Double.MAX_VALUE;

        for (Map.Entry<Location, KeyType> entry : lockedDoors.entrySet()) {
            Location dLoc = entry.getKey();
            if (entry.getValue() == type && !unlockedDoors.contains(dLoc)) {
                if (dLoc.getWorld() != null && dLoc.getWorld().equals(fromLoc.getWorld())) {
                    double dstSq = dLoc.distanceSquared(fromLoc);
                    if (dstSq < minDstSq) {
                        minDstSq = dstSq;
                        best = dLoc;
                    }
                }
            }
        }
        return best;
    }

    private Location normalizeDoorLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private Location normalizeDoorBlock(Block block) {
        if (block.getBlockData() instanceof Bisected bisected) {
            if (bisected.getHalf() == Bisected.Half.TOP) {
                block = block.getRelative(BlockFace.DOWN);
            }
        }
        return normalizeDoorLocation(block.getLocation());
    }

    public Map<Location, KeyType> getLockedDoors() {
        return Collections.unmodifiableMap(lockedDoors);
    }

    public void saveToConfig(FileConfiguration config, File file) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Location, KeyType> entry : lockedDoors.entrySet()) {
            Location loc = entry.getKey();
            Map<String, Object> map = new HashMap<>();
            map.put("world", loc.getWorld() != null ? loc.getWorld().getName() : "hungergames_arena");
            map.put("x", loc.getBlockX());
            map.put("y", loc.getBlockY());
            map.put("z", loc.getBlockZ());
            map.put("key", entry.getValue().name());
            list.add(map);
        }
        config.set("keys.locked-doors", list);
        try {
            config.save(file);
        } catch (Exception e) {
            AmonPackPlugin.plugin.getLogger().warning("[BattleRoyale] Błąd zapisu zamkniętych drzwi: " + e.getMessage());
        }
    }

    public void shiftDoors(int dx, int dy, int dz, org.bukkit.World targetWorld) {
        Map<Location, KeyType> shifted = new ConcurrentHashMap<>();
        for (Map.Entry<Location, KeyType> entry : lockedDoors.entrySet()) {
            Location old = entry.getKey();
            org.bukkit.World w = targetWorld != null ? targetWorld : old.getWorld();
            Location newLoc = new Location(w, old.getBlockX() + dx, old.getBlockY() + dy, old.getBlockZ() + dz);
            shifted.put(newLoc, entry.getValue());
        }
        lockedDoors.clear();
        lockedDoors.putAll(shifted);
    }

    public void cleanup() {
        unlockedDoors.clear();
    }
}
