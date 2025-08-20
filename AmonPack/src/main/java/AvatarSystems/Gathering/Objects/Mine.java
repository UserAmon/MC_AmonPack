package AvatarSystems.Gathering.Objects;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;

public class Mine {
    private Location loc;
    private int YOffsetUp;
    private int Radius;
    private int RestoreTime;
    private Material MainBlock;
    private HashMap<Material, Integer> OresList = new HashMap();
    private HashMap<String, Integer> LootList = new HashMap();
    private HashMap<Material, Double> ExpList = new HashMap<>();


    public Mine(Location loc, HashMap<Material, Double> expList, HashMap<String, Integer> lootList) {
        this.loc = loc;
        ExpList = expList;
        LootList = lootList;
    }

    public Mine(Location loc, int YOffsetUp, int radius, int restoreTime, Material mainBlock, HashMap<Material, Integer> oresList, HashMap<String, Integer> lootList, HashMap<Material,Double> explist) {
        this.loc = loc;
        this.YOffsetUp = YOffsetUp;
        Radius = radius;
        RestoreTime = restoreTime;
        MainBlock = mainBlock;
        OresList = oresList;
        LootList = lootList;
        ExpList = explist;
    }

    public Location getLoc() {
        return loc;
    }
    public int getYOffsetUp() {
        return YOffsetUp;
    }
    public int getRadius() {
        return Radius;
    }
    public int getRestoreTime() {
        return RestoreTime;
    }
    public Material getMainBlock() {
        return MainBlock;
    }
    public HashMap<Material, Integer> getOresList() {
        return OresList;
    }
    public HashMap<String, Integer> getLootList() {
        return LootList;
    }
    public double GetExpByMaterial(Material mat){
        if(ExpList.containsKey(mat)){
            return ExpList.get(mat);
        }
        else {
            return 2;
        }
    }
}
