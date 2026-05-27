package RPG.Dungeons;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

public class DungeonBlessingManager {

    private static final Random random = new Random();

    public static void handleVampirism(Player killer, LivingEntity victim, DungeonPlayerStats stats) {
        if (stats == null) return;
        int lvl = stats.getBlessingLevel("VAMPIRISM");
        if (lvl <= 0) return;

        if (victim.getFireTicks() > 0) {
            if (random.nextInt(100) < (5 * lvl)) {
                double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();
                double currentHealth = killer.getHealth();
                double newHealth = Math.min(maxHealth, currentHealth + 4.0);
                
                killer.setHealth(newHealth);
                
                killer.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.1);
                killer.getWorld().spawnParticle(Particle.FLAME, killer.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                killer.sendMessage(ChatColor.DARK_RED + "[Wampiryzm] " + ChatColor.RED + "+4 HP za zgładzenie płonącego wroga!");
            }
        }
    }

    public static boolean handleDodge(Player player, EntityDamageEvent event, DungeonPlayerStats stats) {
        if (stats == null) return false;
        int lvl = stats.getBlessingLevel("DODGE");
        if (lvl <= 0) return false;

        if (random.nextInt(100) < (5 * lvl)) {
            event.setDamage(0);
            event.setCancelled(true);
            
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 3, 0.1, 0.1, 0.1, 0.0);
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_LOOP, 1.0f, 2.0f);
            player.sendMessage(ChatColor.GREEN + "[Unik!] Uniknąłeś ciosu cieni!");
            return true;
        }
        return false;
    }

    public static void handleAdrenaline(Player player, EntityDamageByEntityEvent event, DungeonPlayerStats stats) {
        if (stats == null) return;
        int lvl = stats.getBlessingLevel("ADRENALINE");
        if (lvl <= 0) return;

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();

        if (currentHealth < (maxHealth * 0.20)) {
            if (random.nextInt(100) < (5 * lvl)) {
                double originalDamage = event.getDamage();
                double newDamage = originalDamage * 1.50;
                event.setDamage(newDamage);
                
                player.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.2);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.8f);
            }
        }
    }

    public static void handleLifesteal(Player attacker, LivingEntity victim, double damage, DungeonPlayerStats stats) {
        if (stats == null) return;
        int lvl = stats.getBlessingLevel("LIFESTEAL");
        if (lvl <= 0) return;
        if (random.nextInt(100) < (5 * lvl)) {
            double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
            double healAmount = damage * 0.20;
            attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + healAmount));
            attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0.0);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        }
    }

    public static void handleHeal(Player player, org.bukkit.event.entity.EntityRegainHealthEvent event, DungeonPlayerStats stats) {
        if (stats == null) return;
        int lvl = stats.getBlessingLevel("HEAL");
        if (lvl <= 0) return;
        if (random.nextInt(100) < (5 * lvl)) {
            double extra = event.getAmount() * 0.50;
            event.setAmount(event.getAmount() + extra);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.0, 0), 5, 0.3, 0.3, 0.3, 0.05);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.8f);
        }
    }

    public static void handleKnockback(Player attacker, LivingEntity victim, DungeonPlayerStats stats) {
        if (stats == null) return;
        int lvl = stats.getBlessingLevel("KNOCKBACK");
        if (lvl <= 0) return;
        if (random.nextInt(100) < (5 * lvl)) {
            org.bukkit.util.Vector dir = victim.getLocation().toVector().subtract(attacker.getLocation().toVector());
            if (dir.lengthSquared() > 0) {
                dir.normalize();
            } else {
                dir = new org.bukkit.util.Vector(0, 0, 1);
            }
            dir.setY(0.35);
            victim.setVelocity(dir.multiply(1.2));
            victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 1.0, 0), 5, 0.2, 0.2, 0.2, 0.05);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
        }
    }

    public static boolean handleCounter(Player player, LivingEntity attacker, double damage, DungeonPlayerStats stats, EntityDamageByEntityEvent event) {
        if (stats == null) return false;
        int lvl = stats.getBlessingLevel("COUNTER");
        if (lvl <= 0) return false;
        if (random.nextInt(100) < (5 * lvl)) {
            event.setDamage(0);
            event.setCancelled(true);
            attacker.damage(damage, player);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.0, 0), 3, 0.1, 0.1, 0.1, 0.0);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.2f);
            return true;
        }
        return false;
    }

    public static void handleFlaming(Player attacker, LivingEntity victim, DungeonPlayerStats stats) {
        if (stats == null) return;
        int lvl = stats.getBlessingLevel("FLAMING");
        if (lvl <= 0) return;
        if (random.nextInt(100) < (5 * lvl)) {
            victim.setFireTicks(80 * lvl);
            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0, 1.0, 0), 8, 0.2, 0.2, 0.2, 0.05);
            attacker.playSound(attacker.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);
        }
    }

    public static void handlePoison(Player attacker, LivingEntity victim, DungeonPlayerStats stats) {
        if (stats == null) return;
        int lvl = stats.getBlessingLevel("POISON");
        if (lvl <= 0) return;
        if (random.nextInt(100) < (5 * lvl)) {
            victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 80, 0));
        }
    }

    public static void handleCombustion(Player attacker, LivingEntity victim, DungeonPlayerStats stats, EntityDamageByEntityEvent event) {
        if (stats == null) return;
        int lvl = stats.getBlessingLevel("COMBUSTION");
        if (lvl <= 0) return;
        if (victim.getFireTicks() > 0) {
            if (random.nextInt(100) < (5 * lvl)) {
                event.setDamage(event.getDamage() * 1.25);
            }
        }
    }
}
