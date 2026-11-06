package RPG.BattleRoyale.Events;

import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.BattleRoyaleArena;
import RPG.BattleRoyale.BattleRoyaleGame;
import RPG.BattleRoyale.Loot.BattleRoyaleLootManager;
import RPG.BattleRoyale.Noise.NoiseEvent;
import RPG.BattleRoyale.Zombies.CustomZombieManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Zarządza dynamicznymi wydarzeniami w trakcie trwania meczu:
 * 1. Air Drop (Zrzut zaopatrzenia z dymem i elitarnym łupem)
 * 2. Blackout (Lokalna awaria zasilania w sektorze miasta)
 * 3. Blood Moon (Krwawy Księżyc - wzmocnienie zombie, BossBar i czerwona atmosfera)
 * 4. Car Alarm (Alarm samochodowy wzywający hordy)
 */
public class DynamicEventManager {

    private final BattleRoyaleGame game;
    private final Random random = new Random();

    // Konfiguracja
    private boolean enabled = true;
    private int minimumGameTimeSeconds = 180;
    private int cooldownSeconds = 120;

    private double airDropChance = 0.25;
    private int airDropCooldown = 300;

    private double blackoutChance = 0.20;
    private int blackoutDuration = 60;
    private int blackoutCooldown = 180;

    private double bloodMoonChance = 0.15;
    private int bloodMoonDuration = 60;
    private int bloodMoonCooldown = 300;

    private double carAlarmChance = 0.30;
    private int carAlarmDuration = 45;
    private int carAlarmCooldown = 180;

    private long gameStartTimestamp = 0L;
    private long lastGlobalEventTime = 0L;
    private final Map<String, Long> lastEventTimePerType = new ConcurrentHashMap<>();

    // Stan aktywnych eventów
    // 1. Blackout
    private Location blackoutCenter = null;
    private double blackoutRadius = 55.0;
    private long blackoutEndTime = 0L;

    // 2. Blood Moon
    private boolean bloodMoonActive = false;
    private long bloodMoonEndTime = 0L;
    private BossBar bloodMoonBossBar = null;

    // 3. Alarmy samochodowe
    private final Map<Location, Long> activeCarAlarms = new ConcurrentHashMap<>();

    // 4. Zadania spadających zrzutów
    private final List<BukkitTask> activeAirDropTasks = new CopyOnWriteArrayList<>();

    public DynamicEventManager(BattleRoyaleGame game) {
        this.game = game;
    }

    public void loadFromConfig(FileConfiguration config) {
        enabled = config.getBoolean("dynamic-events.enabled", true);
        minimumGameTimeSeconds = config.getInt("dynamic-events.minimum-game-time", 180);
        cooldownSeconds = config.getInt("dynamic-events.cooldown", 120);

        airDropChance = config.getDouble("air-drop.chance", 0.25);
        airDropCooldown = config.getInt("air-drop.cooldown", 300);

        blackoutChance = config.getDouble("blackout.chance", 0.20);
        blackoutDuration = config.getInt("blackout.duration", 60);
        blackoutCooldown = config.getInt("blackout.cooldown", 180);

        bloodMoonChance = config.getDouble("blood-moon.chance", 0.15);
        bloodMoonDuration = config.getInt("blood-moon.duration", 60);
        bloodMoonCooldown = config.getInt("blood-moon.cooldown", 300);

        carAlarmChance = config.getDouble("car-alarm.chance", 0.30);
        carAlarmDuration = config.getInt("car-alarm.duration", 45);
        carAlarmCooldown = config.getInt("car-alarm.cooldown", 180);
    }

    public void start() {
        gameStartTimestamp = System.currentTimeMillis();
        lastGlobalEventTime = System.currentTimeMillis();
    }

    /**
     * Taktowanie co 1 sekundę z pętli gry.
     */
    public void tick(World world, List<Player> players, double currentRadius, Location center) {
        if (!enabled || world == null) return;
        long now = System.currentTimeMillis();
        long elapsedSeconds = (now - gameStartTimestamp) / 1000L;

        // 1. Sprawdzenie czy wylosować nowy event
        if (elapsedSeconds >= minimumGameTimeSeconds && (now - lastGlobalEventTime) >= (cooldownSeconds * 1000L)) {
            tryTriggerRandomEvent(world, currentRadius, center);
        }

        // 2. Aktualizacja aktywnego Blackoutu
        if (blackoutCenter != null) {
            tickBlackout(players, now);
        }

        // 3. Aktualizacja aktywnego Blood Moon
        if (bloodMoonActive) {
            tickBloodMoon(players, now);
        }

        // 4. Aktualizacja aktywnych alarmów samochodowych
        if (!activeCarAlarms.isEmpty()) {
            tickCarAlarms(world, now);
        }
    }

