package Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import RPG.Levels.BendingTree.PlayerBendingBranch;
import RPG.Levels.Objects.LevelSkill;
import RPG.Levels.PlayerLevelMenager;
//import RPG.UnUsed.Menagerie.MMORPG.GuiMenu;
import RPG.UnUsed.Menagerie.MenagerieMenager;
import RPG.Bounties.BountiesMenager;
import com.projectkorra.projectkorra.Element;
import Plugin.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class Commands implements CommandExecutor, TabCompleter {

    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        String cmdName = cmd.getName().toLowerCase();
        if (!AmonPackPlugin.ENABLE_SKILL_TREE && (cmdName.equals("selectelement") || cmdName.equals("level"))) {
            sender.sendMessage(ChatColor.RED
                    + "[AmonPack] Ta funkcja (Drzewko Skilli / Poziomy) jest wyłączona w tym buildzie pluginu!");
            return true;
        }
        if (!AmonPackPlugin.ENABLE_DUNGEONS
                && (cmdName.equals("dungeons") || cmdName.equals("dungbuild") || cmdName.equals("menagerie"))) {
            sender.sendMessage(ChatColor.RED
                    + "[AmonPack] Ta funkcja (Dungeony / Menagerie) jest wyłączona w tym buildzie pluginu!");
            return true;
        }
        if (!AmonPackPlugin.ENABLE_BOUNTIES && cmdName.equals("bounties")) {
            sender.sendMessage(ChatColor.RED
                    + "[AmonPack] Ta funkcja (System Nagród / Bounties) jest wyłączona w tym buildzie pluginu!");
            return true;
        }
        if (!AmonPackPlugin.ENABLE_PARTY && (cmdName.equals("party") || cmdName.equals("p"))) {
            sender.sendMessage(ChatColor.RED
                    + "[AmonPack] Ta funkcja (System Drużyn / Party) jest wyłączona w tym buildzie pluginu!");
            return true;
        }
        if (!AmonPackPlugin.ENABLE_RPG_GATHERING && cmdName.equals("craft")) {
            sender.sendMessage(
                    ChatColor.RED + "[AmonPack] Ta funkcja (Crafting / RPG) jest wyłączona w tym buildzie pluginu!");
            return true;
        }

        switch (cmdName) {
            case "selectelement":
                if (sender instanceof Player) {
                    PlayerLevelMenager.OpenSelectElementMenu((Player) sender);
                } else {
                    sender.sendMessage("Tylko gracz może użyć tej komendy!");
                }
                return true;

            case "level":
                if (args != null && args.length == 4 && args[0].equalsIgnoreCase("magic")) {
                    try {
                        Player p = Bukkit.getPlayer(args[1]);
                        PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(p.getName());
                        if (branch != null) {
                            if (args[2].equalsIgnoreCase("Set")) {
                                branch.SetCurrentElement(Element.getElement(args[3]));
                            } else {
                                int points = Integer.parseInt(args[2]);
                                if (args[3].equalsIgnoreCase("All")) {
                                    branch.AddAllPoints(points);
                                } else {
                                    Element element = Element.getElement(args[3]);
                                    branch.AddPoints(element, points);
                                }
                            }
                        } else {
                            int points = Integer.parseInt(args[2]);
                            List<Element> elements = new ArrayList<>();
                            List<String> abilities = new ArrayList<>();
                            elements.add(Element.getElement(args[3]));
                            branch = new PlayerBendingBranch(0, Element.getElement(args[3]), 0, elements, 0,
                                    p.getName(), abilities, 0);
                            AmonPackPlugin.levelsBending.AddNewBranch(branch);
                            AmonPackPlugin.levelsBending.GetBranchByPlayerName(p.getName())
                                    .AddPoints(Element.getElement(args[3]), points);
                        }
                    } catch (Exception e) {
                        System.out.println("Error in Level magic command " + e.getMessage());
                    }
                } else if (args.length != 0 && args[0].equalsIgnoreCase("add")) {
                    try {
                        LevelSkill.SkillType type = LevelSkill.SkillType.valueOf(args[1]);
                        Player p = Bukkit.getPlayer(args[2]);
                        AmonPackPlugin.getPlayerMenager().AddPointsToSkill(type, p, Double.parseDouble(args[3]), true);
                    } catch (Exception e) {
                        System.out.println("Error in Level command " + e.getMessage());
                    }
                } else {
                    if (sender instanceof Player) {
                        System.out.println("Otwieram menu levela!");
                        try {
                            PlayerLevelMenager.TryOpenPlayerLevel((Player) sender);
                        } catch (Exception e) {
                            System.out.println("Error!!! " + e.getMessage());
                        }
                    } else {
                        sender.sendMessage("Tylko gracz może użyć tej komendy!");
                    }
                }
                return true;

            case "reload":
                if (sender.hasPermission("amonpack.admin") || sender.isOp() || !(sender instanceof Player)) {
                    if (AmonPackPlugin.plugin != null) {
                        ((AmonPackPlugin) AmonPackPlugin.plugin).reloadPluginConfigurations();
                    }
                    sender.sendMessage(ChatColor.GREEN + "[AmonPack] Zresetowano i odświeżono wszystkie konfiguracje oraz instancje pluginu!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień do tej komendy.");
                }
                return true;

            case "dungeons":
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("reload")) {
                        if (sender.isOp()) {
                            RPG.Dungeons.DungeonManager.getInstance().loadTemplates();
                            sender.sendMessage(ChatColor.GREEN + "[Dungeons] Szablony zostaly przeladowane!");
                        } else {
                            sender.sendMessage(ChatColor.RED + "Brak uprawnien!");
                        }
                    } else if (args[0].equalsIgnoreCase("leave") && sender instanceof Player) {
                        Player player = (Player) sender;
                        RPG.Dungeons.DungeonInstance run = RPG.Dungeons.DungeonManager.getInstance()
                                .getActiveInstance(player);
                        if (run != null) {
                            run.ejectPlayer(player);
                        } else {
                            player.sendMessage(ChatColor.RED + "Nie jestes w zadnym dungeonie!");
                        }
                    } else if (args[0].equalsIgnoreCase("skills") && sender instanceof Player) {
                        Player player = (Player) sender;
                        RPG.Dungeons.DungeonInstance run = RPG.Dungeons.DungeonManager.getInstance()
                                .getActiveInstance(player);
                        if (run != null) {
                            AmonPackPlugin.levelsBending.OpenDungeonSkillMenu(player.getName());
                        } else {
                            player.sendMessage(ChatColor.RED + "Ta komenda dziala tylko w dungeonie!");
                        }
                    } else if (args[0].equalsIgnoreCase("start")) {
                        if (args.length < 2) {
                            sender.sendMessage(ChatColor.RED + "Użycie: /dungeons start <dungeonId>");
                            return true;
                        }
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "Tylko gracz może uruchomić dungeon!");
                            return true;
                        }
                        Player player = (Player) sender;
                        String dungId = args[1];
                        if (RPG.Dungeons.DungeonManager.getInstance().getActiveInstance(player) != null) {
                            player.sendMessage(ChatColor.RED + "Jesteś już w aktywnym dungeonie!");
                            return true;
                        }
                        RPG.Dungeons.DungeonEntryGui.open(player, dungId);
                    } else {
                        sender.sendMessage(
                                ChatColor.RED + "Niepoprawne argumenty! Dostepne: start, leave, skills, reload");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Dostepne podkomendy: start, leave, skills, reload");
                }
                return true;

            case "bounties":
                if (sender instanceof Player) {
                    if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
                        if (sender.isOp()) {
                            BountiesMenager.ForceReset((Player) sender);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Nie masz uprawnień do tej komendy!");
                        }
                    } else {
                        BountiesMenager.OpenBountiesGui((Player) sender);
                    }
                } else {
                    sender.sendMessage("Tylko gracz może użyć tej komendy!");
                }
                return true;

            case "dungbuild":
                if (sender instanceof Player) {
                    RPG.Dungeons.DungBuildManager.openDungBuildGui((Player) sender);
                } else {
                    sender.sendMessage("Tylko gracz może użyć tej komendy!");
                }
                return true;

            case "menagerie":
                List<Player> listofplayers = new ArrayList<>();
                for (String s : args) {
                    try {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getName().equalsIgnoreCase(s)) {
                                listofplayers.add(p);
                                break;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                if (args.length > 0) {
                    MenagerieMenager.StartMenagerie(listofplayers, args[0]);
                }
                return true;

            case "arenabuilding":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (args.length > 0 && args[0].equalsIgnoreCase("On")) {
                        AmonPackPlugin.BuildingOn();
                        player.sendMessage(ChatColor.RED + "Można Budować");
                    } else {
                        AmonPackPlugin.BuildingOff();
                        player.sendMessage(ChatColor.RED + "Nie Można Budować");
                    }
                } else {
                    sender.sendMessage("Tylko gracz może użyć tej komendy!");
                }
                return true;

            case "party":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Tylko gracz może użyć tej komendy!");
                    return true;
                }
                Player pParty = (Player) sender;
                if (args.length == 0) {
                    pParty.sendMessage(ChatColor.GOLD + "========== " + ChatColor.AQUA + "SYSTEM PARTY" + ChatColor.GOLD
                            + " ==========");
                    pParty.sendMessage(ChatColor.YELLOW + "/party invite <gracz> " + ChatColor.GRAY
                            + "- Zaprasza gracza do party");
                    pParty.sendMessage(
                            ChatColor.YELLOW + "/party accept " + ChatColor.GRAY + "- Akceptuje zaproszenie");
                    pParty.sendMessage(ChatColor.YELLOW + "/party leave " + ChatColor.GRAY + "- Opuszcza obecne party");
                    pParty.sendMessage(ChatColor.YELLOW + "/party kick <gracz> " + ChatColor.GRAY
                            + "- Wyrzuca gracza z party (lider)");
                    pParty.sendMessage(
                            ChatColor.YELLOW + "/party pvp " + ChatColor.GRAY + "- Przełącza friendly fire (lider)");
                    pParty.sendMessage(
                            ChatColor.YELLOW + "/party list " + ChatColor.GRAY + "- Pokazuje liste graczy w party");
                    pParty.sendMessage(
                            ChatColor.YELLOW + "/party chat <tekst> " + ChatColor.GRAY + "- Wysyła wiadomość do party");
                    pParty.sendMessage(
                            ChatColor.YELLOW + "/party togglechat " + ChatColor.GRAY + "- Togglowanie pętli czatu");
                    pParty.sendMessage(ChatColor.GOLD + "========================================");
                    return true;
                }

                String sub = args[0].toLowerCase();
                RPG.Party.PartyManager pm = RPG.Party.PartyManager.getInstance();

                if (sub.equals("invite") || sub.equals("zapros")) {
                    if (args.length < 2) {
                        pParty.sendMessage(ChatColor.RED + "Podaj nazwę gracza: /party invite <gracz>");
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null || !target.isOnline()) {
                        pParty.sendMessage(ChatColor.RED + "Ten gracz jest offline.");
                        return true;
                    }
                    pm.invitePlayer(pParty, target);
                } else if (sub.equals("accept") || sub.equals("akceptuj")) {
                    pm.acceptInvite(pParty);
                } else if (sub.equals("leave") || sub.equals("opusc")) {
                    pm.leaveParty(pParty);
                } else if (sub.equals("kick") || sub.equals("wyrzuc")) {
                    if (args.length < 2) {
                        pParty.sendMessage(ChatColor.RED + "Podaj nazwę gracza: /party kick <gracz>");
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        pParty.sendMessage(ChatColor.RED + "Gracz nieznaleziony.");
                        return true;
                    }
                    pm.kickPlayer(pParty, target);
                } else if (sub.equals("pvp") || sub.equals("ff")) {
                    pm.toggleFriendlyFire(pParty);
                } else if (sub.equals("list") || sub.equals("lista")) {
                    RPG.Party.Party partyObj = pm.getParty(pParty.getUniqueId());
                    if (partyObj == null) {
                        pParty.sendMessage(ChatColor.RED + "Nie jesteś w żadnej drużynie!");
                        return true;
                    }
                    pParty.sendMessage(ChatColor.GOLD + "=== Członkowie drużyny (FF: "
                            + (partyObj.isFriendlyFireEnabled() ? "ON" : "OFF") + ") ===");
                    for (UUID uuid : partyObj.getMembers()) {
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        if (name == null)
                            name = uuid.toString();

                        String status = Bukkit.getPlayer(uuid) != null && Bukkit.getPlayer(uuid).isOnline()
                                ? ChatColor.GREEN + "[ONLINE]"
                                : ChatColor.RED + "[OFFLINE]";

                        String suffix = partyObj.getLeaderUUID().equals(uuid) ? ChatColor.GOLD + " (Lider) *" : "";
                        pParty.sendMessage(ChatColor.YELLOW + "- " + name + suffix + " " + status);
                    }
                } else if (sub.equals("chat") || sub.equals("c")) {
                    if (args.length < 2) {
                        pParty.sendMessage(ChatColor.RED + "Użycie: /party chat <wiadomość>");
                        return true;
                    }
                    StringBuilder sbMsg = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        sbMsg.append(args[i]).append(" ");
                    }
                    pm.sendPartyChat(pParty, sbMsg.toString().trim());
                } else if (sub.equals("togglechat") || sub.equals("tc")) {
                    pm.togglePartyChatRouting(pParty);
                } else {
                    pParty.sendMessage(ChatColor.RED + "Nieznane polecenie party. Wpisz /party aby zobaczyć pomoc.");
                }
                return true;

            case "p":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Tylko gracz może użyć tej komendy!");
                    return true;
                }
                Player pChat = (Player) sender;
                if (args.length == 0) {
                    pChat.sendMessage(ChatColor.RED + "Użycie: /p <wiadomość>");
                    return true;
                }
                StringBuilder sbMsg = new StringBuilder();
                for (String arg : args) {
                    sbMsg.append(arg).append(" ");
                }
                RPG.Party.PartyManager.getInstance().sendPartyChat(pChat, sbMsg.toString().trim());
                return true;

            case "questitems":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    for (String key : AmonPackPlugin.getAbilitiesConfig().getConfigurationSection("AmonPack.Items")
                            .getKeys(false)) {
                        if (AmonPackPlugin.getAbilitiesConfig().getString("AmonPack.Items." + key + ".Name") != null) {
                            String type = AmonPackPlugin.getAbilitiesConfig()
                                    .getString("AmonPack.Items." + key + ".Type");
                            String name = "" + AmonPackPlugin.getAbilitiesConfig()
                                    .getString("AmonPack.Items." + key + ".Name").replace("&", "§");
                            List<String> lorelist = new ArrayList<String>();
                            if (AmonPackPlugin.getAbilitiesConfig()
                                    .getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
                                for (String lores : AmonPackPlugin.getAbilitiesConfig()
                                        .getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
                                    String lore = "" + AmonPackPlugin.getAbilitiesConfig()
                                            .getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");
                                    ;
                                    if (lore != null) {
                                        lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
                                    }
                                }
                            }
                            ItemStack QuestItem = new ItemStack(Material.getMaterial(type), 1);
                            ItemMeta QuestItemMeta = QuestItem.getItemMeta();
                            if (AmonPackPlugin.getAbilitiesConfig()
                                    .getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
                                for (String enchname : AmonPackPlugin.getAbilitiesConfig()
                                        .getConfigurationSection("AmonPack.Items." + key + ".Enchantment")
                                        .getKeys(false)) {
                                    int enchpower = AmonPackPlugin.getAbilitiesConfig().getInt(
                                            "AmonPack.Items." + key + ".Enchantment." + enchname + ".EnchantmentLevel");
                                    QuestItemMeta.addEnchant(Enchantment.getByName(enchname), enchpower, true);
                                }
                            }
                            if (name != null) {
                                QuestItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                            }
                            if (lorelist != null) {
                                QuestItemMeta.setLore(lorelist);
                            }
                            QuestItem.setItemMeta(QuestItemMeta);
                            player.getInventory().addItem(QuestItem);
                        }
                    }
                } else {
                    System.out.println("QuestItems");
                }
                return true;



            case "amonpack":
                if (args != null && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("amonpack.admin") || sender.isOp() || !(sender instanceof Player)) {
                        if (AmonPackPlugin.plugin != null) {
                            ((AmonPackPlugin) AmonPackPlugin.plugin).reloadPluginConfigurations();
                        }
                        sender.sendMessage(ChatColor.GREEN + "[AmonPack] Zresetowano i odświeżono wszystkie konfiguracje oraz instancje pluginu!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Brak uprawnień do tej komendy.");
                    }
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "[AmonPack] Użycie: /amonpack reload");
                return true;
        }
        return false;
    }

    public static ItemStack QuestItemConfig(String itemname) {
        ItemStack QuestItem = new ItemStack(Material.DIRT, 1);
        for (String key : AmonPackPlugin.getAbilitiesConfig().getConfigurationSection("AmonPack.Items")
                .getKeys(false)) {
            if (key.equalsIgnoreCase(itemname)) {
                String name = null;
                String type = AmonPackPlugin.getAbilitiesConfig().getString("AmonPack.Items." + key + ".Type");
                if (AmonPackPlugin.getAbilitiesConfig().getString("AmonPack.Items." + key + ".Name") != null) {
                    name = "" + AmonPackPlugin.getAbilitiesConfig().getString("AmonPack.Items." + key + ".Name")
                            .replace("&", "§");
                }
                List<String> lorelist = new ArrayList<String>();
                if (AmonPackPlugin.getAbilitiesConfig()
                        .getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
                    for (String lores : AmonPackPlugin.getAbilitiesConfig()
                            .getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
                        String lore = "" + AmonPackPlugin.getAbilitiesConfig()
                                .getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");
                        ;
                        if (lore != null) {
                            lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
                        }
                    }
                }
                QuestItem = new ItemStack(Material.getMaterial(type), 1);
                ItemMeta QuestItemMeta = QuestItem.getItemMeta();
                if (AmonPackPlugin.getAbilitiesConfig()
                        .getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
                    for (String enchname : AmonPackPlugin.getAbilitiesConfig()
                            .getConfigurationSection("AmonPack.Items." + key + ".Enchantment").getKeys(false)) {
                        int enchpower = AmonPackPlugin.getAbilitiesConfig()
                                .getInt("AmonPack.Items." + key + ".Enchantment." + enchname + ".EnchantmentLevel");
                        QuestItemMeta.addEnchant(Enchantment.getByName(enchname), enchpower, true);
                    }
                }
                if (name != null) {
                    QuestItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                }
                if (lorelist != null) {
                    QuestItemMeta.setLore(lorelist);
                }
                QuestItem.setItemMeta(QuestItemMeta);
            }
        }
        for (String key : AmonPackPlugin.getAbilitiesConfig().getConfigurationSection("AmonPack.Items")
                .getKeys(false)) {
            if (key.equalsIgnoreCase(itemname)) {
                String name = null;
                String type = AmonPackPlugin.getAbilitiesConfig().getString("AmonPack.Items." + key + ".Type");
                if (AmonPackPlugin.getAbilitiesConfig().getString("AmonPack.Items." + key + ".Name") != null) {
                    name = "" + AmonPackPlugin.getAbilitiesConfig().getString("AmonPack.Items." + key + ".Name")
                            .replace("&", "§");
                }
                List<String> lorelist = new ArrayList<String>();
                if (AmonPackPlugin.getAbilitiesConfig()
                        .getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
                    for (String lores : AmonPackPlugin.getAbilitiesConfig()
                            .getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
                        String lore = "" + AmonPackPlugin.getAbilitiesConfig()
                                .getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");
                        ;
                        if (lore != null) {
                            lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
                        }
                    }
                }
                QuestItem = new ItemStack(Material.getMaterial(type), 1);
                ItemMeta QuestItemMeta = QuestItem.getItemMeta();
                if (AmonPackPlugin.getAbilitiesConfig()
                        .getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
                    for (String enchname : AmonPackPlugin.getAbilitiesConfig()
                            .getConfigurationSection("AmonPack.Items." + key + ".Enchantment").getKeys(false)) {
                        int enchpower = AmonPackPlugin.getAbilitiesConfig()
                                .getInt("AmonPack.Items." + key + ".Enchantment." + enchname + ".EnchantmentLevel");
                        QuestItemMeta.addEnchant(Enchantment.getByName(enchname), enchpower, true);
                    }
                }
                if (name != null) {
                    QuestItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                }
                if (lorelist != null) {
                    QuestItemMeta.setLore(lorelist);
                }
                QuestItem.setItemMeta(QuestItemMeta);
            }
        }
        return QuestItem;
    }

    public static class ExecuteCommandExample {
        public void executeCommand(String command) {
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(console, command);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (cmd.getName().equalsIgnoreCase("dungeons")) {
            if (args.length == 1) {
                List<String> subCommands = Arrays.asList("start", "leave", "skills", "reload");
                String currentArg = args[0].toLowerCase();
                for (String sub : subCommands) {
                    if (sub.startsWith(currentArg)) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
                String currentArg = args[1].toLowerCase();
                if (RPG.Dungeons.DungeonManager.getInstance() != null) {
                    for (String key : RPG.Dungeons.DungeonManager.getInstance().getTemplates().keySet()) {
                        if (key.startsWith(currentArg)) {
                            completions.add(key);
                        }
                    }
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("party")) {
            if (args.length == 1) {
                List<String> subCommands = Arrays.asList("invite", "accept", "leave", "kick", "pvp", "list", "chat",
                        "togglechat");
                String currentArg = args[0].toLowerCase();
                for (String sub : subCommands) {
                    if (sub.startsWith(currentArg)) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
                String currentArg = args[1].toLowerCase();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(currentArg)) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        }

        return completions;
    }

}
