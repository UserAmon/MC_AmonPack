package Abilities.PK_Abilities.Fire;

import Abilities.Bending.SpecialTriggerable;
import Abilities.Bending.SpecialTriggerManager;
import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class FlameWhip extends FireAbility implements AddonAbility, SpecialTriggerable {

    private int durationTicks = 0;
    private int maxDurationTicks = 80;

    private double currentLength = 0.3;
    private final double minLength = 1.0;
    private double maxLength;
    private double damage;
    private double knockback;
    private long cooldown;
    private int burnDuration;

    private Vector currentWhipDir = null;
    private Vector lastCameraDir = null;
    private Location currentHandLoc = null;

    public FlameWhip(Player player) {
        super(player);

        if (AmonPackPlugin.ENABLE_SKILL_TREE) {
            if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
                return;
            }
            SpecialTriggerManager.registerActiveSpecial(player);
        } else {
            if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
                return;
            }
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameWhip.Damage", 4.0);
        this.maxLength = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameWhip.MaxRange", 9.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameWhip.Knockback", 0.7);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FlameWhip.Cooldown", 6000L);
        this.maxDurationTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameWhip.DurationTicks", 80);
        this.burnDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameWhip.BurnDuration", 60);

        start();
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            finish();
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
        if (durationTicks <= 20) {
            currentLength = maxLength * ((double) durationTicks / 80.0);
        } else if (lastCameraDir != null) {
            double angleDiff = Math.toDegrees(lastCameraDir.angle(targetCameraDir));
            if (Double.isNaN(angleDiff)) {
                angleDiff = 0;
            }
            if (angleDiff > 0.4) {
                isCameraMoving = true;
            }

            if (angleDiff > 0.8) {
                double lengthGain = angleDiff * 0.02;
                currentLength = Math.min(maxLength, currentLength + lengthGain);
            } else {
                currentLength = Math.max(minLength, currentLength - 0.1);
            }
        }
        lastCameraDir = targetCameraDir.clone();

        Location targetHand = eyeLoc.clone();
        if (currentHandLoc == null) {
            currentHandLoc = targetHand.clone();
        } else {
            currentHandLoc.add(targetHand.clone().subtract(currentHandLoc).multiply(0.25));
        }

        Vector rightVector = currentWhipDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Location leftHand = currentHandLoc.clone().add(rightVector.clone().multiply(-1.1)).subtract(0, 0.3, 0);
        Location rightHand = currentHandLoc.clone().add(rightVector.clone().multiply(1.1)).subtract(0, 0.3, 0);

        Set<LivingEntity> hitEntities = new HashSet<>();

        renderWhipBranch(leftHand, currentWhipDir, rightVector, -1, hitEntities, isCameraMoving);
        renderWhipBranch(rightHand, currentWhipDir, rightVector, 1, hitEntities, isCameraMoving);

        if (durationTicks % 4 == 0) {
            eyeLoc.getWorld().playSound(eyeLoc, Sound.ITEM_FLINTANDSTEEL_USE, 0.8f, 1.2f);
            eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.4f);
        }
    }

    private void renderWhipBranch(Location handLoc, Vector whipDir, Vector rightVec, int sideMultiplier,
            Set<LivingEntity> hitEntities, boolean isCameraMoving) {
        boolean isBlue = bPlayer.hasElement(com.projectkorra.projectkorra.Element.BLUE_FIRE)
                || bPlayer.canUseSubElement(com.projectkorra.projectkorra.Element.BLUE_FIRE);
        int segments = (int) Math.ceil(currentLength * 6.0);
        for (int i = 0; i <= segments; i++) {
            double progressRatio = (double) i / (double) segments;
            double dist = progressRatio * currentLength;

            double waveOffset = Math.sin(progressRatio * Math.PI) * 0.3 * sideMultiplier * (currentLength / maxLength);
            double verticalWave = Math.sin(progressRatio * Math.PI * 3.0 + (durationTicks * 0.4)) * 0.5;

            Location segmentLoc = handLoc.clone()
                    .add(whipDir.clone().multiply(dist))
                    .add(rightVec.clone().multiply(waveOffset))
                    .add(0, verticalWave, 0);

            if (i % 2 == 0) {
                if (isBlue) {
                    segmentLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, segmentLoc, 1, 0.03, 0.03, 0.03,
                            0.01);
                } else {
                    ParticleEffect.FLAME.display(segmentLoc, 1, 0.03, 0.03, 0.03, 0.01);
                }
            }
            if (i % 4 == 0) {
                ParticleEffect.SMOKE_NORMAL.display(segmentLoc, 1, 0.02, 0.02, 0.02, 0.01);
            }
            if (i == segments) {
                if (isBlue) {
                    segmentLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, segmentLoc, 3, 0.1, 0.1, 0.1, 0.05);
                } else {
                    ParticleEffect.LAVA.display(segmentLoc, 2, 0.1, 0.1, 0.1, 0.05);
                }
            }

            Block block = segmentLoc.getBlock();
            if (block.getType().isSolid()) {
                break;
            }

            if (isCameraMoving) {
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(segmentLoc, 1.3)) {
                    if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                        LivingEntity target = (LivingEntity) entity;
                        if (!hitEntities.contains(target)) {
                            hitEntities.add(target);
                            DamageHandler.damageEntity(target, damage, this);
                            target.setFireTicks(burnDuration);

                            Vector kb = whipDir.clone().multiply(knockback).setY(0.25);
                            target.setVelocity(kb);

                            segmentLoc.getWorld().playSound(segmentLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
                            if (isBlue) {
                                segmentLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, segmentLoc, 10, 0.3, 0.3,
                                        0.3, 0.1);
                            } else {
                                ParticleEffect.FLAME.display(segmentLoc, 10, 0.3, 0.3, 0.3, 0.1);
                            }
                        }
                    }
                }
            }
        }
    }

    private void finish() {
        bPlayer.addCooldown(this, cooldown);
        if (AmonPackPlugin.ENABLE_SKILL_TREE) {
            SpecialTriggerManager.unregisterActiveSpecial(player);
        }
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
        return "FlameWhip";
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
        return false;
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        finish();
    }

    @Override
    public String getDescription() {
        return "Wystrzeliwuje dwa ogniste bicze z obu rąk z pełną obsługą niebieskiego ognia.";
    }

    @Override
    public String getInstructions() {
        return "Naciśnij F (SWAP) lub kliknij LPM aby uderzyć ognistymi biczami!";
    }
}
