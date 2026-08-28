package CustomContent.Guns;

import org.bukkit.Material;

public enum GunType {
    FLINTLOCK_PISTOL(
            "flintlock_pistol",
            "§6§lPistolet Skałkowy",
            Material.IRON_HOE,
            10050,
            5.5,
            1.5,
            1,
            36, // 1.8s (36 ticków)
            25.0,
            0.17, // 2x większa losowość / rozrzut bazowy
            150, // durability
            AmmoType.LEAD_BULLET,
            false
    ),
    FLINTLOCK_MUSKET(
            "flintlock_musket",
            "§e§lMuszkiet Piechoty",
            Material.IRON_HOE,
            10051,
            11.0,
            1.5,
            1,
            60, // 3.0s (60 ticków)
            60.0,
            0.11, // rozrzut z biodra (ciężko trafić bez celowania)
            250,
            AmmoType.LEAD_BULLET,
            true
    ),
    BLUNDERBUSS(
            "blunderbuss",
            "§c§lGarłacz Rozpylający",
            Material.IRON_HOE,
            10052,
            1.5, // 8-10 x 1.5 = 12.0 - 15.0 dla śrutu
            1.5,
            1,
            50, // 2.5s (50 ticków)
            14.0,
            0.40, // potężny, bardzo szeroki stożek śrutu
            200,
            AmmoType.SCATTER_SHOT,
            false
    ),
    PEPPERBOX(
            "pepperbox",
            "§b§lPieprzniczka Obrotowa",
            Material.IRON_HOE,
            10053,
            4.5,
            1.5,
            4, // 4 komory
            15, // 0.75s na komorę
            20.0,
            0.22, // duży rozrzut w szybkiej serii
            220,
            AmmoType.LEAD_BULLET,
            false
    );

    private final String id;
    private final String displayName;
    private final Material baseMaterial;
    private final int customModelData;
    private final double baseDamage;
    private final double headshotMultiplier;
    private final int maxAmmo;
    private final int reloadTicks;
    private final double maxRange;
    private final double baseSpread;
    private final int maxDurability;
    private final AmmoType requiredAmmoType;
    private final boolean supportsBayonet;

    GunType(String id, String displayName, Material baseMaterial, int customModelData,
            double baseDamage, double headshotMultiplier, int maxAmmo, int reloadTicks,
            double maxRange, double baseSpread, int maxDurability, AmmoType requiredAmmoType,
            boolean supportsBayonet) {
        this.id = id;
        this.displayName = displayName;
        this.baseMaterial = baseMaterial;
        this.customModelData = customModelData;
        this.baseDamage = baseDamage;
        this.headshotMultiplier = headshotMultiplier;
        this.maxAmmo = maxAmmo;
        this.reloadTicks = reloadTicks;
        this.maxRange = maxRange;
        this.baseSpread = baseSpread;
        this.maxDurability = maxDurability;
        this.requiredAmmoType = requiredAmmoType;
        this.supportsBayonet = supportsBayonet;
    }

    public boolean isCompatibleAmmo(AmmoType ammo) {
        if (ammo == null) return false;
        if (this == BLUNDERBUSS) {
            return ammo == AmmoType.SCATTER_SHOT || ammo == AmmoType.SLUG_CARTRIDGE || ammo == AmmoType.DRAGON_SCATTER_SHOT;
        }
        return ammo == AmmoType.LEAD_BULLET || ammo == AmmoType.DRAGON_CARTRIDGE;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getBaseMaterial() { return baseMaterial; }
    public int getCustomModelData() { return customModelData; }
    public double getBaseDamage() { return baseDamage; }
    public double getHeadshotMultiplier() { return headshotMultiplier; }
    public int getMaxAmmo() { return maxAmmo; }
    public int getReloadTicks() { return reloadTicks; }
    public double getMaxRange() { return maxRange; }
    public double getBaseSpread() { return baseSpread; }
    public int getMaxDurability() { return maxDurability; }
    public AmmoType getRequiredAmmoType() { return requiredAmmoType; }
    public boolean isSupportsBayonet() { return supportsBayonet; }

    public static GunType fromId(String id) {
        if (id == null) return null;
        for (GunType gt : values()) {
            if (gt.id.equalsIgnoreCase(id)) return gt;
        }
        return null;
    }
}
