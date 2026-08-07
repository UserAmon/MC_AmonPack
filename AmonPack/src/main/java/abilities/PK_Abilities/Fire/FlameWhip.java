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

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameWhip.Damage", 4.0);
        this.maxLength = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameWhip.MaxRange", 9.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameWhip.Knockback", 0.7);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FlameWhip.Cooldown", 6000L);
        this.maxDurationTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameWhip.DurationTicks", 80);
        this.burnDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameWhip.BurnDuration", 60);

        SpecialTriggerManager.registerActiveSpecial(player);
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

        SpecialTriggerManager.applySoftCooldownToToolbar(player);

        Location eyeLoc = player.getEyeLocation();
        Vector targetCameraDir = eyeLoc.getDirection().clone().normalize();

        if (currentWhipDir == null) {
            currentWhipDir = targetCameraDir.clone();
        } else {
            // Zmniejszona 2-krotnie prędkość podążania za kamerą (LERP factor 0.045)
            currentWhipDir.add(targetCameraDir.clone().subtract(currentWhipDir).multiply(0.045)).normalize();
        }

        // Wydłużenie rozciągania: 2 sekundy (40 ticków) na osiągnięcie pełnej długości od bardzo krótkiego przy dłoni
        if (durationTicks <= 40) {
            currentLength = Math.min(maxLength, 0.3 + (((double) durationTicks / 40.0) * (maxLength - 0.3)));
        } else if (lastCameraDir != null) {
            double angleDiff = Math.toDegrees(lastCameraDir.angle(targetCameraDir));
            if (Double.isNaN(angleDiff)) {
                angleDiff = 0;
            }

            // Spowolnione rozciąganie ruchem kamery
            if (angleDiff > 0.8) {
                double lengthGain = angleDiff * 0.05;
                currentLength = Math.min(maxLength, currentLength + lengthGain);
            } else {
                currentLength = Math.max(minLength, currentLength - 0.2);
            }
        }
        lastCameraDir = targetCameraDir.clone();

        // Bezwładność i spowolnienie podążania ręki za ruchem gracza
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

        renderWhipBranch(leftHand, currentWhipDir, rightVector, -1, hitEntities);
        renderWhipBranch(rightHand, currentWhipDir, rightVector, 1, hitEntities);

        if (durationTicks % 4 == 0) {
            eyeLoc.getWorld().playSound(eyeLoc, Sound.ITEM_FLINTANDSTEEL_USE, 0.8f, 1.2f);
            eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.4f);
        }
    }

    private void renderWhipBranch(Location handLoc, Vector whipDir, Vector rightVec, int sideMultiplier, Set<LivingEntity> hitEntities) {
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
                ParticleEffect.FLAME.display(segmentLoc, 1, 0.03, 0.03, 0.03, 0.01);
            }
            if (i % 4 == 0) {
                ParticleEffect.SMOKE_NORMAL.display(segmentLoc, 1, 0.02, 0.02, 0.02, 0.01);
            }
            if (i == segments) {
                ParticleEffect.LAVA.display(segmentLoc, 2, 0.1, 0.1, 0.1, 0.05);
            }

            Block block = segmentLoc.getBlock();
            if (block.getType().isSolid()) {
                break;
            }

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
                        ParticleEffect.FLAME.display(segmentLoc, 10, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }
        }
    }

    private void finish() {
        bPlayer.addCooldown(this, cooldown);
        SpecialTriggerManager.unregisterActiveSpecial(player);
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
        return "1.0";
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
        remove();
    }

    @Override
    public String getDescription() {
        return "Podwójne ogniste bicze podążające z opóźnieniem i bezwładnością za kamerą i ruchem gracza (2s rozwijanie, falowanie góra/dół).";
    }

    @Override
    public String getInstructions() {
        return "Naciśnij F (SWAP), aby aktywować podwójny ognisty bicz!";
    }
}
