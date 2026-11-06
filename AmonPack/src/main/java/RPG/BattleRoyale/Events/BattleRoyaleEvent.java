package RPG.BattleRoyale.Events;

public enum BattleRoyaleEvent {
    NONE("Brak", "Standardowe warunki pogodowe i atmosferyczne"),
    SILENCE("Strefa Ciszy", "Moby reagują na najmniejszy hałas! Bieganie, skakanie i wystrzały przyciągają hordy zombie."),
    DEHYDRATION("Fala Upałów (Odwodnienie)", "Ekstremalna susza! Gracze muszą dbać o nawodnienie pijąc z rzek lub szukając butelek wody.");

    private final String displayName;
    private final String description;

    BattleRoyaleEvent(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
