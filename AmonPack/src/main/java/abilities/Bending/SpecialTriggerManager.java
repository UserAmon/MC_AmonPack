package Abilities.Bending;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import Plugin.AmonPackPlugin;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpecialTriggerManager {

    private static final Set<UUID> activeSpecialPlayers = new HashSet<>();

    public static void registerActiveSpecial(Player player) {
        if (player != null) {
            activeSpecialPlayers.add(player.getUniqueId());
        }
    }

    public static void unregisterActiveSpecial(Player player) {
        if (player != null) {
            activeSpecialPlayers.remove(player.getUniqueId());
        }
    }

    public static boolean isSpecialActive(Player player) {
        if (player == null) return false;
        // Sprawdzamy czy gracz jest na naszej liście lub czy posiada aktywną instancję skilla z SpecialTriggerable
        if (activeSpecialPlayers.contains(player.getUniqueId())) {
            return true;
        }
        for (CoreAbility ability : CoreAbility.getAbilities(player, CoreAbility.class)) {
            if (ability instanceof SpecialTriggerable) {
                return true;
            }
        }
        return false;
    }

    /**
     * Uniwersalna metoda aktywująca umiejętność specjalną przypisaną do danego wyzwalacza (SWAP lub DROP).
     */
    public static boolean executeSpecialAbility(Player player, String abilityName, SpecialTriggerable.TriggerType triggerType) {
        if (abilityName == null || abilityName.isEmpty()) {
            return false;
        }

        PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
        if (branch == null) {
            return false;
        }

        // Weryfikacja czy gracz posiada odblokowaną daną umiejętność
        if (!branch.getUnlockedAbilities().contains(abilityName) && !branch.hasUpgrade(abilityName)) {
            return false;
        }

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) {
            return false;
        }

        // Pobranie instancji CoreAbility ProjectKorra
        CoreAbility ability = CoreAbility.getAbility(abilityName);
        if (ability == null) {
            return false;
        }

        // Sprawdzenie czy gracz może używać magii (czy żywioł się zgadza i czy nie ma blokady)
        if (!bPlayer.canBendIgnoreBinds(ability)) {
            return false;
        }

        // Sprawdzenie cooldownu
        if (bPlayer.isOnCooldown(ability)) {
            return false;
        }

        // Próba uruchomienia umiejętności za pomocą refleksji (konstruktor z argumentem Player)
        try {
            Class<?> clazz = ability.getClass();
            Constructor<?> constructor = clazz.getConstructor(Player.class);
            constructor.newInstance(player);
            return true;
        } catch (Exception e) {
            System.err.println("[AmonPack] Błąd podczas aktywacji specjalnego skilla " + abilityName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Sprawdza czy dana umiejętność wspiera dany typ wyzwalacza (SWAP/DROP).
     */
    public static boolean isTriggerSupported(String abilityName, SpecialTriggerable.TriggerType triggerType) {
        if (abilityName == null || abilityName.isEmpty()) {
            return false;
        }
        CoreAbility ability = CoreAbility.getAbility(abilityName);
        if (ability instanceof SpecialTriggerable) {
            SpecialTriggerable st = (SpecialTriggerable) ability;
            return st.isTriggerTypeSupported(triggerType);
        }
        return true;
    }
}
