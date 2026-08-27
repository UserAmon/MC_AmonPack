package RPG.Magic.model;

public enum SpellElement {
    FIRE("§cOgień", "§c🔥"),
    WATER("§bWoda", "§b💧"),
    EARTH("§aZiemia", "§a🌱"),
    AIR("§fPowietrze", "§f💨"),
    LIGHTNING("§ePiorun", "§e⚡"),
    ARCANE("§dArkana", "§d🔮"),
    DARK("§8Ciemność", "§8🌑"),
    LIGHT("§eŚwiatłość", "§e✨");

    private final String displayName;
    private final String symbol;

    SpellElement(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }
}
