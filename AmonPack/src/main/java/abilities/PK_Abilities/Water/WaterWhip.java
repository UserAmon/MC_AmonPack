package Abilities.PK_Abilities.Water;

import Abilities.Bending.SpecialTriggerable;
import Abilities.Bending.SpecialTriggerManager;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
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
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class WaterWhip extends WaterAbility implements AddonAbility, SpecialTriggerable {

    private enum State {
        CHARGING, EXTENDING
    }

    private State state;
    private int durationTicks = 0;
    private int maxDurationTicks = 100;

    private double currentLength = 0.3;
    private final double minLength = 1.0;
    private double maxLength;
    private double damage;
    private double knockback;
    private long cooldown;
    private double sourceRange;
    private Block sourceBlock;

    private Vector currentWhipDir = null;
    private Vector lastCameraDir = null;
    private Location currentHandLoc = null;

    public WaterWhip(Player player) {
        super(player);

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Damage", 4.5);
        this.maxLength = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.MaxRange", 7.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Knockback", 0.6);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.WaterWhip.Cooldown", 5000);
        this.maxDurationTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.WaterWhip.DurationTicks", 100);
        this.sourceRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.SourceRange", 15.0);

        if (AmonPackPlugin.ENABLE_SKILL_TREE) {
            if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
                return;
            }
            SpecialTriggerManager.registerActiveSpecial(player);
            this.state = State.EXTENDING;
            start();
        } else {
            if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
                return;
            }
            Block water = WaterAbility.getWaterSourceBlock(player, sourceRange, true);
            if (water != null) {
                this.sourceBlock = water;
                this.state = State.CHARGING;
                start();
            } else {
                remove();
            }
        }
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            finish();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                state = State.EXTENDING;
                this.currentHandLoc = sourceBlock.getLocation().add(0.5, 1.0, 0.5);
                player.getWorld().playSound(currentHandLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.2f);
                return;
            }

            Location srcLoc = sourceBlock.getLocation().add(0.5, 1.0, 0.5);
            ParticleEffect.WATER_SPLASH.display(srcLoc, 4, 0.2, 0.2, 0.2, 0.05);
            ParticleEffect.WATER_DROP.display(srcLoc, 4, 0.2, 0.2, 0.2, 0.05);
            return;
        }

        durationTicks++;
        if (durationTicks >= maxDurationTicks) {
            finish();
            return;
        }

        if (AmonPackPlugin.ENABLE_SKILL_TREE) {
            SpecialTriggerManager.applySoftCooldownToToolbar(player);
        }

        Location eyeLoc = player.getEyeLocation();
        Vector targetCameraDir = eyeLoc.getDirection().clone().normalize();

        if (currentWhipDir == null) {
            currentWhipDir = targetCameraDir.clone();
        } else {
            currentWhipDir.add(targetCameraDir.clone().subtract(currentWhipDir).multiply(0.045)).normalize();
        }

        boolean isCameraMoving = durationTicks <= 40;
        if (durationTicks <= 40) {
            currentLength = Math.min(maxLength, 0.3 + (((double) durationTicks / 40.0) * (maxLength - 0.3)));
        } else if (lastCameraDir != null) {
            double angleDiff = Math.toDegrees(lastCameraDir.angle(targetCameraDir));
            if (Double.isNaN(angleDiff)) {
                angleDiff = 0;
            }
            if (angleDiff > 0.4) {
                isCameraMoving = true;
            }

            if (angleDiff > 0.8) {
                double lengthGain = angleDiff * 0.05;
                currentLength = Math.min(maxLength, currentLength + lengthGain);
            } else {
                currentLength = Math.max(minLength, currentLength - 0.2);
            }
        }
        lastCameraDir = targetCameraDir.clone();

        Location targetHand = getHandLocation();
        if (currentHandLoc == null) {
            currentHandLoc = targetHand.clone();
        } else {
            currentHandLoc.add(targetHand.clone().subtract(currentHandLoc).multiply(0.25));
        }

        Set<LivingEntity> hitEntities = new HashSet<>();

        int segments = (int) Math.ceil(currentLength * 6.0);
        for (int i = 0; i <= segments; i++) {
            double progressRatio = (double) i / (double) segments;
            double dist = progressRatio * currentLength;

            double waveOffset = Math.sin(progressRatio * Math.PI) * 0.28 * (currentLength / maxLength);
            double verticalWave = Math.sin(progressRatio * Math.PI * 3.0 + (durationTicks * 0.4)) * 0.5;
            Vector rightVector = currentWhipDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();

            Location segmentLoc = currentHandLoc.clone()
                    .add(currentWhipDir.clone().multiply(dist))
                    .add(rightVector.multiply(waveOffset))
                    .add(0, verticalWave, 0);

            if (i % 4 == 0) {
                ParticleEffect.WATER_DROP.display(segmentLoc, 1, 0.03, 0.03, 0.03, 0.01);
                ParticleEffect.WATER_SPLASH.display(segmentLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }

            Block block = segmentLoc.getBlock();
            if (block.getType() == Material.AIR) {
                new TempBlock(block, Material.WATER).setRevertTime(150);
            }

            if (isCameraMoving) {
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(segmentLoc, 1.2)) {
                    if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                        LivingEntity target = (LivingEntity) entity;
                        if (!hitEntities.contains(target)) {
                            hitEntities.add(target);
                            DamageHandler.damageEntity(target, damage, this);
                            target.setVelocity(currentWhipDir.clone().multiply(knockback).setY(0.2));
                            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
                            ParticleEffect.WATER_SPLASH.display(target.getLocation().add(0, 1, 0), 8, 0.15, 0.15, 0.15, 0.05);
                        }
                    }
                }
            }
        }

        if (durationTicks % 8 == 0) {
            player.getWorld().playSound(currentHandLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.5f, 1.5f);
        }
    }

    private Location getHandLocation() {
        Location base = player.getLocation().clone().add(0, 1.05, 0);
        Vector forward = player.getLocation().getDirection().clone().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        return base.add(forward.multiply(0.3)).add(right.multiply(1.7));
    }

    private void finish() {
        if (AmonPackPlugin.ENABLE_SKILL_TREE) {
            SpecialTriggerManager.unregisterActiveSpecial(player);
        }
        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public TriggerType getSupportedTriggerType() {
        return TriggerType.SWAP;
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
        return "WaterWhip";
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
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        remove();
    }

    @Override
    public String getDescription() {
        return "Bicz wodny z obsługą trybu zwykłego (ze źródła wody) oraz trybu drzewka umiejętności (SWAP).";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj shift na wodzie i puść, lub naciśnij SWAP!";
    }
}
