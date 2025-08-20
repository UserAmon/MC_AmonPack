package AvatarSystems.Perks.Objects;

import AvatarSystems.Levels.PlayerBendingBranch;
import AvatarSystems.Levels.PlayerLevelMenager;
import AvatarSystems.Util_Objects.LevelSkill;
import methods_plugins.AmonPackPlugin;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Perk {
    String Name;
    PerkType type;
    List<PerkCondition> Conditions = new ArrayList<>();
    public enum PerkType{
        ABILITY,
        PASSIVE,
        ITEM
    }

    public Perk(List<PerkCondition> conditions, String name) {
        Conditions = conditions;
        Name = name;
        type=PerkType.PASSIVE;
    }

    public boolean CheckConditions(Player player){
        for (PerkCondition condition : Conditions){
            if(condition.isSkill()){
            if(PlayerLevelMenager.GetSkillByPlayer(condition.getLevelTypeCondition(),player)<condition.getLevelCondition()){
                return false;
            }
            }else{
                PlayerBendingBranch playersBranch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
                if(!new HashSet<>(playersBranch.getUnlockedAbilities()).containsAll(condition.getRequiredAbilities())){
                    return false;
                }
            }
        }
        return true;
    }

    public String getName() {
        return Name;
    }
}

