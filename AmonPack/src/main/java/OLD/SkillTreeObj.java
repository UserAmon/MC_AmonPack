package OLD;
/*
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import General.AmonPackPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static OLD.PGrowth.*;
import static General.AmonPackPlugin.ExecuteQuery;

public class SkillTreeObj {
    String Player;
    int ActSkillPoints;
    List<String> SelectedPath;
    int currentPage;
    String CurrentElement;
    List<String> ElementsInPossesion;

    public SkillTreeObj(String player, int actSkillPoints, String Path, String ElePath, String Element) {
        Player = player;
        ActSkillPoints = actSkillPoints;
        if (Path != null){
            SelectedPath = Arrays.asList(Path.split(","));
        }
        if (ElePath != null){
            ElementsInPossesion = Arrays.asList(ElePath.split(","));
        }
        CurrentElement = Element;
    }

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
    }
    public int CountCostByElement(List<STAbility> costlist,Element ele){
        int totalcost = 0;
        if (SelectedPath != null){
            for (String st:SelectedPath) {
                for (STAbility STA:costlist) {
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
    }
    public static void AddPoints(String p, int i) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree where Player='" + p+"'");
        if (!rs.next()) {
            ExecuteQuery("INSERT INTO SpellTree (Player,SkillPoint) VALUES ('" + p +"',"+ i +");");
            SkillPoints.add(new SkillTreeObj(p,i,"","",""));
        } else {
            ExecuteQuery("UPDATE SpellTree SET SkillPoint = '" + i + "' WHERE Player = '"+ p+"';");
            for (SkillTreeObj STO:SkillPoints) {
                if (STO.getPlayer().equalsIgnoreCase(p)){
                    STO.setActSkillPoints(i);
                }}}
        stmt.close();
    }

    public static void AddElement(String p, String ele) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree where Player='" + p+"'");
        if (!rs.next()) {
            ExecuteQuery("INSERT INTO SpellTree (Player,AllElements) VALUES ('" + p +"',"+ ele +");");
            SkillPoints.add(new SkillTreeObj(p,0,"",ele,ele));
        } else {
            for (SkillTreeObj STO:SkillPoints) {
                if (STO.getPlayer().equalsIgnoreCase(p)){
                    STO.setElementsInPossesion(Arrays.asList((STO.getElementsInPossesionAsString()+","+ele).split(",")));
                    ExecuteQuery("UPDATE SpellTree SET AllElements = '" + STO.getElementsInPossesionAsString()+","+ele + "' WHERE Player = '"+ p+"';");
                }}
        }
        stmt.close();
    }

    public static void SetPathAndPoints(String p, int points, String path, String ele) throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree where Player='" + p+"'");
        if (!rs.next()) {
            ExecuteQuery("INSERT INTO SpellTree (Player,SkillPoint,Path,Element) VALUES ('" + p +"',"+ points+",'"+ path +"','"+ele+"');");
                    SkillPoints.add(new SkillTreeObj(p,points,path,ele,""));
        } else {
            SkillTreeObj sto = null;
            for (SkillTreeObj STO:SkillPoints) {
                if (STO.getPlayer().equalsIgnoreCase(p)){
                    sto = STO;
                    STO.setSelectedPath(Arrays.asList(path.split(",")));
                    STO.setActSkillPoints(points);
                    STO.setCurrentElement(ele);
                    //STO.setElementsInPossesion(Arrays.asList((STO.getElementsInPossesionAsString()+","+ElementsList).split(",")));
                    STO.setElementsInPossesion(Arrays.asList((STO.getElementsInPossesionAsString()).split(",")));
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
    }

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
}
*/