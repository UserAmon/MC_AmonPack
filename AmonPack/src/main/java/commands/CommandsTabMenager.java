package commands;
/*
import Mechanics.PVE.Menagerie.Menagerie;
import Mechanics.PVE.Menagerie.MenagerieMenager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CommandsTabMenager implements TabCompleter {
    /*@Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof  Player){
            Player player = (Player) sender;
            switch (command.getName().toLowerCase()) {
                case "goworld":
                    if (args.length == 1) {
                        List<String> suggestions = new ArrayList<>();
                        suggestions.add("PodZadaniowy");
                        String folderPath = "MultiWorlds";
                        File folder = new File(folderPath);
                        if (folder.exists() && folder.isDirectory()) {
                            File[] files = folder.listFiles();
                            if (files != null && files.length > 0) {
                                for (File file : files) {
                                        suggestions.add(file.getName());
                                }
                            }}
                        return suggestions;
                    }
                    if (args.length == 2 && !args[0].equalsIgnoreCase("PodZadaniowy")) {
                        List<String> suggestions = new ArrayList<>();
                        String folderPath = "MultiWorlds/"+args[0];
                        File folder = new File(folderPath);
                        if (folder.exists() && folder.isDirectory()) {
                            File[] files = folder.listFiles();
                            if (files != null && files.length > 0) {
                                for (File file : files) {
                                    suggestions.add(file.getName());
                                }
                            }}
                        return suggestions;
                    }
                    break;
                case "fallchest":
                    if (args.length == 1) {
                        List<String> suggestions = new ArrayList<>();
                        suggestions.add("Multibend");
                        suggestions.add("Semibend");
                        suggestions.add("On");
                        suggestions.add("Off");
                        suggestions.add("Fall");
                        suggestions.add("Event");
                        suggestions.add("Rtp");
                        return suggestions;
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("Event")) {
                        List<String> suggestions = new ArrayList<>();
                        suggestions.add("Boss");
                        suggestions.add("Loot");
                        suggestions.add("Zar");
                        suggestions.add("Chlod");
                        suggestions.add("Mapa");
                        suggestions.add("RTP");
                        return suggestions;
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("Rtp")) {
                        List<String> suggestions = new ArrayList<>();
                        suggestions.add("1");
                        suggestions.add("2");
                        return suggestions;
                    }
                    break;
                /*case "dungeon":
                    if (args.length == 1) {
                        List<String> suggestions = new ArrayList<>();
                        for (Dungeon dung:Dungeons.ListOfDungeons) {
                            suggestions.add(dung.getName());
                        }
                        return suggestions;
                    }
                    break;*/
                /*case "wave":
                    if (args.length == 1) {
                        List<String> suggestions = new ArrayList<>();
                        for (AssaultDef A: AssaultMenager.listOfAssaultDef) {
                            suggestions.add(A.getName());
                        }
                        for (AssaultOffensive A: AssaultMenager.listOfAssaultOffens) {
                            suggestions.add(A.getName());
                        }
                        return suggestions;
                    }
                    break;
                case "ava":
                    if (args.length == 1) {
                        List<String> suggestions = new ArrayList<>();
                        suggestions.add("bind");
                        return suggestions;
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("bind")) {
                        List<String> suggestions = new ArrayList<>();
                        /*for (AssaultDef A:AssaultMenager.listOfAssaultDef) {
                            if (AssaultMethods.InArenaRange(player.getLocation(),A.getArenaLocation(),A.getRange(),A.getRange())){
                                if (A.BonusAbilities.get(player) != null){
                                    for (String st:A.BonusAbilities.get(player)) {
                                        suggestions.add(st);
                                    }
                                }
                            }
                        }*/
                        /*for (SkillTreeObj STO:SkillPoints) {
                            if (STO.getPlayer().equalsIgnoreCase(sender.getName())){
                                for (STAbility sto: PGrowth.STAList) {
                                    if (STO.getSelectedPath().contains(sto.getName())){
                                        suggestions.add(sto.getName());
                                    }}}
                        }*/
                        /*return suggestions;
                    }
                    break;
                case "arenabuilding":
                    if (args.length == 1) {
                        List<String> suggestions = new ArrayList<>();
                        suggestions.add("On");
                        suggestions.add("Off");
                        return suggestions;
                    }
                    break;
                case "menagerie":
                    if (args.length == 1) {
                        List<String> suggestions = new ArrayList<>();
                        for (Menagerie mena: MenagerieMenager.ListOfAllMenageries) {
                            suggestions.add(mena.getMenagerieName());
                        }
                        return suggestions;
                    }
                    break;
            }
        }
        return new ArrayList<>();
    }
}*/
