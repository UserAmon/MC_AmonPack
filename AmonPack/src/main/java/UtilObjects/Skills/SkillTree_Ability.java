package UtilObjects.Skills;

import com.projectkorra.projectkorra.Element;

import java.util.List;

public class SkillTree_Ability {
    Element element;
    String name;
    int cost;
    List<String> ListOfPreAbility;
    int place;
    boolean def;

    public SkillTree_Ability(Element element, String name, int cost, List<String> listOfPreAbility, int place, boolean aDefault) {
        this.element = element;
        this.name = name;
        this.cost = cost;
        ListOfPreAbility = listOfPreAbility;
        this.place = place;
        def = aDefault;
    }


    public Element getElement() {
        return element;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public List<String> getListOfPreAbility() {
        return ListOfPreAbility;
    }

    public int getPlace() {
        return place;
    }

    public boolean isdef() {
        return def;
    }
}
