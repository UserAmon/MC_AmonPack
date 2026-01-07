package abilities;

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

import methods_plugins.Methods;

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
            if (isEarthbendable(player, b) && GeneralMethods.isSolid(b) && (b.getY() > center.getY() - 2 && b.getY()< center.getY()+2)) {
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
}
