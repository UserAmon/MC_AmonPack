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

import java.util.HashSet;
import java.util.Set;

public class BlazingSpell extends Spell {

    public BlazingSpell() {
        super("blazing", "§c§lBlazing", SpellElement.FIRE, 55, 6.0);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "blazing_mana") || MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "blazing_cd") || MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");
        boolean hasPower = MagicItemManager.hasUpgrade(tomeItem, "blazing_power");

        int effectiveMana = hasManaRed ? Math.max(15, getManaCost() - 15) : getManaCost();
        double effectiveCd = hasCdRed ? Math.max(1.5, getCooldownSeconds() - 1.5) : getCooldownSeconds();

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

        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().setY(0).normalize();
        if (dir.lengthSquared() < 0.01) dir = player.getLocation().getDirection().normalize();
        Vector perp = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        player.getWorld().playSound(start, Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.9f);

        final Vector fDir = dir;
        final Vector fPerp = perp;
        final Set<LivingEntity> hit = new HashSet<>();

        new BukkitRunnable() {
            int step = 0;
            Location currentCenter = start.clone();

            @Override
            public void run() {
                if (step++ > 14 || !player.isOnline()) {
                    cancel();
                    return;
                }

                currentCenter.add(fDir.clone().multiply(1.2));
                for (double offset = -2.0; offset <= 2.0; offset += 0.5) {
                    Location pLoc = currentCenter.clone().add(fPerp.clone().multiply(offset));
                    pLoc.getWorld().spawnParticle(Particle.FLAME, pLoc, 2, 0.1, 0.1, 0.1, 0.02);
                    pLoc.getWorld().spawnParticle(Particle.LAVA, pLoc, 1, 0.05, 0.05, 0.05, 0.0);

                    for (org.bukkit.entity.Entity e : pLoc.getWorld().getNearbyEntities(pLoc, 0.8, 1.2, 0.8)) {
                        if (e instanceof LivingEntity target && !e.equals(player) && !hit.contains(target)) {
                            hit.add(target);
                            ElementStatusManager.triggerDamageAndReaction(player, target, 7.5, SpellElement.FIRE, tomeItem);
                            target.setFireTicks(100);
                        }
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);

        return true;
    }
}
