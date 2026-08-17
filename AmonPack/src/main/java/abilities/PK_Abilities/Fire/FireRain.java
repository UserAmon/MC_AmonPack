package Abilities.PK_Abilities.Fire;

import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class FireRain extends FireAbility implements AddonAbility {

    private long cooldown;
    private double baseDamage;
    private double maxRange;
    private double baseRadius;
    private int fireTicks;
    private long multiWindow;

    private boolean hasWide;
    private boolean hasDouble;
    private boolean hasInferno;

    private int maxCasts = 1;
    private int castCount = 0;
    private long lastCastTime = 0;

    private List<ParabolicFireProjectile> projectiles = new ArrayList<>();

    public FireRain(Player player) {
        super(player);

        if (hasAbility(player, FireRain.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfigAndUpgrades();
        start();
        performCast();
    }

    private void loadConfigAndUpgrades() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FireRain.Cooldown", 7000L);
        this.baseDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FireRain.Damage", 5.0);
        this.maxRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FireRain.Range", 20.0);
        this.baseRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FireRain.Radius", 2.5);
        this.fireTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FireRain.FireTicks", 60);
        this.multiWindow = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FireRain.MultiWindow", 3000L);

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null)
                ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName())
                : null;
        this.hasWide = (branch != null && (branch.hasUpgrade("FireRainWide") || branch.hasUpgrade("RainWide")));
        this.hasDouble = (branch != null && (branch.hasUpgrade("DoubleTheFun") || branch.hasUpgrade("DoubleFun")));
        this.hasInferno = (branch != null
                && (branch.hasUpgrade("FireRainInferno") || branch.hasUpgrade("RainInferno")));

        this.maxCasts = hasDouble ? 2 : 1;
    }

    public void onClick() {
        if (castCount < maxCasts) {
            performCast();
        }
    }

    private void performCast() {
        Block targetBlock = player.getTargetBlockExact((int) maxRange);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            Location eye = player.getEyeLocation();
            targetBlock = eye.clone().add(eye.getDirection().multiply(maxRange)).getBlock();
        }

        Location targetCenter = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        castCount++;
        lastCastTime = System.currentTimeMillis();

        boolean isFirelord = FirelordStanceManager.isActive(player);
        double dmg = baseDamage * (isFirelord ? 1.5 : 1.0);
        double rad = (hasWide ? baseRadius * 1.8 : baseRadius) * (isFirelord ? 1.3 : 1.0);

        Location origin = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5));

        if (hasInferno) {
            Vector look = player.getEyeLocation().getDirection().setY(0).normalize();
            Vector side = new Vector(-look.getZ(), 0, look.getX()).normalize();

            Location target1 = targetCenter.clone().add(side.clone().multiply(2.0));
            Location target2 = targetCenter.clone().subtract(side.clone().multiply(2.0));

            projectiles.add(new ParabolicFireProjectile(origin.clone().add(side.clone().multiply(0.8)), target1,
                    dmg * 0.75, rad, isFirelord));
            projectiles.add(new ParabolicFireProjectile(origin.clone().subtract(side.clone().multiply(0.8)), target2,
                    dmg * 0.75, rad, isFirelord));
        } else {
            projectiles.add(new ParabolicFireProjectile(origin, targetCenter, dmg, rad, isFirelord));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.8f);

        if (maxCasts > 1) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§c[FireRain] &e(" + castCount + "/" + maxCasts + ")"));

        }

        if (castCount >= maxCasts) {
            long finalCd = isFirelord ? (long) (cooldown * 0.6) : cooldown;
            bPlayer.addCooldown(this, finalCd);
        }
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (castCount < maxCasts && System.currentTimeMillis() - lastCastTime > multiWindow) {
            boolean isFirelord = FirelordStanceManager.isActive(player);
            long finalCd = isFirelord ? (long) (cooldown * 0.6) : cooldown;
            bPlayer.addCooldown(this, finalCd);
            castCount = maxCasts;
        }

        if (!projectiles.isEmpty()) {
            List<ParabolicFireProjectile> copy = new ArrayList<>(projectiles);
            for (ParabolicFireProjectile proj : copy) {
                proj.progress();
                if (proj.isDead()) {
                    projectiles.remove(proj);
                }
            }
        }

        if (castCount >= maxCasts && projectiles.isEmpty()) {
            remove();
        }
    }

    public class ParabolicFireProjectile {
        private Location startLoc;
        private Location targetLoc;
        private double damage;
        private double radius;
        private boolean isFirelord;

        private int totalTicks = 26;
        private int currentTick = 0;
        private boolean dead = false;

        private double dx, dz;
        private double y0, yt;
        private double apexH;

        public ParabolicFireProjectile(Location start, Location target, double dmg, double rad, boolean firelord) {
            this.startLoc = start.clone();
            this.targetLoc = target.clone();
            this.damage = dmg;
            this.radius = rad;
            this.isFirelord = firelord;

            double dist = startLoc.distance(targetLoc);
            this.totalTicks = Math.max(16, Math.min(36, (int) (dist * 1.5)));

            this.dx = (targetLoc.getX() - startLoc.getX()) / totalTicks;
            this.dz = (targetLoc.getZ() - startLoc.getZ()) / totalTicks;
            this.y0 = startLoc.getY();
            this.yt = targetLoc.getY();
            this.apexH = Math.max(6.0, dist * 0.4);
        }

        public void progress() {
            if (dead)
                return;
            currentTick++;

            double t = (double) currentTick / totalTicks;
            double curX = startLoc.getX() + dx * currentTick;
            double curZ = startLoc.getZ() + dz * currentTick;
            double linearY = y0 + (yt - y0) * t;
            double arcY = 4.0 * apexH * t * (1.0 - t);
            double curY = linearY + arcY;

            Location currentLoc = new Location(startLoc.getWorld(), curX, curY, curZ);

            boolean isBlue = bPlayer.hasElement(com.projectkorra.projectkorra.Element.BLUE_FIRE)
                    || bPlayer.canUseSubElement(com.projectkorra.projectkorra.Element.BLUE_FIRE);

            if (isFirelord) {
                currentLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentLoc, 3, 0.15, 0.15, 0.15, 0.03);
            } else if (isBlue) {
                currentLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, currentLoc, 3, 0.2, 0.2, 0.2, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.SOUL, currentLoc, 1, 0.1, 0.1, 0.1, 0.01);
            } else {
                currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 3, 0.25, 0.25, 0.25, 0.01);
            }

            if (currentTick >= totalTicks || (currentTick > 6 && currentLoc.getBlock().getType().isSolid())) {
                explode(currentLoc);
            }
        }

        private void explode(Location loc) {
            if (dead)
                return;
            dead = true;

            boolean isBlue = bPlayer.hasElement(com.projectkorra.projectkorra.Element.BLUE_FIRE)
                    || bPlayer.canUseSubElement(com.projectkorra.projectkorra.Element.BLUE_FIRE);

            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.1f);
            if (isBlue) {
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 35, radius * 0.6, 0.5, radius * 0.6, 0.08);
                loc.getWorld().spawnParticle(Particle.SOUL, loc, 12, radius * 0.4, 0.4, radius * 0.4, 0.02);
            } else {
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 35, radius * 0.6, 0.5, radius * 0.6, 0.08);
                loc.getWorld().spawnParticle(Particle.LAVA, loc, 8, radius * 0.4, 0.4, radius * 0.4, 0.0);
            }

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, radius)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    DamageHandler.damageEntity(target, damage, FireRain.this);
                    target.setFireTicks(fireTicks);
                    Vector knock = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.5)
                            .setY(0.3);
                    target.setVelocity(knock);
                }
            }

            if (hasWide) {
                for (Block b : GeneralMethods.getBlocksAroundPoint(loc, radius * 0.7)) {
                    if (b.getType() == Material.AIR && b.getRelative(BlockFace.DOWN).getType().isSolid()) {
                        b.setType(isBlue ? Material.SOUL_FIRE : Material.FIRE);
                    }
                }
            }
        }

        public boolean isDead() {
            return dead;
        }
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
        return "FireRain";
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
        remove();
    }
}
