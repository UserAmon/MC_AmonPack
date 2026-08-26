package RPG.Progression.command;

import RPG.Progression.ProgressionManager;
import RPG.Progression.model.Quest;
import RPG.Progression.model.StageType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public class ProgressionTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> sub = new ArrayList<>(Arrays.asList("gui", "status", "quests", "stage", "biomes"));
            if (sender.hasPermission("amonpack.progression.admin")) {
                sub.add("admin");
            }
            return StringUtil.copyPartialMatches(args[0], sub, completions);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("amonpack.progression.admin")) {
            List<String> adminSubs = Arrays.asList("setstage", "complete", "reset", "reload", "help");
            return StringUtil.copyPartialMatches(args[1], adminSubs, completions);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("amonpack.progression.admin")) {
            if (args[1].equalsIgnoreCase("setstage") || args[1].equalsIgnoreCase("complete") || args[1].equalsIgnoreCase("reset")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                return StringUtil.copyPartialMatches(args[2], players, completions);
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("amonpack.progression.admin")) {
            if (args[1].equalsIgnoreCase("setstage")) {
                List<String> stages = new ArrayList<>();
                for (StageType st : StageType.values()) {
                    stages.add(st.name());
                }
                return StringUtil.copyPartialMatches(args[3], stages, completions);
            }
            if (args[1].equalsIgnoreCase("complete")) {
                List<String> questIds = new ArrayList<>();
                if (ProgressionManager.getInstance() != null) {
                    for (Quest q : ProgressionManager.getInstance().getQuestRegistry().getAllQuests()) {
                        questIds.add(q.getId());
                    }
                }
                return StringUtil.copyPartialMatches(args[3], questIds, completions);
            }
        }

        return completions;
    }
}
