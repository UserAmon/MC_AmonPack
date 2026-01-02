package AvatarSystems.Bounties.Objects;

import java.util.HashMap;
import java.util.Map;

public class PlayerBountyData {
    private String playerName;
    private Map<String, Integer> activeBounties; // BountyID -> Progress
    private Map<String, Boolean> completedBounties; // BountyID -> IsClaimed (or just completed today)
    private long lastResetTime;

    public PlayerBountyData(String playerName) {
        this.playerName = playerName;
        this.activeBounties = new HashMap<>();
        this.completedBounties = new HashMap<>();
        this.lastResetTime = System.currentTimeMillis();
    }

    public String getPlayerName() {
        return playerName;
    }

    public Map<String, Integer> getActiveBounties() {
        return activeBounties;
    }

    public void setActiveBounties(Map<String, Integer> activeBounties) {
        this.activeBounties = activeBounties;
    }

    public Map<String, Boolean> getCompletedBounties() {
        return completedBounties;
    }

    public void setCompletedBounties(Map<String, Boolean> completedBounties) {
        this.completedBounties = completedBounties;
    }

    public long getLastResetTime() {
        return lastResetTime;
    }

    public void setLastResetTime(long lastResetTime) {
        this.lastResetTime = lastResetTime;
    }

    public void addProgress(String bountyId, int amount) {
        if (activeBounties.containsKey(bountyId)) {
            activeBounties.put(bountyId, activeBounties.get(bountyId) + amount);
        }
    }

    public int getProgress(String bountyId) {
        return activeBounties.getOrDefault(bountyId, 0);
    }

    public boolean isCompleted(String bountyId) {
        return completedBounties.containsKey(bountyId);
    }

    public void completeBounty(String bountyId) {
        completedBounties.put(bountyId, true);
    }
}
