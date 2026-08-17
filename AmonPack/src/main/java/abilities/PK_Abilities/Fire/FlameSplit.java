package Abilities.PK_Abilities.Fire;

import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.chat.TextComponent;

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

    private Location startLoc;
    private Vector forward;
    private Vector right;

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
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSplit.Damage", 0.0);
        this.fireTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameSplit.FireTicks", 40);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSplit.Knockback", 0.8);
    }

    private boolean hasParried = false;

    public boolean isParrying() {
        return state == State.CHARGING && !hasParried;
    }

    public void onParryDamage() {
        if (state == State.CHARGING && !hasParried) {
            hasParried = true;
            playParryEffect();
            triggerRelease();
        }
    }

    public void playParryEffect() {
        boolean isBlue = bPlayer.hasElement(com.projectkorra.projectkorra.Element.BLUE_FIRE)
                || bPlayer.canUseSubElement(com.projectkorra.projectkorra.Element.BLUE_FIRE);
        Location eye = player.getEyeLocation();
        player.getWorld().playSound(eye, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
        player.getWorld().playSound(eye, Sound.ENTITY_ITEM_BREAK, 0.8f, 1.5f);
        player.getWorld().spawnParticle(Particle.CRIT,
                eye.clone().add(player.getLocation().getDirection().multiply(0.5)), 8, 0.2, 0.2, 0.2, 0.1);
        if (isBlue) {
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, eye, 6, 0.3, 0.3, 0.3, 0.05);
            player.getWorld().spawnParticle(Particle.FLAME, eye, 6, 0.3, 0.3, 0.3, 0.05);
        } else {
            player.getWorld().spawnParticle(Particle.FLAME, eye, 12, 0.3, 0.3, 0.3, 0.05);
        }
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

        if (FirelordStanceManager.isActive(player)) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§1⚡ Firelord — §cFlameSplit"));
        }

        switch (state) {
            case CHARGING:
                ticksCharging++;
                if (!player.isSneaking() || ticksCharging > 80) {
                    triggerRelease();
                } else {
                    drawRotatingDiscs();
                }
                break;

            case EXTENDING:
                ticksExtending++;
                if (ticksExtending > 7) {
                    state = State.SWEEPING;
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.7f);
                } else {
                    drawExtendingLines();
                }
                break;

            case SWEEPING:
                ticksSweeping++;
                if (ticksSweeping > 15) {
                    remove();
                } else {
                    drawSweepingLines();
                }
                break;
        }
    }

    private void drawRotatingDiscs() {
        boolean isFirelord = FirelordStanceManager.isActive(player);
        boolean isBlue = bPlayer.hasElement(com.projectkorra.projectkorra.Element.BLUE_FIRE)
                || bPlayer.canUseSubElement(com.projectkorra.projectkorra.Element.BLUE_FIRE);

        double angleRad = Math.toRadians(ticksCharging * 15.0);
        Vector curForward = player.getLocation().getDirection().setY(0).normalize();
        if (curForward.lengthSquared() < 0.01) {
            curForward = new Vector(1, 0, 0);
        }
        Vector curRight = curForward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        Location centerLoc = player.getLocation().add(0, 1.0, 0).add(curForward.clone().multiply(1.0));
        double radius = 0.9;

        for (int i = 0; i < 2; i++) {
            double angle = angleRad + Math.toRadians(i * 180.0);
            Vector offset = curRight.clone().multiply(radius * Math.sin(angle))
                    .add(new Vector(0, radius * Math.cos(angle), 0));

            if (isFirelord) {
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, centerLoc.clone().add(offset), 2, 0.02, 0.02,
                        0.02, 0.05);
            } else {
                Particle particle = (isBlue && i == 0) ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
                player.getWorld().spawnParticle(particle, centerLoc.clone().add(offset), 1, 0, 0, 0, 0);
            }
        }

        if (ticksCharging % 5 == 0) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.4f, 1.5f);
        }
    }

    private void triggerRelease() {
        boolean isFirelord = FirelordStanceManager.isActive(player);
        FirelordStance stance = FirelordStanceManager.getStance(player);
        long actualCooldown = isFirelord ? (long) (cooldown * stance.getCooldownMultiplier()) : cooldown;

        bPlayer.addCooldown(this, actualCooldown);
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
        boolean isFirelord = FirelordStanceManager.isActive(player);
        FirelordStance stance = FirelordStanceManager.getStance(player);
        double actualRange = isFirelord ? range * stance.getRangeMultiplier() : range;

        boolean isBlue = bPlayer.hasElement(com.projectkorra.projectkorra.Element.BLUE_FIRE)
                || bPlayer.canUseSubElement(com.projectkorra.projectkorra.Element.BLUE_FIRE);
        Particle leftParticle = isBlue ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
        Particle rightParticle = Particle.FLAME;

        double currentLen = (ticksExtending / 7.0) * actualRange;
        for (double d = 0; d <= currentLen; d += 0.5) {
            Location leftLoc = startLoc.clone().add(forward.clone().multiply(d)).add(right.clone().multiply(-0.3));
            Location rightLoc = startLoc.clone().add(forward.clone().multiply(d)).add(right.clone().multiply(0.3));
            leftLoc.setY(getGroundY(leftLoc) + 1.0);
            rightLoc.setY(getGroundY(rightLoc) + 1.0);

            if (!leftLoc.getBlock().getType().isSolid()) {
                if (isFirelord) {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, leftLoc, 2, 0.05, 0.05, 0.05, 0.05);
                } else {
                    player.getWorld().spawnParticle(leftParticle, leftLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
            if (!rightLoc.getBlock().getType().isSolid()) {
                if (isFirelord) {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, rightLoc, 2, 0.05, 0.05, 0.05, 0.05);
                } else {
                    player.getWorld().spawnParticle(rightParticle, rightLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

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
        boolean isFirelord = FirelordStanceManager.isActive(player);
        FirelordStance stance = FirelordStanceManager.getStance(player);
        double actualRange = isFirelord ? range * stance.getRangeMultiplier() : range;
        double actualDamage = isFirelord ? damage * stance.getDamageMultiplier() : damage;

        boolean isBlue = bPlayer.hasElement(com.projectkorra.projectkorra.Element.BLUE_FIRE)
                || bPlayer.canUseSubElement(com.projectkorra.projectkorra.Element.BLUE_FIRE);
        Particle leftParticle = isBlue ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
        Particle rightParticle = Particle.FLAME;

        double sweepProgress = ticksSweeping / 15.0;
        double yOffset = 1.0 + (0.75 * sweepProgress);
        for (double d = 0; d <= actualRange; d += 0.5) {
            double currentSideOffset = 0.3 + (d * 0.65) * sweepProgress;
            Location leftLoc = startLoc.clone().add(forward.clone().multiply(d))
                    .add(right.clone().multiply(-currentSideOffset));
            Location rightLoc = startLoc.clone().add(forward.clone().multiply(d))
                    .add(right.clone().multiply(currentSideOffset));
            leftLoc.setY(getGroundY(leftLoc) + yOffset);
            rightLoc.setY(getGroundY(rightLoc) + yOffset);

            if (!leftLoc.getBlock().getType().isSolid()) {
                if (isFirelord) {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, leftLoc, 3, 0.1, 0.1, 0.1, 0.05);
                } else {
                    player.getWorld().spawnParticle(leftParticle, leftLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }
                player.getWorld().spawnParticle(Particle.SMOKE, leftLoc, 1, 0.05, 0.05, 0.05, 0.01);
                Vector leftPushDir = right.clone().multiply(-1.0);
                checkDamageAtLocation(leftLoc, leftPushDir, actualDamage);
            }

            if (!rightLoc.getBlock().getType().isSolid()) {
                if (isFirelord) {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, rightLoc, 3, 0.1, 0.1, 0.1, 0.05);
                } else {
                    player.getWorld().spawnParticle(rightParticle, rightLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }
                player.getWorld().spawnParticle(Particle.SMOKE, rightLoc, 1, 0.05, 0.05, 0.05, 0.01);
                Vector rightPushDir = right.clone();
                checkDamageAtLocation(rightLoc, rightPushDir, actualDamage);
            }
        }

        if (ticksSweeping % 3 == 0) {
            player.getWorld().playSound(startLoc, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 1.0f);
        }
    }

    private void checkDamageAtLocation(Location loc, Vector knockbackDir, double currentDamage) {
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1)) {
            if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                LivingEntity target = (LivingEntity) entity;
                if (!hitEntities.contains(target)) {
                    hitEntities.add(target);
                    DamageHandler.damageEntity(target, currentDamage, this);
                    target.setFireTicks(fireTicks);
                    target.setVelocity(knockbackDir.clone().normalize().multiply(knockback).setY(0.25));
                }
            }
        }
    }

    private double getGroundY(Location loc) {
        Location check = loc.clone();
        for (int i = 0; i < 5; i++) {
            if (check.getBlock().getType().isSolid()) {
                return check.getY();
            }
            check.add(0, -1, 0);
        }
        return loc.getY();
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
        return "FlameSplit";
    }

    @Override
    public String getAuthor() {
        return "AmonPack";
    }

    @Override
    public String getVersion() {
        return "2.0";
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
        return "Blokuje uderzenie i wypuszcza dwa ogniste cięcia na boki.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj Shift aby sparować uderzenie i wypuścić fala ognia!";
    }
}
