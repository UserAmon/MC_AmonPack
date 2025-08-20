package AvatarSystems.Perks;

import AvatarSystems.Perks.Objects.Perk;
import AvatarSystems.Perks.Objects.PerkCondition;
import AvatarSystems.Util_Objects.LevelSkill;
import methods_plugins.AmonPackPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PerksMenager {
    static List<Perk> ListOfPerks;

    public PerksMenager() {
        ReloadConfig();
    }

    public void ReloadConfig(){
        ListOfPerks=new ArrayList<>();
        FileConfiguration Config = AmonPackPlugin.getPerksConfig();
        for(String PerkName : Objects.requireNonNull(Config.getConfigurationSection("Perks.Passive")).getKeys(false)) {
            List<PerkCondition> Conditions = new ArrayList<>();
            for(String ConditionName : Objects.requireNonNull(Config.getConfigurationSection("Perks.Passive."+PerkName)).getKeys(false)) {
                if(Config.getString("Perks.Passive." + PerkName+"."+ConditionName + ".Skill_Type")!=null){
                    LevelSkill.SkillType skillType = LevelSkill.SkillType.valueOf(Config.getString("Perks.Passive." + PerkName+"."+ConditionName + ".Skill_Type"));
                    int RequiredLevel = Config.getInt("Perks.Passive." + PerkName+"."+ConditionName + ".Skill_Level");
                    Conditions.add(new PerkCondition(RequiredLevel,skillType));
                }else{
                    List<String> Abilities = Config.getStringList("Perks.Passive." + PerkName+"."+ConditionName + ".Abilities");
                    Conditions.add(new PerkCondition(Abilities));
                }
            }
            ListOfPerks.add(new Perk(Conditions,PerkName));
        }
    }

    public static boolean PlayerConditionPerk(Player player, String PerkName, Perk.PerkType type){
        if(type== Perk.PerkType.PASSIVE){
        try {
            Perk perk = ListOfPerks.stream().filter(perk1 -> perk1.getName().equalsIgnoreCase(PerkName)).findFirst().orElse(null);
            if(perk!=null){
                return perk.CheckConditions(player);
            }
        } catch (Exception e) {
            System.out.println("Error w perksmenager "+e);
        }
        }
        return false;
    }

}
