package RPG.BattleRoyale;

import CustomContent.Guns.GunType;
import CustomContent.Guns.GunUniqueMod;
import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Barricades.BarricadeManager;
import RPG.BattleRoyale.Bots.BattleRoyaleBotManager;
import RPG.BattleRoyale.Events.BattleRoyaleEvent;
import RPG.BattleRoyale.Events.HydrationManager;
import RPG.BattleRoyale.Events.NoiseManager;
import RPG.BattleRoyale.GroundLoot.GroundLootManager;
import RPG.BattleRoyale.Infection.InfectionManager;
import RPG.BattleRoyale.Items.BandageHandler;
import RPG.BattleRoyale.Loot.BattleRoyaleLootManager;
import RPG.BattleRoyale.Visuals.NavigationVisualsManager;
import RPG.BattleRoyale.Weapons.BattleRoyaleWeaponHelper;
import RPG.BattleRoyale.Zombies.CustomZombieManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BattleRoyaleGame {

    public enum GameState {
        PREPARING,
        COUNTDOWN,
        IN_PROGRESS,
        FINAL_PHASE,
        ENDED
    }

    private final BattleRoyaleArena arena;
    private final BattleRoyaleManager manager;
    private final BattleRoyaleWorldManager worldManager;
    private final BattleRoyaleBotManager botManager;
    private final InfectionManager infectionManager;
    private final BattleRoyaleLootManager lootManager;
    private final GroundLootManager groundLootManager;
    private final BandageHandler bandageHandler;
    private final NoiseManager noiseManager;
    private final HydrationManager hydrationManager;
    private final CustomZombieManager zombieManager;
    private final BarricadeManager barricadeManager;
    private final NavigationVisualsManager visualsManager;

    private int hungerTickCounter = 0;
    private BattleRoyaleEvent currentEvent = BattleRoyaleEvent.NONE;

    private final Set<UUID> activePlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> spectatorPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Location> lootedContainers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Location> assignedSpawns = new HashMap<>();

    private World world;
    private GameState state = GameState.PREPARING;
    private boolean freezeActive = false;

    // Strefa
    private double currentZoneRadius;
    private double targetZoneRadius;
    private int currentPhaseIndex = 0;
    private int phaseWaitSecondsRemaining = 0;
    private int phaseShrinkSecondsRemaining = 0;
    private double radiusShrinkPerSecond = 0.0;

    // PVE i Portal
    private int pveSpawnCounter = 0;
    private Location portalLocation;
    private boolean portalActive = false;
    private double portalAnimationAngle = 0.0;

    private BukkitTask gameLoopTask;
    private BukkitTask fastLoopTask;
    private BukkitTask countdownTask;
    private final Random random = new Random();

    public BattleRoyaleGame(BattleRoyaleArena arena, BattleRoyaleManager manager, List<Player> initialPlayers) {
        this.arena = arena;
        this.manager = manager;
        this.worldManager = new BattleRoyaleWorldManager(arena);
        this.botManager = new BattleRoyaleBotManager();
        this.infectionManager = new InfectionManager(arena.getInfectionTotalDurationSeconds());
        this.lootManager = manager.getLootManager();
        this.groundLootManager = manager.getGroundLootManager();
        this.bandageHandler = new BandageHandler();
        this.noiseManager = new NoiseManager();
        this.hydrationManager = new HydrationManager();
        this.zombieManager = new CustomZombieManager();
        this.barricadeManager = new BarricadeManager();
        this.visualsManager = new NavigationVisualsManager();

        for (Player p : initialPlayers) {
            activePlayers.add(p.getUniqueId());
        }
    }

    /**
     * Startuje całą procedurę meczu.
     */
    public void start() {
        state = GameState.PREPARING;

        // 1. Przygotowanie świata
        world = worldManager.prepareWorld();
        if (world == null) {
            broadcastMessage(ChatColor.RED + "[BattleRoyale] Błąd ładowania świata areny!");
            endGame();
            return;
        }

        // Ustalenie środka i portalu
        Location center = arena.getCenterLocation();
        portalLocation = new Location(world, center.getX(), center.getY() + 1.0, center.getZ());
        currentZoneRadius = arena.getInitialRadius();

        // 2. Przypisanie spawnów i teleportacja graczy
        List<Location> spawns = new ArrayList<>(arena.getSpawnLocations());
        Collections.shuffle(spawns);

        int spawnIndex = 0;
        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                Location spLoc = spawnIndex < spawns.size() ? spawns.get(spawnIndex) : spawns.get(0);
                Location resolved = new Location(world, spLoc.getX(), spLoc.getY(), spLoc.getZ(), spLoc.getYaw(), spLoc.getPitch());
                assignedSpawns.put(uuid, resolved);

                // Zapisanie ekwipunku
                BattleRoyaleInventoryBackup.backupAndClear(p);

                // Teleport
                p.teleport(resolved);
                p.playSound(resolved, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
                spawnIndex++;
            }
        }

        // 3. Wypełnienie brakujących miejsc botami AI (obsługa trybu 1-osobowego i uzupełniania)
        if (arena.isFillWithBots()) {
            int maxSlots = Math.min(arena.getMaxPlayers(), spawns.size());
            int botsNeeded = Math.min(arena.getMaxBotsToSpawn(), maxSlots - activePlayers.size());

            for (int i = 0; i < botsNeeded && spawnIndex < spawns.size(); i++) {
                Location spLoc = spawns.get(spawnIndex);
                Location resolved = new Location(world, spLoc.getX(), spLoc.getY(), spLoc.getZ(), spLoc.getYaw(), spLoc.getPitch());
                botManager.spawnBot(resolved, i);
                spawnIndex++;
            }
        }

        // 4. Rozpoczęcie odliczania z zamrożeniem (Freeze 20s)
        startCountdown();
    }

    private void startCountdown() {
        state = GameState.COUNTDOWN;
        freezeActive = true;

        countdownTask = new BukkitRunnable() {
            int seconds = arena.getFreezeCountdownSeconds();

            @Override
            public void run() {
                if (state != GameState.COUNTDOWN) {
                    cancel();
                    return;
                }

                if (seconds > 0) {
                    ChatColor color = seconds <= 3 ? ChatColor.RED : seconds <= 5 ? ChatColor.GOLD : ChatColor.YELLOW;
                    for (UUID uuid : activePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.sendTitle(color + "" + ChatColor.BOLD + seconds, ChatColor.GRAY + "Przygotuj się na start!", 0, 25, 5);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, seconds <= 3 ? 1.5f : 1.0f);
                        }
                    }
                    seconds--;
                } else {
                    // START GRY!
                    cancel();
                    freezeActive = false;
                    state = GameState.IN_PROGRESS;

                    // Losowanie wydarzenia mapy jeśli nie zostało narzucone przez admina
                    if (currentEvent == BattleRoyaleEvent.NONE) {
                        double roll = random.nextDouble();
                        if (roll < 0.35) {
                            currentEvent = BattleRoyaleEvent.SILENCE;
                        } else if (roll < 0.70) {
                            currentEvent = BattleRoyaleEvent.DEHYDRATION;
                        } else {
                            currentEvent = BattleRoyaleEvent.NONE;
                        }
                    }

                    // Inicjalizacja poziomu nawodnienia graczy
                    if (currentEvent == BattleRoyaleEvent.DEHYDRATION) {
                        for (UUID uuid : activePlayers) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) hydrationManager.initPlayer(p);
                        }
                    }

                    // Rozmieszczenie niewidzialnych ramek Ground Loot na mapie
                    groundLootManager.spawnGroundLoot(world, arena, lootManager);

                    for (UUID uuid : activePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "START!", ChatColor.YELLOW + "Walcz, szukaj łupów i unikaj infekcji!", 5, 40, 15);
                            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.9f, 1.0f);
                            p.sendMessage(ChatColor.GOLD + "========================================");
                            p.sendMessage(ChatColor.GREEN + " Gra rozpoczęta! Biegaj po mapie i otwieraj skrzynie!");
                            p.sendMessage(ChatColor.YELLOW + " Moby nakładają śmiertelną infekcję wręcz.");
                            p.sendMessage(ChatColor.AQUA + " Znajdź Antidotum lub Bandaż, aby leczyć rany!");
                            p.sendMessage(ChatColor.GOLD + " ★ Wydarzenie Mapy: " + ChatColor.BOLD + currentEvent.getDisplayName());
                            p.sendMessage(ChatColor.GRAY + "   " + currentEvent.getDescription());
                            p.sendMessage(ChatColor.GOLD + "========================================");
                        }
                    }

                    // Inicjalizacja pierwszej fazy strefy
                    setupZonePhase(0);

                    // Start pętli głównej meczu (co 1 sekundę)
                    startGameLoop();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 20L);
    }

    private void setupZonePhase(int phaseIndex) {
        currentPhaseIndex = phaseIndex;
        List<BattleRoyaleArena.ZonePhase> phases = arena.getZonePhases();
        if (phaseIndex < phases.size()) {
            BattleRoyaleArena.ZonePhase phase = phases.get(phaseIndex);
            phaseWaitSecondsRemaining = phase.getWaitSeconds();
            phaseShrinkSecondsRemaining = phase.getDurationSeconds();
            targetZoneRadius = phase.getTargetRadius();
            radiusShrinkPerSecond = (currentZoneRadius - targetZoneRadius) / Math.max(1, phaseShrinkSecondsRemaining);

            broadcastMessage(ChatColor.RED + "[Strefa] Nowa faza! Strefa zatrzyma się na " + phaseWaitSecondsRemaining + "s, a potem zmniejszy do " + (int) targetZoneRadius + "m.");
        } else {
            // Finałowa faza strefy
            targetZoneRadius = arena.getFinalRadius();
            currentZoneRadius = arena.getFinalRadius();
            triggerFinalPhase();
        }
    }

    private void triggerFinalPhase() {
        if (state == GameState.FINAL_PHASE) return;
        state = GameState.FINAL_PHASE;
        portalActive = true;

        broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "========================================");
        broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "  FINAŁOWE STARCIE! STREFA ZATRZYMAŁA SIĘ!");
        broadcastMessage(ChatColor.YELLOW + "  Na środku pojawił się PORTAL EWAKUACYJNY!");
        broadcastMessage(ChatColor.GRAY + "  (Pamiętaj: musisz być czysty od wirusa zombie, aby uciec!)");
        broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "========================================");

        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
                p.sendTitle(ChatColor.DARK_RED + "☠ FINAŁOWY RING ☠", ChatColor.YELLOW + "Ewakuuj się przez portal na środku!", 10, 60, 20);
            }
        }

        // Spawn Bossa Finałowego i Hordy Zombie
        spawnFinalHorde();
    }

    private void spawnFinalHorde() {
        Location center = arena.getCenterLocation();
        Location bossLoc = new Location(world, center.getX(), center.getY() + 1.0, center.getZ());

        // Finałowy MythicMob Boss lub potężny Vanilla Boss
        String mmBoss = arena.getFinalBossMythicMob();
        if (mmBoss != null && !mmBoss.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mm mobs spawn -s " + mmBoss + ":1 1 "
                    + world.getName() + "," + bossLoc.getX() + "," + bossLoc.getY() + "," + bossLoc.getZ());
        } else {
            // Vanilla fallback: Potężny Wither Skeleton Boss
            WitherSkeleton boss = world.spawn(bossLoc, WitherSkeleton.class);
            boss.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Tytan Apokalipsy");
            boss.setCustomNameVisible(true);
            if (boss.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
                boss.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(150.0);
                boss.setHealth(150.0);
            }
        }

        // Finałowa fala zombie
        int zombiesCount = arena.getFinalZombiesCount();
        for (int i = 0; i < zombiesCount; i++) {
            double angle = (2 * Math.PI / zombiesCount) * i;
            double r = 8.0;
            Location zLoc = new Location(world, center.getX() + r * Math.cos(angle), center.getY() + 1.0, center.getZ() + r * Math.sin(angle));
            Zombie z = world.spawn(zLoc, Zombie.class);
            z.setCustomName(ChatColor.RED + "Zarażony Zombie Hordy");
            z.setCustomNameVisible(true);
        }

        // Specjalni zombie w finałowej walce
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = 6.0;
            Location zLoc = new Location(world, center.getX() + r * Math.cos(angle), center.getY() + 1.0, center.getZ() + r * Math.sin(angle));
            zombieManager.spawnLeaper(zLoc, null);
        }
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = 9.0;
            Location zLoc = new Location(world, center.getX() + r * Math.cos(angle), center.getY() + 1.0, center.getZ() + r * Math.sin(angle));
            zombieManager.spawnGunner(zLoc, null);
        }
    }

    private void startGameLoop() {
        gameLoopTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.ENDED) {
                    cancel();
                    return;
                }

                // 1. Taktowanie infekcji graczy
                infectionManager.tick();

                // 2. Zarządzanie zamykaniem strefy
                tickZone();

                // 3. Efekty cząsteczek granicy strefy i obrażenia poza nią
                tickZoneEffects();

                // 4. Animacja portalu ewakuacyjnego i detekcja wejścia
                if (portalActive) {
                    tickPortal();
                }

                // 5. Wydarzenia mapy (Cisza / Odwodnienie)
                List<Player> onlinePlayers = getActiveOnlinePlayers();
                if (currentEvent == BattleRoyaleEvent.SILENCE) {
                    noiseManager.tick(world, onlinePlayers, zombieManager);
                } else if (currentEvent == BattleRoyaleEvent.DEHYDRATION) {
                    hydrationManager.tick(world, onlinePlayers);
                }

                // 6. Zwiększony spadek głodu
                if (arena.isIncreasedHungerDrain()) {
                    hungerTickCounter++;
                    if (hungerTickCounter >= arena.getHungerDrainIntervalSeconds()) {
                        hungerTickCounter = 0;
                        for (Player p : onlinePlayers) {
                            p.setExhaustion(p.getExhaustion() + arena.getHungerExhaustionAddition());
                            if (p.isSprinting() && p.getFoodLevel() > 0 && random.nextDouble() < 0.45) {
                                p.setFoodLevel(Math.max(0, p.getFoodLevel() - 1));
                            }
                        }
                    }
                }

                // 7. Okresowy spawn mobów PVE
                tickPveSpawns();

                // 8. Weryfikacja stanu gry (żywi gracze)
                checkGameStatus();
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 20L, 20L);

        // Szybka pętla AI specjalnych mobów, proximity lootu, wskazówek wizualnych i obrony barykad (co 10 ticków = 0.5s)
        fastLoopTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.ENDED) {
                    cancel();
                    return;
                }
                List<Player> players = getActiveOnlinePlayers();
                zombieManager.tick(world);
                groundLootManager.checkProximityPickups(players);
                visualsManager.tick(world, players, portalLocation != null ? portalLocation : arena.getCenterLocation(), currentZoneRadius);
                barricadeManager.tickZombieSiege(world, worldManager, arena.getBarricadeHitsToDestroy());
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 10L, 10L);
    }

    private void tickZone() {
        if (state == GameState.FINAL_PHASE) return;

        if (phaseWaitSecondsRemaining > 0) {
            phaseWaitSecondsRemaining--;
            if (phaseWaitSecondsRemaining == 10 || phaseWaitSecondsRemaining == 5) {
                broadcastMessage(ChatColor.RED + "[Strefa] Zmniejszanie strefy rozpocznie się za " + phaseWaitSecondsRemaining + "s!");
            }
        } else if (phaseShrinkSecondsRemaining > 0) {
            phaseShrinkSecondsRemaining--;
            currentZoneRadius = Math.max(targetZoneRadius, currentZoneRadius - radiusShrinkPerSecond);

            if (phaseShrinkSecondsRemaining <= 0) {
                currentZoneRadius = targetZoneRadius;
                // Przejście do następnej fazy
                setupZonePhase(currentPhaseIndex + 1);
            }
        }
    }

    private void tickZoneEffects() {
        Location center = arena.getCenterLocation();

        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline() || p.isDead()) continue;

            Location pLoc = p.getLocation();
            double dist = Math.sqrt(Math.pow(pLoc.getX() - center.getX(), 2) + Math.pow(pLoc.getZ() - center.getZ(), 2));

            // Poza strefą
            if (dist > currentZoneRadius) {
                p.damage(arena.getDamageOutsidePerSecond());
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c§lPOZA BEZPIECZNĄ STREFĄ! Wracaj natychmiast! (Promień: " + (int) currentZoneRadius + "m)"));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 1.2f);

                // Nakłada infekcję jeśli nie posiada
                if (!infectionManager.isInfected(p)) {
                    infectionManager.infect(p);
                } else {
                    // Przyspiesza infekcję poza strefą
                    RPG.BattleRoyale.Infection.InfectionState inf = infectionManager.getInfection(p.getUniqueId());
                    if (inf != null) inf.reduceSeconds(2);
                }
            } else {
                // Wewnątrz strefy: informacja na Action Barze
                int remBots = botManager.getAliveBotCount();
                int remPlayers = activePlayers.size();
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§eStrefa: §f" + (int) currentZoneRadius + "m §8| §cPrzeciwnicy: §f" + (remPlayers + remBots - 1)));
            }

            // Cząsteczki granicy strefy jeśli gracz jest w pobliżu krawędzi
            if (Math.abs(dist - currentZoneRadius) < 25.0) {
                renderBorderParticlesForPlayer(p, center, currentZoneRadius);
            }
        }
    }

    private void renderBorderParticlesForPlayer(Player p, Location center, double radius) {
        Location pLoc = p.getLocation();
        double playerAngle = Math.atan2(pLoc.getZ() - center.getZ(), pLoc.getX() - center.getX());

        // Renderuj łuk 120 stopni przed graczem
        int points = 30;
        double arc = Math.toRadians(120);
        double startAngle = playerAngle - (arc / 2);
        double step = arc / points;

        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(220, 20, 60), 1.5f);

        for (int i = 0; i <= points; i++) {
            double angle = startAngle + (i * step);
            double bx = center.getX() + radius * Math.cos(angle);
            double bz = center.getZ() + radius * Math.sin(angle);
            double by = pLoc.getY();

            // Słup cząsteczek w pionie
            for (double dy = -1; dy <= 4; dy += 1.2) {
                p.spawnParticle(Particle.DUST, bx, by + dy, bz, 1, 0, 0, 0, 0, dust);
            }
        }
    }

    private void tickPortal() {
        if (portalLocation == null) return;

        portalAnimationAngle += 0.25;

        // Animacja cząsteczek portalu (wielowarstwowa helisa)
        for (double y = 0; y <= 3.5; y += 0.35) {
            double a = portalAnimationAngle + (y * 1.5);
            double rx = 1.6 * Math.cos(a);
            double rz = 1.6 * Math.sin(a);

            world.spawnParticle(Particle.PORTAL, portalLocation.getX() + rx, portalLocation.getY() + y, portalLocation.getZ() + rz, 2, 0.05, 0.05, 0.05, 0.02);
            world.spawnParticle(Particle.END_ROD, portalLocation.getX() - rx, portalLocation.getY() + y, portalLocation.getZ() - rz, 1, 0.02, 0.02, 0.02, 0.01);
        }
        world.spawnParticle(Particle.DRAGON_BREATH, portalLocation.getX(), portalLocation.getY() + 0.2, portalLocation.getZ(), 5, 0.6, 0.1, 0.6, 0.01);

        // Detekcja graczy próbujących ewakuacji
        double portalR = arena.getPortalRadius();
        for (UUID uuid : new ArrayList<>(activePlayers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline() || p.isDead()) continue;

            if (p.getLocation().distance(portalLocation) <= portalR) {
                // WERYFIKACJA INFEKCJI
                if (infectionManager.isInfected(p)) {
                    // Odrzucenie gracza zakażonego
                    Vector dir = p.getLocation().toVector().subtract(portalLocation.toVector()).normalize().setY(0.4).multiply(1.2);
                    p.setVelocity(dir);
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                    p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 0.5f);

                    p.sendTitle(ChatColor.DARK_RED + "☠ PORTAL ODRZUCA! ☠", ChatColor.YELLOW + "Jesteś zainfekowany! Użyj lekarstwa, aby uciec!", 5, 40, 10);
                    p.sendMessage(ChatColor.RED + "[BattleRoyale] Nie możesz się ewakuować z wirusem zombie w krwioobiegu! Wypij Antidotum!");
                } else {
                    // UDANA EWAKUACJA! WYGRANA!
                    handlePlayerExtractionWin(p);
                }
            }
        }
    }

    private void handlePlayerExtractionWin(Player player) {
        broadcastMessage(ChatColor.GOLD + "========================================");
        broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "★ GRACZ " + player.getName().toUpperCase() + " EWAKUOWAŁ SIĘ PRZEZ PORTAL!");
        broadcastMessage(ChatColor.YELLOW + "★ Przetrwał apokalipsę zombie i WYGRAŁ GRĘ BATTLE ROYALE!");
        broadcastMessage(ChatColor.GOLD + "========================================");

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.sendTitle(ChatColor.GOLD + "★ ZWYCIĘSTWO! ★", ChatColor.GREEN + "Pomyślnie ewakuowałeś się z areny!", 10, 80, 20);

        // Nagrody
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String cmd : arena.getRewardCommands()) {
            Bukkit.dispatchCommand(console, cmd.replace("%player%", player.getName()));
        }
        player.giveExp(arena.getRewardExp());

        // Przywrócenie ekwipunku i powrót
        activePlayers.remove(player.getUniqueId());
        BattleRoyaleInventoryBackup.restore(player);

        World mainWorld = Bukkit.getWorlds().get(0);
        player.teleport(mainWorld.getSpawnLocation());

        // Koniec meczu
        endGame();
    }

    private void tickPveSpawns() {
        pveSpawnCounter++;
        if (pveSpawnCounter >= arena.getPveSpawnIntervalSeconds()) {
            pveSpawnCounter = 0;

            int currentMobs = 0;
            for (Entity e : world.getEntities()) {
                if (e instanceof LivingEntity && !(e instanceof Player)) currentMobs++;
            }

            if (currentMobs < arena.getMaxAliveMobs()) {
                spawnRandomPveMob();
            }
        }
    }

    private void spawnRandomPveMob() {
        Location center = arena.getCenterLocation();
        double r = random.nextDouble() * (currentZoneRadius * 0.85);
        double angle = random.nextDouble() * 2 * Math.PI;

        double sx = center.getX() + r * Math.cos(angle);
        double sz = center.getZ() + r * Math.sin(angle);
        int sy = world.getHighestBlockYAt((int) sx, (int) sz) + 1;
        Location spawnLoc = new Location(world, sx, sy, sz);

        // Losowanie: MythicMob, Custom Zombie (Leaper/Gunner), czy Vanilla
        if (!arena.getMythicMobs().isEmpty() && random.nextDouble() < 0.25) {
            Map<?, ?> mmEntry = arena.getMythicMobs().get(random.nextInt(arena.getMythicMobs().size()));
            String name = (String) mmEntry.get("name");
            int lvl = mmEntry.get("level") instanceof Number ? ((Number) mmEntry.get("level")).intValue() : 1;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mm mobs spawn -s " + name + ":" + lvl + " 1 "
                    + world.getName() + "," + sx + "," + sy + "," + sz);
        } else if (random.nextDouble() < 0.45) {
            // Niestandardowy zombie: Skoczek lub Strzelec
            zombieManager.spawnRandomSpecialZombie(spawnLoc, null);
        } else {
            // Vanilla
            EntityType type = random.nextDouble() < 0.70 ? EntityType.ZOMBIE : EntityType.SKELETON;
            world.spawnEntity(spawnLoc, type);
        }
    }

    private void checkGameStatus() {
        // Usunięcie graczy offline
        activePlayers.removeIf(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            return p == null || !p.isOnline();
        });

        if (activePlayers.isEmpty()) {
            endGame();
            return;
        }

        // Jeśli został 1 gracz i 0 botów, aktywuj finał
        if (activePlayers.size() == 1 && botManager.getAliveBotCount() == 0 && !portalActive) {
            triggerFinalPhase();
        }
    }

    public void handlePlayerDeath(Player player) {
        if (!activePlayers.contains(player.getUniqueId())) return;

        broadcastMessage(ChatColor.RED + "[BattleRoyale] Gracz " + player.getName() + " poległ w walce! Pozostało graczy: " + (activePlayers.size() - 1));
        activePlayers.remove(player.getUniqueId());
        infectionManager.cure(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    BattleRoyaleInventoryBackup.restore(player);
                    World mainWorld = Bukkit.getWorlds().get(0);
                    player.teleport(mainWorld.getSpawnLocation());
                    player.sendMessage(ChatColor.YELLOW + "[BattleRoyale] Twój ekwipunek został przywrócony.");
                }
            }
        }.runTaskLater(AmonPackPlugin.plugin, 10L);

        checkGameStatus();
    }

    public void handlePlayerLeave(Player player) {
        if (activePlayers.remove(player.getUniqueId())) {
            infectionManager.cure(player);
            BattleRoyaleInventoryBackup.restore(player);
            World mainWorld = Bukkit.getWorlds().get(0);
            player.teleport(mainWorld.getSpawnLocation());
            broadcastMessage(ChatColor.GRAY + "[BattleRoyale] Gracz " + player.getName() + " opuścił grę.");
            checkGameStatus();
        }
    }

    public void handleContainerOpen(Block block, Player player) {
        if (block == null) return;
        Location loc = block.getLocation();
        if (lootedContainers.add(loc)) {
            lootManager.populateContainer(block, arena);
            player.playSound(loc, Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
        }
    }

    public void endGame() {
        if (state == GameState.ENDED) return;
        state = GameState.ENDED;

        if (gameLoopTask != null) gameLoopTask.cancel();
        if (fastLoopTask != null) fastLoopTask.cancel();
        if (countdownTask != null) countdownTask.cancel();

        groundLootManager.cleanAllFrames(world);
        bandageHandler.cleanup();
        noiseManager.cleanup();
        hydrationManager.cleanup();
        zombieManager.cleanup();
        barricadeManager.cleanup();
        visualsManager.cleanup();

        infectionManager.cleanupAll();
        botManager.cleanup();

        // Przywrócenie pozostałych graczy
        for (UUID uuid : new ArrayList<>(activePlayers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                BattleRoyaleInventoryBackup.restore(p);
                World mainWorld = Bukkit.getWorlds().get(0);
                p.teleport(mainWorld.getSpawnLocation());
            }
        }
        activePlayers.clear();

        // Reset świata areny
        worldManager.resetWorld();

        manager.onGameEnd(this);
    }

    public void broadcastMessage(String msg) {
        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    public List<Player> getActiveOnlinePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !p.isDead()) {
                list.add(p);
            }
        }
        return list;
    }

    // Gettery
    public BattleRoyaleArena getArena() { return arena; }
    public World getWorld() { return world; }
    public boolean isFreezeActive() { return freezeActive; }
    public boolean isPlayerInGame(UUID uuid) { return activePlayers.contains(uuid); }
    public boolean isPlayerInGame(Player player) { return player != null && isPlayerInGame(player.getUniqueId()); }
    public InfectionManager getInfectionManager() { return infectionManager; }
    public BattleRoyaleBotManager getBotManager() { return botManager; }
    public BattleRoyaleWorldManager getWorldManager() { return worldManager; }
    public GroundLootManager getGroundLootManager() { return groundLootManager; }
    public BandageHandler getBandageHandler() { return bandageHandler; }
    public NoiseManager getNoiseManager() { return noiseManager; }
    public HydrationManager getHydrationManager() { return hydrationManager; }
    public CustomZombieManager getZombieManager() { return zombieManager; }
    public BarricadeManager getBarricadeManager() { return barricadeManager; }
    public NavigationVisualsManager getVisualsManager() { return visualsManager; }
    public BattleRoyaleEvent getCurrentEvent() { return currentEvent; }
    public void setCurrentEvent(BattleRoyaleEvent event) { this.currentEvent = event; }
    public BattleRoyaleLootManager getLootManager() { return lootManager; }
    public GameState getState() { return state; }
}
