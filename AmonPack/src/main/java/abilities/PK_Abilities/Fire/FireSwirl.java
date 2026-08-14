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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class FireSwirl extends FireAbility implements AddonAbility {

    private long cooldown;
    private double baseDamage;
    private double baseRange;
    private double baseSpeed;
    private int fireTicks;
    private long multiWindow;
    private long minShotInterval;

    private boolean hasMulti;
    private boolean hasDouble;
    private boolean hasMaster;

    private int maxShots = 1;
    private int shotsFired = 0;
    private long lastShotTime = 0;

    private List<SwirlProjectile> projectiles = new ArrayList<>();

    public FireSwirl(Player player) {
        super(player);

        if (hasAbility(player, FireSwirl.class)) {
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
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FireSwirl.Cooldown", 5000L);
        this.baseDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FireSwirl.Damage", 3.5);
        this.baseRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FireSwirl.Range", 20.0);
        this.baseSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FireSwirl.Speed", 0.9);
        this.fireTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FireSwirl.FireTicks", 60);
        this.multiWindow = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FireSwirl.MultiWindow", 3000L);
        this.minShotInterval = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FireSwirl.MinShotIntervalMs", 500L);

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        this.hasMulti = (branch != null && (branch.hasUpgrade("FireSwirlMulti") || branch.hasUpgrade("MultiShot")));
        this.hasDouble = (branch != null && (branch.hasUpgrade("FireSwirlDouble") || branch.hasUpgrade("DoubleMulti")));
        this.hasMaster = (branch != null && (branch.hasUpgrade("FireSwirlMaster") || branch.hasUpgrade("SwirlMaster")));

        if (hasDouble) {
            this.maxShots = 3;
        } else if (hasMulti) {
            this.maxShots = 2;
        } else {
            this.maxShots = 1;
        }

        if (hasMaster) {
            this.fireTicks *= 2;
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

        boolean isFirelord = FirelordStanceManager.isActive(player);
        double dmg = baseDamage * (isFirelord ? 1.5 : 1.0);
        double rng = baseRange * (isFirelord ? 1.4 : 1.0);
        double spd = baseSpeed * (isFirelord ? 1.3 : 1.0);

        projectiles.add(new SwirlProjectile(eyeLoc.clone(), dir, dmg, rng, spd, isFirelord));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.3f);

        if (isFirelord) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§6⚡ Firelord — §eFireSwirl (" + shotsFired + "/" + maxShots + ")"));
        } else if (maxShots > 1) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§6[FireSwirl] §eStrzał " + shotsFired + "/" + maxShots));
        }

        if (shotsFired >= maxShots) {
            long finalCd = isFirelord ? (long)(cooldown * 0.6) : cooldown;
            bPlayer.addCooldown(this, finalCd);
        }
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (shotsFired < maxShots && System.currentTimeMillis() - lastShotTime > multiWindow) {
            boolean isFirelord = FirelordStanceManager.isActive(player);
            long finalCd = isFirelord ? (long)(cooldown * 0.6) : cooldown;
            bPlayer.addCooldown(this, finalCd);
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
        private boolean isFirelord;
        private double distTraveled = 0;
        private int ticksAlive = 0;
        private boolean dead = false;
        private Set<UUID> hitEntities = new HashSet<>();

        private Vector right;
        private Vector up;

        public SwirlProjectile(Location startLoc, Vector dir, double dmg, double rng, double spd, boolean firelord) {
            this.baseLoc = startLoc.clone();
            this.baseDir = dir.clone().normalize();
            this.damage = dmg;
            this.range = rng;
            this.speed = spd;
            this.isFirelord = firelord;

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

            currentPos.getWorld().spawnParticle(Particle.FLAME, currentPos, 3, 0.08, 0.08, 0.08, 0.02);
            currentPos.getWorld().spawnParticle(Particle.SMOKE, currentPos, 1, 0.05, 0.05, 0.05, 0.01);
            if (isFirelord) {
                currentPos.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentPos, 2, 0.1, 0.1, 0.1, 0.04);
            }

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentPos, hitboxRadius)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    if (hitEntities.contains(target.getUniqueId())) continue;

                    hitEntities.add(target.getUniqueId());
                    DamageHandler.damageEntity(target, damage, FireSwirl.this);
                    target.setFireTicks(fireTicks);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_BURN, 1.0f, 1.0f);

                    if (!hasDouble) {
                        destroy();
                        return;
                    }
                }
            }
        }

        private void destroy() {
            if (dead) return;
            dead = true;
            baseLoc.getWorld().spawnParticle(Particle.FLAME, baseLoc, 8, 0.2, 0.2, 0.2, 0.05);
            baseLoc.getWorld().spawnParticle(Particle.SMOKE, baseLoc, 5, 0.2, 0.2, 0.2, 0.05);
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
        return "FireSwirl";
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
