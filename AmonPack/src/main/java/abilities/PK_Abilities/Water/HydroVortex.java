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
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class HydroVortex extends WaterAbility implements AddonAbility {

    private double scanRadius;
    private double maxRingSize;
    private long chargeRate;
    private long cooldown;
    private int totalUses;
    private int remainingUses;
    private long comboWindow;
    private long freezeDuration;
    private long barrierDuration;
    private int barrierCostPercent;
    private double streamDamage;
    private double comboDamage;

    private long startTime;
    private int absorbedWaterCount = 0;
    private double currentRingSize = 0.5;
    private double ringAngle = 0;
    private boolean isCharging = true;
    private boolean barrierActive = false;
    private long barrierStartTime = 0;

    private final List<Long> clickTimestamps = new ArrayList<>();
    private final List<TempBlock> ringTempBlocks = new ArrayList<>();

    public HydroVortex(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.scanRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.HydroVortex.ScanRadius", 10.0);
        this.maxRingSize = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.HydroVortex.MaxRingSize", 2.5);
        this.chargeRate = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.HydroVortex.ChargeRate", 1000);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.HydroVortex.Cooldown", 8000);
        this.totalUses = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.HydroVortex.Uses", 10);
        this.remainingUses = totalUses;
        this.comboWindow = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.HydroVortex.ComboWindow", 3000);
        this.freezeDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.HydroVortex.FreezeDuration", 3000);
        this.barrierDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.HydroVortex.BarrierDuration", 2000);
        this.barrierCostPercent = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.HydroVortex.BarrierCostPercent", 50);
        this.streamDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.HydroVortex.StreamDamage", 4.0);
        this.comboDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.HydroVortex.ComboDamage", 7.0);

        this.startTime = System.currentTimeMillis();

        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            revertRingBlocks();
            remove();
            return;
        }

        long now = System.currentTimeMillis();

        if (isCharging) {
            long elapsed = now - startTime;
            if (elapsed % chargeRate < 50 && absorbedWaterCount < 4) {
                absorbNextWaterBlock();
            }

            if (!player.isSneaking()) {
                if (absorbedWaterCount < 1) {
                    revertRingBlocks();
                    remove();
                    return;
                }
                isCharging = false;
            }
        }

        if (barrierActive) {
            if (now - barrierStartTime > barrierDuration) {
                barrierActive = false;
            } else {
                renderBarrierSphere();
                handleBarrierInteractions();
                return;
            }
        }

        renderOrbitingRings();
    }

    private void absorbNextWaterBlock() {
        absorbedWaterCount++;
        Location pLoc = player.getLocation();
        for (Block b : GeneralMethods.getBlocksAroundPoint(pLoc, scanRadius)) {
            if (b.getType() == Material.WATER) {
                new TempBlock(b, Material.AIR.createBlockData(), 4000);
                b.getWorld().playSound(b.getLocation(), Sound.ITEM_BUCKET_FILL, 0.8f, 1.2f);
                break;
            }
        }
    }

    private void renderOrbitingRings() {
        revertRingBlocks();
        ringAngle += 0.2;
        Location center = player.getLocation().add(0, 1.0, 0);

        // 45 degrees tilt calculation (like TideLock)
        double cos45 = Math.cos(Math.toRadians(45));
        double sin45 = Math.sin(Math.toRadians(45));

        // Max ring fraction based on absorbedWaterCount (1 = 0.5 ring, 2 = 1.0 ring, 3 = 1.5 ring, 4 = 2.0 ring)
        double ringCoverage = (absorbedWaterCount * 0.5) * Math.PI * 2;
        int points = (int) (ringCoverage * 4);
        points = Math.max(4, points);

        for (int i = 0; i < points; i++) {
            double angle = ringAngle + (i * 0.3);
            double x = Math.cos(angle) * currentRingSize;
            double z = Math.sin(angle) * currentRingSize;

            // Apply 45 degree tilt
            double yTilted = x * sin45;
            double xTilted = x * cos45;

            Location pt1 = center.clone().add(xTilted, yTilted, z);
            Block b1 = pt1.getBlock();
            if (b1.getType() == Material.AIR) {
                ringTempBlocks.add(new TempBlock(b1, Material.WATER.createBlockData(), 100));
            }

            // Opposite 45 degree tilt
            if (absorbedWaterCount >= 3) {
                Location pt2 = center.clone().add(-xTilted, -yTilted, -z);
                Block b2 = pt2.getBlock();
                if (b2.getType() == Material.AIR) {
                    ringTempBlocks.add(new TempBlock(b2, Material.WATER.createBlockData(), 100));
                }
            }
        }
    }

    private void renderBarrierSphere() {
        revertRingBlocks();
        Location center = player.getLocation().add(0, 1.0, 0);
        for (Block b : GeneralMethods.getBlocksAroundPoint(center, maxRingSize)) {
            if (b.getType() == Material.AIR && b.getLocation().distance(center) >= (maxRingSize - 0.8)) {
                ringTempBlocks.add(new TempBlock(b, Material.WATER.createBlockData(), 100));
            }
        }
    }

    private void handleBarrierInteractions() {
        Location center = player.getLocation().add(0, 1.0, 0);
        for (Entity e : GeneralMethods.getEntitiesAroundPoint(center, maxRingSize + 0.5)) {
            if (e instanceof Projectile proj && proj.getShooter() != player) {
                if (proj instanceof Fireball || proj instanceof SmallFireball) {
                    proj.remove();
                    player.getWorld().spawnParticle(Particle.SMOKE, proj.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
                } else if (proj instanceof Arrow || proj instanceof Snowball) {
                    proj.setVelocity(proj.getVelocity().multiply(-0.8));
                } else {
                    proj.remove();
                }
            } else if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                Vector push = le.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.2).setY(0.3);
                le.setVelocity(push);
            }
        }
    }

    public void onClick() {
        if (remainingUses <= 0 || barrierActive || isCharging) {
            return;
        }

        long now = System.currentTimeMillis();
        clickTimestamps.add(now);
        clickTimestamps.removeIf(t -> now - t > comboWindow);

        boolean isCombo = clickTimestamps.size() >= 3;
        if (isCombo) {
            clickTimestamps.clear();
        }

        remainingUses--;
        fireWaterStream(isCombo);

        if (remainingUses <= 0) {
            bPlayer.addCooldown(this, cooldown);
            revertRingBlocks();
            remove();
        }
    }

    public void onRightClick() {
        if (barrierActive || remainingStrikesCostCheck()) {
            return;
        }

        int cost = (remainingUses * barrierCostPercent) / 100;
        remainingUses = Math.max(0, remainingUses - Math.max(1, cost));

        barrierActive = true;
        barrierStartTime = System.currentTimeMillis();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 0.8f);

        if (remainingUses <= 0) {
            bPlayer.addCooldown(this, cooldown);
        }
    }

    private boolean remainingStrikesCostCheck() {
        return remainingUses <= 0;
    }

    private void fireWaterStream(boolean isCombo) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize().multiply(1.4);

        player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_SPLASH, 1.0f, isCombo ? 0.6f : 1.2f);

        new BukkitRunnable() {
            private Location loc = eye.clone().add(dir.clone().multiply(1.2));
            private int ticks = 0;
            private final Set<UUID> hitSet = new HashSet<>();
            private final List<TempBlock> streamTempBlocks = new ArrayList<>();

            @Override
            public void run() {
                ticks++;
                if (ticks > 25 || player == null || !player.isOnline()) {
                    for (TempBlock tb : streamTempBlocks) tb.revertBlock();
                    cancel();
                    return;
                }

                loc.add(dir);
                Block b = loc.getBlock();
                if (b.getType() == Material.AIR) {
                    streamTempBlocks.add(new TempBlock(b, Material.WATER.createBlockData(), 150));
                }

                for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, isCombo ? 2.2 : 1.5)) {
                    if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId() && !hitSet.contains(e.getUniqueId())) {
                        hitSet.add(e.getUniqueId());
                        DamageHandler.damageEntity(le, isCombo ? comboDamage : streamDamage, HydroVortex.this);

                        if (isCombo) {
                            freezeTargetInIce(le);
                        }
                        for (TempBlock tb : streamTempBlocks) tb.revertBlock();
                        cancel();
                        return;
                    }
                }

                if (loc.getBlock().getType().isSolid()) {
                    for (TempBlock tb : streamTempBlocks) tb.revertBlock();
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void freezeTargetInIce(LivingEntity target) {
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
        Block b1 = target.getLocation().getBlock();
        Block b2 = b1.getRelative(org.bukkit.block.BlockFace.UP);

        if (b1.getType() == Material.AIR) new TempBlock(b1, Material.ICE.createBlockData(), freezeDuration);
        if (b2.getType() == Material.AIR) new TempBlock(b2, Material.ICE.createBlockData(), freezeDuration);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (freezeDuration / 50L), 10));
    }

    private void revertRingBlocks() {
        for (TempBlock tb : new ArrayList<>(ringTempBlocks)) {
            if (tb != null) {
                tb.revertBlock();
            }
        }
        ringTempBlocks.clear();
    }

    @Override
    public void remove() {
        revertRingBlocks();
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player != null ? player.getLocation() : null;
    }

    @Override
    public String getName() {
        return "HydroVortex";
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
