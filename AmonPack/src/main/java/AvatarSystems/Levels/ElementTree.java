package AvatarSystems.Levels;

import UtilObjects.Skills.SkillTree_Ability;
import com.projectkorra.projectkorra.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ElementTree {
    Element element;
    int rows;
    List<SkillTree_Ability> Abilities = new ArrayList<>();
    List<Integer> PathDecoration = new ArrayList<>();

    public ElementTree(List<SkillTree_Ability> abilities, Element element, List<Integer> pathDecoration, int rows) {
        Abilities = abilities;
        this.element = element;
        PathDecoration = pathDecoration;
        this.rows = rows;
    }
    public int GetCostByAbilityName(String name){
        return Abilities.stream().filter(skillTreeAbility -> skillTreeAbility.getName().equalsIgnoreCase(name)).findFirst().orElse(null).getCost();
    }
    public List<SkillTree_Ability> getAbilities() {
        return Abilities;
    }
    public void setAbilities(List<SkillTree_Ability> abilities) {
        Abilities = abilities;
    }
    public Element getElement() {
        return element;
    }
    public void setElement(Element element) {
        this.element = element;
    }
    public List<Integer> getPathDecoration() {
        return PathDecoration;
    }
    public void setPathDecoration(List<Integer> pathDecoration) {
        PathDecoration = pathDecoration;
    }
    public int getRows() {
        return rows;
    }
    public void setRows(int rows) {
        this.rows = rows;
    }
}
