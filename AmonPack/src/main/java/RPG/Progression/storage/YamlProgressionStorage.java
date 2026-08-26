package RPG.Progression.storage;

import Plugin.AmonPackPlugin;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.StageType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlProgressionStorage implements IProgressionStorage {

    private File dataFolder;

    @Override
    public void init() {
        dataFolder = new File(new File(AmonPackPlugin.plugin.getDataFolder(), "progression"), "player_data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    public PlayerProgressionData loadPlayer(UUID playerUuid, String playerName) {
        if (dataFolder == null) init();
        File file = new File(dataFolder, playerUuid.toString() + ".yml");
        PlayerProgressionData data = new PlayerProgressionData(playerUuid, playerName);

        if (!file.exists()) {
            savePlayer(data);
            return data;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        data.setPlayerName(cfg.getString("player_name", playerName));
        data.setCurrentStage(StageType.fromName(cfg.getString("stage", "WOODEN")));
        data.setStageUnlockTimestamp(cfg.getLong("unlock_time", System.currentTimeMillis()));

        // Quests
        ConfigurationSection questsSec = cfg.getConfigurationSection("quests");
        if (questsSec != null) {
            for (String qId : questsSec.getKeys(false)) {
                int prog = questsSec.getInt(qId + ".progress", 0);
                boolean comp = questsSec.getBoolean(qId + ".completed", false);
                data.setProgress(qId, prog);
                if (comp) {
                    data.markQuestCompleted(qId);
                }
            }
        }

        // Biomes
        List<String> biomes = cfg.getStringList("biomes");
        if (biomes != null) {
            for (String b : biomes) {
                data.addDiscoveredBiome(b);
            }
        }

        // Structures
        List<String> structs = cfg.getStringList("structures");
        if (structs != null) {
            for (String s : structs) {
                data.addDiscoveredStructure(s);
            }
        }

        data.setDirty(false);
        return data;
    }

    @Override
    public void savePlayer(PlayerProgressionData data) {
        if (data == null) return;
        if (dataFolder == null) init();
        File file = new File(dataFolder, data.getPlayerUuid().toString() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("uuid", data.getPlayerUuid().toString());
        cfg.set("player_name", data.getPlayerName());
        cfg.set("stage", data.getCurrentStage().name());
        cfg.set("unlock_time", data.getStageUnlockTimestamp());

        // Quests
        for (Map.Entry<String, Integer> entry : data.getQuestProgressMap().entrySet()) {
            String qId = entry.getKey();
            cfg.set("quests." + qId + ".progress", entry.getValue());
            cfg.set("quests." + qId + ".completed", data.isQuestCompleted(qId));
        }

        // Biomes
        cfg.set("biomes", new ArrayList<>(data.getDiscoveredBiomes()));

        // Structures
        cfg.set("structures", new ArrayList<>(data.getDiscoveredStructures()));

        try {
            cfg.save(file);
            data.setDirty(false);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[SlowProgression] Błąd podczas zapisu pliku YAML gracza " + data.getPlayerName() + ": " + e.getMessage());
        }
    }

    @Override
    public void saveAll(Collection<PlayerProgressionData> allData) {
        if (allData == null || allData.isEmpty()) return;
        for (PlayerProgressionData data : allData) {
            if (data.isDirty()) {
                savePlayer(data);
            }
        }
    }

    @Override
    public void close() {
    }
}
