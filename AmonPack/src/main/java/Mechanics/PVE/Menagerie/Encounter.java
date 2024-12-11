package Mechanics.PVE.Menagerie;

import Mechanics.PVE.Menagerie.Objectives.ObjectiveConditions;
import Mechanics.PVE.Menagerie.Objectives.ObjectiveEffect;
import Mechanics.PVE.Menagerie.Objectives.Objectives;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class Encounter {
    String Name;
    private final List<Objectives> AllObjectivesList;
    private final List<Objectives> ActiveObjectivesList = new ArrayList<>();
    private final Location StartTeleportLocation;
    private final Menagerie.Doors ReadyWaitingDoors;
    private final List<String> FirstObjectiveTitle;
    private boolean IsDone;
    private final boolean IsLast;

    public Encounter(String n, List<Objectives> allObjectivesList, Location startTeleportLocation, Menagerie.Doors readyWaitingDoors, boolean isLast) {
        AllObjectivesList = allObjectivesList;
        StartTeleportLocation = startTeleportLocation;
        ReadyWaitingDoors = readyWaitingDoors;
        IsLast = isLast;
        FirstObjectiveTitle=AllObjectivesList.get(0).getDisplay();
        ActiveObjectivesList.add(AllObjectivesList.get(0));
        IsDone=false;
        Name=n;
    }

    public boolean isDone() {
        return IsDone;
    }
    public boolean isLast() {
        return IsLast;
    }
    public void setDone(boolean done) {
        IsDone = done;
    }
    public List<String> getFirstObjectiveTitle() {
        return FirstObjectiveTitle;
    }
    public String getName() {
        return Name;
    }
    public List<Objectives> getAllObjectivesList() {
        return AllObjectivesList;
    }
    public List<Objectives> getActiveObjectivesList() {
        return ActiveObjectivesList;
    }
    public Location getStartTeleportLocation() {
        return StartTeleportLocation;
    }
    public Menagerie.Doors getReadyWaitingDoors() {
        return ReadyWaitingDoors;
    }
    public void ResetObjectives(){
        ActiveObjectivesList.clear();
        ActiveObjectivesList.add(AllObjectivesList.get(0));
        IsDone=false;
        for (Objectives obj : AllObjectivesList){
            obj.setUsed(false);
            if(obj.getEffects()!=null){
                for (ObjectiveEffect effect:obj.getEffects()) {
                    effect.setExecuted(false);
                    if(effect.getEType().equals(ObjectiveEffect.EffectType.BLOCKS)){
                        effect.getDoors().ChangeFirst(!effect.isSetDoorsAir());
                    }}}
            if(obj.getConditions()!=null){
                for (ObjectiveConditions con:obj.getConditions()) {
                    con.setChecked(false);
                }}
        }
        }
}
