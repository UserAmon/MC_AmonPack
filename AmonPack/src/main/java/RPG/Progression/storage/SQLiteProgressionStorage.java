package RPG.Progression.storage;

import Plugin.AmonPackPlugin;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.StageType;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;

public class SQLiteProgressionStorage implements IProgressionStorage {

    private Connection getConnection() throws SQLException {
        if (AmonPackPlugin.sqlite != null && AmonPackPlugin.sqlite.getConnection() != null) {
            return AmonPackPlugin.sqlite.getConnection();
        }
        return null;
    }

    @Override
    public void init() {
        try {
            Connection conn = getConnection();
            if (conn == null) {
                Bukkit.getLogger().warning("[SlowProgression] Brak aktywnego połączenia SQLite, tabele nie zostały utworzone.");
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ProgressionPlayers (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "player_name VARCHAR(50), " +
                        "stage VARCHAR(20), " +
                        "unlock_time BIGINT" +
                        ")");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ProgressionQuests (" +
                        "uuid VARCHAR(36), " +
                        "quest_id VARCHAR(100), " +
                        "progress INT, " +
                        "completed INT, " +
                        "PRIMARY KEY(uuid, quest_id)" +
                        ")");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ProgressionBiomes (" +
                        "uuid VARCHAR(36), " +
                        "biome_name VARCHAR(100), " +
                        "PRIMARY KEY(uuid, biome_name)" +
                        ")");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ProgressionStructures (" +
                        "uuid VARCHAR(36), " +
                        "structure_name VARCHAR(100), " +
                        "PRIMARY KEY(uuid, structure_name)" +
                        ")");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[SlowProgression] Błąd podczas inicjalizacji bazy SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public PlayerProgressionData loadPlayer(UUID playerUuid, String playerName) {
        PlayerProgressionData data = new PlayerProgressionData(playerUuid, playerName);
        try {
            Connection conn = getConnection();
            if (conn == null) return data;

            // 1. Load Player
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ProgressionPlayers WHERE uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String stageStr = rs.getString("stage");
                        long unlockTime = rs.getLong("unlock_time");
                        data.setCurrentStage(StageType.fromName(stageStr));
                        data.setStageUnlockTimestamp(unlockTime);
                    } else {
                        // New player in DB, save immediately
                        savePlayer(data);
                        return data;
                    }
                }
            }

            // 2. Load Quests
            try (PreparedStatement ps = conn.prepareStatement("SELECT quest_id, progress, completed FROM ProgressionQuests WHERE uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String qId = rs.getString("quest_id");
                        int prog = rs.getInt("progress");
                        int comp = rs.getInt("completed");
                        data.setProgress(qId, prog);
                        if (comp == 1) {
                            data.markQuestCompleted(qId);
                        }
                    }
                }
            }

            // 3. Load Biomes
            try (PreparedStatement ps = conn.prepareStatement("SELECT biome_name FROM ProgressionBiomes WHERE uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        data.addDiscoveredBiome(rs.getString("biome_name"));
                    }
                }
            }

            // 4. Load Structures
            try (PreparedStatement ps = conn.prepareStatement("SELECT structure_name FROM ProgressionStructures WHERE uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        data.addDiscoveredStructure(rs.getString("structure_name"));
                    }
                }
            }

            data.setDirty(false);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[SlowProgression] Błąd podczas wczytywania gracza " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    @Override
    public void savePlayer(PlayerProgressionData data) {
        if (data == null) return;
        try {
            Connection conn = getConnection();
            if (conn == null) return;

            // 1. Save Player Record (UPSERT)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ProgressionPlayers (uuid, player_name, stage, unlock_time) VALUES (?, ?, ?, ?) " +
                            "ON CONFLICT(uuid) DO UPDATE SET player_name = excluded.player_name, stage = excluded.stage, unlock_time = excluded.unlock_time")) {
                ps.setString(1, data.getPlayerUuid().toString());
                ps.setString(2, data.getPlayerName());
                ps.setString(3, data.getCurrentStage().name());
                ps.setLong(4, data.getStageUnlockTimestamp());
                ps.executeUpdate();
            }

            // 2. Save Quests
            for (Map.Entry<String, Integer> entry : data.getQuestProgressMap().entrySet()) {
                String qId = entry.getKey();
                int progress = entry.getValue();
                int completed = data.isQuestCompleted(qId) ? 1 : 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ProgressionQuests (uuid, quest_id, progress, completed) VALUES (?, ?, ?, ?) " +
                                "ON CONFLICT(uuid, quest_id) DO UPDATE SET progress = excluded.progress, completed = excluded.completed")) {
                    ps.setString(1, data.getPlayerUuid().toString());
                    ps.setString(2, qId);
                    ps.setInt(3, progress);
                    ps.setInt(4, completed);
                    ps.executeUpdate();
                }
            }

            // 3. Save Biomes
            for (String biome : data.getDiscoveredBiomes()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO ProgressionBiomes (uuid, biome_name) VALUES (?, ?)")) {
                    ps.setString(1, data.getPlayerUuid().toString());
                    ps.setString(2, biome);
                    ps.executeUpdate();
                }
            }

            // 4. Save Structures
            for (String struct : data.getDiscoveredStructures()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO ProgressionStructures (uuid, structure_name) VALUES (?, ?)")) {
                    ps.setString(1, data.getPlayerUuid().toString());
                    ps.setString(2, struct);
                    ps.executeUpdate();
                }
            }

            data.setDirty(false);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[SlowProgression] Błąd podczas zapisu gracza " + data.getPlayerName() + ": " + e.getMessage());
            e.printStackTrace();
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
        // Shared SQLite connection handled by AmonPackPlugin
    }
}
