package UtilObjects.Skills;

import Mechanics.Skills.BendingGuiMenu;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.board.BendingBoardManager;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static Mechanics.Skills.BendingGuiMenu.AllPlayersSkillTrees;
import static methods_plugins.AmonPackPlugin.ExecuteQuery;
import static Mechanics.Skills.BendingGuiMenu.SubElementByElement;

public class PlayerSkillTree {
    String Player;
    int ActSkillPoints;
    List<String> SelectedPath;
    int currentPage;
    String CurrentElement;
    List<String> ElementsInPossesion;
    boolean Multibend;

    public PlayerSkillTree(String player, int actSkillPoints, String Path, String ElePath, String Element) {
        Player = player;
        ActSkillPoints = actSkillPoints;
        if (Path != null){
            SelectedPath = Arrays.asList(Path.split(","));
        }
        if (ElePath != null){
            ElementsInPossesion = Arrays.asList(ElePath.split(","));
        }
        CurrentElement = Element;
        Multibend = false;
    }




/*
    public int CountCost(List<STAbility> costlist){
        int totalcost = 0;
        if (SelectedPath != null){
        for (String st:SelectedPath) {
            for (STAbility STA:costlist) {
                if (st.equalsIgnoreCase(STA.getName())){
                    totalcost = totalcost+STA.getCost();
                    break;
                }}}}
        return totalcost;
    }*/
    public int CountCostByElement(List<SkillTree_Ability> costlist,Element ele){
        int totalcost = 0;
        if (SelectedPath != null){
            for (String st:SelectedPath) {
                for (SkillTree_Ability STA:costlist) {
                    if (st.equalsIgnoreCase(STA.getName())){
                        if (SubElementByElement(ele,CoreAbility.getAbility(STA.getName()))){
                            totalcost = totalcost+STA.getCost();
                            break;
                        }
                    }}}}
        return totalcost;
    }

    public String PathRemoveElement(Element Element){
        List<String> TempStringList = new ArrayList<>();
        for (String st:SelectedPath) {
            if (CoreAbility.getAbility(st) != null){
                if (!SubElementByElement(Element,CoreAbility.getAbility(st))) {
                    TempStringList.add(st);
                }}}
        return String.join(",", TempStringList);
    }

    /*public static int ActPointsDB(String player) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select SkillPoint from SpellTree where Player='" + player+"'");
        int AP = rs.getInt(1);
        stmt.close();
        return AP;
    }
    public static String ActPathDB(String player) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select Path from SpellTree where Player='" + player+"'");
        String AP = rs.getString(1);
        stmt.close();
        return AP;
    }*/

    public void AddElement(PlayerSkillTree PST, String ele) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree where Player='" + PST.getPlayer()+"'");
        if (!rs.next()) {
            System.out.println("Ten gracz nie ma stworzonego Drzewka Skilli");
        } else {
            for (PlayerSkillTree STO:AllPlayersSkillTrees) {
                if (STO.getPlayer().equalsIgnoreCase(PST.getPlayer())){
                    List<String> newele = PST.getElementsInPossesion();
                    newele.add(ele);
                    STO.setElementsInPossesion(newele);
                    break;
                }}
            ExecuteQuery("UPDATE SpellTree SET AllElements = '" + PST.getElementsInPossesionAsString() + "' WHERE Player = '"+ PST.getPlayer()+"';");
        }
        stmt.close();
    }


    public void AddPoint(PlayerSkillTree PST, int points) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree where Player='" + PST.getPlayer()+"'");
        if (!rs.next()) {
            System.out.println("Ten gracz nie ma stworzonego Drzewka Skilli");
        } else {
            for (PlayerSkillTree STO:AllPlayersSkillTrees) {
                if (STO.getPlayer().equalsIgnoreCase(PST.getPlayer())){
                    int newpoints=STO.getActSkillPoints()+points;
                    STO.setActSkillPoints(newpoints);
                    ExecuteQuery("UPDATE SpellTree SET SkillPoint = '" + newpoints + "' WHERE Player = '"+ PST.getPlayer()+"';");
                    break;
                }}
        }
        stmt.close();
    }


    public static void ResetSkillTree(PlayerSkillTree Sto) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree where Player='" + Sto.getPlayer()+"'");
        if (!rs.next()) {
            ExecuteQuery("INSERT INTO SpellTree (Player,SkillPoint,Path,Element,AllElements) VALUES ('" + Sto.getPlayer() +"',"+ Sto.getActSkillPoints()+",'"+ Sto.getSelectedPathAsString() +"','"+Sto.getCurrentElement()+"','"+Sto.getElementsInPossesionAsString()+",');");
            AllPlayersSkillTrees.add(Sto);
        } else {
            for (PlayerSkillTree STO:AllPlayersSkillTrees) {
                if (STO.getPlayer().equalsIgnoreCase(Sto.getPlayer())){
                    STO.setSelectedPath(Sto.getSelectedPath());
                    STO.setActSkillPoints(Sto.getActSkillPoints());
                    STO.setCurrentElement(Sto.getCurrentElement());
                    STO.setElementsInPossesion(Sto.getElementsInPossesion());
                    break;
                }}
            ExecuteQuery("UPDATE SpellTree SET Path = '" + Sto.getSelectedPathAsString() + "' WHERE Player = '"+ Sto.getPlayer()+"';");
            ExecuteQuery("UPDATE SpellTree SET SkillPoint = '" + Sto.getActSkillPoints() + "' WHERE Player = '"+ Sto.getPlayer()+"';");
            ExecuteQuery("UPDATE SpellTree SET Element = '" + Sto.getCurrentElement() + "' WHERE Player = '"+ Sto.getPlayer()+"';");
            ExecuteQuery("UPDATE SpellTree SET AllElements = '" + Sto.getElementsInPossesionAsString() + "' WHERE Player = '"+ Sto.getPlayer()+"';");
        }
        org.bukkit.entity.Player p = Bukkit.getPlayer(Sto.getPlayer());
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(Sto.getPlayer());
        Element ele = Element.getElement(Sto.getCurrentElement());
        for (int i = 0; i <= 9; i++) {
            if(CoreAbility.getAbility(bPlayer.getAbilities().get(i))!=null&&(CoreAbility.getAbility(bPlayer.getAbilities().get(i)).getElement().equals(ele)
                    || Objects.equals(BendingGuiMenu.ElementBasedOnSubElement(CoreAbility.getAbility(bPlayer.getAbilities().get(i)).getElement()), ele))){
                BendingBoardManager.getBoard(p).get().clearSlot(i);
                bPlayer.getAbilities().remove(i);
            }}
        bPlayer.removeUnusableAbilities();
        stmt.close();
    }


    public static void SetPathAndPoints(String p, int points, String path, String ele) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree where Player='" + p+"'");
        if (!rs.next()) {
            ExecuteQuery("INSERT INTO SpellTree (Player,SkillPoint,Path,Element,AllElements) VALUES ('" + p +"',"+ points+",'"+ path +"','"+ele+"','"+ele+",');");
            AllPlayersSkillTrees.add(new PlayerSkillTree(p,points,path,ele,ele));
        } else {
            PlayerSkillTree sto = null;
            for (PlayerSkillTree STO:AllPlayersSkillTrees) {
                if (STO.getPlayer().equalsIgnoreCase(p)){
                    sto = STO;
                    STO.setSelectedPath(Arrays.asList(path.split(",")));
                    STO.setActSkillPoints(points);
                    STO.setCurrentElement(ele);
                    System.out.println("test bledu4   "+STO.getElementsInPossesion());
                    STO.setElementsInPossesion(Arrays.asList((STO.getElementsInPossesionAsString()).split(",")));
                    break;
                }}
            ExecuteQuery("UPDATE SpellTree SET Path = '" + path + "' WHERE Player = '"+ p+"';");
            ExecuteQuery("UPDATE SpellTree SET SkillPoint = '" + points + "' WHERE Player = '"+ p+"';");
            ExecuteQuery("UPDATE SpellTree SET Element = '" + ele + "' WHERE Player = '"+ p+"';");
            ExecuteQuery("UPDATE SpellTree SET AllElements = '" + sto.getElementsInPossesionAsString() + "' WHERE Player = '"+ p+"';");
        }
        stmt.close();
    }
