package commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


import Mechanics.MMORPG.GuiMenu;
import Mechanics.MMORPG.ReputationMenager;
import Mechanics.PVE.Menagerie.BoardManager;
import Mechanics.PVE.Menagerie.MenagerieMenager;
import Mechanics.PVE.Mining;
import Mechanics.PVE.SimpleWorldGenerator;
import Mechanics.Skills.BendingGuiMenu;
import Mechanics.Skills.JobsMenager;
import UtilObjects.Skills.PlayerSkillTree;
import com.projectkorra.projectkorra.BendingPlayer;
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
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;


public class Commands implements CommandExecutor {

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		switch (cmd.getName().toLowerCase()) {
			case "menagerie":
				Player p = Bukkit.getPlayer(args[0]);
				MenagerieMenager.StartMenagerie(p,args[1]);
				break;
			case "spelltree":
				try {
					Player pl = Bukkit.getPlayer(args[1]);
					if(args[0].equalsIgnoreCase("Multibend")){
						BendingGuiMenu.getPlayerSkillTreeByName(pl).setMultibend(Boolean.parseBoolean(args[2]));
					}else if(args[0].equalsIgnoreCase("Create")){
						PlayerSkillTree NPST = new PlayerSkillTree(args[1],Integer.parseInt(args[2]),"",args[3],args[3]);
						 NPST.ResetSkillTree(NPST);
					}else if(args[0].equalsIgnoreCase("Set")){
						 PlayerSkillTree NPST = new PlayerSkillTree(args[1],Integer.parseInt(args[2]),args[3],args[4],args[5]);
						 NPST.ResetSkillTree(NPST);
					 }else if (args[0].equalsIgnoreCase("AddP")){
						 int i = BendingGuiMenu.getPlayerSkillTreeByName(Bukkit.getPlayer(args[1])).getActSkillPoints();
						BendingGuiMenu.getPlayerSkillTreeByName(Bukkit.getPlayer(args[1])).setActSkillPoints(i+Integer.parseInt(args[2]));
					}else if (args[0].equalsIgnoreCase("AddE")){
						 PlayerSkillTree PST = BendingGuiMenu.getPlayerSkillTreeByName(Bukkit.getPlayer(args[1]));
						PST.AddElement(PST,args[2]);
					}
				}catch (Exception e){
					System.out.println("JAKIS BLAD   Z KOMENDA SPELLTREE   " + e.getMessage());
				}

				break;
		}
		/*if(cmd.getName().equalsIgnoreCase("Dungeon")) {
				List<Player> PList = new ArrayList<>();
			PList.add((Player) sender);
				for (String playerName : args) {
					Player targetPlayer = Bukkit.getServer().getPlayer(playerName);
					if (targetPlayer != null) {
						PList.add(targetPlayer);
					}}
				try {
					Dungeons.StartDungeon(PList, args[0]);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
			}}*/
		/*if(cmd.getName().equalsIgnoreCase("SpellTree")) {
			if (args[0].equalsIgnoreCase("Set")) {
				//for (SkillTreeObj sto:SkillPoints) {
				//if (sto.getPlayer().equalsIgnoreCase(args[0])){
				try {
					System.out.println(args[1]+"  "+args[2]+"  "+args[3]);
					AddPoints(args[1], Integer.parseInt(args[2]));
					AddElement(args[1], args[3]);
					//BendingGuiMenu.AllPlayersSkillTrees.add()
				} catch (SQLException e) {
					e.printStackTrace();
				}}}*/			//}}
		if (sender instanceof Player) {
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer((OfflinePlayer) sender);
			Player player = (Player) sender;
			switch (cmd.getName().toLowerCase()) {
				case "spelltree":
					PlayerSkillTree NPST = new PlayerSkillTree(player.getName(),10,"","","Fire");
					BendingGuiMenu.AllPlayersSkillTrees.add(NPST);
					break;
					/*
				case "woda":
					if (bPlayer.hasElement(Element.WATER)) {
						PGrowth.OpenBendingGui((Player) sender, Element.WATER);
					} else {
						sender.sendMessage(ChatColor.RED + "Nie masz dostępu do tego żywiołu!");
					}
					break;
				case "ogien":
					if (bPlayer.hasElement(Element.FIRE)) {
						PGrowth.OpenBendingGui((Player) sender, Element.FIRE);
					} else {
						sender.sendMessage(ChatColor.RED + "Nie masz dostępu do tego żywiołu!");
					}
					break;
				case "ziemia":
					if (bPlayer.hasElement(Element.EARTH)) {
						PGrowth.OpenBendingGui((Player) sender, Element.EARTH);
					} else {
						sender.sendMessage(ChatColor.RED + "Nie masz dostępu do tego żywiołu!");
					}
					break;
				case "powietrze":
					if (bPlayer.hasElement(Element.AIR)) {
						PGrowth.OpenBendingGui((Player) sender, Element.AIR);
					} else {
						sender.sendMessage(ChatColor.RED + "Nie masz dostępu do tego żywiołu!");
					}
					break;
				case "ava":
					if (args.length == 2 && args[0].equalsIgnoreCase("bind")) {
						bPlayer.bindAbility(args[1]);
					}
					break;
					*/
				case "skills":
					BendingGuiMenu.OpenGeneralBendingMenu(player);
					break;
				case "itemy":
					GuiMenu.OItemGui((Player) sender);
					break;
				case "pomoc":
					GuiMenu.OHelpGui((Player) sender);
					break;
				case "quests":
					AmonPackPlugin.reloadAllConfigs();
					break;
				case "menagerie":
					MenagerieMenager.StartMenagerie(player,args[0]);
					break;
				case "jobs":
					if (args[0].equalsIgnoreCase("show")) {
						JobsMenager.ShowPlayerData(player);
					}
					if (args[0].equalsIgnoreCase("add")) {
						if(args[2]==null){
							player.sendMessage("Error with command!!!");
						}else{
						int i1 = Integer.parseInt(args[1]);
						int i2 = Integer.parseInt(args[2]);
						try {
							JobsMenager.AddPoints(player.getName(),i1,i2);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
					}
					break;
				case "scbadder":
					BoardManager BM = new BoardManager();
					if (args[0].equalsIgnoreCase("remove")) {
						BM.removeTopRows(player);
					}else{
						int numRows = Integer.parseInt(args[0]);
						StringBuilder text = new StringBuilder();
						for (int i = 1; i < args.length; i++) {
							text.append(args[i]).append(" ");
						}
						BM.addRowsWithoutremove(player, String.valueOf(text), numRows);
					}
					break;
				case "goworld":
					if (args.length==1){
						SimpleWorldGenerator.createAndSaveTemporaryWorld(player,args[0], "",null);
					}else {
						SimpleWorldGenerator.createAndSaveTemporaryWorld(player,args[0], args[1],null);
					}
				case "arenabuilding":
					if (args[0].equalsIgnoreCase("On")){
						AmonPackPlugin.BuildingOn();
						player.sendMessage(ChatColor.RED+"Można Budować");
					}else {
						AmonPackPlugin.BuildingOff();
						player.sendMessage(ChatColor.RED+"Nie Można Budować");
					}
					break;
				case "reputation":
					ReputationMenager.OpenRepGui((Player) sender);
					break;
				case "mingathoff":
					AmonPackPlugin.off();
					break;
				case "mingathon":
					AmonPackPlugin.on();
					break;
			}
		}/*
		if(cmd.getName().equalsIgnoreCase("FallChest")) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				if (args[0].equalsIgnoreCase("Multibend")){
					Multibend = true;
				}
				if (args[0].equalsIgnoreCase("Semibend")){
					Multibend = false;
				}
				if (args[0].equalsIgnoreCase("On")){
					AmonPackPlugin.PvPon();
				}else if (args[0].equalsIgnoreCase("Off")){
					AmonPackPlugin.PvPoff();
				}else if (args[0].equalsIgnoreCase("Fall")){
					PvP.Fall();
				}else
				if (args[0].equalsIgnoreCase("Event")){
					PvP.ActEvent = "";
					Bukkit.getScheduler().cancelTask(taskId);
					if (args[1].equalsIgnoreCase("Boss")){
						PvP.RaidBoss();
						PvP.ActEvent = "Boss";
					}else if (args[1].equalsIgnoreCase("Loot")){
						for (Player player : Bukkit.getOnlinePlayers()) {
							player.sendMessage(ChatColor.DARK_PURPLE+"Król **** **** dostrzegł wasze dokonania. Radujcie się jego darami!");
							player.sendMessage(ChatColor.DARK_PURPLE+"Zwiększony loot, spada więcej skrzyń, szybkość dla każdego!!");
						}
						PvP.LootCounter = 2;
						PvP.ActEvent = "Loot";
						GlobalPotions(PotionEffectType.SPEED);
					}else if (args[1].equalsIgnoreCase("Zar")){
						for (Player player : Bukkit.getOnlinePlayers()) {
							player.sendMessage(ChatColor.DARK_PURPLE+"Żar leje się z nieba, trzymaj się blisko wody!");
						}
						PvP.ClimateChange(PvP.WMats, PotionEffectType.CONFUSION, 8);
						PvP.ActEvent = "Zar";
					}else if (args[1].equalsIgnoreCase("Chlod")){
						for (Player player : Bukkit.getOnlinePlayers()) {
							player.sendMessage(ChatColor.DARK_PURPLE+"Nadciągają chłody, trzymaj się blisko ognia!");
						}
						PvP.ClimateChange(PvP.FMats, PotionEffectType.SLOW, 3);
						PvP.ActEvent = "Chlod";
					}else if (args[1].equalsIgnoreCase("Mapa")){
						PvP.MapRollBack();
					}else {
						PvP.RandomSpawner();
					}
				}else
				if (args[0].equalsIgnoreCase("RTP")){
					p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
					if (args[1].equalsIgnoreCase("1")){
						p.teleport(PvP.RTPPvP1());
					}
					if (args[1].equalsIgnoreCase("2")){
						p.teleport(PvP.RTP());
					}
					if (!p.getInventory().contains(Material.COMPASS)){
						p.getInventory().addItem(new ItemStack(Material.COMPASS));
					}
					sendTitleMessage(p,ChatColor.DARK_PURPLE + "Losowy Teleport!", ChatColor.YELLOW + "", 20,40,20);
				}else{
					System.out.println("Błąd komendy");
				}
			}else if (args[0].equalsIgnoreCase("RTP")){
				Player p = Bukkit.getPlayer(args[2]);
				if (p!=null){
				p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
				if (args[1].equalsIgnoreCase("1")){
					p.teleport(PvP.RTPPvP1());
				}
				if (args[1].equalsIgnoreCase("2")){
					p.teleport(PvP.RTP());
				}
				if (!p.getInventory().contains(Material.COMPASS)){
					p.getInventory().addItem(new ItemStack(Material.COMPASS));
				}
				sendTitleMessage(p,ChatColor.DARK_PURPLE + "Losowy Teleport!", ChatColor.YELLOW + "", 20,40,20);
			}}

		}*/
	    if(cmd.getName().equalsIgnoreCase("QuestItems")) {
    		if (sender instanceof Player) {
       		 	Player player = (Player) sender;
       		for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items").getKeys(false)) {
				if (AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Name") != null) {
       			String type = AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Type");
				String name = ""+AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Name").replace("&", "§");
       			List<String> lorelist = new ArrayList<String>();
       			if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
       			for(String lores : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
           		String lore = ""+AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");;
           		if (lore != null) {
               		lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
           		}}}
       			ItemStack QuestItem = new ItemStack(Material.getMaterial(type), 1);
       	        ItemMeta QuestItemMeta = QuestItem.getItemMeta();
       			if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
       			for(String enchname : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Enchantment").getKeys(false)) {
           		int enchpower = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Items." + key + ".Enchantment." + enchname + ".EnchantmentLevel");
				QuestItemMeta.addEnchant(Enchantment.getByName(enchname), enchpower, true);
       			}}
       	        if (name != null) {
				QuestItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
      			}
				if (lorelist != null) {
				QuestItemMeta.setLore(lorelist);
      			}
				QuestItem.setItemMeta(QuestItemMeta);
       			player.getInventory().addItem(QuestItem);
       		}}
			//player.getInventory().addItem(Kopalnie.PickaxeTier1);
			//player.getInventory().addItem(Kopalnie.PickaxeTier2);
			player.getInventory().addItem(Mining.PickaxeTier3);
       	 } else {
             System.out.println("QuestItems");
           }
	    }
	    return false;
	}

	public static ItemStack QuestItemConfig(String itemname){
		ItemStack QuestItem = new ItemStack(Material.DIRT, 1);
		for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items").getKeys(false)) {
			if (key.equalsIgnoreCase(itemname)){
			String name = null;
			String type = AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Type");
			if (AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Name") != null) {
				name = ""+AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Name").replace("&", "§");
			}
			List<String> lorelist = new ArrayList<String>();
			if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
				for(String lores : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
					String lore = ""+AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");;
					if (lore != null) {
						lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
					}}}
			QuestItem = new ItemStack(Material.getMaterial(type), 1);
			ItemMeta QuestItemMeta = QuestItem.getItemMeta();
			if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
			for(String enchname : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Enchantment").getKeys(false)) {
			int enchpower = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Items." + key + ".Enchantment." + enchname + ".EnchantmentLevel");
			QuestItemMeta.addEnchant(Enchantment.getByName(enchname), enchpower, true);
			}}
			if (name != null) {
				QuestItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
			}
			if (lorelist != null) {
				QuestItemMeta.setLore(lorelist);
			}
			QuestItem.setItemMeta(QuestItemMeta);
		}}
		for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items").getKeys(false)) {
			if (key.equalsIgnoreCase(itemname)){
				String name = null;
				String type = AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Type");
				if (AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Name") != null) {
					name = ""+AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Name").replace("&", "§");
				}
				List<String> lorelist = new ArrayList<String>();
				if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
					for(String lores : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
						String lore = ""+AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");;
						if (lore != null) {
							lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
						}}}
				QuestItem = new ItemStack(Material.getMaterial(type), 1);
				ItemMeta QuestItemMeta = QuestItem.getItemMeta();
				if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
					for(String enchname : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Enchantment").getKeys(false)) {
						int enchpower = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Items." + key + ".Enchantment." + enchname + ".EnchantmentLevel");
						QuestItemMeta.addEnchant(Enchantment.getByName(enchname), enchpower, true);
					}}
				if (name != null) {
					QuestItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
				}
				if (lorelist != null) {
					QuestItemMeta.setLore(lorelist);
				}
				QuestItem.setItemMeta(QuestItemMeta);
			}}
		return QuestItem;
	}
	public static class ExecuteCommandExample {
		public void executeCommand(String command) {
			ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
			Bukkit.dispatchCommand(console, command);
		}
	}

}