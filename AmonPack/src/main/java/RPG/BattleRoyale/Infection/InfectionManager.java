package RPG.BattleRoyale.Infection;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InfectionManager {

    private final Map<UUID, InfectionState> activeInfections = new ConcurrentHashMap<>();
    private final int defaultDurationSeconds;

    public InfectionManager(int defaultDurationSeconds) {
        this.defaultDurationSeconds = defaultDurationSeconds;
    }

    /**
     * Zakaża gracza wirusem, jeśli nie jest jeszcze zainfekowany.
     */
    public boolean infect(Player player) {
        if (player == null || !player.isOnline()) return false;
        UUID uuid = player.getUniqueId();
        if (activeInfections.containsKey(uuid)) return false;

        InfectionState state = new InfectionState(uuid, defaultDurationSeconds);
        activeInfections.put(uuid, state);

        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 0.9f);
        player.spawnParticle(Particle.ENTITY_EFFECT, player.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0);

        player.sendTitle(
                ChatColor.DARK_RED + "☣ ZOSTAŁEŚ ZAINFEKOWANY! ☣",
                ChatColor.GRAY + "Znajdź antidotum w skrzyniach zanim minie czas!",
                10, 50, 20
        );
        player.sendMessage(ChatColor.DARK_RED + "[BattleRoyale] Zostałeś zakażony wirusem zombie! Twój czas ucieka (sprawdź BossBar na górze ekranu).");
        return true;
    }

    /**
     * Ulecza gracza z infekcji za pomocą lekarstwa.
     */
    public boolean cure(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        InfectionState state = activeInfections.remove(uuid);
        if (state != null) {
            state.cleanup();

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.6f);
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.1);
            player.spawnParticle(Particle.HEART, player.getEyeLocation().add(0, 0.3, 0), 5, 0.3, 0.3, 0.3, 0.1);

            player.sendTitle(
                    ChatColor.GREEN + "✔ WYLECZONO! ✔",
                    ChatColor.YELLOW + "Wirus został usunięty z twojego krwioobiegu.",
                    10, 40, 15
            );
            player.sendMessage(ChatColor.GREEN + "[BattleRoyale] Antidotum zadziałało! Jesteś teraz zdrowy i możesz ewakuować się przez portal!");
            return true;
        }
        return false;
    }

    public boolean isInfected(UUID uuid) {
        return activeInfections.containsKey(uuid);
    }

    public boolean isInfected(Player player) {
        return player != null && isInfected(player.getUniqueId());
    }

    public InfectionState getInfection(UUID uuid) {
        return activeInfections.get(uuid);
    }

    /**
     * Skraca pozostały czas życia gracza z infekcją (np. po wybuchu Spuchlaka).
     */
    public void reduceInfectionTime(Player player, int seconds) {
        if (player == null) return;
        InfectionState state = activeInfections.get(player.getUniqueId());
        if (state != null) {
            state.reduceSeconds(seconds);
        }
    }

    /**
     * Taktowanie co sekundę dla wszystkich zakażonych graczy.
     */
    public void tick() {
        Iterator<Map.Entry<UUID, InfectionState>> it = activeInfections.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, InfectionState> entry = it.next();
            boolean killed = entry.getValue().tick();
            if (killed) {
                it.remove();
            }
        }
    }

    public void cleanupAll() {
        for (InfectionState state : activeInfections.values()) {
            state.cleanup();
        }
        activeInfections.clear();
    }
}
