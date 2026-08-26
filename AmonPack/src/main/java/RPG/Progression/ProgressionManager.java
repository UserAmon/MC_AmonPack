package RPG.Progression;

import Plugin.AmonPackPlugin;
import RPG.Progression.command.ProgressionCommand;
import RPG.Progression.command.ProgressionTabCompleter;
import RPG.Progression.gui.ProgressionGuiListener;
import RPG.Progression.listener.*;
import RPG.Progression.service.*;
import RPG.Progression.storage.IProgressionStorage;
import RPG.Progression.storage.SQLiteProgressionStorage;
import RPG.Progression.storage.YamlProgressionStorage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ProgressionManager {

    private static ProgressionManager instance;

    private final QuestRegistry questRegistry;
    private final StageService stageService;
    private final RewardService rewardService;
    private final RestrictionService restrictionService;
    private final ExplorationService explorationService;
    private final ProgressionService progressionService;
    private final IProgressionStorage storage;

    private FileConfiguration config;
    private File configFile;
    private BukkitTask autoSaveTask;

    public ProgressionManager() {
        instance = this;

        this.questRegistry = new QuestRegistry();
        this.stageService = new StageService(questRegistry);
        this.rewardService = new RewardService();
        this.restrictionService = new RestrictionService();
        this.explorationService = new ExplorationService();

        if (AmonPackPlugin.ENABLE_DATABASE) {
            this.storage = new SQLiteProgressionStorage();
        } else {
            this.storage = new YamlProgressionStorage();
        }

        this.progressionService = new ProgressionService(questRegistry, stageService, rewardService, storage);
    }

    public static ProgressionManager getInstance() {
        return instance;
    }

    public void load() {
        // 1. Save and load slow_progression.yml config
        File progFolder = new File(AmonPackPlugin.plugin.getDataFolder(), "progression");
        if (!progFolder.exists()) progFolder.mkdirs();

        configFile = new File(progFolder, "slow_progression.yml");
        if (!configFile.exists()) {
            AmonPackPlugin.plugin.saveResource("progression/slow_progression.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defStream = AmonPackPlugin.plugin.getResource("progression/slow_progression.yml");
        if (defStream == null) {
            defStream = AmonPackPlugin.plugin.getResource("slow_progression.yml");
        }
        if (defStream != null) {
            YamlConfiguration defCfg = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defCfg);
        }

        // 2. Init storage
        storage.init();

        // 3. Load configurations into registries
        questRegistry.load(config);
        stageService.load(config);
        restrictionService.load(config);

        // 4. Register Listeners
        Bukkit.getPluginManager().registerEvents(new ProgressionBlockListener(progressionService, restrictionService), AmonPackPlugin.plugin);
        Bukkit.getPluginManager().registerEvents(new ProgressionCraftListener(progressionService, restrictionService), AmonPackPlugin.plugin);
        Bukkit.getPluginManager().registerEvents(new ProgressionCombatListener(progressionService), AmonPackPlugin.plugin);
        Bukkit.getPluginManager().registerEvents(new ProgressionInteractListener(progressionService, restrictionService), AmonPackPlugin.plugin);
        Bukkit.getPluginManager().registerEvents(new ProgressionExplorationListener(progressionService, explorationService, restrictionService), AmonPackPlugin.plugin);
        Bukkit.getPluginManager().registerEvents(new ProgressionIntegrationListener(progressionService), AmonPackPlugin.plugin);
        Bukkit.getPluginManager().registerEvents(new ProgressionGuiListener(), AmonPackPlugin.plugin);

        // 5. Register Commands
        org.bukkit.plugin.java.JavaPlugin javaPlugin = (org.bukkit.plugin.java.JavaPlugin) AmonPackPlugin.plugin;
        if (javaPlugin.getCommand("progression") != null) {
            ProgressionCommand cmd = new ProgressionCommand();
            javaPlugin.getCommand("progression").setExecutor(cmd);
            javaPlugin.getCommand("progression").setTabCompleter(new ProgressionTabCompleter());
        }

        // 6. Load online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            progressionService.loadPlayer(p);
        }

        // 7. Auto-save task (every 5 minutes = 6000 ticks)
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(AmonPackPlugin.plugin, () -> {
            try {
                progressionService.saveAllOnline();
            } catch (Exception e) {
                Bukkit.getLogger().warning("[SlowProgression] Błąd podczas okresowego autosave: " + e.getMessage());
            }
        }, 6000L, 6000L);

        Bukkit.getLogger().info("[SlowProgression] System Slow Progression został pomyślnie załadowany.");
    }

    public void reload() {
        if (configFile == null) {
            configFile = new File(new File(AmonPackPlugin.plugin.getDataFolder(), "progression"), "slow_progression.yml");
        }
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        questRegistry.load(config);
        stageService.load(config);
        restrictionService.load(config);
    }

    public void unload() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        if (progressionService != null) {
            progressionService.saveAllOnline();
        }
        if (storage != null) {
            storage.close();
        }
        instance = null;
        Bukkit.getLogger().info("[SlowProgression] System Slow Progression został wyłączony.");
    }

    public QuestRegistry getQuestRegistry() {
        return questRegistry;
    }

    public StageService getStageService() {
        return stageService;
    }

    public RewardService getRewardService() {
        return rewardService;
    }

    public RestrictionService getRestrictionService() {
        return restrictionService;
    }

    public ExplorationService getExplorationService() {
        return explorationService;
    }

    public ProgressionService getProgressionService() {
        return progressionService;
    }

    public IProgressionStorage getStorage() {
        return storage;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
