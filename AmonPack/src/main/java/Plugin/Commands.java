package Plugin;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class Commands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("reload")) {
            if (sender.isOp() || sender.hasPermission("amonpack.reload")) {
                AmonPackPlugin.reloadAllConfigs();
                sender.sendMessage("§a[AmonPack] Konfiguracja przeładowana pomyślnie!");
            } else {
                sender.sendMessage("§c[AmonPack] Brak uprawnień do przeładowania konfiguracji!");
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return new ArrayList<>();
    }
}
