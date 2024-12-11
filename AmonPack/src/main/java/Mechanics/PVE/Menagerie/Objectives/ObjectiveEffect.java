package Mechanics.PVE.Menagerie.Objectives;

import Mechanics.PVE.Menagerie.Menagerie;
import commands.Commands;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ObjectiveEffect {
    Location EffectLocation;
    String Message;
    List<Enemy> Enemies;
    EffectType EType;
    Menagerie.Doors Doors;
    boolean SetDoorsAir;
    ItemStack GiveItem;
    List<String>ReqObjToEnd=new ArrayList<>();
    int Intervals;
    boolean executed;

    public ObjectiveEffect(String message) {
        Message = message;
        EType=EffectType.MESSAGE;
    }
    public ObjectiveEffect(String message,boolean iscommand) {
        Message = message;
        EType=EffectType.COMMAND;
    }

    public ObjectiveEffect(ItemStack giveItem) {
        GiveItem = giveItem;
        EType=EffectType.GIVEITEM;
    }

    public ObjectiveEffect(List<Enemy> EnemiesToSpawn) {
        Enemies=EnemiesToSpawn;
        EType = EffectType.SPAWNENEMIES;
    }

    public ObjectiveEffect(Menagerie.Doors doors, boolean setDoorsAir) {
        Doors = doors;
        SetDoorsAir = setDoorsAir;
        EType=EffectType.BLOCKS;

    }

    public ObjectiveEffect(Location effectLocation) {
        EffectLocation = effectLocation;
        EType=EffectType.TELEPORTALLPLAYERS;


    }
    public ObjectiveEffect(){
        EType=EffectType.ENCOUNTER;
    }

    public void Start(List<Player> p,Menagerie menagerie){
        executed=true;
        if(!ReqObjToEnd.isEmpty()&&Intervals>0){
                BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(!executed){
                            cancel();
                        }else{
                        int isObjectiveChecked = 0;
                        for (String reqObjective : ReqObjToEnd) {
                            for (Objectives objective : menagerie.getActiveEncounter().getAllObjectivesList()) {
                                if (objective.getObjectiveName().equals(reqObjective) && objective.isUsed()) {
                                    isObjectiveChecked++;
                                }}}
                        if(isObjectiveChecked<ReqObjToEnd.size()){
                            Execute(p,menagerie);
                        }else{
                            cancel();
                        }
                        }}};
                runnable.runTaskTimer(AmonPackPlugin.plugin, 0L, Intervals* 20L);
        }else{
            Execute(p,menagerie);
        }
    }

    private void Execute(List<Player>p,Menagerie menagerie){
        switch (EType){
            case TELEPORTALLPLAYERS:
                for (Player pl:p) {
                    pl.teleport(EffectLocation);
                }
                break;
            case GIVEITEM:
                for (Player pl:p) {
                    pl.getInventory().addItem(GiveItem);
                }
                break;
            case MESSAGE:
                for (Player pl:p) {
                    pl.sendMessage(Message);
                }
                break;
            case COMMAND:
                Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                for (Player pl:p) {
                    example.executeCommand(Message.replace("%player%",pl.getName()));
                }
                break;
            case SPAWNENEMIES:
                for (Enemy E:Enemies) {
                    if(E.getMaxSpawned()>0){
                        if(E.getMaxSpawned()>menagerie.GetEnemiesInDung().size()){
                            E.Spawn();
                        }}else {
                        E.Spawn();
                    }
                }
                break;
            case BLOCKS:
                Doors.ChangeFirst(SetDoorsAir);
                break;
            case ENCOUNTER:
                menagerie.NextRandomEncouter();
                if(!menagerie.getActiveEncounter().isLast()){
                    menagerie.MenagerieRest(p);
                }
                break;
        }
    }

    public enum EffectType {
        SPAWNENEMIES,
        TELEPORT,
        TELEPORTALLPLAYERS,
        COMMAND,
        MESSAGE,
        BLOCKS,
        GIVEITEM,
        GIVEPOTIONEFFECT,
        ENCOUNTER,
    }

    public Location getEffectLocation() {
        return EffectLocation;
    }
    public Menagerie.Doors getDoors() {
        return Doors;
    }
    public boolean isSetDoorsAir() {
        return SetDoorsAir;
    }
    public EffectType getEType() {
        return EType;
    }

    public void setReqObjToEnd(List<String> reqObjToEnd) {
        ReqObjToEnd = reqObjToEnd;
        System.out.println("Ustawiono!!! 1   "+ReqObjToEnd.size());
        System.out.println("Ustawiono!!! 2   "+ReqObjToEnd.get(0));
    }

    public void setIntervals(int intervals) {
        Intervals = intervals;
        System.out.println("Ustawiono!!! 3   "+Intervals);
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }
}
