package RPG.BattleRoyale;

import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Keys.KeyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BattleRoyaleManager {

    private static BattleRoyaleManager instance;
    private FileConfiguration config;
    private File configFile;
    private BattleRoyaleArena defaultArena;

    private BattleRoyaleLobby currentLobby;
    private BattleRoyaleGame currentGame;

    private final BattleRoyaleLootManager lootManager = new BattleRoyaleLootManager();
    private final GroundLootManager groundLootManager = new GroundLootManager();
    private final KeyManager keyManager = new KeyManager();

    public BattleRoyaleManager() {
        instance = this;
    }

    public static BattleRoyaleManager getInstance() {
        if (instance == null) {
            instance = new BattleRoyaleManager();
        }
        return instance;
    }

    public void init() {
        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(AmonPackPlugin.plugin.getDataFolder(), "battleroyale.yml");
        if (!configFile.exists()) {
            AmonPackPlugin.plugin.saveResource("battleroyale.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        defaultArena = BattleRoyaleArena.fromConfig(config);
        lootManager.loadFromConfig(config);
        groundLootManager.loadFromConfig(config);
        keyManager.loadFromConfig(config);
        AmonPackPlugin.plugin.getLogger().info("[BattleRoyale] Załadowano konfigurację areny: " + defaultArena.getName());
    }

    public void saveCarLocationsToConfig() {
        if (config != null && defaultArena != null) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Location loc : defaultArena.getCarLocations()) {
                Map<String, Object> map = new HashMap<>();
                map.put("x", loc.getBlockX());
                map.put("y", loc.getBlockY());
                map.put("z", loc.getBlockZ());
                list.add(map);
            }
            config.set("arena.car-locations", list);
        }
    }

    public void saveConfig() {
        if (config != null && configFile != null) {
            try {
                lootManager.saveThemedChestsToConfig(config, configFile);
                groundLootManager.saveToConfig(config, configFile);
                keyManager.saveToConfig(config, configFile);
                saveCarLocationsToConfig();
                config.save(configFile);
            } catch (Exception e) {
                AmonPackPlugin.plugin.getLogger().warning("[BattleRoyale] Błąd zapisu battleroyale.yml: " + e.getMessage());
            }
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    public static class MapRelocationResult {
        public final int dx, dy, dz;
        public final String newWorld;
        public final int spawnsCount;
        public final int chestsCount;
        public final int groundLootCount;
        public final int carsCount;
        public final int doorsCount;

        public MapRelocationResult(int dx, int dy, int dz, String newWorld, int spawnsCount, int chestsCount, int groundLootCount, int carsCount, int doorsCount) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.newWorld = newWorld;
            this.spawnsCount = spawnsCount;
            this.chestsCount = chestsCount;
            this.groundLootCount = groundLootCount;
            this.carsCount = carsCount;
            this.doorsCount = doorsCount;
        }
    }

    /**
     * Przesuwa wszystkie zarejestrowane koordynaty areny (skrzynie, groundloot, drzwi, spawny, pojazdy, paste, center)
     * o wektor [dx, dy, dz] i opcjonalnie przypisuje je do nowego świata.
     * Aktualizuje konfigurację w locie i zapisuje battleroyale.yml na dysku.
     */
    public MapRelocationResult shiftAllCoordinates(int dx, int dy, int dz, String newWorldName) {
        if (defaultArena == null || config == null) return null;

        if (newWorldName != null && !newWorldName.trim().isEmpty()) {
            defaultArena.setWorldName(newWorldName.trim());
            config.set("arena.world-name", newWorldName.trim());
        }

        World targetWorld = Bukkit.getWorld(defaultArena.getWorldName());

        // 1. Paste location
        if (defaultArena.getPasteLocation() != null) {
            Location oldP = defaultArena.getPasteLocation();
            Location newP = new Location(targetWorld != null ? targetWorld : oldP.getWorld(),
                    oldP.getX() + dx, oldP.getY() + dy, oldP.getZ() + dz,
                    oldP.getYaw(), oldP.getPitch());
            defaultArena.setPasteLocation(newP);
            config.set("arena.paste-location.x", newP.getX());
            config.set("arena.paste-location.y", newP.getY());
            config.set("arena.paste-location.z", newP.getZ());
        }

        // 2. Center location
        if (defaultArena.getCenterLocation() != null) {
            Location oldC = defaultArena.getCenterLocation();
            Location newC = new Location(targetWorld != null ? targetWorld : oldC.getWorld(),
                    oldC.getX() + dx, oldC.getY() + dy, oldC.getZ() + dz,
                    oldC.getYaw(), oldC.getPitch());
            defaultArena.setCenterLocation(newC);
            config.set("arena.center-location.x", newC.getX());
            config.set("arena.center-location.y", newC.getY());
            config.set("arena.center-location.z", newC.getZ());
        }

        // 3. Spawny
        List<Location> newSpawns = new ArrayList<>();
        List<Map<String, Object>> spawnsList = new ArrayList<>();
        for (Location sp : defaultArena.getSpawnLocations()) {
            Location nSp = new Location(targetWorld != null ? targetWorld : sp.getWorld(),
                    sp.getX() + dx, sp.getY() + dy, sp.getZ() + dz,
                    sp.getYaw(), sp.getPitch());
            newSpawns.add(nSp);

            Map<String, Object> m = new HashMap<>();
            m.put("x", nSp.getX());
            m.put("y", nSp.getY());
            m.put("z", nSp.getZ());
            m.put("yaw", nSp.getYaw());
            m.put("pitch", nSp.getPitch());
            spawnsList.add(m);
        }
        defaultArena.getSpawnLocations().clear();
        defaultArena.getSpawnLocations().addAll(newSpawns);
        config.set("arena.spawns", spawnsList);

        // 4. Pojazdy
        List<Location> newCars = new ArrayList<>();
        for (Location c : defaultArena.getCarLocations()) {
            newCars.add(new Location(targetWorld != null ? targetWorld : c.getWorld(),
                    c.getX() + dx, c.getY() + dy, c.getZ() + dz,
                    c.getYaw(), c.getPitch()));
        }
        defaultArena.setCarLocations(newCars);
        saveCarLocationsToConfig();

        // 5. Skrzynie tematyczne
        int chestsCount = lootManager.getConfiguredThemedChests().size();
        lootManager.shiftChests(dx, dy, dz, targetWorld);

        // 6. Ground loot
        int groundCount = groundLootManager.getConfiguredPoints().size();
        groundLootManager.shiftPoints(dx, dy, dz, targetWorld);

        // 7. Drzwi na klucze
        int doorsCount = keyManager.getLockedDoors().size();
        keyManager.shiftDoors(dx, dy, dz, targetWorld);

        // Zapis do battleroyale.yml
        saveConfig();

        return new MapRelocationResult(dx, dy, dz, defaultArena.getWorldName(),
                newSpawns.size(), chestsCount, groundCount, newCars.size(), doorsCount);
    }

    public boolean createLobby(Player host) {
        if (currentGame != null && currentGame.getState() != BattleRoyaleGame.GameState.ENDED) {
            host.sendMessage(ChatColor.RED + "[BattleRoyale] Trwa już aktywny mecz Battle Royale!");
            return false;
        }
        if (currentLobby != null) {
            host.sendMessage(ChatColor.YELLOW + "[BattleRoyale] Lobby jest już otwarte! Użyj /hungergames join aby dołączyć.");
            return false;
        }

        currentLobby = new BattleRoyaleLobby(defaultArena, this, host);
        currentLobby.start();
        return true;
    }

    public void launchGame(BattleRoyaleArena arena, List<Player> players) {
        currentLobby = null;
        currentGame = new BattleRoyaleGame(arena, this, players);
        currentGame.start();
    }

    public void onGameEnd(BattleRoyaleGame game) {
        if (this.currentGame == game) {
            this.currentGame = null;
        }
    }

    public void clearLobby() {
        this.currentLobby = null;
    }

    public BattleRoyaleGame getGameByPlayer(Player player) {
        if (currentGame != null && currentGame.isPlayerInGame(player)) {
            return currentGame;
        }
        return null;
    }

    public BattleRoyaleGame getGameByWorld(World world) {
        if (currentGame != null && currentGame.getWorld() != null && currentGame.getWorld().equals(world)) {
            return currentGame;
        }
        return null;
    }

    public void shutdown() {
        if (currentLobby != null) {
            currentLobby.cancelLobby("Wyłączenie serwera/pluginu.");
            currentLobby = null;
        }
        if (currentGame != null) {
            currentGame.endGame();
            currentGame = null;
        }
    }

    public FileConfiguration getConfig() { return config; }
    public File getConfigFile() { return configFile; }
    public BattleRoyaleArena getDefaultArena() { return defaultArena; }
    public BattleRoyaleLobby getCurrentLobby() { return currentLobby; }
    public BattleRoyaleGame getCurrentGame() { return currentGame; }
    public BattleRoyaleLootManager getLootManager() { return lootManager; }
    public GroundLootManager getGroundLootManager() { return groundLootManager; }
    public KeyManager getKeyManager() { return keyManager; }
}
