package Abilities.PK_Abilities.Fire;

import Abilities.Bending.SmokeAbility;
import Abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Coil extends FireAbility implements AddonAbility {

    private enum State { CHARGING, FIRED }

    private State state;
    private long startTime;
    private int maxRings;
    private long chargeIntervalPerRing;
    private int ringCount = 0;
    private double projectileSpeed;
    private double randomnessFactor;
    private int lightningVisualInterval;
    private double smokeRadius;
    private double aoeDamage;
    private double aoeKnockback;
    private double damagePerProjectile;
    private long cooldown;

    private double ringAngle = 0;

    public Coil(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.maxRings = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Coil.MaxRings", 3);
        this.chargeIntervalPerRing = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.Coil.ChargeIntervalPerRing", 1000);
        this.projectileSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.ProjectileSpeed", 0.7);
        this.randomnessFactor = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.RandomnessFactor", 0.45);
        this.lightningVisualInterval = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Coil.LightningVisualInterval", 6);
        this.smokeRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.SmokeRadius", 5.0);
        this.aoeDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.AoEDamage", 8.0);
        this.aoeKnockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.AoEKnockback", 1.2);
        this.damagePerProjectile = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.DamagePerProjectile", 4.5);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.Coil.Cooldown", 8000);

        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();

        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (FirelordStanceManager.isActive(player)) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§6⚡ Firelord — §eCoil"));
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                fire();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            ringCount = Math.min(maxRings, (int) (elapsed / chargeIntervalPerRing) + 1);

            renderTideLockStyleSlowRings();

            if (elapsed % 600 < 50) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_COPPER_BREAK, 0.4f, 1.0f + (ringCount * 0.2f));
            }
        }
    }

    private void renderTideLockStyleSlowRings() {
        ringAngle += 0.08; // Slower rotation like TideLock
        Location center = player.getLocation().add(0, 1.0, 0);
        boolean isFirelord = FirelordStanceManager.isActive(player);

        for (int r = 0; r < ringCount; r++) {
            double radius = 1.4 + (r * 0.5);
            double yOffset = (r - 1) * 0.2;

            int points = 16;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i + (r % 2 == 0 ? ringAngle : -ringAngle);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location pt = center.clone().add(x, yOffset + Math.sin(angle) * 0.2, z);

                if (isFirelord) {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pt, 2, 0.02, 0.02, 0.02, 0.05);
                    player.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(180, 220, 255), 1.0f));
                } else {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pt, 1, 0.01, 0.01, 0.01, 0.01);
                    if (i % 4 == 0) {
                        player.getWorld().spawnParticle(Particle.FIREWORK, pt, 1, 0.01, 0.01, 0.01, 0.01);
                    }
                }
            }
        }
    }

    private void fire() {
        state = State.FIRED;
        Location eye = player.getEyeLocation();
        Vector baseDir = eye.getDirection().setY(0.02).normalize();

        player.getWorld().playSound(eye, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.6f);

        boolean isFirelord = FirelordStanceManager.isActive(player);
        FirelordStance stance = FirelordStanceManager.getStance(player);

        double actualSpeed = isFirelord ? projectileSpeed * stance.getSpeedMultiplier() : projectileSpeed;
        double actualDamage = isFirelord ? damagePerProjectile * stance.getDamageMultiplier() : damagePerProjectile;
        double actualSmokeRadius = isFirelord ? smokeRadius * stance.getRangeMultiplier() : smokeRadius;
        long actualCooldown = isFirelord ? (long) (cooldown * stance.getCooldownMultiplier()) : cooldown;

        for (int r = 0; r < ringCount; r++) {
            Vector spreadDir = baseDir.clone().add(new Vector(
                    (Math.random() - 0.5) * 0.8,
                    0.02,
                    (Math.random() - 0.5) * 0.8
            )).normalize().multiply(actualSpeed);

            spawnGroundLightningProjectile(eye.clone(), spreadDir, actualDamage, actualSmokeRadius);
        }

        bPlayer.addCooldown(this, actualCooldown);
        remove();
    }

    private void spawnGroundLightningProjectile(Location startLoc, Vector initialVel, double currentDamage, double currentSmokeRadius) {
        Abilities.Util_Objects.LightningBolt bolt = new Abilities.Util_Objects.LightningBolt(
                player, this, startLoc, initialVel.normalize(), currentDamage, 25.0, 0, false
        );

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > 50 || player == null || !player.isOnline() || bolt.isDead()) {
                    cancel();
                    return;
                }

                Location currentLoc = bolt.getLocation();

                if (ticks % lightningVisualInterval == 0) {
                    currentLoc.getWorld().strikeLightningEffect(currentLoc);
                }

                // Smoke Source Interaction Check
                SmokeSource nearSource = SmokeAbility.UseSmokeSource(player, currentSmokeRadius);
                if (nearSource != null || checkSmokeSourcesNear(currentLoc, currentSmokeRadius)) {
                    triggerSmokeElectrification(currentLoc, currentSmokeRadius);
                    cancel();
                    return;
                }

                List<Abilities.Util_Objects.LightningBolt> branches = bolt.progress();
                if (branches != null && !branches.isEmpty()) {
                    for (Abilities.Util_Objects.LightningBolt b : branches) {
                        b.progress();
                    }
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private boolean checkSmokeSourcesNear(Location targetLoc, double currentSmokeRadius) {
        SmokeSource source = SmokeAbility.UseSmokeSource(player, currentSmokeRadius);
        if (source != null) {
            SmokeAbility.DeleteSource(source);
            return true;
        }
        return false;
    }

    private void triggerSmokeElectrification(Location loc, double currentSmokeRadius) {
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().strikeLightningEffect(loc.clone().add(1.5, 0, 1.5));
        loc.getWorld().strikeLightningEffect(loc.clone().add(-1.5, 0, -1.5));

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2, 0.5, 0.5, 0.5, 0.0);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 30, 1.5, 1.5, 1.5, 0.1);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 40, 2.0, 2.0, 2.0, 0.2);

        boolean isFirelord = FirelordStanceManager.isActive(player);
        FirelordStance stance = FirelordStanceManager.getStance(player);
        double actualAoEDamage = isFirelord ? aoeDamage * stance.getDamageMultiplier() : aoeDamage;

        for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, currentSmokeRadius)) {
            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                DamageHandler.damageEntity(le, actualAoEDamage, this);
                Vector push = le.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(aoeKnockback).setY(0.4);
                le.setVelocity(push);
            }
        }
    }

    @Override
    public void remove() {
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
        return "Coil";
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
