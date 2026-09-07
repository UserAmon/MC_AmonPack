package RPG.BattleRoyale.Events;

import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Zombies.CustomZombieManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zarządza poziomem hałasu generowanego przez graczy podczas ewenty "Strefa Ciszy" (SILENCE).
 * Hałas generują: sprint, skoki, ataki wręcz, wystrzały z broni palnej i niszczenie bloków.
 * Wysoki hałas alarmuje okoliczne potwory oraz powoduje natychmiastowe spawnowanie agresywnych zombie!
 */
public class NoiseManager {

    private final Map<UUID, Double> playerNoise = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void addNoise(Player player, double amount) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        double current = playerNoise.getOrDefault(uuid, 0.0);
        double updated = Math.min(100.0, current + amount);
        playerNoise.put(uuid, updated);

        // Odtwórz subtelny dźwięk wibracji jeśli duży skok hałasu (np. wystrzał)
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
     * Taktowanie co 1 sekundę z pętli gry.
     */
    public void tick(World world, List<Player> players, CustomZombieManager zombieManager) {
        if (players == null || players.isEmpty()) return;

        for (Player player : players) {
            if (player == null || !player.isOnline() || player.isDead()) continue;
            UUID uuid = player.getUniqueId();
            double noise = playerNoise.getOrDefault(uuid, 0.0);

            // Sprawdzenie akcji gracza
            if (player.isSprinting()) {
                noise = Math.min(100.0, noise + 6.0);
            } else if (player.isSneaking()) {
                // Szybki spadek hałasu podczas skradania
                noise = Math.max(0.0, noise - 12.0);
            } else {
                // Naturalny spadek hałasu podczas stania/chodzenia
                noise = Math.max(0.0, noise - 6.0);
            }

            playerNoise.put(uuid, noise);

            // Wyświetlenie wskaźnika hałasu na Action Barze
            String bar = buildNoiseBar(noise);
            ChatColor color = noise > 75.0 ? ChatColor.DARK_RED : noise > 45.0 ? ChatColor.GOLD : ChatColor.GREEN;
            String text = color + "Hałas: " + bar + " " + ChatColor.WHITE + (int) noise + "%";
            if (noise > 80.0) {
                text += ChatColor.RED + " ⚠ UWAGA! ZOMBIE SŁYSZĄ CIĘ!";
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));

            // Efekty wysokiego hałasu
            if (noise >= 50.0) {
                // 1. Alert okolicznych mobów (kierują się na gracza)
                alertNearbyMonsters(player, 32.0);

                if (noise >= 80.0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1.2f);
                    // 2. Szansa na natychmiastowy spawn agresywnego zombie / ambush
                    if (random.nextDouble() < 0.35) {
                        spawnAmbushZombie(player, zombieManager);
                    }
                }
            }
        }
    }

    private void alertNearbyMonsters(Player player, double radius) {
        Location loc = player.getLocation();
        for (LivingEntity e : player.getWorld().getLivingEntities()) {
            if (e instanceof Monster monster && !e.isDead()) {
                if (monster.getLocation().distanceSquared(loc) <= radius * radius) {
                    monster.setTarget(player);
                    monster.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, monster.getEyeLocation(), 1);
                }
            }
        }
    }

    private void spawnAmbushZombie(Player player, CustomZombieManager zombieManager) {
        Location pLoc = player.getLocation();
        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = 8.0 + random.nextDouble() * 6.0;
        double sx = pLoc.getX() + dist * Math.cos(angle);
        double sz = pLoc.getZ() + dist * Math.sin(angle);
        int sy = player.getWorld().getHighestBlockYAt((int) sx, (int) sz);
        Location spawnLoc = new Location(player.getWorld(), sx, sy + 1, sz);

        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.6f);
        player.getWorld().spawnParticle(Particle.SCULK_SOUL, spawnLoc.clone().add(0, 1.0, 0), 12, 0.5, 0.5, 0.5, 0.05);

        if (zombieManager != null) {
            zombieManager.spawnRandomSpecialZombie(spawnLoc, player);
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

    public void cleanup() {
        playerNoise.clear();
    }
}
