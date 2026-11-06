package RPG.BattleRoyale.Infection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class InfectionState {

    private final UUID playerUuid;
    private final int totalDurationSeconds;
    private int remainingSeconds;
    private BossBar bossBar;
    private int symptomTickCounter = 0;

    public InfectionState(UUID playerUuid, int totalDurationSeconds) {
        this.playerUuid = playerUuid;
        this.totalDurationSeconds = Math.max(30, totalDurationSeconds);
        this.remainingSeconds = this.totalDurationSeconds;

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            this.bossBar = Bukkit.createBossBar(
                    getBossBarTitle(),
                    BarColor.GREEN,
                    BarStyle.SEGMENTED_10
            );
            this.bossBar.setProgress(1.0);
            this.bossBar.addPlayer(player);
            this.bossBar.setVisible(true);
        }
    }

    /**
     * Wywoływane co 1 sekundę (20 ticków).
     * @return true jeśli infekcja zabiła gracza (czas upłynął).
     */
    public boolean tick() {
        remainingSeconds--;
        symptomTickCounter++;

        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline() || player.isDead()) {
            return false;
        }

        // Aktualizacja BossBara
        double progress = Math.max(0.0, Math.min(1.0, (double) remainingSeconds / totalDurationSeconds));
        if (bossBar != null) {
            bossBar.setProgress(progress);
            bossBar.setTitle(getBossBarTitle());

            if (progress > 0.6) {
                bossBar.setColor(BarColor.GREEN);
            } else if (progress > 0.25) {
                bossBar.setColor(BarColor.YELLOW);
            } else {
                bossBar.setColor(BarColor.RED);
            }
        }

        // Cząsteczki zakażenia wokół głowy gracza
        if (symptomTickCounter % 3 == 0) {
            Location eye = player.getEyeLocation().add(0, 0.2, 0);
            player.getWorld().spawnParticle(Particle.ENTITY_EFFECT, eye, 2, 0.2, 0.2, 0.2, 0);
        }

        // Etapy objawów
        double ratio = (double) remainingSeconds / totalDurationSeconds;

        if (ratio > 0.70) {
            // Etap 1: Bezobjawowy (inkubacja)
        } else if (ratio > 0.40) {
            // Etap 2: Lekkie spowolnienie (Slowness I)
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false, false));
            if (symptomTickCounter % 25 == 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.8f, 0.8f);
            }
        } else if (ratio > 0.10) {
            // Etap 3: Umiarkowane spowolnienie (Slowness II) + okresowa ślepota i nudności
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, false));

            if (symptomTickCounter % 15 == 0) {
                // Chwilowa ślepota (1.5s) i nudności (4s)
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0, false, false, false));
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 0.9f, 0.7f);
                player.sendMessage(ChatColor.DARK_RED + "☠ [Infekcja] Tracisz wzrok i kręci Ci się w głowie! Znajdź lekarstwo!");
            }
        } else {
            // Etap 4: Stan krytyczny (Slowness III) + przyspieszone bicie serca
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false, false));

            if (symptomTickCounter % 2 == 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, 0.5f);
            }
        }

        if (remainingSeconds <= 0) {
            // Śmierć z infekcji
            cleanup();
            player.sendMessage(ChatColor.DARK_RED + "☠ Infekcja zombie całkowicie przejęła kontrolę nad Twoim ciałem!");
            player.setHealth(0.0);
            return true;
        }

        return false;
    }

    private String getBossBarTitle() {
        int min = Math.max(0, remainingSeconds / 60);
        int sec = Math.max(0, remainingSeconds % 60);
        String timeStr = String.format("%02d:%02d", min, sec);
        return ChatColor.DARK_RED + "" + ChatColor.BOLD + "WIRUS ZOMBIE: " + ChatColor.YELLOW + timeStr
                + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + "Wypij Lekarstwo!";
    }

    public void cleanup() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.NAUSEA);
        }
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public void reduceSeconds(int amount) { this.remainingSeconds = Math.max(0, this.remainingSeconds - amount); }
}
