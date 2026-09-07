package CustomContent.Guns;

public enum GunUniqueMod {
    NONE("none", "Brak", null),

    // Muszkiet
    MUSKET_STALKER("musket_stalker", "§2§lStalker", GunType.FLINTLOCK_MUSKET),
    MUSKET_INFANTRYMAN("musket_infantryman", "§e§lPiechur", GunType.FLINTLOCK_MUSKET),

    // Pistolet
    PISTOL_FAST_AND_FURIOUS("pistol_fast_and_furious", "§a§lSzybki i Wściekły", GunType.FLINTLOCK_PISTOL),
    PISTOL_WITCH_HUNTER("pistol_witch_hunter", "§5§lŁowca Czarownic", GunType.FLINTLOCK_PISTOL),
    PISTOL_EMOTIONAL_SUPPORT("pistol_emotional_support", "§d§lWsparcie Emocjonalne", GunType.FLINTLOCK_PISTOL),
    PISTOL_PUNISHER("pistol_punisher", "§c§lPunisher", GunType.FLINTLOCK_PISTOL),

    // Garłacz
    SHOTGUN_DOUBLE_BARREL("shotgun_double_barrel", "§6§lDubeltówka", GunType.BLUNDERBUSS),
    SHOTGUN_DEMOLITION("shotgun_demolition", "§c§lDemolka", GunType.BLUNDERBUSS),

    // Pieprzniczka
    PEPPERBOX_PERFECT_SOLDIER("pepperbox_perfect_soldier", "§b§lŻołnierz Doskonały", GunType.PEPPERBOX),
    PEPPERBOX_HURRICANE("pepperbox_hurricane", "§3§lHuragan", GunType.PEPPERBOX);

    private final String id;
    private final String displayName;
    private final GunType compatibleGun;

    GunUniqueMod(String id, String displayName, GunType compatibleGun) {
        this.id = id;
        this.displayName = displayName;
        this.compatibleGun = compatibleGun;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public GunType getCompatibleGun() { return compatibleGun; }

    public static GunUniqueMod fromId(String id) {
        if (id == null) return NONE;
        for (GunUniqueMod m : values()) {
            if (m.id.equalsIgnoreCase(id) || m.name().equalsIgnoreCase(id)) return m;
        }
        return NONE;
    }
}
