package RPG.Magic.spells.fire;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class FireCircleSpell extends Spell {

    public FireCircleSpell() {
        super("fire_circle", "§6§lFire Circle", SpellElement.FIRE, 50, 7.0);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "fire_circle_mana") || MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "fire_circle_cd") || MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");
        boolean hasBarrier = MagicItemManager.hasUpgrade(tomeItem, "fire_circle_barrier");

        int effectiveMana = hasManaRed ? Math.max(15, getManaCost() - 15) : getManaCost();
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

        Location center = player.getLocation().add(0, 0.2, 0);
        center.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.7f);
        center.getWorld().playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.5f);

        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 120, 0));

        double radius = 4.0;
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location pLoc = center.clone().add(x, 0.3, z);
            center.getWorld().spawnParticle(Particle.FLAME, pLoc, 3, 0.1, 0.2, 0.1, 0.03);
            center.getWorld().spawnParticle(Particle.SMOKE, pLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, radius + 1.0, 2.5, radius + 1.0)) {
            if (e instanceof LivingEntity target && !e.equals(player)) {
                Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2).setY(0.4);
                target.setVelocity(push);
                ElementStatusManager.triggerDamageAndReaction(player, target, 6.5, SpellElement.FIRE, tomeItem);
                target.setFireTicks(100);
            }
        }

        return true;
    }
}
