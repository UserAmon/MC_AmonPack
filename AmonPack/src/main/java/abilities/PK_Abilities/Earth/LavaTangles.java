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

public class LavaTangles extends LavaAbility implements AddonAbility {

    private enum State {
        GROWING, READY, STRIKING, DISSOLVING
    }

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

    public LavaTangles(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Lava.LavaTangles.Damage", 6.0);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Lava.LavaTangles.Cooldown", 8000);
        this.maxStrikes = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Lava.LavaTangles.MaxStrikes", 3);
        this.sourceRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Lava.LavaTangles.SourceRange",
                15.0);

        Block target = player.getTargetBlockExact((int) sourceRange, org.bukkit.FluidCollisionMode.ALWAYS);
        if (target == null || !(target.getType() == Material.LAVA || isEarthbendable(target))) {
            if (player.getLocation().getBlock().getType() == Material.LAVA) {
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

        // Always refresh base lava source block so the lake beneath never disappears
        refreshBaseLavaSource();

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

    private void refreshBaseLavaSource() {
        if (originBlock != null && !TempBlock.isTempBlock(originBlock)) {
            activeTempBlocks.add(new TempBlock(originBlock, Material.LAVA.createBlockData(), 200));
        }
    }

    private void renderWrithingTentacle(int hLimit) {
        revertTempBlocks();
        refreshBaseLavaSource();

        waveTime += 0.12;

        for (int h = 1; h <= hLimit; h++) {
            // Writhing S-curve sin/cos offsets based on height and time
            double offsetX = Math.cos(waveTime + h * 0.8) * (0.35 + (h * 0.05));
            double offsetZ = Math.sin(waveTime + h * 0.8) * (0.35 + (h * 0.05));

            Location blockLoc = originLoc.clone().add(offsetX, h, offsetZ);
            Block b = blockLoc.getBlock();

            if (b.getType() == Material.AIR) {
                activeTempBlocks.add(new TempBlock(b, Material.LAVA.createBlockData(), 200));
            }
            blockLoc.getWorld().spawnParticle(Particle.LAVA, blockLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        if (System.currentTimeMillis() % 400 < 50) {
            originLoc.getWorld().playSound(originLoc, Sound.BLOCK_LAVA_AMBIENT, 0.5f, 1.2f);
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

        Location topLoc = originLoc.clone().add(0, 4, 0);
        Location targetLoc = player.getTargetBlockExact(20) != null
                ? player.getTargetBlockExact(20).getLocation().add(0.5, 1, 0.5)
                : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(15));

        Vector strikeDir = targetLoc.toVector().subtract(topLoc.toVector()).normalize();
        if (strikeDir.lengthSquared() < 0.001) {
            strikeDir = player.getEyeLocation().getDirection().normalize();
        }

        Vector rightVec = strikeDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (rightVec.lengthSquared() < 0.001) {
            rightVec = new Vector(1, 0, 0);
        }
        Vector upVec = rightVec.clone().crossProduct(strikeDir).normalize();

        final Vector finalStrikeDir = strikeDir;
        final Vector finalRight = rightVec;
        final Vector finalUp = upVec;

        double maxReach = Math.min(16.0, topLoc.distance(targetLoc));

        new BukkitRunnable() {

            private int tick = 0;
            private final int totalTicks = 12; // Fast extension and return
            private final List<TempBlock> strikeTempBlocks = new ArrayList<>();

            @Override
            public void run() {
                tick++;

                // Revert previous tick temp blocks
                for (TempBlock tb : strikeTempBlocks) {
                    tb.revertBlock();
                }
                strikeTempBlocks.clear();

                if (tick > totalTicks || player == null || !player.isOnline()) {
                    state = State.READY;

                    if (remainingStrikes <= 0) {
                        bPlayer.addCooldown(LavaTangles.this, cooldown);
                        startDissolving();
                    }
                    cancel();
                    return;
                }

                // Calculate extension distance (peaks at 50% progress, returns by 100%)
                double progressRatio = (double) tick / (double) totalTicks;
                double extensionRatio = Math.sin(progressRatio * Math.PI); // 0 -> 1 -> 0 smooth bell curve
                double currentDist = maxReach * extensionRatio;

                topLoc.getWorld().playSound(topLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
                topLoc.getWorld().playSound(topLoc, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.5f);

                // Render writhing lava tentacle along current length
                int segments = (int) Math.max(3, Math.ceil(currentDist * 1.5));
                for (int i = 0; i <= segments; i++) {
                    double segRatio = (double) i / (double) segments;
                    double dist = segRatio * currentDist;

                    // Sinusoidal writhing wave displacement
                    double waveAngle = (segRatio * Math.PI * 2.5) + (tick * 0.6);
                    double waveAmplitude = Math.sin(segRatio * Math.PI) * 0.7;
                    double sideOffset = Math.sin(waveAngle) * waveAmplitude;
                    double upOffset = Math.cos(waveAngle) * waveAmplitude * 0.5;

                    Location segLoc = topLoc.clone()
                            .add(finalStrikeDir.clone().multiply(dist))
                            .add(finalRight.clone().multiply(sideOffset))
                            .add(finalUp.clone().multiply(upOffset));

                    // Lava particles along tentacle body
                    segLoc.getWorld().spawnParticle(Particle.LAVA, segLoc, 2, 0.1, 0.1, 0.1, 0.05);
                    segLoc.getWorld().spawnParticle(Particle.FLAME, segLoc, 3, 0.08, 0.08, 0.08, 0.02);

                    if (i == segments) {
                        segLoc.getWorld().spawnParticle(Particle.EXPLOSION, segLoc, 1, 0.1, 0.1, 0.1, 0.0);
                    }

                    // Lava TempBlocks at tip and middle nodes
                    if (i % 2 == 0 || i == segments) {
                        Block b = segLoc.getBlock();
                        if (b.getType() == Material.AIR) {
                            strikeTempBlocks.add(new TempBlock(b, Material.LAVA.createBlockData(), 100));
                        }
                    }

                    // Hit detection on extension phase
                    if (progressRatio <= 0.6) {
                        for (Entity e : GeneralMethods.getEntitiesAroundPoint(segLoc, 1.4)) {
                            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()
                                    && !hitEntities.contains(e.getUniqueId())) {
                                hitEntities.add(e.getUniqueId());
                                DamageHandler.damageEntity(le, damage, LavaTangles.this);
                                le.setFireTicks(60);
                                Vector push = finalStrikeDir.clone().multiply(0.8).setY(0.3);
                                le.setVelocity(push);
                                segLoc.getWorld().playSound(segLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.2f);
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
        return "LavaTangles";
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
