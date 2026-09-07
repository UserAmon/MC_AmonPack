package CustomContent.Guns;

import Plugin.AmonPackPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
    public static final NamespacedKey KEY_MOD_UNIQUE = new NamespacedKey(AmonPackPlugin.plugin, "gun_mod_unique");
    public static final NamespacedKey KEY_UNIQUE_MOD_ID = new NamespacedKey(AmonPackPlugin.plugin, "gun_unique_mod_id");

    private static final UUID SPEED_MOD_UUID = UUID.fromString("6a71e621-3df2-4f38-bc02-b2d952676b71");

    private final GunType gunType;
    private int currentAmmo;
    private AmmoType loadedAmmoType;
    private int currentDurability;
    private int level = 1;
    private boolean rifling = false;
    private boolean reinforcedLock = false;
    private boolean brassScope = false;
    private boolean bayonet = false;
    private GunUniqueMod uniqueMod = GunUniqueMod.NONE;

    public GunData(GunType gunType) {
        this.gunType = gunType;
        this.currentAmmo = 0;
        this.loadedAmmoType = null;
        this.currentDurability = getMaxDurability();
    }

    public static boolean isGun(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(KEY_GUN_TYPE, PersistentDataType.STRING)) return true;
        if (meta.hasCustomModelData()) {
            int cmd = meta.getCustomModelData();
            if (cmd >= 10050 && cmd <= 10053) return true;
        }
        return false;
    }

    public static GunData fromItemStack(ItemStack item) {
        if (!isGun(item)) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String typeStr = pdc.get(KEY_GUN_TYPE, PersistentDataType.STRING);
        GunType type = GunType.fromId(typeStr);
        if (type == null && meta.hasCustomModelData()) {
            int cmd = meta.getCustomModelData();
            if (cmd == 10050) type = GunType.FLINTLOCK_PISTOL;
            else if (cmd == 10051) type = GunType.FLINTLOCK_MUSKET;
            else if (cmd == 10052) type = GunType.BLUNDERBUSS;
            else if (cmd == 10053) type = GunType.PEPPERBOX;
        }
        if (type == null) return null;

        GunData data = new GunData(type);
        data.currentAmmo = pdc.getOrDefault(KEY_AMMO, PersistentDataType.INTEGER, 0);
        String ammoStr = pdc.get(KEY_LOADED_AMMO_TYPE, PersistentDataType.STRING);
        data.loadedAmmoType = AmmoType.fromId(ammoStr);

        data.currentDurability = pdc.getOrDefault(KEY_DURABILITY, PersistentDataType.INTEGER, data.getMaxDurability());
        data.level = pdc.getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 1);
        data.rifling = pdc.getOrDefault(KEY_MOD_RIFLING, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
        data.reinforcedLock = pdc.getOrDefault(KEY_MOD_LOCK, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
        data.brassScope = pdc.getOrDefault(KEY_MOD_SCOPE, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
        data.bayonet = pdc.getOrDefault(KEY_MOD_BAYONET, PersistentDataType.BYTE, (byte) 0) == (byte) 1;

        String uModStr = pdc.get(KEY_UNIQUE_MOD_ID, PersistentDataType.STRING);
        if (uModStr != null && !uModStr.isEmpty()) {
            data.uniqueMod = GunUniqueMod.fromId(uModStr);
        } else if (pdc.getOrDefault(KEY_MOD_UNIQUE, PersistentDataType.BYTE, (byte) 0) == (byte) 1) {
            // Legacy mapping
            if (type == GunType.FLINTLOCK_PISTOL) data.uniqueMod = GunUniqueMod.PISTOL_FAST_AND_FURIOUS;
            else if (type == GunType.BLUNDERBUSS) data.uniqueMod = GunUniqueMod.SHOTGUN_DOUBLE_BARREL;
            else if (type == GunType.PEPPERBOX) data.uniqueMod = GunUniqueMod.PEPPERBOX_PERFECT_SOLDIER;
            else if (type == GunType.FLINTLOCK_MUSKET) data.uniqueMod = GunUniqueMod.MUSKET_STALKER;
        } else {
            data.uniqueMod = GunUniqueMod.NONE;
        }

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
        } else if (currentAmmo <= 0) {
            pdc.remove(KEY_LOADED_AMMO_TYPE);
        }
        pdc.set(KEY_DURABILITY, PersistentDataType.INTEGER, currentDurability);
        pdc.set(KEY_LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(KEY_MOD_RIFLING, PersistentDataType.BYTE, (byte) (rifling ? 1 : 0));
        pdc.set(KEY_MOD_LOCK, PersistentDataType.BYTE, (byte) (reinforcedLock ? 1 : 0));
        pdc.set(KEY_MOD_SCOPE, PersistentDataType.BYTE, (byte) (brassScope ? 1 : 0));
        pdc.set(KEY_MOD_BAYONET, PersistentDataType.BYTE, (byte) (bayonet ? 1 : 0));
        pdc.set(KEY_MOD_UNIQUE, PersistentDataType.BYTE, (byte) (uniqueMod != GunUniqueMod.NONE ? 1 : 0));
        pdc.set(KEY_UNIQUE_MOD_ID, PersistentDataType.STRING, uniqueMod.getId());

        // Kara do prędkości poruszania się (-10%), chyba że broń ma ulepszenie Szybki i Wściekły
        meta.removeAttributeModifier(Attribute.MOVEMENT_SPEED);
        boolean hasSpeedPenalty = (uniqueMod != GunUniqueMod.PISTOL_FAST_AND_FURIOUS);
        if (hasSpeedPenalty) {
            meta.addAttributeModifier(
                    Attribute.MOVEMENT_SPEED,
                    new AttributeModifier(
                            SPEED_MOD_UUID,
                            "gun_speed_penalty",
                            GunConfigManager.getInstance().getMovementSpeedPenalty(),
                            AttributeModifier.Operation.ADD_SCALAR,
                            EquipmentSlot.HAND
                    )
            );
        }

        meta.setCustomModelData(gunType.getCustomModelData());
        meta.setDisplayName(gunType.getDisplayName() + (level > 1 ? " §7[Poz. " + level + "]" : ""));
        meta.setLore(getFormattedLore());
        meta.setUnbreakable(false);

        // Synchronizacja stanu naładowania kuszy
        if (meta instanceof CrossbowMeta cm) {
            if (currentAmmo > 0) {
                if (!cm.hasChargedProjectiles()) {
                    cm.addChargedProjectile(new ItemStack(Material.ARROW, 1));
                }
            } else {
                cm.setChargedProjectiles(Collections.emptyList());
            }
        }

        // Vanilla durability bar sync
        if (meta instanceof Damageable dmg) {
            double ratio = 1.0 - ((double) currentDurability / getMaxDurability());
            int vanillaMax = item.getType().getMaxDurability();
            dmg.setDamage((int) Math.max(0, Math.min(vanillaMax - 1, ratio * vanillaMax)));
        }

        item.setItemMeta(meta);
    }

    public List<String> getFormattedLore() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Prymitywna broń czarnoprochowa.");
        lore.add("");

        int maxCap = getMaxAmmoCapacity();

        // Stan amunicji
        if (currentAmmo > 0) {
            String aName = loadedAmmoType != null ? loadedAmmoType.getDisplayName() : "Standardowa Kula";
            if (maxCap > 1) {
                StringBuilder sb = new StringBuilder("§eKomory bębna: ");
                for (int i = 0; i < maxCap; i++) {
                    if (i < currentAmmo) {
                        sb.append("§a● ");
                    } else {
                        sb.append("§8○ ");
                    }
                }
                sb.append("§f(").append(currentAmmo).append("/").append(maxCap).append(")");
                lore.add(sb.toString());
                lore.add("§7Załadowano: " + aName);
            } else {
                lore.add("§eZaładowana amunicja: " + aName + " §a(1/1)");
            }
        } else {
            lore.add("§eStan komory: §cRozładowana (0/" + maxCap + ")");
        }

        // Trwałość & Waga
        int maxDur = getMaxDurability();
        double durPercent = ((double) currentDurability / maxDur) * 100;
        String durColor = durPercent > 50 ? "§a" : durPercent > 20 ? "§e" : "§c";
        lore.add("§7Wytrzymałość: " + durColor + currentDurability + "§7/" + maxDur);
        boolean hasSpeedPenalty = (uniqueMod != GunUniqueMod.PISTOL_FAST_AND_FURIOUS);
        lore.add("§7Ciężar broni: " + (hasSpeedPenalty ? "§c-10% Prędkości Ruchu" : "§aLekka (0% spowolnienia)"));
        lore.add("");

        // Statystyki
        double dmg = getDamage();
        double hsMult = getHeadshotMultiplier() * 100;
        lore.add("§6Właściwości Balistyczne:");
        if (gunType == GunType.BLUNDERBUSS && loadedAmmoType == AmmoType.SLUG_CARTRIDGE) {
            lore.add(" §c⚔ Obrażenia pocisku (Slug): §f+" + String.format(Locale.ROOT, "%.1f", dmg));
        } else if (gunType == GunType.BLUNDERBUSS && loadedAmmoType == AmmoType.DRAGON_SCATTER_SHOT) {
            lore.add(" §c⚔ Obrażenia smoczego śrutu: §f10x " + String.format(Locale.ROOT, "%.1f", dmg) + " §7(Max: " + String.format(Locale.ROOT, "%.1f", dmg * 10) + " + Ogień)");
        } else if (gunType == GunType.BLUNDERBUSS) {
            lore.add(" §c⚔ Obrażenia śrutu: §f8x " + String.format(Locale.ROOT, "%.1f", dmg) + " §7(Max: " + String.format(Locale.ROOT, "%.1f", dmg * 8) + ")");
        } else {
            lore.add(" §c⚔ Obrażenia bazowe: §f+" + String.format(Locale.ROOT, "%.1f", dmg));
        }
        lore.add(" §4🎯 Trafienie w głowę: §f+" + (int) hsMult + "%");
        lore.add(" §b⚡ Zasięg skuteczny: §f" + (int) getEffectiveRange() + "m");
        lore.add(" §e⏳ Czas ładowania: §f" + String.format(Locale.ROOT, "%.1f", getReloadTimeSeconds()) + "s");

        // Zainstalowane ulepszenia rusznikarskie
        boolean hasMods = rifling || reinforcedLock || brassScope || bayonet || (uniqueMod != GunUniqueMod.NONE);
        if (hasMods) {
            lore.add("");
            lore.add("§dModyfikacje Rusznikarskie:");
            if (rifling) lore.add(" §f✦ §aGwintowana Lufa §7(+30% celności, +10m)");
            if (reinforcedLock) lore.add(" §f✦ §eWzmocniony Zamek §7(-30% czasu ładowania)");
            if (bayonet) {
                switch (gunType) {
                    case FLINTLOCK_PISTOL:
                        lore.add(" §f✦ §6Wzmocniony Chwyt §7(+50 Wytrzymałości broni)");
                        break;
                    case BLUNDERBUSS:
                        lore.add(" §f✦ §6Ergonomiczne Łoże §7(Większy odrzut, Zabójstwo: 'Fach' na 5s [-0.5s ładowania])");
                        break;
                    case FLINTLOCK_MUSKET:
                        lore.add(" §f✦ §cRykoszetujący Zamek §7(Trafienia krytyczne: 33% szansy na przeskok do kolejnego celu)");
                        break;
                    case PEPPERBOX:
                        lore.add(" §f✦ §bPowiększony Cylinder §7(+1 pocisk w magazynku)");
                        break;
                }
            }

            if (uniqueMod != GunUniqueMod.NONE) {
                switch (uniqueMod) {
                    case MUSKET_STALKER:
                        lore.add(" §f✦ §2[Unikalne] Stalker §7(Kamuflaż w krzakach, +35% Crit DMG z ukrycia i 5s po wyjściu)");
                        break;
                    case MUSKET_INFANTRYMAN:
                        lore.add(" §f✦ §e[Unikalne] Piechur §7(-1s ładowania, Speed w trakcie naciągania, szansa na darmowy nabój)");
                        break;
                    case PISTOL_FAST_AND_FURIOUS:
                        lore.add(" §f✦ §a[Unikalne] Szybki i Wściekły §7(Brak kary speeda, +75 dur, +10m, +2.0 DMG)");
                        break;
                    case PISTOL_WITCH_HUNTER:
                        lore.add(" §f✦ §5[Unikalne] Łowca Czarownic §7(-0.5s ładowania, +100% EXP, 2x Drop, drop amunicji, +40% Crit)");
                        break;
                    case PISTOL_EMOTIONAL_SUPPORT:
                        lore.add(" §f✦ §d[Unikalne] Wsparcie Emocjonalne §7(Debuff Wyczerpanie: spowolnienie i +30% DMG z pistoletów)");
                        break;
                    case PISTOL_PUNISHER:
                        lore.add(" §f✦ §c[Unikalne] Punisher §7(+1 pocisk, +1.5s ładowania, laserowa celność z lufą, +10m, Speed I po killu)");
                        break;
                    case SHOTGUN_DOUBLE_BARREL:
                        lore.add(" §f✦ §6[Unikalne] Dubeltówka §7(Druga lufa - 2 strzały przed przeładowaniem)");
                        break;
                    case SHOTGUN_DEMOLITION:
                        lore.add(" §f✦ §c[Unikalne] Demolka §7(+ilość śrutu, +reload, większy zasięg i zapłon, slug ze śrutem)");
                        break;
                    case PEPPERBOX_PERFECT_SOLDIER:
                        lore.add(" §f✦ §b[Unikalne] Żołnierz Doskonały §7(+1 pocisk w magazynku [do 6], -50% odrzutu i dymu)");
                        break;
                    case PEPPERBOX_HURRICANE:
                        lore.add(" §f✦ §3[Unikalne] Huragan §7(-2 pociski w magazynku [2 lub 3 z cylindrem], Headshot kill ładuje nabój)");
                        break;
                    default:
                        break;
                }
            }
        }

        lore.add("");
        lore.add("§8[Trzymaj PPM] Załadowanie i naciągnięcie broni");
        lore.add("§8[Kliknij PPM] Wystrzał kuli po naładowaniu");

        return lore;
    }

    public int getMaxAmmoCapacity() {
        int maxCap = gunType.getMaxAmmo();
        if (gunType == GunType.PEPPERBOX && bayonet) {
            maxCap += 1;
        }
        if (gunType == GunType.PEPPERBOX) {
            if (uniqueMod == GunUniqueMod.PEPPERBOX_HURRICANE) {
                maxCap -= 2; // -2 pociski (3 z cylindrem, 2 bez)
            } else if (uniqueMod == GunUniqueMod.PEPPERBOX_PERFECT_SOLDIER) {
                maxCap += 1; // +1 pocisk (6 z cylindrem, 5 bez)
            }
        }
        if (gunType == GunType.FLINTLOCK_PISTOL && uniqueMod == GunUniqueMod.PISTOL_PUNISHER) {
            maxCap += 1; // Punisher: +1 (2 pociski)
        }
        if (uniqueMod == GunUniqueMod.SHOTGUN_DOUBLE_BARREL) {
            maxCap = 2;
        }
        return Math.max(1, maxCap);
    }

    public int getMaxDurability() {
        int dur = GunConfigManager.getInstance().getMaxDurability(gunType);
        if (gunType == GunType.FLINTLOCK_PISTOL && bayonet) {
            dur += 50; // Wzmocniony Chwyt (+50 trwałości)
        }
        if (uniqueMod == GunUniqueMod.PISTOL_FAST_AND_FURIOUS) {
            dur += GunConfigManager.getInstance().getUniqueInt("pistol_fast_and_furious", "durability_bonus", 75);
        }
        return dur;
    }

    public double getDamage() {
        if (gunType == GunType.BLUNDERBUSS) {
            if (loadedAmmoType == AmmoType.SLUG_CARTRIDGE) {
                return 15.0 + (level - 1) * 1.5;
            } else if (loadedAmmoType == AmmoType.DRAGON_SCATTER_SHOT) {
                return 1.8 + (level - 1) * 0.2;
            }
            return GunConfigManager.getInstance().getBaseDamage(gunType) + (level - 1) * 0.2;
        }
        double base = GunConfigManager.getInstance().getBaseDamage(gunType);
        if (uniqueMod == GunUniqueMod.PISTOL_FAST_AND_FURIOUS) {
            base += GunConfigManager.getInstance().getUniqueDouble("pistol_fast_and_furious", "damage_bonus", 2.0);
        }
        double lvlBonus = (level - 1) * 0.8;
        return base + lvlBonus;
    }

    public double getHeadshotMultiplier() {
        double mult = GunConfigManager.getInstance().getHeadshotMultiplier(gunType);
        if (brassScope) mult += 0.25;
        if (uniqueMod == GunUniqueMod.PISTOL_WITCH_HUNTER) {
            mult += GunConfigManager.getInstance().getUniqueDouble("pistol_witch_hunter", "crit_bonus", 0.40);
        }
        mult += (level - 1) * 0.05;
        return mult;
    }

    public double getEffectiveRange() {
        double r = GunConfigManager.getInstance().getEffectiveRange(gunType);
        if (loadedAmmoType == AmmoType.SLUG_CARTRIDGE) {
            r += 15.0;
        } else if (loadedAmmoType == AmmoType.DRAGON_SCATTER_SHOT) {
            r += 5.0;
        }
        if (rifling) r += GunConfigManager.getInstance().getRiflingRangeBonus();
        if (uniqueMod == GunUniqueMod.PISTOL_FAST_AND_FURIOUS) {
            r += GunConfigManager.getInstance().getUniqueDouble("pistol_fast_and_furious", "range_bonus", 10.0);
        }
        if (uniqueMod == GunUniqueMod.PISTOL_PUNISHER) {
            r += 10.0;
        }
        if (uniqueMod == GunUniqueMod.SHOTGUN_DEMOLITION && (loadedAmmoType == AmmoType.DRAGON_CARTRIDGE || loadedAmmoType == AmmoType.DRAGON_SCATTER_SHOT)) {
            r += GunConfigManager.getInstance().getUniqueDouble("shotgun_demolition", "dragon_range_bonus", 8.0);
        }
        return r;
    }

    public double getSpread() {
        if (gunType == GunType.BLUNDERBUSS && loadedAmmoType == AmmoType.SLUG_CARTRIDGE) {
            double s = 0.07;
            if (rifling) s *= (1.0 - GunConfigManager.getInstance().getRiflingSpreadReduction());
            s *= Math.max(0.40, 1.0 - ((level - 1) * 0.12));
            return s;
        }

        double s = GunConfigManager.getInstance().getBaseSpread(gunType);
        if (rifling) s *= (1.0 - GunConfigManager.getInstance().getRiflingSpreadReduction());
        if (uniqueMod == GunUniqueMod.PISTOL_PUNISHER) {
            if (rifling) {
                s = 0.0; // Maksymalna celność z gwintowaną lufą!
            } else {
                s *= 0.35;
            }
        }
        s *= Math.max(0.40, 1.0 - ((level - 1) * 0.12));
        return s;
    }

    public double getReloadTimeSeconds() {
        return getReloadTicks() / 20.0;
    }

    public int getReloadTicks() {
        double ticks = GunConfigManager.getInstance().getReloadTicks(gunType);
        if (loadedAmmoType == AmmoType.SLUG_CARTRIDGE || loadedAmmoType == AmmoType.DRAGON_SCATTER_SHOT) {
            ticks += 20;
        }
        if (reinforcedLock) ticks *= (1.0 - GunConfigManager.getInstance().getLockReloadReduction());

        if (uniqueMod == GunUniqueMod.MUSKET_INFANTRYMAN) {
            ticks -= GunConfigManager.getInstance().getUniqueInt("musket_infantryman", "reload_reduction_ticks", 20);
        } else if (uniqueMod == GunUniqueMod.PISTOL_WITCH_HUNTER) {
            ticks -= GunConfigManager.getInstance().getUniqueInt("pistol_witch_hunter", "reload_reduction_ticks", 10);
        } else if (uniqueMod == GunUniqueMod.PISTOL_PUNISHER) {
            ticks += 30; // +1.5s czasu ładowania (+30 ticków)
        } else if (uniqueMod == GunUniqueMod.SHOTGUN_DEMOLITION) {
            ticks += GunConfigManager.getInstance().getUniqueInt("shotgun_demolition", "reload_penalty_ticks", 20);
        } else if (uniqueMod == GunUniqueMod.PEPPERBOX_HURRICANE) {
            ticks += GunConfigManager.getInstance().getUniqueInt("pepperbox_hurricane", "reload_penalty_ticks", 16);
        }

        return (int) Math.max(10, ticks);
    }

    // Getters and Setters
    public GunType getGunType() { return gunType; }
    public int getCurrentAmmo() { return currentAmmo; }
    public void setCurrentAmmo(int currentAmmo) {
        this.currentAmmo = Math.max(0, Math.min(getMaxAmmoCapacity(), currentAmmo));
    }
    public AmmoType getLoadedAmmoType() { return loadedAmmoType; }
    public void setLoadedAmmoType(AmmoType loadedAmmoType) { this.loadedAmmoType = loadedAmmoType; }
    public int getCurrentDurability() { return currentDurability; }
    public void setCurrentDurability(int currentDurability) { this.currentDurability = Math.max(0, Math.min(getMaxDurability(), currentDurability)); }
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
    public GunUniqueMod getUniqueMod() { return uniqueMod; }
    public void setUniqueMod(GunUniqueMod uniqueMod) { this.uniqueMod = (uniqueMod != null ? uniqueMod : GunUniqueMod.NONE); }
    public boolean hasUniqueMod() { return uniqueMod != GunUniqueMod.NONE; }
}
