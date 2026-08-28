package CustomContent.Guns;

import org.bukkit.Material;

public enum AmmoType {
    LEAD_BULLET(
            "lead_bullet",
            "§fOłowiana Kula Muszkietowa",
            Material.IRON_NUGGET,
            10060,
            "§7Standardowa ciężka kula ołowiana do pistoletów i muszkietów."
    ),
    SCATTER_SHOT(
            "scatter_shot",
            "§ePakiet Śrutu Ołowianego",
            Material.IRON_NUGGET,
            10061,
            "§7Woreczek zawierający 8 ołowianych śrucin dedykowany do Garłacza."
    ),
    DRAGON_CARTRIDGE(
            "dragon_cartridge",
            "§cKula Zapalająca (Smoczy Oddech)",
            Material.IRON_NUGGET,
            10062,
            "§7Nasycona siarką kula podpalająca wrogów i podłoże w miejscu uderzenia."
    ),
    SLUG_CARTRIDGE(
            "slug_cartridge",
            "§6Ciężki Pocisk Brenek (Slug)",
            Material.IRON_NUGGET,
            10063,
            "§7Ciężki monolityczny pocisk do Garłacza zadający potężne obrażenia."
    ),
    DRAGON_SCATTER_SHOT(
            "dragon_scatter_shot",
            "§cSmoczy Śrut Rozpylający",
            Material.IRON_NUGGET,
            10064,
            "§7Nasycony siarką pakiet 10 płonących śrucin do Garłacza o zwiększonym zasięgu."
    );

    private final String id;
    private final String displayName;
    private final Material material;
    private final int customModelData;
    private final String description;

    AmmoType(String id, String displayName, Material material, int customModelData, String description) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public String getDescription() { return description; }

    public static AmmoType fromId(String id) {
        if (id == null) return null;
        if (id.equalsIgnoreCase("slug_cartide")) return SLUG_CARTRIDGE;
        if (id.equalsIgnoreCase("dragon_scatter")) return DRAGON_SCATTER_SHOT;
        for (AmmoType at : values()) {
            if (at.id.equalsIgnoreCase(id)) return at;
        }
        return null;
    }
}
