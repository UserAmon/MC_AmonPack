package abilities.Util_Objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

public class LightningBolt {

    private Player player;
    private CoreAbility ability;
    private Location loc;
    private Vector dir;
    private double damage;
    private double maxDistance;
    private double distanceTraveled;
    private int bounces;
    private boolean dead;
    private double speed = 1.3;
    private boolean canBranch;
    private Random random = new Random();

    public LightningBolt(Player player, CoreAbility ability, Location loc, Vector dir, double damage,
            double maxDistance, int bounces, boolean canBranch) {
        this.player = player;
        this.ability = ability;
        this.loc = loc.clone();
        this.dir = dir.normalize();
        this.damage = damage;
        this.maxDistance = maxDistance;
        this.bounces = bounces;
        this.canBranch = canBranch;
        this.dead = false;
    }

    public List<LightningBolt> progress() {
        List<LightningBolt> newBolts = new ArrayList<>();
        if (dead)
            return newBolts;

        if (distanceTraveled >= maxDistance) {
            dead = true;
            return newBolts;
        }

        if (random.nextDouble() < 0.3) {
            dir.add(new Vector((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2)).normalize();
        }

        if (GeneralMethods.isRegionProtectedFromBuild(player, "LightningWeave", loc)) {
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

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.25)) {
            if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                DamageHandler.damageEntity(entity, damage, ability);
            }
        }

        if (canBranch && random.nextDouble() < 0.05 && distanceTraveled < maxDistance * 0.8) {
            Vector branchDir = dir.clone().add(new Vector((random.nextDouble() - 0.5), (random.nextDouble() - 0.5),
                    (random.nextDouble() - 0.5))).normalize();
            newBolts.add(new LightningBolt(player, ability, loc, branchDir, damage * 0.5,
                    maxDistance - distanceTraveled, 0, false));
        }

        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(100, 200, 255), 1);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 2, 0, 0.3, 0.3, 0.3, dust);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 4, 0, 0.6, 0.6, 0.6, dust);

        return newBolts;
    }

    private boolean isWater(Block block) {
        return block.getType() == org.bukkit.Material.WATER || block.getType() == org.bukkit.Material.BUBBLE_COLUMN;
    }

    public boolean isDead() {
        return dead;
    }

    public Location getLocation() {
        return loc;
    }
}
