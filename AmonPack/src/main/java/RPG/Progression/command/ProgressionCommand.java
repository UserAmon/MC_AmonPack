package RPG.Progression.command;

import RPG.Progression.ProgressionManager;
import RPG.Progression.gui.ExplorationBiomesGui;
import RPG.Progression.gui.ProgressionMenuGui;
import RPG.Progression.gui.StageDetailGui;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.Quest;
import RPG.Progression.model.StageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class ProgressionCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ProgressionManager manager = ProgressionManager.getInstance();
        if (manager == null) {
            sender.sendMessage(ChatColor.RED + "System Slow Progression jest obecnie wyłączony.");
            return true;
        }

        // Admin commands
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("amonpack.progression.admin")) {
                sender.sendMessage(ChatColor.RED + "Brak uprawnień do komend administracyjnych progresji.");
                return true;
            }

            if (args.length == 1 || args[1].equalsIgnoreCase("help")) {
                sender.sendMessage("§6§l=== Slow Progression - Pomoc Admina ===");
                sender.sendMessage("§e/progression admin setstage <gracz> <etap> §7- Ustawia etap gracza");
                sender.sendMessage("§e/progression admin complete <gracz> <questId> §7- Zalicza zadanie graczowi");
                sender.sendMessage("§e/progression admin reset <gracz> §7- Resetuje progresję gracza");
                sender.sendMessage("§e/progression admin reload §7- Przeładowuje konfigurację progresji");
                return true;
            }

            if (args[1].equalsIgnoreCase("reload")) {
                manager.reload();
                sender.sendMessage(ChatColor.GREEN + "[SlowProgression] Pomyślnie przeładowano konfigurację slow_progression.yml!");
                return true;
            }

            if (args[1].equalsIgnoreCase("setstage")) {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Użycie: /progression admin setstage <gracz> <WOODEN|STONE|IRON|DIAMOND|NETHER|END>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Gracz " + args[2] + " nie jest online.");
                    return true;
                }
                StageType targetStage = StageType.fromName(args[3]);
                PlayerProgressionData data = manager.getProgressionService().getPlayerData(target);
                if (data != null) {
                    data.setCurrentStage(targetStage);
                    sender.sendMessage(ChatColor.GREEN + "Pomyślnie zmieniono etap gracza " + target.getName() + " na " + targetStage.getDisplayName());
                    target.sendMessage(ChatColor.GOLD + "[Progresja] Administrator ustawił Twój etap na: " + targetStage.getDisplayName());
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("complete")) {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Użycie: /progression admin complete <gracz> <questId>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Gracz " + args[2] + " nie jest online.");
                    return true;
                }
                String qId = args[3].toLowerCase(Locale.ROOT);
                Quest quest = manager.getQuestRegistry().getQuest(qId);
                if (quest == null) {
                    sender.sendMessage(ChatColor.RED + "Nie odnaleziono zadania o ID: " + qId);
                    return true;
                }
                PlayerProgressionData data = manager.getProgressionService().getPlayerData(target);
                if (data != null) {
                    manager.getProgressionService().completeQuest(target, data, quest);
                    sender.sendMessage(ChatColor.GREEN + "Zaliczono zadanie " + quest.getTitle() + " dla gracza " + target.getName());
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("reset")) {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Użycie: /progression admin reset <gracz>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Gracz " + args[2] + " nie jest online.");
                    return true;
                }
                PlayerProgressionData data = manager.getProgressionService().getPlayerData(target);
                if (data != null) {
                    data.resetProgress();
                    sender.sendMessage(ChatColor.YELLOW + "Zresetowano całą progresję dla gracza " + target.getName());
                    target.sendMessage(ChatColor.RED + "[Progresja] Twoja progresja RPG została zresetowana.");
                }
                return true;
            }
        }

        // Player commands (requires player)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda wymaga bycia graczem.");
            return true;
        }

        PlayerProgressionData data = manager.getProgressionService().getPlayerData(player);
        if (data == null) {
            player.sendMessage(ChatColor.RED + "Nie udało się załadować Twoich danych progresji.");
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("biomes") || args[0].equalsIgnoreCase("atlas")) {
                new ExplorationBiomesGui(player, data).open();
                return true;
            }
            if (args[0].equalsIgnoreCase("stage") || args[0].equalsIgnoreCase("quests")) {
                new StageDetailGui(player, data, data.getCurrentStage()).open();
                return true;
            }
            if (args[0].equalsIgnoreCase("status")) {
                StageType current = data.getCurrentStage();
                int completedReq = manager.getStageService().getCompletedRequiredQuestsCount(data, current);
                int totalReq = manager.getStageService().getRequiredQuestsCount(current);
                player.sendMessage("§6§l=== Twoja Progresja RPG ===");
                player.sendMessage("§7Aktualny Etap: " + current.getDisplayName());
                player.sendMessage("§7Wymagane zadania: §e" + completedReq + "§7/§e" + totalReq);
                player.sendMessage("§7Odkrytych biomów: §a" + data.getDiscoveredBiomes().size());
                return true;
            }
        }

        // Default: Open Main Progression Menu GUI
        new ProgressionMenuGui(player, data).open();
        return true;
    }
}
