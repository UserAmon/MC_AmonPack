package Abilities.PK_Abilities.Earth;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

import Plugin.AmonPackPlugin;

public class SteelGrab extends MetalAbility implements AddonAbility {

    private double cableLength = -1;

    private long cooldown;
    private double range;
    private double speed;

    private Location cableLoc;
    private Vector cableDir;
    private boolean cableTraveling;

    private boolean cableIsAttachedToBlock;
    private Location cableAttachedLoc;

    private boolean cableIsAttachedToEnemy;
    private LivingEntity targetEnemy;

    private long linkStartTime;
    private boolean wasSneaking;

    public SteelGrab(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.cooldown = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.SteelGrab.Cooldown", 6000);
        this.range = AmonPackPlugin.plugin.getConfig().getDouble("AmonPack.Earth.Metal.SteelGrab.Range", 28.0);
        this.speed = AmonPackPlugin.plugin.getConfig().getDouble("AmonPack.Earth.Metal.SteelGrab.Speed", 2.2);

        this.cableLoc = player.getEyeLocation();
        this.cableDir = player.getEyeLocation().getDirection().normalize();
        this.cableTraveling = true;
        this.cableIsAttachedToBlock = false;
        this.cableIsAttachedToEnemy = false;
        this.wasSneaking = player.isSneaking();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (cableTraveling) {
            Vector step = cableDir.clone().multiply(0.2);
            for (int i = 0; i < (int) (speed / 0.2); i++) {
                cableLoc.add(step);
                
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(cableLoc, 1.2)) {
                    if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                        targetEnemy = (LivingEntity) entity;
                        cableIsAttachedToEnemy = true;
                        cableTraveling = false;
                        linkStartTime = System.currentTimeMillis();
                        player.getWorld().playSound(targetEnemy.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.6f);
                        player.getWorld().playSound(targetEnemy.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.2f);
                        break;
                    }
                }
                
                if (!cableTraveling) {
                    break;
                }

                Block b = cableLoc.getBlock();
                if (b.getType().isSolid() && b.getType() != Material.WATER && b.getType() != Material.LAVA) {
                    cableAttachedLoc = cableLoc.clone();
                    cableIsAttachedToBlock = true;
                    cableTraveling = false;
                    linkStartTime = System.currentTimeMillis();
                    player.getWorld().playSound(cableAttachedLoc, Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.6f);
                    player.getWorld().playSound(cableAttachedLoc, Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.2f);
                    break;
                }
            }

