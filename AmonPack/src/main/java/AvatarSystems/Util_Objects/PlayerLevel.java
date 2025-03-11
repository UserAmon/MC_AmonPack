package AvatarSystems.Util_Objects;

import org.bukkit.entity.Player;

import java.util.List;

public class PlayerLevel {
    private String PlayerName;
    List<LevelSkill> PlayerSkills;

    public PlayerLevel(String playerName, List<LevelSkill> playerSkills) {
        PlayerName = playerName;
        PlayerSkills = playerSkills;
    }

    public String getPlayerName() {
        return PlayerName;
    }
    public LevelSkill GetSkillByType(LevelSkill.SkillType type){
        return  PlayerSkills.stream().filter(sk -> sk.getType()==type).findFirst().get();
    }
    public List<LevelSkill> getPlayerSkills() {
        return PlayerSkills;
    }
}
