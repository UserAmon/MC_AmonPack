package RPG.Progression.listener;

import CustomContent.Bosses.CustomBoss;
import RPG.Progression.event.CustomBossDefeatEvent;
import RPG.Progression.event.DungeonCompleteEvent;
import RPG.Progression.event.PlayerSkillLevelUpEvent;
import RPG.Progression.model.ObjectiveType;
import RPG.Progression.service.ProgressionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

public class ProgressionIntegrationListener implements Listener {

    private final ProgressionService progressionService;

    public ProgressionIntegrationListener(ProgressionService progressionService) {
        this.progressionService = progressionService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCustomBossDefeat(CustomBossDefeatEvent event) {
        CustomBoss boss = event.getBossTemplate();
        if (boss == null) return;

        Set<Player> creditedPlayers = new HashSet<>(event.getParticipants());
        if (event.getKiller() != null) {
            creditedPlayers.add(event.getKiller());
        }

        String bossId = boss.getId();
        for (Player player : creditedPlayers) {
            if (player != null && player.isOnline()) {
                progressionService.handleObjective(player, ObjectiveType.DEFEAT_BOSS, bossId, 1);
                progressionService.handleObjective(player, ObjectiveType.DEFEAT_BOSS, "CUSTOM_BOSS", 1);
                progressionService.handleObjective(player, ObjectiveType.DEFEAT_BOSS, boss.getDisplayName(), 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDungeonComplete(DungeonCompleteEvent event) {
        String dungeonName = event.getDungeonTemplate() != null ? event.getDungeonTemplate().getName() : "DUNGEON";

        for (Player player : event.getPlayers()) {
            if (player != null && player.isOnline()) {
                progressionService.handleObjective(player, ObjectiveType.COMPLETE_DUNGEON, dungeonName, 1);
                progressionService.handleObjective(player, ObjectiveType.COMPLETE_DUNGEON, "DUNGEON", 1);
                progressionService.handleObjective(player, ObjectiveType.COMPLETE_DUNGEON, "ANY", 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSkillLevelUp(PlayerSkillLevelUpEvent event) {
        Player player = event.getPlayer();
        if (player != null && player.isOnline()) {
            String skillKey = event.getSkillType().name();
            int newLvl = event.getNewLevel();
            progressionService.handleObjective(player, ObjectiveType.REACH_SKILL_LEVEL, skillKey, newLvl);
        }
    }
}
