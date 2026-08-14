package Abilities.PK_Abilities.Earth;

import Abilities.Util_Objects.EarthDisc;
import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class EarthStrike extends EarthAbility implements AddonAbility {

    private enum State {
        CHARGING, FIRING
    }

    private State state;
    private long cooldown;
    private double damage;
    private double range;
    private double speed;
    private double sourceRange;
    private double multiSourceRange;
    private int splitCount;
    private double splitRange;
    private double splitDamage;

    private boolean hasSplitShot;
    private boolean hasTriOrbit;
    private boolean hasPiercing;

    private Material sourceMaterial = Material.STONE;
    private List<Location> sourceBlocks = new ArrayList<>();
    private List<Location> currentBlockPositions = new ArrayList<>();
    private List<TempBlock> floatingTempBlocks = new ArrayList<>();

    private double orbitAngle = 0;
    private List<EarthStrikeProjectile> projectiles = new ArrayList<>();

    public EarthStrike(Player player) {
        super(player);

        if (hasAbility(player, EarthStrike.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfigAndUpgrades();

        int neededSources = hasTriOrbit ? 3 : 1;
        List<Block> candidates = new ArrayList<>();
        for (Block b : GeneralMethods.getBlocksAroundPoint(player.getLocation(), hasTriOrbit ? multiSourceRange : sourceRange)) {
            if (b.getLocation().getY() <= player.getLocation().getY() + 1
                    && b.getLocation().distance(player.getLocation()) >= 1.5
                    && EarthAbility.isEarthbendable(player, b)) {
                candidates.add(b);
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        Collections.shuffle(candidates);
        int takeCount = Math.min(neededSources, candidates.size());
        for (int i = 0; i < takeCount; i++) {
            Block b = candidates.get(i);
            sourceBlocks.add(b.getLocation().clone().add(0.5, 0.5, 0.5));
            sourceMaterial = b.getType();
            TempBlock tb = new TempBlock(b, Material.AIR);
            tb.setRevertTime(10000);
            currentBlockPositions.add(b.getLocation().clone().add(0.5, 0.5, 0.5));
        }

        this.state = State.CHARGING;
        start();
    }

    private void loadConfigAndUpgrades() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthStrike.Cooldown", 6000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthStrike.Damage", 5.0);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthStrike.Range", 30.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthStrike.Speed", 1.2);
        this.sourceRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthStrike.SourceRange", 10.0);
        this.multiSourceRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthStrike.MultiSourceRange", 12.0);
        this.splitCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthStrike.SplitCount", 5);
        this.splitRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthStrike.SplitRange", 8.0);
        this.splitDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthStrike.SplitDamage", 2.0);

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        this.hasSplitShot = (branch != null && (branch.hasUpgrade("EarthStrikeSplit") || branch.hasUpgrade("SplitShot")));
        this.hasTriOrbit = (branch != null && (branch.hasUpgrade("EarthStrikeTriOrbit") || branch.hasUpgrade("TriOrbit")));
        this.hasPiercing = (branch != null && (branch.hasUpgrade("EarthStrikePiercing") || branch.hasUpgrade("Piercing")));
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            cleanFloatingBlocks();
            remove();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                onShiftRelease();
                return;
            }

            cleanFloatingBlocks();

            Location eyeLoc = player.getEyeLocation();
            Vector lookDir = eyeLoc.getDirection().normalize();
            Location centerTarget = eyeLoc.clone().add(lookDir.clone().multiply(2.5));

            Vector right = new Vector(-lookDir.getZ(), 0, lookDir.getX()).normalize();
            if (right.lengthSquared() < 0.01) {
                right = new Vector(1, 0, 0);
            }
            Vector up = right.clone().crossProduct(lookDir).normalize();

            orbitAngle += 0.15;
            int count = currentBlockPositions.size();

            for (int i = 0; i < count; i++) {
                Location targetPos;
                if (count > 1) {
                    double angle = orbitAngle + (i * (2 * Math.PI / count));
                    Vector offset = right.clone().multiply(Math.cos(angle) * 0.8).add(up.clone().multiply(Math.sin(angle) * 0.8));
                    targetPos = centerTarget.clone().add(offset);
                } else {
                    targetPos = centerTarget.clone();
                }

                Location current = currentBlockPositions.get(i);
                Vector diff = targetPos.toVector().subtract(current.toVector());
                current.add(diff.multiply(0.15));

                Block blk = current.getBlock();
                if (blk.getType() == Material.AIR) {
                    TempBlock tb = new TempBlock(blk, sourceMaterial);
                    tb.setRevertTime(150);
                    floatingTempBlocks.add(tb);
                }

                player.getWorld().spawnParticle(Particle.BLOCK_CRACK, current, 3, 0.15, 0.15, 0.15, 0.02, sourceMaterial.createBlockData());
            }

        } else if (state == State.FIRING) {
            if (projectiles.isEmpty()) {
                remove();
                return;
            }

            List<EarthStrikeProjectile> copy = new ArrayList<>(projectiles);
            for (EarthStrikeProjectile proj : copy) {
                proj.progress();
                if (proj.isDead()) {
                    projectiles.remove(proj);
                }
            }
        }
    }

    public void onShiftRelease() {
        if (state == State.CHARGING) {
            cleanFloatingBlocks();
            state = State.FIRING;

            Location eyeLoc = player.getEyeLocation();
            Vector baseDir = eyeLoc.getDirection().normalize();

            Vector right = new Vector(-baseDir.getZ(), 0, baseDir.getX()).normalize();
            if (right.lengthSquared() < 0.01) {
                right = new Vector(1, 0, 0);
            }

            int count = currentBlockPositions.size();
            for (int i = 0; i < count; i++) {
                Location spawnLoc = currentBlockPositions.get(i).clone();
                Vector shotDir = baseDir.clone();
                if (count == 3) {
                    double spread = (i - 1) * 0.12;
                    shotDir.add(right.clone().multiply(spread)).normalize();
                }

                projectiles.add(new EarthStrikeProjectile(spawnLoc, shotDir, false));
            }

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.2f, 0.6f);
            bPlayer.addCooldown(this);
        }
    }

    private void cleanFloatingBlocks() {
        for (TempBlock tb : floatingTempBlocks) {
            if (tb != null) {
                tb.revertBlock();
            }
        }
        floatingTempBlocks.clear();
    }

    @Override
    public void remove() {
        cleanFloatingBlocks();
        super.remove();
    }

    public class EarthStrikeProjectile {
        private Location loc;
        private Vector vel;
        private double distTraveled = 0;
        private boolean dead = false;
        private boolean isMini;
        private Set<UUID> hitEntities = new HashSet<>();
        private TempBlock currentTempBlock = null;

        public EarthStrikeProjectile(Location startLoc, Vector initialDir, boolean isMini) {
            this.loc = startLoc.clone();
            this.isMini = isMini;
            double projSpeed = isMini ? speed * 0.9 : speed;
            this.vel = initialDir.clone().normalize().multiply(projSpeed);
        }

        public void progress() {
            if (dead) return;

            if (!isMini && hasPiercing && player != null && player.isOnline()) {
                Vector eyeDir = player.getEyeLocation().getDirection();
                Vector horizTarget = new Vector(eyeDir.getX(), 0, eyeDir.getZ()).normalize();
                Vector currentHoriz = new Vector(vel.getX(), 0, vel.getZ());
                double horizMag = currentHoriz.length();
                if (horizMag > 0.05) {
                    currentHoriz.normalize().add(horizTarget.multiply(0.04)).normalize().multiply(horizMag);
                    vel.setX(currentHoriz.getX());
                    vel.setZ(currentHoriz.getZ());
                }
                if (eyeDir.getY() < vel.getY()) {
                    vel.setY(vel.getY() + (eyeDir.getY() - vel.getY()) * 0.04);
                }
            }

            vel.setY(vel.getY() - 0.03);

            if (currentTempBlock != null) {
                currentTempBlock.revertBlock();
                currentTempBlock = null;
            }

            loc.add(vel);
            distTraveled += vel.length();

            double maxDist = isMini ? splitRange : range;
            if (distTraveled >= maxDist) {
                destroy(false);
                return;
            }

            Block blk = loc.getBlock();
            if (blk.getType().isSolid()) {
                destroy(true);
                return;
            }

            if (!isMini && blk.getType() == Material.AIR) {
                currentTempBlock = new TempBlock(blk, sourceMaterial);
                currentTempBlock.setRevertTime(150);
            }

            loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, isMini ? 2 : 4, 0.15, 0.15, 0.15, 0.02, sourceMaterial.createBlockData());
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(120, 85, 50), isMini ? 0.7f : 1.1f);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, isMini ? 0.9 : 1.2)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    if (hitEntities.contains(target.getUniqueId())) continue;

                    hitEntities.add(target.getUniqueId());
                    double dmg = isMini ? splitDamage : damage;
                    DamageHandler.damageEntity(target, dmg, EarthStrike.this);

                    Vector knock = vel.clone().normalize().multiply(isMini ? 0.4 : 0.8).setY(0.3);
                    target.setVelocity(knock);
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);

                    if (!hasPiercing || isMini) {
                        destroy(hasSplitShot && !isMini);
                        return;
                    }
                }
            }
        }

        private void destroy(boolean triggerSplit) {
            if (dead) return;
            dead = true;

            if (currentTempBlock != null) {
                currentTempBlock.revertBlock();
                currentTempBlock = null;
            }

            loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 12, 0.3, 0.3, 0.3, 0.05, sourceMaterial.createBlockData());
            loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);

            if (triggerSplit && hasSplitShot && !isMini) {
                spawnSplitProjectiles();
            }
        }

        private void spawnSplitProjectiles() {
            Random rand = new Random();
            for (int i = 0; i < splitCount; i++) {
                double angle = (2 * Math.PI / splitCount) * i + (rand.nextDouble() * 0.3);
                double pitch = (rand.nextDouble() - 0.2) * 0.8;
                Vector splitDir = new Vector(Math.cos(angle), pitch, Math.sin(angle)).normalize();
                if (vel.lengthSquared() > 0.01) {
                    splitDir.add(vel.clone().normalize().multiply(0.4)).normalize();
                }
                projectiles.add(new EarthStrikeProjectile(loc.clone(), splitDir, true));
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
        return "EarthStrike";
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
        remove();
    }
}
