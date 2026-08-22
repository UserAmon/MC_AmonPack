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
import Plugin.AmonPackPlugin;

public class BloodArrow extends BloodAbility implements AddonAbility {

    private enum State {
        CHARGING, FIRING
    }

    private State state;
    private long startTime;
    private long chargeTimePerLevel;
    private int maxChargeLevel;
    private double selfDamage;
    public double speed;
    private long cooldown;
    private int lastReportedLevel = -1;
    private List<BloodArrowProjectile> arrows = new ArrayList<>();
    private boolean canTrack;
    private double trackRange;
    private double chainRange;
    private double baseDamage;
    private double damageMultiplier;
    private double baseRange;
    private double rangeMultiplier;
    private boolean canKillUser;
    private boolean canKillEnemy;

    public BloodArrow(Player player) {
        super(player);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.BloodArrow.Cooldown", 7000L);
        this.chargeTimePerLevel = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.BloodArrow.ChargeTimePerLevel", 1000L);
        this.maxChargeLevel = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.BloodArrow.MaxChargeLevel", 3);
        this.selfDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.BloodArrow.SelfDamage", 1.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.BloodArrow.Speed", 1.2);
        this.canTrack = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Water.BloodArrow.CanTrack", true);
        this.trackRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.BloodArrow.TrackRange", 5.0);
        this.chainRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.BloodArrow.ChainRange", 7.0);
        this.baseDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.BloodArrow.BaseDamage", 3.0);
        this.damageMultiplier = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.BloodArrow.DamageMultiplier", 1.5);
        this.baseRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.BloodArrow.BaseRange", 20.0);
        this.rangeMultiplier = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.BloodArrow.RangeMultiplier", 10.0);
        this.canKillUser = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Water.BloodArrow.CanKillUser", false);
        this.canKillEnemy = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Water.BloodArrow.CanKillEnemy", true);

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
                    double minHp = canKillUser ? 0.0 : 1.0;
                    double newHealth = Math.max(minHp, player.getHealth() - this.selfDamage);
                    if (newHealth <= 0.0) {
                        DamageHandler.damageEntity(player, selfDamage, this);
                    } else {
                        player.setHealth(newHealth);
                        float pitch = 0.7f + (level * 0.2f);
                        player.playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 0.8f, pitch);
                        player.playSound(player.getLocation(), Sound.BLOCK_BONE_BLOCK_BREAK, 0.4f, pitch);
                    }
                }
            }

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

        boolean isVeinFlow = VeinFlowManager.isActive(player);
        VeinFlow stance = VeinFlowManager.getStance(player);
        long actualCd = isVeinFlow ? (long) (cooldown * 0.6) : cooldown;

        bPlayer.addCooldown(this, actualCd);
        state = State.FIRING;

        double damage = baseDamage + (level * damageMultiplier);
        double range = baseRange + (level * rangeMultiplier);
        if (isVeinFlow && stance != null) {
            damage *= stance.getDamageMultiplier();
        }
        int chains = 1 + level;
        double trackDist = trackRange + (level * 2.0);
        double chainDist = chainRange + (level * 2.0);

        BloodArrowProjectile arrow = new BloodArrowProjectile(player, this, player.getEyeLocation(),
                player.getLocation().getDirection().clone(), damage, range, chains, canTrack, trackDist, chainDist);
        arrows.add(arrow);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.1f);
    }

    @Override
    public long getCooldown() {
        return AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.BloodArrow.Cooldown", 7000L);
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
        return "2.0";
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
        return "Gromadzi krew poprzez ładowanie i wystrzeliwuje naprowadzającą strzałę krwi łączącą cele.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj shift aby ładować BloodArrow. Puść aby wystrzelić!";
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
        private final boolean canTrack;
        private final double trackRange;
        private final double chainRange;
        private final List<LivingEntity> hitEntities = new ArrayList<>();
        private double speed;

        public BloodArrowProjectile(Player player, BloodArrow ability, Location origin, Vector direction,
                double damage, double maxDistance, int chainsRemaining, boolean canTrack, double trackRange, double chainRange) {
            this.player = player;
            this.ability = ability;
            this.loc = origin.clone();
            this.dir = direction.normalize();
            this.damage = damage;
            this.maxDistance = maxDistance;
            this.chainsRemaining = chainsRemaining;
            this.canTrack = canTrack;
            this.trackRange = trackRange;
            this.chainRange = chainRange;
            this.dead = false;
            this.distanceTraveled = 0;
            this.speed = ability.speed;
        }

        public void progress() {
            if (dead) {
                return;
            }

            if (distanceTraveled >= maxDistance) {
                dead = true;
                return;
            }

            if (canTrack) {
                LivingEntity nearest = findNearestTarget(loc, trackRange);
                if (nearest != null) {
                    Vector targetDir = GeneralMethods.getDirection(loc, nearest.getLocation()).normalize();
                    dir = dir.clone().multiply(0.85).add(targetDir.multiply(0.15)).normalize();
                }
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

                if (canKillEnemy) {
                    DamageHandler.damageEntity(target, damage, ability);
                } else {
                    double targetNewHealth = Math.max(1.0, target.getHealth() - damage);
                    if (targetNewHealth > 0.0) {
                        target.setHealth(targetNewHealth);
                    }
                }

                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 1.2f);
                target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.0, 0), 6, 0.1, 0.5,
                        0.1, 0);

                if (chainsRemaining > 0) {
                    LivingEntity next = findNearestTarget(target.getLocation(), chainRange);
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
