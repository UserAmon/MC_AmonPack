package RPG.Magic.spells.lightning;

import Plugin.AmonPackPlugin;
import RPG.Magic.elements.ElementStatusManager;
import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.model.Spell;
import RPG.Magic.model.SpellElement;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class LightningSpell extends Spell {

    public LightningSpell() {
        super("lightning", "§e§lLightning", SpellElement.LIGHTNING, 30, 3.5);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "lightning_mana") || MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "lightning_cd") || MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");

        int effectiveMana = hasManaRed ? Math.max(15, getManaCost() - 10) : getManaCost();
        double effectiveCd = hasCdRed ? Math.max(1.5, getCooldownSeconds() - 1.0) : getCooldownSeconds();

        if (isOnCooldown(player)) {
            sendCooldownActionBar(player);
            return false;
        }

        if (!manaManager.hasMana(player, effectiveMana)) {
            sendNoManaActionBar(player, effectiveMana, manaManager);
            return false;
        }

        manaManager.consumeMana(player, effectiveMana);
        setCooldown(player, (long) (effectiveCd * 1000));

        // Raytrace celowania gracza do 35 bloków
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        RayTraceResult result = player.getWorld().rayTrace(eye, dir, 35.0, FluidCollisionMode.NEVER, true, 0.5, entity -> !entity.equals(player));

        Location targetLoc;
        if (result != null && result.getHitBlock() != null) {
            targetLoc = result.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
        } else if (result != null && result.getHitEntity() != null) {
            targetLoc = result.getHitEntity().getLocation();
        } else {
            targetLoc = eye.clone().add(dir.clone().multiply(25.0));
            targetLoc = targetLoc.getWorld().getHighestBlockAt(targetLoc).getLocation().add(0.5, 1.0, 0.5);
        }

        player.getWorld().playSound(eye, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.8f);
        player.getWorld().playSound(eye, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.9f);

        // 2-sekundowy animowany pierścień z odliczaniem i partiklami ładowania
        final Location strikeLoc = targetLoc;
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 40; // 2 sekundy (20 ticks = 1s)

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                ticks += 2;
                double progress = (double) ticks / maxTicks;
                double radius = 2.5 * (1.0 - (progress * 0.3));

                // Rysowanie pierścienia elektrycznego na ziemi
                int points = 16;
                double angleStep = (2 * Math.PI) / points;
                double offsetAngle = (ticks * 0.2);

                for (int i = 0; i < points; i++) {
                    double angle = i * angleStep + offsetAngle;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location ringPoint = strikeLoc.clone().add(x, 0.1, z);
                    strikeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ringPoint, 1, 0, 0, 0, 0);
                    if (i % 4 == 0) {
                        strikeLoc.getWorld().spawnParticle(Particle.CRIT, ringPoint, 1, 0, 0.05, 0, 0.02);
                    }
                }

                if (ticks % 8 == 0) {
                    float pitch = 1.0f + (float) (progress * 0.8f);
                    strikeLoc.getWorld().playSound(strikeLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, pitch);
                }

                if (ticks >= maxTicks) {
                    cancel();
                    // Uderzenie pioruna po 2 sekundach
                    strikeLoc.getWorld().strikeLightning(strikeLoc);
                    strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.0f);
                    strikeLoc.getWorld().spawnParticle(Particle.EXPLOSION, strikeLoc, 2, 0.2, 0.2, 0.2, 0.0);

                    for (org.bukkit.entity.Entity e : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 4.0, 4.0, 4.0)) {
                        if (e instanceof LivingEntity le && !e.equals(player)) {
                            ElementStatusManager.triggerDamageAndReaction(player, le, 12.0, SpellElement.LIGHTNING, tomeItem);
                        }
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);

        return true;
    }
}
