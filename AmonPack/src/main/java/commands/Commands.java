package commands;

import java.util.ArrayList;
import java.util.List;

import AvatarSystems.Levels.PlayerBendingBranch;
import AvatarSystems.Util_Objects.LevelSkill;
import AvatarSystems.Levels.PlayerLevelMenager;
//import Mechanics.MMORPG.GuiMenu;
import Mechanics.PVE.Menagerie.MenagerieMenager;
import AvatarSystems.Bounties.BountiesMenager;
//import Mechanics.PVP.newPvP;
import com.projectkorra.projectkorra.Element;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Commands implements CommandExecutor {

    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "level":
                if (args != null && args.length == 4 && args[0].equalsIgnoreCase("magic")) {
                    try {
                        Player p = Bukkit.getPlayer(args[1]);
                        PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(p.getName());
                        if (branch != null) {
                            // magic amon set fire -> ustawia na fire
                            // magic amon 5 fire -> daje 5 points fire
                            // magic amon 5 all -> daje 5 points wszystkim zywiolom
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
                            // magic amon 5 fire -> tworzy branch, zapisuje, ustawia element, dodaje do tego
                            // elementu punkty
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
                        try {
                            PlayerLevelMenager.TryOpenPlayerLevel((Player) sender);
                        } catch (Exception e) {
                            System.out.println("Error!!! " + e.getMessage());
                        }
                    }
                }
                break;
            case "reload":
                AmonPackPlugin.reloadAllConfigs();
                break;
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
                }
                break;
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            switch (cmd.getName().toLowerCase()) {
                /*
                 * case "craft":
                 * ItemStack item = player.getInventory().getItemInMainHand();
                 * CraftedWeapon Mold= CraftingMenager.getCraftedWeaponByItem(item);
                 * if(args.length!=0 && Mold!=null) {
                 * if(args[0].equalsIgnoreCase("ogien")){
                 * List<MagicEffects> ListOfEffects = new ArrayList<>(List.of(new
                 * MagicEffects("Podpalenie_1")));
                 * Mold.Craft(player,ListOfEffects,item,false);
                 * }
                 * if(args[0].equalsIgnoreCase("earth")){
                 * List<MagicEffects> ListOfEffects = new ArrayList<>(List.of(new
                 * MagicEffects("Earth_1")));
                 * Mold.Craft(player,ListOfEffects,item,false);
                 * }
                 * if(args[0].equalsIgnoreCase("fire")){
                 * List<MagicEffects> ListOfEffects = new ArrayList<>(List.of(new
                 * MagicEffects("Agility_1")));
                 * Mold.Craft(player,ListOfEffects,item,false);
                 * }
                 * if(args[0].equalsIgnoreCase("craft")){
                 * List<MagicEffects> ListOfEffects = new ArrayList<>();
                 * Mold.Craft(player,ListOfEffects,item,true);
                 * }
                 * }else{
                 * for (CraftedWeapon w : AllCraftableWeapons){
                 * player.getInventory().addItem(w.to_Empty_Mold_ItemStack());
                 * }
                 * }
                 * break;
                 */
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
                    MenagerieMenager.StartMenagerie(listofplayers, args[0]);
                    break;
                case "arenabuilding":
                    if (args[0].equalsIgnoreCase("On")) {
                        AmonPackPlugin.BuildingOn();
                        player.sendMessage(ChatColor.RED + "Można Budować");
                    } else {
                        AmonPackPlugin.BuildingOff();
                        player.sendMessage(ChatColor.RED + "Nie Można Budować");
                    }
                    break;
            }
        } else {
            switch (cmd.getName().toLowerCase()) {
                /*
                 * case "pvp":
                 * if(args[0].equalsIgnoreCase("rtp")){
                 * try {
                 * Player player =
                 * Bukkit.getOnlinePlayers().stream().filter(p->p.getName().equalsIgnoreCase(
                 * args[1])).collect(Collectors.toList()).get(0);
                 * Location loc = PvPMethods.RTP(newPvP.radius,newPvP.Loc);
                 * player.teleport(loc);
                 * } catch (Exception e) {
                 * System.out.println("blad z rtp "+e.getMessage());
                 * }
                 * } else if (args[0].equalsIgnoreCase("on")) {
                 * AmonPackPlugin.PvPon();
                 * } else if (args[0].equalsIgnoreCase("off")) {
                 * AmonPackPlugin.PvPoff();
                 * }
                 * break;
                 */
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
                    MenagerieMenager.StartMenagerie(listofplayers, args[0]);
                    break;
            }
        }

        if (cmd.getName().equalsIgnoreCase("QuestItems")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                for (String key : AmonPackPlugin.plugin.getConfig().getConfigurationSection("AmonPack.Items")
                        .getKeys(false)) {
                    if (AmonPackPlugin.plugin.getConfig().getString("AmonPack.Items." + key + ".Name") != null) {
                        String type = AmonPackPlugin.plugin.getConfig().getString("AmonPack.Items." + key + ".Type");
                        String name = "" + AmonPackPlugin.plugin.getConfig()
                                .getString("AmonPack.Items." + key + ".Name").replace("&", "§");
                        List<String> lorelist = new ArrayList<String>();
                        if (AmonPackPlugin.plugin.getConfig()
                                .getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
                            for (String lores : AmonPackPlugin.plugin.getConfig()
                                    .getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
                                String lore = "" + AmonPackPlugin.plugin.getConfig()
                                        .getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");
                                ;
                                if (lore != null) {
                                    lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
                                }
                            }
                        }
                        ItemStack QuestItem = new ItemStack(Material.getMaterial(type), 1);
                        ItemMeta QuestItemMeta = QuestItem.getItemMeta();
                        if (AmonPackPlugin.plugin.getConfig()
                                .getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
                            for (String enchname : AmonPackPlugin.plugin.getConfig()
                                    .getConfigurationSection("AmonPack.Items." + key + ".Enchantment").getKeys(false)) {
                                int enchpower = AmonPackPlugin.plugin.getConfig().getInt(
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
        }
        return false;
    }

    public static ItemStack QuestItemConfig(String itemname) {
        ItemStack QuestItem = new ItemStack(Material.DIRT, 1);
        for (String key : AmonPackPlugin.plugin.getConfig().getConfigurationSection("AmonPack.Items").getKeys(false)) {
            if (key.equalsIgnoreCase(itemname)) {
                String name = null;
                String type = AmonPackPlugin.plugin.getConfig().getString("AmonPack.Items." + key + ".Type");
                if (AmonPackPlugin.plugin.getConfig().getString("AmonPack.Items." + key + ".Name") != null) {
                    name = "" + AmonPackPlugin.plugin.getConfig().getString("AmonPack.Items." + key + ".Name")
                            .replace("&", "§");
                }
                List<String> lorelist = new ArrayList<String>();
                if (AmonPackPlugin.plugin.getConfig()
                        .getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
                    for (String lores : AmonPackPlugin.plugin.getConfig()
                            .getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
                        String lore = "" + AmonPackPlugin.plugin.getConfig()
                                .getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");
                        ;
                        if (lore != null) {
                            lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
                        }
                    }
                }
                QuestItem = new ItemStack(Material.getMaterial(type), 1);
                ItemMeta QuestItemMeta = QuestItem.getItemMeta();
                if (AmonPackPlugin.plugin.getConfig()
                        .getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
                    for (String enchname : AmonPackPlugin.plugin.getConfig()
                            .getConfigurationSection("AmonPack.Items." + key + ".Enchantment").getKeys(false)) {
                        int enchpower = AmonPackPlugin.plugin.getConfig()
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
        for (String key : AmonPackPlugin.plugin.getConfig().getConfigurationSection("AmonPack.Items").getKeys(false)) {
            if (key.equalsIgnoreCase(itemname)) {
                String name = null;
                String type = AmonPackPlugin.plugin.getConfig().getString("AmonPack.Items." + key + ".Type");
                if (AmonPackPlugin.plugin.getConfig().getString("AmonPack.Items." + key + ".Name") != null) {
                    name = "" + AmonPackPlugin.plugin.getConfig().getString("AmonPack.Items." + key + ".Name")
                            .replace("&", "§");
                }
                List<String> lorelist = new ArrayList<String>();
                if (AmonPackPlugin.plugin.getConfig()
                        .getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
                    for (String lores : AmonPackPlugin.plugin.getConfig()
                            .getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
                        String lore = "" + AmonPackPlugin.plugin.getConfig()
                                .getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");
                        ;
                        if (lore != null) {
                            lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
                        }
                    }
                }
                QuestItem = new ItemStack(Material.getMaterial(type), 1);
                ItemMeta QuestItemMeta = QuestItem.getItemMeta();
                if (AmonPackPlugin.plugin.getConfig()
                        .getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
                    for (String enchname : AmonPackPlugin.plugin.getConfig()
                            .getConfigurationSection("AmonPack.Items." + key + ".Enchantment").getKeys(false)) {
                        int enchpower = AmonPackPlugin.plugin.getConfig()
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

}
