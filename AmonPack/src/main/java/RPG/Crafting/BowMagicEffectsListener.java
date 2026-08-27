package RPG.Crafting;

import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BowMagicEffectsListener implements Listener {

    private static final Map<UUID, Integer> pinnedArrows = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta()) return;

        boolean tripleShot = hasEffect(bow, "Bow_Triple_Shot", "Potrójny Strzał");
        boolean piercing = hasEffect(bow, "Bow_Piercing_Arrows", "Przeszywające");
        boolean pinArrows = hasEffect(bow, "Bow_Pin_Arrows", "Grotowe");

        if (event.getProjectile() instanceof AbstractArrow arrow) {
            if (piercing) {
                arrow.setPierceLevel(5);
                arrow.setMetadata("piercing_arrow", new FixedMetadataValue(AmonPackPlugin.plugin, true));
            }
            if (pinArrows) {
                arrow.setMetadata("pin_arrow", new FixedMetadataValue(AmonPackPlugin.plugin, true));
            }
        }

        if (tripleShot && event.getProjectile() instanceof AbstractArrow mainArrow) {
            Vector velocity = mainArrow.getVelocity();
            double speed = velocity.length();
            if (speed < 0.1) speed = 1.5;

            Vector dir = velocity.clone().normalize();
            Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

            // Dwie dodatkowe strzały w rozrzucie
            Vector leftDir = dir.clone().add(right.clone().multiply(-0.15)).normalize().multiply(speed);
            Vector rightDir = dir.clone().add(right.clone().multiply(0.15)).normalize().multiply(speed);

            Location loc = mainArrow.getLocation();

            Arrow leftArrow = player.getWorld().spawnArrow(loc, leftDir, (float) speed, 0);
            leftArrow.setShooter(player);
            leftArrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);

            Arrow rightArrow = player.getWorld().spawnArrow(loc, rightDir, (float) speed, 0);
            rightArrow.setShooter(player);
            rightArrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);

            if (piercing) {
                leftArrow.setPierceLevel(5);
                rightArrow.setPierceLevel(5);
            }
            if (pinArrows) {
                leftArrow.setMetadata("pin_arrow", new FixedMetadataValue(AmonPackPlugin.plugin, true));
                rightArrow.setMetadata("pin_arrow", new FixedMetadataValue(AmonPackPlugin.plugin, true));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (arrow.hasMetadata("pin_arrow")) {
            UUID targetId = target.getUniqueId();
            int current = pinnedArrows.getOrDefault(targetId, 0) + 1;
            pinnedArrows.put(targetId, current);

            // Każda strzała zwiększa otrzymywane obrażenia o 15%
            double multiplier = 1.0 + (current * 0.15);
            event.setDamage(event.getDamage() * multiplier);

            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, target.getHeight() / 2, 0), 10, 0.2, 0.2, 0.2, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);

            // Po 12 sekundach strzała odpada i zmniejsza licznik
            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                Integer count = pinnedArrows.get(targetId);
                if (count != null) {
                    if (count <= 1) {
                        pinnedArrows.remove(targetId);
                    } else {
                        pinnedArrows.put(targetId, count - 1);
                    }
                }
            }, 240L);
        }
    }

    private boolean hasEffect(ItemStack item, String effectKey, String displayNameSub) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                if (line.contains(displayNameSub) || line.contains(effectKey)) {
                    return true;
                }
            }
        }
        return false;
    }
}
