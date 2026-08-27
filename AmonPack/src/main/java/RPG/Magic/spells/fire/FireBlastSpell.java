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

public class FireBlastSpell extends Spell {

    public FireBlastSpell() {
        super("fireblast", "§c§lFire Blast", SpellElement.FIRE, 40, 4.0);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");
        boolean hasMultiCast = MagicItemManager.hasUpgrade(tomeItem, "multi_cast");
        boolean hasFireChain = MagicItemManager.hasUpgrade(tomeItem, "fire_chain");

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

        launchProjectile(player, eye, dir, tomeItem, hasFireChain, 0, new HashSet<>());

        if (hasMultiCast) {
            Vector leftDir = dir.clone().rotateAroundY(Math.toRadians(12));
            Vector rightDir = dir.clone().rotateAroundY(Math.toRadians(-12));
            launchProjectile(player, eye, leftDir, tomeItem, hasFireChain, 0, new HashSet<>());
            launchProjectile(player, eye, rightDir, tomeItem, hasFireChain, 0, new HashSet<>());
        }

        player.getWorld().playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
        return true;
    }

    private void launchProjectile(Player player, Location startLoc, Vector initialDir, ItemStack tomeItem, boolean hasChain, int chainCount, Set<LivingEntity> hitEntities) {
        new BukkitRunnable() {
            Location current = startLoc.clone();
            Vector velocity = initialDir.clone().multiply(1.3);
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 50 || !player.isOnline()) {
                    cancel();
                    return;
                }

                velocity.setY(velocity.getY() - 0.025);
                current.add(velocity);

                current.getWorld().spawnParticle(Particle.FLAME, current, 4, 0.08, 0.08, 0.08, 0.02);
                current.getWorld().spawnParticle(Particle.SMOKE, current, 2, 0.05, 0.05, 0.05, 0.01);

                if (current.getBlock().getType().isSolid()) {
                    explode(current, player, tomeItem, null, hasChain, chainCount, hitEntities);
                    cancel();
                    return;
                }

                for (org.bukkit.entity.Entity e : current.getWorld().getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                    if (e instanceof LivingEntity target && !e.equals(player) && !hitEntities.contains(target)) {
                        hitEntities.add(target);
                        explode(current, player, tomeItem, target, hasChain, chainCount, hitEntities);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void explode(Location loc, Player player, ItemStack tomeItem, LivingEntity directHit, boolean hasChain, int chainCount, Set<LivingEntity> hitEntities) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 25, 0.4, 0.4, 0.4, 0.08);

        LivingEntity chainTarget = null;
        boolean wasInFireStatus = false;

        if (directHit != null && directHit.isValid() && !directHit.isDead()) {
            wasInFireStatus = ElementStatusManager.hasElementStatus(directHit, SpellElement.FIRE);
            ElementStatusManager.triggerDamageAndReaction(player, directHit, 6.0, SpellElement.FIRE, tomeItem);
            directHit.setFireTicks(80);
            if (wasInFireStatus) chainTarget = directHit;
        }

        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 2.2, 2.2, 2.2)) {
            if (e instanceof LivingEntity target && !e.equals(player) && (directHit == null || !e.equals(directHit))) {
                if (ElementStatusManager.hasElementStatus(target, SpellElement.FIRE)) {
                    wasInFireStatus = true;
                    if (chainTarget == null) chainTarget = target;
                }
                ElementStatusManager.triggerDamageAndReaction(player, target, 4.0, SpellElement.FIRE, tomeItem);
                target.setFireTicks(60);
                hitEntities.add(target);
            }
        }

        // Mechanika łańcuchowa (Fire Chain Upgrade): jeśli trafiony cel był w Stanie Ognia -> rykoszet do max 3 wrogów w 8 blokach
        if (hasChain && wasInFireStatus && chainCount < 3) {
            Location origin = directHit != null ? directHit.getEyeLocation() : loc;
            LivingEntity nextTarget = findNextChainTarget(origin, player, hitEntities, 8.0);
            if (nextTarget != null) {
                hitEntities.add(nextTarget);
                Vector homingDir = nextTarget.getEyeLocation().toVector().subtract(origin.toVector()).normalize();
                if (player != null) {
                    player.sendMessage("§4§l✦ OGNISTY ŁAŃCUCH! ✦ §fPocisk przeskakuje na kolejnego wroga! §7(" + (chainCount + 1) + "/3)");
                }
                launchProjectile(player, origin, homingDir, tomeItem, true, chainCount + 1, hitEntities);
            }
        }
    }

    private LivingEntity findNextChainTarget(Location origin, Player player, Set<LivingEntity> hitEntities, double radius) {
        LivingEntity closest = null;
        double minDistance = Double.MAX_VALUE;
        for (org.bukkit.entity.Entity e : origin.getWorld().getNearbyEntities(origin, radius, radius, radius)) {
            if (e instanceof LivingEntity target && !e.equals(player) && !hitEntities.contains(target) && target.isValid() && !target.isDead()) {
                double dist = e.getLocation().distanceSquared(origin);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = target;
                }
            }
        }
        return closest;
    }
}
