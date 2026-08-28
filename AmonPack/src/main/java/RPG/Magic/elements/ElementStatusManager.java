package RPG.Magic.elements;

import Plugin.AmonPackPlugin;
import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.model.SpellElement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ElementStatusManager {

    private static final Map<UUID, Map<SpellElement, Long>> activeStatuses = new ConcurrentHashMap<>();

    public static void init() {
        Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, () -> {
            long now = System.currentTimeMillis();
            for (Iterator<Map.Entry<UUID, Map<SpellElement, Long>>> it = activeStatuses.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Map<SpellElement, Long>> entry = it.next();
                org.bukkit.entity.Entity entity = Bukkit.getEntity(entry.getKey());
                if (entity == null || !entity.isValid() || entity.isDead()) {
                    it.remove();
                    continue;
                }

                Map<SpellElement, Long> statuses = entry.getValue();
                statuses.entrySet().removeIf(e -> e.getValue() < now);
                if (statuses.isEmpty()) {
                    it.remove();
                    continue;
                }

                Location loc = entity.getLocation().add(0, entity.getHeight() + 0.3, 0);
                for (SpellElement el : statuses.keySet()) {
                    switch (el) {
                        case FIRE -> loc.getWorld().spawnParticle(Particle.FLAME, loc, 2, 0.2, 0.1, 0.2, 0.01);
                        case WATER -> loc.getWorld().spawnParticle(Particle.FALLING_WATER, loc, 3, 0.2, 0.1, 0.2, 0.0);
                        case AIR -> loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0.1, 0.1, 0.1, 0.0);
                        case LIGHTNING -> loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 2, 0.2, 0.1, 0.2, 0.02);
                        case EARTH -> loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 2, 0.2, 0.1, 0.2, 0.0);
                        default -> {}
                    }
                }
            }
        }, 10L, 10L);
    }

    public static void applyElementStatus(LivingEntity entity, SpellElement element, double durationSec) {
        if (entity == null || !entity.isValid() || entity.isDead()) return;
        long expiry = System.currentTimeMillis() + (long) (durationSec * 1000);
        activeStatuses.computeIfAbsent(entity.getUniqueId(), k -> new ConcurrentHashMap<>()).put(element, expiry);
    }

    public static boolean hasElementStatus(LivingEntity entity, SpellElement element) {
        if (entity == null) return false;
        Map<SpellElement, Long> statuses = activeStatuses.get(entity.getUniqueId());
        if (statuses == null) return false;
        Long expiry = statuses.get(element);
        if (expiry == null) return false;
        if (expiry < System.currentTimeMillis()) {
            statuses.remove(element);
            return false;
        }
        return true;
    }

    public static void removeElementStatus(LivingEntity entity, SpellElement element) {
        if (entity == null) return;
        Map<SpellElement, Long> statuses = activeStatuses.get(entity.getUniqueId());
        if (statuses != null) {
            statuses.remove(element);
        }
    }

    public static void triggerDamageAndReaction(Player caster, LivingEntity victim, double damage, SpellElement attackElement, ItemStack tomeItem) {
        triggerDamageAndReaction(caster, victim, damage, attackElement, tomeItem, false);
    }

    public static void triggerDamageAndReaction(Player caster, LivingEntity victim, double damage, SpellElement attackElement, ItemStack tomeItem, boolean ignoreArmor) {
        if (victim == null || !victim.isValid() || victim.isDead()) return;

        double finalDamage = damage;
        boolean reactionTriggered = false;

        // 1. REAKCJA: OGIEŃ + POWIETRZE -> PŁOMIENNY WIR (Swirl)
        if ((attackElement == SpellElement.FIRE && hasElementStatus(victim, SpellElement.AIR)) ||
            (attackElement == SpellElement.AIR && hasElementStatus(victim, SpellElement.FIRE))) {
            reactionTriggered = true;
            removeElementStatus(victim, SpellElement.FIRE);
            removeElementStatus(victim, SpellElement.AIR);

            Location loc = victim.getLocation().add(0, 0.5, 0);
            loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.8f);
            loc.getWorld().spawnParticle(Particle.FLAME, loc, 50, 1.5, 0.5, 1.5, 0.1);
            loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 10, 1.0, 0.5, 1.0, 0.1);

            for (org.bukkit.entity.Entity nearby : victim.getWorld().getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
                if (nearby instanceof LivingEntity le && !nearby.equals(caster)) {
                    if (ignoreArmor && le instanceof Player p) {
                        p.setMetadata("magic_ignore_armor", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, true));
                    }
                    le.damage(5.0, caster);
                    le.setFireTicks(80);
                }
            }
        }
        // 2. REAKCJA: WODA + PIORUN -> PORAŻENIE PRĄDEM (Electro-Charged)
        else if ((attackElement == SpellElement.LIGHTNING && hasElementStatus(victim, SpellElement.WATER)) ||
                 (attackElement == SpellElement.WATER && hasElementStatus(victim, SpellElement.LIGHTNING))) {
            reactionTriggered = true;
            removeElementStatus(victim, SpellElement.WATER);
            removeElementStatus(victim, SpellElement.LIGHTNING);

            Location loc = victim.getLocation();
            victim.getWorld().strikeLightningEffect(loc);
            loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);

            for (org.bukkit.entity.Entity nearby : victim.getWorld().getNearbyEntities(loc, 5.0, 5.0, 5.0)) {
                if (nearby instanceof LivingEntity le && !nearby.equals(caster)) {
                    if (ignoreArmor && le instanceof Player p) {
                        p.setMetadata("magic_ignore_armor", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, true));
                    }
                    le.damage(7.0, caster);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                }
            }
        }
        // 3. REAKCJA: OGIEŃ + WODA -> PAROWANIE (Vaporize)
        else if ((attackElement == SpellElement.FIRE && hasElementStatus(victim, SpellElement.WATER)) ||
                 (attackElement == SpellElement.WATER && hasElementStatus(victim, SpellElement.FIRE))) {
            reactionTriggered = true;
            removeElementStatus(victim, SpellElement.FIRE);
            removeElementStatus(victim, SpellElement.WATER);

            finalDamage *= 1.5;
            Location loc = victim.getLocation().add(0, 1.0, 0);
            loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 30, 0.8, 0.8, 0.8, 0.05);
            loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
        }

        // Zadanie obrażeń głównemu celowi
        if (ignoreArmor && victim instanceof Player p) {
            p.setMetadata("magic_ignore_armor", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, true));
        }
        victim.damage(finalDamage, caster);

        if (!reactionTriggered) {
            applyElementStatus(victim, attackElement, 6.0);
        }

        if (caster != null && tomeItem != null && (victim.isDead() || victim.getHealth() <= 0)) {
            MagicItemManager.recordMagicKill(caster, tomeItem);
        }
    }
}
