package Abilities.Bending;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.SubElement;
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
     * tak aby nie były używalne podczas trwania skilla specjalnego.
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
     * Zapobiega otwieraniu waniliowego menu osiągnięć.
     */
    public static void registerAdvancementPacketListener() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(
                    new com.comphenix.protocol.events.PacketAdapter(
                        AmonPackPlugin.plugin,
                        com.comphenix.protocol.events.ListenerPriority.HIGHEST,
                        com.comphenix.protocol.PacketType.Play.Client.ADVANCEMENTS
                    ) {
                        @Override
                        public void onPacketReceiving(com.comphenix.protocol.events.PacketEvent event) {
                            Player player = event.getPlayer();
                            if (player == null) return;

                            event.setCancelled(true);

                            Bukkit.getScheduler().runTask(AmonPackPlugin.plugin, () -> {
                                player.closeInventory();
                            });

                            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                                if (player.isOnline()) {
                                    player.closeInventory();
                                }
                            }, 1L);
                        }
                    }
                );
            }
        } catch (Throwable t) {
            System.err.println("[AmonPack] Nie udało się zarejestrować ProtocolLib PacketListenera dla klawisza L: " + t.getMessage());
        }
    }

    /**
     * Weryfikuje czy gracz w jakimkolwiek trybie gry (w tym w Survival GM 0) może użyć skilla specjalnego.
     */
    public static boolean canPlayerBendSpecial(BendingPlayer bPlayer, CoreAbility ability) {
        if (bPlayer == null || ability == null) return false;
        Player player = bPlayer.getPlayer();
        if (player == null || !player.isOnline() || player.isDead()) return false;
        if (bPlayer.isChiBlocked() || bPlayer.isParalyzed() || Abilities.PK_Abilities.Chi.ChiManager.isParalyzed(player)) return false;
        if (!bPlayer.isToggled() || !bPlayer.isElementToggled(ability.getElement())) return false;

        Element mainElement = ability.getElement();
        if (mainElement instanceof SubElement) {
            mainElement = ((SubElement) mainElement).getParentElement();
        }
        return bPlayer.hasElement(mainElement) || bPlayer.hasElement(ability.getElement());
    }

    /**
     * Uniwersalna metoda aktywująca umiejętność specjalną przypisaną do wyzwalacza SWAP (F).
     */
    public static boolean executeSpecialAbility(Player player, String abilityName, SpecialTriggerable.TriggerType triggerType) {
        if (abilityName == null || abilityName.isEmpty()) {
            return false;
        }

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        if (branch == null) {
            return false;
        }

        boolean isUnlocked = abilityName.equalsIgnoreCase(branch.getSwapAbility())
                || branch.hasUpgrade(abilityName)
                || (branch.getUnlockedAbilities() != null && branch.getUnlockedAbilities().stream().anyMatch(a -> a.equalsIgnoreCase(abilityName)));
        if (!isUnlocked) {
            return false;
        }

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) {
            return false;
        }

        for (CoreAbility activeAbility : CoreAbility.getAbilities(player, CoreAbility.class)) {
            if (activeAbility != null && !(activeAbility instanceof SpecialTriggerable)) {
                return false;
            }
        }

        CoreAbility ability = CoreAbility.getAbility(abilityName);
        if (ability == null) {
            return false;
        }

        // Poprawiona weryfikacja działająca poprawnie w GM 0 (Survival) oraz GM 1 (Creative)
        if (!canPlayerBendSpecial(bPlayer, ability)) {
            return false;
        }

        if (bPlayer.isOnCooldown(ability)) {
            return false;
        }

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
