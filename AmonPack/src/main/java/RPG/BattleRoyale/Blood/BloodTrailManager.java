package RPG.BattleRoyale.Blood;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Zarządza śladami krwi pozostawianymi przez rannych graczy na mapie.
 * Gracze krwawią po otrzymaniu obrażeń, ugryzieniu przez zombie i zabiciu moba.
 * Zombie mogą tropić ślady krwi i podążać ich tropem.
 */
public class BloodTrailManager {

    private final List<BloodTrail> activeTrails = new CopyOnWriteArrayList<>();
    private final Map<UUID, Long> lastBleedTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> bleedingDropsRemaining = new ConcurrentHashMap<>();

    private long trailLifetimeMs = 35_000L; // 35 sekund
    private double bloodDetectionMultiplier = 1.0;

    public void setBloodDetectionMultiplier(double mult) {
        this.bloodDetectionMultiplier = Math.max(1.0, mult);
    }

    public void setTrailLifetimeMs(long ms) {
        this.trailLifetimeMs = ms;
    }

    /**
     * Wywoływane po otrzymaniu obrażeń przez gracza.
     */
    public void onPlayerDamage(Player player, double damage) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();

        // Dodanie ładunków krwawienia zależnie od obrażeń (2-4 plamy)
        int drops = Math.min(5, 1 + (int) (damage / 4.0));
        bleedingDropsRemaining.put(uuid, drops);

        // Natychmiastowa pierwsza plama pod graczem
        spawnBloodDrop(player.getLocation(), 1.0, uuid);
    }

    /**
     * Ugryzienie przez zombie - intensywne krwawienie.
     */
    public void onZombieBite(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        bleedingDropsRemaining.put(uuid, 4);
        spawnBloodDrop(player.getLocation(), 1.0, uuid);
    }

    /**
     * Rozbryzg krwi przy śmierci zombie.
     */
    public void onZombieKilled(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        spawnBloodDrop(loc, 0.8, null);
    }

    /**
     * Użycie bandaża - tamuje aktywne krwawienie gracza i zostawia ostatni mały ślad.
     */
    public void onBandageUsed(Player player) {
        if (player == null) return;
        bleedingDropsRemaining.remove(player.getUniqueId());
        spawnBloodDrop(player.getLocation(), 0.35, player.getUniqueId());
    }

    private void spawnBloodDrop(Location loc, double intensity, UUID playerId) {
        if (loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();

        Location floor = findFloorAt(world, loc.getX(), loc.getBlockY(), loc.getZ());
        if (floor == null) floor = loc.clone();

        BloodTrail trail = new BloodTrail(floor.add(0, 0.05, 0), intensity, trailLifetimeMs, playerId);
        activeTrails.add(trail);

        if (playerId != null) {
            lastBleedTime.put(playerId, System.currentTimeMillis());
        }
    }

    /**
     * Taktowanie co 1 sekundę z pętli gry.
     */
    public void tick(World world, List<Player> players) {
        long now = System.currentTimeMillis();

        // 1. Czyszczenie wygasłych śladów
        activeTrails.removeIf(t -> t.isExpired(now));

        // 2. Okresowe krwawienie rannych graczy (co 3.5 sekundy)
        if (players != null) {
            for (Player player : players) {
                if (player == null || !player.isOnline() || player.isDead()) continue;
                UUID uuid = player.getUniqueId();

                int drops = bleedingDropsRemaining.getOrDefault(uuid, 0);
                if (drops > 0) {
                    long last = lastBleedTime.getOrDefault(uuid, 0L);
                    if (now - last >= 3500) {
                        spawnBloodDrop(player.getLocation(), 0.75, uuid);
                        drops--;
                        if (drops <= 0) {
                            bleedingDropsRemaining.remove(uuid);
                        } else {
                            bleedingDropsRemaining.put(uuid, drops);
                        }
                    }
                }
            }
        }

        // 3. Renderowanie cząsteczek krwi na ziemi
        for (BloodTrail trail : activeTrails) {
            Location tLoc = trail.getLocation();
            double freshness = trail.getFreshness(now);
            if (freshness > 0.05 && tLoc.getWorld() != null) {
                // Lekki efekt plamy czerwieni na ziemi
                tLoc.getWorld().spawnParticle(
                        Particle.BLOCK,
                        tLoc,
                        1,
                        0.15, 0.01, 0.15, 0,
                        Material.REDSTONE_BLOCK.createBlockData()
                );
            }
        }
    }

    /**
     * Znajduje najbardziej wartościowy/świeży ślad krwi dla zombie w promieniu poszukiwań.
     */
    public BloodTrail findBestBloodTrailNear(Location zombieLoc, double maxRadius) {
        if (zombieLoc == null || activeTrails.isEmpty()) return null;

        long now = System.currentTimeMillis();
        double effectiveRadius = maxRadius * bloodDetectionMultiplier;
        double maxRadiusSq = effectiveRadius * effectiveRadius;

        BloodTrail best = null;
        double highestPriority = -1.0;

        for (BloodTrail trail : activeTrails) {
            if (!trail.getLocation().getWorld().equals(zombieLoc.getWorld())) continue;
            double distSq = trail.getLocation().distanceSquared(zombieLoc);
            if (distSq <= maxRadiusSq) {
                double dist = Math.sqrt(distSq);
                double freshness = trail.getFreshness(now);
                if (freshness <= 0.05) continue;

                // Priorytet: świeższa krew + mniejsza odległość
                double priority = freshness * (1.0 / (1.0 + (dist * 0.15))) * bloodDetectionMultiplier;
                if (priority > highestPriority) {
                    highestPriority = priority;
                    best = trail;
                }
            }
        }
        return best;
    }

    private Location findFloorAt(World world, double x, int baseY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);

        for (int y = baseY + 1; y >= baseY - 4; y--) {
            Block block = world.getBlockAt(bx, y, bz);
            if (!block.getType().isAir() && block.getType().isSolid()) {
                return new Location(world, x, y + 1.0, z);
            }
        }
        return null;
    }

    public List<BloodTrail> getActiveTrails() {
        return Collections.unmodifiableList(activeTrails);
    }

    public void cleanup() {
        activeTrails.clear();
        lastBleedTime.clear();
        bleedingDropsRemaining.clear();
        bloodDetectionMultiplier = 1.0;
    }
}