    private void tryTriggerRandomEvent(World world, double currentRadius, Location center) {
        long now = System.currentTimeMillis();
        lastGlobalEventTime = now;

        List<String> candidates = new ArrayList<>();
        if (canTrigger("airdrop", airDropCooldown)) candidates.add("airdrop");
        if (canTrigger("blackout", blackoutCooldown)) candidates.add("blackout");
        if (canTrigger("bloodmoon", bloodMoonCooldown)) candidates.add("bloodmoon");
        if (canTrigger("caralarm", carAlarmCooldown) && !game.getArena().getCarLocations().isEmpty()) candidates.add("caralarm");

        if (candidates.isEmpty()) return;
        Collections.shuffle(candidates);

        for (String type : candidates) {
            double roll = random.nextDouble();
            if (type.equals("airdrop") && roll <= airDropChance) {
                triggerAirDrop(world, currentRadius, center);
                lastEventTimePerType.put("airdrop", now);
                break;
            } else if (type.equals("blackout") && roll <= blackoutChance) {
                triggerBlackout(world, currentRadius, center);
                lastEventTimePerType.put("blackout", now);
                break;
            } else if (type.equals("bloodmoon") && roll <= bloodMoonChance) {
                triggerBloodMoon();
                lastEventTimePerType.put("bloodmoon", now);
                break;
            } else if (type.equals("caralarm") && roll <= carAlarmChance) {
                triggerRandomCarAlarm();
                lastEventTimePerType.put("caralarm", now);
                break;
            }
        }
    }

    private boolean canTrigger(String type, int cooldownSec) {
        long last = lastEventTimePerType.getOrDefault(type, 0L);
        return (System.currentTimeMillis() - last) >= (cooldownSec * 1000L);
    }

    // =========================================================================
    // 1. AIR DROP
    // =========================================================================
    public void triggerAirDrop(World world, double currentRadius, Location center) {
        if (world == null) return;

        // Wybór bezpiecznej lokalizacji wewnątrz strefy
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = random.nextDouble() * (Math.max(10.0, currentRadius * 0.7));
        double dropX = center.getX() + r * Math.cos(angle);
        double dropZ = center.getZ() + r * Math.sin(angle);

        Location floorLoc = findFloorAt(world, dropX, (int) center.getY(), dropZ);
        if (floorLoc == null) {
            floorLoc = new Location(world, dropX, center.getY() + 1.0, dropZ);
        }

        triggerAirDropAt(floorLoc);
    }

