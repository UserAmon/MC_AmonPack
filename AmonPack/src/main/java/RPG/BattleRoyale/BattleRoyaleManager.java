package RPG.BattleRoyale;

import Plugin.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

public class BattleRoyaleManager {

    private static BattleRoyaleManager instance;
    private FileConfiguration config;
    private File configFile;
    private BattleRoyaleArena defaultArena;

    private BattleRoyaleLobby currentLobby;
    private BattleRoyaleGame currentGame;

    private final BattleRoyaleLootManager lootManager = new BattleRoyaleLootManager();
    private final GroundLootManager groundLootManager = new GroundLootManager();

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
        AmonPackPlugin.plugin.getLogger().info("[BattleRoyale] Załadowano konfigurację areny: " + defaultArena.getName());
    }

    public void saveConfig() {
        if (config != null && configFile != null) {
            try {
                lootManager.saveThemedChestsToConfig(config, configFile);
                groundLootManager.saveToConfig(config, configFile);
                config.save(configFile);
            } catch (Exception e) {
                AmonPackPlugin.plugin.getLogger().warning("[BattleRoyale] Błąd zapisu battleroyale.yml: " + e.getMessage());
            }
        }
    }

    public void reloadConfig() {
        loadConfig();
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
}
