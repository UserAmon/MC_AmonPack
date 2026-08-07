package Abilities.Bending;

/**
 * Interfejs dla umiejętności specjalnych aktywowanych
 * poprzez wyzwalacze (SWAP - F oraz ADVANCEMENT - L).
 */
public interface SpecialTriggerable {

    enum TriggerType {
        SWAP("Zamiana Rąk (F)");

        private final String displayName;

        TriggerType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    TriggerType getSupportedTriggerType();

    default boolean isTriggerTypeSupported(TriggerType type) {
        return type == TriggerType.SWAP;
    }
}
