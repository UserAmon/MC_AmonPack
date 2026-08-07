package Abilities.PK_Abilities.Fire;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class FlameSplit extends FireAbility implements AddonAbility {

    private enum State {
        CHARGING, EXTENDING, SWEEPING
    }

    private State state;
    private long startTime;
    private int slot;
    private int ticksCharging = 0;
    private int ticksExtending = 0;
    private int ticksSweeping = 0;

    // Direction and location vectors locked upon shift release
    private Location startLoc;
    private Vector forward;
    private Vector right;

    // Config variables
    private long cooldown;
    private double range;
    private double damage;
    private int fireTicks;
    private double knockback;

    private Set<Entity> hitEntities = new HashSet<>();

    public FlameSplit(Player player) {
        super(player);
        loadConfig();

        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }

        this.slot = player.getInventory().getHeldItemSlot();
        this.startTime = System.currentTimeMillis();
        this.state = State.CHARGING;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5f, 1.2f);
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FlameSplit.Cooldown", 5000);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSplit.Range", 6.0);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSplit.Damage", 0.0); // Default to 0.0 per "Damage jest niwelony"
        this.fireTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameSplit.FireTicks", 40);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSplit.Knockback", 0.8);
    }

    public boolean isParrying() {
        return state == State.CHARGING;
    }

    public void onParryDamage() {
        if (state == State.CHARGING) {
            playParryEffect();
            triggerRelease();
        }
    }

    public void playParryEffect() {
        Location eye = player.getEyeLocation();
        player.getWorld().playSound(eye, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
        player.getWorld().playSound(eye, Sound.ENTITY_ITEM_BREAK, 0.8f, 1.5f);
        player.getWorld().spawnParticle(Particle.FLASH, eye.add(player.getLocation().getDirection().multiply(0.5)), 3, 0.1, 0.1, 0.1, 0);
        player.getWorld().spawnParticle(Particle.FLAME, eye, 10, 0.3, 0.3, 0.3, 0.05);
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (state == State.CHARGING && player.getInventory().getHeldItemSlot() != slot) {
            remove();
            return;
        }

        switch (state) {
            case CHARGING:
                ticksCharging++;
                if (!player.isSneaking() || ticksCharging > 80) {
                    // Release shift or auto-release at 4 seconds
                    triggerRelease();
                } else {
                    drawRotatingDiscs();
                }
                break;

            case EXTENDING:
                ticksExtending++;
                if (ticksExtending > 7) { // 0.35s duration (snappier)
                    state = State.SWEEPING;
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.7f);
                } else {
                    drawExtendingLines();
                }
                break;

            case SWEEPING:
                ticksSweeping++;
                if (ticksSweeping > 15) { // 0.75s duration (snappier)
                    remove();
                } else {
                    drawSweepingLines();
                }
                break;
        }
    }

    private void drawRotatingDiscs() {
        boolean isBlue = bPlayer.hasElement(com.projectkorra.projectkorra.Element.BLUE_FIRE) || bPlayer.canUseSubElement(com.projectkorra.projectkorra.Element.BLUE_FIRE);
        Particle flameParticle = isBlue ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;

        double angleRad = Math.toRadians(ticksCharging * 15.0);
        Vector curForward = player.getLocation().getDirection().setY(0).normalize();
        if (curForward.lengthSquared() < 0.01) {
            curForward = new Vector(1, 0, 0);
        }
        Vector curRight = curForward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        // One single vertical circle in the center
        Location centerLoc = player.getLocation().add(0, 1.0, 0).add(curForward.clone().multiply(1.0));
        double radius = 0.9; // Larger radius

        for (int i = 0; i < 2; i++) { // Two rotating fire points
            double angle = angleRad + Math.toRadians(i * 180.0);
            Vector offset = curRight.clone().multiply(radius * Math.sin(angle)).add(new Vector(0, radius * Math.cos(angle), 0));

            player.getWorld().spawnParticle(flameParticle, centerLoc.clone().add(offset), 1, 0, 0, 0, 0);
        }

        if (ticksCharging % 5 == 0) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.4f, 1.5f);
        }
    }

    private void triggerRelease() {
        bPlayer.addCooldown(this);
        this.state = State.EXTENDING;
        this.startLoc = player.getLocation().clone();
        this.forward = player.getLocation().getDirection().setY(0).normalize();
        if (this.forward.lengthSquared() < 0.01) {
            this.forward = new Vector(1, 0, 0);
        }
        this.right = this.forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.9f);
    }

    private void drawExtendingLines() {
        double currentLen = (ticksExtending / 7.0) * range;
        for (double d = 0; d <= currentLen; d += 0.5) {
            Location leftLoc = startLoc.clone().add(forward.clone().multiply(d)).add(right.clone().multiply(-0.3));
            Location rightLoc = startLoc.clone().add(forward.clone().multiply(d)).add(right.clone().multiply(0.3));
            leftLoc.setY(getGroundY(leftLoc) + 1.0);
            rightLoc.setY(getGroundY(rightLoc) + 1.0);

            if (!leftLoc.getBlock().getType().isSolid()) {
                player.getWorld().spawnParticle(Particle.FLAME, leftLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }
            if (!rightLoc.getBlock().getType().isSolid()) {
                player.getWorld().spawnParticle(Particle.FLAME, rightLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Particle spark at the tip of the expanding line
        Location leftTip = startLoc.clone().add(forward.clone().multiply(currentLen)).add(right.clone().multiply(-0.3));
        Location rightTip = startLoc.clone().add(forward.clone().multiply(currentLen)).add(right.clone().multiply(0.3));
        leftTip.setY(getGroundY(leftTip) + 1.0);
        rightTip.setY(getGroundY(rightTip) + 1.0);
        
        if (!leftTip.getBlock().getType().isSolid()) {
            player.getWorld().spawnParticle(Particle.SMOKE, leftTip, 2, 0.1, 0.1, 0.1, 0.01);
        }
        if (!rightTip.getBlock().getType().isSolid()) {
            player.getWorld().spawnParticle(Particle.SMOKE, rightTip, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private void drawSweepingLines() {
        double sweepProgress = ticksSweeping / 15.0;
        double yOffset = 1.0 + (0.75 * sweepProgress);
        for (double d = 0; d <= range; d += 0.5) {
            // Sweeping sideways offset
            double currentSideOffset = 0.3 + (d * 0.65) * sweepProgress;
            Location leftLoc = startLoc.clone().add(forward.clone().multiply(d)).add(right.clone().multiply(-currentSideOffset));
            Location rightLoc = startLoc.clone().add(forward.clone().multiply(d)).add(right.clone().multiply(currentSideOffset));
            leftLoc.setY(getGroundY(leftLoc) + yOffset);
            rightLoc.setY(getGroundY(rightLoc) + yOffset);

            if (!leftLoc.getBlock().getType().isSolid()) {
                player.getWorld().spawnParticle(Particle.FLAME, leftLoc, 2, 0.1, 0.1, 0.1, 0.02);
                player.getWorld().spawnParticle(Particle.SMOKE, leftLoc, 1, 0.05, 0.05, 0.05, 0.01);
                Vector leftPushDir = right.clone().multiply(-1.0);
                checkDamageAtLocation(leftLoc, leftPushDir);
            }

            if (!rightLoc.getBlock().getType().isSolid()) {
                player.getWorld().spawnParticle(Particle.FLAME, rightLoc, 2, 0.1, 0.1, 0.1, 0.02);
                player.getWorld().spawnParticle(Particle.SMOKE, rightLoc, 1, 0.05, 0.05, 0.05, 0.01);
                Vector rightPushDir = right.clone();
                checkDamageAtLocation(rightLoc, rightPushDir);
            }
        }

        if (ticksSweeping % 3 == 0) {
            player.getWorld().playSound(startLoc, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 1.0f);
        }
    }

    private void checkDamageAtLocation(Location loc, Vector knockbackDir) {
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.4)) {
            if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                if (!hitEntities.contains(entity)) {
                    hitEntities.add(entity);
                    LivingEntity le = (LivingEntity) entity;

                    // Nullified damage (deals 0.0 damage, parries only)
                    DamageHandler.damageEntity(le, damage, this);
                    le.setFireTicks(fireTicks);

                    // Push sideways
                    Vector velocity = knockbackDir.clone().normalize().multiply(knockback).setY(0.22);
                    le.setVelocity(velocity);

                    le.getWorld().playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 1.0f);
                }
            }
        }
    }

    private double getGroundY(Location loc) {
        Location check = loc.clone();
        for (int dy = 2; dy >= -4; dy--) {
            Block b = check.clone().add(0, dy, 0).getBlock();
            if (b.getType().isSolid()) {
                return b.getY() + 1.0;
            }
        }
        return loc.getY();
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
        return "FlameSplit";
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
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Hold Sneak to charge and parry incoming damage with rotating fire shields. Release Sneak to stretch flame lines along the ground and sweep them sideways to knockback and ignite enemies.";
    }

    @Override
    public String getInstructions() {
        return "Hold Sneak to charge fire shields. Release Sneak to sweep flames.";
    }
}
