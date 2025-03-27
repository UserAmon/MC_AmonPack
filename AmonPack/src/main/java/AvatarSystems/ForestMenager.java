package AvatarSystems;

import AvatarSystems.Util_Objects.Forest;
import AvatarSystems.Util_Objects.InventoryXHolder;
import AvatarSystems.Util_Objects.PlayerLevel;
import AvatarSystems.Util_Objects.Resource;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ForestMenager {
    public static InventoryXHolder ForestHolder;
    public static List<Forest> ListOfForests;

    public ForestMenager() {
        LoadData();
        SetInvetory();
    }
    public static void LoadData(){
        FileConfiguration Config = AmonPackPlugin.getForestConfig();
        ListOfForests=new ArrayList<>();
        for(String ForestName : Config.getConfigurationSection("AmonPack.Forest").getKeys(false)) {
            World world = Bukkit.getWorld(Objects.requireNonNull(Config.getString("AmonPack.Forest." + ForestName + ".World")));
            double X = Config.getDouble("AmonPack.Forest." + ForestName + ".X");
            double Y = Config.getDouble("AmonPack.Forest." + ForestName + ".Y");
            double Z = Config.getDouble("AmonPack.Forest." + ForestName + ".Z");
            double Range = Config.getDouble("AmonPack.Forest." + ForestName + ".Range");
            Location ForestLocation = new Location(world,X,Y,Z);
            List<Resource> ListOfResoruce = new ArrayList<>();
            for(String ResourceName : Config.getConfigurationSection("AmonPack.Forest."+ForestName+".Resources").getKeys(false)) {
                String ResPath = "AmonPack.Forest."+ForestName+".Resources."+ResourceName;
                String LootName = Config.getString(ResPath+".Loot");
                int ClicksReq = Config.getInt(ResPath+".ClicksReq");
                double RestoreTime = Config.getDouble(ResPath+".RestoreTime");
                long DelocateTime = Config.getLong(ResPath+".DelocateTime");
                int Exp = Config.getInt(ResPath+".Exp");
                Resource resource = new Resource(ResourceName,DelocateTime,ClicksReq,LootName,RestoreTime,Exp);
                ListOfResoruce.add(resource);
            }
            Forest forest = new Forest(ForestLocation,ListOfResoruce,Range);
            ListOfForests.add(forest);
        }
    }
    private void SetInvetory(){
        ForestHolder=new InventoryXHolder(54,"");
    }
    public static Forest GetForestByLocation(Location loc){
        Optional<Forest> Exist = ListOfForests.stream().filter(forest -> forest.getCenterLocation().getWorld().equals(loc.getWorld())&&forest.getCenterLocation().distance(loc)<=forest.getRange()).findFirst();
        if(Exist.isPresent()){
                return Exist.get();
        }
        return null;
    }
}
