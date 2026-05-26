package RPG.Party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Party {
    private final UUID partyId;
    private UUID leaderUUID;
    private final List<UUID> members = new ArrayList<>();
    private boolean friendlyFire = false; // default friendly fire is disabled (no pvp within party)

    public Party(UUID partyId, UUID leaderUUID) {
        this.partyId = partyId;
        this.leaderUUID = leaderUUID;
        this.members.add(leaderUUID);
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getLeaderUUID() {
        return leaderUUID;
    }

    public void setLeaderUUID(UUID leaderUUID) {
        if (members.contains(leaderUUID)) {
            this.leaderUUID = leaderUUID;
        }
    }

    public List<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID member) {
        if (!members.contains(member)) {
            members.add(member);
        }
    }

    public void removeMember(UUID member) {
        members.remove(member);
    }

    public boolean isFriendlyFireEnabled() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }
}
