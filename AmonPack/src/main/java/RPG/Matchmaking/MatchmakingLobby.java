package RPG.Matchmaking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchmakingLobby {
    private final UUID ownerUUID;
    private final String ownerName;
    private final String dungeonId;
    private final List<UUID> temporaryMembers = new ArrayList<>();
    private final List<UUID> pendingRequests = new ArrayList<>();

    public MatchmakingLobby(UUID ownerUUID, String ownerName, String dungeonId) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.dungeonId = dungeonId;
        this.temporaryMembers.add(ownerUUID);
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public List<UUID> getTemporaryMembers() {
        return temporaryMembers;
    }

    public List<UUID> getPendingRequests() {
        return pendingRequests;
    }

    public void addTemporaryMember(UUID uuid) {
        if (!temporaryMembers.contains(uuid)) {
            temporaryMembers.add(uuid);
        }
        pendingRequests.remove(uuid);
    }

    public void removeTemporaryMember(UUID uuid) {
        temporaryMembers.remove(uuid);
    }

    public void addRequest(UUID uuid) {
        if (!pendingRequests.contains(uuid) && !temporaryMembers.contains(uuid)) {
            pendingRequests.add(uuid);
        }
    }

    public void removeRequest(UUID uuid) {
        pendingRequests.remove(uuid);
    }
}
