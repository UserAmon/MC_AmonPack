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

    private enum State { GROWING, READY, STRIKING }

    private State state;
    private double damage;
    private long cooldown;
    private int maxStrikes;
    private double sourceRange;

    private Block originBlock;
    private Location originLoc;
    private int currentHeight = 0;
    private final int targetHeight = 4;
    private long growStartTime;

    private int remainingStrikes;
    private final List<TempBlock> columnTempBlocks = new ArrayList<>();
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
            revertColumn();
            remove();
            return;
        }

        if (state == State.GROWING) {
            if (!player.isSneaking()) {
                revertColumn();
                remove();
                return;
            }

            long elapsed = System.currentTimeMillis() - growStartTime;
            int desiredHeight = Math.min(targetHeight, (int) ((elapsed / 2000.0) * targetHeight) + 1);

            if (desiredHeight > currentHeight) {
                currentHeight = desiredHeight;
                Block b = originBlock.getRelative(org.bukkit.block.BlockFace.UP, currentHeight);
                if (b.getType() == Material.AIR) {
                    columnTempBlocks.add(new TempBlock(b, Material.WATER.createBlockData(), 300));
                    b.getWorld().playSound(b.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.5f, 1.2f);
                }
            }

            // Maintain TempBlocks during growth
            for (int h = 1; h <= currentHeight; h++) {
                Block b = originBlock.getRelative(org.bukkit.block.BlockFace.UP, h);
                if (b.getType() == Material.AIR) {
                    columnTempBlocks.add(new TempBlock(b, Material.WATER.createBlockData(), 300));
                }
            }

            if (elapsed >= 2000) {
                state = State.READY;
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.6f);
            }
        } else if (state == State.READY) {
            if (!player.isSneaking()) {
                revertColumn();
                remove();
                return;
            }

            // Maintain column
            for (int h = 1; h <= targetHeight; h++) {
                Block b = originBlock.getRelative(org.bukkit.block.BlockFace.UP, h);
                if (b.getType() == Material.AIR) {
                    columnTempBlocks.add(new TempBlock(b, Material.WATER.createBlockData(), 300));
                }
            }
            originLoc.getWorld().spawnParticle(Particle.SPLASH, originLoc.clone().add(0, targetHeight, 0), 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    public void onClick() {
        if (state != State.READY || remainingStrikes <= 0) {
            return;
        }

        state = State.STRIKING;
        remainingStrikes--;
        hitEntities.clear();

        Location topLoc = originLoc.clone().add(0, targetHeight, 0);
        Location targetLoc = player.getTargetBlockExact(20) != null ? player.getTargetBlockExact(20).getLocation().add(0.5, 1, 0.5) : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(15));
        Vector strikeDir = targetLoc.toVector().subtract(topLoc.toVector()).normalize();

        new BukkitRunnable() {
            private Location cur = topLoc.clone();
            private int step = 0;
            private final List<TempBlock> strikeTempBlocks = new ArrayList<>();

            @Override
            public void run() {
                step++;
                if (step > 15 || player == null || !player.isOnline()) {
                    for (TempBlock tb : strikeTempBlocks) tb.revertBlock();
                    state = State.READY;
                    if (remainingStrikes <= 0) {
                        bPlayer.addCooldown(WaterTentacle.this, cooldown);
                        revertColumn();
                        remove();
                    }
                    cancel();
                    return;
                }

                cur.add(strikeDir);
                Block b = cur.getBlock();
                if (b.getType() == Material.AIR) {
                    strikeTempBlocks.add(new TempBlock(b, Material.WATER.createBlockData(), 200));
                }

                for (Entity e : GeneralMethods.getEntitiesAroundPoint(cur, 1.5)) {
                    if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId() && !hitEntities.contains(e.getUniqueId())) {
                        hitEntities.add(e.getUniqueId());
                        DamageHandler.damageEntity(le, damage, WaterTentacle.this);
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void revertColumn() {
        for (TempBlock tb : new ArrayList<>(columnTempBlocks)) {
            if (tb != null) {
                tb.revertBlock();
            }
        }
        columnTempBlocks.clear();
    }

    @Override
    public void remove() {
        revertColumn();
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
        return "1.2";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }
}
