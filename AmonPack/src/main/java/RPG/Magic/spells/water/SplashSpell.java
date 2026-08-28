package RPG.Magic.spells.water;

import Plugin.AmonPackPlugin;
import RPG.Magic.elements.ElementStatusManager;
import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.model.Spell;
import RPG.Magic.model.SpellElement;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SplashSpell extends Spell {

    private static final Map<UUID, Long> preparedWaterSources = new ConcurrentHashMap<>();

    public SplashSpell() {
        super("splash", "§9§lSplash", SpellElement.WATER, 20, 2.0);
    }

    public static boolean hasPreparedSource(UUID uuid) {
        Long exp = preparedWaterSources.get(uuid);
        return exp != null && exp > System.currentTimeMillis();
    }

    public static void setPreparedSource(UUID uuid, long durationMs) {
        preparedWaterSources.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public static void removePreparedSource(UUID uuid) {
        preparedWaterSources.remove(uuid);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        if (isOnCooldown(player)) {
            sendCooldownActionBar(player);
            return false;
        }

        UUID uuid = player.getUniqueId();
        boolean hasBottle = hasWaterBottle(player);
        boolean hasDrawnWater = hasPreparedSource(uuid);
        boolean isCreative = player.getGameMode() == org.bukkit.GameMode.CREATIVE;

        // Jeśli gracz nie ma butelki i nie ma przyciągniętej wody przez Shift
        if (!hasBottle && !hasDrawnWater && !isCreative) {
            // Próba natychmiastowego pobrania ze spojrzenia na wodę (jeśli patrzy na wodę w zasięgu 12 bloków)
            RayTraceResult ray = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 12.0, FluidCollisionMode.ALWAYS, true);
            if (ray != null && ray.getHitBlock() != null && isWaterOrIce(ray.getHitBlock().getType())) {
                // Przyciągnięcie wody przed gracza
                pullWaterSphere(player, ray.getHitBlock().getLocation());
                sendActionBar(player, "§b✦ Przyciągnięto wodę! §eKliknij LPM aby wystrzelić pocisk Splash!");
                return true;
            } else {
                sendActionBar(player, "§c✦ Wymagana Butelka Wody lub spojrzenie na wodę/lód (Shift)!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.7f);
                return false;
            }
        }

        if (!checkAndConsumeCost(player, tomeItem, manaManager)) {
            return false;
        }

        removePreparedSource(uuid);

        // Wystrzelenie pocisku Waterstrike
        fireWaterstrike(player, tomeItem);
        return true;
    }

    public static void pullWaterSphere(Player player, Location sourceLoc) {
        UUID uuid = player.getUniqueId();
        setPreparedSource(uuid, 6000L); // Woda wisi przez 6 sekund

        sourceLoc.getWorld().playSound(sourceLoc, Sound.ITEM_BUCKET_FILL, 1.0f, 1.3f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !hasPreparedSource(uuid)) {
                    cancel();
                    return;
                }

                ticks += 2;
                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize();
                Location sphereLoc = eye.clone().add(dir.clone().multiply(1.8));

                // Wirująca kula wody przed oczami gracza
                for (int i = 0; i < 6; i++) {
                    double angle = (ticks * 0.3) + (i * Math.PI / 3);
                    double x = Math.cos(angle) * 0.4;
                    double y = Math.sin(angle) * 0.4;
                    Location p = sphereLoc.clone().add(x, y, 0);
                    sphereLoc.getWorld().spawnParticle(Particle.FALLING_WATER, p, 2, 0.05, 0.05, 0.05, 0);
                    sphereLoc.getWorld().spawnParticle(Particle.FALLING_WATER, p, 1, 0, 0, 0, 0);
                }

                if (ticks >= 60) { // 3 sekundy
                    cancel();
                    removePreparedSource(uuid);
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);
    }

    private void fireWaterstrike(Player player, ItemStack tomeItem) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_SPLASH, 1.2f, 1.4f);
        player.getWorld().playSound(eye, Sound.ENTITY_BOAT_PADDLE_WATER, 1.0f, 1.6f);

        new BukkitRunnable() {
            Location currentLoc = eye.clone().add(dir.clone().multiply(1.0));
            int distance = 0;
            final int maxDistance = 25;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                for (int step = 0; step < 2; step++) {
                    currentLoc.add(dir.clone().multiply(0.8));
                    distance++;

                    currentLoc.getWorld().spawnParticle(Particle.FALLING_WATER, currentLoc, 6, 0.15, 0.15, 0.15, 0.02);
                    currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 3, 0.1, 0.1, 0.1, 0.05);

                    Block b = currentLoc.getBlock();
                    if (b.getType().isSolid()) {
                        cancel();
                        currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.5f);
                        currentLoc.getWorld().spawnParticle(Particle.SPLASH, currentLoc, 20, 0.3, 0.3, 0.3, 0.1);
                        return;
                    }

                    for (org.bukkit.entity.Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                        if (e instanceof LivingEntity target && !e.equals(player)) {
                            cancel();
                            ElementStatusManager.triggerDamageAndReaction(player, target, getBaseDamage(), SpellElement.WATER, tomeItem);
                            target.setVelocity(dir.clone().multiply(0.6).setY(0.3));
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0f, 1.2f);
                            target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.1);
                            return;
                        }
                    }

                    if (distance >= maxDistance) {
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private boolean hasWaterBottle(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() == Material.WATER_BUCKET) return true;
            if (item.getType() == Material.POTION && item.getItemMeta() instanceof PotionMeta pm) {
                if (pm.getBasePotionData().getType() == PotionType.WATER) return true;
            }
        }
        return false;
    }

    private static boolean isWaterOrIce(Material mat) {
        return mat == Material.WATER || mat == Material.ICE || mat == Material.PACKED_ICE || mat == Material.BLUE_ICE || mat == Material.FROSTED_ICE;
    }
}
