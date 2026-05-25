package Abilities.PK_Abilities.Air;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AerialPush extends AirAbility implements AddonAbility {

    private enum State {
        PROJECTILE, MONITORING
    }

    private State state;
    private Location location;
    private Vector direction;
    private Location startLocation;
    private LivingEntity target;
    private long monitorStartTime;

    // Configurable variables
    private double damage = 2.0;
    private double wallDamage = 4.0;
    private double knockback = 2.5;
    private double speed = 1.5;
    private double range = 20;
    private long cooldown = 3000;
    private long monitorDuration = 1000; // 1 second to hit a wall

    public AerialPush(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        this.startLocation = player.getEyeLocation();
        this.location = player.getEyeLocation();
        this.direction = player.getEyeLocation().getDirection().normalize();
        this.state = State.PROJECTILE;
        bPlayer.addCooldown(this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (state == State.PROJECTILE) {
            progressProjectile();
        } else if (state == State.MONITORING) {
            progressMonitoring();
        }
    }

    private void progressProjectile() {
        if (location.distanceSquared(startLocation) > range * range || !isTransparent(location.getBlock())) {
            remove();
            return;
        }

        location.add(direction.clone().multiply(speed));

        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        for (double i = -0.5; i <= 0.5; i += 0.2) {
            Vector offset = right.clone().multiply(i);
            Vector curve = direction.clone().multiply(-Math.abs(i) * 0.5);
            Location pLoc = location.clone().add(offset).add(curve);
            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.GRAY, 0.75f);
            player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0.1, 0.1, 0.1, 0, dustOptions);
            player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(Color.WHITE, 0.75f));
        }

        // Play sound occasionally
        if (Math.random() < 0.3) {
            location.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
        }

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.5)) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                hitEntity((LivingEntity) entity);
                return;
            }
        }
    }

    private void hitEntity(LivingEntity entity) {
        DamageHandler.damageEntity(entity, damage, this);
        entity.setVelocity(direction.clone().multiply(knockback).setY(0.5));
        this.target = entity;
        this.monitorStartTime = System.currentTimeMillis();
        this.state = State.MONITORING;

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);
    }

    private void progressMonitoring() {
        if (target == null || target.isDead() || !target.isValid()) {
            remove();
            return;
        }

        if (System.currentTimeMillis() - monitorStartTime > monitorDuration) {
            remove();
            return;
        }

        Vector velocity = target.getVelocity();
        double speed = velocity.length();

        if (speed > 0.1) {
            Location checkLoc = target.getEyeLocation();
            double traceDist = speed + 0.5;

            org.bukkit.util.RayTraceResult result = target.getWorld().rayTraceBlocks(checkLoc,
                    velocity.clone().normalize(), traceDist, org.bukkit.FluidCollisionMode.NEVER, true);

            if (result != null && result.getHitBlock() != null && result.getHitBlockFace() != null) {
                BlockFace face = result.getHitBlockFace();
                Vector normal = new Vector(face.getModX(), face.getModY(), face.getModZ());

                Vector direction = velocity.clone().normalize();
                double dot = direction.dot(normal);
                Vector reflection = direction.subtract(normal.multiply(2 * dot));

                target.setVelocity(reflection.multiply(speed * 1.2));

                DamageHandler.damageEntity(target, wallDamage, this);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.5f);

                Vector dir = velocity.clone().normalize();
                for (double i = 0; i < 360; i += 20) {
                    Vector offset = GeneralMethods.getOrthogonalVector(dir, i, 1.5);
                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.GRAY, 1f);
                    target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(offset), 1, 0.1, 0.1, 0.1,
                            0, dustOptions);
                }

                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));

                ParticleEffect.BLOCK_CRACK.display(target.getLocation(), 10, 0.5, 0.5, 0.5, 0.1,
                        Material.STONE.createBlockData());

                remove();
            }
        }
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return "AerialPush";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public String getAuthor() {
        return "AmonPack";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        remove();
    }
}
