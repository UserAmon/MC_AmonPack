package RPG.Magic.spells.lightning;

import Plugin.AmonPackPlugin;
import RPG.Magic.elements.ElementStatusManager;
import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.model.Spell;
import RPG.Magic.model.SpellElement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class ChainLightningSpell extends Spell {

    public ChainLightningSpell() {
        super("chain_lightning", "§b§lChain Lightning", SpellElement.LIGHTNING, 45, 5.0);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "chain_lightning_mana") || MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "chain_lightning_cd") || MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");

        int effectiveMana = hasManaRed ? Math.max(20, getManaCost() - 15) : getManaCost();
        double effectiveCd = hasCdRed ? Math.max(2.0, getCooldownSeconds() - 1.5) : getCooldownSeconds();

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
        Vector dir = eye.getDirection().normalize();

        player.getWorld().playSound(eye, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 2.0f);
        player.getWorld().playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.6f);

        new BukkitRunnable() {
            Location currentLoc = eye.clone().add(dir.clone().multiply(1.0));
            int distance = 0;
            final int maxDistance = 30;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                for (int step = 0; step < 3; step++) {
                    currentLoc.add(dir.clone().multiply(0.8));
                    distance++;

                    currentLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentLoc, 4, 0.1, 0.1, 0.1, 0.05);
                    currentLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, currentLoc, 1, 0, 0, 0, 0);

                    Block b = currentLoc.getBlock();
                    // Sprawdzenie czy pocisk trafił w wodę
                    if (b.getType() == Material.WATER) {
                        cancel();
                        triggerWaterElectricExplosion(player, currentLoc, tomeItem);
                        return;
                    }

                    if (b.getType().isSolid()) {
                        cancel();
                        currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_ANVIL_LAND, 0.5f, 2.0f);
                        currentLoc.getWorld().spawnParticle(Particle.FLASH, currentLoc, 1);
                        return;
                    }

                    // Sprawdzenie kolizji z przeciwnikami
                    for (org.bukkit.entity.Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5)) {
                        if (e instanceof LivingEntity target && !e.equals(player)) {
                            cancel();
                            triggerChainLightning(player, target, tomeItem);
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

        return true;
    }

    private void triggerChainLightning(Player caster, LivingEntity firstTarget, ItemStack tomeItem) {
        Set<LivingEntity> hitEntities = new HashSet<>();
        hitEntities.add(firstTarget);

        ElementStatusManager.triggerDamageAndReaction(caster, firstTarget, 8.0, SpellElement.LIGHTNING, tomeItem);
        firstTarget.getWorld().playSound(firstTarget.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.8f);

        LivingEntity current = firstTarget;
        int maxBounces = 4;

        for (int bounce = 0; bounce < maxBounces; bounce++) {
            LivingEntity nextTarget = null;
            double closestDist = 8.0;

            for (org.bukkit.entity.Entity nearby : current.getWorld().getNearbyEntities(current.getLocation(), 8.0, 8.0, 8.0)) {
                if (nearby instanceof LivingEntity le && !nearby.equals(caster) && !hitEntities.contains(le)) {
                    double d = nearby.getLocation().distance(current.getLocation());
                    if (d < closestDist) {
                        closestDist = d;
                        nextTarget = le;
                    }
                }
            }

            if (nextTarget == null) break;

            hitEntities.add(nextTarget);
            // Wizualny promień błyskawicy łączący cele
            drawLightningBeam(current.getLocation().add(0, 1.0, 0), nextTarget.getLocation().add(0, 1.0, 0));
            ElementStatusManager.triggerDamageAndReaction(caster, nextTarget, 6.0, SpellElement.LIGHTNING, tomeItem);
            nextTarget.getWorld().playSound(nextTarget.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.6f);

            // Jeśli trafił w moba stojącego w wodzie
            if (nextTarget.getLocation().getBlock().getType() == Material.WATER) {
                triggerWaterElectricExplosion(caster, nextTarget.getLocation(), tomeItem);
            }

            current = nextTarget;
        }
    }

    private void triggerWaterElectricExplosion(Player caster, Location loc, ItemStack tomeItem) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.4f);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.2f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 100, 3.0, 1.5, 3.0, 0.2);
        loc.getWorld().spawnParticle(Particle.SPLASH, loc, 80, 2.0, 1.0, 2.0, 0.1);

        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 6.0, 4.0, 6.0)) {
            if (e instanceof LivingEntity le && !e.equals(caster)) {
                ElementStatusManager.triggerDamageAndReaction(caster, le, 10.0, SpellElement.LIGHTNING, tomeItem);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
            }
        }
    }

    private void drawLightningBeam(Location start, Location end) {
        Vector diff = end.toVector().subtract(start.toVector());
        double dist = start.distance(end);
        Vector dir = diff.clone().normalize();
        int points = (int) (dist * 4);

        for (int i = 0; i <= points; i++) {
            Location p = start.clone().add(dir.clone().multiply(i * 0.25));
            start.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.1, 0.1, 0.1, 0.02);
            if (i % 2 == 0) {
                start.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0, 0, 0, 0);
            }
        }
    }
}
