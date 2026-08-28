package CustomContent.Guns;

import Plugin.AmonPackPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GunData {

    public static final NamespacedKey KEY_GUN_TYPE = new NamespacedKey(AmonPackPlugin.plugin, "gun_type");
    public static final NamespacedKey KEY_AMMO = new NamespacedKey(AmonPackPlugin.plugin, "gun_ammo_loaded");
    public static final NamespacedKey KEY_LOADED_AMMO_TYPE = new NamespacedKey(AmonPackPlugin.plugin, "gun_loaded_ammo_type");
    public static final NamespacedKey KEY_DURABILITY = new NamespacedKey(AmonPackPlugin.plugin, "gun_durability");
    public static final NamespacedKey KEY_LEVEL = new NamespacedKey(AmonPackPlugin.plugin, "gun_level");
    public static final NamespacedKey KEY_MOD_RIFLING = new NamespacedKey(AmonPackPlugin.plugin, "gun_mod_rifling");
    public static final NamespacedKey KEY_MOD_LOCK = new NamespacedKey(AmonPackPlugin.plugin, "gun_mod_lock");
    public static final NamespacedKey KEY_MOD_SCOPE = new NamespacedKey(AmonPackPlugin.plugin, "gun_mod_scope");
    public static final NamespacedKey KEY_MOD_BAYONET = new NamespacedKey(AmonPackPlugin.plugin, "gun_mod_bayonet");

    private final GunType gunType;
    private int currentAmmo;
    private AmmoType loadedAmmoType;
    private int currentDurability;
    private int level = 1;
    private boolean rifling = false;
    private boolean reinforcedLock = false;
    private boolean brassScope = false;
    private boolean bayonet = false;

    public GunData(GunType gunType) {
        this.gunType = gunType;
        this.currentAmmo = 0; // domyślnie rozładowana po wykuciu
        this.loadedAmmoType = null;
        this.currentDurability = gunType.getMaxDurability();
    }

    public static boolean isGun(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(KEY_GUN_TYPE, PersistentDataType.STRING);
    }

    public static GunData fromItemStack(ItemStack item) {
        if (!isGun(item)) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String typeStr = pdc.get(KEY_GUN_TYPE, PersistentDataType.STRING);
        GunType type = GunType.fromId(typeStr);
        if (type == null) return null;

        GunData data = new GunData(type);
        data.currentAmmo = pdc.getOrDefault(KEY_AMMO, PersistentDataType.INTEGER, 0);
        String ammoStr = pdc.get(KEY_LOADED_AMMO_TYPE, PersistentDataType.STRING);
        data.loadedAmmoType = AmmoType.fromId(ammoStr);
        if (data.currentAmmo <= 0) {
            data.loadedAmmoType = null;
        }

        data.currentDurability = pdc.getOrDefault(KEY_DURABILITY, PersistentDataType.INTEGER, type.getMaxDurability());
        data.level = pdc.getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 1);
        data.rifling = pdc.getOrDefault(KEY_MOD_RIFLING, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
        data.reinforcedLock = pdc.getOrDefault(KEY_MOD_LOCK, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
        data.brassScope = pdc.getOrDefault(KEY_MOD_SCOPE, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
        data.bayonet = pdc.getOrDefault(KEY_MOD_BAYONET, PersistentDataType.BYTE, (byte) 0) == (byte) 1;

        return data;
    }

    public void applyToItemStack(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_GUN_TYPE, PersistentDataType.STRING, gunType.getId());
        pdc.set(KEY_AMMO, PersistentDataType.INTEGER, currentAmmo);
        if (loadedAmmoType != null && currentAmmo > 0) {
            pdc.set(KEY_LOADED_AMMO_TYPE, PersistentDataType.STRING, loadedAmmoType.getId());
        } else {
            pdc.remove(KEY_LOADED_AMMO_TYPE);
        }
        pdc.set(KEY_DURABILITY, PersistentDataType.INTEGER, currentDurability);
        pdc.set(KEY_LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(KEY_MOD_RIFLING, PersistentDataType.BYTE, (byte) (rifling ? 1 : 0));
        pdc.set(KEY_MOD_LOCK, PersistentDataType.BYTE, (byte) (reinforcedLock ? 1 : 0));
        pdc.set(KEY_MOD_SCOPE, PersistentDataType.BYTE, (byte) (brassScope ? 1 : 0));
        pdc.set(KEY_MOD_BAYONET, PersistentDataType.BYTE, (byte) (bayonet ? 1 : 0));

        meta.setCustomModelData(gunType.getCustomModelData());
        meta.setDisplayName(gunType.getDisplayName() + (level > 1 ? " §7[Poz. " + level + "]" : ""));
        meta.setLore(getFormattedLore());
        meta.setUnbreakable(false);

        // Vanilla durability bar sync
        if (meta instanceof org.bukkit.inventory.meta.Damageable dmg) {
            double ratio = 1.0 - ((double) currentDurability / gunType.getMaxDurability());
            int vanillaMax = item.getType().getMaxDurability();
            dmg.setDamage((int) Math.max(0, Math.min(vanillaMax - 1, ratio * vanillaMax)));
        }

        item.setItemMeta(meta);
    }

    public List<String> getFormattedLore() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Prymitywna broń czarnoprochowa.");
        lore.add("");

        // Stan amunicji
        if (currentAmmo > 0) {
            String aName = loadedAmmoType != null ? loadedAmmoType.getDisplayName() : "Standardowa Kula";
            if (gunType.getMaxAmmo() > 1) {
                StringBuilder sb = new StringBuilder("§eKomory bębna: ");
                for (int i = 0; i < gunType.getMaxAmmo(); i++) {
                    if (i < currentAmmo) {
                        sb.append("§a● ");
                    } else {
                        sb.append("§8○ ");
                    }
                }
                sb.append("§f(").append(currentAmmo).append("/").append(gunType.getMaxAmmo()).append(")");
                lore.add(sb.toString());
                lore.add("§7Załadowano: " + aName);
            } else {
                lore.add("§eZaładowana amunicja: " + aName + " §a(1/1)");
            }
        } else {
            lore.add("§eStan komory: §cRozładowana (0/" + gunType.getMaxAmmo() + ")");
        }

        // Trwałość
        double durPercent = ((double) currentDurability / gunType.getMaxDurability()) * 100;
        String durColor = durPercent > 50 ? "§a" : durPercent > 20 ? "§e" : "§c";
        lore.add("§7Wytrzymałość: " + durColor + currentDurability + "§7/" + gunType.getMaxDurability());
        lore.add("");

        // Statystyki
        double dmg = getDamage();
        double hsMult = getHeadshotMultiplier() * 100;
        lore.add("§6Właściwości Balistyczne:");
        if (gunType == GunType.BLUNDERBUSS && loadedAmmoType == AmmoType.SLUG_CARTRIDGE) {
            lore.add(" §c⚔ Obrażenia pocisku (Slug): §f+" + String.format(Locale.ROOT, "%.1f", dmg));
        } else if (gunType == GunType.BLUNDERBUSS) {
            lore.add(" §c⚔ Obrażenia śrutu: §f8x " + String.format(Locale.ROOT, "%.1f", dmg) + " §7(Max: " + String.format(Locale.ROOT, "%.1f", dmg * 8) + ")");
        } else {
            lore.add(" §c⚔ Obrażenia bazowe: §f+" + String.format(Locale.ROOT, "%.1f", dmg));
        }
        lore.add(" §4🎯 Trafienie w głowę: §f+" + (int) hsMult + "%");
        lore.add(" §b⚡ Zasięg skuteczny: §f" + (int) getEffectiveRange() + "m");
        lore.add(" §e⏳ Czas ładowania: §f" + String.format(Locale.ROOT, "%.1f", getReloadTimeSeconds()) + "s");

        // Zainstalowane ulepszenia rusznikarskie
        boolean hasMods = rifling || reinforcedLock || brassScope || bayonet;
        if (hasMods) {
            lore.add("");
            lore.add("§dModyfikacje Rusznikarskie:");
            if (rifling) lore.add(" §f✦ §aGwintowana Lufa §7(+40% celności, +10m)");
            if (reinforcedLock) lore.add(" §f✦ §eWzmocniony Zamek §7(-30% czasu ładowania)");
            if (brassScope) lore.add(" §f✦ §bLunetka Mosiężna §7(Super zoom ADS, +25% headshot)");
            if (bayonet) lore.add(" §f✦ §cBagnet Myśliwski §7(+7.0 DMG wręcz przy uderzeniu)");
        }

        lore.add("");
        lore.add("§8[LPM] Wystrzał  |  [PPM] Celowanie (ADS)");
        lore.add("§8[F] Przeładowanie (wybiera amunicję z początku paska/EQ)");

        return lore;
    }

    public double getDamage() {
        if (gunType == GunType.BLUNDERBUSS && loadedAmmoType == AmmoType.SLUG_CARTRIDGE) {
            return 15.0 + (level - 1) * 1.5;
        }
        double base = gunType.getBaseDamage();
        double lvlBonus = (level - 1) * 0.8;
        return base + lvlBonus;
    }

    public double getHeadshotMultiplier() {
        double mult = gunType.getHeadshotMultiplier();
        if (brassScope) mult += 0.25;
        mult += (level - 1) * 0.05;
        return mult;
    }

    public double getEffectiveRange() {
        double r = gunType.getMaxRange();
        if (loadedAmmoType == AmmoType.SLUG_CARTRIDGE) {
            r += 15.0;
        }
        if (rifling) r += 10.0;
        return r;
    }

    public double getSpread() {
        if (gunType == GunType.BLUNDERBUSS && loadedAmmoType == AmmoType.SLUG_CARTRIDGE) {
            double s = 0.035;
            if (rifling) s *= 0.60;
            s *= Math.max(0.40, 1.0 - ((level - 1) * 0.12));
            return s;
        }

        double s = gunType.getBaseSpread();
        if (rifling) s *= 0.60;
        // Każdy poziom znacząco polepsza skupienie/celność kuli
        s *= Math.max(0.40, 1.0 - ((level - 1) * 0.12));
        return s;
    }

    public double getReloadTimeSeconds() {
        return getReloadTicks() / 20.0;
    }

    public int getReloadTicks() {
        double ticks = gunType.getReloadTicks();
        if (loadedAmmoType == AmmoType.SLUG_CARTRIDGE) {
            ticks += 20; // 1s dłużej
        }
        if (reinforcedLock) ticks *= 0.70;
        return (int) Math.max(10, ticks);
    }

    // Getters and Setters
    public GunType getGunType() { return gunType; }
    public int getCurrentAmmo() { return currentAmmo; }
    public void setCurrentAmmo(int currentAmmo) {
        this.currentAmmo = Math.max(0, Math.min(gunType.getMaxAmmo(), currentAmmo));
        if (this.currentAmmo <= 0) {
            this.loadedAmmoType = null;
        }
    }
    public AmmoType getLoadedAmmoType() { return loadedAmmoType; }
    public void setLoadedAmmoType(AmmoType loadedAmmoType) { this.loadedAmmoType = loadedAmmoType; }
    public int getCurrentDurability() { return currentDurability; }
    public void setCurrentDurability(int currentDurability) { this.currentDurability = Math.max(0, Math.min(gunType.getMaxDurability(), currentDurability)); }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(5, level)); }
    public boolean hasRifling() { return rifling; }
    public void setRifling(boolean rifling) { this.rifling = rifling; }
    public boolean hasReinforcedLock() { return reinforcedLock; }
    public void setReinforcedLock(boolean reinforcedLock) { this.reinforcedLock = reinforcedLock; }
    public boolean hasBrassScope() { return brassScope; }
    public void setBrassScope(boolean brassScope) { this.brassScope = brassScope; }
    public boolean hasBayonet() { return bayonet; }
    public void setBayonet(boolean bayonet) { this.bayonet = bayonet; }
}
