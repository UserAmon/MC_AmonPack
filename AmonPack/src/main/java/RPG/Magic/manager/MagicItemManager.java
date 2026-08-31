package RPG.Magic.manager;

import Plugin.AmonPackPlugin;
import RPG.Magic.model.SpellElement;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class MagicItemManager {

    public enum MagicItemType {
        TOME,
        WAND,
        STAFF
    }

    public static final NamespacedKey MAGIC_ITEM_ID_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_item_id");
    public static final NamespacedKey TOME_LEVEL_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_tome_level");
    public static final NamespacedKey TOME_KILLS_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_tome_kills");
    public static final NamespacedKey TOME_UPGRADES_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_tome_upgrades");
    public static final NamespacedKey SPELL_PRIMARY_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_primary_spell");
    public static final NamespacedKey SPELL_SECONDARY_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_secondary_spell");

    public static String getMagicItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        Material mat = item.getType();
        String matName = mat.name();
        if (matName.contains("HELMET") || matName.contains("CHESTPLATE") || matName.contains("LEGGINGS") || matName.contains("BOOTS")
                || matName.contains("SWORD") || matName.contains("PICKAXE") || matName.contains("AXE") || matName.contains("SHOVEL") || matName.contains("HOE")) {
            return null;
        }

        if (CustomContent.Guns.GunData.isGun(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(MAGIC_ITEM_ID_KEY, PersistentDataType.STRING);
        if (id != null && !id.isEmpty()) return id.toLowerCase(Locale.ROOT);

        // Odczyt z weapon_id jeśli był ustawiony
        NamespacedKey wKey = new NamespacedKey(AmonPackPlugin.plugin, "weapon_id");
        String wId = pdc.get(wKey, PersistentDataType.STRING);
        if (wId != null && !wId.isEmpty()) {
            String lower = wId.toLowerCase(Locale.ROOT);
            if (lower.startsWith("tome_") || lower.startsWith("wand_") || lower.startsWith("staff_")) {
                return lower;
            }
        }

        // Fallback po CustomModelData / DisplayName
        if (meta.hasCustomModelData()) {
            int cmd = meta.getCustomModelData();
            if (cmd == 20001 || cmd == 10010) return "tome_fire";
            if (cmd == 20002) return "wand_fen";
            if (cmd == 20003) return "staff_lightning";
            if (cmd == 20004) return "wand_water";
        }
        if (meta.hasDisplayName()) {
            String name = meta.getDisplayName().toLowerCase(Locale.ROOT);
            if (name.contains("tom ognia") || name.contains("tome of fire")) return "tome_fire";
            if (name.contains("różdżka fenów") || name.contains("rozdzka fenow") || name.contains("wand of air")) return "wand_fen";
            if (name.contains("laska błyskawic") || name.contains("laska blyskawic") || name.contains("staff of lightning")) return "staff_lightning";
            if (name.contains("różdżka wody") || name.contains("rozdzka wody") || name.contains("wand of water")) return "wand_water";
        }
        return null;
    }

    public static MagicItemType getMagicItemType(ItemStack item) {
        String id = getMagicItemId(item);
        if (id != null) {
            if (id.startsWith("staff_") || id.contains("staff") || id.contains("laska")) return MagicItemType.STAFF;
            if (id.startsWith("wand_") || id.contains("wand") || id.contains("różdżka")) return MagicItemType.WAND;
            if (id.startsWith("tome_") || id.contains("tome") || id.contains("tom") || id.contains("księga")) return MagicItemType.TOME;
        }
        if (item != null) {
            if (item.getType() == Material.BOW || item.getType() == Material.CROSSBOW) return MagicItemType.STAFF;
            if (item.getType() == Material.STICK || item.getType() == Material.PRISMARINE_SHARD) return MagicItemType.WAND;
            if (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK) return MagicItemType.TOME;
        }
        return MagicItemType.WAND;
    }

    public static SpellElement getMagicElement(ItemStack item) {
        String id = getMagicItemId(item);
        if (id != null) {
            if (id.contains("fire") || id.contains("ogień")) return SpellElement.FIRE;
            if (id.contains("fen") || id.contains("air") || id.contains("wiatr") || id.contains("powietrze")) return SpellElement.AIR;
            if (id.contains("lightning") || id.contains("piorun") || id.contains("błyskawic")) return SpellElement.LIGHTNING;
            if (id.contains("water") || id.contains("woda") || id.contains("wody")) return SpellElement.WATER;
            if (id.contains("earth") || id.contains("ziemia")) return SpellElement.EARTH;
        }
        return SpellElement.FIRE;
    }

    public static boolean isMagicItem(ItemStack item) {
        return getMagicItemId(item) != null;
    }

    public static boolean isMagicStaff(ItemStack item) {
        return isMagicItem(item) && getMagicItemType(item) == MagicItemType.STAFF;
    }

    public static boolean isMagicWand(ItemStack item) {
        return isMagicItem(item) && getMagicItemType(item) == MagicItemType.WAND;
    }

    public static boolean isMagicTome(ItemStack item) {
        return isMagicItem(item) && getMagicItemType(item) == MagicItemType.TOME;
    }

    public static boolean isWaterWand(ItemStack item) {
        String id = getMagicItemId(item);
        return id != null && (id.equals("wand_water") || (isMagicWand(item) && getMagicElement(item) == SpellElement.WATER));
    }

    public static boolean isAirWand(ItemStack item) {
        String id = getMagicItemId(item);
        return id != null && (id.equals("wand_fen") || (isMagicWand(item) && getMagicElement(item) == SpellElement.AIR));
    }

    public static void initMagicItem(ItemStack item, String customId) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String id = (customId != null && !customId.isEmpty()) ? customId.toLowerCase(Locale.ROOT) : getMagicItemId(item);
        if (id == null) id = "tome_fire";

        pdc.set(MAGIC_ITEM_ID_KEY, PersistentDataType.STRING, id);
        if (!pdc.has(TOME_LEVEL_KEY, PersistentDataType.INTEGER)) {
            pdc.set(TOME_LEVEL_KEY, PersistentDataType.INTEGER, 1);
        }
        if (!pdc.has(TOME_KILLS_KEY, PersistentDataType.INTEGER)) {
            pdc.set(TOME_KILLS_KEY, PersistentDataType.INTEGER, 0);
        }

        String defaultPrimary = getDefaultPrimarySpell(id);
        String defaultSecondary = (getMagicItemType(item) == MagicItemType.TOME) ? "none" : "none";

        if (!pdc.has(SPELL_PRIMARY_KEY, PersistentDataType.STRING)) {
            pdc.set(SPELL_PRIMARY_KEY, PersistentDataType.STRING, defaultPrimary);
        }
        if (!pdc.has(SPELL_SECONDARY_KEY, PersistentDataType.STRING)) {
            pdc.set(SPELL_SECONDARY_KEY, PersistentDataType.STRING, defaultSecondary);
        }

        item.setItemMeta(meta);
        updateTomeLore(item);
    }

    public static String getDefaultPrimarySpell(String id) {
        if (id == null) return "fireblast";
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "wand_fen" -> "blow";
            case "wand_water" -> "splash";
            case "staff_lightning" -> "lightning";
            default -> "fireblast";
        };
    }

    public static int getTomeLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer level = pdc.get(TOME_LEVEL_KEY, PersistentDataType.INTEGER);
        return (level != null && level > 0) ? level : 1;
    }

    public static void setTomeLevel(ItemStack item, int level) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(TOME_LEVEL_KEY, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
        updateTomeLore(item);
    }

    public static int getTomeKills(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer kills = pdc.get(TOME_KILLS_KEY, PersistentDataType.INTEGER);
        return kills != null ? kills : 0;
    }

    public static void recordMagicKill(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        int currentKills = getTomeKills(item) + 1;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(TOME_KILLS_KEY, PersistentDataType.INTEGER, currentKills);
        item.setItemMeta(meta);
        updateTomeLore(item);

        if (player != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);
        }
    }

    public static Set<String> getUpgrades(ItemStack item) {
        Set<String> set = new HashSet<>();
        if (item == null || !item.hasItemMeta()) return set;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String raw = pdc.get(TOME_UPGRADES_KEY, PersistentDataType.STRING);
        if (raw != null && !raw.isEmpty()) {
            set.addAll(Arrays.asList(raw.split(",")));
        }
        return set;
    }

    public static boolean hasUpgrade(ItemStack item, String upgradeId) {
        return getUpgrades(item).contains(upgradeId);
    }

    public static void addUpgrade(ItemStack item, String upgradeId) {
        if (item == null || !item.hasItemMeta()) return;
        Set<String> upgrades = getUpgrades(item);
        upgrades.add(upgradeId);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(TOME_UPGRADES_KEY, PersistentDataType.STRING, String.join(",", upgrades));
        item.setItemMeta(meta);
        updateTomeLore(item);
    }

    public static String getPrimarySpellId(ItemStack item) {
        String itemId = getMagicItemId(item);
        String defaultSpell = getDefaultPrimarySpell(itemId);

        if (item == null || !item.hasItemMeta()) return defaultSpell;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(SPELL_PRIMARY_KEY, PersistentDataType.STRING);
        return (id != null && !id.isEmpty()) ? id : defaultSpell;
    }

    public static void setPrimarySpellId(ItemStack item, String spellId) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(SPELL_PRIMARY_KEY, PersistentDataType.STRING, spellId);
        item.setItemMeta(meta);
        updateTomeLore(item);
    }

    public static String getSecondarySpellId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "none";
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(SPELL_SECONDARY_KEY, PersistentDataType.STRING);
        return id != null ? id : "none";
    }

    public static void setSecondarySpellId(ItemStack item, String spellId) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(SPELL_SECONDARY_KEY, PersistentDataType.STRING, spellId);
        item.setItemMeta(meta);
        updateTomeLore(item);
    }

    public static void updateTomeLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        String id = getMagicItemId(item);
        if (id == null) return;

        int level = getTomeLevel(item);
        int kills = getTomeKills(item);
        String primary = getPrimarySpellId(item);
        String secondary = getSecondarySpellId(item);
        MagicItemType type = getMagicItemType(item);

        // Pobranie krótkiego opisu z konfiguracji
        String shortDesc = getShortDescription(id);

        List<String> lore = new ArrayList<>();
        lore.add(shortDesc);
        lore.add("");
        lore.add("§6✦ Poziom: §e" + level + " §8| §c⚔ Zabójstwa: §f" + kills);

        switch (type) {
            case STAFF -> {
                lore.add("§e✦ PPM (Ładowanie): §f" + formatSpellName(primary));
                lore.add("");
                lore.add("§d✦ Shift + PPM: §fMenu czarów");
            }
            case WAND -> {
                lore.add("§b✦ LPM: §f" + formatSpellName(primary));
                lore.add("");
                lore.add("§d✦ PPM: §fMenu czarów");
            }
            case TOME -> {
                lore.add("§6✦ LPM: §f" + formatSpellName(primary));
                lore.add("§e✦ Kucanie (Shift): §f" + (secondary.equalsIgnoreCase("none") ? "§8[Brak]" : formatSpellName(secondary)));
                lore.add("");
                lore.add("§d✦ PPM: §fMenu czarów");
            }
        }

        ItemMeta meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private static String getShortDescription(String id) {
        FileConfiguration cfg = AmonPackPlugin.magicConfig;
        if (cfg != null && cfg.contains("magic.items." + id + ".short_desc")) {
            return "§7" + cfg.getString("magic.items." + id + ".short_desc");
        }
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "wand_fen" -> "§7Mistyczna różdżka wiatru wykuta z esencji fenów.";
            case "wand_water" -> "§7Różdżka ukształtowana z czystego oceanicznego kryształu.";
            case "staff_lightning" -> "§7Mistyczna laska przewodząca pioruny i energię burzy.";
            default -> "§7Starożytna księga zawierająca pierwotne zaklęcia.";
        };
    }

    public static String formatSpellName(String id) {
        if (id == null) return "Brak";
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "fireblast" -> "Fire Blast";
            case "blazing" -> "Blazing";
            case "flashpoint" -> "FlashPoint";
            case "fire_circle" -> "Fire Circle";
            case "barrage" -> "Barrage";
            case "blow" -> "Blow";
            case "airblade" -> "Airblade";
            case "air_vortex" -> "Wir Powietrza";
            case "lightning" -> "Lightning";
            case "chain_lightning" -> "Chain Lightning";
            case "splash" -> "Splash";
            case "freeze" -> "Freeze";
            case "evaporate" -> "Evaporate";
            case "none" -> "§8[Brak]";
            default -> id;
        };
    }
}
