package Abilities.Bending;

/**
 * Interfejs dla umiejętności specjalnych aktywowanych
 * poprzez wyzwalacze (SWAP - F oraz ADVANCEMENT - L).
 */
public interface SpecialTriggerable {

    enum TriggerType {
        SWAP("Zamiana Rąk (F)"),
        ADVANCEMENT("Osiągnięcia (L)"),
        BOTH("Swap (F) / Advancement (L)");

        private final String displayName;

        TriggerType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Zwraca typ wyzwalacza wspierany przez tę umiejętność.
     */
    TriggerType getSupportedTriggerType();

    default boolean isTriggerTypeSupported(TriggerType type) {
        TriggerType supported = getSupportedTriggerType();
        return supported == TriggerType.BOTH || supported == type;
    }
}
