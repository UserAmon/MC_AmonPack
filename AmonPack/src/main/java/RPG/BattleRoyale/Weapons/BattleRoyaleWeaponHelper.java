package RPG.BattleRoyale.Weapons;

import CustomContent.Guns.AmmoType;
import CustomContent.Guns.GunData;
import CustomContent.Guns.GunType;
import CustomContent.Guns.GunUniqueMod;
import Plugin.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionColor;

import java.util.*;

public class BattleRoyaleWeaponHelper {

    public static final NamespacedKey KEY_BR_CURE = new NamespacedKey(AmonPackPlugin.plugin, "br_cure_item");
    public static final NamespacedKey KEY_BR_UPGRADE_KIT = new NamespacedKey(AmonPackPlugin.plugin, "br_upgrade_kit");
    public static final NamespacedKey KEY_UPGRADE_TIER = new NamespacedKey(AmonPackPlugin.plugin, "br_upgrade_tier");

    private static final Random random = new Random();

    /**
     * Tworzy w pełni skonfigurowaną broń palną.
     */
    public static ItemStack createGun(GunType type, int level, boolean rifling, boolean lock, boolean scope, boolean bayonet, GunUniqueMod uniqueMod) {
        GunData data = new GunData(type);
        data.setLevel(Math.max(1, level));
        data.setRifling(rifling);
        data.setReinforcedLock(lock);
        data.setBrassScope(scope);
        data.setBayonet(bayonet);
        data.setUniqueMod(uniqueMod != null ? uniqueMod : GunUniqueMod.NONE);
        data.setCurrentAmmo(data.getMaxAmmoCapacity());
        data.setLoadedAmmoType(type.getRequiredAmmoType());

        ItemStack stack = new ItemStack(type.getBaseMaterial());
        data.applyToItemStack(stack);
        return stack;
    }

    /**
     * Tworzy losową broń palną z możliwym losowym ulepszeniem.
     */
    public static ItemStack createRandomGun(int minLevel, int maxLevel) {
        GunType[] types = GunType.values();
        GunType type = types[random.nextInt(types.length)];
        int level = minLevel + random.nextInt(Math.max(1, maxLevel - minLevel + 1));

        boolean rifling = random.nextDouble() < 0.35;
        boolean lock = random.nextDouble() < 0.30;
        boolean scope = (type == GunType.FLINTLOCK_MUSKET && random.nextDouble() < 0.40);
        boolean bayonet = random.nextDouble() < 0.25;

        GunUniqueMod uMod = GunUniqueMod.NONE;
        if (random.nextDouble() < 0.20) {
            List<GunUniqueMod> compatible = new ArrayList<>();
            for (GunUniqueMod m : GunUniqueMod.values()) {
                if (m.getCompatibleGun() == type) {
                    compatible.add(m);
                }
            }
            if (!compatible.isEmpty()) {
                uMod = compatible.get(random.nextInt(compatible.size()));
            }
        }

        return createGun(type, level, rifling, lock, scope, bayonet, uMod);
    }

    /**
     * Tworzy pakiet amunicji.
     */
    public static ItemStack createAmmo(AmmoType ammoType, int amount) {
        ItemStack item = new ItemStack(ammoType.getMaterial(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ammoType.getDisplayName());
            meta.setCustomModelData(ammoType.getCustomModelData());
            meta.setLore(Collections.singletonList(ammoType.getDescription()));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Tworzy zestaw ulepszania broni (Upgrade Kit).
     */
    public static ItemStack createUpgradeKit(int tier) {
        ItemStack item = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Zestaw Ulepszenia Broni [★ Tier " + tier + "]");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Specjalistyczne narzędzia do modyfikacji broni palnej.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Kliknij (PPM) trzymając broń w drugiej ręce");
            lore.add(ChatColor.YELLOW + "lub upuść ten przedmiot na broń w ekwipunku,");
            lore.add(ChatColor.GREEN + "aby zwiększyć jej poziom o +" + tier + "!");
            lore.add(ChatColor.GRAY + "• Zwiększa obrażenia bazowe broni");
            lore.add(ChatColor.GRAY + "• Poprawia stabilność i celność");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(KEY_BR_UPGRADE_KIT, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(KEY_UPGRADE_TIER, PersistentDataType.INTEGER, tier);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isUpgradeKit(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_BR_UPGRADE_KIT, PersistentDataType.BYTE);
    }

    public static int getUpgradeKitTier(ItemStack item) {
        if (!isUpgradeKit(item)) return 1;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(KEY_UPGRADE_TIER, PersistentDataType.INTEGER, 1);
    }

    /**
     * Tworzy Lekarstwo na Infekcję (Antidotum).
     */
    public static ItemStack createInfectionCure() {
        return createInfectionCure("POTION", "&a&l💉 Antidotum na Infekcję", Collections.singletonList("&7Wypij ten wywar, aby natychmiast usunąć wirus zombie!"), 22001);
    }

    public static ItemStack createInfectionCure(String materialStr, String displayName, List<String> lore, int customModelData) {
        Material mat = Material.matchMaterial(materialStr);
        if (mat == null) mat = Material.POTION;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            List<String> formattedLore = new ArrayList<>();
            if (lore != null) {
                for (String l : lore) {
                    formattedLore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
            }
            if (formattedLore.isEmpty()) {
                formattedLore.add(ChatColor.GRAY + "Wypij, aby natychmiast cofnąć wirusa zombie!");
            }
            meta.setLore(formattedLore);

            if (meta instanceof PotionMeta pm) {
                pm.setColor(PotionColor.fromRGB(40, 220, 100));
                pm.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            }

            meta.getPersistentDataContainer().set(KEY_BR_CURE, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isInfectionCure(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_BR_CURE, PersistentDataType.BYTE);
    }

    /**
     * Tworzy losową broń vanilla (miecze, łuki, topory) z wyważonymi enchantami.
     */
    public static ItemStack createVanillaWeapon(String type) {
        ItemStack item;
        switch (type.toUpperCase()) {
            case "DIAMOND_SWORD":
                item = new ItemStack(Material.DIAMOND_SWORD);
                item.addEnchantment(Enchantment.SHARPNESS, 1 + random.nextInt(2));
                break;
            case "IRON_SWORD":
                item = new ItemStack(Material.IRON_SWORD);
                if (random.nextBoolean()) item.addEnchantment(Enchantment.SHARPNESS, 1);
                break;
            case "BOW":
                item = new ItemStack(Material.BOW);
                item.addEnchantment(Enchantment.POWER, 1 + random.nextInt(2));
                break;
            case "CROSSBOW":
                item = new ItemStack(Material.CROSSBOW);
                if (random.nextBoolean()) item.addEnchantment(Enchantment.QUICK_CHARGE, 1);
                break;
            case "IRON_AXE":
                item = new ItemStack(Material.IRON_AXE);
                break;
            case "SHIELD":
                item = new ItemStack(Material.SHIELD);
                break;
            default:
                Material m = Material.matchMaterial(type);
                item = new ItemStack(m != null ? m : Material.IRON_SWORD);
                break;
        }
        return item;
    }
}
