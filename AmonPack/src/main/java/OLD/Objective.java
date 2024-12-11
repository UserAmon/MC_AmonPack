package OLD;
/*
import OLD.Assault.AssaultMethods;
import commands.Commands;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Objective {
    public enum ObjectiveType {
        Keys, Test2, Test3;
    }
    private List<String> EnemyMobTypes;
    private double MinMobs;
    private int MobMaxLevel;
    private List<Location> EnemyLocList;
    private List<AssaultMethods.Doors> ListOfDoors;
    private ObjectiveType type;
    private String Message;
    private String Name;
    //Keys
    private int KeysToDeposit;
    private int DepositedKeys;

    public Objective(List<String> enemyMobTypes, double minMobs, int mobMaxLevel, List<Location> enemyLocList, List<AssaultMethods.Doors> listOfDoors, ObjectiveType type, String message, String name) {
        EnemyMobTypes = enemyMobTypes;
        MinMobs = minMobs;
        MobMaxLevel = mobMaxLevel;
        EnemyLocList = enemyLocList;
        ListOfDoors = listOfDoors;
        this.type = type;
        Message = message;
        Name = name;
    }
    //Keys
    public Objective(List<String> enemyMobTypes, double minMobs, int mobMaxLevel, List<Location> enemyLocList, List<AssaultMethods.Doors> listOfDoors, ObjectiveType type, String message, String name, int keysToDeposit) {
        EnemyMobTypes = enemyMobTypes;
        MinMobs = minMobs;
        MobMaxLevel = mobMaxLevel;
        EnemyLocList = enemyLocList;
        ListOfDoors = listOfDoors;
        this.type = type;
        Message = message;
        Name = name;
        KeysToDeposit = keysToDeposit;
    }

    public void SpawnMob() {
        Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
        List<Location> remainingLocations = new ArrayList<>(EnemyLocList);
        for (int ml = 0; ml < EnemyLocList.size(); ml++) {
                int randomIndex = new Random().nextInt(remainingLocations.size());
                Location loc = remainingLocations.remove(randomIndex);
                    for (int i = 0; i < MinMobs; i++) {
                        int RandomLvl = (new Random().nextInt(MobMaxLevel))+1;
                        int offsetX = new Random().nextInt(8);
                        offsetX = new Random().nextBoolean() ? -offsetX : offsetX;
                        int offsetZ = new Random().nextInt(8);
                        offsetZ = new Random().nextBoolean() ? -offsetZ : offsetZ;
                        example.executeCommand("mm mobs spawn -s " + EnemyMobTypes.get(new Random().nextInt(EnemyMobTypes.size())) + ":"+ RandomLvl +" "+"1"+" " + loc.getWorld().getName() + "," + (loc.getX()+offsetX) + "," + (loc.getY()) + "," + (loc.getZ()+offsetZ));
                    }
        }
    }

    public List<String> getEnemyMobTypes() {
        return EnemyMobTypes;
    }

    public double getMinMobs() {
        return MinMobs;
    }

    public int getMobMaxLevel() {
        return MobMaxLevel;
    }

    public List<Location> getEnemyLocList() {
        return EnemyLocList;
    }

    public List<AssaultMethods.Doors> getListOfDoors() {
        return ListOfDoors;
    }

    public ObjectiveType getType() {
        return type;
    }

    public String getMessage() {
        return Message;
    }

    public String getName() {
        return Name;
    }

    public int getKeysToDeposit() {
        return KeysToDeposit;
    }

    public int getDepositedKeys() {
        return DepositedKeys;
    }

    public void AddDepositedKeys() {
        DepositedKeys++;
    }
}
*/