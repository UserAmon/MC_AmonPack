package UtilObjects.PVP;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class RandomEvents {
    private final String name;
    private String type;
    private String boss;
    private final List<String> loot = new ArrayList<>();
    private final List<Double> lootchance = new ArrayList<>();
    private String Command;
    private int enemyamount;
    private int SpawnsAmount;
    private final List<String> EnemyTypes = new ArrayList<>();
    private Location BossLoc;
    private Location BossSpawnLoc;
    private int ArenaS;
    private int ArenaH;

    public RandomEvents(String name, String type, List<String> l, List<Double> lc) {
        this.name = name;
        this.type = type;
        this.loot.addAll(l);
        this.lootchance.addAll(lc);
    }

    public RandomEvents(String name, String type, int eamo, List<String> ETypes, int spaamount) {
        this.name = name;
        this.type = type;
        this.enemyamount = eamo;
        this.SpawnsAmount = spaamount;
        this.EnemyTypes.addAll(ETypes);
    }

    public RandomEvents(String name, String type, String BossN, List<String> l, List<Double> lc, Location loc, int AS, int AH,Location loc2) {
        this.name = name;
        this.type = type;
        this.boss = BossN;
        this.loot.addAll(l);
        this.lootchance.addAll(lc);
        this.BossLoc = loc;
        this.ArenaH = AH;
        this.ArenaS = AS;
        this.BossSpawnLoc = loc2;
    }

    public Location getBossSpawnLoc() {
        return BossSpawnLoc;
    }

    public int getSpawnsAmount() {
        return SpawnsAmount;
    }

    public int getArenaS() {
        return ArenaS;
    }

    public int getArenaH() {
        return ArenaH;
    }

    public List<Double> getLootchance() {
        return lootchance;
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

    public String getBoss() {
        return boss;
    }

    public Location getBossLoc() {
        return BossLoc;
    }
}
