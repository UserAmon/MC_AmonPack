package RPG.Progression.service;

import Plugin.AmonPackPlugin;
import RPG.Progression.event.QuestCompleteEvent;
import RPG.Progression.model.ObjectiveType;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.Quest;
import RPG.Progression.model.StageType;
import RPG.Progression.storage.IProgressionStorage;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProgressionService {

    private final QuestRegistry questRegistry;
    private final StageService stageService;
    private final RewardService rewardService;
    private final IProgressionStorage storage;
    private final Map<UUID, PlayerProgressionData> playerDataMap = new ConcurrentHashMap<>();

    public ProgressionService(QuestRegistry questRegistry, StageService stageService, RewardService rewardService, IProgressionStorage storage) {
        this.questRegistry = questRegistry;
        this.stageService = stageService;
        this.rewardService = rewardService;
        this.storage = storage;
    }

    public PlayerProgressionData getPlayerData(Player player) {
        if (player == null) return null;
        return playerDataMap.computeIfAbsent(player.getUniqueId(), uuid -> storage.loadPlayer(uuid, player.getName()));
    }

    public PlayerProgressionData getPlayerData(UUID uuid) {
        if (uuid == null) return null;
        return playerDataMap.get(uuid);
    }

    public void loadPlayer(Player player) {
        if (player == null) return;
        PlayerProgressionData data = storage.loadPlayer(player.getUniqueId(), player.getName());
        playerDataMap.put(player.getUniqueId(), data);
    }

    public void unloadPlayer(Player player) {
        if (player == null) return;
        PlayerProgressionData data = playerDataMap.remove(player.getUniqueId());
        if (data != null && data.isDirty()) {
            storage.savePlayer(data);
        }
    }

    public void saveAllOnline() {
        storage.saveAll(playerDataMap.values());
    }

    public void handleObjective(Player player, ObjectiveType type, String target, int amount) {
        if (player == null || type == null || amount <= 0) return;
        PlayerProgressionData data = getPlayerData(player);
        if (data == null) return;

        StageType currentStage = data.getCurrentStage();
        List<Quest> stageQuests = questRegistry.getQuestsForStage(currentStage);

        for (Quest quest : stageQuests) {
            if (quest.getObjectiveType() != type) continue;
            if (data.isQuestCompleted(quest.getId())) continue;

            if (!matchesTarget(quest, type, target)) continue;

            // Process progress
            boolean completed = false;

            if (type == ObjectiveType.MINE_TO_DEPTH) {
                try {
                    int targetDepth = Integer.parseInt(quest.getTarget());
                    int currentY = Integer.parseInt(target);
                    if (currentY <= targetDepth) {
                        data.setProgress(quest.getId(), 1);
                        completed = true;
                    }
                } catch (Exception ignored) {
                }
            } else if (type == ObjectiveType.TRAVEL_DISTANCE) {
                int currentDist = amount;
                int reqDist = quest.getRequiredAmount();
                data.setProgress(quest.getId(), Math.min(reqDist, currentDist));
                if (currentDist >= reqDist) {
                    completed = true;
                }
            } else if (type == ObjectiveType.REACH_SKILL_LEVEL) {
                int currentLvl = amount;
                int reqLvl = quest.getRequiredAmount();
                data.setProgress(quest.getId(), Math.min(reqLvl, currentLvl));
                if (currentLvl >= reqLvl) {
                    completed = true;
                }
            } else {
                // Cumulative progress
                int newProg = data.addProgress(quest.getId(), amount);
                if (newProg >= quest.getRequiredAmount()) {
                    completed = true;
                } else {
                    // Send actionbar progress feedback
                    sendActionBarProgress(player, quest, newProg);
                }
            }

            if (completed) {
                completeQuest(player, data, quest);
            }
        }
    }

    private boolean matchesTarget(Quest quest, ObjectiveType type, String target) {
        if (quest.getTarget() == null || quest.getTarget().isEmpty()) return true;
        if (target == null) return false;

        String qTarget = quest.getTarget().trim();
        String t = target.trim();

        if (qTarget.equalsIgnoreCase("ANY") || qTarget.equalsIgnoreCase("*")) return true;

        if (qTarget.equalsIgnoreCase(t)) return true;

        // Multi-target or aliases support (e.g. "OAK_LOG,BIRCH_LOG,SPRUCE_LOG" or "HOSTILE")
        if (qTarget.contains(",")) {
            for (String part : qTarget.split(",")) {
                if (part.trim().equalsIgnoreCase(t)) return true;
            }
        }

        // Hostile mob category alias
        if (qTarget.equalsIgnoreCase("HOSTILE")) {
            return isHostileMob(t);
        }

        // Wood alias
        if (qTarget.equalsIgnoreCase("WOOD") || qTarget.equalsIgnoreCase("LOG")) {
            return t.toUpperCase().endsWith("_LOG") || t.toUpperCase().endsWith("_WOOD") || t.toUpperCase().endsWith("_STEM");
        }

        // Planks alias
        if (qTarget.equalsIgnoreCase("PLANKS")) {
            return t.toUpperCase().endsWith("_PLANKS");
        }

        // Crops alias
        if (qTarget.equalsIgnoreCase("CROP") || qTarget.equalsIgnoreCase("CROPS")) {
            return t.equalsIgnoreCase("WHEAT") || t.equalsIgnoreCase("CARROTS") || t.equalsIgnoreCase("POTATOES") || t.equalsIgnoreCase("BEETROOTS");
        }

        // Fish alias
        if (qTarget.equalsIgnoreCase("FISH")) {
            return t.equalsIgnoreCase("COD") || t.equalsIgnoreCase("SALMON") || t.equalsIgnoreCase("TROPICAL_FISH") || t.equalsIgnoreCase("PUFFERFISH");
        }

        // Meat/Food alias
        if (qTarget.equalsIgnoreCase("FOOD") || qTarget.equalsIgnoreCase("MEAT")) {
            return t.contains("BEEF") || t.contains("PORKCHOP") || t.contains("CHICKEN") || t.contains("MUTTON") || t.contains("RABBIT") || t.contains("FISH") || t.contains("BREAD");
        }

        // Full armor set check aliases
        if (qTarget.toUpperCase().startsWith("ARMOR_SET_")) {
            String armorType = qTarget.substring("ARMOR_SET_".length()); // e.g. LEATHER, IRON, DIAMOND, NETHERITE
            return t.equalsIgnoreCase(armorType);
        }

        return false;
    }

    private boolean isHostileMob(String mobType) {
        if (mobType == null) return false;
        String m = mobType.toUpperCase(Locale.ROOT);
        return m.contains("ZOMBIE") || m.contains("SKELETON") || m.contains("SPIDER") || m.contains("CREEPER")
                || m.contains("SLIME") || m.contains("ENDERMAN") || m.contains("WITCH") || m.contains("PILLAGER")
                || m.contains("VINDICATOR") || m.contains("EVOKER") || m.contains("RAVAGER") || m.contains("GUARDIAN")
                || m.contains("BLAZE") || m.contains("GHAST") || m.contains("MAGMA_CUBE") || m.contains("PIGLIN_BRUTE")
                || m.contains("WITHER") || m.contains("WARDEN") || m.contains("SHULKER") || m.contains("DROWNED")
                || m.contains("HUSK") || m.contains("STRAY") || m.contains("PHANTOM");
    }

    private void sendActionBarProgress(Player player, Quest quest, int progress) {
        String msg = "§6[Zadanie] " + quest.getTitle() + " §7- §e" + progress + "§7/§e" + quest.getRequiredAmount();
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    public void completeQuest(Player player, PlayerProgressionData data, Quest quest) {
        if (player == null || data == null || quest == null) return;
        if (data.isQuestCompleted(quest.getId())) return;

        data.markQuestCompleted(quest.getId());
        data.setProgress(quest.getId(), quest.getRequiredAmount());

        // Fire event
        QuestCompleteEvent event = new QuestCompleteEvent(player, quest);
        Bukkit.getPluginManager().callEvent(event);

        // Give reward
        rewardService.giveReward(player, quest.getReward());

        // Notify
        int completedReq = stageService.getCompletedRequiredQuestsCount(data, quest.getStage());
        int totalReq = stageService.getRequiredQuestsCount(quest.getStage());

        player.sendMessage("§a§l✔ [UKOŃCZONO ZADANIE] §e" + quest.getTitle() + " §8(§b" + completedReq + "§7/§b" + totalReq + " wymaganych§8)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);

        // Check if all required quests for the stage are completed
        if (stageService.canAdvanceStage(data)) {
            stageService.advanceStage(player, data);
        }
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

    public IProgressionStorage getStorage() {
        return storage;
    }
}
