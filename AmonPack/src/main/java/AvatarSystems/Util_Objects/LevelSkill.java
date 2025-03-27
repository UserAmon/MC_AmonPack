package AvatarSystems.Util_Objects;

import java.util.ArrayList;
import java.util.List;

public class LevelSkill {
    private double ExpPoints;
    private SkillType Type;
    private List<Integer> UsedRewards = new ArrayList<>();
    private double UpgradePercent;
    public enum SkillType{
        MINING,
        FARMING,
        MAGIC,
        COMBAT,
        GENERAL
    }

    public LevelSkill(double expPoints, SkillType type, List<Integer> usedRewards, double upgradePercent) {
        ExpPoints = expPoints;
        Type = type;
        UpgradePercent = upgradePercent;
        UsedRewards = usedRewards;
    }

    public double getExpPoints() {
        return ExpPoints;
    }
    public void setExpPoints(double expPoints) {
        ExpPoints = expPoints;
    }
    public SkillType getType() {
        return Type;
    }
    public List<Integer> getUsedRewards() {
        return UsedRewards;
    }
    public void setUsedRewards(List<Integer> usedRewards) {
        UsedRewards = usedRewards;
    }
    public double getUpgradePercent() {
        return UpgradePercent;
    }
    public void setUpgradePercent(double upgradePercent) {
        UpgradePercent = upgradePercent;
    }
}
