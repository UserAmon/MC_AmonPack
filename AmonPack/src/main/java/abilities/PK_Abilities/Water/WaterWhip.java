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

    private double currentLength = 1.5; // Startowa długość = 1.5 bloka
    private final double minLength = 1.5;
    private double maxLength; // Krótszy max zasięg (np. 7.0 bloków)
    private double damage;
    private double knockback;
    private long cooldown;

    private Vector lastDirection = null;

    public WaterWhip(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Damage", 4.5);
        this.maxLength = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.MaxRange", 7.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Knockback", 0.6);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.WaterWhip.Cooldown", 5000);
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

        // Obliczanie prędkości ruchu myszką (kąt obrotu kamery między tickami)
        if (lastDirection != null) {
            double angleDiff = Math.toDegrees(lastDirection.angle(currentDirection));
            if (Double.isNaN(angleDiff)) {
                angleDiff = 0;
            }

            // Płynne rozciąganie bicza przy szybkim ruchu myszką
            if (angleDiff > 0.8) {
                double lengthGain = angleDiff * 0.35;
                currentLength = Math.min(maxLength, currentLength + lengthGain);
            } else {
                // Skracanie do rozmiaru bazowego gdy gracz zwalnia
                currentLength = Math.max(minLength, currentLength - 0.35);
            }
        }
        lastDirection = currentDirection.clone();

        // Punkt kotwiczenia biegnący z prawej ręki (jak w WaterFist)
        Location handLoc = getHandLocation();
        Set<LivingEntity> hitEntities = new HashSet<>();

        // Bardzo płynne renderowanie segmentów (gęstość 6.0 cząsteczek na blok)
        int segments = (int) Math.ceil(currentLength * 6.0);
        for (int i = 0; i <= segments; i++) {
            double progressRatio = (double) i / (double) segments;
            double dist = progressRatio * currentLength;

            // Wygięcie fali bicza
            double waveOffset = Math.sin(progressRatio * Math.PI) * 0.28 * (currentLength / maxLength);
            Vector rightVector = currentDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();

            Location segmentLoc = handLoc.clone()
                    .add(currentDirection.clone().multiply(dist))
                    .add(rightVector.multiply(waveOffset));

            // Efekty cząsteczkowe wody
            ParticleEffect.WATER_WAKE.display(segmentLoc, 2, 0.05, 0.05, 0.05, 0.01);
            ParticleEffect.WATER_SPLASH.display(segmentLoc, 2, 0.05, 0.05, 0.05, 0.01);
            if (i % 4 == 0) {
                ParticleEffect.WATER_DROP.display(segmentLoc, 1, 0.03, 0.03, 0.03, 0.01);
            }

            Block block = segmentLoc.getBlock();
            if (block.getType() == Material.AIR) {
                new TempBlock(block, Material.WATER).setRevertTime(150);
            }

            // Kolizje i obrażenia
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(segmentLoc, 1.2)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    if (!hitEntities.contains(target)) {
                        hitEntities.add(target);
                        DamageHandler.damageEntity(target, damage, this);
                        target.setVelocity(currentDirection.clone().multiply(knockback).setY(0.2));
                        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
                        ParticleEffect.WATER_SPLASH.display(target.getLocation().add(0, 1, 0), 8, 0.15, 0.15, 0.15, 0.05);
                    }
                }
            }
        }

        if (durationTicks % 8 == 0) {
            player.getWorld().playSound(handLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.5f, 1.5f);
        }
    }

    private Location getHandLocation() {
        // Dokładne oddalenie ręki jak w WaterFist (offset w prawo 1.2 i na wysokość 1.05)
        Location base = player.getLocation().clone().add(0, 1.05, 0);
        Vector forward = player.getLocation().getDirection().clone().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        return base.add(forward.multiply(0.3)).add(right.multiply(1.2));
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
        return "2.1";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}
}
