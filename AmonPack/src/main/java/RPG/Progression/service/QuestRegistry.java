package RPG.Progression.service;

import RPG.Levels.Objects.LevelSkill;
import RPG.Progression.model.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class QuestRegistry {

    private final Map<String, Quest> questsById = new LinkedHashMap<>();
    private final Map<StageType, List<Quest>> questsByStage = new EnumMap<>(StageType.class);
    private final Map<StageType, Map<QuestCategory, List<Quest>>> questsByStageAndCategory = new EnumMap<>(StageType.class);
    private final Map<ObjectiveType, List<Quest>> questsByObjectiveType = new EnumMap<>(ObjectiveType.class);

    public QuestRegistry() {
        for (StageType stage : StageType.values()) {
            questsByStage.put(stage, new ArrayList<>());
            questsByStageAndCategory.put(stage, new EnumMap<>(QuestCategory.class));
            for (QuestCategory cat : QuestCategory.values()) {
                questsByStageAndCategory.get(stage).put(cat, new ArrayList<>());
            }
        }
        for (ObjectiveType type : ObjectiveType.values()) {
            questsByObjectiveType.put(type, new ArrayList<>());
        }
    }

    public void load(FileConfiguration config) {
        questsById.clear();
        for (StageType stage : StageType.values()) {
            questsByStage.get(stage).clear();
            for (QuestCategory cat : QuestCategory.values()) {
                questsByStageAndCategory.get(stage).get(cat).clear();
            }
        }
        for (ObjectiveType type : ObjectiveType.values()) {
            questsByObjectiveType.get(type).clear();
        }

        if (config == null) return;

        ConfigurationSection stagesSec = config.getConfigurationSection("stages");
        if (stagesSec == null) {
            Bukkit.getLogger().warning("[SlowProgression] Brak sekcji 'stages' w konfiguracji.");
            return;
        }

        for (String stageKey : stagesSec.getKeys(false)) {
            StageType stage = StageType.fromName(stageKey);
            ConfigurationSection stageSec = stagesSec.getConfigurationSection(stageKey);
            if (stageSec == null) continue;

            ConfigurationSection questsSec = stageSec.getConfigurationSection("quests");
            if (questsSec == null) continue;

            for (String questId : questsSec.getKeys(false)) {
                String qPath = questId + ".";
                ConfigurationSection qSec = questsSec.getConfigurationSection(questId);
                if (qSec == null) continue;

                String title = qSec.getString("title", questId);
                String desc = qSec.getString("description", "");
                String catStr = qSec.getString("category", "CORE");
                QuestCategory category = QuestCategory.fromName(catStr);

                String objStr = qSec.getString("objective_type", "CUSTOM_OBJECTIVE");
                ObjectiveType objectiveType = ObjectiveType.fromKey(objStr);

                String target = qSec.getString("target", "");
                int amount = qSec.getInt("amount", 1);
                boolean required = qSec.getBoolean("required", true);

                String iconMatStr = qSec.getString("icon", "PAPER");
                Material icon = Material.matchMaterial(iconMatStr);
                if (icon == null) icon = Material.PAPER;
                int cmd = qSec.getInt("custom_model_data", 0);

                // Rewards
                QuestReward reward = new QuestReward();
                ConfigurationSection rewSec = qSec.getConfigurationSection("reward");
                if (rewSec != null) {
                    reward.setMoney(rewSec.getDouble("money", 0.0));
                    reward.setPlayerExp(rewSec.getInt("exp", 0));

                    ConfigurationSection itemsSec = rewSec.getConfigurationSection("items");
                    if (itemsSec != null) {
                        for (String itemKey : itemsSec.getKeys(false)) {
                            reward.addItem(itemKey, itemsSec.getInt(itemKey, 1));
                        }
                    }

                    ConfigurationSection skillsSec = rewSec.getConfigurationSection("skills_exp");
                    if (skillsSec != null) {
                        for (String skillKey : skillsSec.getKeys(false)) {
                            try {
                                LevelSkill.SkillType st = LevelSkill.SkillType.valueOf(skillKey.toUpperCase(Locale.ROOT));
                                reward.addSkillExp(st, skillsSec.getDouble(skillKey, 0.0));
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    List<String> commands = rewSec.getStringList("commands");
                    if (commands != null) {
                        for (String cmdStr : commands) {
                            reward.addCommand(cmdStr);
                        }
                    }

                    reward.setBroadcastMessage(rewSec.getString("broadcast", null));
                }

                Quest quest = new Quest(questId.toLowerCase(Locale.ROOT), stage, category, title, desc,
                        objectiveType, target, amount, required, reward, icon, cmd);

                registerQuest(quest);
            }
        }

        Bukkit.getLogger().info("[SlowProgression] Pomyślnie załadowano " + questsById.size() + " zadań progresji.");
    }

    public void registerQuest(Quest quest) {
        if (quest == null) return;
        questsById.put(quest.getId().toLowerCase(Locale.ROOT), quest);
        questsByStage.get(quest.getStage()).add(quest);
        questsByStageAndCategory.get(quest.getStage()).get(quest.getCategory()).add(quest);
        questsByObjectiveType.get(quest.getObjectiveType()).add(quest);
    }

    public Quest getQuest(String id) {
        if (id == null) return null;
        return questsById.get(id.toLowerCase(Locale.ROOT));
    }

    public List<Quest> getQuestsForStage(StageType stage) {
        return Collections.unmodifiableList(questsByStage.getOrDefault(stage, Collections.emptyList()));
    }

    public List<Quest> getQuestsForStageAndCategory(StageType stage, QuestCategory category) {
        Map<QuestCategory, List<Quest>> catMap = questsByStageAndCategory.get(stage);
        if (catMap != null) {
            return Collections.unmodifiableList(catMap.getOrDefault(category, Collections.emptyList()));
        }
        return Collections.emptyList();
    }

    public List<Quest> getQuestsByObjective(ObjectiveType type) {
        return Collections.unmodifiableList(questsByObjectiveType.getOrDefault(type, Collections.emptyList()));
    }

    public Collection<Quest> getAllQuests() {
        return Collections.unmodifiableCollection(questsById.values());
    }
}
