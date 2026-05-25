package Abilities.PK_Abilities.Water;

import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import Plugin.Methods;

public class Whirlpool extends WaterAbility implements AddonAbility {

    private enum State {
        GATHERING, SHIELD, SHOOT
    }

    private State state;
    private long startTime;

    private long gatheringDuration = 4000;
    private double damage = 4.0;
    private double speed = 1.5;
    private double range = 30;
    private long cooldown = 6000;

    private Location projectileLoc;
    private Vector projectileDir;
    private double distanceTraveled = 0;

    public Whirlpool(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }
        Location source = Methods.findWaterSource(player, 15);
        if (source == null) {
            return;
        }
        this.startTime = System.currentTimeMillis();
        this.state = State.GATHERING;
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (state) {
            case GATHERING:
                if (!player.isSneaking()) {
                    remove();
                    return;
                }
                if (System.currentTimeMillis() - startTime > gatheringDuration) {
                    state = State.SHIELD;
                    return;
                }
                progressGathering();
                break;
            case SHIELD:
                if (!player.isSneaking()) {
                    startShoot();
                    return;
                }
                progressShield();
                break;
            case SHOOT:
                progressShoot();
                break;
        }
    }

    private void progressGathering() {
        long timeElapsed = System.currentTimeMillis() - startTime;
        Location shieldLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2));

        for (int i = 0; i < 2; i++) {
            Location spawnLoc = shieldLoc.clone()
                    .add(Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).multiply(5));
            Vector dir = shieldLoc.toVector().subtract(spawnLoc.toVector()).normalize();
            player.getWorld().spawnParticle(Particle.CLOUD, spawnLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.5);
            if (Math.random() < 0.3) {
                player.getWorld().spawnParticle(Particle.SPLASH, spawnLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.5);
            }
        }

        if (timeElapsed > 2000) {
            double progress = (timeElapsed - 2000) / 2000.0;
            drawShield(shieldLoc, progress);
        }
    }

    private void progressShield() {
        Location shieldLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3));
        drawShield(shieldLoc, 1.0);

        for (Entity e : GeneralMethods.getEntitiesAroundPoint(shieldLoc, 2)) {
            if (e instanceof Projectile && !e.getUniqueId().equals(player.getUniqueId())) {
                e.setVelocity(e.getVelocity().multiply(-0.5));
                ParticleEffect.WATER_SPLASH.display(e.getLocation(), 5, 0.1, 0.1, 0.1, 0.1);
            }
        }
    }

    private void drawShield(Location shieldLoc, double progress) {
        Vector dir = player.getEyeLocation().getDirection().multiply(2).normalize();
        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = right.clone().crossProduct(dir).normalize();
        Material mat = Material.WATER;

        double maxR = 1 * progress;
        if (progress > 0 && maxR < 0.5)
            maxR = 0.5;

        for (double r = 0.5; r <= 1.5; r += 0.5) {
            if (r > maxR)
                continue;

            for (double theta = 0; theta < 360; theta += 45) {
                double x = r * Math.cos(Math.toRadians(theta));
                double y = r * Math.sin(Math.toRadians(theta));

                Vector offset = right.clone().multiply(x).add(up.clone().multiply(y));
                Location pLoc = shieldLoc.clone().add(offset);

                Block b = pLoc.getBlock();
                if (isTransparent(b)) {
                    new TempBlock(b, mat).setRevertTime(100);
                }
            }
        }
    }

    private void startShoot() {
        state = State.SHOOT;
        projectileLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2));
        projectileDir = player.getEyeLocation().getDirection().normalize();
        bPlayer.addCooldown(this);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 1f);
    }

    private void progressShoot() {
        if (distanceTraveled > range) {
            remove();
            return;
        }

        projectileLoc.add(projectileDir.clone().multiply(speed));
        distanceTraveled += speed;

        if ((!isTransparent(projectileLoc.getBlock()) && !projectileLoc.getBlock().isPassable())
                || projectileLoc.getBlock().getType().isSolid()) {
            createIceSpike(projectileLoc);
            remove();
            return;
        }

        new TempBlock(projectileLoc.getBlock(), Material.WATER).setRevertTime(100);
        ParticleEffect.WATER_SPLASH.display(projectileLoc, 5, 0.5, 0.5, 0.5, 0.1);

        for (Entity e : GeneralMethods.getEntitiesAroundPoint(projectileLoc, 1.5)) {
            if (e instanceof LivingEntity && e.getUniqueId() != player.getUniqueId()) {
                DamageHandler.damageEntity(e, damage, this);
                e.setVelocity(projectileDir.clone().multiply(1.2).setY(0.5));
                remove();
                return;
            }
        }
    }

    private void createIceSpike(Location loc) {
        Vector dir = projectileDir.clone().normalize();

        for (int i = 0; i < 4; i++) {
            Location spikeLoc = loc.clone().add(dir.clone().multiply(i));
            Block b = spikeLoc.getBlock();
            if (isTransparent(b) || !b.getType().isSolid()) {
                new TempBlock(b, Material.BLUE_ICE).setRevertTime(5000);
                ParticleEffect.BLOCK_CRACK.display(spikeLoc, 10, 0.5, 0.5, 0.5, 0.1,
                        Material.BLUE_ICE.createBlockData());
            }
        }

        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = right.clone().crossProduct(dir).normalize();

        for (int i = 0; i < 4; i++) {
            double angle = i * 90;
            double x = Math.cos(Math.toRadians(angle));
            double y = Math.sin(Math.toRadians(angle));

            Vector offset = right.clone().multiply(x).add(up.clone().multiply(y));
            Location sideLoc = loc.clone().add(offset).add(dir.clone().multiply(1));

            Block b = sideLoc.getBlock();
            if (isTransparent(b) || !b.getType().isSolid()) {
                new TempBlock(b, Material.PACKED_ICE).setRevertTime(5000);
            }

            Location sideLoc2 = sideLoc.clone().add(dir.clone().multiply(1));
            Block b2 = sideLoc2.getBlock();
            if (isTransparent(b2) || !b2.getType().isSolid()) {
                new TempBlock(b2, Material.ICE).setRevertTime(5000);
            }
        }

        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return projectileLoc != null ? projectileLoc : player.getLocation();
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

    @Override
    public String getDescription() {
        return "Drag moisture from the air to form a whirpool around you. After a while - you will form a shield that can be launched!";
    }

    @Override
    public String getInstructions() {
        return "Sneak while in water to form a whirlpool of moisture around you. After a while - you will form a shield. Release shift to launch the shild forward.";
    }

}