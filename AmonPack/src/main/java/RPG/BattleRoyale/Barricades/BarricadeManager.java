package RPG.BattleRoyale.Barricades;

import RPG.BattleRoyale.BattleRoyaleWorldManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zarządza barykadami stawianymi przez graczy (np. dębowe płyty, deski).
 * Śledzi ich wytrzymałość i umożliwia zombie oraz bossom aktywne atakowanie
 * i niszczenie bloków blokujących im drogę do gracza.
 */
public class BarricadeManager {

    private final Map<Location, Integer> barricades = new ConcurrentHashMap<>();
    private final Map<UUID, Long> mobAttackCooldowns = new ConcurrentHashMap<>();

    public void registerBarricade(Location loc, int initialHits) {
        if (loc == null) return;
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        barricades.put(blockLoc, Math.max(1, initialHits));
    }

    public boolean isBarricade(Location loc) {
        if (loc == null) return false;
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return barricades.containsKey(blockLoc);
    }

    public void removeBarricade(Location loc) {
        if (loc == null) return;
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        barricades.remove(blockLoc);
    }

    public Map<Location, Integer> getBarricades() {
        return Collections.unmodifiableMap(barricades);
    }

    /**
     * Pętla oblegania i niszczenia barykad przez zombie oraz bossów.
     * Wywoływana regularnie w pętli gry.
     */
    public void tickZombieSiege(World world, BattleRoyaleWorldManager worldManager, int defaultHits) {
        if (world == null || barricades.isEmpty()) return;

        long now = System.currentTimeMillis();

        for (Monster monster : world.getEntitiesByClass(Monster.class)) {
            if (monster.isDead() || !monster.isValid()) continue;
            if (!(monster.getTarget() instanceof Player player) || !player.isOnline() || player.isDead()) continue;

            // Sprawdzenie cooldownu ataku potwora (co 1.2 sekundy)
            long lastAttack = mobAttackCooldowns.getOrDefault(monster.getUniqueId(), 0L);
            if (now - lastAttack < 1200) continue;

            Location mLoc = monster.getLocation();
            Location pLoc = player.getLocation();

            // Szukamy barykady bezpośrednio przed potworem lub w promieniu 1.8 bloku
            Block targetBarricade = findBarricadeInFront(monster, world);
            if (targetBarricade == null) {
                targetBarricade = findClosestBarricadeToPlayer(mLoc, pLoc, world, 1.8);
            }

            if (targetBarricade != null) {
                mobAttackCooldowns.put(monster.getUniqueId(), now);
                damageBarricade(targetBarricade, monster, player, worldManager, defaultHits);
            }
        }
    }

    private Block findBarricadeInFront(Monster monster, World world) {
        Location mEye = monster.getEyeLocation();
        Location forwardFeet = monster.getLocation().add(monster.getLocation().getDirection().multiply(0.9));
        Location forwardHead = mEye.clone().add(monster.getLocation().getDirection().multiply(0.9));

        Block bFeet = forwardFeet.getBlock();
        if (isBarricade(bFeet.getLocation())) return bFeet;

        Block bHead = forwardHead.getBlock();
        if (isBarricade(bHead.getLocation())) return bHead;

        return null;
    }

    private Block findClosestBarricadeToPlayer(Location mLoc, Location pLoc, World world, double maxDist) {
        double mDistToP = mLoc.distanceSquared(pLoc);
        Block best = null;
        double minD = Double.MAX_VALUE;

        for (Location bLoc : barricades.keySet()) {
            if (!bLoc.getWorld().equals(world)) continue;
            double distToMonster = bLoc.distanceSquared(mLoc);
            if (distToMonster <= maxDist * maxDist) {
                // Sprawdź czy barykada leży w kierunku gracza (jest bliżej gracza niż sam mob)
                double bDistToP = bLoc.distanceSquared(pLoc);
                if (bDistToP <= mDistToP && distToMonster < minD) {
                    minD = distToMonster;
                    best = bLoc.getBlock();
                }
            }
        }
        return best;
    }

    private void damageBarricade(Block block, Monster monster, Player player, BattleRoyaleWorldManager worldManager, int defaultHits) {
        Location bLoc = block.getLocation();
        int currentHp = barricades.getOrDefault(bLoc, defaultHits);
        currentHp--;

        // Animacja ataku potwora
        monster.swingMainHand();

        // Dźwięk uderzenia i cząsteczki
        worldManager.recordBlockChange(block);
        block.getWorld().playSound(bLoc.clone().add(0.5, 0.5, 0.5), Sound.BLOCK_WOOD_HIT, 1.0f, 0.85f);
        block.getWorld().spawnParticle(Particle.BLOCK, bLoc.clone().add(0.5, 0.5, 0.5), 10, 0.25, 0.25, 0.25, block.getBlockData());

        if (currentHp <= 0) {
            // Zniszczenie barykady!
            barricades.remove(bLoc);
            block.setType(Material.AIR);
            block.getWorld().playSound(bLoc.clone().add(0.5, 0.5, 0.5), Sound.BLOCK_WOOD_BREAK, 1.2f, 0.9f);
            block.getWorld().spawnParticle(Particle.BLOCK, bLoc.clone().add(0.5, 0.5, 0.5), 25, 0.4, 0.4, 0.4,
                    Material.OAK_PLANKS.createBlockData());

            if (player != null && player.isOnline() && player.getLocation().distanceSquared(bLoc) < 144.0) {
                player.sendMessage(ChatColor.RED + "[BattleRoyale] ☠ Potwory zniszczyły twoją barykadę!");
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8f, 1.0f);
            }
        } else {
            barricades.put(bLoc, currentHp);
        }
    }

    public void cleanup() {
        barricades.clear();
        mobAttackCooldowns.clear();
    }
}
