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

    // Active state machine
    private String activeEncounterId;
    private final Map<String, Integer> killedMobsCounter = new HashMap<>();
    private final Map<Location, String> activeLootChests = new HashMap<>();

    // Rogue-lite Random Room Progression tracking
    private boolean randomPhaseActive = false;
    private int randomClearsCount = 0;
    private int reqClears = 0;
    private String encAfterClears = null;
    private final List<String> randomExcludes = new ArrayList<>();
    private final Set<String> completedEncounters = new HashSet<>();

    private boolean isFinished = false;

    public DungeonInstance(Dungeon template, List<Player> party) {
        this.template = template;
        this.instanceId = UUID.randomUUID();

        // 1. Create a dynamic empty world
        this.world = DungeonWorldManager.createDungeonWorld(template.getId());

        // 2. Initialize players
        for (Player p : party) {
            this.players.add(p.getUniqueId());
            this.playerStatsMap.put(p.getUniqueId(), new DungeonPlayerStats(p.getUniqueId(), p.getName()));
            this.readyPlayers.put(p.getUniqueId(), false);
        }

        this.activeEncounterId = template.getInitialEncounterId();
    }

    /**
     * Starts the dungeon run instance: backups inventories, pastes schematic,
     * teleports players, and starts loop.
     */
    public void start() {
        if (world == null) {
            broadcast(ChatColor.RED + "[Dungeons] Blad krytyczny: Nie udalo sie wygenerowac swiata!");
            cleanup();
            return;
        }

        // 1. Backup inventories (excluding crafted items)
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                DungeonInventoryBackup.backupAndClearInventory(player, AmonPackPlugin.plugin);
            }
        }

        // 2. Paste WorldEdit schematic
        Vector paste = template.getPasteLocation();
        boolean pasted = SchematicManager.pasteSchematic(world, template.getSchematicFile(), paste.getBlockX(),
                paste.getBlockY(), paste.getBlockZ(), AmonPackPlugin.plugin);

        if (!pasted) {
            broadcast(ChatColor.RED + "[Dungeons] Blad krytyczny: Nie udalo sie wkleic schematu terenu!");
            cleanup();
            return;
        }

        // 3. Teleport players to spawn location and apply dungeon attributes
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

        // 4. Trigger initial encounter effects
        executeActiveEncounterEffects();
    }

    /**
     * Runs periodic updates (e.g., area checking, player status, condition
     * evaluations).
     */
    public void update() {
        if (isFinished)
            return;

        // Check if all players left or offline
        List<Player> online = getOnlinePlayers();
        if (online.isEmpty()) {
            System.out.println("[Dungeons] Wszyscy gracze opuscili instancje: " + world.getName() + ". Czyszczenie...");
            cleanup();
            return;
        }

        // Check if all online players are spectators
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

        // Evaluate active encounter conditions
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
                // Execute effects and transition
                transitionToNext();
            }
        }
    }

    /**
     * Executes the effects of the active encounter, and moves to the next.
     */
    public void transitionToNext() {
        Encounter current = getActiveEncounter();
        if (current == null)
            return;

        System.out.println("[Dungeons] Zrealizowano etap: " + current.getId() + " (" + current.getDescription()
                + ") na " + world.getName());

        // If completed dungeon, do not transition
        if (isFinished)
            return;

        // Mark the current encounter as completed in this run
        completedEncounters.add(current.getId());

        String nextId = null;

        if (randomPhaseActive) {
            // We are in the middle of a random phase!
            randomClearsCount++;
            System.out.println("[Dungeons] Postep fazy losowej: " + randomClearsCount + "/" + reqClears);

            if (randomClearsCount >= reqClears) {
                // Completed all required random rooms!
                randomPhaseActive = false;
                nextId = encAfterClears;
                System.out.println("[Dungeons] Faza losowa ukonczona! Nastepny etap: " + nextId);
            } else {
                // Select next random room
                List<String> eligible = new ArrayList<>();
                for (String id : template.getEncounters().keySet()) {
                    if (!completedEncounters.contains(id) && !randomExcludes.contains(id)) {
                        eligible.add(id);
                    }
                }

                if (eligible.isEmpty()) {
                    // Repeat room fallback: reset completed list but keep excludes and current
                    completedEncounters.clear();
                    completedEncounters.add(current.getId());
                    for (String id : template.getEncounters().keySet()) {
                        if (!completedEncounters.contains(id) && !randomExcludes.contains(id)) {
                            eligible.add(id);
                        }
                    }
                }

                if (eligible.isEmpty()) {
                    // Still empty? Skip random phase and go to exit
                    randomPhaseActive = false;
                    nextId = encAfterClears;
                } else {
                    nextId = eligible.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligible.size()));
                }
            }
        } else if (!current.getNextEncounters().isEmpty()
                && current.getNextEncounters().get(0).equalsIgnoreCase("random")) {
            // Activate the random room progression phase!
            randomPhaseActive = true;
            randomClearsCount = 0;
            reqClears = current.getReqClears();
            encAfterClears = current.getEncAfterClears();
            randomExcludes.clear();
            randomExcludes.addAll(current.getExclude());

            System.out.println("[Dungeons] Inicjalizacja fazy losowej! Wymagane ukonczenia: " + reqClears
                    + ", Nastepny cel po zakonczeniu: " + encAfterClears);

            List<String> eligible = new ArrayList<>();
            for (String id : template.getEncounters().keySet()) {
                if (!completedEncounters.contains(id) && !randomExcludes.contains(id)) {
                    eligible.add(id);
                }
            }

            if (eligible.isEmpty()) {
                randomPhaseActive = false;
                nextId = encAfterClears;
            } else {
                nextId = eligible.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligible.size()));
            }
        } else {
            // Standard progression logic
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

        // Reset state for new encounter
        killedMobsCounter.clear();

        // Reset ready statuses for prep phases
        for (UUID uuid : readyPlayers.keySet()) {
            readyPlayers.put(uuid, false);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.getInventory().remove(Material.COMPASS);
            }
        }

        this.activeEncounterId = nextId;
        System.out.println("[Dungeons] Aktywowano nowy etap: " + nextId + " na " + world.getName());

        // 3. Trigger new active encounter effects
        executeActiveEncounterEffects();
    }

    private void executeActiveEncounterEffects() {
        Encounter encounter = getActiveEncounter();
        if (encounter != null) {
            for (DungeonEffect effect : encounter.getEffects()) {
                // Exclude spawn chest or complete dungeon on start unless they are prep effects
                if (effect.getType() != DungeonEffect.EffectType.COMPLETE_DUNGEON) {
                    effect.execute(this);
                }
            }
        }
    }

    /**
     * Safely ends the dungeon with success, awarding players, restoring
     * inventories, and deleting world.
     */
    public void completeDungeon() {
        if (isFinished)
            return;
        isFinished = true;

        broadcast(ChatColor.GREEN + "[Dungeons] ========================================");
        broadcast(ChatColor.YELLOW + "      DUNGEON UKONCZONY POMYSLNIE!");
        broadcast(ChatColor.GREEN + "[Dungeons] ========================================");

        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

        // Process rewards for each player
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {

                // 1. Restore Inventory
                DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);

                // 2. Reset Attributes
                DungeonPlayerStats stats = playerStatsMap.get(uuid);
                if (stats != null) {
                    stats.resetAttributes(player);
                }

                // Restore Gamemode
                player.setGameMode(GameMode.SURVIVAL);

                // Clear temporary abilities and elements
                clearPlayerTemporaryStatsAndAbilities(player);

                // 3. Award Dungeoneering EXP
                int xp = template.getRewards().getDungeonXp();
                if (xp > 0) {
                    AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.DUNGEON, player, xp);
                    player.sendMessage(
                            ChatColor.GOLD + "[Levels] " + ChatColor.YELLOW + "+" + xp + " EXP Eksploracji Dungeonow!");
                }

                // 4. Award Vault money (if commands or Vault API is used)
                double money = template.getRewards().getMoney();
                if (money > 0) {
                    player.sendMessage(
                            ChatColor.GOLD + "[Portfel] " + ChatColor.YELLOW + "+" + money + "$ za ukonczenie lochu!");
                }

                // 5. Award Items
                for (ItemStack item : template.getRewards().getItems()) {
                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(item.clone());
                    if (!left.isEmpty()) {
                        // Drop at their feet at survival exit point
                        Location dropLoc = getExitLocation();
                        for (ItemStack drop : left.values()) {
                            dropLoc.getWorld().dropItemNaturally(dropLoc, drop);
                        }
                        player.sendMessage(ChatColor.RED
                                + "Brak miejsca w ekwipunku! Nagrody zostaly upuszczone pod Twoimi nogami.");
                    }
                }

                // 6. Execute console commands
                for (String cmd : template.getRewards().getCommands()) {
                    String finalCmd = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(console, finalCmd);
                }

                // 7. Teleport to exit location
                player.teleport(getExitLocation());
                player.sendMessage(
                        ChatColor.GREEN + "Zostales bezpiecznie przeteleportowany z powrotem na glowny swiat.");
            }
        }

        // Delete backup files and dynamic world folder
        cleanupWorldAndBackups();
    }

    /**
     * Handles emergency cleanup (e.g., party failure, empty world, server
     * shutdown).
     */
    public void cleanup() {
        isFinished = true;

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Restore inventory
                DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);

                // Reset stats
                DungeonPlayerStats stats = playerStatsMap.get(uuid);
                if (stats != null) {
                    stats.resetAttributes(player);
                }

                // Restore Gamemode
                player.setGameMode(GameMode.SURVIVAL);

                // Clear temporary abilities and elements
                clearPlayerTemporaryStatsAndAbilities(player);

                // Teleport to exit
                player.teleport(getExitLocation());
                player.sendMessage(ChatColor.RED + "Dungeon zostal zamkniety. Przywrocono Twoj ekwipunek survivalowy.");
            }
        }

        cleanupWorldAndBackups();
    }

    private void cleanupWorldAndBackups() {
        // Delete all backup files on disk to prevent leaks
        File backupsFolder = new File(AmonPackPlugin.plugin.getDataFolder(), "backups");
        for (UUID uuid : players) {
            File file = new File(backupsFolder, uuid.toString() + ".yml");
            if (file.exists()) {
                file.delete();
            }
        }

        // Delete dynamic world
        DungeonWorldManager.deleteDungeonWorld(world);
    }

    /**
     * Teleports a single player out of the dungeon, restoring their items.
     */
    public void ejectPlayer(Player player) {
        if (player == null)
            return;
        UUID uuid = player.getUniqueId();

        if (players.contains(uuid)) {
            players.remove(uuid);
            readyPlayers.remove(uuid);
            spectatorPlayers.remove(uuid);

            // Restore inventory & stats
            DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);
            DungeonPlayerStats stats = playerStatsMap.remove(uuid);
            if (stats != null) {
                stats.resetAttributes(player);
            }

            player.setGameMode(GameMode.SURVIVAL);

            // Clear temporary abilities and elements
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
        // Increment kill counter
        String cleanName = mobName.toLowerCase();
        int count = killedMobsCounter.getOrDefault(cleanName, 0) + 1;
        killedMobsCounter.put(cleanName, count);

        // Check if this satisfies active conditions
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

            // Check if this satisfies conditions immediately
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

        // Get list of active living (non-spectator) online players
        List<UUID> activeLiving = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !isPlayerSpectator(p)) {
                activeLiving.add(uuid);
            }
        }

        // If everyone active has claimed their reward, break the chest
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
            
            // Remove temporary elements from ProjectKorra BendingPlayer
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
}
