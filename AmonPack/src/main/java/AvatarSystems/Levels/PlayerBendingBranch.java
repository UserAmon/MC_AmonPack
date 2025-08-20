package AvatarSystems.Levels;

import com.projectkorra.projectkorra.Element;
import methods_plugins.AmonPackPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static methods_plugins.AmonPackPlugin.ExecuteQuery;

public class PlayerBendingBranch {
    String Name;
    int WaterPoints;
    int EarthPoints;
    int AirPoints;
    int FirePoints;
    List<Element> ElementsInPossesion;
    List<String> UnlockedAbilities;
    List<String> TemporaryAbilities;
    Element CurrentElement;
    int CurrentPage;


    public PlayerBendingBranch(int airPoints, Element currentElement, int earthPoints, List<Element> elementsInPossesion, int firePoints, String name, List<String> unlockedAbilities, int waterPoints) {
        AirPoints = airPoints;
        CurrentElement = currentElement;
        EarthPoints = earthPoints;
        ElementsInPossesion = elementsInPossesion;
        FirePoints = firePoints;
        Name = name;
        UnlockedAbilities = new ArrayList<>(unlockedAbilities);
        TemporaryAbilities=new ArrayList<>();
        WaterPoints = waterPoints;
    }
    public void SetCurrentElement(Element element){
        CurrentElement=element;
        try {
            SaveInDatabaes();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void ClearAbilities(ElementTree elementtree){
        int totalcost = 0;
        List<String> AbilitiesToRemove=new ArrayList<>();
            for (String st:UnlockedAbilities) {
                int temp_cost = elementtree.GetCostByAbilityName(st);
                if(temp_cost>=0){
                    totalcost = totalcost+temp_cost;
                    AbilitiesToRemove.add(st);
                }
            }
        UnlockedAbilities.removeAll(AbilitiesToRemove);
        AddPoints(CurrentElement,totalcost);
        try {
            SaveInDatabaes();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void AddAllPoints(int amount){
        AirPoints+=amount;
        WaterPoints+=amount;
        EarthPoints+=amount;
        FirePoints+=amount;
        try {
            SaveInDatabaes();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void AddPoints(Element element,int amount){
        if(element.equals(Element.AIR))AirPoints+=amount;
        if(element.equals(Element.WATER))WaterPoints+=amount;
        if(element.equals(Element.FIRE))FirePoints+=amount;
        if(element.equals(Element.EARTH))EarthPoints+=amount;
        try {
            SaveInDatabaes();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void UnlockAbility(Element element,int amount, String AbilityName){
        if(element.equals(Element.AIR))AirPoints-=amount;
        if(element.equals(Element.WATER))WaterPoints-=amount;
        if(element.equals(Element.FIRE))FirePoints-=amount;
        if(element.equals(Element.EARTH))EarthPoints-=amount;
        try {
            UnlockedAbilities.add(AbilityName);
        } catch (Exception e) {
            System.out.println("blad :<  "+e);
        }
        try {
            SaveInDatabaes();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void SaveInDatabaes() throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from BendingTree WHERE Player='" + Name+"'");
        String elementsText = ElementsInPossesion.stream().map(Element::getName).collect(Collectors.joining(","));
        String abilitiesText = String.join(",", UnlockedAbilities);
        if (rs.next()) {
            String st = "UPDATE BendingTree SET" +
                    " AirPoints = '" + AirPoints + "'," +
                    " FirePoints = '" + FirePoints + "'," +
                    " WaterPoints = '" + WaterPoints + "'," +
                    " EarthPoints = '" + EarthPoints + "'," +
                    " CurrentElement = '" + CurrentElement.getName().toString() + "'," +
                    " AllElements = '" + elementsText + "'," +
                    " UnlockedAbilities = '" + abilitiesText + "'" +
                    " WHERE Player = '"+ Name+"';";
            ExecuteQuery(st);
        }else {
            String st = "INSERT INTO BendingTree (Player, AirPoints, FirePoints, WaterPoints, EarthPoints, CurrentElement, AllElements, UnlockedAbilities) VALUES (" +
                    "'" + Name + "', " +
                    "'" + AirPoints + "', " +
                    "'" + FirePoints + "', " +
                    "'" + WaterPoints + "', " +
                    "'" + EarthPoints + "', " +
                    "'" + CurrentElement.getName().toString() + "', " +
                    "'" + elementsText + "', " +
                    "'" + abilitiesText + "');";
            ExecuteQuery(st);
        }
        stmt.close();
    }
    public int getAirPoints() {
        return AirPoints;
    }
    public Element getCurrentElement() {
        return CurrentElement;
    }
    public int getEarthPoints() {
        return EarthPoints;
    }
    public List<Element> getElementsInPossesion() {
        return ElementsInPossesion;
    }
    public int getFirePoints() {
        return FirePoints;
    }
    public String getName() {
        return Name;
    }
    public List<String> getTemporaryAbilities() {
        return TemporaryAbilities;
    }
    public List<String> getUnlockedAbilities() {
        return UnlockedAbilities;
    }
    public int getWaterPoints() {
        return WaterPoints;
    }
    public int getCurrentPage() {
        return CurrentPage;
    }
    public void setCurrentPage(int currentPage) {
        CurrentPage = currentPage;
    }
    public int GetPoints(Element element){
        if(element.equals(Element.AIR))return AirPoints;
        if(element.equals(Element.WATER))return WaterPoints;
        if(element.equals(Element.FIRE))return FirePoints;
        if(element.equals(Element.EARTH))return EarthPoints;
        return 0;
    }
}
