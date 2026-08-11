package Abilities.PK_Abilities.Earth;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MoltenBlast extends LavaAbility implements AddonAbility {

    private enum State {
        CHARGING, FIRED, IMPACTED
    }

    private State state;
    private long chargeStartTime;
    private long maxChargeTime;
    private int chargeLevel = 1;
    private double damage;
    private long cooldown;
    private int shardsCount;
    private int lavaPoolRadius;

    private Location projectileLoc;
    private Vector projectileVel;
    private final Set<UUID> hitEntities = new HashSet<>();
    private final List<TempBlock> tempLavaBlocks = new ArrayList<>();

    public MoltenBlast(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Lava.MoltenBlast.Damage", 8.0);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Lava.MoltenBlast.Cooldown", 6000);
        this.maxChargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Lava.MoltenBlast.ChargeTime", 1500);
        this.shardsCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Lava.MoltenBlast.ShardsCount", 6);
        this.lavaPoolRadius = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Lava.MoltenBlast.LavaPoolRadius", 2);

        this.state = State.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();

        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                fire();
                return;
            }

            long elapsed = System.currentTimeMillis() - chargeStartTime;
            if (elapsed >= maxChargeTime) {
                chargeLevel = 3;
            } else if (elapsed >= maxChargeTime / 2) {
                chargeLevel = 2;
            } else {
                chargeLevel = 1;
            }

            Location center = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.5));
            player.getWorld().spawnParticle(org.bukkit.Particle.LAVA, center, chargeLevel * 3, 0.2, 0.2, 0.2, 0.05);
            player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, center, chargeLevel * 4, 0.1, 0.1, 0.1, 0.03);

            for (Block b : GeneralMethods.getBlocksAroundPoint(center, 4.0)) {
                if (isEarthbendable(b) && new Random().nextInt(10) < chargeLevel * 2) {
                    Vector dir = center.toVector().subtract(b.getLocation().toVector()).normalize().multiply(0.3);
                    player.getWorld().spawnParticle(org.bukkit.Particle.FALLING_DUST, b.getLocation().add(0.5, 0.5, 0.5), 3,
                            dir.getX(), dir.getY(), dir.getZ(), 0.1, Material.MAGMA_BLOCK.createBlockData());
                }
            }

            if (elapsed % 300 < 50) {
                player.getWorld().playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 0.4f, 0.8f + (chargeLevel * 0.3f));
            }
        } else if (state == State.FIRED) {
            projectileVel.add(new Vector(0, -0.03, 0));
            projectileLoc.add(projectileVel);

            player.getWorld().spawnParticle(org.bukkit.Particle.LAVA, projectileLoc, 5, 0.2, 0.2, 0.2, 0.02);
            player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, projectileLoc, 8, 0.3, 0.3, 0.3, 0.05);

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projectileLoc, 1.8)) {
                if (entity instanceof LivingEntity le && entity.getEntityId() != player.getEntityId() && !hitEntities.contains(entity.getUniqueId())) {
                    hitEntities.add(entity.getUniqueId());
                    DamageHandler.damageEntity(le, damage * (0.8 + (chargeLevel * 0.2)), this);
                    le.setFireTicks(60);
                }
            }

            Block block = projectileLoc.getBlock();
            if (block.getType().isSolid()) {
                impact(projectileLoc);
            } else if (projectileLoc.distanceSquared(player.getLocation()) > 400) {
                remove();
            }
        }
    }

    private void fire() {
        state = State.FIRED;
        projectileLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.2));
        projectileVel = player.getLocation().getDirection().normalize().multiply(1.2 + (chargeLevel * 0.3));

        player.getWorld().playSound(projectileLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.2f);

        for (int i = 0; i < shardsCount; i++) {
            Vector shardDir = projectileVel.clone().add(new Vector(
                    (Math.random() - 0.5) * 0.6,
                    (Math.random() - 0.5) * 0.4,
                    (Math.random() - 0.5) * 0.6
            ));
            spawnShard(projectileLoc.clone(), shardDir);
        }

        bPlayer.addCooldown(this, cooldown);
    }

    private void spawnShard(Location startLoc, Vector vel) {
        new BukkitRunnable() {
            private Location loc = startLoc.clone();
            private int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > 20) {
                    cancel();
                    return;
                }
                loc.add(vel);
                loc.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.02);
                for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 1.2)) {
                    if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                        DamageHandler.damageEntity(le, damage * 0.4, MoltenBlast.this);
                        le.setFireTicks(40);
                        cancel();
                        return;
                    }
                }
                if (loc.getBlock().getType().isSolid()) {
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void impact(Location impactLoc) {
        state = State.IMPACTED;
        impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.7f);

        new BukkitRunnable() {
            private int step = 0;

            @Override
            public void run() {
                if (step > lavaPoolRadius) {
                    cancel();
                    remove();
                    return;
                }

                for (Block b : GeneralMethods.getBlocksAroundPoint(impactLoc, step + 0.5)) {
                    if (isEarthbendable(b) && b.getType() != Material.BEDROCK) {
                        if (!TempBlock.isTempBlock(b)) {
                            TempBlock tb = new TempBlock(b, Material.LAVA.createBlockData(), 5000);
                            tempLavaBlocks.add(tb);
                        }
                    }
                }
                impactLoc.getWorld().spawnParticle(org.bukkit.Particle.LAVA, impactLoc, step * 10 + 5, step * 0.5, 0.2, step * 0.5, 0.05);
                step++;
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return projectileLoc != null ? projectileLoc : (player != null ? player.getLocation() : null);
    }

    @Override
    public String getName() {
        return "MoltenBlast";
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public String getAuthor() {
        return "AmonPack";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }
}
