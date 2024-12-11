package Mechanics.Skills;

import UtilObjects.Skills.PlayerLvL;
import UtilObjects.Skills.PlayerSkillTree;
import methods_plugins.AmonPackPlugin;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import static methods_plugins.AmonPackPlugin.ExecuteQuery;

public class JobsMenager {
    public static List<PlayerLvL> AllPlayersLvL = new ArrayList<>();
    public JobsMenager() throws SQLException {
        AddPlayerFromDBToListOnEnable();
    }
    public static void ShowPlayerData(Player p){
        PlayerLvL PLvL=GetPlayerLvL(p);
        if(PLvL!=null){
        p.sendMessage("Job1: "+PLvL.RealLvL(PLvL.getJob1LvL()));
        p.sendMessage("Job2: "+PLvL.RealLvL(PLvL.getJob2LvL()));
        p.sendMessage("Job3: "+PLvL.RealLvL(PLvL.getJob3LvL()));
        p.sendMessage("Job4: "+PLvL.RealLvL(PLvL.getJob4LvL()));
    }else{
            p.sendMessage("Player lvl is null");
        }
    }
    private static PlayerLvL GetPlayerLvL(Player p){
        for (PlayerLvL PLvL:AllPlayersLvL) {
            if(Objects.equals(PLvL.getPlayer(), p.getName())){
                return PLvL;
            }
        }
        return null;
    }
    public void AddPlayerFromDBToListOnEnable() throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from Jobs");
        if (rs.next()) {
            AllPlayersLvL.add(new PlayerLvL(
                    rs.getString(1),
                    rs.getInt(2),
                    rs.getInt(3),
                    rs.getInt(4),
                    rs.getInt(5)
            ));
        }
        stmt.close();
    }
    public static void AddPoints(String p, int i, int job) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from Jobs where Player='" + p + "'");
        if (!rs.next()) {
            ExecuteQuery("INSERT INTO Jobs (Player) VALUES ('" + p + "');");
            AllPlayersLvL.add(new PlayerLvL(p, 0, 0, 0, 0));
        }
            ExecuteQuery("UPDATE Jobs SET Job" + job + " = '" + i + "' WHERE Player = '" + p + "';");
            for (PlayerLvL PLvL:AllPlayersLvL) {
                if(Objects.equals(PLvL.getPlayer(), p)) {
                    switch (job){
                        case 1:
                            PLvL.setJob1LvL(i);
                            break;
                        case 2:
                            PLvL.setJob2LvL(i);
                            break;
                        case 3:
                            PLvL.setJob3LvL(i);
                            break;
                        case 4:
                            PLvL.setJob4LvL(i);
                            break;
                    }}}
            stmt.close();

    }
}
