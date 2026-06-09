package Abilities.PK_Abilities.Earth;

import java.util.ArrayList;
import java.util.List;
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

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;

import Plugin.AmonPackPlugin;
import Plugin.Methods;

public class SteelSwing extends MetalAbility implements AddonAbility {

    private boolean isEarthbendableBlock;
    private boolean blockFlyingFree;
    private Location launchLoc;

    private long cooldown;
    private double range;
    private double speed;

    private int initialSlot;
    private Location cable1Loc;
    private Vector cable1Dir;
    private boolean cable1Traveling;
    private boolean cable1IsAttached;
    private Location cable1AttachedLoc;
    private Material cable1Material = Material.STONE;
    private long firstCableAttachTime;

    private Location cable2Loc;
    private Vector cable2Dir;
    private boolean cable2Traveling;
    private boolean cable2IsAttached;
    private Location cable2AttachedLoc;
    private Material cable2Material = Material.STONE;

    private boolean isAttractingPlayer;
    private boolean isPullingBlock;
    private Location pulledBlockLoc;
    private Vector pulledBlockVelocity;
    private Material pulledBlockMaterial;

    public SteelSwing(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Metal.SteelSwing.Cooldown", 5000);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Metal.SteelSwing.Range", 25.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Metal.SteelSwing.Speed", 2.2);

        this.initialSlot = player.getInventory().getHeldItemSlot();
        this.cable1Loc = player.getEyeLocation();
        this.cable1Dir = player.getEyeLocation().getDirection().normalize();
        this.cable1Traveling = true;
        this.cable1IsAttached = false;
        this.cable2Traveling = false;
        this.cable2IsAttached = false;
        this.isAttractingPlayer = false;
        this.isPullingBlock = false;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);
        start();
    }

    public void onClick() {
        if ((cable1Traveling || cable1IsAttached) && !cable2Traveling && !cable2IsAttached && !isPullingBlock) {
            this.cable2Loc = player.getEyeLocation();
            this.cable2Dir = player.getEyeLocation().getDirection().normalize();
            this.cable2Traveling = true;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (cable1Traveling) {
            Vector step = cable1Dir.clone().multiply(0.2);
            for (int i = 0; i < (int) (speed / 0.2); i++) {
                cable1Loc.add(step);
                Block b = cable1Loc.getBlock();
                if (b.getType().isSolid() && b.getType() != Material.WATER && b.getType() != Material.LAVA) {
                    cable1AttachedLoc = cable1Loc.clone();
                    cable1Material = b.getType();
                    cable1IsAttached = true;
                    cable1Traveling = false;
                    player.getWorld().playSound(cable1AttachedLoc, Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.6f);
                    player.getWorld().playSound(cable1AttachedLoc, Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.2f);
                    firstCableAttachTime = System.currentTimeMillis();
                    break;
                }
            }
            if (cable1Traveling) {
                drawTether(player.getEyeLocation(), cable1Loc);
                if (player.getEyeLocation().distanceSquared(cable1Loc) > range * range) {
                    remove();
                    bPlayer.addCooldown(this);
                    return;
                }
            }
        }

        if (cable1IsAttached && !cable2Traveling && !cable2IsAttached && !isPullingBlock) {
            drawTether(player.getEyeLocation(), cable1AttachedLoc);

            if (System.currentTimeMillis() - firstCableAttachTime > 7000) {
                remove();
                bPlayer.addCooldown(this);
                return;
            }

            if (player.isSneaking()) {
                isPullingBlock = true;
                isEarthbendableBlock = EarthAbility.isEarthbendable(player, cable1AttachedLoc.getBlock());
                blockFlyingFree = false;
                pulledBlockLoc = cable1AttachedLoc.clone();
                pulledBlockMaterial = cable1Material;
                pulledBlockVelocity = player.getEyeLocation().toVector().subtract(cable1AttachedLoc.toVector())
                        .normalize().multiply(0.35);
                new TempBlock(cable1AttachedLoc.getBlock(), Material.AIR).setRevertTime(100);
                player.getWorld().playSound(cable1AttachedLoc, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1.5f);
            }
        }

        if (cable2Traveling) {
            Vector step = cable2Dir.clone().multiply(0.2);
            for (int i = 0; i < (int) (speed / 0.2); i++) {
                cable2Loc.add(step);
                Block b = cable2Loc.getBlock();
                if (b.getType().isSolid() && b.getType() != Material.WATER && b.getType() != Material.LAVA) {
                    cable2AttachedLoc = cable2Loc.clone();
                    cable2Material = b.getType();
                    cable2IsAttached = true;
                    cable2Traveling = false;
                    player.getWorld().playSound(cable2AttachedLoc, Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.6f);
                    player.getWorld().playSound(cable2AttachedLoc, Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.2f);
                    break;
                }
            }
            if (cable1IsAttached) {
                drawTether(player.getEyeLocation(), cable1AttachedLoc);
            } else {
                drawTether(player.getEyeLocation(), cable1Loc);
            }
            if (cable2Traveling) {
                drawTether(player.getEyeLocation(), cable2Loc);
                if (player.getEyeLocation().distanceSquared(cable2Loc) > range * range) {
                    remove();
                    bPlayer.addCooldown(this);
                    return;
                }
            }
        }

        if (cable1IsAttached && cable2IsAttached && !isAttractingPlayer && !isPullingBlock) {
            isAttractingPlayer = true;
        }

        if (isAttractingPlayer) {
            drawTether(player.getEyeLocation(), cable1AttachedLoc);
            drawTether(player.getEyeLocation(), cable2AttachedLoc);

            Location midpoint = new Location(player.getWorld(),
                    (cable1AttachedLoc.getX() + cable2AttachedLoc.getX()) / 2.0,
                    (cable1AttachedLoc.getY() + cable2AttachedLoc.getY()) / 2.0,
                    (cable1AttachedLoc.getZ() + cable2AttachedLoc.getZ()) / 2.0);

            if (player.getInventory().getHeldItemSlot() != initialSlot) {
                Vector thrust = midpoint.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.85)
                        .setY(0.4);
                player.setVelocity(thrust);
                remove();
                bPlayer.addCooldown(this);
                return;
            }

            Vector pullDir = midpoint.toVector().subtract(player.getLocation().toVector());
            double dist = pullDir.length();
            if (dist > 2.0) {
                player.setVelocity(pullDir.normalize().multiply(0.95).setY(pullDir.normalize().getY() * 0.95 + 0.1));
            } else {
                Vector boost = pullDir.normalize().multiply(1.0).setY(0.4);
                player.setVelocity(boost);
                remove();
                bPlayer.addCooldown(this);
                return;
            }
        }

        if (isPullingBlock) {
            if (!blockFlyingFree) {
                Location start = player.getEyeLocation();
                Location end = pulledBlockLoc;
                if (start.getWorld().equals(end.getWorld())) {
                    Vector dir = end.toVector().subtract(start.toVector()).normalize();
                    double dist = start.distance(end);
                    for (double d = 1.0; d < dist - 1.0; d += 0.5) {
                        Block checkBlock = start.clone().add(dir.clone().multiply(d)).getBlock();
                        if (checkBlock.getType().isSolid()) {
                            remove();
                            bPlayer.addCooldown(this);
                            return;
                        }
                    }
                }

                Vector direction = player.getEyeLocation().getDirection().normalize();
                Vector up = new Vector(0, 1, 0);
                Vector right = direction.clone().crossProduct(up).normalize();
                boolean isRightHand = player.getMainHand() == org.bukkit.inventory.MainHand.RIGHT;

                Vector toPlayer = player.getEyeLocation().toVector().subtract(pulledBlockLoc.toVector());
                double dist = toPlayer.length();

                double offsetScale = (isEarthbendableBlock ? 1.6 : 1.1) * Math.min(1.0, dist / 6.0);
                Vector pullOffset = right.multiply(isRightHand ? offsetScale : -offsetScale);
                Location targetLoc = player.getEyeLocation().add(pullOffset);

                Vector targetDir = targetLoc.toVector().subtract(pulledBlockLoc.toVector());
                double accel = isEarthbendableBlock ? 0.024 : 0.032;
                pulledBlockVelocity.add(targetDir.normalize().multiply(accel));

                double drag = isEarthbendableBlock ? 0.985 : 0.965;
                pulledBlockVelocity.multiply(drag);

                double maxSpeed = isEarthbendableBlock ? 0.44 : 0.48;
                if (pulledBlockVelocity.length() > maxSpeed) {
                    pulledBlockVelocity.normalize().multiply(maxSpeed);
                }

                pulledBlockLoc.add(pulledBlockVelocity);

                Vector sideOffset = right.clone().multiply(isRightHand ? 0.65 : -0.65);
                Location handLoc = player.getEyeLocation().add(sideOffset);
                drawTether(handLoc, pulledBlockLoc);

                if (isEarthbendableBlock) {
                    player.getWorld().spawnParticle(Particle.BLOCK, pulledBlockLoc, 4, 0.25, 0.25, 0.25, 0.05, pulledBlockMaterial.createBlockData());
                    player.getWorld().spawnParticle(Particle.DUST, pulledBlockLoc, 3, 0.2, 0.2, 0.2, 0.05, new Particle.DustOptions(Color.fromRGB(200, 180, 150), 0.7f));
                    
                    double distanceMoved = pulledBlockLoc.distance(cable1AttachedLoc);
                    if (distanceMoved <= 2.0) {
                        Methods.spawnFallingBlocks(pulledBlockLoc, pulledBlockMaterial, 1, 0.45, player);
                    }
                } else {
                    player.getWorld().spawnParticle(Particle.BLOCK, pulledBlockLoc, 6, 0.1, 0.1, 0.1, 0.05, pulledBlockMaterial.createBlockData());
                }

                Block b = pulledBlockLoc.getBlock();
                if (!b.getType().isSolid() || b.getType() == Material.AIR) {
                    new TempBlock(b, pulledBlockMaterial).setRevertTime(100);
                }

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(pulledBlockLoc, 1.8)) {
                    if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                        LivingEntity target = (LivingEntity) entity;
                        DamageHandler.damageEntity(target, 4.0, this);
                        Vector pullVector = player.getLocation().toVector().subtract(target.getLocation().toVector())
                                .normalize().multiply(0.6).setY(0.25);
                        target.setVelocity(pullVector);
                        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 1.2f);
                    }
                }

                if (pulledBlockLoc.distance(player.getEyeLocation()) <= 1.8) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.6f);
                    player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1,
                            pulledBlockMaterial.createBlockData());
                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), 3.5)) {
                        if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                            LivingEntity target = (LivingEntity) entity;
                            DamageHandler.damageEntity(target, 6.0, this);
                            Vector pullVector = player.getLocation().toVector().subtract(target.getLocation().toVector())
                                    .normalize().multiply(0.7).setY(0.3);
                            target.setVelocity(pullVector);
                        }
                    }
                    remove();
                    bPlayer.addCooldown(this);
                    return;
                }

                if (!player.isSneaking()) {
                    if (isEarthbendableBlock) {
                        blockFlyingFree = true;
                        launchLoc = pulledBlockLoc.clone();
                    } else {
                        remove();
                        bPlayer.addCooldown(this);
                        return;
                    }
                }
            } else {
                pulledBlockLoc.add(pulledBlockVelocity);

                if (isEarthbendableBlock) {
                    player.getWorld().spawnParticle(Particle.BLOCK, pulledBlockLoc, 4, 0.25, 0.25, 0.25, 0.05, pulledBlockMaterial.createBlockData());
                    player.getWorld().spawnParticle(Particle.DUST, pulledBlockLoc, 3, 0.2, 0.2, 0.2, 0.05, new Particle.DustOptions(Color.fromRGB(200, 180, 150), 0.7f));
                } else {
                    player.getWorld().spawnParticle(Particle.BLOCK, pulledBlockLoc, 6, 0.1, 0.1, 0.1, 0.05, pulledBlockMaterial.createBlockData());
                }

                Block b = pulledBlockLoc.getBlock();
                if (!b.getType().isSolid() || b.getType() == Material.AIR) {
                    new TempBlock(b, pulledBlockMaterial).setRevertTime(100);
                }

                boolean hit = false;
                if (b.getType().isSolid() && b.getType() != Material.WATER && b.getType() != Material.LAVA) {
                    hit = true;
                }

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(pulledBlockLoc, 1.8)) {
                    if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                        hit = true;
                        break;
                    }
                }

                if (hit) {
                    player.getWorld().playSound(pulledBlockLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.6f);
                    player.getWorld().spawnParticle(Particle.BLOCK, pulledBlockLoc, 30, 0.5, 0.5, 0.5, 0.1,
                            pulledBlockMaterial.createBlockData());
                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(pulledBlockLoc, 3.5)) {
                        if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                            LivingEntity target = (LivingEntity) entity;
                            DamageHandler.damageEntity(target, 6.0, this);
                            Vector pullVector = player.getLocation().toVector().subtract(target.getLocation().toVector())
                                    .normalize().multiply(0.7).setY(0.3);
                            target.setVelocity(pullVector);
                        }
                    }
                    remove();
                    bPlayer.addCooldown(this);
                    return;
                }

                if (launchLoc != null && launchLoc.distance(pulledBlockLoc) > 20.0) {
                    player.getWorld().playSound(pulledBlockLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.6f);
                    player.getWorld().spawnParticle(Particle.BLOCK, pulledBlockLoc, 30, 0.5, 0.5, 0.5, 0.1,
                            pulledBlockMaterial.createBlockData());
                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(pulledBlockLoc, 3.5)) {
                        if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                            LivingEntity target = (LivingEntity) entity;
                            DamageHandler.damageEntity(target, 6.0, this);
                            Vector pullVector = player.getLocation().toVector().subtract(target.getLocation().toVector())
                                    .normalize().multiply(0.7).setY(0.3);
                            target.setVelocity(pullVector);
                        }
                    }
                    remove();
                    bPlayer.addCooldown(this);
                    return;
                }
            }
        }
    }

    private void drawTether(Location start, Location end) {
        if (!start.getWorld().equals(end.getWorld())) {
            return;
        }
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < dist; d += 0.4) {
            Location point = start.clone().add(dir.clone().multiply(d));
            double wave = Math.sin(d * 1.5 + (System.currentTimeMillis() / 60.0)) * 0.12;
            point.add(0, wave, 0);
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 150, 150), 0.6f));
        }
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
        return "SteelSwing";
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
        return "Left-click to shoot a steel cable. Left-click again on a block to swing towards the midpoint, or hold sneak to retract the cable and pull the block with physics.";
    }

    @Override
    public String getInstructions() {
        return "Left-click on a block to attach the first cable. Left-click another block to swing, or hold sneak to pull.";
    }
}
