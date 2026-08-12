package Abilities.PK_Abilities.Water;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
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

public class WaterTentacle extends WaterAbility implements AddonAbility {

    private enum State { GROWING, READY, STRIKING, DISSOLVING }

    private State state;
    private double damage;
    private long cooldown;
    private int maxStrikes;
    private double sourceRange;

    private Block originBlock;
    private Location originLoc;
    private final int maxHeight = 5;
    private long growStartTime;
    private double waveTime = 0;

    private int remainingStrikes;
    private final List<TempBlock> activeTempBlocks = new ArrayList<>();
    private final Set<UUID> hitEntities = new HashSet<>();

    public WaterTentacle(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterTentacle.Damage", 5.5);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.WaterTentacle.Cooldown", 7000);
        this.maxStrikes = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.WaterTentacle.MaxStrikes", 3);
        this.sourceRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterTentacle.SourceRange", 15.0);

        Block target = player.getTargetBlockExact((int) sourceRange, org.bukkit.FluidCollisionMode.ALWAYS);
        if (target == null || !(target.getType() == Material.WATER || isWaterbendable(target))) {
            if (player.getLocation().getBlock().getType() == Material.WATER) {
                target = player.getLocation().getBlock();
            } else {
                return;
            }
        }

        this.originBlock = target;
        this.originLoc = target.getLocation().add(0.5, 0.5, 0.5);
        this.remainingStrikes = maxStrikes;
        this.state = State.GROWING;
        this.growStartTime = System.currentTimeMillis();

        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            revertTempBlocks();
            remove();
            return;
        }

        // Always refresh base water source block so the lake beneath never disappears
        refreshBaseWaterSource();

        if (state == State.GROWING) {
            if (!player.isSneaking()) {
                startDissolving();
                return;
            }

            long elapsed = System.currentTimeMillis() - growStartTime;
            double progress = Math.min(1.0, elapsed / 2000.0);
            int currentHeight = (int) (progress * maxHeight);

            renderWrithingTentacle(currentHeight);

            if (elapsed >= 2000) {
                state = State.READY;
                player.getWorld().playSound(originLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            }

        } else if (state == State.READY) {
            if (!player.isSneaking()) {
                startDissolving();
                return;
            }

            renderWrithingTentacle(maxHeight);
        }
    }

    private void refreshBaseWaterSource() {
        if (originBlock != null && !TempBlock.isTempBlock(originBlock)) {
            activeTempBlocks.add(new TempBlock(originBlock, Material.WATER.createBlockData(), 200));
        }
    }

    private void renderWrithingTentacle(int hLimit) {
        revertTempBlocks();
        refreshBaseWaterSource();

        waveTime += 0.12;

        for (int h = 1; h <= hLimit; h++) {
            // Writhing S-curve sin/cos offsets based on height and time
            double offsetX = Math.cos(waveTime + h * 0.8) * (0.35 + (h * 0.05));
            double offsetZ = Math.sin(waveTime + h * 0.8) * (0.35 + (h * 0.05));

            Location blockLoc = originLoc.clone().add(offsetX, h, offsetZ);
            Block b = blockLoc.getBlock();

            if (b.getType() == Material.AIR) {
                activeTempBlocks.add(new TempBlock(b, Material.WATER.createBlockData(), 200));
            }
            blockLoc.getWorld().spawnParticle(Particle.SPLASH, blockLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        if (System.currentTimeMillis() % 400 < 50) {
            originLoc.getWorld().playSound(originLoc, Sound.ITEM_BUCKET_EMPTY, 0.5f, 1.2f);
        }
    }

    public void onClick() {
        if (state != State.READY || remainingStrikes <= 0) {
            return;
        }

        state = State.STRIKING;
        remainingStrikes--;
        hitEntities.clear();
        revertTempBlocks();

        Location targetLoc = player.getTargetBlockExact(20) != null ? player.getTargetBlockExact(20).getLocation().add(0.5, 1, 0.5) : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(15));
        Vector strikeDir = targetLoc.toVector().subtract(originLoc.toVector()).normalize();

        // 3-second (60 ticks) strike and return writhing animation
        new BukkitRunnable() {
            private int ticks = 0;
            private final List<TempBlock> strikeTempBlocks = new ArrayList<>();

            @Override
            public void run() {
                ticks++;

                for (TempBlock tb : new ArrayList<>(strikeTempBlocks)) tb.revertBlock();
                strikeTempBlocks.clear();
                refreshBaseWaterSource();

                if (ticks > 60 || player == null || !player.isOnline()) {
                    for (TempBlock tb : strikeTempBlocks) tb.revertBlock();
                    strikeTempBlocks.clear();
                    state = State.READY;

                    if (remainingStrikes <= 0) {
                        bPlayer.addCooldown(WaterTentacle.this, cooldown);
                        startDissolving();
                    }
                    cancel();
                    return;
                }

                // Ticks 0-30: Slamming down toward target / Ticks 30-60: Writhing back up to upright position
                double progress;
                if (ticks <= 30) {
                    progress = ticks / 30.0;
                } else {
                    progress = (60 - ticks) / 30.0;
                }

                double extension = progress * 10.0;
                Vector tiltVec = strikeDir.clone().multiply(extension / maxHeight).add(new Vector(0, Math.sin(progress * Math.PI) * 1.8, 0));

                for (int h = 1; h <= maxHeight; h++) {
                    double waveX = Math.cos((ticks * 0.15) + h * 0.8) * 0.25;
                    double waveZ = Math.sin((ticks * 0.15) + h * 0.8) * 0.25;

                    Location pt = originLoc.clone().add(
                            (tiltVec.getX() * (h / (double) maxHeight)) + waveX,
                            (h * (1.0 - (progress * 0.7))) + (tiltVec.getY() * (h / (double) maxHeight)),
                            (tiltVec.getZ() * (h / (double) maxHeight)) + waveZ
                    );

                    Block b = pt.getBlock();
                    if (b.getType() == Material.AIR) {
                        strikeTempBlocks.add(new TempBlock(b, Material.WATER.createBlockData(), 200));
                    }

                    // Damage is dealt ONLY when strike hits entities during progress
                    if (progress > 0.5 && ticks <= 35) {
                        for (Entity e : GeneralMethods.getEntitiesAroundPoint(pt, 1.6)) {
                            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId() && !hitEntities.contains(e.getUniqueId())) {
                                hitEntities.add(e.getUniqueId());
                                DamageHandler.damageEntity(le, damage, WaterTentacle.this);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void startDissolving() {
        state = State.DISSOLVING;
        bPlayer.addCooldown(this, cooldown);

        new BukkitRunnable() {
            private int step = maxHeight;

            @Override
            public void run() {
                step--;
                if (step <= 0) {
                    revertTempBlocks();
                    remove();
                    cancel();
                    return;
                }
                renderWrithingTentacle(step);
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
        return originLoc;
    }

    @Override
    public String getName() {
        return "WaterTentacle";
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
        return "2.0";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }
}
