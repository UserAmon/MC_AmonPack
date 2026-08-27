package RPG.Magic.manager;

import Plugin.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class MagicItemManager {

    public static final NamespacedKey TOME_LEVEL_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_tome_level");
    public static final NamespacedKey TOME_KILLS_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_tome_kills");
    public static final NamespacedKey TOME_UPGRADES_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_tome_upgrades");
    public static final NamespacedKey SPELL_PRIMARY_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_primary_spell");
    public static final NamespacedKey SPELL_SECONDARY_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_secondary_spell");

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

    public static boolean isMagicStaff(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (meta.hasDisplayName() && meta.getDisplayName().toLowerCase().contains("laska")) return true;
        return meta.hasCustomModelData() && meta.getCustomModelData() == 20003;
    }

    public static boolean isWaterWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (meta.hasDisplayName() && (meta.getDisplayName().toLowerCase().contains("wody") || meta.getDisplayName().toLowerCase().contains("water"))) return true;
        return meta.hasCustomModelData() && meta.getCustomModelData() == 20004;
    }

    public static boolean isAirWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (meta.hasDisplayName() && (meta.getDisplayName().toLowerCase().contains("fen") || meta.getDisplayName().toLowerCase().contains("powietrza"))) return true;
        return meta.hasCustomModelData() && meta.getCustomModelData() == 20002;
    }

    public static boolean isMagicWand(ItemStack item) {
        return isAirWand(item) || isWaterWand(item);
    }

    public static boolean isMagicTome(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (meta.hasDisplayName() && (meta.getDisplayName().toLowerCase().contains("tom") || meta.getDisplayName().toLowerCase().contains("tome") || meta.getDisplayName().toLowerCase().contains("księga"))) return true;
        return meta.hasCustomModelData() && (meta.getCustomModelData() == 20001 || meta.getCustomModelData() == 10010);
    }

    public static String getPrimarySpellId(ItemStack item) {
        String defaultSpell = "fireblast";
        if (isAirWand(item)) defaultSpell = "blow";
        else if (isWaterWand(item)) defaultSpell = "splash";
        else if (isMagicStaff(item)) defaultSpell = "lightning";

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
        int level = getTomeLevel(item);
        int kills = getTomeKills(item);
        String primary = getPrimarySpellId(item);
        String secondary = getSecondarySpellId(item);
        Set<String> upgrades = getUpgrades(item);

        List<String> lore = new ArrayList<>();
        if (isMagicStaff(item)) {
            lore.add("§7Mistyczna laska przewodząca pioruny i energię burzy.");
            lore.add("");
            lore.add("§6✦ Poziom Laski: §e" + level + " §8| §c⚔ Zabójstwa: §f" + kills);
            lore.add("§a✦ Zaklęcie (PPM - Ładowanie): §f" + formatSpellName(primary));
        } else if (isWaterWand(item)) {
            lore.add("§7Różdżka ukształtowana z czystego oceanicznego kryształu.");
            lore.add("");
            lore.add("§6✦ Poziom Różdżki: §e" + level + " §8| §c⚔ Zabójstwa: §f" + kills);
            lore.add("§a✦ Zaklęcie (LPM): §f" + formatSpellName(primary));
        } else if (isAirWand(item)) {
            lore.add("§7Mistyczna różdżka wiatru wykuta z esencji fenów.");
            lore.add("");
            lore.add("§6✦ Poziom Różdżki: §e" + level + " §8| §c⚔ Zabójstwa: §f" + kills);
            lore.add("§a✦ Zaklęcie (LPM / Shift+LPM): §f" + formatSpellName(primary));
        } else {
            lore.add("§7Starożytna księga zawierająca pierwotne zaklęcia.");
            lore.add("");
            lore.add("§6✦ Poziom Tomu: §e" + level + " §8| §c⚔ Zabójstwa: §f" + kills);
            lore.add("§a✦ Zaklęcie Główne (LPM): §f" + formatSpellName(primary));
            lore.add("§b✦ Tkany Czar (Shift): §f" + (secondary.equalsIgnoreCase("none") ? "§8[Brak]" : formatSpellName(secondary)));
        }

        lore.add("");
        lore.add("§e✦ Ulepszenia: §f" + (upgrades.isEmpty() ? "§7Brak" : upgrades.size() + " aktywne"));
        lore.add("§d✦ PPM: §fOtwórz menu przypisywania czarów");

        ItemMeta meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static String formatSpellName(String id) {
        return switch (id.toLowerCase()) {
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
            default -> id;
        };
    }
}
