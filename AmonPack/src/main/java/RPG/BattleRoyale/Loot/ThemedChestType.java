package RPG.BattleRoyale.Loot;

public enum ThemedChestType {
    FOOD("Spożywczy", "§6Skrzynia z Żywnością", "Jedzenie, chleb, pieczone mięso, złote marchewki i mikstury regeneracji"),
    MEDICAL("Medyczny", "§aSkrzynia Medyczna", "Lekarstwa na infekcję, bandaże, mikstury lecznicze i pożywienie"),
    GUNS("Rusznikarski", "§cSkrzynia z Bronią Palną", "Pistolety, muszkiety, garłacze, amunicja i zestawy ulepszeń"),
    MAGIC("Magiczny", "§dSkrzynia Magiczna", "Zwoje zaklęć, mikstury many i nasycona broń"),
    MELEE("Zbrojownia", "§bSkrzynia z Bronią Białą", "Miecze, topory, tarcze i pancerze ochronne"),
    MIXED("Ogólny", "§eSkrzynia z Łupem", "Losowa mieszanka broni, zasobów, medykamentów i amunicji"),
    RANDOM("Losowy", "§fOpuszczona Skrzynia", "Losowany przy starcie motyw sklepowy");

    private final String id;
    private final String displayName;
    private final String description;

    ThemedChestType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public static ThemedChestType fromString(String str) {
        if (str == null) return MIXED;
        for (ThemedChestType t : values()) {
            if (t.name().equalsIgnoreCase(str) || t.id.equalsIgnoreCase(str)) return t;
        }
        return MIXED;
    }
}
