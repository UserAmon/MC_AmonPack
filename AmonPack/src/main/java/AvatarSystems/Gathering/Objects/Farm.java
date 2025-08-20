package AvatarSystems.Gathering.Objects;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;

public class Farm {
    private Location loc;
    private HashMap<Material, Double> ExpList = new HashMap<>();

    public Farm(Location loc, HashMap<Material, Double> expList) {
        this.loc = loc;
        ExpList = expList;
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
