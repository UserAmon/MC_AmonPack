package AvatarSystems.Gathering.Objects;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;

public class Forest {
    private Location loc;
    private HashMap<Material, Double> ExpList = new HashMap<>();

    public Forest(HashMap<Material, Double> expList, Location loc) {
        ExpList = expList;
        this.loc = loc;
    }

    public Location getLoc() {
        return loc;
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
