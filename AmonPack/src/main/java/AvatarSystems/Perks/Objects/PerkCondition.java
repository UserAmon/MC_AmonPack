package AvatarSystems.Perks.Objects;

import AvatarSystems.Util_Objects.LevelSkill;

import java.util.List;

public class PerkCondition {
    private List<String> RequiredAbilities;
    private LevelSkill.SkillType LevelTypeCondition;
    private int LevelCondition;
    private boolean IsSkill;

    public PerkCondition(int levelCondition, LevelSkill.SkillType levelTypeCondition) {
        LevelCondition = levelCondition;
        LevelTypeCondition = levelTypeCondition;
        IsSkill=true;
    }

    public PerkCondition(List<String> requiredAbilities) {
        RequiredAbilities = requiredAbilities;
        IsSkill=false;
    }

    public int getLevelCondition() {
        return LevelCondition;
    }

    public LevelSkill.SkillType getLevelTypeCondition() {
        return LevelTypeCondition;
    }

    public List<String> getRequiredAbilities() {
        return RequiredAbilities;
    }

    public boolean isSkill() {
        return IsSkill;
    }
}
