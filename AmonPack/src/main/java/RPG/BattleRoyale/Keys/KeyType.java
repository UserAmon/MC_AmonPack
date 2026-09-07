package RPG.BattleRoyale.Keys;

import org.bukkit.ChatColor;

public enum KeyType {
    POLICE_STATION("Police Station Key", "Posterunek Policji", ChatColor.BLUE, 101),
    PHARMACY("Pharmacy Key", "Apteka Miejska", ChatColor.GREEN, 102),
    MILITARY_DEPOT("Military Depot Key", "Skład Wojskowy", ChatColor.RED, 103),
    APARTMENT_204("Apartment 204 Key", "Apartament 204", ChatColor.GOLD, 104);

    private final String displayName;
    private final String locationName;
    private final ChatColor color;
    private final int customModelData;

    KeyType(String displayName, String locationName, ChatColor color, int customModelData) {
        this.displayName = displayName;
        this.locationName = locationName;
        this.color = color;
        this.customModelData = customModelData;
    }

    public String getDisplayName() { return displayName; }
    public String getLocationName() { return locationName; }
    public ChatColor getColor() { return color; }
    public int getCustomModelData() { return customModelData; }

    public static KeyType fromString(String str) {
        if (str == null) return null;
        for (KeyType type : values()) {
            if (type.name().equalsIgnoreCase(str) || type.displayName.equalsIgnoreCase(str)) {
                return type;
            }
        }
        return null;
    }
}
