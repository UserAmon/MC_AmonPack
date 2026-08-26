package RPG.Progression.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerProgressionData {

    private final UUID playerUuid;
    private String playerName;
    private StageType currentStage;
    private final Map<String, Integer> questProgress = new ConcurrentHashMap<>();
    private final Set<String> completedQuests = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> discoveredBiomes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> discoveredStructures = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private long stageUnlockTimestamp;
    private boolean dirty = false;

    public PlayerProgressionData(UUID playerUuid, String playerName) {
        this(playerUuid, playerName, StageType.WOODEN, System.currentTimeMillis());
    }

    public PlayerProgressionData(UUID playerUuid, String playerName, StageType currentStage, long stageUnlockTimestamp) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.currentStage = currentStage != null ? currentStage : StageType.WOODEN;
        this.stageUnlockTimestamp = stageUnlockTimestamp;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
        this.dirty = true;
    }

    public StageType getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(StageType currentStage) {
        this.currentStage = currentStage;
        this.stageUnlockTimestamp = System.currentTimeMillis();
        this.dirty = true;
    }

    public long getStageUnlockTimestamp() {
        return stageUnlockTimestamp;
    }

    public void setStageUnlockTimestamp(long stageUnlockTimestamp) {
        this.stageUnlockTimestamp = stageUnlockTimestamp;
        this.dirty = true;
    }

    public int getProgress(String questId) {
        if (questId == null) return 0;
        return questProgress.getOrDefault(questId.toLowerCase(Locale.ROOT), 0);
    }

    public void setProgress(String questId, int amount) {
        if (questId == null) return;
        questProgress.put(questId.toLowerCase(Locale.ROOT), amount);
        this.dirty = true;
    }

    public int addProgress(String questId, int amount) {
        if (questId == null) return 0;
        int updated = getProgress(questId) + amount;
        questProgress.put(questId.toLowerCase(Locale.ROOT), updated);
        this.dirty = true;
        return updated;
    }

    public boolean isQuestCompleted(String questId) {
        if (questId == null) return false;
        return completedQuests.contains(questId.toLowerCase(Locale.ROOT));
    }

    public void markQuestCompleted(String questId) {
        if (questId == null) return;
        completedQuests.add(questId.toLowerCase(Locale.ROOT));
        this.dirty = true;
    }

    public void unmarkQuestCompleted(String questId) {
        if (questId == null) return;
        completedQuests.remove(questId.toLowerCase(Locale.ROOT));
        this.dirty = true;
    }

    public Map<String, Integer> getQuestProgressMap() {
        return Collections.unmodifiableMap(questProgress);
    }

    public Set<String> getCompletedQuests() {
        return Collections.unmodifiableSet(completedQuests);
    }

    public boolean hasDiscoveredBiome(String biomeName) {
        if (biomeName == null) return false;
        return discoveredBiomes.contains(biomeName.toUpperCase(Locale.ROOT));
    }

    public boolean addDiscoveredBiome(String biomeName) {
        if (biomeName == null) return false;
        boolean added = discoveredBiomes.add(biomeName.toUpperCase(Locale.ROOT));
        if (added) this.dirty = true;
        return added;
    }

    public Set<String> getDiscoveredBiomes() {
        return Collections.unmodifiableSet(discoveredBiomes);
    }

    public boolean hasDiscoveredStructure(String structureName) {
        if (structureName == null) return false;
        return discoveredStructures.contains(structureName.toUpperCase(Locale.ROOT));
    }

    public boolean addDiscoveredStructure(String structureName) {
        if (structureName == null) return false;
        boolean added = discoveredStructures.add(structureName.toUpperCase(Locale.ROOT));
        if (added) this.dirty = true;
        return added;
    }

    public Set<String> getDiscoveredStructures() {
        return Collections.unmodifiableSet(discoveredStructures);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void resetProgress() {
        this.currentStage = StageType.WOODEN;
        this.questProgress.clear();
        this.completedQuests.clear();
        this.discoveredBiomes.clear();
        this.discoveredStructures.clear();
        this.stageUnlockTimestamp = System.currentTimeMillis();
        this.dirty = true;
    }
}
