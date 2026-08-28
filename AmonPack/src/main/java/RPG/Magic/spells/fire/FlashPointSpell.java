package RPG.Magic.spells.fire;

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
import org.bukkit.util.RayTraceResult;

public class FlashPointSpell extends Spell {

    public FlashPointSpell() {
        super("flashpoint", "§4§lFlashPoint", SpellElement.FIRE, 70, 8.0);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "flashpoint_mana") || MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "flashpoint_cd") || MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");
        boolean hasRadius = MagicItemManager.hasUpgrade(tomeItem, "flashpoint_radius");

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

        RayTraceResult result = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                25.0,
                FluidCollisionMode.NEVER,
                true,
                0.5,
                e -> e instanceof LivingEntity && !e.equals(player)
        );

        Location targetLoc;
        LivingEntity directHit = null;

        if (result != null && result.getHitEntity() instanceof LivingEntity le) {
            targetLoc = le.getLocation().add(0, 0.5, 0);
            directHit = le;
        } else if (result != null && result.getHitPosition() != null) {
            targetLoc = result.getHitPosition().toLocation(player.getWorld());
        } else {
            targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(20.0));
        }

        manaManager.consumeMana(player, effectiveMana);
        setCooldown(player, (long) (effectiveCd * 1000));

        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.8f);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.5f);
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 1);
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 60, 0.8, 0.8, 0.8, 0.2);
        targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 20, 0.5, 0.5, 0.5, 0.1);

        if (directHit != null && directHit.isValid()) {
            ElementStatusManager.triggerDamageAndReaction(player, directHit, getBaseDamage(), SpellElement.FIRE, tomeItem);
            directHit.setFireTicks(120);
        }

        for (org.bukkit.entity.Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, 3.5, 3.5, 3.5)) {
            if (e instanceof LivingEntity target && !e.equals(player) && (directHit == null || !e.equals(directHit))) {
                ElementStatusManager.triggerDamageAndReaction(player, target, getBaseDamage() * 0.7, SpellElement.FIRE, tomeItem);
                target.setFireTicks(80);
            }
        }

        return true;
    }
}
