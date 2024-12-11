package UtilObjects.PVP;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class FallingChest {
    private String name;
    private String type;
    private final List<String> loot = new ArrayList<>();
    private final List<Double> lootchance = new ArrayList<>();
    private String Command;
    private Location StartLoc;
    private Location EndLoc;
    private int enemyamount;
    private final List<String> EnemyTypes = new ArrayList<>();

    public List<Double> getLootchance() {
        return lootchance;
    }

    public FallingChest(String name, String type, List<String> l, List<Double> lc) {
        this.name = name;
        this.type = type;
        this.loot.addAll(l);
        this.lootchance.addAll(lc);
    }
    public FallingChest(String name, String type, List<String> l, List<Double> lc, int eamo, List<String> ETypes) {
        this.name = name;
        this.type = type;
        this.enemyamount = eamo;
        this.loot.addAll(l);
        this.EnemyTypes.addAll(ETypes);
        this.lootchance.addAll(lc);
    }
    public FallingChest(String name, String type, List<String> l, List<Double> lc, String c) {
        this.name = name;
        this.type = type;
        this.Command = c;
        this.loot.addAll(l);
        this.lootchance.addAll(lc);
    }

    public FallingChest(String name, String type, List<String> l, List<Double> lc, Location loc1, Location loc2) {
        this.name = name;
        this.type = type;
        this.loot.addAll(l);
        this.lootchance.addAll(lc);
        this.StartLoc = loc1;
        this.EndLoc = loc2;
    }
    public Location getStartLoc() {
        return StartLoc;
    }

    public Location getEndLoc() {
        return EndLoc;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getLoot() {
        return loot;
    }

    public String getCommand() {
        return Command;
    }

    public int getEnemyamount() {
        return enemyamount;
    }

    public List<String> getEnemyTypes() {
        return EnemyTypes;
    }
}
