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

    /**
     * Handles the VAMPIRISM blessing: heals the killer if the killed entity was on fire.
     */
    public static void handleVampirism(Player killer, LivingEntity victim, DungeonPlayerStats stats) {
        if (stats == null || !stats.hasBlessing("VAMPIRISM")) return;

        // Check if victim was on fire
        if (victim.getFireTicks() > 0) {
            double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();
            double currentHealth = killer.getHealth();
            double newHealth = Math.min(maxHealth, currentHealth + 4.0); // Heal 2 hearts (4 HP)
            
            killer.setHealth(newHealth);
            
            // Visual and audio effects
            killer.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.1);
            killer.getWorld().spawnParticle(Particle.FLAME, killer.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            killer.sendMessage(ChatColor.DARK_RED + "[Wampiryzm] " + ChatColor.RED + "+4 HP za zgładzenie płonącego wroga!");
        }
    }

    /**
     * Handles the DODGE blessing: 10% chance to dodge all incoming damage.
     * Returns true if the damage was dodged (and thus event should be modified/cancelled).
     */
    public static boolean handleDodge(Player player, EntityDamageEvent event, DungeonPlayerStats stats) {
        if (stats == null || !stats.hasBlessing("DODGE")) return false;

        // 10% chance
        if (random.nextInt(100) < 10) {
            event.setDamage(0);
            event.setCancelled(true);
            
            // Visual and audio effects
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 3, 0.1, 0.1, 0.1, 0.0);
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_LOOP, 1.0f, 2.0f);
            player.sendMessage(ChatColor.GREEN + "[Unik!] Uniknąłeś ciosu cieni!");
            return true;
        }
        return false;
    }

    /**
     * Handles the ADRENALINE blessing: +35% damage when health is below 20%.
     */
    public static void handleAdrenaline(Player player, EntityDamageByEntityEvent event, DungeonPlayerStats stats) {
        if (stats == null || !stats.hasBlessing("ADRENALINE")) return;

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();

        // Check if below 20% health
        if (currentHealth < (maxHealth * 0.20)) {
            double originalDamage = event.getDamage();
            double newDamage = originalDamage * 1.35; // +35% boost
            event.setDamage(newDamage);
            
            // Visual and audio effects
            player.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.2);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.8f);
        }
    }
}
