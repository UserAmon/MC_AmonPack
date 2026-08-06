package Abilities.Bending;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
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
            applySoftCooldownToToolbar(player);
        }
    }

    public static void unregisterActiveSpecial(Player player) {
        if (player != null) {
            activeSpecialPlayers.remove(player.getUniqueId());
        }
    }

    public static boolean isSpecialActive(Player player) {
        if (player == null) return false;
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
     * Nakłada lekki 0.1s (100ms) cooldown na nieużywane umiejętności z paska toolbaru,
     * tak aby nie były używalne podczas trwania skilla specjalnego (nie nadpisując długich CD).
     */
    public static void applySoftCooldownToToolbar(Player player) {
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;
        for (String boundAbi : bPlayer.getAbilities().values()) {
            if (boundAbi != null && !boundAbi.isEmpty()) {
                if (!bPlayer.isOnCooldown(boundAbi)) {
                    bPlayer.addCooldown(boundAbi, 100);
                }
            }
        }
    }

    /**
     * Rejestruje ProtocolLib PacketListener przechwytujący wciśnięcie klawisza L (ADVANCEMENT_TAB).
     */
    public static void registerAdvancementPacketListener() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(
                    new com.comphenix.protocol.events.PacketAdapter(
                        AmonPackPlugin.plugin,
                        com.comphenix.protocol.events.ListenerPriority.HIGH,
                        com.comphenix.protocol.PacketType.Play.Client.ADVANCEMENTS
                    ) {
                        @Override
                        public void onPacketReceiving(com.comphenix.protocol.events.PacketEvent event) {
                            Player player = event.getPlayer();
                            if (player == null || AmonPackPlugin.levelsBending == null) return;

                            PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
                            if (branch != null) {
                                String advAbi = branch.getDropAbility(); // Wykorzystujemy 2. slot specjalny w bazie DB dla L
                                if (advAbi != null && !advAbi.isEmpty()) {
                                    event.setCancelled(true);
                                    Bukkit.getScheduler().runTask(AmonPackPlugin.plugin, () -> {
                                        executeSpecialAbility(player, advAbi, SpecialTriggerable.TriggerType.ADVANCEMENT);
                                    });
                                }
                            }
                        }
                    }
                );
                System.out.println("[AmonPack] Zarejestrowano ProtocolLib PacketListener dla klawisza L (ADVANCEMENT_TAB)!");
            }
        } catch (Throwable t) {
            System.err.println("[AmonPack] Nie udało się zarejestrować ProtocolLib PacketListenera dla klawisza L: " + t.getMessage());
        }
    }

    /**
     * Uniwersalna metoda aktywująca umiejętność specjalną przypisaną do wyzwalacza SWAP (F) lub ADVANCEMENT (L).
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

        // Jeśli jakakolwiek umiejętność z paska toolbaru jest już aktywna, zablokuj uruchomienie skilla specjalnego
        for (CoreAbility activeAbility : CoreAbility.getAbilities(player, CoreAbility.class)) {
            if (activeAbility != null && !(activeAbility instanceof SpecialTriggerable)) {
                return false;
            }
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
            applySoftCooldownToToolbar(player);
            return true;
        } catch (Exception e) {
            System.err.println("[AmonPack] Błąd podczas aktywacji specjalnego skilla " + abilityName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Sprawdza czy dana umiejętność wspiera dany typ wyzwalacza.
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
