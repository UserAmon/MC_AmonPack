package RPG.Progression.model;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Quest {

    private final String id;
    private final StageType stage;
    private final QuestCategory category;
    private final String title;
    private final String description;
    private final ObjectiveType objectiveType;
    private final String target;
    private final int requiredAmount;
    private final boolean required;
    private final QuestReward reward;
    private final Material icon;
    private final int customModelData;
    private final List<String> additionalDetails = new ArrayList<>();
    private final List<String> prerequisites = new ArrayList<>();

    public Quest(String id, StageType stage, QuestCategory category, String title, String description,
                 ObjectiveType objectiveType, String target, int requiredAmount, boolean required,
                 QuestReward reward, Material icon, int customModelData) {
        this.id = id;
        this.stage = stage;
        this.category = category;
        this.title = title;
        this.description = description;
        this.objectiveType = objectiveType;
        this.target = target;
        this.requiredAmount = Math.max(1, requiredAmount);
        this.required = required;
        this.reward = reward != null ? reward : new QuestReward();
        this.icon = icon != null ? icon : Material.PAPER;
        this.customModelData = customModelData;
    }

    public List<String> getPrerequisites() {
        return Collections.unmodifiableList(prerequisites);
    }

    public void addPrerequisite(String questId) {
        if (questId != null && !questId.trim().isEmpty()) {
            prerequisites.add(questId.trim().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public String getId() {
        return id;
    }

    public StageType getStage() {
        return stage;
    }

    public QuestCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return ChatColor.translateAlternateColorCodes('&', title);
    }

    public String getDescription() {
        return ChatColor.translateAlternateColorCodes('&', description);
    }

    public ObjectiveType getObjectiveType() {
        return objectiveType;
    }

    public String getTarget() {
        return target;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public boolean isRequired() {
        return required;
    }

    public QuestReward getReward() {
        return reward;
    }

    public Material getIcon() {
        return icon;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public List<String> getAdditionalDetails() {
        return additionalDetails;
    }
}