    public void triggerAirDropAt(Location targetGround) {
        if (targetGround == null || targetGround.getWorld() == null) return;
        World world = targetGround.getWorld();

        int sectorX = ((int) targetGround.getX() / 20) * 20;
        int sectorZ = ((int) targetGround.getZ() / 20) * 20;

        game.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "========================================");
        game.broadcastMessage(ChatColor.YELLOW + "  [AIR DROP] Zrzut zaopatrzenia nadlatuje!");
        game.broadcastMessage(ChatColor.GRAY + "  Szacowany sektor lądowania: X: " + sectorX + ", Z: " + sectorZ);
        game.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "========================================");

        for (Player p : world.getPlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f);
        }

        // Spadająca skrzynia od Y + 38
        final double startY = targetGround.getY() + 38.0;
        final Location spawnCrate = new Location(world, targetGround.getX(), startY, targetGround.getZ());

        ArmorStand crateStand = world.spawn(spawnCrate, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setHelmet(new ItemStack(Material.CHEST));
            s.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "📦 ZRZUT ZAOPATRZENIA");
            s.setCustomNameVisible(true);
        });

        BukkitTask task = new BukkitRunnable() {
            double currentY = startY;

            @Override
            public void run() {
                if (game.isEnded() || crateStand.isDead()) {
                    cancel();
                    return;
                }

                currentY -= 0.65; // prędkość opadania
                Location cur = new Location(world, targetGround.getX(), currentY, targetGround.getZ());
                crateStand.teleport(cur);

                // Słup dymu spadochronu
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, cur.clone().add(0, 1.8, 0), 3, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.FLAME, cur.clone().add(0, 0.2, 0), 1, 0, 0, 0, 0);

                if (currentY <= targetGround.getY()) {
                    // LĄDOWANIE!
                    cancel();
                    crateStand.remove();
                    onAirDropLanded(targetGround);
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 2L, 2L);

        activeAirDropTasks.add(task);
    }

    private void onAirDropLanded(Location groundLoc) {
        World world = groundLoc.getWorld();
        if (world == null) return;

        Block block = groundLoc.getBlock();
        game.getWorldManager().recordBlockChange(block);
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest chest) {
            game.getLootManager().populateAirDropChest(chest.getInventory());
        }

        // Efekty lądowania
        world.playSound(groundLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 1.2f);
        world.playSound(groundLoc, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.8f);
        world.spawnParticle(Particle.EXPLOSION, groundLoc.clone().add(0.5, 0.8, 0.5), 3);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, groundLoc.clone().add(0.5, 1.2, 0.5), 50, 0.4, 1.5, 0.4, 0.05);

        // Potężny impuls hałasu ściągający zombie z promienia 70 bloków
        game.getNoiseManager().recordNoise(groundLoc, 90.0, 70.0, "AIR_DROP_IMPACT", null, game.getZombieManager());

        game.broadcastMessage(ChatColor.GOLD + "[AIR DROP] " + ChatColor.GREEN + "Zrzut zaopatrzenia wylądował na koordynatach: "
                + groundLoc.getBlockX() + ", " + groundLoc.getBlockZ() + "!");
    }

    // =========================================================================
    // 2. BLACKOUT
    // =========================================================================
    public void triggerBlackout(World world, double currentRadius, Location center) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = random.nextDouble() * (Math.max(5.0, currentRadius * 0.5));
        Location pick = new Location(world, center.getX() + r * Math.cos(angle), center.getY(), center.getZ() + r * Math.sin(angle));
        triggerBlackoutAt(pick);
    }

    public void triggerBlackoutAt(Location center) {
        if (center == null || center.getWorld() == null) return;
        World world = center.getWorld();
        blackoutCenter = center;
        blackoutEndTime = System.currentTimeMillis() + (blackoutDuration * 1000L);

        int sectorX = ((int) center.getX() / 30) * 30;
        int sectorZ = ((int) center.getZ() / 30) * 30;

        game.broadcastMessage(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "========================================");
        game.broadcastMessage(ChatColor.RED + "  ⚠ [AWARIA ZASILANIA] Awaria sieci energetycznej w sektorze!");
        game.broadcastMessage(ChatColor.GRAY + "  Sektor: X: " + sectorX + ", Z: " + sectorZ + " (promień " + (int) blackoutRadius + "m)");
        game.broadcastMessage(ChatColor.GRAY + "  Ciemność i gęsta mgła spowiły ulice miasta. Uważaj na zombie...");
        game.broadcastMessage(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "========================================");

        world.playSound(blackoutCenter, Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 0.5f);
        world.playSound(blackoutCenter, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.5f, 0.7f);
    }

    private void tickBlackout(List<Player> players, long now) {
        if (now >= blackoutEndTime) {
            endBlackout();
            return;
        }

        if (players != null && blackoutCenter != null) {
            double radSq = blackoutRadius * blackoutRadius;
            for (Player p : players) {
                if (p != null && p.isOnline() && p.getWorld().equals(blackoutCenter.getWorld())) {
                    if (p.getLocation().distanceSquared(blackoutCenter) <= radSq) {
                        // Ciemność i dźwięki awarii
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 50, 0, false, false, true));
                        if (random.nextDouble() < 0.25) {
                            p.playSound(p.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.6f, 1.6f);
                            p.getWorld().spawnParticle(Particle.WAX_OFF, p.getLocation().add(0, 1.2, 0), 2, 0.3, 0.3, 0.3, 0.05);
                        }
                    }
                }
            }
        }
    }

    private void endBlackout() {
        blackoutCenter = null;
        game.broadcastMessage(ChatColor.GREEN + "[AWARIA] Zasilanie w sektorze zostało przywrócone. Światła znów działają!");
    }

    // =========================================================================
    // 3. BLOOD MOON
    // =========================================================================
    public void triggerBloodMoon() {
        bloodMoonActive = true;
        bloodMoonEndTime = System.currentTimeMillis() + (bloodMoonDuration * 1000L);

        // Modyfikatory
        game.getNoiseManager().setBloodMoonMultiplier(2.0);
        game.getBloodTrailManager().setBloodDetectionMultiplier(2.0);

        // BossBar
        bloodMoonBossBar = Bukkit.createBossBar(
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ BLOOD MOON — 01:00",
                BarColor.RED,
                BarStyle.SOLID
        );
        for (Player p : game.getActiveOnlinePlayers()) {
            bloodMoonBossBar.addPlayer(p);
            p.playSound(p.getLocation(), Sound.AMBIENT_NETHER_WASTES_MOOD, 1.2f, 0.6f);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.7f);
        }

        game.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "========================================");
        game.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "  ☠ KRWAWY KSIĘŻYC WZSZEDŁ NAD MIASTEM! ☠");
        game.broadcastMessage(ChatColor.YELLOW + "  Zombie stają się szybsze, zadają większe obrażenia");
        game.broadcastMessage(ChatColor.YELLOW + "  i z łatwością wyczuwają każdy dźwięk oraz zapach krwi!");
        game.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "========================================");
    }

    private void tickBloodMoon(List<Player> players, long now) {
        long remainingMs = bloodMoonEndTime - now;
        if (remainingMs <= 0) {
            endBloodMoon();
            return;
        }

        int sec = (int) (remainingMs / 1000L);
        double progress = Math.max(0.0, Math.min(1.0, (double) remainingMs / (bloodMoonDuration * 1000L)));

        if (bloodMoonBossBar != null) {
            bloodMoonBossBar.setProgress(progress);
            bloodMoonBossBar.setTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + String.format("☠ BLOOD MOON — 00:%02d", sec));
            if (players != null) {
                for (Player p : players) {
                    if (!bloodMoonBossBar.getPlayers().contains(p)) {
                        bloodMoonBossBar.addPlayer(p);
                    }
                }
            }
        }

        // Czerwona atmosfera wokół graczy
        if (players != null) {
            for (Player p : players) {
                if (p != null && p.isOnline()) {
                    Location pLoc = p.getLocation();
                    p.spawnParticle(Particle.DUST, pLoc.clone().add(0, 1.5, 0), 4, 3.0, 1.5, 3.0,
                            new Particle.DustOptions(Color.fromRGB(220, 20, 20), 0.9f));
                }
            }
        }
    }

    private void endBloodMoon() {
        bloodMoonActive = false;
        game.getNoiseManager().setBloodMoonMultiplier(1.0);
        game.getBloodTrailManager().setBloodDetectionMultiplier(1.0);

        if (bloodMoonBossBar != null) {
            bloodMoonBossBar.removeAll();
            bloodMoonBossBar = null;
        }

        game.broadcastMessage(ChatColor.GOLD + "[Strefa] Krwawy Księżyc zachodzi. Szał zombie ustaje.");
    }

    // =========================================================================
    // 4. CAR ALARM
    // =========================================================================
    public void triggerRandomCarAlarm() {
        List<Location> cars = game.getArena().getCarLocations();
        if (cars.isEmpty()) return;
        Location pick = cars.get(random.nextInt(cars.size()));
        triggerCarAlarm(pick);
    }

    public void triggerCarAlarm(Location carLoc) {
        if (carLoc == null || activeCarAlarms.containsKey(carLoc)) return;

        activeCarAlarms.put(carLoc, System.currentTimeMillis() + (carAlarmDuration * 1000L));

        game.broadcastMessage(ChatColor.RED + "🚨 [ALARM] Uruchomił się alarm samochodowy na koordynatach: "
                + carLoc.getBlockX() + ", " + carLoc.getBlockZ() + "! Dźwięk przyciąga hordy!");

        carLoc.getWorld().playSound(carLoc, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 1.8f);
    }

    private void tickCarAlarms(World world, long now) {
        Iterator<Map.Entry<Location, Long>> it = activeCarAlarms.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, Long> entry = it.next();
            Location loc = entry.getKey();
            long expiry = entry.getValue();

            if (now >= expiry || game.isEnded()) {
                it.remove();
                continue;
            }

            // Dźwięk syreny alarmu
            world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 1.4f, 1.9f);
            world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BIT, 1.2f, 2.0f);

            // Migające reflektory (żółte i czerwone iskry)
            world.spawnParticle(Particle.WAX_ON, loc.clone().add(0.8, 0.6, 0.8), 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticle(Particle.WAX_ON, loc.clone().add(-0.8, 0.6, -0.8), 2, 0.1, 0.1, 0.1, 0.05);

            // Co 2 sekundy impuls hałasu wzywający okoliczne moby w promieniu 50m
            if (now % 2000 < 1000) {
                game.getNoiseManager().recordNoise(loc, 85.0, 50.0, "CAR_ALARM", null, game.getZombieManager());
            }
        }
    }

    private Location findFloorAt(World world, double x, int baseY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);

        for (int y = baseY + 5; y >= baseY - 6; y--) {
            Block block = world.getBlockAt(bx, y, bz);
            if (!block.getType().isAir() && block.getType().isSolid()) {
                return new Location(world, x, y + 1.0, z);
            }
        }
        return null;
    }

    public boolean isCarAlarmActive(Location loc) {
        return activeCarAlarms.containsKey(loc);
    }

    public void cleanup() {
        for (BukkitTask t : activeAirDropTasks) {
            try { t.cancel(); } catch (Exception ignored) {}
        }
        activeAirDropTasks.clear();
        activeCarAlarms.clear();
        if (bloodMoonBossBar != null) {
            bloodMoonBossBar.removeAll();
            bloodMoonBossBar = null;
        }
        bloodMoonActive = false;
        blackoutCenter = null;
    }
}
