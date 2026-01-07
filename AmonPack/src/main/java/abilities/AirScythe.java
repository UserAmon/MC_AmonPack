package abilities;

import java.util.List;
import java.util.Random;

import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

import methods_plugins.AmonPackPlugin;

public class AirScythe extends AirAbility implements AddonAbility {

    private enum State {
        READY, SHOT_1, SHOT_2, COOLDOWN
    }

    private State state;
    private long lastFireTime;
    private long comboWindow = 3000;
    private long cooldown = 4000;
    private double damage = 2.0;
    private double speed = 1.5;
    private double range = 20;

    public AirScythe(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }
        if (hasAbility(player, AirScythe.class)) {
            AirScythe existing = getAbility(player, AirScythe.class);
            if (existing.state == State.SHOT_1) {
                existing.fireSecondShot();
                return;
            } else if (existing.state == State.SHOT_2) {
                return;
            }
        }
            this.state = State.READY;
            start();
            fireFirstShot();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (state == State.SHOT_1 || state == State.SHOT_2) {
            if (System.currentTimeMillis() - lastFireTime > comboWindow) {
                bPlayer.addCooldown(this);
                remove();
            }
        } else if (state == State.COOLDOWN) {
            remove();
        }
    }

    public void onShift() {
        if (state == State.SHOT_2) {
            fireThirdShot();
        }
    }

    private void fireFirstShot() {
        state = State.SHOT_1;
        lastFireTime = System.currentTimeMillis();
        fireHorizontalShot();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);
    }

    public void fireSecondShot() {
        state = State.SHOT_2;
        lastFireTime = System.currentTimeMillis();
        fireHorizontalShot();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.8f);
    }

    public void fireThirdShot() {
        state = State.COOLDOWN;
        bPlayer.addCooldown(this);

        Vector kickback = player.getLocation().getDirection().normalize().clone().multiply(-1).setY(0.5);
        player.setVelocity(kickback);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 2f);

        fireVerticalShot();
    }

    private void fireHorizontalShot() {
        Location origin = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection().normalize();

        new BukkitRunnable() {
            Location loc = origin.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > range / speed) {
                    this.cancel();
                    return;
                }

                loc.add(direction.clone().multiply(speed));

                Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                for (double i = -1.5; i <= 1.5; i += 0.2) {
                    Vector offset = right.clone().multiply(i);
                    Vector curve = direction.clone().multiply(-Math.abs(i) * 0.5);
                    Location pLoc = loc.clone().add(offset).add(curve);
                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.GRAY, 0.75f);
                    player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0.1, 0.1, 0.1, 0, dustOptions);
                    player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.WHITE, 0.75f));
                }

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.5)) {
                    if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                        DamageHandler.damageEntity(entity, damage, AirScythe.this);
                        entity.setVelocity(direction.clone().multiply(1));
                        this.cancel();
                        return;
                    }
                }
                if (loc.getBlock().getType().isSolid()) {
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
    }

    private void fireVerticalShot() {
        Location origin = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection().normalize();

        new BukkitRunnable() {
            Location loc = origin.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > range / speed) {
                    this.cancel();
                    return;
                }

                loc.add(direction.clone().multiply(speed));

                Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                Vector up = right.clone().crossProduct(direction).normalize();

                for (double i = -1.5; i <= 1.5; i += 0.2) {
                    Vector offset = up.clone().multiply(i);
                    Vector curve = direction.clone().multiply(-Math.abs(i) * 0.5);
                    Location pLoc = loc.clone().add(offset).add(curve);
                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.GRAY, 0.75f);
                    player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0.1, 0.1, 0.1, 0, dustOptions);
                    player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.WHITE, 0.75f));
                }

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.5)) {
                    if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                        DamageHandler.damageEntity(entity, damage * 2.0, AirScythe.this);
                        entity.setVelocity(direction.clone().multiply(0.5).setY(0.7));
                        this.cancel();
                        return;
                    }
                }
                if (loc.getBlock().getType().isSolid()) {
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
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
        return "AirScythe";
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
    }
}
