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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

        if (!player.isSneaking()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§cTego ruchu można użyć tylko kucając!"));
            return;
        }

        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.BoulderRoll.Cooldown", 8000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.BoulderRoll.Damage", 5.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.BoulderRoll.Knockback", 1.2);
        this.range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.BoulderRoll.Range", 22);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.BoulderRoll.Speed", 0.7);
        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.BoulderRoll.Radius", 1.5);
        this.revertTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.BoulderRoll.RevertTime", 10000L);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        this.state = State.CHARGING;
        SpecialTriggerManager.registerActiveSpecial(player);
        initTargetPoints();
        start();
    }

    private void initTargetPoints() {
        Location eyeLoc = player.getEyeLocation();
        Vector forward = eyeLoc.getDirection().normalize().multiply(3.5);
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        if (right.lengthSquared() < 0.01) {
            right = new Vector(1, 0, 0);
        }
        Vector up = right.clone().crossProduct(forward).normalize();

        Random rand = new Random();
        pointOffsets.add(forward.clone().add(right.clone().multiply(-1.0 + (rand.nextDouble() * 0.4))).add(up.clone().multiply(0.8 + (rand.nextDouble() * 0.3))));
        pointOffsets.add(forward.clone().add(right.clone().multiply(1.0 - (rand.nextDouble() * 0.4))).add(up.clone().multiply(0.7 + (rand.nextDouble() * 0.3))));
        pointOffsets.add(forward.clone().add(right.clone().multiply(-0.9 + (rand.nextDouble() * 0.4))).add(up.clone().multiply(-0.6 - (rand.nextDouble() * 0.3))));
        pointOffsets.add(forward.clone().add(right.clone().multiply(0.9 - (rand.nextDouble() * 0.4))).add(up.clone().multiply(-0.7 - (rand.nextDouble() * 0.3))));
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

                if (angle < 25.0 && angle < minAngle) {
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
                if (b.getY() <= player.getLocation().getY() + 1 && isEarthbendable(player, b) && b.getType() != Material.AIR) {
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
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§cRuch anulowany - przestałeś kucać!"));
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
                        ParticleEffect.BLOCK_CRACK.display(ptLoc, 3, 0.1, 0.1, 0.1, 0.05, Material.STONE.createBlockData());
                    }
                }

                if (!flyingBlocks.isEmpty() && tickCounter % 2 == 0) {
                    flyingBlocks = Methods.BendableBlocksAnimation(flyingBlocks, player.getLocation().clone(), Material.STONE, 0.8);
                }

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§6[BoulderRoll] Zbieranie głazu: §e" + collectedCount + "/4 §7(Najeźdź cel i naciśnij F/L)"));
            } else if (state == State.FULLY_CHARGED) {
                ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 5, 0.4, 0.2, 0.4, 0.1, Material.DIRT.createBlockData());
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§aGłaz załadowany! Puść SHIFT, aby go przetoczyć!"));
            }
        } else if (state == State.ROLLING) {
            processRolling();
        }
    }

    private void startRolling() {
        state = State.ROLLING;
        boulderLoc = player.getLocation().clone();
        rollDir = player.getLocation().getDirection().setY(0).normalize();
        if (rollDir.lengthSquared() < 0.01) {
            rollDir = player.getLocation().getDirection().normalize();
        }
        initialYaw = player.getLocation().getYaw();
        initialPitch = player.getLocation().getPitch();
        initialSlot = player.getInventory().getHeldItemSlot();

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 300, 1, false, false));
        player.getWorld().playSound(boulderLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
    }

    private void processRolling() {
        distanceTraveled += speed;
        boulderLoc.add(rollDir.clone().multiply(speed));

        Block currentBlock = boulderLoc.getBlock();
        if (currentBlock.getType() != Material.AIR && !PlantAbility.isPlant(currentBlock)) {
            boulderLoc.setY(boulderLoc.getY() + 1);
        }
        Block belowBlock = boulderLoc.clone().subtract(0, 1, 0).getBlock();
        if (belowBlock.getType() == Material.AIR || PlantAbility.isPlant(belowBlock)) {
            boulderLoc.setY(boulderLoc.getY() - 1);
        }

        Block obstacleBlock = boulderLoc.clone().add(0, 1, 0).getBlock();
        if (distanceTraveled >= range || (!isEarthbendable(player, obstacleBlock) && obstacleBlock.getType() != Material.AIR && !PlantAbility.isPlant(obstacleBlock))) {
            explodeAndFinish();
            return;
        }

        // 1. Stwórz w środku kuli poruszającą się kulę dirtu i stone (tempbloki z czas revertTime 100ms / 50ms) w promieniu 1 od środka
        Location center = boulderLoc.clone().add(0, 0.5, 0);
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
                            tb.setRevertTime(100L); // 100ms revert time dla efektu poruszającej się stałej kuli
                        }
                    }
                }
            }
        }

        // 2. W obrębie okręgu o rozmiarze 3 (promień 1.5) rozproszone fallingblocki tworzące dynamiczny efekt toczącego się głazu
        if (tickCounter % 2 == 0) {
            for (int i = 0; i < 3; i++) {
                double offsetX = (rand.nextDouble() - 0.5) * 3.0; // w obrębie okręgu/kuli o rozmiarze 3
                double offsetY = (rand.nextDouble() - 0.5) * 2.0 + 0.5;
                double offsetZ = (rand.nextDouble() - 0.5) * 3.0;
                Location spawnLoc = center.clone().add(offsetX, offsetY, offsetZ);

                Material fbMat = rand.nextInt(3) == 0 ? Material.STONE : (rand.nextBoolean() ? Material.DIRT : Material.COARSE_DIRT);
                org.bukkit.entity.FallingBlock fb = spawnLoc.getWorld().spawnFallingBlock(spawnLoc, fbMat.createBlockData());
                fb.setDropItem(false);

                Vector vel = rollDir.clone().multiply(speed * 0.8)
                        .add(new Vector((rand.nextDouble() - 0.5) * 0.3, 0.15 + (rand.nextDouble() * 0.15), (rand.nextDouble() - 0.5) * 0.3));
                fb.setVelocity(vel);

                Methods.SpawnedByMe.add(fb.getUniqueId());

                org.bukkit.Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                    if (fb.isValid() && !fb.isDead()) {
                        ParticleEffect.BLOCK_CRACK.display(fb.getLocation(), 4, 0.2, 0.2, 0.2, 0.05, fbMat.createBlockData());
                        fb.remove();
                    }
                }, 10L);
            }
            boulderLoc.getWorld().playSound(boulderLoc, Sound.BLOCK_GRASS_STEP, 0.8f, 0.6f);
        }

        double angle = distanceTraveled * 3.0;
        for (double i = 0; i < Math.PI * 2; i += Math.PI / 4) {
            double x = radius * Math.cos(i + angle);
            double z = radius * Math.sin(i + angle);
            double y = radius * Math.sin(i * 2);
            Location pLoc = boulderLoc.clone().add(x, y + 0.7, z);
            ParticleEffect.BLOCK_CRACK.display(pLoc, 2, 0.1, 0.1, 0.1, 0.05, Material.DIRT.createBlockData());
            ParticleEffect.BLOCK_CRACK.display(pLoc, 2, 0.1, 0.1, 0.1, 0.05, Material.STONE.createBlockData());
        }

        Location playerDest = boulderLoc.clone().add(0, 1.8, 0);
        playerDest.setYaw(initialYaw);
        playerDest.setPitch(initialPitch);
        player.teleport(playerDest);
        player.setVelocity(new Vector(0, 0, 0));
        player.getInventory().setHeldItemSlot(initialSlot);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 1, false, false));

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(boulderLoc, radius + 0.5)) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                DamageHandler.damageEntity(entity, damage, this);
                Vector kb = rollDir.clone().multiply(knockback).setY(0.35);
                entity.setVelocity(kb);
            }
        }
    }

    private void explodeAndFinish() {
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Location finishLoc = boulderLoc.clone().add(0, 0.5, 0);
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
        SpecialTriggerManager.unregisterActiveSpecial(player);
        remove();
    }

    private void cancelAbility() {
        if (player != null) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        if (bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
        }
        SpecialTriggerManager.unregisterActiveSpecial(player);
        remove();
    }

    public boolean isRolling() {
        return state == State.ROLLING;
    }

    @Override
    public TriggerType getSupportedTriggerType() {
        return TriggerType.BOTH;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return boulderLoc != null ? boulderLoc : (player != null ? player.getLocation() : null);
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
        return "1.0";
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
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Pozwala na zebranie 4 punktów ziemi przed sobą podczas kucania, a następnie zamianę w toczący się głaz z odrzutem i obrażeniami.";
    }

    @Override
    public String getInstructions() {
        return "Kucaj i aktywuj F/L aby zbierać punkty. Po zebraniu 4 punktów puść SHIFT aby przetoczyć głaz!";
    }
}
