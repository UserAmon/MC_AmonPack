package RPG.Magic.spells.air;

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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BlowSpell extends Spell {

    private final Random random = new Random();

    public BlowSpell() {
        super("blow", "§b§lBlow", SpellElement.AIR, 25, 2.5);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");

        int effectiveMana = hasManaRed ? Math.max(10, getManaCost() - 10) : getManaCost();
        double effectiveCd = hasCdRed ? Math.max(1.0, getCooldownSeconds() - 1.0) : getCooldownSeconds();

        if (isOnCooldown(player)) {
            player.sendMessage("§cZaklęcie " + getName() + " §codnawia się (" + String.format("%.1f", getRemainingCooldown(player)) + "s)!");
            return false;
        }

        if (!manaManager.hasMana(player, effectiveMana)) {
            player.sendMessage("§cBrak many! Wymagane: " + effectiveMana + " MP (" + manaManager.getMana(player) + "/" + manaManager.getMaxMana(player) + ")");
            return false;
        }

        manaManager.consumeMana(player, effectiveMana);
        setCooldown(player, (long) (effectiveCd * 1000));

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        player.getWorld().playSound(eye, Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.6f);
        player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);

        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        if (right.lengthSquared() < 0.01) right = new Vector(1, 0, 0);
        Vector up = right.clone().crossProduct(dir).normalize();

        final Vector fDir = dir;
        final Vector fRight = right;
        final Vector fUp = up;
        final Set<LivingEntity> hit = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;
            Location current = eye.clone();
            double phase = random.nextDouble() * Math.PI * 2;

            @Override
            public void run() {
                if (ticks++ > 40 || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Powolny ruch naprzód ze spiralnymi losowymi odchyleniami
                double spiralOffset = Math.sin(ticks * 0.4 + phase) * 0.45;
                double verticalOffset = Math.cos(ticks * 0.4 + phase) * 0.35;

                Vector forwardStep = fDir.clone().multiply(0.85);
                Vector spiralStep = fRight.clone().multiply(spiralOffset).add(fUp.clone().multiply(verticalOffset));
                current.add(forwardStep).add(spiralStep.multiply(0.3));

                current.getWorld().spawnParticle(Particle.CLOUD, current, 3, 0.1, 0.1, 0.1, 0.01);
                current.getWorld().spawnParticle(Particle.SWEEP_ATTACK, current, 1, 0.05, 0.05, 0.05, 0.0);

                if (current.getBlock().getType().isSolid()) {
                    current.getWorld().spawnParticle(Particle.CLOUD, current, 8, 0.3, 0.3, 0.3, 0.05);
                    cancel();
                    return;
                }

                for (org.bukkit.entity.Entity e : current.getWorld().getNearbyEntities(current, 1.0, 1.0, 1.0)) {
                    if (e instanceof LivingEntity target && !e.equals(player) && !hit.contains(target)) {
                        hit.add(target);

                        // Silny odrzut w kierunku pocisku
                        Vector knockback = fDir.clone().multiply(1.4).setY(0.45);
                        target.setVelocity(knockback);

                        ElementStatusManager.triggerDamageAndReaction(player, target, 5.0, SpellElement.AIR, tomeItem);
                        current.getWorld().playSound(current, Sound.ENTITY_BAT_TAKEOFF, 1.2f, 0.8f);
                        current.getWorld().spawnParticle(Particle.EXPLOSION, current, 1);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);

        return true;
    }
}
