package Abilities.Bending;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import Plugin.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;

public class SpecialTriggerManager {

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
        // Domyślnie dopuszczamy wszystkie zdefiniowane skille autorskie
        return true;
    }
}
