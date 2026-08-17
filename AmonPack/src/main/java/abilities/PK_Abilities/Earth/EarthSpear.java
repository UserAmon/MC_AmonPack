package Abilities.PK_Abilities.Earth;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
    private int lastReportedLevel = 0;

    private double damage;
    private double knockback;
    private int fragmentCount;
    private long cooldown;

    private final List<TempBlock> groundSpearTempBlocks = new ArrayList<>();
    private Location launchLoc;
    private Material spearMaterial = Material.DIRT;

    public EarthSpear(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        Vector initDir = player.getEyeLocation().getDirection().setY(0).normalize();
        if (initDir.lengthSquared() < 0.01) {
            initDir = new Vector(1, 0, 0);
        }
        if (findEarthGround(player.getLocation(), initDir, 3.0) == null) {
            return;
        }

        this.chargeTimePerLevel = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthSpear.ChargeTimePerLevel", 800);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthSpear.Cooldown", 6000);

        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();

        start();
    }

    private Location findEarthGround(Location origin, Vector direction, double dist) {
        Location target = origin.clone().add(direction.clone().setY(0).normalize().multiply(dist));
        for (int y = 3; y >= -4; y--) {
            Block b = target.clone().add(0, y, 0).getBlock();
            if (TempBlock.isTempBlock(b)) continue;
            if (isEarthbendable(player, b) || isEarth(b)) {
                return b.getLocation().add(0.5, 1.0, 0.5);
            }
        }
        return null;
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            revertGroundSpear();
            remove();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                fire();
                return;
            }

            Vector checkDir = player.getEyeLocation().getDirection().setY(0).normalize();
            if (checkDir.lengthSquared() < 0.01) {
                checkDir = new Vector(1, 0, 0);
            }
            if (findEarthGround(player.getLocation(), checkDir, 3.0) == null) {
                revertGroundSpear();
                remove();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            chargeLevel = Math.min(3, (int) (elapsed / chargeTimePerLevel) + 1);

            if (chargeLevel != lastReportedLevel) {
                lastReportedLevel = chargeLevel;
                float pitch = 0.8f + (chargeLevel * 0.3f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 0.8f, pitch);
            }

            // Actionbar progress text like FlameWeave
            String bar;
            if (chargeLevel == 1) {
                bar = "§e[ §6█§7░░ §e] §e§lEARTH SPEAR L1";
            } else if (chargeLevel == 2) {
                bar = "§6[ §e██§7░ §6] §6§lEARTH SPEAR L2";
            } else {
                bar = "§c§l[ §e███ §c§l] §e§lEARTH SPEAR COMPLETE!";
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));

            updateDamageAndKnockback();
            renderGroundSpearRamp();
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

    private void renderGroundSpearRamp() {
        revertGroundSpear();

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().setY(0).normalize();
        if (dir.lengthSquared() < 0.01) {
            dir = new Vector(1, 0, 0);
        }

        // Shifted forward by 1 block (3.0 blocks in front of player) on earthbendable ground
        Location groundLoc = findEarthGround(player.getLocation(), dir, 3.0);
        if (groundLoc == null) {
            launchLoc = null;
            return;
        }

        Block underBlock = groundLoc.clone().subtract(0, 1, 0).getBlock();
        if (isEarthbendable(player, underBlock)) {
            spearMaterial = underBlock.getType().isSolid() ? underBlock.getType() : Material.DIRT;
        } else {
            spearMaterial = Material.DIRT;
        }

        Location baseLoc = groundLoc.clone();
        int rampLength = chargeLevel + 1;
        for (int i = 0; i < rampLength; i++) {
            Location pt = baseLoc.clone().add(dir.clone().multiply(i * 0.8)).add(0, i * 0.5, 0);
            Block b = pt.getBlock();
            if (b.getType() == Material.AIR) {
                groundSpearTempBlocks.add(new TempBlock(b, spearMaterial.createBlockData(), 150));
            }
            pt.getWorld().spawnParticle(Particle.FALLING_DUST, pt, 2, 0.05, 0.05, 0.05, 0.01, spearMaterial.createBlockData());
            launchLoc = pt;
        }
    }

    private void fire() {
        state = State.FIRED;
        revertGroundSpear();

        if (launchLoc == null) {
            remove();
            return;
        }

        Location eye = player.getEyeLocation();
        Location startLoc = launchLoc.clone();
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

                // Projectile visual TempBlock
                Block b = loc.getBlock();
                if (b.getType() == Material.AIR) {
                    projectileTempBlocks.add(new TempBlock(b, spearMaterial.createBlockData(), 150));
                }

                loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 3, 0.1, 0.1, 0.1, 0.02, spearMaterial.createBlockData());

                // PROJECTILE LOGIC AHEAD OF TEMPBLOCKS (checking ahead of motion vector)
                Location checkLoc = loc.clone().add(currentVel.clone().normalize().multiply(1.0));
                for (Entity e : GeneralMethods.getEntitiesAroundPoint(checkLoc, 1.8)) {
                    if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                        DamageHandler.damageEntity(le, damage, EarthSpear.this);
                        Vector push = currentVel.clone().normalize().multiply(knockback).setY(0.3);
                        le.setVelocity(push);
                        shatterSpear(checkLoc);
                        for (TempBlock tb : projectileTempBlocks) tb.revertBlock();
                        cancel();
                        return;
                    }
                }

                Block aheadBlock = checkLoc.getBlock();
                if (aheadBlock.getType().isSolid() && !TempBlock.isTempBlock(aheadBlock)) {
                    shatterSpear(checkLoc);
                    for (TempBlock tb : projectileTempBlocks) tb.revertBlock();
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void shatterSpear(Location loc) {
        loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1.2f, 0.7f);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 25, 0.5, 0.5, 0.5, 0.1, spearMaterial.createBlockData());

        for (int i = 0; i < fragmentCount; i++) {
            Vector fragVel = new Vector(
                    (Math.random() - 0.5) * 0.8,
                    Math.random() * 0.4 + 0.1,
                    (Math.random() - 0.5) * 0.8
            );
            loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 5, fragVel.getX(), fragVel.getY(), fragVel.getZ(), 0.1, spearMaterial.createBlockData());
        }

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    private void revertGroundSpear() {
        for (TempBlock tb : new ArrayList<>(groundSpearTempBlocks)) {
            if (tb != null) {
                tb.revertBlock();
            }
        }
        groundSpearTempBlocks.clear();
    }

    @Override
    public void remove() {
        revertGroundSpear();
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return launchLoc != null ? launchLoc : (player != null ? player.getLocation() : null);
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
        return "1.3";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }
}
