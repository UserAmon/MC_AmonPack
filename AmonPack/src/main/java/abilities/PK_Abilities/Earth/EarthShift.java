package Abilities.PK_Abilities.Earth;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

import Plugin.Methods;
import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;

public class EarthShift extends EarthAbility implements AddonAbility {

    private enum State {
        CHARGING, AIMING, PULLING
    }

    private State state;
    private long startTime;
    private long chargeTime = 1500;
    private long cooldown = 6000;
    private double range = 15;
    private double radius = 3;
    private Location targetLoc;

    private Location waveLoc;
    private Vector waveDir;
    private double waveSpeed = 0.8;
    private double distanceTraveled = 0;
    private double totalDistance;

    public EarthShift(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
        boolean hasChunky = (branch != null && branch.hasUpgrade("Chunky"));
        if (hasChunky) {
            this.range = 16.0;
            this.radius = 4.0;
        }

        this.startTime = System.currentTimeMillis();
        this.state = State.CHARGING;
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (state) {
            case CHARGING:
                if (!player.isSneaking()) {
                    remove();
                    return;
                }
                if (System.currentTimeMillis() - startTime > chargeTime) {
                    state = State.AIMING;
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);
                }
                break;

            case AIMING:
                if (!player.isSneaking()) {
                    executePull();
                    return;
                }

                Location targeted = Methods.getTargetLocation(player, (int) range);
                if (targeted.getBlock().getType() != Material.AIR && isEarthbendable(player, targeted.getBlock())) {
                    targetLoc = targeted;
                    displayAimingParticles(targetLoc);
                } else {
                    targetLoc = null;
                }
                break;

            case PULLING:
                progressWave();
                break;
        }
    }

    private void displayAimingParticles(Location center) {
        center.getWorld().spawnParticle(Particle.BLOCK, center.clone().add(0, 1, 0), 1, 0.2, 0.2, 0.2, 0,
                center.getBlock().getType().createBlockData());

        for (Block b : GeneralMethods.getBlocksAroundPoint(center, radius)) {
            if (isEarthbendable(player, b) && GeneralMethods.isSolid(b)
                    && (b.getY() > center.getY() - 2 && b.getY() < center.getY() + 2)) {
                b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0,
                        b.getType().createBlockData());
            }
        }
    }

    private void executePull() {
        if (targetLoc == null) {
            remove();
            return;
        }

        bPlayer.addCooldown(this);
        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.5f);

        RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
        boolean hasChunky = (branch != null && branch.hasUpgrade("Chunky"));
        if (hasChunky) {
            EarthHammer.chunkyHaste.put(player.getUniqueId(), System.currentTimeMillis());
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.5f);

            new org.bukkit.scheduler.BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks += 2;
                    if (ticks > 200 || !player.isOnline() || player.isDead()) {
                        cancel();
                        return;
                    }
                    if (!EarthHammer.chunkyHaste.containsKey(player.getUniqueId()) || 
                        System.currentTimeMillis() - EarthHammer.chunkyHaste.getOrDefault(player.getUniqueId(), 0L) > 10000) {
                        cancel();
                        return;
                    }

                    Location hand = player.getLocation().clone().add(0, 0.9, 0);
                    Vector right = player.getLocation().getDirection().clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(-0.35); // right hand
                    Location handLoc = hand.add(right);

                    for (double d = 0; d <= 0.4; d += 0.15) {
                        Location handlePoint = handLoc.clone().add(player.getLocation().getDirection().multiply(d));
                        player.getWorld().spawnParticle(Particle.BLOCK, handlePoint, 1, 0, 0, 0, 0, org.bukkit.Material.DIRT.createBlockData());
                    }
                    Location headCenter = handLoc.clone().add(player.getLocation().getDirection().multiply(0.4));
                    Vector up = new Vector(0, 1, 0);
                    Vector rightVec = player.getLocation().getDirection().crossProduct(up).normalize();
                    for (double h = -0.15; h <= 0.15; h += 0.1) {
                        player.getWorld().spawnParticle(Particle.BLOCK, headCenter.clone().add(up.clone().multiply(h)), 1, 0, 0, 0, 0, org.bukkit.Material.DIRT.createBlockData());
                        player.getWorld().spawnParticle(Particle.BLOCK, headCenter.clone().add(rightVec.clone().multiply(h)), 1, 0, 0, 0, 0, org.bukkit.Material.DIRT.createBlockData());
                    }
                }
            }.runTaskTimer(AmonPackPlugin.plugin, 0, 2);
        }

        state = State.PULLING;
        waveLoc = targetLoc.clone();
        waveDir = player.getLocation().toVector().subtract(targetLoc.toVector()).normalize();
        totalDistance = targetLoc.distance(player.getLocation());
    }

    private void progressWave() {
        if (distanceTraveled >= totalDistance || waveLoc.distance(player.getLocation()) < 1.5) {
            remove();
            return;
        }

        waveLoc.add(waveDir.clone().multiply(waveSpeed));
        distanceTraveled += waveSpeed;

        for (Block b : GeneralMethods.getBlocksAroundPoint(waveLoc, radius)) {
            if (isEarthbendable(player, b) && GeneralMethods.isSolid(b)
                    && (b.getY() >= waveLoc.getY() - 2 && b.getY() < waveLoc.getY() + 2)) {
                if (Math.random() < 0.3) {
                    Methods.spawnFallingBlocks(b.getLocation(), b.getType(), 1, 0.6, player);
                }

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(b.getLocation(), 1.5)) {
                    if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                        Vector forceDir = waveDir.clone().normalize().multiply(1.2).setY(0.5);
                        entity.setVelocity(forceDir);
                        DamageHandler.damageEntity(entity, 2, this);
                    }
                }
            }
        }
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return targetLoc != null ? targetLoc : player.getLocation();
    }

    @Override
    public String getName() {
        return "EarthShift";
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
    public void load() {
    }

    @Override
    public void stop() {
        remove();
    }

    @Override
    public String getDescription() {
        return "Shifts a section of the earth towards yourself, along with enemies on the path!";
    }

    @Override
    public String getInstructions() {
        return "Shift to charge, look at an earthbendable blocks to aim, release to shift the earth toward yourself.";
    }

}