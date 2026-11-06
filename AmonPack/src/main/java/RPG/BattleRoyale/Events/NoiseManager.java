package RPG.BattleRoyale.Events;

import RPG.BattleRoyale.Noise.NoiseEvent;
import RPG.BattleRoyale.Zombies.CustomZombieManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralny menedżer systemu hałasu i „echa” na mapie.
 * Rejestruje pojedyncze zdarzenia dźwiękowe (NoiseEvent) oraz poziom hałasu graczy.
 * Zombie w zasięgu słuchu zapamiętują lokalizację hałasu (bez wallhacka) i idą zbadać źródło.
 */
public class NoiseManager {

    private final Map<UUID, Double> playerNoise = new ConcurrentHashMap<>();
    private final List<NoiseEvent> activeNoiseEvents = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    private double silenceMultiplier = 1.0;
    private double bloodMoonMultiplier = 1.0;

    public void setSilenceMultiplier(double mult) {
        this.silenceMultiplier = Math.max(1.0, mult);
    }

    public void setBloodMoonMultiplier(double mult) {
        this.bloodMoonMultiplier = Math.max(1.0, mult);
    }

    /**
     * Rejestruje zdarzenie hałasu w świecie gry.
     */
    public NoiseEvent recordNoise(Location loc, double baseIntensity, double baseRadius, String sourceType, Entity sourceEntity, CustomZombieManager zombieManager) {
        if (loc == null || loc.getWorld() == null) return null;

        double finalIntensity = Math.min(100.0, baseIntensity * silenceMultiplier);
        double finalRadius = baseRadius * silenceMultiplier * bloodMoonMultiplier;

        NoiseEvent event = new NoiseEvent(loc, finalIntensity, finalRadius, sourceType, sourceEntity, 8000L);
        activeNoiseEvents.add(event);

        // Efekty cząsteczkowe rozchodzenia się fali dźwiękowej
        if (finalIntensity >= 30.0) {
            loc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, loc.clone().add(0, 0.8, 0), 4, 0.2, 0.2, 0.2, 0.05);
        }

        // Alertowanie zombie w zasięgu słuchu – zapamiętują lokalizację źródła bez wallhacka!
        if (zombieManager != null) {
            propagateEchoToMonsters(event, zombieManager);
        }

        return event;
    }

    /**
     * Informuje potwory w promieniu słyszalności o wystąpieniu hałasu.
     */
    private void propagateEchoToMonsters(NoiseEvent event, CustomZombieManager zombieManager) {
        Location noiseLoc = event.getLocation();
        World world = noiseLoc.getWorld();
        if (world == null) return;

        double radiusSq = event.getRadius() * event.getRadius();

        for (LivingEntity entity : world.getLivingEntities()) {
            if (!(entity instanceof Mob mob) || !mob.isValid() || mob.isDead()) continue;
            if (!(mob instanceof Monster)) continue;

            // Jeśli mob już walczy bezpośrednio w zwarciu (< 4 kratek od celu), nie przerywamy walki
            if (mob.getTarget() != null && mob.getLocation().distanceSquared(mob.getTarget().getLocation()) <= 16.0) {
                continue;
            }

            if (mob.getLocation().distanceSquared(noiseLoc) <= radiusSq) {
                // Przekazanie informacji o hałasie do pamięci AI zombie
                zombieManager.onMonsterHearNoise(mob, noiseLoc, event.getIntensity());
            }
        }
    }

    public void addNoise(Player player, double amount) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        double current = playerNoise.getOrDefault(uuid, 0.0);
        double updated = Math.min(100.0, current + (amount * silenceMultiplier));
        playerNoise.put(uuid, updated);

        if (amount >= 20.0) {
            player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.9f, 1.4f);
            player.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, player.getLocation().add(0, 0.8, 0), 6, 0.3, 0.3, 0.3, 0.05);
        }
    }

    public double getNoise(UUID uuid) {
        return playerNoise.getOrDefault(uuid, 0.0);
    }

    public void setNoise(UUID uuid, double noise) {
        playerNoise.put(uuid, Math.max(0.0, Math.min(100.0, noise)));
    }

    /**
     * Taktowanie co 1 sekundę z pętli gry: oczyszcza stare eventy i aktualizuje UI graczy.
     */
    public void tick(World world, List<Player> players, CustomZombieManager zombieManager, boolean isSilenceActive) {
        // 1. Usunięcie wygasłych zdarzeń hałasu
        activeNoiseEvents.removeIf(NoiseEvent::isExpired);

        if (players == null || players.isEmpty()) return;

        for (Player player : players) {
            if (player == null || !player.isOnline() || player.isDead()) continue;
            UUID uuid = player.getUniqueId();
            double noise = playerNoise.getOrDefault(uuid, 0.0);

            // Spadek/przyrost hałasu gracza
            if (player.isSprinting()) {
                noise = Math.min(100.0, noise + (6.0 * silenceMultiplier));
            } else if (player.isSneaking()) {
                noise = Math.max(0.0, noise - 12.0);
            } else {
                noise = Math.max(0.0, noise - 6.0);
            }

            playerNoise.put(uuid, noise);

            if (isSilenceActive) {
                // Wyświetlenie paska hałasu na Action Barze tylko gdy aktywny jest event ciszy
                String bar = buildNoiseBar(noise);
                ChatColor color = noise > 75.0 ? ChatColor.DARK_RED : noise > 45.0 ? ChatColor.GOLD : ChatColor.GREEN;
                String text = color + "Hałas: " + bar + " " + ChatColor.WHITE + (int) noise + "%";
                if (noise > 80.0) {
                    text += ChatColor.RED + " ⚠ ZOMBIE SŁYSZĄ CIĘ!";
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
            }

            // Bardzo wysoki hałas alarmuje bezpośrednio
            if (noise >= 75.0) {
                recordNoise(player.getLocation(), noise, 35.0, "PLAYER_LOUD", player, zombieManager);
            }
        }
    }

    private String buildNoiseBar(double noise) {
        int total = 10;
        int filled = (int) Math.round((noise / 100.0) * total);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                if (i >= 7) sb.append(ChatColor.RED);
                else if (i >= 4) sb.append(ChatColor.YELLOW);
                else sb.append(ChatColor.GREEN);
                sb.append("▮");
            } else {
                sb.append(ChatColor.DARK_GRAY).append("▯");
            }
        }
        return "[" + sb + ChatColor.RESET + "]";
    }

    public List<NoiseEvent> getActiveNoiseEvents() {
        return Collections.unmodifiableList(activeNoiseEvents);
    }

    public void cleanup() {
        playerNoise.clear();
        activeNoiseEvents.clear();
        silenceMultiplier = 1.0;
        bloodMoonMultiplier = 1.0;
    }
}
