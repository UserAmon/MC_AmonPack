package Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("amonpack.reload") && !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                return true;
            }
            AmonPackPlugin.plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Pomyślnie przeładowano konfigurację AmonPack!");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return new ArrayList<>();
    }
}
