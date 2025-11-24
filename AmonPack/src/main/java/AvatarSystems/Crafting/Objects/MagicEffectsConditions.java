package AvatarSystems.Crafting.Objects;

import AvatarSystems.Util_Objects.LevelSkill;
import org.bukkit.inventory.ItemStack;

public class MagicEffectsConditions {
    int RequiredSkillLevel;
    LevelSkill.SkillType Type;
    ItemStack itemprice;
    boolean IsSkillRequired;

    public MagicEffectsConditions(int requiredSkillLevel, LevelSkill.SkillType type) {
        RequiredSkillLevel = requiredSkillLevel;
        Type = type;
        IsSkillRequired=true;
    }

    public MagicEffectsConditions(ItemStack itemprice) {
        this.itemprice = itemprice;
        IsSkillRequired=false;
    }

    public boolean isSkillRequired() {
        return IsSkillRequired;
    }

    public ItemStack getItemprice() {
        return itemprice;
    }

    public int getRequiredSkillLevel() {
        return RequiredSkillLevel;
    }

    public LevelSkill.SkillType getType() {
        return Type;
    }
}
