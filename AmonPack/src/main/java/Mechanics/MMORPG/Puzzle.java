package Mechanics.MMORPG;

import commands.Commands;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.ArrayList;
import java.util.List;

import static methods_plugins.AmonPackPlugin.plugin;
import static methods_plugins.Methods.getRandom;


public class Puzzle implements CommandExecutor, Listener {
	ItemStack UnConnected = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
	ItemStack Connected = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
	ItemStack Broken = new ItemStack(Material.RED_STAINED_GLASS_PANE);
	ItemStack Batery = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
	ItemStack Power = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
	ItemStack Core = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);

	public Puzzle() {
		ItemMeta glassMeta = UnConnected.getItemMeta();
		glassMeta.setDisplayName(ChatColor.DARK_PURPLE + "UnConnected");
		UnConnected.setItemMeta(glassMeta);
		glassMeta = Connected.getItemMeta();
		glassMeta.setDisplayName(ChatColor.GOLD + "Connected");
		Connected.setItemMeta(glassMeta);
		glassMeta = Broken.getItemMeta();
		glassMeta.setDisplayName(ChatColor.RED + "Broken");
		Broken.setItemMeta(glassMeta);
		glassMeta = Batery.getItemMeta();
		glassMeta.setDisplayName(ChatColor.GREEN + "Batery");
		Batery.setItemMeta(glassMeta);
		glassMeta = Core.getItemMeta();
		glassMeta.setDisplayName(ChatColor.WHITE + "Core");
		Core.setItemMeta(glassMeta);
		glassMeta = Power.getItemMeta();
		glassMeta.setDisplayName(ChatColor.GREEN + "Power");
		Power.setItemMeta(glassMeta);
	}
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
		if (!(sender instanceof Player) && args != null) {
		Player player = Bukkit.getPlayer(args[1]);
		if(cmd.getName().equalsIgnoreCase("Puzzle") && args[1] != null) {
  		for(String keys : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Puzzle").getKeys(false)) {
			  if (args[0].equalsIgnoreCase(keys)){
				List<String> PuzzleList = AmonPackPlugin.getNewConfigz().getStringList("AmonPack.Puzzle."+keys);
				  OpenPuzzleGui(player,PuzzleList);
				  return true;
			  }}}}
		return false;
	}
	public void OpenPuzzleGui(Player player, List<String> list) {
		Inventory menu = Bukkit.createInventory(null, 54, "Puzzle");
		String CommandType = null;
		int BrokenNum = 1;
		String randomplace = "false";
		List<Integer> CoreNum = new ArrayList<>();
		List<Integer> BrokenPlace = new ArrayList<>();
		int BateryNum = 1;
		for (int i = 0; i < list.size(); i++) {
			String Option = list.get(i);
			if (Option.startsWith("Command:")){
				CommandType = Option.substring("Command:".length());
				if (CommandType.contains("ThisPlayer")) {
					String newSentence = CommandType.replace("ThisPlayer", player.getName());
					CommandType = newSentence;
				}}
			if (Option.startsWith("BrokenChance:")){
				BrokenNum = Integer.parseInt(Option.substring("BrokenChance:".length()));
			}
			if (Option.startsWith("IsRandom:")){
				randomplace = Option.substring("IsRandom:".length());
			}
			if (Option.startsWith("BrokenPlace:")){
				BrokenPlace.add(Integer.parseInt(Option.substring("BrokenPlace:".length())));
			}
			if (Option.startsWith("CorePlace:")){
				CoreNum.add(Integer.parseInt(Option.substring("CorePlace:".length())));
			}
			if (Option.startsWith("BateryPlace:")){
				BateryNum = Integer.parseInt(Option.substring("BateryPlace:".length()));
			}}
		String[] test = new String[1];
		test[0] = CommandType;
		player.setMetadata("puzzleArgs", new FixedMetadataValue(plugin, test));


		for (int i = 0; i < 54; i++) {

			if (randomplace.equalsIgnoreCase("true")){
				int ran = getRandom(0, 100);
				if (ran < BrokenNum){
					menu.setItem(i, Broken);
				}else if (ran % 2 != 0){
					menu.setItem(i, Connected);
				}else if (ran % 2 == 0){
					menu.setItem(i, UnConnected);
				}
			}else {
				int ran = getRandom(0, 100);
				if (ran < BrokenNum){
					menu.setItem(i, Broken);
				}else if (i % 2 != 0){
					menu.setItem(i, Connected);
				}else if (i % 2 == 0){
					menu.setItem(i, UnConnected);
				}
			}
		}
		menu.setItem(BateryNum, Batery);
		for (Integer number : CoreNum) {
			menu.setItem(number, Core);
		}
		for (Integer brokenplacenum : BrokenPlace) {
			menu.setItem(brokenplacenum, Broken);
		}
		player.openInventory(menu);
		checkaround(menu, player);
	}
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
			if (event.getCurrentItem() != null && event.getView().getTitle().equals("Puzzle")  && event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
			ItemStack clickedItem = event.getCurrentItem();
			Player player = (Player) event.getWhoClicked();
			event.setCancelled(true);
			int clickedSlot = event.getSlot();
			Inventory clickedInventory = event.getClickedInventory();
				if (clickedItem != null && clickedItem.isSimilar(UnConnected)) {
					event.setCurrentItem(Connected);
					((Player) event.getWhoClicked()).updateInventory();
				}else if (clickedItem != null && clickedItem.isSimilar(Connected)) {
					event.setCurrentItem(UnConnected);
					((Player) event.getWhoClicked()).updateInventory();
				}else if (clickedItem != null && clickedItem.isSimilar(Power)) {
					event.setCurrentItem(UnConnected);
					((Player) event.getWhoClicked()).updateInventory();
				}else {
					return;
				}
				if (clickedItem.isSimilar(UnConnected) || clickedItem.isSimilar(Connected) || clickedItem.isSimilar(Power)) {
					if ((clickedSlot+1)%9!=0){
						FlipAroundAdd(clickedInventory, clickedSlot, 1);
						FlipAroundAdd(clickedInventory, clickedSlot, 10);
						FlipAroundSub(clickedInventory, clickedSlot, 8);
					}
					if (clickedSlot%9!=0){
						FlipAroundSub(clickedInventory, clickedSlot, 1);
						FlipAroundSub(clickedInventory, clickedSlot, 10);
						FlipAroundAdd(clickedInventory, clickedSlot, 8);
					}
					FlipAroundAdd(clickedInventory, clickedSlot, 9);
					FlipAroundSub(clickedInventory, clickedSlot, 9);
				}
				for (int i = 0; i < clickedInventory.getSize(); i++) {
					if (clickedInventory.getItem(i).isSimilar(Power)){
						clickedInventory.setItem(i, Connected);
					}}
				checkaround(clickedInventory, player);
			}
	}
	void checkaround(Inventory clickedInventory, Player player){
		int size = clickedInventory.getSize();
		int reqcoreact = 0;
		int coreCounter = 0;
		for (int i = 0; i < size; i++) {
			if (clickedInventory.getItem(i).isSimilar(Core)){
				coreCounter++;
			}
		}
		for (int i = 0; i < size; i++) {
			if (i < size && clickedInventory.getItem(i).isSimilar(Batery) || clickedInventory.getItem(i).isSimilar(Power) && i!=0){
				if (i>0 && clickedInventory.getItem(i-1).isSimilar(Connected) && i%9!=0){
					clickedInventory.setItem(i-1, Power);
					i = 0;
				}
				if (i>8 && clickedInventory.getItem(i-9).isSimilar(Connected)){
					clickedInventory.setItem(i-9, Power);
					i = 0;
				}
				if (i<clickedInventory.getSize()-1 && clickedInventory.getItem(i+1).isSimilar(Connected)&& (i+1)%9!=0){
					if (clickedInventory.getItem(i).isSimilar(Power) || clickedInventory.getItem(i).isSimilar(Batery)){
					clickedInventory.setItem(i+1, Power);
					i = 0;
				}}
				if (i<clickedInventory.getSize()-9 && clickedInventory.getItem(i+9).isSimilar(Connected)){
					if (clickedInventory.getItem(i).isSimilar(Power) || clickedInventory.getItem(i).isSimilar(Batery)){
					clickedInventory.setItem(i+9, Power);
					i = 0;
				}}}
			if (i < size && clickedInventory.getItem(i).isSimilar(Core)){
				if (i>0 && clickedInventory.getItem(i-1).isSimilar(Power) && i%9!=0){
					reqcoreact++;
				}else
				if (i>8 && clickedInventory.getItem(i-9).isSimilar(Power)){
					reqcoreact++;
				}else
				if (i<clickedInventory.getSize()-1 && clickedInventory.getItem(i+1).isSimilar(Power)&& (i+1)%9!=0){
					reqcoreact++;
				}else
				if (i<clickedInventory.getSize()-9 && clickedInventory.getItem(i+9).isSimilar(Power)){
					reqcoreact++;
				}else {
					reqcoreact = 0;
				}
			}}
		if (reqcoreact >= coreCounter){

			List<MetadataValue> metadata = player.getMetadata("puzzleArgs");
			if (!metadata.isEmpty()) {
				String[] args = (String[]) metadata.get(0).value();
				StringBuilder stringBuilder = new StringBuilder();
				for (String arg : args) {
					if (!arg.equalsIgnoreCase(player.getName())) {
						stringBuilder.append(arg).append(" ");
					}
				}
				String joinedString = stringBuilder.toString().trim();
				plugin.getLogger().info(joinedString);
				Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
				example.executeCommand(joinedString);

				player.closeInventory();
			}


		}
	}

	void FlipAroundAdd(Inventory clickedInventory, int clickedSlot, int i){
		if (clickedSlot+i < clickedInventory.getSize()){
			if (clickedInventory.getItem(clickedSlot+i).isSimilar(UnConnected)){
				clickedInventory.setItem(clickedSlot+i, Connected);
			}else if (clickedInventory.getItem(clickedSlot+i).isSimilar(Connected)){
				clickedInventory.setItem(clickedSlot+i, UnConnected);
			}else if (clickedInventory.getItem(clickedSlot+i).isSimilar(Broken) && !clickedInventory.getItem(clickedSlot).isSimilar(Broken)){
				clickedInventory.setItem(clickedSlot+i, UnConnected);
				clickedInventory.setItem(clickedSlot, Broken);
			}else if (clickedInventory.getItem(clickedSlot+i).isSimilar(Power)){
				clickedInventory.setItem(clickedSlot+i, UnConnected);
			} else{
				return;
			}
		}
	}
	void FlipAroundSub(Inventory clickedInventory, int clickedSlot, int i){
		if (clickedSlot-i >= 0){
			if (clickedInventory.getItem(clickedSlot-i).isSimilar(UnConnected)){
				clickedInventory.setItem(clickedSlot-i, Connected);
			}else if (clickedInventory.getItem(clickedSlot-i).isSimilar(Connected)){
				clickedInventory.setItem(clickedSlot-i, UnConnected);
			}else if (clickedInventory.getItem(clickedSlot-i).isSimilar(Broken) && !clickedInventory.getItem(clickedSlot).isSimilar(Broken)){
				clickedInventory.setItem(clickedSlot-i, UnConnected);
				clickedInventory.setItem(clickedSlot, Broken);
			}else if (clickedInventory.getItem(clickedSlot-i).isSimilar(Power)){
				clickedInventory.setItem(clickedSlot-i, UnConnected);
			}else {
			return;
			}
		}
	}



}