package RPG.BattleRoyale;

import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BattleRoyaleLobby {

    private final BattleRoyaleArena arena;
    private final BattleRoyaleManager manager;
    private final Set<UUID> lobbyPlayers = new LinkedHashSet<>();
    private final UUID hostUuid;
    private int secondsLeft;
    private BukkitTask countdownTask;
    private boolean starting = false;

    public BattleRoyaleLobby(BattleRoyaleArena arena, BattleRoyaleManager manager, Player host) {
        this.arena = arena;
        this.manager = manager;
        this.hostUuid = host != null ? host.getUniqueId() : null;
        this.secondsLeft = arena.getLobbyDurationSeconds();

        if (host != null) {
            lobbyPlayers.add(host.getUniqueId());
        }
    }

    public void start() {
        broadcastLobbyInvite();

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (starting) {
                    cancel();
                    return;
                }

                secondsLeft--;

                // Przypomnienia na czacie
                if (secondsLeft == 30 || secondsLeft == 15 || secondsLeft == 5) {
                    broadcastReminder();
                }

                // Dźwięk odliczania dla graczy w lobby
                if (secondsLeft <= 5 && secondsLeft > 0) {
                    for (UUID uuid : lobbyPlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }

                // Koniec czasu lub pełne lobby
                if (secondsLeft <= 0 || lobbyPlayers.size() >= arena.getMaxPlayers()) {
                    cancel();
                    tryStartMatch();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 20L, 20L);
    }

    public boolean addPlayer(Player player) {
        if (starting) {
            player.sendMessage(ChatColor.RED + "[BattleRoyale] Gra już wystartowała!");
            return false;
        }
        if (lobbyPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "[BattleRoyale] Już jesteś w lobby gry!");
            return false;
        }
        if (lobbyPlayers.size() >= arena.getMaxPlayers()) {
            player.sendMessage(ChatColor.RED + "[BattleRoyale] Lobby jest pełne (" + arena.getMaxPlayers() + "/" + arena.getMaxPlayers() + ")!");
            return false;
        }

        lobbyPlayers.add(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "[BattleRoyale] Dołączyłeś do lobby gry! Oczekiwanie na start...");

        broadcastMessage(ChatColor.YELLOW + "[BattleRoyale] Gracz " + ChatColor.WHITE + player.getName()
                + ChatColor.YELLOW + " dołączył do gry (" + lobbyPlayers.size() + "/" + arena.getMaxPlayers() + ")!");

        // Jeśli osiągnięto limit graczy, start natychmiast
        if (lobbyPlayers.size() >= arena.getMaxPlayers()) {
            tryStartMatch();
        }
        return true;
    }

    public boolean removePlayer(Player player) {
        if (lobbyPlayers.remove(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "[BattleRoyale] Opuściłeś lobby gry.");
            broadcastMessage(ChatColor.GRAY + "[BattleRoyale] Gracz " + player.getName() + " opuścił lobby (" + lobbyPlayers.size() + ").");

            if (lobbyPlayers.isEmpty()) {
                cancelLobby("Wszyscy gracze opuścili lobby.");
            }
            return true;
        }
        return false;
    }

    public void forceStart() {
        if (countdownTask != null) countdownTask.cancel();
        tryStartMatch();
    }

    private void tryStartMatch() {
        if (starting) return;
        starting = true;

        // Sprawdzenie minimalnej liczby graczy (domyślnie min 1, więc można grać solo!)
        if (lobbyPlayers.size() < arena.getMinPlayers()) {
            cancelLobby("Zbyt mało graczy, aby rozpocząć grę (wymagane min: " + arena.getMinPlayers() + ").");
            return;
        }

        List<Player> validPlayers = new ArrayList<>();
        for (UUID uuid : lobbyPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                validPlayers.add(p);
            }
        }

        if (validPlayers.isEmpty()) {
            cancelLobby("Brak dostępnych graczy online.");
            return;
        }

        manager.launchGame(arena, validPlayers);
    }

    public void cancelLobby(String reason) {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        broadcastMessage(ChatColor.RED + "[BattleRoyale] Lobby zostało anulowane: " + reason);
        lobbyPlayers.clear();
        manager.clearLobby();
    }

    private void broadcastLobbyInvite() {
        Player host = hostUuid != null ? Bukkit.getPlayer(hostUuid) : null;
        String hostName = host != null ? host.getName() : "Serwer";

        TextComponent line1 = new TextComponent("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        TextComponent line2 = new TextComponent("§e§l[BATTLE ROYALE] §fGracz §a" + hostName + " §fstworzył nowe lobby gry!\n");
        TextComponent line3 = new TextComponent("§7Tryb: §cPvPvE Zombie Apocalypse §8| §7Czas na dołączenie: §e" + secondsLeft + "s\n");

        TextComponent joinButton = new TextComponent("§a§l  [➤ KLIKNIJ TUTAJ, ABY DOŁĄCZYĆ DO GRY]  \n");
        joinButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hungergames join"));
        joinButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aKliknij, aby dołączyć do rozgrywki!").create()));

        TextComponent line4 = new TextComponent("§7Lub wpisz na czacie: §f/hungergames join\n");
        TextComponent line5 = new TextComponent("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        TextComponent fullMessage = new TextComponent("");
        fullMessage.addExtra(line1);
        fullMessage.addExtra(line2);
        fullMessage.addExtra(line3);
        fullMessage.addExtra(joinButton);
        fullMessage.addExtra(line4);
        fullMessage.addExtra(line5);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(fullMessage);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        }
    }

    private void broadcastReminder() {
        TextComponent reminder = new TextComponent("§6[BattleRoyale] §eLobby startuje za §c" + secondsLeft + "s§e! Graczy: §f"
                + lobbyPlayers.size() + "/" + arena.getMaxPlayers() + " ");
        TextComponent joinBtn = new TextComponent("§a§l[DOŁĄCZ]");
        joinBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hungergames join"));
        joinBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aDołącz teraz!").create()));
        reminder.addExtra(joinBtn);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(reminder);
        }
    }

    private void broadcastMessage(String msg) {
        for (UUID uuid : lobbyPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    public Set<UUID> getLobbyPlayers() { return lobbyPlayers; }
    public boolean containsPlayer(UUID uuid) { return lobbyPlayers.contains(uuid); }
    public int getSecondsLeft() { return secondsLeft; }
}
