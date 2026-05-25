package Abilities.Bending;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.SubAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import Plugin.AmonPackPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class SoundAbility extends AirAbility implements SubAbility {
    public static HashMap<Entity, Double> AfffectedEntities = new HashMap<>();

    public SoundAbility(Player player) {
        super(player);
    }

    public Class<? extends Ability> getParentAbility() {
        return AirAbility.class;
    }

    public Element getElement() {
        return Element.SubElement.getElement("Sound");
    }

    public static void HandleDamage(Entity entity, double i) {
        entity.getWorld().spawnParticle(org.bukkit.Particle.SCULK_CHARGE_POP, entity.getLocation(), 15, 1.4, 1.7, 1.4, 0.1);
        if (Math.random() < 0.2) {
            entity.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, entity.getLocation(), 1, 0, 0, 0, 0);
        }
        ParticleEffect.NOTE.display(entity.getLocation(), 10, 1.4, 1.7, 1.4, 0);
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
                List<Entity> ToRemove = new ArrayList<>();
                if (!AfffectedEntities.keySet().isEmpty()) {
                    for (Entity entity : AfffectedEntities.keySet()) {
                        if (entity != null) {
                            if (entity.isDead()) {
                                ToRemove.add(entity);
                            } else {
                                double time = AfffectedEntities.get(entity);
                                if (time > 0) {
                                    if (time >= 20.0) {
                                        ((LivingEntity) entity).damage(4);
                                        ((LivingEntity) entity).addPotionEffect(new PotionEffect(
                                                PotionEffectType.BLINDNESS, 40, 3, false, false, false));
                                        ((LivingEntity) entity).addPotionEffect(
                                                new PotionEffect(PotionEffectType.NAUSEA, 40, 10, false, false, false));
                                        ((LivingEntity) entity).addPotionEffect(new PotionEffect(
                                                PotionEffectType.SLOWNESS, 40, 3, false, false, false));
                                        entity.getWorld().spawnParticle(org.bukkit.Particle.SCULK_CHARGE_POP, entity.getLocation(), 15, 1.4, 1.7, 1.4, 0.1);
                                        if (Math.random() < 0.2) {
                                            entity.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, entity.getLocation(), 1, 0, 0, 0, 0);
                                        }
                                        ParticleEffect.NOTE.display(entity.getLocation(), 10, 1.4, 1.7, 1.4, 0);
                                        ToRemove.add(entity);
                                    } else {
                                        entity.getWorld().spawnParticle(org.bukkit.Particle.SCULK_CHARGE_POP, entity.getLocation(), (int) time, 0.4, 1.7, 0.4, 0.1);
                                        ParticleEffect.NOTE.display(entity.getLocation(), (int) time, 0.4, 1.7, 0.4, 0);
                                        
                                        // Rising pitch note sound effect
                                        float volume = 0.4f + 0.6f * (float)(time / 20.0);
                                        float pitch = 0.5f + 1.5f * (float)(time / 20.0);
                                        entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch);
                                        
                                        AfffectedEntities.put(entity, (time - 0.5));
                                    }
                                }
                            }
                        }
                    }
                    for (Entity ent : ToRemove) {
                        AfffectedEntities.remove(ent);
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 20, 5);
    }
}
