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
import java.util.Set;

public class AirbladeSpell extends Spell {

    public AirbladeSpell() {
        super("airblade", "§f§lAirblade", SpellElement.AIR, 45, 4.5);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "airblade_mana") || MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "airblade_cd") || MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");
        boolean hasWidth = MagicItemManager.hasUpgrade(tomeItem, "airblade_width");

        int effectiveMana = hasManaRed ? Math.max(15, getManaCost() - 15) : getManaCost();
        double effectiveCd = hasCdRed ? Math.max(1.5, getCooldownSeconds() - 1.5) : getCooldownSeconds();

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

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().setY(0).normalize();
        if (dir.lengthSquared() < 0.01) dir = eye.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.8f);
        player.getWorld().playSound(eye, Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.9f);

        final Vector fDir = dir;
        final Vector fRight = right;
        final Set<LivingEntity> hit = new HashSet<>();

        new BukkitRunnable() {
            int step = 0;
            Location current = eye.clone();

            @Override
            public void run() {
                if (step++ > 18 || !player.isOnline()) {
                    cancel();
                    return;
                }

                current.add(fDir.clone().multiply(1.4));

                // Poziome ostrze o szerokości 3 bloków
                for (double offset = -1.5; offset <= 1.5; offset += 0.5) {
                    Location pLoc = current.clone().add(fRight.clone().multiply(offset));
                    pLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pLoc, 1, 0, 0, 0, 0);
                    pLoc.getWorld().spawnParticle(Particle.CLOUD, pLoc, 2, 0.05, 0.05, 0.05, 0.01);

                    for (org.bukkit.entity.Entity e : pLoc.getWorld().getNearbyEntities(pLoc, 0.8, 1.2, 0.8)) {
                        if (e instanceof LivingEntity target && !e.equals(player) && !hit.contains(target)) {
                            hit.add(target);
                            Vector push = fDir.clone().multiply(0.8).setY(0.3);
                            target.setVelocity(push);
                            ElementStatusManager.triggerDamageAndReaction(player, target, 7.0, SpellElement.AIR, tomeItem);
                        }
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);

        return true;
    }
}
