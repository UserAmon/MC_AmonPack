package Abilities.PK_Abilities.Earth;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Burrow extends EarthAbility implements AddonAbility {

    private enum State {
        CHARGING, BURROW_AREA, TRAVELING
    }

    private State state;
    private long startTime;
    private long chargeTime;
    private double radius;
    private int burrowDepth;
    private long duration;
    private double travelSpeed;
    private long cooldown;

    private float startYaw;
    private float startPitch;

    private double ringAngle = 0;
    private final List<LivingEntity> detectedTargets = new ArrayList<>();
    private final List<TempBlock> tempBlocks = new ArrayList<>();

    public Burrow(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Burrow.ChargeTime", 1500);
        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Burrow.Radius", 6.0);
        this.burrowDepth = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Burrow.BurrowDepth", 2);
        this.duration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Burrow.Duration", 4000);
        this.travelSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Burrow.TravelSpeed", 0.6);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Burrow.Cooldown", 8000);

        this.startYaw = player.getLocation().getYaw();
        this.startPitch = player.getLocation().getPitch();

        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();

        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            revertTempBlocks();
            remove();
            return;
        }

        if (state == State.CHARGING) {
            long elapsed = System.currentTimeMillis() - startTime;

            if (!player.isSneaking()) {
                if (elapsed >= chargeTime) {
                    executeBurrowArea();
                } else {
                    remove();
                }
                return;
            }

            renderChargeRings(elapsed);
            scanTargets();
        } else if (state == State.TRAVELING) {
            player.setNoDamageTicks(60);
        }
    }

    private void renderChargeRings(long elapsed) {
        ringAngle += 0.2;
        double progress = Math.min(1.0, (double) elapsed / chargeTime);
        double ringRadius = 0.8 + (progress * 0.6);

        Location playerFeet = player.getLocation();
        renderEarthRing(playerFeet, ringRadius);

        for (LivingEntity target : detectedTargets) {
            if (target != null && target.isValid()) {
                renderEarthRing(target.getLocation(), ringRadius * 0.8);
            }
        }
    }

    private void renderEarthRing(Location center, double r) {
        int points = 16;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i + ringAngle;
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;
            Location pt = center.clone().add(x, 0.1, z);
            center.getWorld().spawnParticle(Particle.FALLING_DUST, pt, 2, 0.02, 0.02, 0.02, 0.01,
                    Material.DIRT.createBlockData());
        }
    }

    private void scanTargets() {
        detectedTargets.clear();
        for (Entity e : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), radius)) {
            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                Block under = le.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
                if (isEarthbendable(under) || isEarthbendable(le.getLocation().getBlock())) {
                    detectedTargets.add(le);
                }
            }
        }
    }

    private void executeBurrowArea() {
        state = State.BURROW_AREA;
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.2f, 0.7f);

        for (LivingEntity target : new ArrayList<>(detectedTargets)) {
            if (target != null && target.isValid()) {
                Location loc = target.getLocation();
                Block b1 = loc.getBlock();
                Block b2 = loc.getBlock().getRelative(org.bukkit.block.BlockFace.UP);

                Location buriedLoc = loc.clone().add(0, -burrowDepth, 0);
                target.teleport(buriedLoc);

                if (isEarthbendable(b1)) {
                    tempBlocks.add(new TempBlock(b1, Material.AIR.createBlockData(), duration));
                }
                if (isEarthbendable(b2)) {
                    tempBlocks.add(new TempBlock(b2, Material.AIR.createBlockData(), duration));
                }

                target.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 20, 0.5, 0.5, 0.5, 0.1,
                        Material.DIRT.createBlockData());
            }
        }

        bPlayer.addCooldown(this, cooldown);

        new BukkitRunnable() {
            @Override
            public void run() {
                revertTempBlocks();
                remove();
            }
        }.runTaskLater(AmonPackPlugin.plugin, duration / 50L);
    }

    public void onClick() {
        if (state != State.CHARGING)
            return;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed < chargeTime) {
            return;
        }

        Block targetBlock = player.getTargetBlockExact(25);
        if (targetBlock == null || !isEarthbendable(targetBlock)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacy("§cBrak połączenia z podłożem"));
            return;
        }

        Block startBlock = player.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
        if (!isEarthbendable(startBlock)) {
            startBlock = player.getLocation().getBlock();
        }

        List<Location> tunnelPath = find3DEarthPath(startBlock, targetBlock);
        if (tunnelPath == null || tunnelPath.isEmpty()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacy("§cBrak połączenia z podłożem"));
            return;
        }

        executeBurrowTravel(tunnelPath);
    }

    private List<Location> find3DEarthPath(Block start, Block target) {
        Location startLoc = start.getLocation().add(0.5, 0.5, 0.5);
        Location targetLoc = target.getLocation().add(0.5, 0.5, 0.5);

        double dist = startLoc.distance(targetLoc);
        int steps = (int) (dist / 0.5);

        List<Location> path = new ArrayList<>();
        Vector dir = targetLoc.toVector().subtract(startLoc.toVector()).normalize().multiply(0.5);

        Location current = startLoc.clone();
        for (int i = 0; i <= steps; i++) {
            Block b = current.getBlock();
            if (!isEarthbendable(b) && !isEarthbendable(b.getRelative(org.bukkit.block.BlockFace.DOWN))
                    && !isEarthbendable(b.getRelative(org.bukkit.block.BlockFace.UP))) {
                return null; // Connection broken!
            }
            path.add(current.clone());
            current.add(dir);
        }
        return path;
    }

    private void executeBurrowTravel(List<Location> path) {
        state = State.TRAVELING;
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.2f, 0.6f);

        // Apply blindness during tunnel travel
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false));

        bPlayer.addCooldown(this, cooldown);

        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index >= path.size() || player == null || !player.isOnline()) {
                    emergePlayer(path.get(path.size() - 1));
                    cancel();
                    return;
                }

                player.setNoDamageTicks(60);

                Location surfaceLoc = path.get(index);
                // Teleport player 4 blocks underground with preserved look direction
                Location undergroundLoc = surfaceLoc.clone().add(0, -4.0, 0);
                undergroundLoc.setYaw(startYaw);
                undergroundLoc.setPitch(startPitch);

                Block b = undergroundLoc.getBlock();
                if (isEarthbendable(b) || b.getType().isSolid()) {
                    tempBlocks.add(new TempBlock(b, Material.AIR.createBlockData(), 3000));
                }

                player.teleport(undergroundLoc);
                player.setFallDistance(0);

                surfaceLoc.getWorld().spawnParticle(Particle.FALLING_DUST, surfaceLoc, 6, 0.3, 0.3, 0.3, 0.05,
                        Material.DIRT.createBlockData());
                surfaceLoc.getWorld().playSound(surfaceLoc, Sound.BLOCK_GRAVEL_STEP, 0.5f, 0.6f);

                index++;
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void emergePlayer(Location targetLoc) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        Location exitLoc = targetLoc.clone().add(0, 1.5, 0);
        exitLoc.setYaw(startYaw);
        exitLoc.setPitch(startPitch);

        player.teleport(exitLoc);
        player.setVelocity(new Vector(0, 0.5, 0));

        exitLoc.getWorld().playSound(exitLoc, Sound.BLOCK_GRAVEL_BREAK, 1.2f, 1.2f);
        exitLoc.getWorld().spawnParticle(Particle.FALLING_DUST, exitLoc, 35, 0.6, 0.6, 0.6, 0.15,
                Material.DIRT.createBlockData());

        new BukkitRunnable() {
            @Override
            public void run() {
                revertTempBlocks();
                remove();
            }
        }.runTaskLater(AmonPackPlugin.plugin, 10L);
    }

    private void revertTempBlocks() {
        for (TempBlock tb : new ArrayList<>(tempBlocks)) {
            if (tb != null) {
                tb.revertBlock();
            }
        }
        tempBlocks.clear();
    }

    @Override
    public void remove() {
        if (player != null && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
        revertTempBlocks();
        super.remove();
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
        return "Burrow";
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

    @Override
    public String getDescription() {
        return "Zaawansowany ruch magów ziemie. Pozwala zakopać pobliskie cele pod ziemią lub samemu przemieszczać się pod ziemią.";
    }

    @Override
    public String getInstructions() {
        return "Kucnij (Shift) aby naładować ruch. Puszczenie shifta zakopie pod ziemią pobliskie cele. Kliknięcie LPM natomiast sprawi że przejdziesz pod ziemią w kierunku wybranego bloku.";
    }
}
