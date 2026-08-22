package CustomContent.Bosses;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class BossSkillExecutor {

    private static final Random random = new Random();

    public static void executeSkill(Mob boss, String abilityName, double range) {
        if (boss == null || boss.isDead()) return;

        LivingEntity target = boss.getTarget();
        if (target == null) {
            // Find closest player in range
            double closestDist = Double.MAX_VALUE;
            for (Player p : boss.getWorld().getPlayers()) {
                double dist = p.getLocation().distance(boss.getLocation());
                if (dist <= range && dist < closestDist) {
                    closestDist = dist;
                    target = p;
                }
            }
        }

        if (target == null) return;

        Location origin = boss.getEyeLocation();
        Vector dir = target.getEyeLocation().toVector().subtract(origin.toVector()).normalize();

        switch (abilityName.toLowerCase()) {
            case "earthstrike":
            case "boulderroll":
                castEarthSmash(boss, target, dir);
                break;

            case "fireswirl":
            case "flamewhip":
                castFireSwirl(boss, target);
                break;

            case "coil":
            case "arcblast":
                castLightningStrike(boss, target);
                break;

            case "soundcrash":
            case "bassdrop":
                castSonicBoom(boss, target);
                break;

            default:
                castGenericEarthSpike(boss, target);
                break;
        }
    }

    private static void castEarthSmash(Mob boss, LivingEntity target, Vector dir) {
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.2f, 0.6f);
        boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_STONE_BREAK, 1.2f, 0.8f);

        new BukkitRunnable() {
            Location curr = boss.getLocation().clone().add(0, 0.5, 0);
            int steps = 0;

            @Override
            public void run() {
                steps++;
                if (steps > 20 || boss.isDead()) {
                    cancel();
                    return;
                }

                curr.add(dir.clone().multiply(0.8));
                curr.getWorld().spawnParticle(Particle.BLOCK, curr, 15, 0.4, 0.2, 0.4, 0.1, Material.STONE.createBlockData());
                curr.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, curr, 2, 0.2, 0.1, 0.2, 0.01);

                for (Player p : curr.getWorld().getPlayers()) {
                    if (p.getLocation().distance(curr) < 2.0 && !p.getUniqueId().equals(boss.getUniqueId())) {
                        p.damage(12.0, boss);
                        p.setVelocity(new Vector(0, 0.6, 0).add(dir.clone().multiply(0.5)));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private static void castFireSwirl(Mob boss, LivingEntity target) {
        boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.8f);
        Location center = boss.getLocation().clone().add(0, 1, 0);

        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            double x = Math.cos(rad) * 4.0;
            double z = Math.sin(rad) * 4.0;
            Location point = center.clone().add(x, 0, z);
            center.getWorld().spawnParticle(Particle.FLAME, point, 4, 0.2, 0.2, 0.2, 0.05);
            center.getWorld().spawnParticle(Particle.SMOKE, point, 2, 0.2, 0.2, 0.2, 0.02);
        }

        for (Player p : center.getWorld().getPlayers()) {
            if (p.getLocation().distance(center) <= 5.5) {
                p.damage(10.0, boss);
                p.setFireTicks(80);
                p.setVelocity(GeneralMethods.getDirection(center, p.getLocation()).normalize().multiply(0.8).setY(0.3));
            }
        }
    }

    private static void castLightningStrike(Mob boss, LivingEntity target) {
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
        Location targetLoc = target.getLocation();
        targetLoc.getWorld().strikeLightningEffect(targetLoc);
        target.damage(14.0, boss);
    }

    private static void castSonicBoom(Mob boss, LivingEntity target) {
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.0f);
        Location start = boss.getEyeLocation();
        Vector dir = target.getEyeLocation().toVector().subtract(start.toVector()).normalize();

        for (double d = 0; d < 12; d += 0.8) {
            Location pLoc = start.clone().add(dir.clone().multiply(d));
            pLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, pLoc, 1, 0, 0, 0, 0);
        }
        target.damage(15.0, boss);
        target.setVelocity(dir.clone().multiply(1.2).setY(0.4));
    }

    private static void castGenericEarthSpike(Mob boss, LivingEntity target) {
        Location tLoc = target.getLocation();
        tLoc.getWorld().playSound(tLoc, Sound.BLOCK_ROOTED_DIRT_BREAK, 1.2f, 0.6f);
        tLoc.getWorld().spawnParticle(Particle.BLOCK, tLoc, 30, 0.5, 0.5, 0.5, 0.1, Material.STONE.createBlockData());
        target.damage(10.0, boss);
        target.setVelocity(new Vector(0, 0.5, 0));
    }
}
