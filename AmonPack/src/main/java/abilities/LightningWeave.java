package abilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

public class LightningWeave extends LightningAbility implements AddonAbility {

    private enum State {
        CHARGING, FIRING
    }

    private State state;
    private long startTime;
    private long chargeTimePerLevel = 1000;
    private int maxChargeLevel = 3;
    private long cooldown = 4000;
    private List<LightningBolt> bolts = new ArrayList<>();
    private Random random = new Random();

    public LightningWeave(Player player) {
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
                Location eye = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5)).clone().add(0,-0.5,0);
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(100, 200, 255),
                        0.5f + (level * 0.2f));
                eye.getWorld().spawnParticle(Particle.DUST, eye, level * 2, 0.25, 0.1, 0.25, 0, dust);

                if (System.currentTimeMillis() % 1000 < 50) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f + (level * 0.5f));
                }
            }
        } else if (state == State.FIRING) {
            if (bolts.isEmpty()) {
                remove();
                return;
            }

            List<LightningBolt> newBolts = new ArrayList<>();
            Iterator<LightningBolt> it = bolts.iterator();
            while (it.hasNext()) {
                LightningBolt bolt = it.next();
                bolt.progress(newBolts);
                if (bolt.isDead()) {
                    it.remove();
                }
            }
            bolts.addAll(newBolts);
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

        int boltCount = 1 + (level * 2);
        double damage = 2 + level;
        double range = 20 + (level * 12);

        for (int i = 0; i < boltCount; i++) {
            Vector dir = player.getLocation().getDirection().clone();
            dir.add(new Vector((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2));
            bolts.add(new LightningBolt(player.getEyeLocation(), dir.normalize(), damage, range, 3));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 2f);
    }

    private class LightningBolt {
        private Location loc;
        private Vector dir;
        private double damage;
        private double maxDistance;
        private double distanceTraveled;
        private int bounces;
        private boolean dead;
        private double speed = 1;

        public LightningBolt(Location loc, Vector dir, double damage, double maxDistance, int bounces) {
            this.loc = loc.clone();
            this.dir = dir;
            this.damage = damage;
            this.maxDistance = maxDistance;
            this.bounces = bounces;
            this.dead = false;
        }

        public void progress(List<LightningBolt> newBolts) {
            if (dead)
                return;

            if (distanceTraveled >= maxDistance) {
                dead = true;
                return;
            }

            if (random.nextDouble() < 0.3) {
                dir.add(new Vector((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2,
                        (random.nextDouble() - 0.5) * 0.2)).normalize();
            }

            if (isWater(loc.getBlock())) {
                dir.add(new Vector((random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5,
                        (random.nextDouble() - 0.5) * 0.5)).normalize();
            }

            Vector velocity = dir.clone().multiply(speed);

            org.bukkit.util.RayTraceResult result = loc.getWorld().rayTraceBlocks(loc, velocity, speed,
                    org.bukkit.FluidCollisionMode.NEVER, true);

            if (result != null && result.getHitBlock() != null) {
                Block hitBlock = result.getHitBlock();
                if (GeneralMethods.isSolid(hitBlock)) {
                    if (bounces > 0) {
                        BlockFace face = result.getHitBlockFace();
                        if (face != null) {
                            Vector normal = new Vector(face.getModX(), face.getModY(), face.getModZ());
                            double dot = dir.dot(normal);
                            dir.subtract(normal.multiply(2 * dot));
                            bounces--;
                            loc.add(result.getHitPosition().subtract(loc.toVector()).multiply(0.9));
                            loc.getWorld().playSound(loc, Sound.BLOCK_CHAIN_HIT, 0.5f, 2f);
                        } else {
                            dead = true;
                        }
                    } else {
                        dead = true;
                    }
                }
            } else {
                loc.add(velocity);
            }

            distanceTraveled += speed;

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.5)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    DamageHandler.damageEntity(entity, damage, LightningWeave.this);
                    dead = true;
                    return;
                }
            }

            if (random.nextDouble() < 0.05 && distanceTraveled < maxDistance * 0.8) {
                Vector branchDir = dir.clone().add(new Vector((random.nextDouble() - 0.5), (random.nextDouble() - 0.5),
                        (random.nextDouble() - 0.5))).normalize();
                newBolts.add(new LightningBolt(loc, branchDir, damage * 0.5, maxDistance - distanceTraveled, 0));
            }

            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(100, 200, 255), 1);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 2, 0, 0.3, 0.3, 0.3, dust);
        }

        public boolean isDead() {
            return dead;
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
        return "LightningWeave";
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
}
