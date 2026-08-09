package Abilities.PK_Abilities.Earth;

import Abilities.Bending.SpecialTriggerable;
import Abilities.Bending.SpecialTriggerManager;
import Plugin.AmonPackPlugin;
import Plugin.Methods;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BoulderRoll extends EarthAbility implements AddonAbility, SpecialTriggerable {

    private enum State {
        CHARGING,
        FULLY_CHARGED,
        ROLLING
    }

    private State state;
    private long cooldown;
    private double damage;
    private double knockback;
    private int range;
    private double speed;
    private double radius;
    private long revertTime;

    private List<Vector> pointOffsets = new ArrayList<>();
    private boolean[] collectedPoints = new boolean[4];
    private int collectedCount = 0;
    private List<Location> flyingBlocks = new ArrayList<>();
    private List<TempBlock> activeTempBlocks = new ArrayList<>();

    private Location projectileLoc;
    private Location boulderLoc;
    private Vector rollDir;
    private double distanceTraveled = 0.0;
    private float initialYaw, initialPitch;
    private int initialSlot;
    private int tickCounter = 0;

    public BoulderRoll(Player player) {
        super(player);

        BoulderRoll activeInstance = getAbility(player, BoulderRoll.class);
        if (activeInstance != null) {
            activeInstance.tryCollectPoint();
            return;
        }

        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.BoulderRoll.Cooldown", 8000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.BoulderRoll.Damage", 5.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.BoulderRoll.Knockback", 1.2);
        this.range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.BoulderRoll.Range", 22);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.BoulderRoll.Speed", 0.7) / 2.0;
        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.BoulderRoll.Radius", 1.5);
        this.revertTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.BoulderRoll.RevertTime", 10000L);

        if (AmonPackPlugin.ENABLE_SKILL_TREE) {
            if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
                return;
            }
            SpecialTriggerManager.registerActiveSpecial(player);
        } else {
            if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
                return;
            }
        }

        this.state = State.CHARGING;
        initTargetPoints();
        start();
    }

    public void onLeftClick() {
        tryCollectPoint();
    }

    private void initTargetPoints() {
        Location eyeLoc = player.getEyeLocation();
        Vector forward = eyeLoc.getDirection().normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        if (right.lengthSquared() < 0.01) {
            right = new Vector(1, 0, 0);
        }
        Vector up = right.clone().crossProduct(forward).normalize();

        Random rand = new Random();
        pointOffsets.clear();

        for (int i = 0; i < 4; i++) {
            double distance = 3.5 + (rand.nextDouble() * 3.5);
            double offsetX = (rand.nextDouble() - 0.5) * 5.5;
            double offsetY = (rand.nextDouble() - 0.5) * 3.8;

            Vector pt = forward.clone().multiply(distance)
                    .add(right.clone().multiply(offsetX))
                    .add(up.clone().multiply(offsetY));

            Location testLoc = eyeLoc.clone().add(pt);
            int safety = 0;
            while ((testLoc.getBlock().getType().isSolid() || GeneralMethods.isObstructed(eyeLoc, testLoc))
                    && safety < 10) {
                testLoc.subtract(testLoc.clone().subtract(eyeLoc).toVector().normalize().multiply(0.4));
                safety++;
            }
            pointOffsets.add(testLoc.subtract(eyeLoc).toVector());
        }
    }

    public void tryCollectPoint() {
        if (state != State.CHARGING) {
            return;
        }
        Location eyeLoc = player.getEyeLocation();
        Vector lookDir = eyeLoc.getDirection().normalize();

        int bestPoint = -1;
        double minAngle = 999.0;

        for (int i = 0; i < 4; i++) {
            if (!collectedPoints[i]) {
                Location targetLoc = eyeLoc.clone().add(pointOffsets.get(i));
                Vector toTarget = targetLoc.clone().subtract(eyeLoc).toVector().normalize();
                double angle = Math.toDegrees(lookDir.angle(toTarget));
                if (angle < 9.0 && angle < minAngle) {
                    minAngle = angle;
                    bestPoint = i;
                }
            }
        }

        if (bestPoint != -1) {
            collectedPoints[bestPoint] = true;
            collectedCount++;
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.4f);

            List<Block> nearbyBlocks = GeneralMethods.getBlocksAroundPoint(player.getLocation(), 10);
            for (Block b : nearbyBlocks) {
                if (b.getY() <= player.getLocation().getY() + 1 && isEarthbendable(player, b)
                        && b.getType() != Material.AIR) {
                    TempBlock tb = new TempBlock(b, Material.AIR);
                    tb.setRevertTime(revertTime);
                    flyingBlocks.add(b.getLocation().add(0.5, 0.5, 0.5));
                    break;
                }
            }

            if (collectedCount >= 4) {
                state = State.FULLY_CHARGED;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }
        }
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            cancelAbility();
            return;
        }

        tickCounter++;

        if (state == State.CHARGING || state == State.FULLY_CHARGED) {
            if (!player.isSneaking()) {
                if (state == State.CHARGING) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText("§cRuch anulowany - przestałeś kucać!"));
                    cancelAbility();
                    return;
                } else if (state == State.FULLY_CHARGED) {
                    startRolling();
                    return;
                }
            }

            if (state == State.CHARGING) {
                Location eyeLoc = player.getEyeLocation();
                for (int i = 0; i < 4; i++) {
                    if (!collectedPoints[i]) {
                        Location ptLoc = eyeLoc.clone().add(pointOffsets.get(i));
                        ParticleEffect.CRIT.display(ptLoc, 3, 0.1, 0.1, 0.1, 0.05);
                        ParticleEffect.BLOCK_CRACK.display(ptLoc, 3, 0.1, 0.1, 0.1, 0.05,
                                Material.STONE.createBlockData());
                    }
                }

                if (!flyingBlocks.isEmpty() && tickCounter % 2 == 0) {
                    flyingBlocks = Methods.BendableBlocksAnimation(flyingBlocks, player.getLocation().clone(),
                            Material.STONE, 0.8);
                }

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                        "§6[BoulderRoll] Zbieranie głazu: §e" + collectedCount + "/4 §7(Najeźdź cel i naciśnij F/LPM)"));
            } else if (state == State.FULLY_CHARGED) {
                ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 5, 0.4, 0.2, 0.4, 0.1,
                        Material.DIRT.createBlockData());
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText("§aGłaz załadowany! Puść SHIFT, aby go przetoczyć!"));
            }
        } else if (state == State.ROLLING) {
            processRolling();
        }
    }

    private void startRolling() {
        state = State.ROLLING;
        projectileLoc = player.getLocation().clone();
        rollDir = player.getLocation().getDirection().setY(0).normalize();
        if (rollDir.lengthSquared() < 0.01) {
            rollDir = player.getLocation().getDirection().normalize();
        }
        initialYaw = player.getLocation().getYaw();
        initialPitch = player.getLocation().getPitch();
        initialSlot = player.getInventory().getHeldItemSlot();

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 300, 1, false, false));
        player.getWorld().playSound(projectileLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
    }

    private void processRolling() {
        distanceTraveled += speed;
        projectileLoc.add(rollDir.clone().multiply(speed));

        boulderLoc = projectileLoc.clone().subtract(rollDir.clone().multiply(2.0));

        Location aheadLoc = projectileLoc.clone().add(rollDir.clone().multiply(1.0));
        Block aheadBlock = aheadLoc.getBlock();
        Block aheadBelow = aheadLoc.clone().subtract(0, 1, 0).getBlock();

        if (aheadBlock.getType().isSolid() && !TempBlock.isTempBlock(aheadBlock)) {
            projectileLoc.setY(projectileLoc.getY() + 1);
        } else if (!aheadBelow.getType().isSolid() && !TempBlock.isTempBlock(aheadBelow)) {
            projectileLoc.setY(projectileLoc.getY() - 1);
        }

        Block wallAhead = aheadLoc.clone().add(0, 1, 0).getBlock();
        if (distanceTraveled >= range || (wallAhead.getType().isSolid() && !TempBlock.isTempBlock(wallAhead) && !isEarthbendable(player, wallAhead))) {
            explodeAndFinish();
            return;
        }

        for (TempBlock tb : activeTempBlocks) {
            tb.revertBlock();
        }
        activeTempBlocks.clear();

        Location center = boulderLoc.clone().add(0, 0.75, 0);
        Random rand = new Random();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (loc.distance(center) <= 1.25) {
                        Block b = loc.getBlock();
                        if (b.getType() == Material.AIR || isEarthbendable(player, b) || PlantAbility.isPlant(b)) {
                            Material mat = rand.nextBoolean() ? Material.DIRT : Material.STONE;
                            TempBlock tb = new TempBlock(b, mat);
                            tb.setRevertTime(100L);
                            activeTempBlocks.add(tb);
                        }
                    }
                }
            }
        }

        double rollAngle = distanceTraveled * 3.0;
        for (double phi = 0; phi <= Math.PI; phi += Math.PI / 3) {
            for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 3) {
                double x = radius * Math.sin(phi) * Math.cos(theta + rollAngle);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta + rollAngle);
                Location pLoc = center.clone().add(x, y, z);
                ParticleEffect.BLOCK_CRACK.display(pLoc, 2, 0.05, 0.05, 0.05, 0.02, Material.DIRT.createBlockData());
                ParticleEffect.BLOCK_CRACK.display(pLoc, 2, 0.05, 0.05, 0.05, 0.02, Material.STONE.createBlockData());
            }
        }

        if (tickCounter % 3 == 0) {
            Location spawnLoc = center.clone().add((rand.nextDouble() - 0.5) * 1.5, (rand.nextDouble() - 0.5) * 1.5, (rand.nextDouble() - 0.5) * 1.5);
            Material fbMat = rand.nextBoolean() ? Material.STONE : Material.DIRT;
            FallingBlock fb = spawnLoc.getWorld().spawnFallingBlock(spawnLoc, fbMat.createBlockData());
            fb.setDropItem(false);
            fb.setVelocity(rollDir.clone().multiply(speed * 0.5).add(new Vector((rand.nextDouble() - 0.5) * 0.2, 0.1, (rand.nextDouble() - 0.5) * 0.2)));
            Methods.SpawnedByMe.add(fb.getUniqueId());

            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                if (fb.isValid() && !fb.isDead()) {
                    fb.remove();
                }
            }, 6L);
            boulderLoc.getWorld().playSound(boulderLoc, Sound.BLOCK_GRASS_STEP, 0.8f, 0.6f);
        }

        Location idealPlayerDest = boulderLoc.clone().subtract(rollDir.clone().multiply(3.5)).add(0, 3.5, 0);

        Location rayStart = boulderLoc.clone().add(0, 0.75, 0);
        Vector ray = idealPlayerDest.toVector().subtract(rayStart.toVector());
        double dist = ray.length();
        if (dist > 0.1) {
            RayTraceResult rtr = rayStart.getWorld().rayTraceBlocks(
                    rayStart, ray.clone().normalize(), dist,
                    FluidCollisionMode.NEVER, true);
            if (rtr != null && rtr.getHitPosition() != null && rtr.getHitBlock() != null
                    && rtr.getHitBlock().getType().isSolid() && !TempBlock.isTempBlock(rtr.getHitBlock())) {
                idealPlayerDest = rtr.getHitPosition().toLocation(rayStart.getWorld())
                        .subtract(ray.clone().normalize().multiply(0.4));
            }
        }

        Location playerDest = idealPlayerDest;
        Vector lookDir = center.toVector().subtract(playerDest.toVector());
        playerDest.setDirection(lookDir);

        player.teleport(playerDest);
        player.setVelocity(new Vector(0, 0, 0));
        player.getInventory().setHeldItemSlot(initialSlot);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 1, false, false));

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projectileLoc, radius + 0.5)) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                DamageHandler.damageEntity(entity, damage, this);
                Vector kb = rollDir.clone().multiply(knockback).setY(0.35);
                entity.setVelocity(kb);
            }
        }
    }

    private void explodeAndFinish() {
        for (TempBlock tb : activeTempBlocks) {
            tb.revertBlock();
        }
        activeTempBlocks.clear();

        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Location finishLoc = projectileLoc.clone().add(0, 0.5, 0);
        finishLoc.setYaw(initialYaw);
        finishLoc.setPitch(initialPitch);
        player.teleport(finishLoc);

        Vector momentumPush = rollDir.clone().multiply(1.1).setY(0.4);
        player.setVelocity(momentumPush);

        Methods.spawnFallingBlocks(finishLoc, Material.DIRT, 10, 2.0, player);
        Methods.spawnFallingBlocks(finishLoc, Material.STONE, 8, 2.0, player);

        ParticleEffect.EXPLOSION_LARGE.display(finishLoc, 2, 0.5, 0.5, 0.5, 0.1);
        ParticleEffect.BLOCK_CRACK.display(finishLoc, 40, 1.5, 1.5, 1.5, 0.2, Material.DIRT.createBlockData());
        finishLoc.getWorld().playSound(finishLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

        bPlayer.addCooldown(this, cooldown);
        if (AmonPackPlugin.ENABLE_SKILL_TREE) {
            SpecialTriggerManager.unregisterActiveSpecial(player);
        }
        remove();
    }

    private void cancelAbility() {
        for (TempBlock tb : activeTempBlocks) {
            tb.revertBlock();
        }
        activeTempBlocks.clear();

        if (player != null) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        if (bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
        }
        if (AmonPackPlugin.ENABLE_SKILL_TREE) {
            SpecialTriggerManager.unregisterActiveSpecial(player);
        }
        remove();
    }

    public boolean isRolling() {
        return state == State.ROLLING;
    }

    @Override
    public TriggerType getSupportedTriggerType() {
        return TriggerType.SWAP;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return boulderLoc != null ? boulderLoc : player.getLocation();
    }

    @Override
    public String getName() {
        return "BoulderRoll";
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
        cancelAbility();
    }

    @Override
    public String getDescription() {
        return "Gromadzi odłamki ziemne, aby utworzyć wielki toczący się głaz. Gracz podąża z tyłu głazu z widokiem góry.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj SHIFT, najedź na 4 punkty kalibracji i naciśnij F/LPM, a następnie puść SHIFT!";
    }
}
