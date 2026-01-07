package abilities;

import java.util.List;
import java.util.Random;

import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

import methods_plugins.AmonPackPlugin;

public class Whirlpool extends WaterAbility implements AddonAbility {

    private enum State {
        MIST, BLADE
    }

    private State state;
    private long startTime;
    private long mistDuration = 2000;
    private long cooldown = 5000;
    private double mistRadius = 3;
    private Location mistLoc;

    // Blade variables
    private Location bladeLoc;
    private Vector bladeDir;
    private double bladeSpeed = 0.7;
    private double bladeDamage = 3.0;
    private double bladeRange = 20;
    private double distanceTraveled = 0;

    public Whirlpool(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        this.startTime = System.currentTimeMillis();
        this.state = State.MIST;
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (state == State.MIST) {
            if (!player.isSneaking()) {
                remove();
                return;
            }
            if (System.currentTimeMillis() - startTime > mistDuration) {
                shootBlade();
                return;
            }
            updateMist();
        } else if (state == State.BLADE) {
            progressBlade();
        }
    }

    private void updateMist() {
        mistLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3));

        long timeElapsed = System.currentTimeMillis() - startTime;
        double progress = (double) timeElapsed / mistDuration;
        double currentRadius = mistRadius * (1 - progress);

        double angleSpeed = 10 + (progress * 20); // Spin faster as it gets smaller
        double angle = (timeElapsed / 50.0) * angleSpeed;

        for (int i = 0; i < 3; i++) {
            double currentAngle = angle + (i * 120);
            double x = currentRadius * Math.cos(Math.toRadians(currentAngle));
            double z = currentRadius * Math.sin(Math.toRadians(currentAngle));
            Vector dir = player.getLocation().getDirection().normalize();
            Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            if (right.lengthSquared() < 0.01) {
                right = new Vector(1, 0, 0);
            }
            Vector up = right.clone().crossProduct(dir).normalize();

            Vector pPos = right.clone().multiply(x).add(up.clone().multiply(z));

            mistLoc.getWorld().spawnParticle(Particle.CLOUD, mistLoc.clone().add(pPos), 1, 0, 0, 0, 0);
            if (Math.random() < 0.3) {
                mistLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, mistLoc.clone().add(pPos), 1, 0, 0, 0, 0);
            }
        }

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(mistLoc, mistRadius)) {
            if (entity instanceof Projectile) {
                if (!entity.getUniqueId().equals(player.getUniqueId())) {
                    entity.remove();
                    mistLoc.getWorld().spawnParticle(Particle.SPLASH, entity.getLocation(), 10, 0.2, 0.2, 0.2, 0);
                    mistLoc.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1f);
                }
            }
        }
    }

    private void shootBlade() {
        state = State.BLADE;
        bladeLoc = mistLoc.clone();
        bladeDir = player.getLocation().getDirection().normalize();
        bPlayer.addCooldown(this);
        player.playSound(bladeLoc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 1f);
    }

    private void progressBlade() {
        if (distanceTraveled >= bladeRange) {
            remove();
            return;
        }


        bladeLoc.add(bladeDir.clone().multiply(bladeSpeed));
        distanceTraveled += bladeSpeed;

        Vector right = bladeDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = right.clone().crossProduct(bladeDir).normalize();

        for (double i = -1.5; i <= 1.5; i += 0.2) {
            Vector offset = up.clone().multiply(i);
            Vector curve = bladeDir.clone().multiply(-Math.abs(i) * 0.5);
            Location pLoc = bladeLoc.clone().add(offset).add(curve);

            ParticleEffect.BLOCK_CRACK.display(pLoc, 3, 0.3, 0.5, 0.3, 0.01, Material.ICE.createBlockData());
            if(new Random().nextDouble()>0.6){
                TempBlock tb2 = new TempBlock(bladeLoc.getBlock(), Material.BLUE_ICE);
                tb2.setRevertTime(100);
            }
        }
        if (GeneralMethods.isSolid(bladeLoc.getBlock())) {
            remove();
            return;
        }

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(bladeLoc, 1.5)) {
            if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                DamageHandler.damageEntity(entity, bladeDamage, this);
                entity.setVelocity(bladeDir.clone().multiply(1.2).setY(0.5));
                remove();
                return;
            }
        }
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return mistLoc != null ? mistLoc : player.getLocation();
    }

    @Override
    public String getName() {
        return "Whirlpool";
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
