package OLD;
/*
import Mechanics.PVE.SimpleWorldGenerator;
import General.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.*;

public class Dungeons implements Listener {
    public static List<Dungeon> ListOfDungeons = new ArrayList<>();

    public Dungeons() throws CloneNotSupportedException {
        for(String DungName : Objects.requireNonNull(AmonPackPlugin.getMenagerieConfig().getConfigurationSection("AmonPack.Dungeons")).getKeys(false)) {
        double CenterX = AmonPackPlugin.getMenagerieConfig().getDouble("AmonPack.Dungeons."+DungName+".CenterX");
        double CenterY = AmonPackPlugin.getMenagerieConfig().getDouble("AmonPack.Dungeons."+DungName+".CenterY");
        double CenterZ = AmonPackPlugin.getMenagerieConfig().getDouble("AmonPack.Dungeons."+DungName+".CenterZ");
        double SpawnX = AmonPackPlugin.getMenagerieConfig().getDouble("AmonPack.Dungeons."+DungName+".SpawnX");
        double SpawnY = AmonPackPlugin.getMenagerieConfig().getDouble("AmonPack.Dungeons."+DungName+".SpawnY");
        double SpawnZ = AmonPackPlugin.getMenagerieConfig().getDouble("AmonPack.Dungeons."+DungName+".SpawnZ");
        double RollBackX = AmonPackPlugin.getMenagerieConfig().getDouble("AmonPack.Dungeons."+DungName+".RollBackX");
        double RollBackY = AmonPackPlugin.getMenagerieConfig().getDouble("AmonPack.Dungeons."+DungName+".RollBackY");
        double RollBackZ = AmonPackPlugin.getMenagerieConfig().getDouble("AmonPack.Dungeons."+DungName+".RollBackZ");
        int Xrad = AmonPackPlugin.getMenagerieConfig().getInt("AmonPack.Dungeons."+DungName+".Xradius");
        int Zrad = AmonPackPlugin.getMenagerieConfig().getInt("AmonPack.Dungeons."+DungName+".Zradius");
        String Rollbackcommend = (AmonPackPlugin.getMenagerieConfig().getString("AmonPack.Dungeons."+DungName+".Rollback"));
        String FirstObjective = AmonPackPlugin.getMenagerieConfig().getString("AmonPack.Dungeons."+DungName+".FirstObjective");
        SimpleWorldGenerator.createAndSaveWorldOnLoad("MultiWorlds/Dungeons/"+DungName);
        World world = Bukkit.getWorld("MultiWorlds/Dungeons/"+DungName);
        //World world = Bukkit.getWorld(Objects.requireNonNull(AmonPackPlugin.getDungeonsConfig().getString("AmonPack.Dungeons."+DungName+".World")));
        List<DungEvent> DEL = new ArrayList<>();
        List<String> Conditions;
        for(String DungElementName : Objects.requireNonNull(AmonPackPlugin.getMenagerieConfig().getConfigurationSection("AmonPack.Dungeons."+DungName+".Location")).getKeys(false)) {
            List<Integer> XYZ = AmonPackPlugin.getMenagerieConfig().getIntegerList("AmonPack.Dungeons."+DungName+".Location."+DungElementName+".Loc");
            List<String> EffectsList = AmonPackPlugin.getMenagerieConfig().getStringList("AmonPack.Dungeons."+DungName+".Location."+DungElementName+".Effects");
            Conditions = (AmonPackPlugin.getMenagerieConfig().getStringList("AmonPack.Dungeons."+DungName+".Location."+DungElementName+".Conditions"));
            DEL.add(new DungEvent(DungElementName,EffectsList,Conditions,"Location",new Location(world,XYZ.get(0),XYZ.get(1),XYZ.get(2))));
        }
        for(String DungElementName : Objects.requireNonNull(AmonPackPlugin.getMenagerieConfig().getConfigurationSection("AmonPack.Dungeons."+DungName+".Interact")).getKeys(false)) {
            List<Integer> XYZ = AmonPackPlugin.getMenagerieConfig().getIntegerList("AmonPack.Dungeons."+DungName+".Interact."+DungElementName+".Loc");
            List<String> EffectsList = AmonPackPlugin.getMenagerieConfig().getStringList("AmonPack.Dungeons."+DungName+".Interact."+DungElementName+".Effects");
            Conditions = (AmonPackPlugin.getMenagerieConfig().getStringList("AmonPack.Dungeons."+DungName+".Interact."+DungElementName+".Conditions"));
            DEL.add(new DungEvent(DungElementName,EffectsList,Conditions,"Interact",new Location(world,XYZ.get(0),XYZ.get(1),XYZ.get(2))));
        }
        for(String DungElementName : Objects.requireNonNull(AmonPackPlugin.getMenagerieConfig().getConfigurationSection("AmonPack.Dungeons."+DungName+".Zone")).getKeys(false)) {
            List<Integer> XYZ = AmonPackPlugin.getMenagerieConfig().getIntegerList("AmonPack.Dungeons."+DungName+".Zone."+DungElementName+".Loc");
            List<String> EffectsList = AmonPackPlugin.getMenagerieConfig().getStringList("AmonPack.Dungeons."+DungName+".Zone."+DungElementName+".Effects");
            Conditions = (AmonPackPlugin.getMenagerieConfig().getStringList("AmonPack.Dungeons."+DungName+".Zone."+DungElementName+".Conditions"));
            int Radius = AmonPackPlugin.getMenagerieConfig().getInt("AmonPack.Dungeons."+DungName+".Zone."+DungElementName+".Radius");
            int ChargeTime = AmonPackPlugin.getMenagerieConfig().getInt("AmonPack.Dungeons."+DungName+".Zone."+DungElementName+".Time");
            String Particle = AmonPackPlugin.getMenagerieConfig().getString("AmonPack.Dungeons."+DungName+".Zone."+DungElementName+".Particle");
            DEL.add(new DungEvent(DungElementName,EffectsList,Conditions,"Zone",new Location(world,XYZ.get(0),XYZ.get(1),XYZ.get(2)),Radius,ChargeTime,Particle));
        }
        for(String DungElementName : Objects.requireNonNull(AmonPackPlugin.getMenagerieConfig().getConfigurationSection("AmonPack.Dungeons."+DungName+".Kill")).getKeys(false)) {
            List<Integer> XYZ = AmonPackPlugin.getMenagerieConfig().getIntegerList("AmonPack.Dungeons."+DungName+".Kill."+DungElementName+".Loc");
            List<String> EffectsList = AmonPackPlugin.getMenagerieConfig().getStringList("AmonPack.Dungeons."+DungName+".Kill."+DungElementName+".Effects");
            int Radius = AmonPackPlugin.getMenagerieConfig().getInt("AmonPack.Dungeons."+DungName+".Kill."+DungElementName+".Radius");
            String etype = AmonPackPlugin.getMenagerieConfig().getString("AmonPack.Dungeons."+DungName+".Kill."+DungElementName+".MobType");
            String ename = AmonPackPlugin.getMenagerieConfig().getString("AmonPack.Dungeons."+DungName+".Kill."+DungElementName+".MobName");
            int reqkills = AmonPackPlugin.getMenagerieConfig().getInt("AmonPack.Dungeons."+DungName+".Kill."+DungElementName+".ReqAmount");
            DEL.add(new DungEvent(DungElementName,EffectsList,"Kill",new Location(world,XYZ.get(0),XYZ.get(1),XYZ.get(2)),Radius,etype,ename,reqkills));
        }
        Dungeon Dung1 = new Dungeon(FirstObjective,DEL,DungName,new Location(world,RollBackX,RollBackY,RollBackZ),new Location(world,SpawnX,SpawnY,SpawnZ), new Location(world,CenterX,CenterY,CenterZ), Xrad,Zrad,Rollbackcommend);
        ListOfDungeons.add(Dung1);
            String folderPath = "MultiWorlds/Dungeons";
            File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
                File[] subfolders = folder.listFiles(File::isDirectory);
                if (subfolders != null && subfolders.length > 0) {
                    for (File subfolder : subfolders) {
                        if (subfolder.getName().startsWith(Dung1.getName())){
                            if (!subfolder.getName().equals(Dung1.getName())&&subfolder.getName().substring(0,Dung1.getName().length()).equalsIgnoreCase(Dung1.getName())){
                                String worldName = "MultiWorlds/Dungeons/"+subfolder.getName();
                                SimpleWorldGenerator.createAndSaveWorldOnLoad(worldName);
                                World temporaryWorld = Bukkit.getWorld(worldName);
                                List<DungEvent> TempDeList = new ArrayList<>();
                                for (DungEvent de:DEL) {
                                    DungEvent tempDE = (DungEvent) de.clone();
                                    tempDE.changeloc(100,temporaryWorld);
                                    TempDeList.add(tempDE);
                                }
                                Dungeon Dung2 = new Dungeon(FirstObjective,TempDeList,DungName,new Location(world,RollBackX,RollBackY,RollBackZ),new Location(temporaryWorld,SpawnX,SpawnY,SpawnZ), new Location(world,CenterX,CenterY,CenterZ), Xrad,Zrad,Rollbackcommend);
                                Dung2.ChangeWorld(temporaryWorld);
                                ListOfDungeons.add(Dung2);
                            }
                        }
                    }
                }
            }
        }
    }
    public static void StartDungeon(List<Player> PList, String name) throws CloneNotSupportedException {
        int i =0;
        for (Dungeon dung:ListOfDungeons) {

            if (PInDung(dung, dung.getXRange(), dung.getZRange(),PList).isEmpty() && dung.getName().equalsIgnoreCase(name)) {
                for (Player p:PList) {
                    //p.teleport(dung.getDungStartLoc().clone().add(0,1,0));
                    SimpleWorldGenerator.createAndSaveTemporaryWorld(p,"Dungeons", dung.getName(),dung.getDungStartLoc().clone().add(0,1,0));
                }
                KillMobsInDUng(dung,dung.getXRange(),dung.getZRange());
                dung.ResetActive(PInDung(dung,dung.getXRange(),dung.getZRange()));
                i=1;
                break;
            }}
        if (i==0){
        Dungeon dung = null;
        for (Dungeon dung2:ListOfDungeons) {
            if (dung2.getName().equalsIgnoreCase(name)){
                dung = dung2;
                break;
            }}
        if (dung != null){
            System.out.println("NOWY DUNG");
            String st = dung.getName()+"_"+System.currentTimeMillis();
            SimpleWorldGenerator.createAndSaveWorldOnLoad("MultiWorlds/Dungeons/"+st);
            World w = Bukkit.getWorld("MultiWorlds/Dungeons/"+st);
            List<DungEvent> TempDeList = new ArrayList<>();
            for (DungEvent de:dung.getDEList()) {
                DungEvent tempDE = (DungEvent) de.clone();
                TempDeList.add(tempDE);
                tempDE.changeloc(100,w);
            }
            Dungeon Dung2 = new Dungeon(AmonPackPlugin.getMenagerieConfig().getString("AmonPack.Dungeons."+dung.getName()+".FirstObjective"),TempDeList,dung.getName(),dung.getRollBackLoc().clone(),dung.getDungStartLoc().clone(),dung.getCenterOfArena().clone(), dung.getXRange(), dung.getZRange(),dung.getRollbackCommand());
            //Dungeon Dung2 = new Dungeon("test",TempDeList,dung.getName()+"_"+System.currentTimeMillis(),dung.getRollBackLoc().clone().add(-550,0,0),dung.getDungStartLoc().clone().add(-550,0,0),dung.getCenterOfArena().clone().add(-550,0,0), dung.getXRange(), dung.getZRange(),dung.getRollbackCommand());
            //Dung2.AddToEffects(-550);
            Dung2.ChangeWorld(w);
            ListOfDungeons.add(Dung2);
            for (Player p:PList) {
                SimpleWorldGenerator.createAndSaveTemporaryWorld(p,"Dungeons", st,Dung2.getDungStartLoc().clone().add(0,1,0));
            }
            KillMobsInDUng(Dung2,Dung2.getXRange(),Dung2.getZRange());
            Dung2.ResetActive(PInDung(Dung2,Dung2.getXRange(),Dung2.getZRange()));
        }else{
        System.out.println("Błędna nazwa dungeonu");}
        }


    }

    public static boolean InDungeonRange(Location l, Dungeon dung, int x, int z){
        Location CenterOfArena = dung.getCenterOfArena();
        if ((l.getX() < CenterOfArena.getX()+x && l.getX() > CenterOfArena.getX()-x)&&(l.getZ() < CenterOfArena.getZ()+z && l.getZ() > CenterOfArena.getZ()-z)) {
            return true;
        }
        return false;
    }
    private static List<Player> PInDung(Dungeon dung, int x, int z, List<Player> PList){
        List<Player> PInDung = new ArrayList<>();
        Location CenterOfArena = dung.getCenterOfArena();
        for (Player p:Bukkit.getOnlinePlayers()) {
        if (p.getWorld().equals(CenterOfArena.getWorld())){
            Location l = p.getLocation();
             if ((l.getX() < CenterOfArena.getX()+x && l.getX() > CenterOfArena.getX()-x)&&(l.getZ() < CenterOfArena.getZ()+z && l.getZ() > CenterOfArena.getZ()-z)) {
                 PInDung.add(p);
             }}}
        PInDung.removeAll(PList);
        return PInDung;
    }
    public static List<Player> PInDung(Dungeon dung, int x, int z){
        List<Player> PInDung = new ArrayList<>();
        Location CenterOfArena = dung.getCenterOfArena();
        for (Player p:Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(CenterOfArena.getWorld())){
                Location l = p.getLocation();
                if ((l.getX() < CenterOfArena.getX()+x && l.getX() > CenterOfArena.getX()-x)&&(l.getZ() < CenterOfArena.getZ()+z && l.getZ() > CenterOfArena.getZ()-z)) {
                    PInDung.add(p);
                }}}
        return PInDung;
    }

    public static void KillMobsInDUng(Dungeon dung, int x, int z){
        Location CenterOfArena = dung.getCenterOfArena();
        for (Entity entity : dung.getCenterOfArena().getWorld().getEntities()) {
            if (entity.getWorld().equals(CenterOfArena.getWorld()) && !entity.getType().equals(EntityType.PLAYER)){
                Location l = entity.getLocation();
                if ((l.getX() < CenterOfArena.getX()+x && l.getX() > CenterOfArena.getX()-x)&&(l.getZ() < CenterOfArena.getZ()+z && l.getZ() > CenterOfArena.getZ()-z)) {
                    entity.remove();
                }}}
    }
    public static void KillMobsInDUng(Dungeon dung, int x, int z, String st){
        Location CenterOfArena = dung.getCenterOfArena();
        for (Entity entity : dung.getCenterOfArena().getWorld().getEntities()) {
            if (entity.getWorld().equals(CenterOfArena.getWorld()) && !entity.getType().equals(EntityType.PLAYER)){
                Location l = entity.getLocation();
                if ((l.getX() < CenterOfArena.getX()+x && l.getX() > CenterOfArena.getX()-x)&&(l.getZ() < CenterOfArena.getZ()+z && l.getZ() > CenterOfArena.getZ()-z)) {
                    if (st.length() > 3){
                        if (!entity.getName().substring(4).equalsIgnoreCase(st.substring(0,st.length()-1)) && !entity.getName().substring(4).equalsIgnoreCase(st) && !entity.getName().substring(3).equalsIgnoreCase(st)){
                            entity.remove();
                        }
                    }else{
                        entity.remove();
                    }
}}}
    }


}
*/