package Abilities.PK_Abilities.Water;

import Abilities.Bending.SpecialTriggerable;
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

    private double currentLength = 2.0; // Startowa i minimalna długość = 2 bloki
    private final double minLength = 2.0;
    private double maxLength;
    private double damage;
    private double knockback;
    private long cooldown;

    private Vector lastDirection = null;

    public WaterWhip(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Damage", 4.0);
        this.maxLength = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.MaxRange", 12.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Knockback", 0.6);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.WaterWhip.Cooldown", 6000);
        this.maxDurationTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.WaterWhip.DurationTicks", 100);

        start();
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        durationTicks++;
        if (durationTicks >= maxDurationTicks) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }

        Location eyeLoc = player.getEyeLocation();
        Vector currentDirection = eyeLoc.getDirection().clone().normalize();

        // Wyliczanie prędkości ruchu myszką (kąt obrotu kamery między tickami)
        if (lastDirection != null) {
            double angleDiff = Math.toDegrees(lastDirection.angle(currentDirection));
            if (Double.isNaN(angleDiff)) {
                angleDiff = 0;
            }

            // Rozciąganie bicza przy szybkim ruchu myszką
            if (angleDiff > 1.0) {
                double lengthGain = angleDiff * 0.45;
                currentLength = Math.min(maxLength, currentLength + lengthGain);
            } else {
                // Gdy ruch ustaje, bicz powoli skraca się z powrotem do 2 bloków
                currentLength = Math.max(minLength, currentLength - 0.4);
            }
        }
        lastDirection = currentDirection.clone();

        Location handLoc = getHandLocation();
        Set<LivingEntity> hitEntities = new HashSet<>();

        // Renderowanie bicza od ręki gracza w kierunku patrzania
        int segments = (int) Math.ceil(currentLength * 2.5);
        for (int i = 0; i <= segments; i++) {
            double progressRatio = (double) i / (double) segments;
            double dist = progressRatio * currentLength;

            // Fala / wygięcie bicza w zależności od długości
            double waveOffset = Math.sin(progressRatio * Math.PI) * 0.35 * (currentLength / maxLength);
            Vector rightVector = currentDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();

            Location segmentLoc = handLoc.clone()
                    .add(currentDirection.clone().multiply(dist))
                    .add(rightVector.multiply(waveOffset));

            // Cząsteczki wodne bicza (jak w WaterFist)
            ParticleEffect.WATER_WAKE.display(segmentLoc, 3, 0.08, 0.08, 0.08, 0.02);
            ParticleEffect.WATER_SPLASH.display(segmentLoc, 3, 0.08, 0.08, 0.08, 0.02);
            if (i % 3 == 0) {
                ParticleEffect.WATER_DROP.display(segmentLoc, 2, 0.05, 0.05, 0.05, 0.01);
            }

            Block block = segmentLoc.getBlock();
            if (block.getType() == Material.AIR) {
                new TempBlock(block, Material.WATER).setRevertTime(180);
            }

            // Kolizja z przeciwnikami i zadawanie obrażeń + odrzut
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(segmentLoc, 1.3)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    if (!hitEntities.contains(target)) {
                        hitEntities.add(target);
                        DamageHandler.damageEntity(target, damage, this);
                        target.setVelocity(currentDirection.clone().multiply(knockback).setY(0.2));
                        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
                        ParticleEffect.WATER_SPLASH.display(target.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.08);
                    }
                }
            }
        }

        if (durationTicks % 8 == 0) {
            player.getWorld().playSound(handLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.4f);
        }
    }

    private Location getHandLocation() {
        Location base = player.getLocation().clone().add(0, 1.2, 0);
        Vector forward = player.getLocation().getDirection().clone().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        return base.add(forward.multiply(0.4)).add(right.multiply(0.4));
    }

    @Override
    public TriggerType getSupportedTriggerType() {
        return TriggerType.BOTH;
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
        return "2.0";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}
}
