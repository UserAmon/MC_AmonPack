package Mechanics.PVE.Menagerie.Objectives;

import Mechanics.PVE.Menagerie.Menagerie;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Objectives {
    String ObjectiveName;
    List<String> NextObjectives;
    List<String> ReqObjectivesComplete;
    List<String> Display=new ArrayList<>();
    List<ObjectiveConditions> Conditions;
    List<ObjectiveEffect> Effects;
    boolean Used;
    boolean ItemDropBoolean = false;
    ItemStack ItemDrop;

    public Objectives(String objectiveName , List<String> nextObjectives,List<ObjectiveEffect> effects, List<ObjectiveConditions> conditions) {
        ObjectiveName = objectiveName;
        NextObjectives = nextObjectives;
        Conditions = conditions;
        if(effects!=null){
            Effects = effects;
        }
    }
    public Objectives(String objectiveName,List<ObjectiveEffect> effects, List<ObjectiveConditions> conditions) {
        ObjectiveName = objectiveName;
        NextObjectives = new ArrayList<>();
        Conditions = conditions;
        Effects = effects;
    }

    public List<String> Start(List<Player> p, Menagerie menagerie){
        if(Effects!=null && !Effects.isEmpty()){
            for (ObjectiveEffect E:Effects) {
                E.Start(p,menagerie);
            }
        }
        return NextObjectives;
    }


    public List<ObjectiveEffect> getEffects() {
        return Effects;
    }
    public String getObjectiveName() {
        return ObjectiveName;
    }
    public List<String> getNextObjectives() {
        return NextObjectives;
    }
    public List<ObjectiveConditions> getConditions() {
        return Conditions;
    }
    public boolean isUsed() {
        return Used;
    }
    public void setUsed(boolean used) {
        Used = used;
    }

    public List<String> getReqObjectivesComplete() {
        return ReqObjectivesComplete;
    }

    public void setReqObjectivesComplete(List<String> reqObjectivesComplete) {
        ReqObjectivesComplete = reqObjectivesComplete;
    }

    public List<String> getDisplay() {
        return Display;
    }

    public void setDisplay(List<String>display) {
        Display.addAll(display);
    }

    public boolean isItemDropBoolean() {
        return ItemDropBoolean;
    }

    public void setItemDropBoolean(boolean itemDropBoolean) {
        ItemDropBoolean = itemDropBoolean;
    }

    public ItemStack getItemDrop() {
        return ItemDrop;
    }

    public void setItemDrop(ItemStack itemDrop) {
        ItemDrop = itemDrop;
    }
}
