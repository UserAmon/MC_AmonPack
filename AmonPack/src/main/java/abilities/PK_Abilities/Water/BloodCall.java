package Abilities.PK_Abilities.Water;

import java.util.ArrayList;
import java.util.List;

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
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

import Plugin.AmonPackPlugin;

public class BloodCall extends BloodAbility implements AddonAbility {
    private enum State {
        WEAVING, SUCCESS
    }

    private State state;
    private LivingEntity target;
    private int slot;
    private Vector weaveStartDir;
    private double weaveAngle;
    private long startTime;
    private boolean hurtAt50;
    private boolean hurtAt75;
    private static final double MAX_RANGE = 25.0;
    private static final double FOLLOW_THRESHOLD = 0.97;
    private static final double ANGLE_STEP = 6.0;
    private static final int MAX_DURATION = 120;

    public BloodCall(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }

        this.slot = player.getInventory().getHeldItemSlot();
        this.target = findTarget(player, 6.0);
        if (this.target == null) {
            return;
        }

        this.state = State.WEAVING;
        this.weaveAngle = 0.0;
        this.startTime = System.currentTimeMillis();
        this.weaveStartDir = player.getLocation().getDirection().clone().setY(0).normalize().multiply(1.5);
        if (this.weaveStartDir.lengthSquared() == 0) {
            this.weaveStartDir = new Vector(1, 0, 0);
        }

        start();
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (player.getInventory().getHeldItemSlot() != slot) {
            remove();
            return;
        }

        if (target == null || target.isDead() || target.getLocation().distanceSquared(player.getLocation()) > MAX_RANGE * MAX_RANGE) {
            remove();
            return;
        }

        if (!player.isSneaking()) {
            remove();
            return;
        }

        if (System.currentTimeMillis() - startTime > MAX_DURATION * 50) {
            remove();
            return;
        }

        if (state == State.WEAVING) {
            handleWeaving();
        } else if (state == State.SUCCESS) {
            remove();
        }
    }

    private void handleWeaving() {
        Location targetCenter = target.getLocation().clone().add(0, target.getHeight() * 0.55, 0);
        double progress = Math.min(1.0, weaveAngle / 360.0);

        drawGuidanceRing(targetCenter, progress);
        Location guide = getGuidanceLocation(targetCenter, progress);
        player.spawnParticle(Particle.DUST, guide, 6, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(Color.fromRGB(170, 20, 20), 1.2f));

        if (!hurtAt50 && progress >= 0.5) {
            hurtAt50 = true;
            DamageHandler.damageEntity(player, 1.0, this);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.0f);
        }
        if (!hurtAt75 && progress >= 0.75) {
            hurtAt75 = true;
            DamageHandler.damageEntity(player, 1.0, this);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.0f);
        }

        Vector toGuide = guide.toVector().subtract(player.getEyeLocation().toVector()).normalize();
        double dot = player.getEyeLocation().getDirection().normalize().dot(toGuide);

        if (dot > FOLLOW_THRESHOLD) {
            weaveAngle += ANGLE_STEP;
            if ((int) weaveAngle % 30 == 0) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.3f);
            }
        } else {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("" + org.bukkit.ChatColor.DARK_RED + "Skup się na krążącej krwi"));
        }

        if (weaveAngle >= 360.0) {
            applyBloodLock();
            state = State.SUCCESS;
        }
    }

    private void drawGuidanceRing(Location center, double progress) {
        double height = 1.4;
        double radius = 1.4;
        for (double a = 0; a < 360; a += 20) {
            double rad = Math.toRadians(a);
            Vector offset = rotateY(weaveStartDir, rad);
            Location p = center.clone().add(offset).add(0, height, 0);
            player.spawnParticle(Particle.CRIT, p, 1, 0, 0, 0, 0);
        }
    }

    private Location getGuidanceLocation(Location center, double progress) {
        double path = Math.min(1.0, progress);
        double height;
        double radius;
        double angle;

        if (path < 0.15) {
            double sub = path / 0.15;
            height = 0.6 + 0.8 * sub;
            radius = 0.6 + 0.4 * sub;
            angle = 15.0 * sub;
        } else if (path < 0.85) {
            double sub = (path - 0.15) / 0.7;
            height = 1.4;
            radius = 1.5;
            angle = 360.0 * sub;
        } else {
            double sub = (path - 0.85) / 0.15;
            height = 1.4 - 1.1 * sub;
            radius = 1.5 - 0.8 * sub;
            angle = 360.0;
        }

        Vector ringDir = weaveStartDir.clone().normalize().multiply(radius);
        Location result = center.clone().add(rotateY(ringDir, Math.toRadians(angle))).add(0, height, 0);
        return result;
    }

    private Vector rotateY(Vector input, double angle) {
        double x = input.getX();
        double z = input.getZ();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector(x * cos - z * sin, input.getY(), x * sin + z * cos);
    }

    private LivingEntity findTarget(Player player, double range) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        for (double step = 0.0; step <= range; step += 0.5) {
            Location point = eye.clone().add(direction.clone().multiply(step));
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(point, 1.0)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    if (player.hasLineOfSight(entity)) {
                        return (LivingEntity) entity;
                    }
                }
            }
        }
        return null;
    }

    private void applyBloodLock() {
        if (target == null || target.isDead()) {
            return;
        }

        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.0, 0), 12, 0.3, 1.0, 0.3, 0);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().clone().add(0, 1.0, 0), 40, 1.2, 1.2, 1.2, 0,
                new Particle.DustOptions(Color.fromRGB(160, 0, 0), 1.3f));

        DamageHandler.damageEntity(target, 2.5, this);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10, false, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0, false, false, false));

        new BukkitRunnable() {
            private int counter = 0;

            @Override
            public void run() {
                if (counter++ > 20 || target == null || target.isDead()) {
                    cancel();
                    return;
                }
                target.setVelocity(new Vector(0, 0, 0));
                for (double a = 0; a < 360; a += 30) {
                    double rad = Math.toRadians(a);
                    double x = Math.cos(rad) * 1.0;
                    double z = Math.sin(rad) * 1.0;
                    Location loc = target.getLocation().clone().add(x, 0.6, z);
                    target.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(120, 0, 0), 1.0f));
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0, 1);

        bPlayer.addCooldown(this);
    }

    @Override
    public long getCooldown() {
        return 3000;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "BloodCall";
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
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
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
        return "A bloodbending ritual that binds a nearby opponent. Shift on an enemy, follow the blood particle loop, and trap them in place with bleeding dizziness.";
    }

    @Override
    public String getInstructions() {
        return "Sneak while looking at an enemy to start BloodCall. Follow the moving blood particle around the victim until the ritual completes. Release sneak to cancel.";
    }
}
