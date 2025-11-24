package AvatarSystems.Gathering.Objects;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class Mine {
    private Location loc;
    private int YOffsetUp;
    private int Radius;
    private int RestoreTime;
    private Material MainBlock;
    private HashMap<Material, Integer> OresList = new HashMap();
    private HashMap<String, Integer> LootList = new HashMap();
    private HashMap<Material, Double> ExpList = new HashMap<>();
    private Map<String, Double> iaExpMap = new HashMap<>();



    public Mine(Location loc, HashMap<Material, Double> expList, HashMap<String, Integer> lootList, Map<String, Double> iaExpMap) {
        this.loc = loc;
        ExpList = expList;
        LootList = lootList;
        this.iaExpMap = iaExpMap;
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
    public double GetExpByIA(String namespacedId) {
        return iaExpMap.getOrDefault(namespacedId, 0.0);
    }
}
