package RPG.Progression.storage;

import RPG.Progression.model.PlayerProgressionData;

import java.util.Collection;
import java.util.UUID;

public interface IProgressionStorage {

    void init();

    PlayerProgressionData loadPlayer(UUID playerUuid, String playerName);

    void savePlayer(PlayerProgressionData data);

    void saveAll(Collection<PlayerProgressionData> allData);

    void close();
}
