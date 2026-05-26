package Abilities.PK_Abilities.Water;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

public class BloodArrow extends BloodAbility implements AddonAbility {

    private enum State {
        CHARGING, FIRING
    }

    private State state;
    private long startTime;
    private long chargeTimePerLevel = 1000;
    private int maxChargeLevel = 3;
    private int lastReportedLevel = -1;
    private List<BloodArrowProjectile> arrows = new ArrayList<>();

    public BloodArrow(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }
        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();
        start();
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                fire();
                return;
            }

            int level = getChargeLevel();
            if (level != lastReportedLevel) {
                lastReportedLevel = level;
                if (level > 0) {
                    DamageHandler.damageEntity(player, 1.0, this);
                    float pitch = 0.7f + (level * 0.2f);
                    player.playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 0.8f, pitch);
                    player.playSound(player.getLocation(), Sound.BLOCK_BONE_BLOCK_BREAK, 0.4f, pitch);
                }
            }

            String bar;
            if (level == 0) {
                bar = "§7[ §f░░░ §7] §7§lBLOOD WEAVE...";
            } else if (level == 1) {
                bar = "§c[ §4█§7░░ §c] §c§lLEVEL 1";
            } else if (level == 2) {
                bar = "§c[ §4██§7░ §c] §c§lLEVEL 2";
            } else {
                bar = "§4§l[ §c███ §4§l] §c§lMAX BLOOD ARROW";
            }
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(bar));

            double radius = 0.6 + (level * 0.2);
            double angle = (System.currentTimeMillis() / 160.0) * (level + 1);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location pLoc = player.getLocation().clone().add(x, 1.0 + (level * 0.15), z);
            player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(140, 0, 0), 1.0f));
            player.getWorld().spawnParticle(Particle.CRIT, pLoc, 1, 0, 0, 0, 0);

            if (level > 0) {
                Location eye = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().multiply(0.4))
                        .add(0, -0.4, 0);
                player.getWorld().spawnParticle(Particle.DUST, eye, level * 2, 0.15, 0.05, 0.15, 0,
                        new Particle.DustOptions(Color.fromRGB(180, 0, 0), 0.9f));
                player.getWorld().spawnParticle(Particle.CRIT, eye, level, 0.05, 0.05, 0.05, 0);
            }
        } else if (state == State.FIRING) {
            if (arrows.isEmpty()) {
                remove();
                return;
            }
            List<BloodArrowProjectile> copy = new ArrayList<>(arrows);
            for (BloodArrowProjectile arrow : copy) {
                arrow.progress();
                if (arrow.isDead()) {
                    arrows.remove(arrow);
                }
            }
        }
    }

    private int getChargeLevel() {
        long duration = System.currentTimeMillis() - startTime;
        int level = (int) (duration / chargeTimePerLevel);
        return Math.min(level, maxChargeLevel);
    }

    private void fire() {
        int level = getChargeLevel();
        if (level == 0) {
            remove();
            return;
        }

        bPlayer.addCooldown(this);
        state = State.FIRING;

        double damage = 3.0 + (level * 1.5);
        double range = 20.0 + (level * 10.0);
        int chains = 1 + level;
        double homing = 5.0 + (level * 2.0);

        BloodArrowProjectile arrow = new BloodArrowProjectile(player, this, player.getEyeLocation(),
                player.getLocation().getDirection().clone(), damage, range, chains, homing);
        arrows.add(arrow);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.1f);
    }

    @Override
    public long getCooldown() {
        return 7000;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "BloodArrow";
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
        return true;
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
        return "Collects your blood through charging and fires a homing blood arrow that chains between enemies.";
    }

    @Override
    public String getInstructions() {
        return "Hold shift to charge BloodArrow. Release to fire. Higher charge increases range, damage, and chains.";
    }

    private class BloodArrowProjectile {
        private final Player player;
        private final BloodArrow ability;
        private Location loc;
        private Vector dir;
        private final double damage;
        private final double maxDistance;
        private double distanceTraveled;
        private boolean dead;
        private int chainsRemaining;
        private final double homingRadius;
        private final List<LivingEntity> hitEntities = new ArrayList<>();
        private final double speed = 1.2;

        public BloodArrowProjectile(Player player, BloodArrow ability, Location origin, Vector direction,
                double damage, double maxDistance, int chainsRemaining, double homingRadius) {
            this.player = player;
            this.ability = ability;
            this.loc = origin.clone();
            this.dir = direction.normalize();
            this.damage = damage;
            this.maxDistance = maxDistance;
            this.chainsRemaining = chainsRemaining;
            this.homingRadius = homingRadius;
            this.dead = false;
            this.distanceTraveled = 0;
        }

        public void progress() {
            if (dead) {
                return;
            }

            if (distanceTraveled >= maxDistance) {
                dead = true;
                return;
            }

            LivingEntity nearest = findNearestTarget(loc, homingRadius);
            if (nearest != null) {
                Vector targetDir = GeneralMethods.getDirection(loc, nearest.getLocation()).normalize();
                dir = dir.clone().multiply(0.85).add(targetDir.multiply(0.15)).normalize();
            }

            RayTraceResult result = loc.getWorld().rayTraceBlocks(loc, dir, speed, FluidCollisionMode.NEVER, true);
            if (result != null && result.getHitBlock() != null) {
                Block hit = result.getHitBlock();
                if (isIceBlock(hit.getType())) {
                    hit.setType(Material.AIR);
                    loc = result.getHitPosition().toLocation(loc.getWorld()).add(dir.clone().multiply(0.2));
                    loc.getWorld().playSound(hit.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
                    loc.getWorld().spawnParticle(Particle.SMOKE, hit.getLocation().add(0.5, 0.5, 0.5), 8, 0.1, 0.1, 0.1,
                            0);
                } else {
                    dead = true;
                    return;
                }
            } else {
                loc.add(dir.clone().multiply(speed));
            }

            distanceTraveled += speed;
            loc.getWorld().spawnParticle(Particle.DUST, loc, 2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.fromRGB(160, 0, 0), 1.0f));
            loc.getWorld().spawnParticle(Particle.CRIT, loc, 1, 0.02, 0.02, 0.02, 0);

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.0)) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
                if (entity.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                LivingEntity target = (LivingEntity) entity;
                if (hitEntities.contains(target)) {
                    continue;
                }

                hitEntities.add(target);
                DamageHandler.damageEntity(target, damage, ability);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 1.2f);
                target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.0, 0), 6, 0.1, 0.5,
                        0.1, 0);

                if (chainsRemaining > 0) {
                    LivingEntity next = findNearestTarget(target.getLocation(), homingRadius);
                    if (next != null && !hitEntities.contains(next)) {
                        dir = GeneralMethods.getDirection(target.getLocation(), next.getLocation()).normalize();
                        loc = target.getLocation().clone().add(0, 0.5, 0);
                        chainsRemaining--;
                        return;
                    }
                }
                dead = true;
                return;
            }
        }

        private LivingEntity findNearestTarget(Location center, double radius) {
            LivingEntity nearest = null;
            double best = Double.MAX_VALUE;
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(center, radius)) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
                if (entity.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                LivingEntity living = (LivingEntity) entity;
                if (hitEntities.contains(living)) {
                    continue;
                }
                double dist = center.distanceSquared(living.getLocation());
                if (dist < best) {
                    best = dist;
                    nearest = living;
                }
            }
            return nearest;
        }

        private boolean isIceBlock(Material material) {
            return material == Material.ICE || material == Material.PACKED_ICE || material == Material.BLUE_ICE
                    || material == Material.FROSTED_ICE || material == Material.SNOW_BLOCK
                    || material == Material.SNOW;
        }

        public boolean isDead() {
            return dead;
        }
    }
}
