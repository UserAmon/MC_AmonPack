package RPG.BattleRoyale.Visuals;

import RPG.BattleRoyale.GroundLoot.GroundLootManager;
import RPG.BattleRoyale.Keys.KeyManager;
import RPG.BattleRoyale.Keys.KeyType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Odpowiada za lekkie wskazówki wizualne dla graczy:
 * 1. Subtelne iskry na ziemi wokół leżącego Ground Lootu i interaktywnych elementów w promieniu 5 bloków.
 * 2. Ścieżka z cząsteczek na poziomie podłogi/ziemi wskazująca kierunek do bezpiecznej strefy.
 * 3. Ścieżka z cząsteczek na ziemi prowadząca posiadacza klucza wprost do najbliższych zamkniętych drzwi (np. Military Depot, Pharmacy).
 */
public class NavigationVisualsManager {

    private final Map<UUID, Long> lastNavTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastKeyNavTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHighlightTime = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final Particle.DustOptions SAFE_DUST = new Particle.DustOptions(Color.fromRGB(80, 220, 180), 0.85f);
    private static final Particle.DustOptions WARNING_DUST = new Particle.DustOptions(Color.fromRGB(255, 140, 0), 0.95f);
    private static final Particle.DustOptions DANGER_DUST = new Particle.DustOptions(Color.fromRGB(255, 40, 40), 1.0f);
    private static final Particle.DustOptions KEY_DOOR_DUST = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.1f); // złote cząsteczki dla klucza

    public void tick(World world, List<Player> players, Location center, double currentRadius, KeyManager keyManager) {
        if (world == null || players == null || players.isEmpty()) return;

        long now = System.currentTimeMillis();

        for (Player player : players) {
            if (player == null || !player.isOnline() || player.isDead()) continue;
            UUID uuid = player.getUniqueId();
            Location pLoc = player.getLocation();

            // 1. Wskazówki interaktywnych elementów (co 0.9 sekundy)
            long lastHigh = lastHighlightTime.getOrDefault(uuid, 0L);
            if (now - lastHigh >= 900) {
                lastHighlightTime.put(uuid, now);
                renderInteractableHighlights(player, world, pLoc);
            }

            // 2. Sprawdzenie, czy gracz trzyma w ręce klucz do zamkniętych drzwi
            boolean guidedToDoor = false;
            if (keyManager != null) {
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                ItemStack offHand = player.getInventory().getItemInOffHand();
                KeyType heldKey = keyManager.getKeyType(mainHand);
                if (heldKey == null) heldKey = keyManager.getKeyType(offHand);

                if (heldKey != null) {
                    Location doorLoc = keyManager.findNearestLockedDoor(pLoc, heldKey);
                    if (doorLoc != null) {
                        guidedToDoor = true;
                        long lastKeyNav = lastKeyNavTime.getOrDefault(uuid, 0L);
                        if (now - lastKeyNav >= 1800) {
                            lastKeyNavTime.put(uuid, now);
                            renderGroundNavigationTrail(player, world, pLoc, doorLoc.clone().add(0.5, 0, 0.5), KEY_DOOR_DUST);
                        }
                    }
                }
            }

            // 3. Ścieżka na ziemi do bezpiecznej strefy (jeśli gracz nie jest prowadzony do drzwi)
            if (!guidedToDoor) {
                double distToCenter = Math.hypot(pLoc.getX() - center.getX(), pLoc.getZ() - center.getZ());
                double distToBorder = currentRadius - distToCenter;

                long navCooldown;
                Particle.DustOptions trailColor;
                if (distToBorder <= 0.0) {
                    navCooldown = 1800; // Poza strefą: co 1.8 sekundy
                    trailColor = DANGER_DUST;
                } else if (distToBorder <= 18.0) {
                    navCooldown = 2800; // Blisko krawędzi: co 2.8 sekundy
                    trailColor = WARNING_DUST;
                } else if (distToBorder <= 45.0) {
                    navCooldown = 5500; // Średnia odległość: co 5.5 sekundy
                    trailColor = SAFE_DUST;
                } else {
                    navCooldown = 9000; // Bezpiecznie w centrum: co 9 sekund
                    trailColor = SAFE_DUST;
                }

                long lastNav = lastNavTime.getOrDefault(uuid, 0L);
                if (now - lastNav >= navCooldown) {
                    lastNavTime.put(uuid, now);
                    renderGroundNavigationTrail(player, world, pLoc, center, trailColor);
                }
            }
        }
    }

    /**
     * Iskry wokół interaktywnych elementów w promieniu 5 kratek.
     */
    private void renderInteractableHighlights(Player player, World world, Location pLoc) {
        int px = pLoc.getBlockX();
        int py = pLoc.getBlockY();
        int pz = pLoc.getBlockZ();

        // A. Niewidzialne ramki z leżącym lootem (Ground Loot)
        for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
            if (frame.isValid() && frame.getPersistentDataContainer().has(GroundLootManager.KEY_GROUND_LOOT, org.bukkit.persistence.PersistentDataType.BYTE)) {
                if (frame.getLocation().distanceSquared(pLoc) <= 25.0) {
                    Location fLoc = frame.getLocation();
                    double ox = (random.nextDouble() - 0.5) * 0.45;
                    double oz = (random.nextDouble() - 0.5) * 0.45;
                    player.spawnParticle(Particle.WAX_OFF, fLoc.getX() + ox, fLoc.getY() + 0.05, fLoc.getZ() + oz, 1, 0, 0.02, 0, 0.01);
                }
            }
        }

        // B. Skrzynie, beczki, półki, dźwignie, drzwi
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (x * x + z * z > 25) continue;
                for (int y = -2; y <= 2; y++) {
                    Block block = world.getBlockAt(px + x, py + y, pz + z);
                    Material type = block.getType();
                    if (isInteractable(type)) {
                        Location bLoc = block.getLocation();
                        double ox = 0.2 + random.nextDouble() * 0.6;
                        double oz = 0.2 + random.nextDouble() * 0.6;
                        player.spawnParticle(Particle.WAX_ON, bLoc.getX() + ox, bLoc.getY() + 0.95, bLoc.getZ() + oz, 1, 0, 0.01, 0, 0.01);
                    }
                }
            }
        }
    }

    private boolean isInteractable(Material mat) {
        if (mat == Material.CHEST || mat == Material.TRAPPED_CHEST || mat == Material.BARREL) return true;
        if (mat == Material.LEVER || mat == Material.STONE_BUTTON || mat == Material.OAK_BUTTON) return true;
        String name = mat.name();
        return name.contains("SHELF") || name.contains("DOOR") || name.contains("TRAPDOOR");
    }

    /**
     * Rysuje na podłodze 4-5 lekkich cząsteczek prowadzących po ziemi w kierunku celu.
     */
    private void renderGroundNavigationTrail(Player player, World world, Location pLoc, Location target, Particle.DustOptions dust) {
        double dx = target.getX() - pLoc.getX();
        double dz = target.getZ() - pLoc.getZ();
        double len = Math.hypot(dx, dz);
        if (len < 1.5) return;

        dx /= len;
        dz /= len;

        double[] distances = { 1.2, 2.2, 3.2, 4.2 };
        Location lastStepFloor = null;

        for (double d : distances) {
            double tx = pLoc.getX() + dx * d;
            double tz = pLoc.getZ() + dz * d;

            Location floor = findFloorAt(world, tx, pLoc.getBlockY(), tz);
            if (floor != null) {
                Location particleLoc = floor.clone().add(0, 0.08, 0);
                player.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dust);
                lastStepFloor = floor;
            }
        }

        if (lastStepFloor != null) {
            Vector forward = new Vector(dx, 0, dz);
            Vector left = new Vector(-dz, 0, dx).multiply(0.28);
            Vector right = new Vector(dz, 0, -dx).multiply(0.28);

            Location leftTip = lastStepFloor.clone().subtract(forward.clone().multiply(0.28)).add(left).add(0, 0.08, 0);
            Location rightTip = lastStepFloor.clone().subtract(forward.clone().multiply(0.28)).add(right).add(0, 0.08, 0);

            player.spawnParticle(Particle.DUST, leftTip, 1, 0, 0, 0, 0, dust);
            player.spawnParticle(Particle.DUST, rightTip, 1, 0, 0, 0, 0, dust);
        }
    }

    private Location findFloorAt(World world, double x, int baseY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);

        for (int y = baseY + 1; y >= baseY - 3; y--) {
            Block block = world.getBlockAt(bx, y, bz);
            if (!block.getType().isAir() && block.getType().isSolid()) {
                return new Location(world, x, y + 1.0, z);
            }
        }
        return null;
    }

    public void cleanup() {
        lastNavTime.clear();
        lastKeyNavTime.clear();
        lastHighlightTime.clear();
    }
}
