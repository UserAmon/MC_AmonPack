package Abilities.PK_Abilities.Air;

import Abilities.Bending.SoundAbility;
import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BassDrop extends SoundAbility implements AddonAbility {
    private enum Phase {
        DELAY,
        FALLING,
        SHOCKWAVE
    }

    private static final Map<UUID, BukkitRunnable> ACTIVE_CAMERA_SHAKES = new HashMap<>();

    private final Location impactLocation;
    private final Location projectileLocation;
    private final double height;
    private final double projectileSpeed;
    private final double projectileMinRadius;
    private final double projectileMaxRadius;
    private final double shockwaveRadius;
    private final double shockwaveSpeed;
    private final double damage;
    private final double stacks;
    private final double knockback;
    private final double verticalKnockback;
    private final int delayTicks;
    private final int cameraShakeDurationTicks;
    private final int cameraShakeIntervalTicks;
    private final float cameraShakeYaw;
    private final float cameraShakePitch;
    private final long cooldown;

    private final Set<UUID> hitEntities = new HashSet<>();
    private Phase phase;
    private int ticksElapsed;
    private double currentShockwaveRadius;

    public BassDrop(Player player) {
        this(player, player.getTargetBlockExact(20));
    }

    public BassDrop(Player player, Block targetBlock) {
        super(player);

        this.cooldown = getAbilityConfig().getLong("AmonPack.Air.BassDrop.Cooldown", 8000L);
        this.height = Math.max(1.0, getAbilityConfig().getDouble("AmonPack.Air.BassDrop.Height", 20.0));
        this.projectileSpeed = Math.max(0.05,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.ProjectileSpeed", 1.0));
        this.projectileMinRadius = Math.max(0.05,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.ProjectileMinRingRadius", 0.35));
        this.projectileMaxRadius = Math.max(projectileMinRadius,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.ProjectileMaxRingRadius", 3.8));
        this.shockwaveRadius = Math.max(0.5,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.ShockwaveRadius", 8.0));
        this.shockwaveSpeed = Math.max(0.05,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.ShockwaveSpeed", 0.65));
        this.damage = Math.max(0.0,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.Damage", 1.5));
        this.stacks = Math.max(0.0,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.Stacks", 6.0));
        this.knockback = Math.max(0.0,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.Knockback", 0.65));
        this.verticalKnockback = Math.max(0.0,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.VerticalKnockback", 0.2));
        this.delayTicks = Math.max(0, (int) Math.round(
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.DelaySeconds", 3.0) * 20.0));
        this.cameraShakeDurationTicks = Math.max(1,
                getAbilityConfig().getInt("AmonPack.Air.BassDrop.CameraShakeDurationTicks", 60));
        this.cameraShakeIntervalTicks = Math.max(1,
                getAbilityConfig().getInt("AmonPack.Air.BassDrop.CameraShakeIntervalTicks", 3));
        this.cameraShakeYaw = (float) Math.max(0.0,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.CameraShakeYaw", 8.0));
        this.cameraShakePitch = (float) Math.max(0.0,
                getAbilityConfig().getDouble("AmonPack.Air.BassDrop.CameraShakePitch", 1.5));

        if (targetBlock == null || targetBlock.getType().isAir()) {
            this.impactLocation = null;
            this.projectileLocation = null;
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            this.impactLocation = null;
            this.projectileLocation = null;
            return;
        }

        this.impactLocation = targetBlock.getLocation().clone().add(0.5, 1.0, 0.5);
        this.projectileLocation = impactLocation.clone().add(0.0, height, 0.0);
        this.phase = delayTicks > 0 ? Phase.DELAY : Phase.FALLING;
        bPlayer.addCooldown(this);
        start();
    }

    private static org.bukkit.configuration.file.FileConfiguration getAbilityConfig() {
        return AmonPackPlugin.getAbilitiesConfig();
    }

    @Override
    public void progress() {
        if (impactLocation == null || projectileLocation == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (phase == Phase.DELAY) {
            progressDelay();
        } else if (phase == Phase.FALLING) {
            progressFall();
        } else {
            progressShockwave();
        }
    }

    private void progressDelay() {
        ticksElapsed++;
        if (ticksElapsed % 4 == 0) {
            drawTargetMarker();
        }
        if (ticksElapsed >= delayTicks) {
            phase = Phase.FALLING;
            ticksElapsed = 0;
            player.getWorld().playSound(projectileLocation, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.55f);
        }
    }

    private void progressFall() {
        projectileLocation.subtract(0.0, projectileSpeed, 0.0);
        double remainingHeight = projectileLocation.getY() - impactLocation.getY();
        if (remainingHeight <= 0.0) {
            projectileLocation.setY(impactLocation.getY());
            detonate();
            return;
        }

        double progress = 1.0 - (remainingHeight / height);
        double radius = projectileMinRadius
                + (projectileMaxRadius - projectileMinRadius) * clamp(progress, 0.0, 1.0);
        drawSonicRing(projectileLocation, radius, 0.0);
        projectileLocation.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, projectileLocation, 3,
                0.12, 0.12, 0.12, 0.02);

        ticksElapsed++;
        if (ticksElapsed % 4 == 0) {
            float pitch = (float) (0.45 + (progress * 0.25));
            projectileLocation.getWorld().playSound(projectileLocation, Sound.BLOCK_NOTE_BLOCK_BASS, 0.45f, pitch);
        }
    }

    private void detonate() {
        phase = Phase.SHOCKWAVE;
        ticksElapsed = 0;
        currentShockwaveRadius = 0.0;
        impactLocation.getWorld().playSound(impactLocation, Sound.BLOCK_NOTE_BLOCK_BASS, 2.0f, 0.4f);
        impactLocation.getWorld().playSound(impactLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.8f);
        impactLocation.getWorld().spawnParticle(Particle.SONIC_BOOM, impactLocation, 1, 0, 0, 0, 0);
        impactLocation.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, impactLocation, 20,
                0.7, 0.25, 0.7, 0.1);
    }

    private void progressShockwave() {
        currentShockwaveRadius += shockwaveSpeed;
        if (currentShockwaveRadius > shockwaveRadius) {
            remove();
            return;
        }

        drawSonicRing(impactLocation, currentShockwaveRadius, 0.1);
        hitEntitiesInShockwave();

        ticksElapsed++;
        if (ticksElapsed % 4 == 0) {
            impactLocation.getWorld().playSound(impactLocation, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.55f, 0.65f);
        }
    }

    private void hitEntitiesInShockwave() {
        double searchRadius = currentShockwaveRadius + 1.2;
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(impactLocation, searchRadius)) {
            if (!(entity instanceof LivingEntity) || entity.getUniqueId().equals(player.getUniqueId())
                    || hitEntities.contains(entity.getUniqueId()) || entity.isDead()) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            Location targetLocation = target.getLocation();
            double verticalDistance = Math.abs(targetLocation.getY() - impactLocation.getY());
            if (verticalDistance > 2.5) {
                continue;
            }

            double horizontalDistance = horizontalDistance(targetLocation, impactLocation);
            double ringThickness = 0.85;
            if (horizontalDistance < Math.max(0.0, currentShockwaveRadius - ringThickness)
                    || horizontalDistance > currentShockwaveRadius + ringThickness) {
                continue;
            }

            hitEntities.add(target.getUniqueId());
            HandleDamage(player, target, stacks);
            if (damage > 0.0) {
                DamageHandler.damageEntity(target, damage, this);
            }
            pushAway(target);
            if (target instanceof Player) {
                startCameraShake((Player) target);
            }
        }
    }

    private void pushAway(LivingEntity target) {
        Vector push = target.getLocation().toVector().subtract(impactLocation.toVector());
        push.setY(0.0);
        if (push.lengthSquared() < 0.001) {
            double angle = Math.random() * Math.PI * 2.0;
            push = new Vector(Math.cos(angle), 0.0, Math.sin(angle));
        } else {
            push.normalize();
        }
        target.setVelocity(push.multiply(knockback).setY(verticalKnockback));
    }

    private void drawTargetMarker() {
        Location marker = impactLocation.clone().add(0.0, 0.06, 0.0);
        drawSonicRing(marker, 0.7, 0.0);
        marker.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, marker, 2, 0.15, 0.02, 0.15, 0.01);
    }

    private void drawSonicRing(Location center, double radius, double yOffset) {
        if (radius <= 0.0 || center.getWorld() == null) {
            return;
        }

        int points = Math.max(12, (int) Math.ceil((Math.PI * 2.0 * radius) / 0.5));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            Location point = center.clone().add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
            center.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
        }
    }

    private void startCameraShake(Player target) {
        UUID uuid = target.getUniqueId();
        BukkitRunnable previous = ACTIVE_CAMERA_SHAKES.remove(uuid);
        if (previous != null) {
            previous.cancel();
        }

        Location original = target.getLocation().clone();
        BukkitRunnable shake = new BukkitRunnable() {
            private int elapsed;

            @Override
            public void run() {
                if (!target.isOnline() || target.isDead()) {
                    finish();
                    return;
                }
                if (elapsed >= cameraShakeDurationTicks) {
                    finish();
                    return;
                }

                float yawOffset = (float) ((Math.random() * 2.0 - 1.0) * cameraShakeYaw);
                float pitchOffset = (float) ((Math.random() * 2.0 - 1.0) * cameraShakePitch);
                float pitch = clampPitch(original.getPitch() + pitchOffset);
                target.setRotation(original.getYaw() + yawOffset, pitch);
                elapsed += cameraShakeIntervalTicks;
            }

            private void finish() {
                if (ACTIVE_CAMERA_SHAKES.get(uuid) == this && target.isOnline() && !target.isDead()) {
                    target.setRotation(original.getYaw(), original.getPitch());
                    ACTIVE_CAMERA_SHAKES.remove(uuid);
                }
                cancel();
            }
        };

        ACTIVE_CAMERA_SHAKES.put(uuid, shake);
        shake.runTaskTimer(AmonPackPlugin.plugin, 0L, cameraShakeIntervalTicks);
    }

    private static double horizontalDistance(Location first, Location second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return Math.sqrt((dx * dx) + (dz * dz));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampPitch(float pitch) {
        return (float) clamp(pitch, -90.0, 90.0);
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return projectileLocation != null ? projectileLocation : impactLocation;
    }

    @Override
    public String getName() {
        return "BassDrop";
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
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Calls a bass projectile above a clicked block. The impact sends a sonic shockwave that builds sound stacks and shakes hit players' cameras.";
    }

    @Override
    public String getInstructions() {
        return "Left-click a block to mark it for a delayed bass drop.";
    }
}
