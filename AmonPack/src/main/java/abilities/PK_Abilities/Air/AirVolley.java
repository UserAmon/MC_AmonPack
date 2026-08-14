package Abilities.PK_Abilities.Air;

import Abilities.Bending.SoundAbility;
import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class AirVolley extends AirAbility implements AddonAbility {

    private enum State {
        CHARGING, FIRING
    }

    private State state;
    private long cooldown;
    private long chargeTime;
    private double damage;
    private double speed;
    private double range;
    private double knockback;
    private int baseSegments;
    private int salvoIntervalTicks;
    private double soundStacks;

    private boolean hasBarrage;
    private boolean hasHarmonics;
    private boolean hasPrecision;

    private int totalSegments;
    private int firedShots = 0;
    private int successfulHits = 0;
    private long chargeStartTime = 0;
    private int salvoTimer = 0;

    private List<VolleyProjectile> activeProjectiles = new ArrayList<>();
    private double ringRadius = 1.3;

    public AirVolley(Player player) {
        super(player);

        if (hasAbility(player, AirVolley.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfigAndUpgrades();
        startCharging();
        start();
    }

    private void loadConfigAndUpgrades() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirVolley.Cooldown", 8000L);
        this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirVolley.ChargeTime", 2000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirVolley.Damage", 2.5);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirVolley.Speed", 1.3);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirVolley.Range", 25.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirVolley.Knockback", 0.8);
        this.baseSegments = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Air.AirVolley.Segments", 6);
        this.salvoIntervalTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Air.AirVolley.SalvoIntervalTicks", 3);
        this.soundStacks = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirVolley.SoundStacks", 4.0);

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        this.hasBarrage = (branch != null && (branch.hasUpgrade("AirVolleyBarrage") || branch.hasUpgrade("VolleyBarrage")));
        this.hasHarmonics = (branch != null && (branch.hasUpgrade("AirVolleyHarmonics") || branch.hasUpgrade("VolleyHarmonics")));
        this.hasPrecision = (branch != null && (branch.hasUpgrade("AirVolleyPrecision") || branch.hasUpgrade("VolleyPrecision")));

        this.totalSegments = hasBarrage ? baseSegments + 4 : baseSegments;
    }

    private void startCharging() {
        this.state = State.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();
        this.firedShots = 0;
        this.successfulHits = 0;
        this.salvoTimer = 0;
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.4f);
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (state) {
            case CHARGING:
                if (!player.isSneaking()) {
                    remove();
                    return;
                }

                long elapsed = System.currentTimeMillis() - chargeStartTime;
                double chargeProgress = Math.min(1.0, (double) elapsed / chargeTime);

                renderSegmentedRing(totalSegments, chargeProgress);

                String bar = "§f[AirVolley] Ładowanie salwy: §b" + (int)(chargeProgress * 100) + "%";
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));

                if (elapsed >= chargeTime) {
                    state = State.FIRING;
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.8f);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§b🌪 [AirVolley] Ostrzał rozpoczęty!"));
                }
                break;

            case FIRING:
                salvoTimer++;

                int remainingSegments = totalSegments - firedShots;
                if (remainingSegments > 0) {
                    renderSegmentedRing(remainingSegments, 1.0);
                }

                if (salvoTimer >= salvoIntervalTicks && firedShots < totalSegments) {
                    salvoTimer = 0;
                    fireNextShot();
                }

                if (!activeProjectiles.isEmpty()) {
                    List<VolleyProjectile> copy = new ArrayList<>(activeProjectiles);
                    for (VolleyProjectile proj : copy) {
                        proj.progress();
                        if (proj.isDead()) {
                            activeProjectiles.remove(proj);
                        }
                    }
                }

                if (firedShots >= totalSegments && activeProjectiles.isEmpty()) {
                    if (hasPrecision && successfulHits >= totalSegments && totalSegments > 0) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.5f);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§a⚡ [AirVolley] 100% Celności! Kolejna salwa bez cooldownu!"));
                        startCharging();
                    } else {
                        bPlayer.addCooldown(this, cooldown);
                        remove();
                    }
                }
                break;
        }
    }

    private void fireNextShot() {
        int index = firedShots;
        firedShots++;

        Location spawnLoc = getSegmentLocation(index, totalSegments);
        Vector targetDir = player.getEyeLocation().getDirection().normalize();

        boolean isSound = hasHarmonics && (firedShots % 2 == 0);
        activeProjectiles.add(new VolleyProjectile(spawnLoc, targetDir, isSound));

        if (isSound) {
            player.getWorld().playSound(spawnLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.2f, 1.8f);
        } else {
            player.getWorld().playSound(spawnLoc, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);
        }
    }

    private void renderSegmentedRing(int segmentsToShow, double brightnessRatio) {
        Location eye = player.getEyeLocation();
        Vector lookDir = eye.getDirection().setY(0).normalize();
        if (lookDir.lengthSquared() < 0.01) lookDir = new Vector(0, 0, 1);

        Vector backOffset = lookDir.clone().multiply(-0.7);
        Location ringCenter = eye.clone().add(backOffset).add(0, -0.1, 0);

        Vector right = new Vector(-lookDir.getZ(), 0, lookDir.getX()).normalize();
        Vector up = new Vector(0, 1, 0);

        for (int i = 0; i < segmentsToShow; i++) {
            double angle = (2.0 * Math.PI / totalSegments) * i;
            Vector posOffset = right.clone().multiply(Math.cos(angle) * ringRadius)
                    .add(up.clone().multiply(Math.sin(angle) * ringRadius));
            Location pt = ringCenter.clone().add(posOffset);

            boolean isSoundSegment = hasHarmonics && ((i + 1) % 2 == 0);
            if (isSoundSegment) {
                Particle.DustOptions cyanDust = new Particle.DustOptions(Color.fromRGB(0, 230, 255), (float) (0.8f * brightnessRatio));
                pt.getWorld().spawnParticle(Particle.DUST, pt, 2, 0.05, 0.05, 0.05, 0, cyanDust);
                if (Math.random() < 0.3) {
                    pt.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, pt, 1, 0.02, 0.02, 0.02, 0.0);
                }
            } else {
                Particle.DustOptions whiteDust = new Particle.DustOptions(Color.fromRGB(240, 250, 255), (float) (0.9f * brightnessRatio));
                pt.getWorld().spawnParticle(Particle.DUST, pt, 2, 0.05, 0.05, 0.05, 0, whiteDust);
                pt.getWorld().spawnParticle(Particle.CLOUD, pt, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    private Location getSegmentLocation(int segmentIndex, int total) {
        Location eye = player.getEyeLocation();
        Vector lookDir = eye.getDirection().setY(0).normalize();
        if (lookDir.lengthSquared() < 0.01) lookDir = new Vector(0, 0, 1);

        Vector backOffset = lookDir.clone().multiply(-0.7);
        Location ringCenter = eye.clone().add(backOffset).add(0, -0.1, 0);

        Vector right = new Vector(-lookDir.getZ(), 0, lookDir.getX()).normalize();
        Vector up = new Vector(0, 1, 0);

        double angle = (2.0 * Math.PI / total) * segmentIndex;
        Vector posOffset = right.clone().multiply(Math.cos(angle) * ringRadius)
                .add(up.clone().multiply(Math.sin(angle) * ringRadius));
        return ringCenter.clone().add(posOffset);
    }

    public class VolleyProjectile {
        private Location loc;
        private Vector vel;
        private boolean isSound;
        private double distTraveled = 0;
        private boolean dead = false;
        private boolean scoredHit = false;

        public VolleyProjectile(Location startLoc, Vector targetDir, boolean sound) {
            this.loc = startLoc.clone();
            this.isSound = sound;
            this.vel = targetDir.clone().normalize().multiply(speed);
        }

        public void progress() {
            if (dead) return;

            loc.add(vel);
            distTraveled += speed;

            if (isSound) {
                loc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, loc, 2, 0.08, 0.08, 0.08, 0.02);
                Particle.DustOptions cyanDust = new Particle.DustOptions(Color.fromRGB(0, 220, 255), 1.0f);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, cyanDust);
            } else {
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.06, 0.06, 0.06, 0.01);
                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
            }

            if (distTraveled >= range || loc.getBlock().getType().isSolid()) {
                dead = true;
                return;
            }

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.2)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    dead = true;

                    if (!scoredHit) {
                        scoredHit = true;
                        successfulHits++;
                    }

                    if (isSound) {
                        SoundAbility.HandleDamage(player, target, soundStacks);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 1.4f);
                    } else {
                        DamageHandler.damageEntity(target, damage, AirVolley.this);
                        Vector push = vel.clone().normalize().multiply(knockback).setY(0.25);
                        target.setVelocity(push);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);
                    }
                    return;
                }
            }
        }

        public boolean isDead() {
            return dead;
        }
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
        return "AirVolley";
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
    public String getAuthor() {
        return "AmonPack";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {
        remove();
    }
}
