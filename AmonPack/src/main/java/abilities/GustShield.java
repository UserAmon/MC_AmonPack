package abilities;

import java.util.ArrayList;
import java.util.List;

import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

import methods_plugins.AmonPackPlugin;

public class GustShield extends AirAbility implements AddonAbility {

    private enum State {
        SHIELDING, LAUNCHED
    }

    private State state;
    private long startTime;
    private long duration = 2000;
    private long cooldown = 6000;
    private double speed = 0.7;
    private double range = 15;
    private double pushFactor = 1;

    private Location shieldLoc;
    private Vector shieldDir;

    public GustShield(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }

        bPlayer.addCooldown(this);
        this.state = State.SHIELDING;
        this.startTime = System.currentTimeMillis();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 1, false, false));
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (state) {
            case SHIELDING:
                if (System.currentTimeMillis() - startTime > duration) {
                    remove();
                    return;
                }

                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize();
                shieldLoc = eye.clone().add(dir.clone().multiply(2.5));
                shieldDir = dir;
                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.GRAY, 0.75f);

                for (double i = 0; i < 360; i += 20) {
                    Vector offset = GeneralMethods.getOrthogonalVector(dir, i, 1.35);
                    player.getWorld().spawnParticle(Particle.DUST, shieldLoc.clone().add(offset), 1, 0.1, 0.1, 0.1, 0, dustOptions);
                    player.getWorld().spawnParticle(Particle.DUST, shieldLoc.clone().add(offset), 1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.WHITE, 0.75f));
                }
                for (double i = 0; i < 360; i += 40) {
                    Vector offset = GeneralMethods.getOrthogonalVector(dir, i, 0.7);
                    player.getWorld().spawnParticle(Particle.DUST, shieldLoc.clone().add(offset), 1, 0.1, 0.1, 0.1, 0, dustOptions);
                    player.getWorld().spawnParticle(Particle.DUST, shieldLoc.clone().add(offset), 1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.WHITE, 0.75f));
                }
                break;

            case LAUNCHED:
                if (shieldLoc == null) {
                    remove();
                    return;
                }

                shieldLoc.add(shieldDir.clone().multiply(speed));

                for (double i = 0; i < 360; i += 20) {
                    Vector offset = GeneralMethods.getOrthogonalVector(shieldDir, i, 1);
                    playAirbendingParticles(shieldLoc.clone().add(offset),1);
                }

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(shieldLoc, 1.25)) {
                    if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                        Vector knockback = shieldDir.clone().multiply(pushFactor).setY(0.5);
                        entity.setVelocity(knockback);
                        DamageHandler.damageEntity(entity, 2, this);
                    }
                }
                if (shieldLoc.getBlock().getType().isSolid()
                        || shieldLoc.distanceSquared(player.getEyeLocation()) > range * range) {
                    remove();
                    return;
                }
                break;
        }
    }

    public void onHit() {
        if (state == State.SHIELDING) {
            state = State.LAUNCHED;
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 2f);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.DARKNESS);
        }
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "GustShield";
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
    }
}
