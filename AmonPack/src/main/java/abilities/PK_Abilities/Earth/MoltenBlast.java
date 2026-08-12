package Abilities.PK_Abilities.Earth;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MoltenBlast extends LavaAbility implements AddonAbility {

    private enum State { CHARGING, FIRED, IMPACTED }

    private State state;
    private double damage;
    private long cooldown;
    private int requiredMeltBlocks;
    private double range;
    private int shardsCount;
    private int lavaPoolRadius;

    private int meltedBlocksCount = 0;
    private final Set<Location> meltedBlockLocations = new HashSet<>();
    private final List<TempBlock> activeTempBlocks = new ArrayList<>();
    private Location sphereLoc;
    private boolean fullyChargedNotified = false;

    private Location projectileLoc;
    private Vector projectileVel;
    private final Set<UUID> hitEntities = new HashSet<>();

    public MoltenBlast(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        // Skill initialization MUST start by looking at an earthbendable block within 3 blocks
        Block initialTarget = player.getTargetBlockExact(3);
        if (initialTarget == null || !isEarthbendable(initialTarget)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Lava.MoltenBlast.Damage", 8.0);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Lava.MoltenBlast.Cooldown", 6000);
        this.requiredMeltBlocks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Lava.MoltenBlast.RequiredMeltBlocks", 3);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Lava.MoltenBlast.Range", 35.0);
        this.shardsCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Lava.MoltenBlast.ShardsCount", 6);
        this.lavaPoolRadius = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Lava.MoltenBlast.LavaPoolRadius", 2);

        this.state = State.CHARGING;

        // Melt the initial targeted block
        meltBlock(initialTarget);

        start();
    }

    private void meltBlock(Block target) {
        meltedBlockLocations.add(target.getLocation());
        meltedBlocksCount++;

        // Step 1: Change to MAGMA_BLOCK for 1 second (20 ticks)
        TempBlock magmaTemp = new TempBlock(target, Material.MAGMA_BLOCK.createBlockData(), 1000);
        activeTempBlocks.add(magmaTemp);

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.2f);
        target.getWorld().spawnParticle(Particle.LAVA, target.getLocation().add(0.5, 0.5, 0.5), 3, 0.1, 0.1, 0.1, 0.02);

        // Step 2: After 1 second (20 ticks), convert to AIR TempBlock until skill finishes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.getType() == Material.MAGMA_BLOCK) {
                    if (magmaTemp != null) {
                        magmaTemp.revertBlock();
                    }
                    TempBlock airTemp = new TempBlock(target, Material.AIR.createBlockData(), 10000);
                    activeTempBlocks.add(airTemp);
                }
            }
        }.runTaskLater(AmonPackPlugin.plugin, 20L);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            revertTempBlocks();
            remove();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                if (meltedBlocksCount >= requiredMeltBlocks) {
                    fire();
                } else {
                    revertTempBlocks();
                    remove();
                }
                return;
            }

            // If not yet fully charged, continue melting blocks look target
            if (meltedBlocksCount < requiredMeltBlocks) {
                Block target = player.getTargetBlockExact(3);
                if (target != null && isEarthbendable(target) && !meltedBlockLocations.contains(target.getLocation())) {
                    meltBlock(target);
                }
            } else if (!fullyChargedNotified) {
                // Fully charged visual & sound notification!
                fullyChargedNotified = true;
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.8f);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 1.5f);
            }

            // Offset sphere: 2 blocks forward and 1 block down from eye camera
            Location eye = player.getEyeLocation();
            sphereLoc = eye.clone().add(eye.getDirection().normalize().multiply(2.0)).add(0, -1.0, 0);

            // Substantially reduced particle count
            if (meltedBlocksCount < requiredMeltBlocks) {
                player.getWorld().spawnParticle(Particle.LAVA, sphereLoc, 1, 0.05, 0.05, 0.05, 0.01);
                player.getWorld().spawnParticle(Particle.FLAME, sphereLoc, 2, 0.08, 0.08, 0.08, 0.01);
            } else {
                // Full charge distinct aura
                player.getWorld().spawnParticle(Particle.LAVA, sphereLoc, 3, 0.15, 0.15, 0.15, 0.02);
                player.getWorld().spawnParticle(Particle.FLAME, sphereLoc, 5, 0.2, 0.2, 0.2, 0.03);
            }

        } else if (state == State.FIRED) {
            projectileVel.add(new Vector(0, -0.02, 0));
            projectileLoc.add(projectileVel);

            // Low particle count for flying projectile
            player.getWorld().spawnParticle(Particle.LAVA, projectileLoc, 2, 0.1, 0.1, 0.1, 0.01);
            player.getWorld().spawnParticle(Particle.FLAME, projectileLoc, 3, 0.15, 0.15, 0.15, 0.02);

            // Side shrapnel shards spawn FROM THE FLYING PROJECTILE
            if (new Random().nextInt(3) == 0) {
                spawnProjectileShards(projectileLoc.clone());
            }

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projectileLoc, 1.8)) {
                if (entity instanceof LivingEntity le && entity.getEntityId() != player.getEntityId() && !hitEntities.contains(entity.getUniqueId())) {
                    hitEntities.add(entity.getUniqueId());
                    DamageHandler.damageEntity(le, damage, this);
                    le.setFireTicks(60);
                }
            }

            Block block = projectileLoc.getBlock();
            if (block.getType().isSolid()) {
                impact(projectileLoc);
            } else if (projectileLoc.distanceSquared(player.getLocation()) > (range * range)) {
                revertTempBlocks();
                remove();
            }
        }
    }

    private void fire() {
        state = State.FIRED;
        projectileLoc = sphereLoc != null ? sphereLoc : player.getEyeLocation().add(player.getLocation().getDirection().multiply(2.0)).add(0, -1.0, 0);
        projectileVel = player.getLocation().getDirection().normalize().multiply(1.3);

        player.getWorld().playSound(projectileLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.2f);
        bPlayer.addCooldown(this, cooldown);
    }

    private void spawnProjectileShards(Location currentProjLoc) {
        for (int i = 0; i < 2; i++) {
            Vector sideDir = new Vector(
                    (Math.random() - 0.5) * 0.8,
                    (Math.random() - 0.5) * 0.4,
                    (Math.random() - 0.5) * 0.8
            ).normalize().multiply(0.5);

            new BukkitRunnable() {
                private Location loc = currentProjLoc.clone();
                private int ticks = 0;

                @Override
                public void run() {
                    ticks++;
                    if (ticks > 10) {
                        cancel();
                        return;
                    }
                    loc.add(sideDir);
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.01);
                    for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 1.2)) {
                        if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                            DamageHandler.damageEntity(le, damage * 0.35, MoltenBlast.this);
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
                    revertTempBlocks();
                    remove();
                    return;
                }

                for (Block b : GeneralMethods.getBlocksAroundPoint(impactLoc, step + 0.5)) {
                    if (isEarthbendable(b) && b.getType() != Material.BEDROCK) {
                        if (!TempBlock.isTempBlock(b)) {
                            activeTempBlocks.add(new TempBlock(b, Material.LAVA.createBlockData(), 4000));
                        }
                    }
                }
                impactLoc.getWorld().spawnParticle(Particle.LAVA, impactLoc, step * 4 + 2, step * 0.5, 0.2, step * 0.5, 0.02);
                step++;
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);
    }

    private void revertTempBlocks() {
        for (TempBlock tb : new ArrayList<>(activeTempBlocks)) {
            if (tb != null) {
                tb.revertBlock();
            }
        }
        activeTempBlocks.clear();
    }

    @Override
    public void remove() {
        revertTempBlocks();
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
        return "1.2";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }
}
