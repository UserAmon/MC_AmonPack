package RPG.Levels.BendingTree;

import com.projectkorra.projectkorra.Element;

import java.util.List;

public class SkillTree_Ability {
    Element element;
    String name;
    int cost;
    List<String> ListOfPreAbility;
    int place;
    boolean def;
    boolean isPassiveUpgrade = false;
    boolean isSkillUpgrade = false;
    String skillName = "";
    boolean isSpecialBindAbility = false;
    List<String> lockAbilities = new java.util.ArrayList<>();

    public SkillTree_Ability(Element element, String name, int cost, List<String> listOfPreAbility, int place, boolean aDefault) {
        this.element = element;
        this.name = name;
        this.cost = cost;
        ListOfPreAbility = listOfPreAbility != null ? listOfPreAbility : new java.util.ArrayList<>();
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

    public boolean isUpgrade() {
        return isPassiveUpgrade || isSkillUpgrade;
    }

    public void setUpgrade(boolean upgrade) {
        this.isPassiveUpgrade = upgrade;
    }

    public boolean isPassiveUpgrade() {
        return isPassiveUpgrade;
    }

    public void setPassiveUpgrade(boolean passiveUpgrade) {
        this.isPassiveUpgrade = passiveUpgrade;
    }

    public boolean isSkillUpgrade() {
        return isSkillUpgrade;
    }

    public void setSkillUpgrade(boolean skillUpgrade) {
        this.isSkillUpgrade = skillUpgrade;
    }

    public String getSkillName() {
        return skillName != null ? skillName : "";
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName != null ? skillName : "";
    }

    public boolean isSpecialBindAbility() {
        return isSpecialBindAbility;
    }

    public void setSpecialBindAbility(boolean specialBindAbility) {
        this.isSpecialBindAbility = specialBindAbility;
    }

    public List<String> getLockAbilities() {
        return lockAbilities != null ? lockAbilities : new java.util.ArrayList<>();
    }

    public void setLockAbilities(List<String> lockAbilities) {
        this.lockAbilities = lockAbilities != null ? lockAbilities : new java.util.ArrayList<>();
    }
}