/*
    public static void AddPath(String p, String path) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree where Player='" + p+"'");
        if (!rs.next()) {
            ExecuteQuery("INSERT INTO SpellTree (Player,Path) VALUES ('" + p +"',"+ path +");");
            for (SkillTreeObj STO:SkillPoints) {
                if (STO.getPlayer().equalsIgnoreCase(p)){
                    SkillPoints.add(new SkillTreeObj(p,STO.getActSkillPoints(),path,STO.getCurrentElement()));
                }}
        } else {
            String TempExeString = (rs.getString(3)+","+path);
            String[] parts = TempExeString.split(",");
            Set<String> uniqueParts = new LinkedHashSet<>(Arrays.asList(parts));
            String ExeString = uniqueParts.stream().collect(Collectors.joining(","));
            System.out.println("UPDATE SpellTree SET Path = '" + ExeString + "' WHERE Player = '"+ p+"';");
            ExecuteQuery("UPDATE SpellTree SET Path = '" + ExeString + "' WHERE Player = '"+ p+"';");
            for (SkillTreeObj STO:SkillPoints) {
                if (STO.getPlayer().equalsIgnoreCase(p)){
                    STO.setSelectedPath(Arrays.asList((rs.getString(3)+","+path).split(",")));
                }}}
        stmt.close();
    }*/

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public String getPlayer() {
        return Player;
    }
    public void setPlayer(String player) {
        Player = player;
    }
    public int getActSkillPoints() {
        return ActSkillPoints;
    }
    public void setActSkillPoints(int actSkillPoints) {
        ActSkillPoints = actSkillPoints;
    }

    public String getCurrentElement() {
        return CurrentElement;
    }

    public void setCurrentElement(String currentElement) {
        CurrentElement = currentElement;
    }

    public List<String> getSelectedPath() {
        if (SelectedPath != null){
            List<String> TempSelPath = new ArrayList<>();
            for (String st:SelectedPath) {
                st.replace(" ","");
                if (!TempSelPath.contains(st)){
                    TempSelPath.add(st);
                }}
            SelectedPath = TempSelPath;
        }
        return SelectedPath;
    }
    public String getSelectedPathAsString() {
        return this.getSelectedPath().stream().collect(Collectors.joining(","));
    }

    public List<String> getElementsInPossesion() {
        if (ElementsInPossesion != null){
            List<String> TempSelPath = new ArrayList<>();
            for (String st:ElementsInPossesion) {
                st.replace(" ","");
                if (!TempSelPath.contains(st)){
                    TempSelPath.add(st);
                }}
            ElementsInPossesion = TempSelPath;
        }
        return ElementsInPossesion;
    }
    public String getElementsInPossesionAsString() {
        return this.getElementsInPossesion().stream().collect(Collectors.joining(","));
    }

    public void setSelectedPath(List<String> selectedPath) {
        SelectedPath = selectedPath;
    }
    public void setElementsInPossesion(List<String> selectedPath) {
        ElementsInPossesion = selectedPath;
    }

    public void setMultibend(boolean multibend) {
        Multibend = multibend;
    }

    public boolean isMultibend() {
        return Multibend;
    }
}
