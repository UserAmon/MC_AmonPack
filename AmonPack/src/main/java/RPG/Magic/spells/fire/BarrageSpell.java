package RPG.Magic.spells.fire;

import Plugin.AmonPackPlugin;
import RPG.Magic.elements.ElementStatusManager;
import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.model.Spell;
import RPG.Magic.model.SpellElement;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BarrageSpell extends Spell {

    public BarrageSpell() {
        super("barrage", "§c§lBarrage", SpellElement.FIRE, 65, 8.0);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "barrage_mana") || MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "barrage_cd") || MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");
        boolean hasCount = MagicItemManager.hasUpgrade(tomeItem, "barrage_count");
        int maxShots = hasCount ? 6 : 4;

        int effectiveMana = hasManaRed ? Math.max(20, getManaCost() - 20) : getManaCost();
        double effectiveCd = hasCdRed ? Math.max(2.0, getCooldownSeconds() - 2.0) : getCooldownSeconds();

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

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count++ >= maxShots || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize();
                // Lekki losowy rozrzut dla każdego pocisku
                double rx = (Math.random() - 0.5) * 0.08;
                double ry = (Math.random() - 0.5) * 0.08;
                double rz = (Math.random() - 0.5) * 0.08;
                Vector finalDir = dir.clone().add(new Vector(rx, ry, rz)).normalize();

                player.getWorld().playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.4f + (count * 0.1f));
                launchDart(player, eye, finalDir, tomeItem);
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 3L);

        return true;
    }

    private void launchDart(Player player, Location start, Vector dir, ItemStack tomeItem) {
        new BukkitRunnable() {
            Location current = start.clone();
            Vector vel = dir.clone().multiply(1.6);
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 35 || !player.isOnline()) {
                    cancel();
                    return;
                }

                current.add(vel);
                current.getWorld().spawnParticle(Particle.FLAME, current, 3, 0.05, 0.05, 0.05, 0.02);
                current.getWorld().spawnParticle(Particle.SMOKE, current, 1, 0.02, 0.02, 0.02, 0.01);

                if (current.getBlock().getType().isSolid()) {
                    current.getWorld().spawnParticle(Particle.FLAME, current, 10, 0.2, 0.2, 0.2, 0.05);
                    cancel();
                    return;
                }

                for (org.bukkit.entity.Entity e : current.getWorld().getNearbyEntities(current, 0.7, 0.7, 0.7)) {
                    if (e instanceof LivingEntity target && !e.equals(player)) {
                        ElementStatusManager.triggerDamageAndReaction(player, target, 4.5, SpellElement.FIRE, tomeItem);
                        target.setFireTicks(60);
                        current.getWorld().spawnParticle(Particle.EXPLOSION, current, 1);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }
}
