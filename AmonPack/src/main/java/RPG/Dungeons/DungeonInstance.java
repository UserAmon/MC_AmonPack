package RPG.Dungeons;

import RPG.Levels.Objects.LevelSkill;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import Plugin.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DungeonInstance {
    private final Dungeon template;
    private final UUID instanceId;
    private final World world;
    private final List<UUID> players = new ArrayList<>();
    private final Map<UUID, DungeonPlayerStats> playerStatsMap = new HashMap<>();
    private final Map<UUID, Boolean> readyPlayers = new HashMap<>();
    private final Set<UUID> spectatorPlayers = new HashSet<>();
    private final Map<Location, Set<UUID>> claimedChests = new HashMap<>();
    private final Map<Location, Map<UUID, DungeonLootChest>> chestGuis = new HashMap<>();

    private String activeEncounterId;
    private final Map<String, Integer> killedMobsCounter = new HashMap<>();
    private final Map<Location, String> activeLootChests = new HashMap<>();

    private boolean randomPhaseActive = false;
    private int randomClearsCount = 0;
    private int reqClears = 0;
    private String encAfterClears = null;
    private final List<String> randomExcludes = new ArrayList<>();
    private final Set<String> completedEncounters = new HashSet<>();

    private boolean isFinished = false;

    private org.bukkit.boss.BossBar bossBar;
    private final Map<DungeonCondition, Integer> zoneCaptureCounters = new HashMap<>();
    private final Map<DungeonCondition, Integer> throwHitsCounter = new HashMap<>();
    private final Set<UUID> activeShieldedEnemyUuids = new HashSet<>();
    private final List<Integer> spawnUntilTaskIds = new ArrayList<>();
    private final Set<Integer> completedConditionsIndices = new HashSet<>();
    private final List<String> randomPool = new ArrayList<>();

    private boolean poolListPhaseActive = false;
    private List<String> currentPoolListSequence = new ArrayList<>();
    private int poolListIndex = 0;

    public DungeonInstance(Dungeon template, List<Player> party) {
        this.template = template;
        this.instanceId = UUID.randomUUID();
        this.world = DungeonWorldManager.createDungeonWorld(template.getId());

        for (Player p : party) {
            this.players.add(p.getUniqueId());
            this.playerStatsMap.put(p.getUniqueId(), new DungeonPlayerStats(p.getUniqueId(), p.getName()));
            this.readyPlayers.put(p.getUniqueId(), false);
        }

        this.activeEncounterId = template.getInitialEncounterId();
    }

    public void start() {
        if (world == null) {
            broadcast(ChatColor.RED + "[Dungeons] Blad krytyczny: Nie udalo sie wygenerowac swiata!");
            cleanup();
            return;
        }

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                DungeonInventoryBackup.backupAndClearInventory(player, AmonPackPlugin.plugin);
            }
        }

        Vector paste = template.getPasteLocation();
        boolean pasted = SchematicManager.pasteSchematic(world, template.getSchematicFile(), paste.getBlockX(),
                paste.getBlockY(), paste.getBlockZ(), AmonPackPlugin.plugin);

        if (!pasted) {
            broadcast(ChatColor.RED + "[Dungeons] Blad krytyczny: Nie udalo sie wkleic schematu terenu!");
            cleanup();
            return;
        }

        Vector spawn = template.getSpawnLocation();
        Location spawnLoc = new Location(world, spawn.getX(), spawn.getY(), spawn.getZ());

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(spawnLoc);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20.0);
                player.setFoodLevel(20);

                DungeonPlayerStats stats = playerStatsMap.get(uuid);
                stats.applyStatsToPlayer(player);
            }
        }

        executeActiveEncounterEffects();
    }

    public void update() {
        if (isFinished)
            return;

        List<Player> online = getOnlinePlayers();
        if (online.isEmpty()) {
            System.out.println("[Dungeons] Wszyscy gracze opuscili instancje: " + world.getName() + ". Czyszczenie...");
            cleanup();
            return;
        }

        boolean allSpectators = true;
        for (Player p : online) {
            if (!isPlayerSpectator(p)) {
                allSpectators = false;
                break;
            }
        }

        if (allSpectators) {
            broadcast(ChatColor.RED + "[Dungeons] Wszyscy gracze polegli! Dungeon zakancza sie porazka.");
            cleanup();
            return;
        }

        Encounter encounter = getActiveEncounter();
        if (encounter != null) {
            List<DungeonCondition> conds = encounter.getConditions();
            for (int i = 0; i < conds.size(); i++) {
                DungeonCondition condition = conds.get(i);
                if (condition.isMet(this)) {
                    if (!completedConditionsIndices.contains(i)) {
                        completedConditionsIndices.add(i);
                        for (DungeonEffect effect : condition.getOnCompleteEffects()) {
                            effect.execute(this);
                        }
                    }
                }
            }

            for (DungeonCondition condition : encounter.getConditions()) {
                if (condition.getType() == DungeonCondition.ConditionType.ZONE) {
                    double cx = condition.getX();
                    double cy = condition.getY();
                    double cz = condition.getZ();
                    double radius = condition.getRadius();
                    Location center = new Location(world, cx, cy, cz);
                    
                    for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) {
                        double px = cx + radius * Math.cos(angle);
                        double pz = cz + radius * Math.sin(angle);
                        world.spawnParticle(Particle.GLOW, px, cy + 0.1, pz, 1, 0, 0, 0, 0);
                    }
                    
                    boolean playerInZone = false;
                    for (Player player : getOnlinePlayers()) {
                        if (!isPlayerSpectator(player) && player.getLocation().distanceSquared(center) <= radius * radius) {
                            playerInZone = true;
                            break;
                        }
                    }
                    
                    if (playerInZone) {
                        incrementZoneProgress(condition);
                    } else {
                        int current = getZoneProgress(condition);
                        if (current > 0) {
                            zoneCaptureCounters.put(condition, current - 1);
                        }
                    }
                }
            }

            if (!activeShieldedEnemyUuids.isEmpty()) {
                double angle = (System.currentTimeMillis() / 200.0) % (2 * Math.PI);
                for (UUID uuid : activeShieldedEnemyUuids) {
                    org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
                    if (entity instanceof org.bukkit.entity.LivingEntity && !entity.isDead()) {
                        Location center = entity.getLocation().add(0, 1, 0);
                        for (int i = 0; i < 8; i++) {
                            double finalAngle = angle + (i * Math.PI / 4);
                            double px = 1.0 * Math.cos(finalAngle);
                            double pz = 1.0 * Math.sin(finalAngle);
                            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center.getX() + px, center.getY(), center.getZ() + pz, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }

            if (bossBar != null && bossBar.isVisible()) {
                String titleTemplate = encounter.getTitle();
                if (titleTemplate != null) {
                    int act = 0;
                    int req = 1;
                    for (DungeonCondition condition : encounter.getConditions()) {
                        if (condition.getType() == DungeonCondition.ConditionType.KILL_MOBS) {
                            act = getKilledMobsCount(condition.getMobName());
                            req = condition.getAmount();
                            break;
                        } else if (condition.getType() == DungeonCondition.ConditionType.ZONE) {
                            act = getZoneProgress(condition);
                            req = condition.getTimeRequired();
                            break;
                        } else if (condition.getType() == DungeonCondition.ConditionType.THROW_AT || condition.getType() == DungeonCondition.ConditionType.THROW_AT_ENEMY) {
                            act = getThrowHits(condition);
                            req = condition.getAmount();
                            break;
                        }
                    }
                    String titleText = titleTemplate
                        .replace("$ActNumber$", String.valueOf(act))
                        .replace("$ReqNumber$", String.valueOf(req));
                    bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', titleText));
                    double progress = (double) act / req;
                    bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                }
            }

            boolean allMet = true;
            for (DungeonCondition condition : encounter.getConditions()) {
                if (!condition.isMet(this)) {
                    allMet = false;
                    break;
                }
            }

            if (allMet && !encounter.getConditions().isEmpty()) {
                transitionToNext();
            }
        }
    }

    public void transitionToNext() {
        Encounter current = getActiveEncounter();
        if (current == null)
            return;

        System.out.println("[Dungeons] Zrealizowano etap: " + current.getId() + " (" + current.getDescription()
                + ") na " + world.getName());

        if (isFinished)
            return;

        completedEncounters.add(current.getId());
        clearSpawnUntilTasks();
        completedConditionsIndices.clear();

        String nextId = null;

        if (poolListPhaseActive) {
            poolListIndex++;
            if (poolListIndex >= currentPoolListSequence.size()) {
                poolListPhaseActive = false;
                nextId = encAfterClears;
                System.out.println("[Dungeons] Sekwencja pool_lists ukonczona! Nastepny: " + nextId);
            } else {
                nextId = currentPoolListSequence.get(poolListIndex);
                System.out.println("[Dungeons] Pool list krok " + (poolListIndex + 1) + "/" + currentPoolListSequence.size() + ": " + nextId);
            }
        } else if (randomPhaseActive) {
            randomClearsCount++;
            System.out.println("[Dungeons] Postep fazy losowej: " + randomClearsCount + "/" + reqClears);

            if (randomClearsCount >= reqClears) {
                randomPhaseActive = false;
                nextId = encAfterClears;
                System.out.println("[Dungeons] Faza losowa ukonczona! Nastepny etap: " + nextId);
            } else {
                List<String> eligible = new ArrayList<>();
                Collection<String> sourceCollection = randomPool.isEmpty() ? template.getEncounters().keySet() : randomPool;
                for (String id : sourceCollection) {
                    if (!completedEncounters.contains(id) && !randomExcludes.contains(id)) {
                        eligible.add(id);
                    }
                }

                if (eligible.isEmpty()) {
                    completedEncounters.clear();
                    completedEncounters.add(current.getId());
                    for (String id : sourceCollection) {
                        if (!completedEncounters.contains(id) && !randomExcludes.contains(id)) {
                            eligible.add(id);
                        }
                    }
                }

                if (eligible.isEmpty()) {
                    randomPhaseActive = false;
                    nextId = encAfterClears;
                } else {
                    nextId = eligible.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligible.size()));
                }
            }
        } else if (!current.getNextEncounters().isEmpty()
                && current.getNextEncounters().get(0).equalsIgnoreCase("random")) {

            List<List<String>> poolLists = current.getPoolLists();
            if (!poolLists.isEmpty()) {
                List<List<String>> eligible = new ArrayList<>();
                for (List<String> candidate : poolLists) {
                    boolean available = true;
                    for (String encId : candidate) {
                        if (completedEncounters.contains(encId)) {
                            available = false;
                            break;
                        }
                    }
                    if (available) {
                        eligible.add(candidate);
                    }
                }

                if (eligible.isEmpty()) {
                    nextId = current.getEncAfterClears();
                    System.out.println("[Dungeons] Brak dostepnych pool_lists! Nastepny: " + nextId);
                } else {
                    List<String> chosen = eligible.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligible.size()));
                    poolListPhaseActive = true;
                    currentPoolListSequence = new ArrayList<>(chosen);
                    poolListIndex = 0;
                    encAfterClears = current.getEncAfterClears();
                    nextId = currentPoolListSequence.get(0);
                    System.out.println("[Dungeons] Wybrana lista pool_lists " + chosen + ". Start: " + nextId);
                }
            } else {
                randomPhaseActive = true;
                randomClearsCount = 0;
                reqClears = current.getReqClears();
                encAfterClears = current.getEncAfterClears();
                randomExcludes.clear();
                randomExcludes.addAll(current.getExclude());
                randomPool.clear();
                randomPool.addAll(current.getPool());

                System.out.println("[Dungeons] Inicjalizacja fazy losowej! Wymagane ukonczenia: " + reqClears
                        + ", Nastepny cel po zakonczeniu: " + encAfterClears);

                List<String> eligiblePool = new ArrayList<>();
                Collection<String> sourceCollection = randomPool.isEmpty() ? template.getEncounters().keySet() : randomPool;
                for (String id : sourceCollection) {
                    if (!completedEncounters.contains(id) && !randomExcludes.contains(id)) {
                        eligiblePool.add(id);
                    }
                }

                if (eligiblePool.isEmpty()) {
                    randomPhaseActive = false;
                    nextId = encAfterClears;
                } else {
                    nextId = eligiblePool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligiblePool.size()));
                }
            }
        } else {
            List<String> nextList = current.getNextEncounters();
            if (nextList == null || nextList.isEmpty()) {
                completeDungeon();
                return;
            }

            if (nextList.size() == 1) {
                nextId = nextList.get(0);
            } else {
                nextId = nextList.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(nextList.size()));
            }
        }

        if (nextId == null || nextId.isEmpty()) {
            completeDungeon();
            return;
        }

        killedMobsCounter.clear();

        for (UUID uuid : readyPlayers.keySet()) {
            readyPlayers.put(uuid, false);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.getInventory().remove(Material.COMPASS);
            }
        }

        this.activeEncounterId = nextId;
        System.out.println("[Dungeons] Aktywowano nowy etap: " + nextId + " na " + world.getName());

        executeActiveEncounterEffects();
    }

    private void executeActiveEncounterEffects() {
        Encounter encounter = getActiveEncounter();
        if (encounter != null) {
            if (encounter.getTitle() != null && !encounter.getTitle().isEmpty()) {
                if (bossBar == null) {
                    bossBar = Bukkit.createBossBar(encounter.getTitle(), org.bukkit.boss.BarColor.RED, org.bukkit.boss.BarStyle.SOLID);
                } else {
                    bossBar.setTitle(encounter.getTitle());
                }
                bossBar.removeAll();
                for (Player p : getOnlinePlayers()) {
                    bossBar.addPlayer(p);
                }
                bossBar.setVisible(true);
            } else {
                if (bossBar != null) {
                    bossBar.removeAll();
                    bossBar.setVisible(false);
                }
            }

            for (DungeonCondition condition : encounter.getConditions()) {
                if (condition.getType() == DungeonCondition.ConditionType.THROW_AT_ENEMY) {
                    String command = "mm mobs spawn -s " + condition.getMobName() + ":1 1 " +
                                     world.getName() + "," + condition.getX() + "," + condition.getY() + "," + condition.getZ();
                    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
                    
                    new org.bukkit.scheduler.BukkitRunnable() {
                        @Override
                        public void run() {
                            Location loc = new Location(world, condition.getX(), condition.getY(), condition.getZ());
                            for (org.bukkit.entity.Entity entity : world.getNearbyEntities(loc, 3.0, 3.0, 3.0)) {
                                if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
                                    String name = entity.getName();
                                    String cleanName = ChatColor.stripColor(name);
                                    if (cleanName.equalsIgnoreCase(condition.getMobName()) || entity.getType().name().equalsIgnoreCase(condition.getMobName())) {
                                        activeShieldedEnemyUuids.add(entity.getUniqueId());
                                    }
                                }
                            }
                        }
                    }.runTaskLater(Plugin.AmonPackPlugin.plugin, 2L);
                }
            }

            for (DungeonEffect effect : encounter.getEffects()) {
                if (effect.getType() != DungeonEffect.EffectType.COMPLETE_DUNGEON) {
                    effect.execute(this);
                }
            }
        }
    }

    public void completeDungeon() {
        if (isFinished)
            return;
        isFinished = true;

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }

        clearSpawnUntilTasks();
        completedConditionsIndices.clear();

        broadcast(ChatColor.GREEN + "[Dungeons] ========================================");
        broadcast(ChatColor.YELLOW + "      DUNGEON UKONCZONY POMYSLNIE!");
        broadcast(ChatColor.GREEN + "[Dungeons] ========================================");

        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {

                DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);

                DungeonPlayerStats stats = playerStatsMap.get(uuid);
                if (stats != null) {
                    stats.resetAttributes(player);
                }

                player.setGameMode(GameMode.SURVIVAL);

                clearPlayerTemporaryStatsAndAbilities(player);

                int xp = template.getRewards().getDungeonXp();
                if (xp > 0) {
                    AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.DUNGEON, player, xp);
                    player.sendMessage(
                            ChatColor.GOLD + "[Levels] " + ChatColor.YELLOW + "+" + xp + " EXP Eksploracji Dungeonow!");
                }

                double money = template.getRewards().getMoney();
                if (money > 0) {
                    player.sendMessage(
                            ChatColor.GOLD + "[Portfel] " + ChatColor.YELLOW + "+" + money + "$ za ukonczenie lochu!");
                }

                for (ItemStack item : template.getRewards().getItems()) {
                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(item.clone());
                    if (!left.isEmpty()) {
                        Location dropLoc = getExitLocation();
                        for (ItemStack drop : left.values()) {
                            dropLoc.getWorld().dropItemNaturally(dropLoc, drop);
                        }
                        player.sendMessage(ChatColor.RED
                                + "Brak miejsca w ekwipunku! Nagrody zostaly upuszczone pod Twoimi nogami.");
                    }
                }

                for (String cmd : template.getRewards().getCommands()) {
                    String finalCmd = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(console, finalCmd);
                }

                player.teleport(getExitLocation());
                player.sendMessage(
                        ChatColor.GREEN + "Zostales bezpiecznie przeteleportowany z powrotem na glowny swiat.");
            }
        }

        cleanupWorldAndBackups();
    }

    public void cleanup() {
        isFinished = true;

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }

        clearSpawnUntilTasks();
        completedConditionsIndices.clear();

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);

                DungeonPlayerStats stats = playerStatsMap.get(uuid);
                if (stats != null) {
                    stats.resetAttributes(player);
                }

                player.setGameMode(GameMode.SURVIVAL);

                clearPlayerTemporaryStatsAndAbilities(player);
                player.teleport(getExitLocation());
                player.sendMessage(ChatColor.RED + "Dungeon zostal zamkniety. Przywrocono Twoj ekwipunek survivalowy.");
            }
        }

        cleanupWorldAndBackups();
    }

    private void cleanupWorldAndBackups() {
        File backupsFolder = new File(AmonPackPlugin.plugin.getDataFolder(), "backups");
        for (UUID uuid : players) {
            File file = new File(backupsFolder, uuid.toString() + ".yml");
            if (file.exists()) {
                file.delete();
            }
        }

        DungeonWorldManager.deleteDungeonWorld(world);
    }

    public void ejectPlayer(Player player) {
        if (player == null)
            return;
        UUID uuid = player.getUniqueId();

        if (players.contains(uuid)) {
            players.remove(uuid);
            readyPlayers.remove(uuid);
            spectatorPlayers.remove(uuid);

            if (bossBar != null) {
                bossBar.removePlayer(player);
            }

            DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);
            DungeonPlayerStats stats = playerStatsMap.remove(uuid);
            if (stats != null) {
                stats.resetAttributes(player);
            }

            player.setGameMode(GameMode.SURVIVAL);

            clearPlayerTemporaryStatsAndAbilities(player);
            player.teleport(getExitLocation());
            player.sendMessage(ChatColor.YELLOW + "Opusciles dungeon. Twoje przedmioty zostaly przywrocone.");
            broadcast(ChatColor.RED + player.getName() + " opuscil druzyne dungeonu.");
        }
    }

    public void registerLootChest(Location loc, String type) {
        activeLootChests.put(loc, type);
    }

    public boolean isRegisteredLootChest(Location loc) {
        return activeLootChests.containsKey(loc);
    }

    public void removeLootChest(Location loc) {
        activeLootChests.remove(loc);
    }

    public void onMobKill(String mobName) {
        String cleanName = mobName.toLowerCase();
        int count = killedMobsCounter.getOrDefault(cleanName, 0) + 1;
        killedMobsCounter.put(cleanName, count);

        Encounter encounter = getActiveEncounter();
        if (encounter != null) {
            boolean allMet = true;
            for (DungeonCondition condition : encounter.getConditions()) {
                if (!condition.isMet(this)) {
                    allMet = false;
                    break;
                }
            }
            if (allMet && !encounter.getConditions().isEmpty()) {
                transitionToNext();
            }
        }
    }

    public int getKilledMobsCount(String mobName) {
        return killedMobsCounter.getOrDefault(mobName.toLowerCase(), 0);
    }

    public boolean areAllPlayersReady() {
        if (readyPlayers.isEmpty())
            return false;
        for (boolean ready : readyPlayers.values()) {
            if (!ready)
                return false;
        }
        return true;
    }

    public void setPlayerReady(Player player, boolean ready) {
        if (readyPlayers.containsKey(player.getUniqueId())) {
            readyPlayers.put(player.getUniqueId(), ready);

            if (ready) {
                broadcast(ChatColor.GREEN + player.getName() + " jest gotowy!");
            } else {
                broadcast(ChatColor.RED + player.getName() + " nie jest gotowy.");
            }

            Encounter encounter = getActiveEncounter();
            if (encounter != null) {
                boolean allMet = true;
                for (DungeonCondition condition : encounter.getConditions()) {
                    if (!condition.isMet(this)) {
                        allMet = false;
                        break;
                    }
                }
                if (allMet && !encounter.getConditions().isEmpty()) {
                    transitionToNext();
                }
            }
        }
    }

    public void broadcast(String msg) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(msg);
            }
        }
    }

    public List<Player> getOnlinePlayers() {
        return players.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toList());
    }

    public Location getExitLocation() {
        World exitW = Bukkit.getWorld(template.getExitWorld());
        Vector exit = template.getExitLocation();
        return new Location(exitW == null ? Bukkit.getWorlds().get(0) : exitW, exit.getX(), exit.getY(), exit.getZ());
    }

    public Encounter getActiveEncounter() {
        return template.getEncounters().get(activeEncounterId);
    }

    public World getWorld() {
        return world;
    }

    public Dungeon getTemplate() {
        return template;
    }

    public DungeonPlayerStats getPlayerStats(Player p) {
        return playerStatsMap.get(p.getUniqueId());
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setPlayerSpectator(Player player, boolean spec) {
        UUID uuid = player.getUniqueId();
        if (spec) {
            spectatorPlayers.add(uuid);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED
                    + "[Dungeons] Polegles! Zostales spektatorem. Nie mozesz sie ruszac, dopoki druzyna nie wygra lub nie przegra.");
        } else {
            spectatorPlayers.remove(uuid);
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    public boolean isPlayerSpectator(Player player) {
        return spectatorPlayers.contains(player.getUniqueId());
    }

    public Set<UUID> getSpectatorPlayers() {
        return spectatorPlayers;
    }

    public boolean isPlayerReady(Player player) {
        return readyPlayers.getOrDefault(player.getUniqueId(), false);
    }

    public void markChestClaimed(Player player, Location loc) {
        Set<UUID> claimants = claimedChests.computeIfAbsent(loc, k -> new HashSet<>());
        claimants.add(player.getUniqueId());

        List<UUID> activeLiving = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !isPlayerSpectator(p)) {
                activeLiving.add(uuid);
            }
        }

        if (claimants.containsAll(activeLiving)) {
            org.bukkit.block.Block block = loc.getBlock();
            block.setType(Material.AIR);
            block.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3,
                    0.3, 0.3, Material.CHEST.createBlockData());
            block.getWorld().playSound(block.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);

            removeLootChest(loc);
            claimedChests.remove(loc);
            chestGuis.remove(loc);
        }
    }

    public boolean hasClaimedChest(Player player, Location loc) {
        Set<UUID> claimants = claimedChests.get(loc);
        return claimants != null && claimants.contains(player.getUniqueId());
    }

    public void preGenerateChestGuis(Location loc) {
        Map<UUID, DungeonLootChest> playerGuis = new HashMap<>();
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                DungeonPlayerStats stats = getPlayerStats(player);
                DungeonLootChest gui = new DungeonLootChest(player, stats, template, loc);
                playerGuis.put(uuid, gui);
            }
        }
        chestGuis.put(loc, playerGuis);
    }

    public DungeonLootChest getChestGuiForPlayer(Location loc, Player player) {
        Map<UUID, DungeonLootChest> playerGuis = chestGuis.get(loc);
        if (playerGuis == null) {
            playerGuis = new HashMap<>();
            chestGuis.put(loc, playerGuis);
        }

        DungeonLootChest gui = playerGuis.get(player.getUniqueId());
        if (gui == null) {
            DungeonPlayerStats stats = getPlayerStats(player);
            gui = new DungeonLootChest(player, stats, template, loc);
            playerGuis.put(player.getUniqueId(), gui);
        }

        return gui;
    }

    private void clearPlayerTemporaryStatsAndAbilities(org.bukkit.OfflinePlayer player) {
        if (player == null) return;
        
        PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
        if (branch != null) {
            branch.getTemporaryAbilities().clear();
            
            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bPlayer != null) {
                for (com.projectkorra.projectkorra.Element tempEl : branch.getTemporaryElements()) {
                    bPlayer.getElements().remove(tempEl);
                }
                bPlayer.removeUnusableAbilities();
            }
            branch.getTemporaryElements().clear();
        }
    }

    public void addSpawnUntilTaskId(int id) {
        spawnUntilTaskIds.add(id);
    }

    public void clearSpawnUntilTasks() {
        for (int id : spawnUntilTaskIds) {
            Bukkit.getScheduler().cancelTask(id);
        }
        spawnUntilTaskIds.clear();
    }

    public int getZoneProgress(DungeonCondition condition) {
        return zoneCaptureCounters.getOrDefault(condition, 0);
    }

    public void incrementZoneProgress(DungeonCondition condition) {
        int val = getZoneProgress(condition) + 1;
        zoneCaptureCounters.put(condition, val);
    }

    public int getThrowHits(DungeonCondition condition) {
        return throwHitsCounter.getOrDefault(condition, 0);
    }

    public void incrementThrowHits(DungeonCondition condition) {
        int val = getThrowHits(condition) + 1;
        throwHitsCounter.put(condition, val);
    }

    public void registerProjectileHitCoord(Location hitLoc) {
        Encounter encounter = getActiveEncounter();
        if (encounter == null) return;
        for (DungeonCondition condition : encounter.getConditions()) {
            if (condition.getType() == DungeonCondition.ConditionType.THROW_AT) {
                Location target = new Location(world, condition.getX(), condition.getY(), condition.getZ());
                if (hitLoc.distanceSquared(target) <= condition.getRadius() * condition.getRadius()) {
                    incrementThrowHits(condition);
                    world.playSound(hitLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, hitLoc, 10, 0.2, 0.2, 0.2, 0.05);
                    break;
                }
            }
        }
    }

    public boolean isShieldedEnemy(UUID uuid) {
        return activeShieldedEnemyUuids.contains(uuid);
    }

    public void registerShieldedEnemyHit(org.bukkit.entity.LivingEntity entity, DungeonCustomItem customItem) {
        Encounter encounter = getActiveEncounter();
        if (encounter == null) return;
        for (DungeonCondition condition : encounter.getConditions()) {
            if (condition.getType() == DungeonCondition.ConditionType.THROW_AT_ENEMY) {
                if (activeShieldedEnemyUuids.contains(entity.getUniqueId()) && condition.getCustomItemId().equalsIgnoreCase(customItem.getId())) {
                    incrementThrowHits(condition);
                    world.playSound(entity.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                    world.spawnParticle(Particle.FLASH, entity.getLocation().add(0, 1, 0), 5, 0.1, 0.1, 0.1, 0.01);
                    
                    if (getThrowHits(condition) >= condition.getAmount()) {
                        activeShieldedEnemyUuids.remove(entity.getUniqueId());
                        entity.damage(100.0);
                        world.spawnParticle(Particle.EXPLOSION, entity.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                        world.playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
                    }
                    break;
                }
            }
        }
    }
}
