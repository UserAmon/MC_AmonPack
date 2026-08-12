package Abilities.PK_Abilities.Earth;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class EarthSpear extends EarthAbility implements AddonAbility {

    private enum State { CHARGING, FIRED }

    private State state;
    private long startTime;
    private long chargeTimePerLevel;
    private int chargeLevel = 1;

    private double damage;
    private double knockback;
    private int fragmentCount;
    private long cooldown;

    private final List<TempBlock> spearTempBlocks = new ArrayList<>();
    private Location spearCenterLoc;

    public EarthSpear(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.chargeTimePerLevel = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthSpear.ChargeTimePerLevel", 800);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthSpear.Cooldown", 6000);

        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();

        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            revertSpearBlocks();
            remove();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                fire();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            chargeLevel = Math.min(3, (int) (elapsed / chargeTimePerLevel) + 1);

            updateDamageAndKnockback();
            renderFloatingSpear();
        }
    }

    private void updateDamageAndKnockback() {
        switch (chargeLevel) {
            case 1:
                damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthSpear.DamageLevel1", 4.0);
                knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthSpear.KnockbackLevel1", 0.8);
                fragmentCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthSpear.FragmentCountLevel1", 3);
                break;
            case 2:
                damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthSpear.DamageLevel2", 7.0);
                knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthSpear.KnockbackLevel2", 1.4);
                fragmentCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthSpear.FragmentCountLevel2", 5);
                break;
            case 3:
            default:
                damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthSpear.DamageLevel3", 10.0);
                knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthSpear.KnockbackLevel3", 2.0);
                fragmentCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthSpear.FragmentCountLevel3", 8);
                break;
        }
    }

    private void renderFloatingSpear() {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        // Position spear high above player's head (+2.2 Y) and 1.0 block forward so head doesn't collide
        spearCenterLoc = eye.clone().add(dir.clone().multiply(1.0)).add(0, 2.2, 0);

        int lengthBlocks = chargeLevel + 1;
        for (int i = 0; i < lengthBlocks; i++) {
            Location pt = spearCenterLoc.clone().add(dir.clone().multiply((i - (lengthBlocks / 2.0)) * 0.8));
            Block b = pt.getBlock();
            if (b.getType() == Material.AIR) {
                spearTempBlocks.add(new TempBlock(b, Material.DIRT.createBlockData(), 150));
            }
            pt.getWorld().spawnParticle(Particle.FALLING_DUST, pt, 1, 0.05, 0.05, 0.05, 0.01, Material.DIRT.createBlockData());
        }

        // Gathering animation from ground
        if (System.currentTimeMillis() % 400 < 50) {
            Location groundPt = player.getLocation().add((Math.random() - 0.5) * 3, -0.5, (Math.random() - 0.5) * 3);
            player.getWorld().spawnParticle(Particle.FALLING_DUST, groundPt, 3, 0.2, 0.2, 0.2, 0.1, Material.DIRT.createBlockData());
            player.getWorld().playSound(spearCenterLoc, Sound.BLOCK_GRAVEL_STEP, 0.5f, 1.0f + (chargeLevel * 0.2f));
        }
    }

    private void fire() {
        state = State.FIRED;
        revertSpearBlocks();

        Location eye = player.getEyeLocation();
        Location startLoc = spearCenterLoc != null ? spearCenterLoc : eye.clone().add(eye.getDirection().multiply(1.0)).add(0, 2.2, 0);
        Vector vel = eye.getDirection().normalize().multiply(1.5);

        player.getWorld().playSound(startLoc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.6f);

        new BukkitRunnable() {
            private Location loc = startLoc.clone();
            private Vector currentVel = vel.clone();
            private int ticks = 0;
            private final List<TempBlock> projectileTempBlocks = new ArrayList<>();

            @Override
            public void run() {
                ticks++;
                if (ticks > 40 || player == null || !player.isOnline()) {
                    for (TempBlock tb : projectileTempBlocks) tb.revertBlock();
                    bPlayer.addCooldown(EarthSpear.this, cooldown);
                    remove();
                    cancel();
                    return;
                }

                currentVel.add(new Vector(0, -0.03, 0));
                loc.add(currentVel);

                Block b = loc.getBlock();
                if (b.getType() == Material.AIR) {
                    projectileTempBlocks.add(new TempBlock(b, Material.DIRT.createBlockData(), 150));
                }

                loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 3, 0.1, 0.1, 0.1, 0.02, Material.DIRT.createBlockData());

                for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 1.6)) {
                    if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                        DamageHandler.damageEntity(le, damage, EarthSpear.this);
                        Vector push = currentVel.clone().normalize().multiply(knockback).setY(0.3);
                        le.setVelocity(push);
                        shatterSpear(loc);
                        for (TempBlock tb : projectileTempBlocks) tb.revertBlock();
                        cancel();
                        return;
                    }
                }

                if (loc.getBlock().getType().isSolid()) {
                    shatterSpear(loc);
                    for (TempBlock tb : projectileTempBlocks) tb.revertBlock();
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void shatterSpear(Location loc) {
        loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1.2f, 0.7f);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 25, 0.5, 0.5, 0.5, 0.1, Material.DIRT.createBlockData());

        for (int i = 0; i < fragmentCount; i++) {
            Vector fragVel = new Vector(
                    (Math.random() - 0.5) * 0.8,
                    Math.random() * 0.4 + 0.1,
                    (Math.random() - 0.5) * 0.8
            );
            loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 5, fragVel.getX(), fragVel.getY(), fragVel.getZ(), 0.1, Material.DIRT.createBlockData());
        }

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    private void revertSpearBlocks() {
        for (TempBlock tb : new ArrayList<>(spearTempBlocks)) {
            if (tb != null) {
                tb.revertBlock();
            }
        }
        spearTempBlocks.clear();
    }

    @Override
    public void remove() {
        revertSpearBlocks();
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return spearCenterLoc != null ? spearCenterLoc : (player != null ? player.getLocation() : null);
    }

    @Override
    public String getName() {
        return "EarthSpear";
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public String getAuthor() {
        return "AmonPack";
    }

    @Override
    public String getVersion() {
        return "1.2";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }
}
