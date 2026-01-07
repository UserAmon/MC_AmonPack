package abilities.Util_Objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.util.DamageHandler;

import methods_plugins.AmonPackPlugin;

public class EarthDisc {

    private static List<EarthDisc> instances = new ArrayList<>();

    private Player player;
    private Location location;
    private Vector direction;
    private double damage;
    private double speed;
    private boolean destroyOnEntityHit;
    private long spawnTime;
    private double radius = 0.5;
    private BukkitRunnable runnable;
    private boolean isDead = false;

    public EarthDisc(Player player, Location location, Vector direction, double damage, double speed, boolean destroyOnEntityHit) {
        this.player = player;
        this.location = location;
        this.direction = direction.normalize();
        this.damage = damage;
        this.speed = speed;
        this.destroyOnEntityHit = destroyOnEntityHit;
        this.spawnTime = System.currentTimeMillis();
        instances.add(this);
        start();
    }

    private void start() {
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (isDead || !player.isOnline() || player.isDead()) {
                    remove();
                    this.cancel();
                    return;
                }
                if (System.currentTimeMillis() - spawnTime > 5000) {
                    explode();
                    remove();
                    this.cancel();
                    return;
                }
                progress();
            }
        };
        runnable.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
    }

    private void progress() {
        Vector velocity = direction.clone().multiply(speed);

        org.bukkit.util.RayTraceResult result = location.getWorld().rayTraceBlocks(location, velocity, speed,
                org.bukkit.FluidCollisionMode.NEVER, true);

        if (result != null && result.getHitBlock() != null) {
            Block hitBlock = result.getHitBlock();
            if (hitBlock.getType().isSolid()) {
                BlockFace face = result.getHitBlockFace();

                if (face != null) {
                    // Reflect vector: v = v - 2 * (v . n) * n
                    Vector normal = new Vector(face.getModX(), face.getModY(), face.getModZ());
                    double dot = direction.dot(normal);
                    direction.subtract(normal.multiply(2 * dot));

                    player.getWorld().playSound(location, Sound.BLOCK_STONE_HIT, 1f, 1.5f);
                    // Move slightly off the surface to prevent sticking
                    location.add(result.getHitPosition().subtract(location.toVector()).multiply(0.9));
                } else {
                    explode();
                    remove();
                    return;
                }
            }
        } else {
            location.add(velocity);
        }

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.5)) {
            if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                ((LivingEntity) entity).damage(damage);
                Vector forceDir = GeneralMethods.getDirection(entity.getLocation(), location.clone().subtract(0,1,0));
                entity.setVelocity(forceDir.clone().normalize().multiply(-1));
                if (destroyOnEntityHit) {
                    explode();
                    remove();
                    return;
                }
            }
        }

        display();
    }

    private void display() {
        World world = location.getWorld();
        Vector baseVector = new Vector(0, 0.5, 0);
        Particle.DustOptions dustOptions =
                new Particle.DustOptions(Color.fromRGB(209, 201, 148), 0.5f);
        Particle.DustOptions dustOptionsBrown =
                new Particle.DustOptions(Color.fromRGB(87, 56, 11), 0.6f);
        for (double angle = 0; angle < 360; angle += 30) {
            if(new Random().nextDouble()>0.25){
                Vector blockOffset = GeneralMethods.getOrthogonalVector(baseVector, angle, radius);
                world.spawnParticle(
                        Particle.BLOCK,
                        location.clone().add(blockOffset),
                        1, 0, 0, 0, 0,
                        Material.SANDSTONE.createBlockData()
                );
            }

            Vector dustOffset = GeneralMethods.getOrthogonalVector(baseVector, angle, radius / 2);
            world.spawnParticle(
                    Particle.DUST,
                    location.clone().add(dustOffset),
                    1, 0, 0, 0, 0,
                    dustOptions
            );

            Vector BrowndustOffset = GeneralMethods.getOrthogonalVector(baseVector, angle, radius +0.25);
            world.spawnParticle(
                    Particle.DUST,
                    location.clone().add(BrowndustOffset),
                    1, 0, 0, 0, 0,
                    dustOptionsBrown
            );
        }

    }

    public static void displayParticle(Location location) {
        Vector baseVector = new Vector(0, 0.5, 0);
        Particle.DustOptions dustOptions =
                new Particle.DustOptions(Color.fromRGB(209, 201, 148), 0.5f);
        Particle.DustOptions dustOptionsBrown =
                new Particle.DustOptions(Color.fromRGB(87, 56, 11), 0.6f);
        for (double angle = 0; angle < 360; angle += 30) {
            if(new Random().nextDouble()>0.25){
                Vector blockOffset = GeneralMethods.getOrthogonalVector(baseVector, angle, 0.4);
                location.getWorld().spawnParticle(
                        Particle.BLOCK,
                        location.clone().add(blockOffset),
                        1, 0, 0, 0, 0,
                        Material.SANDSTONE.createBlockData()
                );
            }
            Vector dustOffset = GeneralMethods.getOrthogonalVector(baseVector, angle, 0.2);
            location.getWorld().spawnParticle(
                    Particle.DUST,
                    location.clone().add(dustOffset),
                    1, 0, 0, 0, 0,
                    dustOptions
            );
            Vector BrowndustOffset = GeneralMethods.getOrthogonalVector(baseVector, angle, 0.6);
            location.getWorld().spawnParticle(
                    Particle.DUST,
                    location.clone().add(BrowndustOffset),
                    1, 0, 0, 0, 0,
                    dustOptionsBrown
            );
        }
    }

    public void explode() {
        location.getWorld().spawnParticle(Particle.BLOCK, location, 10, 0.5, 0.5, 0.5, Material.DIRT.createBlockData());
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
    }

    public void remove() {
        isDead = true;
        instances.remove(this);
        if (runnable != null && !runnable.isCancelled()) {
            runnable.cancel();
        }
    }

    public void redirect(Vector newDir) {
        this.direction = newDir.normalize();
        this.spawnTime = System.currentTimeMillis();
        player.getWorld().playSound(location, Sound.ENTITY_GHAST_SHOOT, 0.5f, 1.5f);
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    public static void redirectNearby(Player player, double range) {
        List<EarthDisc> toRedirect = new ArrayList<>();
        for (EarthDisc disc : instances) {
            if (disc.getLocation().distance(player.getLocation()) <= range) {
                toRedirect.add(disc);
            }
        }
        for (EarthDisc disc : toRedirect) {
            disc.redirect(player.getLocation().getDirection());
        }
    }
}
