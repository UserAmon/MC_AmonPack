package Abilities.PK_Abilities.Earth;

import Plugin.AmonPackPlugin;
import Plugin.Methods;
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
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class EarthBarricade extends EarthAbility implements AddonAbility {

    private enum State {
        RISING, HOLDING, LAUNCHING
    }

    private State state;
    private long cooldown;
    private long duration;
    private double wallDistance;
    private int baseWidth;
    private int baseHeight;
    private int baseThickness;
    private double crumbleDamage;
    private double crumbleRadius;
    private int shatterDebrisCount;
    private double launchSpeed;
    private double launchMaxRange;
    private double launchDamage;
    private double launchPush;

    private boolean hasSize;
    private boolean hasShatter;
    private boolean hasLaunch;

    private int width;
    private int height;
    private int thickness;

    private Material wallMaterial = Material.STONE;
    private Vector currentFacing;
    private Location currentCenter;
    private long holdStartTime = 0;
    private int riseTick = 0;
    private int maxRiseTicks = 6;

    private List<TempBlock> activeBlocks = new ArrayList<>();
    private double launchDistTraveled = 0;
    private Set<UUID> hitEnemiesDuringLaunch = new HashSet<>();

    public EarthBarricade(Player player) {
        super(player);

        if (hasAbility(player, EarthBarricade.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfigAndUpgrades();

        if (!player.isOnGround() && player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isAir()) {
            return;
        }

        Vector look = player.getEyeLocation().getDirection().setY(0);
        if (look.lengthSquared() < 0.01) {
            look = new Vector(0, 0, 1);
        }
        this.currentFacing = look.normalize();

        Location initialGround = findGroundAhead(player.getLocation(), currentFacing, wallDistance);
        if (initialGround == null) {
            return;
        }
        this.currentCenter = initialGround.getBlock().getLocation().add(0.5, 0.0, 0.5);

        Block groundBlock = initialGround.getBlock().getRelative(BlockFace.DOWN);
        if (EarthAbility.isEarthbendable(player, groundBlock)) {
            this.wallMaterial = groundBlock.getType();
        } else {
            this.wallMaterial = Material.STONE;
        }

        this.state = State.RISING;
        this.riseTick = 0;
        player.getWorld().playSound(currentCenter, Sound.BLOCK_STONE_PLACE, 1.2f, 0.7f);
        start();
    }

    private void loadConfigAndUpgrades() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthBarricade.Cooldown", 8000L);
        this.duration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthBarricade.Duration", 5000L);
        this.wallDistance = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthBarricade.WallDistance",
                3.5);
        this.baseWidth = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthBarricade.WallWidth", 3);
        this.baseHeight = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthBarricade.WallHeight", 3);
        this.baseThickness = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthBarricade.WallThickness",
                1);
        this.crumbleDamage = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Earth.EarthBarricade.CrumbleDamage", 3.0);
        this.crumbleRadius = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Earth.EarthBarricade.CrumbleRadius", 3.5);
        this.shatterDebrisCount = AmonPackPlugin.getAbilitiesConfig()
                .getInt("AmonPack.Earth.EarthBarricade.ShatterDebrisCount", 10);
        this.launchSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthBarricade.LaunchSpeed",
                0.75);
        this.launchMaxRange = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Earth.EarthBarricade.LaunchMaxRange", 14.0);
        this.launchDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthBarricade.LaunchDamage",
                4.5);
        this.launchPush = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthBarricade.LaunchPush",
                0.85);

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null)
                ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName())
                : null;
        this.hasSize = (branch != null && (branch.hasUpgrade("EarthWallSize") || branch.hasUpgrade("WallSize")));
        this.hasShatter = (branch != null
                && (branch.hasUpgrade("EarthWallShatter") || branch.hasUpgrade("WallShatter")));
        this.hasLaunch = (branch != null && (branch.hasUpgrade("EarthWallLaunch") || branch.hasUpgrade("WallLaunch")));

        if (hasSize) {
            this.width = baseWidth + 2;
            this.height = baseHeight + 1;
            this.thickness = baseThickness + 1;
        } else {
            this.width = baseWidth;
            this.height = baseHeight;
            this.thickness = baseThickness;
        }
    }

    private Location findGroundAhead(Location origin, Vector direction, double dist) {
        Location target = origin.clone().add(direction.clone().setY(0).normalize().multiply(dist));
        for (int y = 3; y >= -5; y--) {
            Block b = target.clone().add(0, y, 0).getBlock();
            if (TempBlock.isTempBlock(b)) {
                continue;
            }
            if (b.getType().isSolid() || EarthAbility.isEarthbendable(player, b)) {
                return b.getLocation().add(0.5, 1.0, 0.5);
            }
        }
        return null;
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            cleanWall();
            remove();
            return;
        }

        switch (state) {
            case RISING:
                if (!player.isSneaking()) {
                    onShiftRelease();
                    return;
                }

                updateFacingAndCenterWithLerp();
                riseTick++;
                renderWall(Math.min(1.0, (double) riseTick / maxRiseTicks));
                player.getWorld().spawnParticle(Particle.BLOCK, currentCenter.clone().add(0, 0.5, 0), 8, 1.0, 0.3, 1.0,
                        0.05, wallMaterial.createBlockData());

                if (riseTick >= maxRiseTicks) {
                    state = State.HOLDING;
                    holdStartTime = System.currentTimeMillis();
                }
                break;

            case HOLDING:
                if (!player.isSneaking()) {
                    onShiftRelease();
                    return;
                }

                if (System.currentTimeMillis() - holdStartTime > duration) {
                    crumble();
                    return;
                }

                updateFacingAndCenterWithLerp();
                renderWall(1.0);

                if (Math.random() < 0.25) {
                    player.getWorld().spawnParticle(Particle.BLOCK, currentCenter.clone().add(0, 1.0, 0), 4, 1.0, 0.5,
                            1.0, 0.02, wallMaterial.createBlockData());
                }
                break;

            case LAUNCHING:
                launchDistTraveled += launchSpeed;
                currentCenter.add(currentFacing.clone().multiply(launchSpeed));

                Location groundLoc = findGroundAhead(currentCenter, currentFacing, 0.2);
                if (groundLoc != null) {
                    currentCenter.setY(groundLoc.getBlockY());
                }

                renderWall(1.0);
                pushAndDamageEnemiesInFront();
                player.getWorld().spawnParticle(Particle.BLOCK, currentCenter.clone().add(0, 0.8, 0), 10, 1.2, 0.5, 1.2,
                        0.05, wallMaterial.createBlockData());

                if (launchDistTraveled >= launchMaxRange || isObstructed()) {
                    crumble();
                }
                break;
        }
    }

    private void updateFacingAndCenterWithLerp() {
        Vector look = player.getEyeLocation().getDirection().setY(0);
        if (look.lengthSquared() > 0.01) {
            look.normalize();
            currentFacing.add(look.clone().subtract(currentFacing).multiply(0.12)).normalize();
        }

        Location targetGround = findGroundAhead(player.getLocation(), currentFacing, wallDistance);
        if (targetGround != null) {
            currentCenter.setX(currentCenter.getX() + (targetGround.getX() - currentCenter.getX()) * 0.15);
            currentCenter.setZ(currentCenter.getZ() + (targetGround.getZ() - currentCenter.getZ()) * 0.15);
            currentCenter.setY(targetGround.getBlockY());
        }
    }

    private void renderWall(double heightFraction) {
        cleanWall();

        Vector right = new Vector(-currentFacing.getZ(), 0, currentFacing.getX()).normalize();
        int currentH = Math.max(1, (int) Math.ceil(height * heightFraction));

        int halfW = width / 2;
        for (int w = -halfW; w <= halfW; w++) {
            for (int t = 0; t < thickness; t++) {
                for (int h = 0; h < currentH; h++) {
                    Vector offset = right.clone().multiply(w)
                            .add(currentFacing.clone().multiply(t))
                            .add(new Vector(0, h, 0));
                    Block b = currentCenter.clone().add(offset).getBlock();
                    if (b.getType() == Material.AIR || b.isLiquid()) {
                        TempBlock tb = new TempBlock(b, wallMaterial);
                        tb.setRevertTime(300);
                        activeBlocks.add(tb);
                    }
                }
            }
        }
    }

    private void pushAndDamageEnemiesInFront() {
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentCenter, width * 0.7)) {
            if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                LivingEntity target = (LivingEntity) entity;
                Vector push = currentFacing.clone().multiply(launchPush).setY(0.35);
                target.setVelocity(push);

                if (!hitEnemiesDuringLaunch.contains(target.getUniqueId())) {
                    hitEnemiesDuringLaunch.add(target.getUniqueId());
                    DamageHandler.damageEntity(target, launchDamage, this);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
                }
            }
        }
    }

    private boolean isObstructed() {
        Block ahead = currentCenter.clone().add(currentFacing.clone().multiply(0.8)).getBlock();
        return ahead.getType().isSolid() && !isTempBlock(ahead);
    }

    private boolean isTempBlock(Block b) {
        for (TempBlock tb : activeBlocks) {
            if (tb.getBlock().equals(b))
                return true;
        }
        return false;
    }

    public void onShiftRelease() {
        if (state == State.RISING || state == State.HOLDING) {
            if (hasLaunch) {
                state = State.LAUNCHING;
                launchDistTraveled = 0;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.2f, 0.6f);
            } else {
                crumble();
            }
        }
    }

    private void crumble() {
        cleanWall();

        player.getWorld().playSound(currentCenter, Sound.BLOCK_STONE_BREAK, 1.2f, 0.8f);
        player.getWorld().spawnParticle(Particle.BLOCK, currentCenter, 30, 1.5, 1.0, 1.5, 0.1,
                wallMaterial.createBlockData());
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(110, 80, 50), 1.3f);
        player.getWorld().spawnParticle(Particle.DUST, currentCenter, 15, 1.5, 1.0, 1.5, 0, dust);

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentCenter, crumbleRadius)) {
            if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                LivingEntity target = (LivingEntity) entity;
                DamageHandler.damageEntity(target, crumbleDamage, this);
                Vector knock = target.getLocation().toVector().subtract(currentCenter.toVector()).normalize()
                        .multiply(0.6).setY(0.3);
                target.setVelocity(knock);
            }
        }

        if (hasShatter) {
            spawnDirectionalShatterDebris();
        }

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    private void spawnDirectionalShatterDebris() {
        Random rand = new Random();
        int count = hasSize ? shatterDebrisCount + 6 : shatterDebrisCount;

        for (int i = 0; i < count; i++) {
            FallingBlock fb = currentCenter.getWorld().spawnFallingBlock(
                    currentCenter.clone().add((rand.nextDouble() - 0.5) * 1.5, 0.5 + rand.nextDouble() * 1.5,
                            (rand.nextDouble() - 0.5) * 1.5),
                    wallMaterial.createBlockData());
            fb.setDropItem(false);
            fb.setCancelDrop(true);

            Vector forwardBias = currentFacing.clone().multiply(0.6 + rand.nextDouble() * 0.5);
            Vector side = new Vector((rand.nextDouble() - 0.5) * 0.7, 0.35 + rand.nextDouble() * 0.35,
                    (rand.nextDouble() - 0.5) * 0.7);
            fb.setVelocity(forwardBias.add(side));

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    ticks++;
                    if (fb.isDead() || !fb.isValid() || fb.isOnGround() || ticks > 30) {
                        if (fb.isValid()) {
                            fb.getWorld().spawnParticle(Particle.BLOCK, fb.getLocation(), 6, 0.2, 0.2, 0.2, 0.05,
                                    wallMaterial.createBlockData());
                            fb.remove();
                        }
                        this.cancel();
                    }
                }
            }.runTaskTimer(AmonPackPlugin.plugin, 1L, 1L);
        }
    }

    private void cleanWall() {
        for (TempBlock tb : activeBlocks) {
            if (tb != null) {
                tb.revertBlock();
            }
        }
        activeBlocks.clear();
    }

    @Override
    public void remove() {
        cleanWall();
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return currentCenter;
    }

    @Override
    public String getName() {
        return "EarthBarricade";
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
        return "Błyskawicznie wznosi solidną ścianę z ziemi i kamienia przed graczem, blokując wrogie ataki i pociski.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj shift aby postawić barykadę z ziemi.";
    }
}
