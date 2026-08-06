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

    private int durationTicks = 0;
    private int maxDurationTicks = 100; // 100 ticków = 5 sekund

    private double currentLength = 1.5; // Startowa długość = 1.5 bloka
    private final double minLength = 1.5;
    private double maxLength; // Krótszy max zasięg (7.0 bloków)
    private double damage;
    private double knockback;
    private long cooldown;

    private Vector currentWhipDir = null; // Kierunek fizyczny bicza (śledzący kamerę z opóźnieniem)
    private Vector lastCameraDir = null;

    public WaterWhip(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Damage", 4.5);
        this.maxLength = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.MaxRange", 7.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Knockback", 0.6);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.WaterWhip.Cooldown", 5000);
        this.maxDurationTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.WaterWhip.DurationTicks",
                100);

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
            // Fizyka opóźnienia: bicz płynnie zmierza w stronę celownika kamery (LERP
            // factor 0.18)
            currentWhipDir.add(targetCameraDir.clone().subtract(currentWhipDir).multiply(0.18)).normalize();
        }

        // Obliczanie prędkości obrotu kamery i dwukrotnie zwolnione tempo rozciągania
        // bicza
        if (lastCameraDir != null) {
            double angleDiff = Math.toDegrees(lastCameraDir.angle(targetCameraDir));
            if (Double.isNaN(angleDiff)) {
                angleDiff = 0;
            }

            if (angleDiff > 0.8) {
                double lengthGain = angleDiff * 0.15; // Dwukrotnie wolniejszy wzrost długości
                currentLength = Math.min(maxLength, currentLength + lengthGain);
            } else {
                currentLength = Math.max(minLength, currentLength - 0.25);
            }
        }
        lastCameraDir = targetCameraDir.clone();

        Location handLoc = getHandLocation();
        Set<LivingEntity> hitEntities = new HashSet<>();

        int segments = (int) Math.ceil(currentLength * 6.0);
        for (int i = 0; i <= segments; i++) {
            double progressRatio = (double) i / (double) segments;
            double dist = progressRatio * currentLength;

            double waveOffset = Math.sin(progressRatio * Math.PI) * 0.28 * (currentLength / maxLength);
            Vector rightVector = currentWhipDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();

            Location segmentLoc = handLoc.clone()
                    .add(currentWhipDir.clone().multiply(dist))
                    .add(rightVector.multiply(waveOffset));

            if (i % 4 == 0) {
                ParticleEffect.WATER_DROP.display(segmentLoc, 1, 0.03, 0.03, 0.03, 0.01);
                ParticleEffect.WATER_SPLASH.display(segmentLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }

            Block block = segmentLoc.getBlock();
            if (block.getType() == Material.AIR) {
                new TempBlock(block, Material.WATER).setRevertTime(150);
            }

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(segmentLoc, 1.2)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    if (!hitEntities.contains(target)) {
                        hitEntities.add(target);
                        DamageHandler.damageEntity(target, damage, this);
                        target.setVelocity(currentWhipDir.clone().multiply(knockback).setY(0.2));
                        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
                        ParticleEffect.WATER_SPLASH.display(target.getLocation().add(0, 1, 0), 8, 0.15, 0.15, 0.15,
                                0.05);
                    }
                }
            }
        }

        if (durationTicks % 8 == 0) {
            player.getWorld().playSound(handLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.5f, 1.5f);
        }
    }

    private Location getHandLocation() {
        Location base = player.getLocation().clone().add(0, 1.05, 0);
        Vector forward = player.getLocation().getDirection().clone().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        return base.add(forward.multiply(0.3)).add(right.multiply(1.2));
    }

    private void finish() {
        SpecialTriggerManager.unregisterActiveSpecial(player);
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
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public String getAuthor() {
        return "Amon";
    }

    @Override
    public String getVersion() {
        return "2.2";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }
}
