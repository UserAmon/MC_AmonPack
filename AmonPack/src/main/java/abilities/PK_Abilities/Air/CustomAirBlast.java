package Abilities.PK_Abilities.Air;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.airbending.AirBurst;
import com.projectkorra.projectkorra.object.HorizontalVelocityTracker;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomAirBlast extends AirAbility implements AddonAbility {

    private static class ChargeTracker {
        int chargesUsed = 0;
        long windowStartTime = 0;
    }

    public static class OriginData {
        public final Location location;
        public final long timestamp;

        public OriginData(Location location, long timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }
    }

    private static final Map<UUID, OriginData> ORIGINS = new ConcurrentHashMap<>();
    private static final Map<UUID, ChargeTracker> CHARGE_TRACKERS = new ConcurrentHashMap<>();
    private static boolean originTaskRunning = false;

    private long cooldown;
    private int maxUses;
    private long resetWindowMs;
    private double range;
    private double speed;
    private double radius;
    private double pushSelf;
    private double pushEntities;
    private double damage;
    private int particles;
    private int selectParticles;
    private double selectRange;
    private boolean canFlickLevers;
    private boolean canOpenDoors;
    private boolean canPressButtons;
    private boolean canCoolLava;

    private Location location;
    private Location origin;
    private Vector direction;
    private double speedFactor;
    private boolean isFromOtherOrigin;
    private AirBurst source;
    private final ArrayList<Block> affectedLevers = new ArrayList<>();
    private final ArrayList<Entity> affectedEntities = new ArrayList<>();
    private final Random random = new Random();

    public CustomAirBlast(Player player) {
        super(player);

        if (bPlayer.isOnCooldown("AirBlast") || !bPlayer.canBendIgnoreCooldowns(this)) {
            return;
        }

        loadConfig();

        ChargeTracker tracker = CHARGE_TRACKERS.computeIfAbsent(player.getUniqueId(), k -> new ChargeTracker());
        long now = System.currentTimeMillis();

        if (tracker.chargesUsed == 0 || (now - tracker.windowStartTime > resetWindowMs)) {
            tracker.chargesUsed = 0;
            tracker.windowStartTime = now;
        }

        tracker.chargesUsed++;
        int remaining = Math.max(0, maxUses - tracker.chargesUsed);

        if (remaining > 0) {
            double timeLeft = Math.max(0.0, (resetWindowMs - (now - tracker.windowStartTime)) / 1000.0);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(String.format("§f💨 §lAIR BLAST: §a[ %d / %d ] §7(Czas: §e%.1fs§7)",
                            remaining, maxUses, timeLeft)));
        } else {
            bPlayer.addCooldown("AirBlast", cooldown);
            tracker.chargesUsed = 0;
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§f💨 §lAIR BLAST: §c[ COOLDOWN ]"));
        }

        final int currentCount = tracker.chargesUsed;
        final long currentWindowStart = tracker.windowStartTime;
        new BukkitRunnable() {
            @Override
            public void run() {
                ChargeTracker currentTracker = CHARGE_TRACKERS.get(player.getUniqueId());
                if (currentTracker != null && currentTracker.windowStartTime == currentWindowStart
                        && currentTracker.chargesUsed > 0 && currentTracker.chargesUsed < maxUses) {
                    if (System.currentTimeMillis() - currentTracker.windowStartTime >= resetWindowMs) {
                        currentTracker.chargesUsed = 0;
                        if (!bPlayer.isOnCooldown("AirBlast")) {
                            bPlayer.addCooldown("AirBlast", cooldown);
                            if (player.isOnline()) {
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                        TextComponent.fromLegacyText("§f💨 §lAIR BLAST: §c[ COOLDOWN ]"));
                            }
                        }
                    }
                }
            }
        }.runTaskLater(AmonPackPlugin.plugin, (resetWindowMs / 50L) + 1L);

        OriginData originData = ORIGINS.remove(player.getUniqueId());
        if (originData != null && originData.location != null
                && originData.location.getWorld().equals(player.getWorld())) {
            this.origin = originData.location;

            Entity targetEntity = GeneralMethods.getTargetedEntity(player, range);
            Location targetLoc = targetEntity != null ? targetEntity.getLocation()
                    : GeneralMethods.getTargetedLocation(player, range);
            if (targetLoc.distanceSquared(this.origin) < 0.01) {
                this.direction = player.getEyeLocation().getDirection().normalize();
            } else {
                this.direction = GeneralMethods.getDirection(this.origin, targetLoc).normalize();
            }
            this.location = this.origin.clone();
            this.isFromOtherOrigin = true;
        } else {
            this.origin = player.getEyeLocation();
            this.direction = player.getEyeLocation().getDirection().normalize();
            this.location = this.origin.clone();
            this.isFromOtherOrigin = false;
        }

        this.speedFactor = speed * (ProjectKorra.time_step / 1000.0);

        start();
        ensureOriginTask();
    }

    public CustomAirBlast(Player player, Location origin, Vector direction, double pushFactor, AirBurst source) {
        super(player);
        loadConfig();
        this.origin = origin.clone();
        this.location = origin.clone();
        this.direction = direction.clone().normalize();
        this.pushEntities = pushFactor;
        this.pushSelf = pushFactor;
        this.source = source;
        this.speedFactor = speed * (ProjectKorra.time_step / 1000.0);
        start();
    }

    private void loadConfig() {
        this.maxUses = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Air.AirBlast.MaxUses", 4);
        this.resetWindowMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirBlast.ResetWindow", 4000L);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirBlast.Cooldown", 3000L);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirBlast.Range", 25.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirBlast.Speed", 25.0);
        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirBlast.Radius", 2.0);
        this.pushSelf = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirBlast.PushSelf", 2.5);
        this.pushEntities = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirBlast.PushEntities", 2.5);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirBlast.Damage", 0.0);
        this.particles = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Air.AirBlast.Particles", 4);
        this.selectParticles = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Air.AirBlast.SelectParticles", 4);
        this.selectRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirBlast.SelectRange", 10.0);
        this.canFlickLevers = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Air.AirBlast.CanFlickLevers",
                true);
        this.canOpenDoors = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Air.AirBlast.CanOpenDoors", true);
        this.canPressButtons = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Air.AirBlast.CanPressButtons",
                true);
        this.canCoolLava = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Air.AirBlast.CanCoolLava", true);
    }

    public static void setOrigin(Player player) {
        if (player == null || !player.isOnline() || player.isDead())
            return;
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null || !bPlayer.canBendIgnoreCooldowns(CoreAbility.getAbility(CustomAirBlast.class))) {
            return;
        }

        double selectRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirBlast.SelectRange", 10.0);
        Location target = GeneralMethods.getTargetedLocation(player, selectRange);
        if (target == null)
            return;

        if (RegionProtection.isRegionProtected(player, target, "AirBlast")) {
            return;
        }

        ORIGINS.put(player.getUniqueId(), new OriginData(target, System.currentTimeMillis()));

        spawnAirParticles(target, 6, 0.8, 0.8, 0.8);
        player.playSound(target, Sound.ENTITY_HORSE_BREATHE, 0.7f, 1.6f);
        ensureOriginTask();
    }

    private static void ensureOriginTask() {
        if (!originTaskRunning && AmonPackPlugin.plugin != null) {
            originTaskRunning = true;
            new BukkitRunnable() {
                @Override
                public void run() {
                    progressOrigins();
                    if (ORIGINS.isEmpty()) {
                        originTaskRunning = false;
                        cancel();
                    }
                }
            }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);
        }
    }

    public static void progressOrigins() {
        long now = System.currentTimeMillis();
        long maxDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirBlast.OriginDuration", 8000L);
        double selectRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirBlast.SelectRange", 10.0);
        double maxDistSq = Math.pow(selectRange + 15.0, 2);

        Iterator<Map.Entry<UUID, OriginData>> it = ORIGINS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, OriginData> entry = it.next();
            UUID uuid = entry.getKey();
            OriginData data = entry.getValue();
            Player p = Bukkit.getPlayer(uuid);

            if (p == null || !p.isOnline() || p.isDead()) {
                it.remove();
                continue;
            }

            if (now - data.timestamp > maxDuration) {
                it.remove();
                continue;
            }

            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(p);
            if (bPlayer == null || !"AirBlast".equalsIgnoreCase(bPlayer.getBoundAbilityName())) {
                it.remove();
                continue;
            }

            Location loc = data.location;
            if (loc.getWorld().equals(p.getWorld()) && loc.distanceSquared(p.getLocation()) <= maxDistSq) {
                spawnAirParticles(loc, 6, 0.8, 0.8, 0.8);
            } else {
                it.remove();
            }
        }
    }

    private static void spawnAirParticles(Location loc, int amount, double ox, double oy, double oz) {
        if (loc == null || loc.getWorld() == null)
            return;
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, amount, ox, oy, oz, 0.01);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, Math.max(1, amount / 2), ox, oy, oz, 0.02);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (RegionProtection.isRegionProtected(player, location, "AirBlast")) {
            remove();
            return;
        }

        advanceLocation();
    }

    private void advanceLocation() {
        if (location.distanceSquared(origin) > range * range) {
            remove();
            return;
        }

        spawnAirParticles(location, particles, 0.15, 0.15, 0.15);
        if (random.nextInt(4) == 0) {
            playAirbendingSound(location);
        }

        processBlock(location);

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, radius)) {
            affect(entity);
        }

        Location nextLoc = location.clone().add(direction.clone().multiply(speedFactor));
        if (nextLoc.getBlock().getType().isSolid() && !nextLoc.getBlock().isPassable()) {
            remove();
            return;
        }

        this.location = nextLoc;
    }

    private void affect(Entity entity) {
        if (affectedEntities.contains(entity)) {
            return;
        }

        boolean isUser = entity.getUniqueId().equals(player.getUniqueId());

        if (isUser) {
            if (isFromOtherOrigin) {
                Vector vel = direction.clone().multiply(pushSelf);
                GeneralMethods.setVelocity(this, player, vel);
                affectedEntities.add(player);
            }
        } else {
            affectedEntities.add(entity);
            Vector vel = direction.clone().multiply(pushEntities);
            GeneralMethods.setVelocity(this, entity, vel);

            if (entity instanceof LivingEntity le) {
                if (damage > 0) {
                    DamageHandler.damageEntity(le, damage, this);
                }
                le.setFireTicks(0);
                le.getWorld().playEffect(le.getLocation(), Effect.EXTINGUISH, 0);
            }
            new HorizontalVelocityTracker(entity, player, 200L, this);
        }
    }

    private void processBlock(Location loc) {
        Block block = loc.getBlock();

        // Extinguish fire
        if (block.getType() == Material.FIRE || block.getType() == Material.SOUL_FIRE) {
            block.setType(Material.AIR);
            block.getWorld().playEffect(block.getLocation(), Effect.EXTINGUISH, 0);
        }

        // Extinguish candles/campfires
        if (block.getBlockData() instanceof Lightable lightable && lightable.isLit()) {
            lightable.setLit(false);
            block.setBlockData(lightable);
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 1.0f, 1.0f);
        }

        // Cool lava
        if (canCoolLava && block.getType() == Material.LAVA) {
            if (block.getBlockData() instanceof Levelled levelled && levelled.getLevel() == 0) {
                new TempBlock(block, Material.OBSIDIAN);
            } else {
                new TempBlock(block, Material.COBBLESTONE);
            }
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 1.2f);
        }

        // Open wooden doors / trapdoors
        if (canOpenDoors) {
            if (block.getBlockData() instanceof Door door && !door.isOpen()) {
                door.setOpen(true);
                block.setBlockData(door);
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1.0f, 1.0f);
            } else if (block.getBlockData() instanceof TrapDoor trapDoor && !trapDoor.isOpen()) {
                trapDoor.setOpen(true);
                block.setBlockData(trapDoor);
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 1.0f, 1.0f);
            }
        }

        // Flick levers / press buttons
        if (canFlickLevers && block.getBlockData() instanceof Switch sw && !sw.isPowered()) {
            sw.setPowered(true);
            block.setBlockData(sw);
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.0f);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (block.getBlockData() instanceof Switch currentSw) {
                        currentSw.setPowered(false);
                        block.setBlockData(currentSw);
                    }
                }
            }.runTaskLater(AmonPackPlugin.plugin, 30L);
        }
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return "AirBlast";
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
        return "2.0";
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
        return "Wystrzeliwujesz potężny podmuch wiatru (LPM). Kucnięcie (Shift) oznacza punkt początkowy powiewu. Posiadasz 4 użycia w serii zanim umiejętność wejdzie na cooldown!";
    }

    @Override
    public String getInstructions() {
        return "LPM: Wystrzel AirBlast. Shift: Zaznacz punkt skąd wiatr wystrzeli (pozwala na self-push!).";
    }
}
