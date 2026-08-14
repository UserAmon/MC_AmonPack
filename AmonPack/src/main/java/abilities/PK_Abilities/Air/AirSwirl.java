package Abilities.PK_Abilities.Air;

import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AirSwirl extends AirAbility implements AddonAbility {

    private long cooldown;
    private double baseDamage;
    private double baseRange;
    private double baseSpeed;
    private double airKnockback;
    private double wallDamage;
    private long multiWindow;
    private long minShotInterval;

    private boolean hasMulti;
    private boolean hasDouble;
    private boolean hasMaster;

    private int maxShots = 1;
    private int shotsFired = 0;
    private long lastShotTime = 0;

    private List<SwirlProjectile> projectiles = new ArrayList<>();

    public AirSwirl(Player player) {
        super(player);

        if (hasAbility(player, AirSwirl.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfigAndUpgrades();
        start();
        fireShot();
    }

    private void loadConfigAndUpgrades() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirSwirl.Cooldown", 5000L);
        this.baseDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirSwirl.Damage", 3.5);
        this.baseRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirSwirl.Range", 20.0);
        this.baseSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirSwirl.Speed", 0.9);
        this.airKnockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirSwirl.AirKnockback", 1.5);
        this.wallDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirSwirl.WallDamage", 3.0);
        this.multiWindow = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirSwirl.MultiWindow", 3000L);
        this.minShotInterval = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirSwirl.MinShotIntervalMs", 500L);

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        this.hasMulti = (branch != null && (branch.hasUpgrade("AirSwirlMulti") || branch.hasUpgrade("MultiShot")));
        this.hasDouble = (branch != null && (branch.hasUpgrade("AirSwirlDouble") || branch.hasUpgrade("DoubleMulti")));
        this.hasMaster = (branch != null && (branch.hasUpgrade("AirSwirlMaster") || branch.hasUpgrade("SwirlMaster")));

        if (hasDouble) {
            this.maxShots = 3;
        } else if (hasMulti) {
            this.maxShots = 2;
        } else {
            this.maxShots = 1;
        }

        if (hasMaster) {
            this.airKnockback *= 1.6;
        }
    }

    public void onClick() {
        if (shotsFired < maxShots) {
            if (System.currentTimeMillis() - lastShotTime < minShotInterval) {
                return;
            }
            fireShot();
        }
    }

    private void fireShot() {
        shotsFired++;
        lastShotTime = System.currentTimeMillis();

        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();

        projectiles.add(new SwirlProjectile(eyeLoc.clone(), dir, baseDamage, baseRange, baseSpeed));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);

        if (maxShots > 1) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§f[AirSwirl] §bStrzał " + shotsFired + "/" + maxShots));
        }

        if (shotsFired >= maxShots) {
            bPlayer.addCooldown(this, cooldown);
        }
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (shotsFired < maxShots && System.currentTimeMillis() - lastShotTime > multiWindow) {
            bPlayer.addCooldown(this, cooldown);
            shotsFired = maxShots;
        }

        if (!projectiles.isEmpty()) {
            List<SwirlProjectile> copy = new ArrayList<>(projectiles);
            for (SwirlProjectile proj : copy) {
                proj.progress();
                if (proj.isDead()) {
                    projectiles.remove(proj);
                }
            }
        }

        if (shotsFired >= maxShots && projectiles.isEmpty()) {
            remove();
        }
    }

    public class SwirlProjectile {
        private Location baseLoc;
        private Vector baseDir;
        private double damage;
        private double range;
        private double speed;
        private double distTraveled = 0;
        private int ticksAlive = 0;
        private boolean dead = false;
        private Set<UUID> hitEntities = new HashSet<>();

        private Vector right;
        private Vector up;

        public SwirlProjectile(Location startLoc, Vector dir, double dmg, double rng, double spd) {
            this.baseLoc = startLoc.clone();
            this.baseDir = dir.clone().normalize();
            this.damage = dmg;
            this.range = rng;
            this.speed = spd;

            this.right = new Vector(-baseDir.getZ(), 0, baseDir.getX()).normalize();
            if (right.lengthSquared() < 0.01) {
                right = new Vector(1, 0, 0);
            }
            this.up = right.clone().crossProduct(baseDir).normalize();
        }

        public void progress() {
            if (dead) return;
            ticksAlive++;

            if (hasMaster && player != null && player.isOnline()) {
                Vector eyeDir = player.getEyeLocation().getDirection().normalize();
                baseDir.add(eyeDir.multiply(0.04)).normalize();
                this.right = new Vector(-baseDir.getZ(), 0, baseDir.getX()).normalize();
                if (right.lengthSquared() < 0.01) {
                    right = new Vector(1, 0, 0);
                }
                this.up = right.clone().crossProduct(baseDir).normalize();
            }

            baseLoc.add(baseDir.clone().multiply(speed));
            distTraveled += speed;

            if (distTraveled >= range) {
                destroy();
                return;
            }

            double progressRatio = Math.min(1.0, distTraveled / range);
            double expansionScale = 1.0 + (0.75 * progressRatio);
            double currentRadius = 0.3 * expansionScale;
            double hitboxRadius = 0.5 * expansionScale;

            double angle = ticksAlive * 0.4;
            Vector spiralOffset = right.clone().multiply(Math.cos(angle) * currentRadius)
                    .add(up.clone().multiply(Math.sin(angle) * currentRadius));
            Location currentPos = baseLoc.clone().add(spiralOffset);

            Block blk = currentPos.getBlock();
            if (blk.getType().isSolid()) {
                destroy();
                return;
            }

            currentPos.getWorld().spawnParticle(Particle.CLOUD, currentPos, 2, 0.05, 0.05, 0.05, 0.01);
            currentPos.getWorld().spawnParticle(Particle.SPELL_WITCH, currentPos, 1, 0.02, 0.02, 0.02, 0.0);
            Particle.DustOptions whiteDust = new Particle.DustOptions(Color.fromRGB(225, 240, 255), 0.8f);
            currentPos.getWorld().spawnParticle(Particle.DUST, currentPos, 1, 0, 0, 0, 0, whiteDust);

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentPos, hitboxRadius)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    if (hitEntities.contains(target.getUniqueId())) continue;

                    hitEntities.add(target.getUniqueId());
                    DamageHandler.damageEntity(target, damage, AirSwirl.this);

                    Vector knock = baseDir.clone().normalize().multiply(airKnockback).setY(0.35);
                    target.setVelocity(knock);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.2f);

                    if (hasMaster) {
                        trackWallImpact(target);
                    }

                    if (!hasDouble) {
                        destroy();
                        return;
                    }
                }
            }
        }

        private void trackWallImpact(final LivingEntity target) {
            new BukkitRunnable() {
                int checks = 0;
                @Override
                public void run() {
                    if (target == null || target.isDead() || !target.isValid() || checks++ > 8) {
                        cancel();
                        return;
                    }

                    for (Block b : GeneralMethods.getBlocksAroundPoint(target.getLocation(), 1.0)) {
                        if (b.getType().isSolid()) {
                            DamageHandler.damageEntity(target, wallDamage, AirSwirl.this);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
                            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                            cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(AmonPackPlugin.getInstance(), 2L, 2L);
        }

        private void destroy() {
            if (dead) return;
            dead = true;
            baseLoc.getWorld().spawnParticle(Particle.CLOUD, baseLoc, 8, 0.2, 0.2, 0.2, 0.05);
            baseLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, baseLoc, 1, 0, 0, 0, 0);
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
        return "AirSwirl";
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
    public void load() {}

    @Override
    public void stop() {
        remove();
    }
}
