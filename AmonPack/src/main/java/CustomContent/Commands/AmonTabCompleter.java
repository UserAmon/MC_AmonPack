package CustomContent.Commands;

import CustomContent.Blocks.CustomBlockManager;
import CustomContent.Bosses.BossManager;
import CustomContent.Items.CustomItemManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AmonTabCompleter implements TabCompleter {

    private final CustomItemManager itemManager;
    private final CustomBlockManager blockManager;
    private final BossManager bossManager;

    public AmonTabCompleter(CustomItemManager itemManager, CustomBlockManager blockManager, BossManager bossManager) {
        this.itemManager = itemManager;
        this.blockManager = blockManager;
        this.bossManager = bossManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            return filter(Arrays.asList("item", "block", "boss", "pack", "debug", "reload"), args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("item")) return filter(Arrays.asList("give", "list"), args[1]);
            if (sub.equals("block")) return filter(Arrays.asList("give", "list"), args[1]);
            if (sub.equals("boss")) return filter(Arrays.asList("spawn", "killall", "list"), args[1]);
            if (sub.equals("pack")) return filter(Arrays.asList("build", "apply", "url"), args[1]);
            if (sub.equals("debug")) return filter(Arrays.asList("tool"), args[1]);
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();

            if (action.equals("give") || (sub.equals("pack") && action.equals("apply"))) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());
                return filter(players, args[2]);
            }

            if (sub.equals("boss") && action.equals("spawn")) {
                return filter(new ArrayList<>(bossManager.getAllBosses().keySet()), args[2]);
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();

            if (sub.equals("item") && action.equals("give")) {
                return filter(new ArrayList<>(itemManager.getAllItems().keySet()), args[3]);
            }
            if (sub.equals("block") && action.equals("give")) {
                return filter(new ArrayList<>(blockManager.getAllCustomBlocks().keySet()), args[3]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> src, String query) {
        List<String> res = new ArrayList<>();
        for (String s : src) {
            if (s.toLowerCase().startsWith(query.toLowerCase())) res.add(s);
        }
        return res;
    }
}
