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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LavaTangles extends LavaAbility implements AddonAbility {

    private Block originBlock;
    private Location originLoc;
    private double damage;
    private long cooldown;
    private int maxStrikes;
    private int strikesUsed = 0;
    private double sourceRange;

    private boolean isStriking = false;
    private long lastTempBlockRefresh = 0;
    private TempBlock originTempBlock;
    private double tentacleAngle = 0;

    public LavaTangles(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.LavaTangles.Damage", 6.0);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.LavaTangles.Cooldown", 8000);
        this.maxStrikes = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.LavaTangles.MaxStrikes", 3);
        this.sourceRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.LavaTangles.SourceRange", 15.0);

        Block target = player.getTargetBlockExact((int) sourceRange);
        if (target == null || !(target.getType() == Material.LAVA || isEarthbendable(target))) {
            return;
        }

        this.originBlock = target;
        this.originLoc = target.getLocation().add(0.5, 0.5, 0.5);

        if (!TempBlock.isTempBlock(originBlock)) {
            this.originTempBlock = new TempBlock(originBlock, Material.LAVA.createBlockData(), 100);
        }

        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (!player.isSneaking()) {
            finishSkill();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTempBlockRefresh >= 80) {
            lastTempBlockRefresh = now;
            if (originBlock != null && !TempBlock.isTempBlock(originBlock)) {
                this.originTempBlock = new TempBlock(originBlock, Material.LAVA.createBlockData(), 100);
            }
        }

        if (!isStriking) {
            renderWrithingTentacle();
        }
    }

    private void renderWrithingTentacle() {
        tentacleAngle += 0.2;
        int heightPoints = 14;
        double maxHeight = 3.5;

        for (int i = 0; i < heightPoints; i++) {
            double progress = (double) i / heightPoints;
            double currentY = progress * maxHeight;
            double radius = 0.3 * Math.sin(progress * Math.PI);

            double offsetX = Math.cos(tentacleAngle + (progress * 3)) * radius;
            double offsetZ = Math.sin(tentacleAngle + (progress * 3)) * radius;

            Location pt = originLoc.clone().add(offsetX, currentY, offsetZ);
            pt.getWorld().spawnParticle(Particle.LAVA, pt, 1, 0.05, 0.05, 0.05, 0.01);
            pt.getWorld().spawnParticle(Particle.FLAME, pt, 2, 0.03, 0.03, 0.03, 0.02);
            if (i % 3 == 0) {
                pt.getWorld().spawnParticle(Particle.DRIPPING_LAVA, pt, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    public void onClick() {
        if (isStriking || strikesUsed >= maxStrikes) {
            return;
        }

        Block targetBlock = player.getTargetBlockExact(20);
        if (targetBlock == null) return;

        strikesUsed++;
        isStriking = true;
        Location targetLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);

        player.getWorld().playSound(originLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
        player.getWorld().playSound(originLoc, Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 0.9f);

        new BukkitRunnable() {
            private int frame = 0;
            private final int totalFrames = 30; // ~1.5s strike + ~1.5s retract = 3s
            private final Set<UUID> hitSet = new HashSet<>();

            @Override
            public void run() {
                frame++;
                if (frame > totalFrames || player == null || !player.isOnline()) {
                    isStriking = false;
                    if (strikesUsed >= maxStrikes) {
                        finishSkill();
                    }
                    cancel();
                    return;
                }

                double t;
                if (frame <= 12) {
                    t = (double) frame / 12.0; // Strike out
                } else {
                    t = 1.0 - ((double) (frame - 12) / 18.0); // Retract back
                }

                Location currentTip = originLoc.clone().add(targetLoc.clone().subtract(originLoc).toVector().multiply(t));
                currentTip.add(0, Math.sin(t * Math.PI) * 2.0, 0);

                int points = 12;
                for (int i = 0; i <= points; i++) {
                    double p = (double) i / points;
                    Location segment = originLoc.clone().add(currentTip.clone().subtract(originLoc).toVector().multiply(p));
                    segment.getWorld().spawnParticle(Particle.LAVA, segment, 2, 0.1, 0.1, 0.1, 0.02);
                    segment.getWorld().spawnParticle(Particle.FLAME, segment, 3, 0.08, 0.08, 0.08, 0.03);
                }

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentTip, 1.8)) {
                    if (entity instanceof LivingEntity le && entity.getEntityId() != player.getEntityId() && !hitSet.contains(entity.getUniqueId())) {
                        hitSet.add(entity.getUniqueId());
                        DamageHandler.damageEntity(le, damage, LavaTangles.this);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                        le.setFireTicks(40);
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void finishSkill() {
        if (bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
        }
        remove();
    }

    @Override
    public void remove() {
        if (originTempBlock != null) {
            originTempBlock.revertBlock();
        }
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return originLoc != null ? originLoc : (player != null ? player.getLocation() : null);
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
        return "1.1";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }
}
