package Mechanics.MMORPG;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class Friends implements CommandExecutor {
	
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
		if (sender instanceof Player) {
		Player player = (Player) sender;
		if (args.length < 1) {
		player.sendMessage("Błędne argumenty");
		return false;
		}else if (args[0].equalsIgnoreCase("list")){
		//Player p2 = Bukkit.getPlayerExact(args[1]);
	  	try {
	  	String frlist = "";
	  	for (int i = 2; i <= checkfriendsize(); i++) {
		PreparedStatement ps = AmonPackPlugin.mysqllite().getConnection().prepareStatement("SELECT Player"+i+" FROM FriendsTable WHERE Player1 = ?");
		ps.setString(1, player.getName());
		ResultSet rs = ps.executeQuery();
		String friendname = "";
		if (rs.next() == true) {
		friendname = rs.getString("Player"+i);
		}
		ps.close();
		frlist = frlist + i +": " +friendname+ " ";
		}
	  	player.sendMessage(frlist);
	  	} catch (SQLException e) {
		e.printStackTrace();
		}}else if (args[0].equalsIgnoreCase("add")){
		Player player2 = Bukkit.getPlayerExact(args[1]);
		if(player2 == null) {
		player.sendMessage("Ten gracz nie jest Online");
		}else {
	  	PreparedStatement ps;
		try {
		int i = 0;
		boolean loop = true;
		do {
		i++;
		Connection connect = AmonPackPlugin.mysqllite().getConnection();
		if (!AmonPackPlugin.mysqllite().columnExists("FriendsTable", "Player"+i)) {
			ps = connect.prepareStatement("ALTER TABLE FriendsTable ADD Player"+i+" varchar(50)");
			ps.executeUpdate();
			ps.close();
			PreparedStatement prestate;
			prestate = AmonPackPlugin.mysqllite().getConnection().prepareStatement("INSERT INTO FriendsTable (Player1,Player"+i+") VALUES (?,?)");
			prestate.setString(1, player.getName());
			prestate.setString(2, args[1]);
			prestate.executeUpdate();
			prestate.close();
			PreparedStatement ps2 = AmonPackPlugin.mysqllite().getConnection().prepareStatement("SELECT Player"+i+" FROM FriendsTable WHERE Player1 = ?");
			ps2.setString(1, player.getName());
			ResultSet rs2 = ps2.executeQuery();
			boolean rowExists = rs2.next();
			if (rowExists == false || rs2.getString("Player"+i) == null) {
			PreparedStatement ps3 = connect.prepareStatement("UPDATE FriendsTable SET Player"+i+" = ? WHERE Player1 = ?;");
			ps3.setString(1, args[1]);
			ps3.setString(2, player.getName());
			ps3.executeUpdate();
			ps3.close();
		    player.sendMessage("Dodałeś do( "+i+" ) znajomych gracza " + args[1]);
		    break;
		}}else if (AmonPackPlugin.mysqllite().columnExists("FriendsTable", "Player"+i)) {
		PreparedStatement prestate = AmonPackPlugin.mysqllite().getConnection().prepareStatement("SELECT 1 FROM FriendsTable WHERE Player1 = ?");
		prestate.setString(1, player.getName());
		ResultSet resset = prestate.executeQuery();
		boolean pexist = resset.next();
		if (pexist == false) {
		prestate = AmonPackPlugin.mysqllite().getConnection().prepareStatement("INSERT INTO FriendsTable (Player1,Player"+i+") VALUES (?,?)");
		prestate.setString(1, player.getName());
		prestate.setString(2, args[1]);
		prestate.executeUpdate();
		prestate.close();
	    player.sendMessage("Dodałeś do( "+i+" ) znajomych gracza " + args[1]);
	    break;
		}else {
		PreparedStatement ps2 = AmonPackPlugin.mysqllite().getConnection().prepareStatement("SELECT Player"+i+" FROM FriendsTable WHERE Player1 = ?");
		ps2.setString(1, player.getName());
		ResultSet rs2 = ps2.executeQuery();
		boolean rowExists = rs2.next();
		if (rowExists == false || rs2.getString("Player"+i) == null) {
		PreparedStatement ps3 = connect.prepareStatement("UPDATE FriendsTable SET Player"+i+" = ? WHERE Player1 = ?;");
		ps3.setString(1, args[1]);
		ps3.setString(2, player.getName());
		ps3.executeUpdate();
		ps3.close();
	    player.sendMessage("Dodałeś do( "+i+" ) znajomych gracza " + args[1]);
	    break;
		}
		}
		}}while(loop);
		} catch (SQLException e) {
		e.printStackTrace();
		}}
		}else if (args[0].equalsIgnoreCase("del")){
			for (int i = 2; i < checkfriendsize(); i++) {
				try {
					PreparedStatement ps = AmonPackPlugin.mysqllite().getConnection().prepareStatement("DELETE FROM FriendsTable WHERE Player"+i+" = ?");
					ps.setString(1, args[2]);
					ps.executeUpdate();
					ps.close();
					player.sendMessage("test");
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}}}return false;
	}
	
	public int checkfriendsize() {
		
	  	int columns = 0;
		boolean loop = true;
		do {
		if (!AmonPackPlugin.mysqllite().columnExists("FriendsTable", "Player"+(columns+1))) {
		break;
		}
		columns++;
		}while(loop);
		
		return columns;
		
	}
	
}