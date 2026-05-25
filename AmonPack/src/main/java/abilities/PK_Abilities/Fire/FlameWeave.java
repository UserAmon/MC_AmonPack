package Abilities.PK_Abilities.Fire;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;

public class FlameWeave extends FireAbility implements AddonAbility {

    private enum State {
        CHARGING, FIRING
    }

    private State state;
    private long startTime;
    private long chargeTimePerLevel = 1000;
    private int maxChargeLevel = 3;
    private long cooldown = 4000;
    private List<FlameBolt> bolts = new ArrayList<>();
    private Random random = new Random();

    public FlameWeave(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        this.startTime = System.currentTimeMillis();
        this.state = State.CHARGING;
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                fire();
                return;
            }

            int level = getChargeLevel();
            if (level > 0) {
                Location eye = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5)).clone()
                        .add(0, -0.5, 0);
                Particle.DustOptions dust = new Particle.DustOptions(Color.ORANGE, 0.5f + (level * 0.2f));
                eye.getWorld().spawnParticle(Particle.DUST, eye, level * 2, 0.25, 0.1, 0.25, 0, dust);
                eye.getWorld().spawnParticle(Particle.FLAME, eye, level, 0.1, 0.1, 0.1, 0.02);

                if (System.currentTimeMillis() % 1000 < 50) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.5f + (level * 0.2f));
                }
            }
        } else if (state == State.FIRING) {
            if (bolts.isEmpty()) {
                remove();
                return;
            }

            List<FlameBolt> boltsCopy = new ArrayList<>(bolts);
            for (FlameBolt bolt : boltsCopy) {
                bolt.progress();
                if (bolt.isDead()) {
                    bolts.remove(bolt);
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

        int boltCount = (int) (2 + (level * 2.5));
        double damage = 1 + level;
        double range = (level * 15) + 10;

        for (int i = 0; i < boltCount; i++) {
            Vector dir = player.getLocation().getDirection().clone();
            dir.add(new Vector((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2));
            bolts.add(new FlameBolt(player, this, player.getEyeLocation(), dir.normalize(), damage, range, 6));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1f, 1f);
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
        return "FlameWeave";
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

    private class FlameBolt {
        private Player player;
        private FireAbility ability;
        private Location loc;
        private Vector dir;
        private double damage;
        private double maxDistance;
        private double distanceTraveled;
        private boolean dead;
        private boolean hasSplit;
        private double speed = 0.8;
        private Random random = new Random();

        public FlameBolt(Player player, FireAbility ability, Location loc, Vector dir, double damage,
                double maxDistance, int crawls) {
            this.player = player;
            this.ability = ability;
            this.loc = loc.clone();
            this.dir = dir.normalize();
            this.damage = damage;
            this.maxDistance = maxDistance;
            this.dead = false;
            this.hasSplit = false;
        }

        // Constructor for split bolts
        public FlameBolt(Player player, FireAbility ability, Location loc, Vector dir, double damage,
                double maxDistance, boolean hasSplit) {
            this.player = player;
            this.ability = ability;
            this.loc = loc.clone();
            this.dir = dir.normalize();
            this.damage = damage;
            this.maxDistance = maxDistance;
            this.dead = false;
            this.hasSplit = hasSplit;
        }

        public void progress() {
            if (dead)
                return;

            if (distanceTraveled >= maxDistance) {
                dead = true;
                return;
            }

            if (isWater(loc.getBlock())) {
                dead = true;
                loc.getWorld().spawnParticle(Particle.SMOKE, loc, 5, 0.1, 0.1, 0.1, 0);
                loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
                return;
            }

            if (random.nextDouble() < 0.4) {
                dir.add(new Vector((random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1,
                        (random.nextDouble() - 0.5) * 0.1)).normalize();
            }

            Vector velocity = dir.clone().multiply(speed);
            RayTraceResult result = null;
            try {
                result = loc.getWorld().rayTraceBlocks(loc, dir, speed,
                        FluidCollisionMode.NEVER, true);
            } catch (Exception ignored) {
            }

            if (result != null && result.getHitBlock() != null) {
                Block hitBlock = result.getHitBlock();
                if (GeneralMethods.isSolid(hitBlock)) {
                    BlockFace face = result.getHitBlockFace();
                    if (face != null) {
                        Vector normal = new Vector(face.getModX(), face.getModY(), face.getModZ());

                        // Bounce logic: Reflection + Original Direction (to keep some forward
                        // momentum/go around)
                        // Reflection = Dir - 2 * (Dir . Normal) * Normal
                        Vector reflection = dir.clone().subtract(normal.clone().multiply(2 * dir.dot(normal)));
                        // Mix reflection with original direction (weighted)
                        Vector bounceDir = reflection.clone().add(dir.clone().multiply(0.5)).normalize();

                        // If bounce is too perpendicular/invalid, add randomness
                        if (bounceDir.lengthSquared() < 0.01) {
                            Vector randomVec = new Vector(random.nextDouble(), random.nextDouble(),
                                    random.nextDouble());
                            bounceDir = randomVec.subtract(normal.multiply(randomVec.dot(normal))).normalize();
                        }

                        dir = bounceDir;
                        loc = result.getHitPosition().toLocation(loc.getWorld()).add(normal.multiply(0.3));

                        if (isTransparent(loc.getBlock())) {
                            new TempBlock(loc.getBlock(), Material.FIRE).setRevertTime(2000);
                        }
                        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 2f);

                        // Split logic
                        if (!hasSplit) {
                            hasSplit = true;
                            // Spawn 3 new bolts
                            // 1. Main bounce direction (already set for this bolt, but we want new ones)
                            // Actually, this bolt continues as one. We spawn 2 more? Or 3 new ones and kill
                            // this?
                            // User said: "wypuść dodatkowe 3 flamebolty" (release additional 3 flamebolts).
                            // So this one continues, and we add 3 more.

                            Vector left = dir.clone().crossProduct(new Vector(0, 1, 0)).multiply(0.5);
                            Vector right = dir.clone().crossProduct(new Vector(0, -1, 0)).multiply(0.5);

                            bolts.add(new FlameBolt(player, ability, loc, dir.clone().add(left).normalize(),
                                    damage * 0.6, maxDistance / 2, true));
                            bolts.add(new FlameBolt(player, ability, loc, dir.clone().add(right).normalize(),
                                    damage * 0.6, maxDistance / 2, true));
                            bolts.add(new FlameBolt(player, ability, loc,
                                    dir.clone().multiply(0.8).add(new Vector(0, 0.5, 0)).normalize(), damage * 0.6,
                                    maxDistance / 2, true));
                        }
                    } else {
                        dead = true;
                    }
                }
            } else {
                loc.add(velocity);
            }

            distanceTraveled += speed;
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    DamageHandler.damageEntity(entity, damage, ability);
                    entity.setFireTicks(40);
                }
            }

            loc.getWorld().spawnParticle(Particle.FLAME, loc, 2, 0.1, 0.1, 0.1, 0.02);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 1, 0.1, 0.1, 0.1, 0);
        }

        public boolean isDead() {
            return dead;
        }
    }
}
