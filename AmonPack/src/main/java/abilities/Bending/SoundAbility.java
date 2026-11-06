package Abilities.Bending;

import Plugin.AmonPackPlugin;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Autorska mechanika dźwiękowa (Sound Ability / Deafness / Rezonans akustyczny)
 * wykorzystywana w Dungeonach, broniach oraz systemie RPG.
 * Całkowicie uniezależniona od bibliotek ProjectKorra.
 */
public class SoundAbility {

    public static HashMap<Entity, Double> AfffectedEntities = new HashMap<>();
    public static HashMap<Entity, Player> stackAppliers = new HashMap<>();

    public static void HandleDamage(Entity entity, double i) {
        HandleDamage(null, entity, i);
    }

    public static void HandleDamage(Player player, Entity entity, double i) {
        if (entity == null) return;
        if (player != null) {
            RPG.Levels.BendingTree.PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
            if (branch != null && branch.hasUpgrade("Encore")) {
                i += 1.0;
            }
            stackAppliers.put(entity, player);
        }
        entity.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, entity.getLocation(), 15, 1.4, 1.7, 1.4, 0.1);
        if (Math.random() < 0.2) {
            entity.getWorld().spawnParticle(Particle.SONIC_BOOM, entity.getLocation(), 1, 0, 0, 0, 0);
        }
        entity.getWorld().spawnParticle(Particle.NOTE, entity.getLocation(), 10, 1.4, 1.7, 1.4, 0);
        if (!AfffectedEntities.isEmpty() && AfffectedEntities.get(entity) != null) {
            Double actual = AfffectedEntities.get(entity);
            AfffectedEntities.put(entity, actual + i);
        } else {
            AfffectedEntities.put(entity, i);
        }
    }

    public static void StartDeafnessTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Entity> toRemove = new ArrayList<>();
                if (!AfffectedEntities.keySet().isEmpty()) {
                    for (Entity entity : AfffectedEntities.keySet()) {
                        if (entity != null) {
                            if (entity.isDead()) {
                                toRemove.add(entity);
                            } else {
                                double time = AfffectedEntities.get(entity);
                                if (time > 0) {
                                    double limit = 20.0;
                                    double decay = 0.5;
                                    Player applier = stackAppliers.get(entity);
                                    if (applier != null) {
                                        RPG.Levels.BendingTree.PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(applier.getName()) : null;
                                        if (branch != null && branch.hasUpgrade("Dysonance")) {
                                            limit = 25.0;
                                            decay = 0.25;
                                        }
                                    }
                                    
                                    if (time >= limit) {
                                        if (entity instanceof LivingEntity) {
                                            LivingEntity living = (LivingEntity) entity;
                                            living.damage(4);
                                            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 3, false, false, false));
                                            living.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 10, false, false, false));
                                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3, false, false, false));
                                        }
                                        entity.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, entity.getLocation(), 15, 1.4, 1.7, 1.4, 0.1);
                                        if (Math.random() < 0.2) {
                                            entity.getWorld().spawnParticle(Particle.SONIC_BOOM, entity.getLocation(), 1, 0, 0, 0, 0);
                                        }
                                        entity.getWorld().spawnParticle(Particle.NOTE, entity.getLocation(), 10, 1.4, 1.7, 1.4, 0);
                                        toRemove.add(entity);
                                    } else {
                                        entity.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, entity.getLocation(), (int) Math.max(1, time), 0.4, 1.7, 0.4, 0.1);
                                        entity.getWorld().spawnParticle(Particle.NOTE, entity.getLocation(), (int) Math.max(1, time), 0.4, 1.7, 0.4, 0);
                                        
                                        // Dźwięk rezonansu
                                        float volume = 0.4f + 0.6f * (float)(time / limit);
                                        float pitch = 0.5f + 1.5f * (float)(time / limit);
                                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch);
                                        
                                        AfffectedEntities.put(entity, (time - decay));
                                    }
                                }
                            }
                        }
                    }
                    for (Entity ent : toRemove) {
                        AfffectedEntities.remove(ent);
                        stackAppliers.remove(ent);
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 20, 5);
    }
}
