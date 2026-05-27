package RPG.Levels.BendingTree;

import com.projectkorra.projectkorra.Element;
import Plugin.AmonPackPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static Plugin.AmonPackPlugin.ExecuteQuery;

public class PlayerBendingBranch {
    String Name;
    int WaterPoints;
    int EarthPoints;
    int AirPoints;
    int FirePoints;
    List<Element> ElementsInPossesion;
    List<String> UnlockedAbilities;
    List<String> TemporaryAbilities;
    List<Element> TemporaryElements;
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
        TemporaryElements=new ArrayList<>();
        WaterPoints = waterPoints;

        unlockDefaultAbilities();
    }

    public void unlockDefaultAbilities() {
        if (ElementsInPossesion == null || AmonPackPlugin.levelsBending == null) {
            return;
        }
        boolean changed = false;
        for (Element element : ElementsInPossesion) {
            ElementTree tree = AmonPackPlugin.levelsBending.GetElement(element);
            if (tree != null) {
                for (SkillTree_Ability ability : tree.getAbilities()) {
                    if (ability.isdef() && !UnlockedAbilities.contains(ability.getName())) {
                        UnlockedAbilities.add(ability.getName());
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            try {
                SaveInDatabaes();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
                // cost > 0 only – free (cost == 0) abilities are kept even after reset
                if(temp_cost > 0){
                    totalcost = totalcost+temp_cost;
                    AbilitiesToRemove.add(st);
                }
            }
        UnlockedAbilities.removeAll(AbilitiesToRemove);
        // Only refund points if we have a current element context
        if (CurrentElement != null && totalcost > 0) {
            AddPoints(CurrentElement, totalcost);
        } else {
            try {
                SaveInDatabaes();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
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
        String currentElementName = (CurrentElement != null) ? CurrentElement.getName() : "";
        if (rs.next()) {
            String st = "UPDATE BendingTree SET" +
                    " AirPoints = '" + AirPoints + "'," +
                    " FirePoints = '" + FirePoints + "'," +
                    " WaterPoints = '" + WaterPoints + "'," +
                    " EarthPoints = '" + EarthPoints + "'," +
                    " CurrentElement = '" + currentElementName + "'," +
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
                    "'" + currentElementName + "', " +
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
    public List<Element> getTemporaryElements() {
        if (TemporaryElements == null) {
            TemporaryElements = new ArrayList<>();
        }
        return TemporaryElements;
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
    public boolean hasUpgrade(String upgradeName) {
        return UnlockedAbilities.contains(upgradeName);
    }
}
