package AvatarSystems.Gathering;

import AvatarSystems.Gathering.Objects.Farm;
import AvatarSystems.Util_Objects.LevelSkill;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Objects;

public class CombatMenager {
    private static HashMap<EntityType, Integer> ExpList = new HashMap<>();

    public CombatMenager() {
        ReloadConfig();
    }

    public void ReloadConfig() {
        FileConfiguration Config = AmonPackPlugin.getConfigs_menager().getCombat_Config();
        for (String key : Objects.requireNonNull(Config.getConfigurationSection("Combat")).getKeys(false)) {
            String World = Config.getString("Combat." + key + ".World");
            if (Config.getConfigurationSection("Combat." + key + ".Exp") != null) {
                for (String MobType : Config.getConfigurationSection("Combat." + key + ".Exp").getKeys(false)) {
                    ExpList.put(EntityType.valueOf(MobType), Config.getInt("Combat." + key + ".Exp." + MobType));
                }
            }
        }
    }

    public static void ExecuteKill(Player player, Entity victim, int expModifier) {
        int ExpAmount = 1;
        if (ExpList.containsKey(victim.getType())) {
            ExpAmount = ExpList.get(victim.getType());
        }
        AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.COMBAT, player, (ExpAmount + expModifier));

    }
}
