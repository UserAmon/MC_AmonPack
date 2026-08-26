package RPG.Progression.service;

import RPG.Progression.event.StageUnlockEvent;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.Quest;
import RPG.Progression.model.StageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class StageService {

    private final QuestRegistry questRegistry;
    private final Map<StageType, List<String>> stageExplorationBiomes = new EnumMap<>(StageType.class);
    private final Map<StageType, String> stageUnlockMessages = new EnumMap<>(StageType.class);

    public StageService(QuestRegistry questRegistry) {
        this.questRegistry = questRegistry;
        for (StageType stage : StageType.values()) {
            stageExplorationBiomes.put(stage, new ArrayList<>());
        }
    }

    public void load(FileConfiguration config) {
        for (StageType stage : StageType.values()) {
            stageExplorationBiomes.get(stage).clear();
            stageUnlockMessages.remove(stage);
        }

        if (config == null) return;
        ConfigurationSection stagesSec = config.getConfigurationSection("stages");
        if (stagesSec == null) return;

        for (String stageKey : stagesSec.getKeys(false)) {
            StageType stage = StageType.fromName(stageKey);
            ConfigurationSection stageSec = stagesSec.getConfigurationSection(stageKey);
            if (stageSec == null) continue;

            List<String> biomes = stageSec.getStringList("exploration_biomes");
            if (biomes != null) {
                stageExplorationBiomes.get(stage).addAll(biomes);
            }

            String unlockMsg = stageSec.getString("completion.message");
            if (unlockMsg != null) {
                stageUnlockMessages.put(stage, unlockMsg);
            }
        }
    }

    public List<String> getStageExplorationBiomes(StageType stage) {
        return Collections.unmodifiableList(stageExplorationBiomes.getOrDefault(stage, Collections.emptyList()));
    }

    public boolean canAdvanceStage(PlayerProgressionData data) {
        if (data == null) return false;
        StageType current = data.getCurrentStage();
        StageType next = current.getNext();
        if (next == null) return false;

        List<Quest> stageQuests = questRegistry.getQuestsForStage(current);
        for (Quest q : stageQuests) {
            if (q.isRequired() && !data.isQuestCompleted(q.getId())) {
                return false;
            }
        }
        return true;
    }

    public int getRequiredQuestsCount(StageType stage) {
        int count = 0;
        for (Quest q : questRegistry.getQuestsForStage(stage)) {
            if (q.isRequired()) count++;
        }
        return count;
    }

    public int getCompletedRequiredQuestsCount(PlayerProgressionData data, StageType stage) {
        if (data == null) return 0;
        int count = 0;
        for (Quest q : questRegistry.getQuestsForStage(stage)) {
            if (q.isRequired() && data.isQuestCompleted(q.getId())) {
                count++;
            }
        }
        return count;
    }

    public boolean advanceStage(Player player, PlayerProgressionData data) {
        if (player == null || data == null) return false;
        StageType current = data.getCurrentStage();
        StageType next = current.getNext();
        if (next == null) return false;

        data.setCurrentStage(next);

        // Fire event
        StageUnlockEvent event = new StageUnlockEvent(player, current, next);
        Bukkit.getPluginManager().callEvent(event);

        // Visual and audio fanfare
        player.sendTitle(
                next.getColor() + "✦ NOWY ETAP ✦",
                ChatColor.YELLOW + "Odblokowano: " + next.getDisplayName(),
                10, 70, 20
        );

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.2f);

        String customMsg = stageUnlockMessages.get(current);
        if (customMsg != null && !customMsg.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', customMsg));
        }

        Bukkit.broadcastMessage("§6§l[AmonPack] §eGracz §a" + player.getName() + " §eukończył " + current.getDisplayName() + " §ei wkroczył w " + next.getDisplayName() + "§e!");

        return true;
    }
}
