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

        for (int r = 0; r < ringCount; r++) {
            double radius = 1.4 + (r * 0.5);
            double yOffset = (r - 1) * 0.4;

            int points = 16;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i + (r % 2 == 0 ? ringAngle : -ringAngle);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location pt = center.clone().add(x, yOffset + Math.sin(angle) * 0.2, z);

                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pt, 1, 0.01, 0.01, 0.01, 0.01);
                if (i % 4 == 0) {
                    player.getWorld().spawnParticle(Particle.FIREWORK, pt, 1, 0.01, 0.01, 0.01, 0.01);
                }
            }
        }
    }

    private void fire() {
        state = State.FIRED;
        Location eye = player.getEyeLocation();
        Vector baseDir = eye.getDirection().setY(0.02).normalize();

        player.getWorld().playSound(eye, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.6f);

        for (int r = 0; r < ringCount; r++) {
            Vector spreadDir = baseDir.clone().add(new Vector(
                    (Math.random() - 0.5) * 0.8,
                    0.02,
                    (Math.random() - 0.5) * 0.8
            )).normalize().multiply(projectileSpeed);

            spawnGroundLightningProjectile(eye.clone(), spreadDir);
        }

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    private void spawnGroundLightningProjectile(Location startLoc, Vector initialVel) {
        new BukkitRunnable() {
            private Location loc = startLoc.clone();
            private Vector vel = initialVel.clone();
            private int ticks = 0;
            private final Set<UUID> hitSet = new HashSet<>();

            @Override
            public void run() {
                ticks++;
                if (ticks > 50 || player == null || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Higher randomness / drift along ground
                vel.add(new Vector(
                        (Math.random() - 0.5) * randomnessFactor,
                        -0.02,
                        (Math.random() - 0.5) * randomnessFactor
                )).setY(Math.max(-0.15, Math.min(0.08, vel.getY())));

                loc.add(vel);

                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 6, 0.25, 0.25, 0.25, 0.06);
                loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 2, 0.15, 0.15, 0.15, 0.02);

                if (ticks % lightningVisualInterval == 0) {
                    loc.getWorld().strikeLightningEffect(loc);
                }

                // Smoke Source Interaction Check
                SmokeSource nearSource = SmokeAbility.UseSmokeSource(player, smokeRadius);
                if (nearSource != null || checkSmokeSourcesNear(loc)) {
                    triggerSmokeElectrification(loc);
                    cancel();
                    return;
                }

                for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 1.6)) {
                    if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId() && !hitSet.contains(e.getUniqueId())) {
                        hitSet.add(e.getUniqueId());
                        DamageHandler.damageEntity(le, damagePerProjectile, Coil.this);
                        le.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, le.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.1);
                        cancel();
                        return;
                    }
                }

                if (loc.getBlock().getType().isSolid()) {
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private boolean checkSmokeSourcesNear(Location targetLoc) {
        SmokeSource source = SmokeAbility.UseSmokeSource(player, smokeRadius);
        if (source != null) {
            SmokeAbility.DeleteSource(source);
            return true;
        }
        return false;
    }

    private void triggerSmokeElectrification(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().strikeLightningEffect(loc.clone().add(1.5, 0, 1.5));
        loc.getWorld().strikeLightningEffect(loc.clone().add(-1.5, 0, -1.5));

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2, 0.5, 0.5, 0.5, 0.0);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 30, 1.5, 1.5, 1.5, 0.1);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 40, 2.0, 2.0, 2.0, 0.2);

        for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, smokeRadius)) {
            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                DamageHandler.damageEntity(le, aoeDamage, this);
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
