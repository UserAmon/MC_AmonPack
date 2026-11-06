package RPG.BattleRoyale.Zombies;

import org.bukkit.Location;

/**
 * Przechowuje stan pamięci poszczególnych zombie na arenie.
 * Pozwala mobom badać ostatnie źródła hałasu, śledzić plamy krwi,
 * pamiętać ostatnią pozycję gracza oraz szukać obejść wokół barykad.
 */
public class ZombieMemory {

    private Location lastKnownNoiseLocation;
    private long noiseExpiryTime = 0L;

    private Location lastKnownPlayerLocation;
    private long playerExpiryTime = 0L;

    private Location lastKnownBloodLocation;
    private long bloodExpiryTime = 0L;

    private Location lastKnownBarricadeLocation;
    private long lastObstacleCheck = 0L;

    // Stan przeszukiwania okolicy po dojściu na miejsce hałasu
    private boolean isInvestigating = false;
    private long investigationEndTime = 0L;

    public void setNoiseLocation(Location loc, long durationMs) {
        this.lastKnownNoiseLocation = loc != null ? loc.clone() : null;
        this.noiseExpiryTime = System.currentTimeMillis() + durationMs;
        this.isInvestigating = false;
    }

    public Location getActiveNoiseLocation() {
        if (lastKnownNoiseLocation != null && System.currentTimeMillis() <= noiseExpiryTime) {
            return lastKnownNoiseLocation;
        }
        lastKnownNoiseLocation = null;
        return null;
    }

    public void clearNoise() {
        this.lastKnownNoiseLocation = null;
        this.noiseExpiryTime = 0L;
        this.isInvestigating = false;
    }

    public void startInvestigating(long durationMs) {
        this.isInvestigating = true;
        this.investigationEndTime = System.currentTimeMillis() + durationMs;
    }

    public boolean isInvestigating() {
        if (isInvestigating && System.currentTimeMillis() <= investigationEndTime) {
            return true;
        }
        isInvestigating = false;
        return false;
    }

    public void setBloodLocation(Location loc, long durationMs) {
        this.lastKnownBloodLocation = loc != null ? loc.clone() : null;
        this.bloodExpiryTime = System.currentTimeMillis() + durationMs;
    }

    public Location getActiveBloodLocation() {
        if (lastKnownBloodLocation != null && System.currentTimeMillis() <= bloodExpiryTime) {
            return lastKnownBloodLocation;
        }
        lastKnownBloodLocation = null;
        return null;
    }

    public void clearBlood() {
        this.lastKnownBloodLocation = null;
        this.bloodExpiryTime = 0L;
    }

    public void setPlayerLocation(Location loc, long durationMs) {
        this.lastKnownPlayerLocation = loc != null ? loc.clone() : null;
        this.playerExpiryTime = System.currentTimeMillis() + durationMs;
    }

    public Location getActivePlayerLocation() {
        if (lastKnownPlayerLocation != null && System.currentTimeMillis() <= playerExpiryTime) {
            return lastKnownPlayerLocation;
        }
        lastKnownPlayerLocation = null;
        return null;
    }

    public Location getLastKnownBarricadeLocation() { return lastKnownBarricadeLocation; }
    public void setLastKnownBarricadeLocation(Location loc) { this.lastKnownBarricadeLocation = loc; }

    public long getLastObstacleCheck() { return lastObstacleCheck; }
    public void setLastObstacleCheck(long t) { this.lastObstacleCheck = t; }
}
