package RPG.Progression.model;

import RPG.Levels.Objects.LevelSkill;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class QuestReward {

    private double money = 0.0;
    private int playerExp = 0;
    private final Map<String, Integer> items = new LinkedHashMap<>();
    private final Map<LevelSkill.SkillType, Double> skillExp = new LinkedHashMap<>();
    private final List<String> commands = new ArrayList<>();
    private String broadcastMessage = null;

    public QuestReward() {
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public int getPlayerExp() {
        return playerExp;
    }

    public void setPlayerExp(int playerExp) {
        this.playerExp = playerExp;
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    public void addItem(String itemKey, int amount) {
        items.put(itemKey, amount);
    }

    public Map<LevelSkill.SkillType, Double> getSkillExp() {
        return skillExp;
    }

    public void addSkillExp(LevelSkill.SkillType type, double amount) {
        skillExp.put(type, amount);
    }

    public List<String> getCommands() {
        return commands;
    }

    public void addCommand(String command) {
        commands.add(command);
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    public boolean isEmpty() {
        return money <= 0 && playerExp <= 0 && items.isEmpty() && skillExp.isEmpty() && commands.isEmpty();
    }
}
