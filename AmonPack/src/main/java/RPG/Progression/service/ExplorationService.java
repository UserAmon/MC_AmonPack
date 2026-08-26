package RPG.Progression.service;

import RPG.Progression.event.BiomeDiscoveredEvent;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.StageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExplorationService {

    private final Map<UUID, Long> lastCheckTimes = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastKnownBiomes = new ConcurrentHashMap<>();
    private final Map<UUID, Location> startLocations = new ConcurrentHashMap<>();

    public ExplorationService() {
    }

    public void checkPlayerLocation(Player player, PlayerProgressionData data, ProgressionService progressionService) {
        if (player == null || data == null) return;

        long now = System.currentTimeMillis();
        long last = lastCheckTimes.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 2000) return; // check every 2 seconds max
        lastCheckTimes.put(player.getUniqueId(), now);

        Location loc = player.getLocation();

        // 1. Biome Check
        Biome biome = loc.getBlock().getBiome();
        String biomeKey = biome.name();
        String lastBiome = lastKnownBiomes.get(player.getUniqueId());

        if (lastBiome == null || !lastBiome.equalsIgnoreCase(biomeKey)) {
            lastKnownBiomes.put(player.getUniqueId(), biomeKey);

            if (data.addDiscoveredBiome(biomeKey)) {
                // First time discovery
                BiomeDiscoveredEvent event = new BiomeDiscoveredEvent(player, biomeKey, biome);
                Bukkit.getPluginManager().callEvent(event);

                player.sendTitle(
                        "§3✦ NOWY BIOM ✦",
                        "§bOdkryto: §f" + formatBiomeName(biomeKey),
                        10, 40, 10
                );
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                player.sendMessage("§3[Eksploracja] §7Odkryłeś nowy biom: §b" + formatBiomeName(biomeKey));
            }

            // Notify progression service for biome discovery objectives
            if (progressionService != null) {
                progressionService.handleObjective(player, RPG.Progression.model.ObjectiveType.DISCOVER_BIOME, biomeKey, 1);
                // Also notify friendly name
                progressionService.handleObjective(player, RPG.Progression.model.ObjectiveType.DISCOVER_BIOME, formatBiomeName(biomeKey), 1);
            }
        }

        // 2. Travel Distance Check
        Location startLoc = startLocations.computeIfAbsent(player.getUniqueId(), u -> loc);
        if (startLoc.getWorld() != null && startLoc.getWorld().equals(loc.getWorld())) {
            double dist = startLoc.distance(loc);
            if (progressionService != null && dist > 10) {
                progressionService.handleObjective(player, RPG.Progression.model.ObjectiveType.TRAVEL_DISTANCE, "DISTANCE", (int) dist);
            }
        }

        // 3. Mining Depth Check (Y level)
        if (progressionService != null) {
            int currentY = loc.getBlockY();
            progressionService.handleObjective(player, RPG.Progression.model.ObjectiveType.MINE_TO_DEPTH, String.valueOf(currentY), 1);
        }
    }

    public static String formatBiomeName(String biomeKey) {
        if (biomeKey == null) return "";
        String[] parts = biomeKey.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public void removePlayer(UUID uuid) {
        lastCheckTimes.remove(uuid);
        lastKnownBiomes.remove(uuid);
        startLocations.remove(uuid);
    }
}