            if (cableTraveling) {
                drawStraightTether(player.getEyeLocation(), cableLoc);
                if (player.getEyeLocation().distanceSquared(cableLoc) > range * range) {
                    remove();
                    bPlayer.addCooldown(this);
                    return;
                }
            }
        }

        if (cableIsAttachedToBlock) {
            drawStraightTether(player.getEyeLocation(), cableAttachedLoc);

            if (System.currentTimeMillis() - linkStartTime > 5000) {
                remove();
                bPlayer.addCooldown(this);
                return;
            }

            Vector vel = player.getVelocity();
            Location eye = player.getEyeLocation();
            Vector toBlock = cableAttachedLoc.toVector().subtract(eye.toVector());
            double dist = toBlock.length();
            Vector toBlockNorm = toBlock.clone().normalize();

            // Set initial cable length on first frame of attachment
            if (cableLength <= 0) {
                cableLength = dist;
            }

            // Tangent velocity: project player velocity onto the tangent plane of the swing sphere
            double dot = vel.dot(toBlockNorm);
            Vector tangentVel = vel.clone().subtract(toBlockNorm.clone().multiply(dot));

            // Constraint / Spring tension force: pulls player toward anchor if they exceed cableLength
            Vector tensionForce = new Vector(0, 0, 0);
            if (dist > cableLength) {
                double excess = dist - cableLength;
                tensionForce = toBlockNorm.clone().multiply(excess * 0.18);
            }

            // Steer/Drift force: allow steering in looking direction projected on swing tangent
            Vector look = eye.getDirection().normalize();
            double lookDot = look.dot(toBlockNorm);
            Vector tangentLook = look.clone().subtract(toBlockNorm.clone().multiply(lookDot));
            Vector steerForce = new Vector(0, 0, 0);
            if (tangentLook.lengthSquared() > 0.001) {
                steerForce = tangentLook.normalize().multiply(0.095);
            }

            // Floatiness/Gravity compensation
            Vector gravity = new Vector(0, -0.045, 0);

            // Drift Feeling: blend current velocity smoothly towards target velocity
            double blendFactor = 0.09; // gradual direction bending
            Vector targetVelocity = tangentVel.clone().add(tensionForce).add(steerForce).add(gravity);
            Vector newVel = vel.clone().multiply(1.0 - blendFactor).add(targetVelocity.multiply(blendFactor));

            // Active reeling-in / pulling on shift (sneaking) like block pulling in SteelSwing!
            if (player.isSneaking()) {
                double pullAccel = 0.095; // responsive active acceleration directly towards block
                newVel.add(toBlockNorm.clone().multiply(pullAccel));
                
                // Reel in the cable length so the swing sphere contracts
                if (cableLength > dist) {
                    cableLength = dist;
                }
                cableLength = Math.max(1.5, cableLength - 0.28);
            }

            // Dampening/friction
            newVel.multiply(0.985);

            // Velocity limit to prevent insane acceleration while remaining highly dynamic
            double maxSwingSpeed = 1.65;
            if (newVel.length() > maxSwingSpeed) {
                newVel.normalize().multiply(maxSwingSpeed);
            }

            // Update player velocity
            player.setVelocity(newVel);

            // Detach and boost when reaching anchor
            if (player.getLocation().distance(cableAttachedLoc) <= 2.2) {
                Vector boost = toBlockNorm.multiply(0.85).setY(0.4);
                player.setVelocity(player.getVelocity().add(boost));
                remove();
                bPlayer.addCooldown(this);
                return;
            }
        }

        if (cableIsAttachedToEnemy) {
            if (targetEnemy == null || targetEnemy.isDead() || !targetEnemy.isValid() || System.currentTimeMillis() - linkStartTime > 5000) {
                remove();
                bPlayer.addCooldown(this);
                return;
            }

            if (!player.hasLineOfSight(targetEnemy)) {
                remove();
                bPlayer.addCooldown(this);
                return;
            }

            Location enemyWaist = targetEnemy.getLocation().add(0, 1, 0);
            for (int angle = 0; angle < 360; angle += 20) {
                double rad = Math.toRadians(angle);
                double x = Math.cos(rad) * 0.8;
                double z = Math.sin(rad) * 0.8;
                Location pLoc = enemyWaist.clone().add(x, 0, z);
                pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(150, 150, 150), 0.6f));
            }

            Location p0 = player.getEyeLocation();
            Location p2 = targetEnemy.getEyeLocation();
            Vector toEnemy = p2.toVector().subtract(p0.toVector()).normalize();
            Vector lookDir = player.getEyeLocation().getDirection().normalize();
            Vector lookOffset = lookDir.clone().subtract(toEnemy.clone().multiply(lookDir.dot(toEnemy)));

            Location midpoint = p0.clone().add(p2.clone().subtract(p0).multiply(0.5));
            Location controlPoint = midpoint.clone().add(lookOffset.clone().multiply(p0.distance(p2) * 0.45));

            drawBezierTether(p0, controlPoint, p2);

            if (player.isSneaking()) {
                Vector enemyVel = targetEnemy.getVelocity();
                Vector toPlayer = player.getLocation().toVector().subtract(targetEnemy.getLocation().toVector()).normalize();
                enemyVel.add(toPlayer.multiply(0.08));
                enemyVel.multiply(0.92);
                if (enemyVel.getY() < 0) {
                    enemyVel.setY(enemyVel.getY() * 0.92 + 0.04);
                }
                targetEnemy.setVelocity(enemyVel);
            }

            if (wasSneaking && !player.isSneaking()) {
                double bendMag = lookOffset.length();
                Vector bendDirection = lookOffset.clone().normalize();
                Vector throwVel = bendDirection.multiply(1.2 + bendMag * 2.2).setY(0.45 + bendMag * 0.8);
                targetEnemy.setVelocity(throwVel);
                targetEnemy.getWorld().playSound(targetEnemy.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.2f, 0.6f);
                targetEnemy.getWorld().playSound(targetEnemy.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 0.8f);
                remove();
                bPlayer.addCooldown(this);
                return;
            }

            wasSneaking = player.isSneaking();
        }
    }

    private void drawStraightTether(Location start, Location end) {
        if (!start.getWorld().equals(end.getWorld())) {
            return;
        }
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < dist; d += 0.4) {
            Location point = start.clone().add(dir.clone().multiply(d));
            double wave = Math.sin(d * 1.5 + (System.currentTimeMillis() / 60.0)) * 0.12;
            point.add(0, wave, 0);
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(150, 150, 150), 0.6f));
        }
    }

    private void drawBezierTether(Location p0, Location p1, Location p2) {
        double dist = p0.distance(p2);
        int segments = (int) (dist * 2.5);
        if (segments < 5) {
            segments = 5;
        }
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double oneMinusT = 1.0 - t;
            double x = oneMinusT * oneMinusT * p0.getX() + 2 * oneMinusT * t * p1.getX() + t * t * p2.getX();
            double y = oneMinusT * oneMinusT * p0.getY() + 2 * oneMinusT * t * p1.getY() + t * t * p2.getY();
            double z = oneMinusT * oneMinusT * p0.getZ() + 2 * oneMinusT * t * p1.getZ() + t * t * p2.getZ();
            Location point = new Location(p0.getWorld(), x, y, z);
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(150, 150, 150), 0.5f));
        }
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "SteelGrab";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
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

    @Override
    public String getDescription() {
        return "Left-click to shoot a steel cable. Hits blocks to pull you along an arc, or hits enemies to link them. Hold shift to pull them, look away to bend the cable, and release shift to sling them.";
    }

    @Override
    public String getInstructions() {
        return "Left-click to grab blocks or enemies. Use shift and look around to pull or sling enemies.";
    }
}
