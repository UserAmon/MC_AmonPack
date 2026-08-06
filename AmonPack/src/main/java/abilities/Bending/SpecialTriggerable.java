package Abilities.Bending;

/**
 * Interfejs dla umiejętności, które mogą być przypisywane i aktywowane
 * poprzez specjalne wyzwalacze (SWAP - klawisz F, DROP - klawisz Q).
 */
public interface SpecialTriggerable {

    enum TriggerType {
        SWAP("Zamiana Rąk (F)"),
        DROP("Wyrzucenie (Q)"),
        BOTH("Swap (F) / Drop (Q)");

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

    /**
     * Sprawdza czy dany typ wyzwalacza jest obsługiwany przez tę moc.
     */
    default boolean isTriggerTypeSupported(TriggerType type) {
        TriggerType supported = getSupportedTriggerType();
        if (supported == TriggerType.BOTH) {
            return true;
        }
        return supported == type;
    }
}
