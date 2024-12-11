package OLD.Assault;
/*
import Mechanics.PVE.SimpleWorldGenerator;

import UtilObjects.PVE.Wave;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AssaultMenager {
    public static List<AssaultDef> listOfAssaultDef = new ArrayList<>();
    public static List<AssaultOffensive> listOfAssaultOffens = new ArrayList<>();
    public AssaultMenager() {

        for(String WaveDefenderName : Objects.requireNonNull(AmonPackPlugin.getWaveConfig().getConfigurationSection("WaveDefender")).getKeys(false)) {
            double SpawnX = AmonPackPlugin.getWaveConfig().getDouble("WaveDefender."+WaveDefenderName+".SpawnX");
            double SpawnY = AmonPackPlugin.getWaveConfig().getDouble("WaveDefender."+WaveDefenderName+".SpawnY");
            double SpawnZ = AmonPackPlugin.getWaveConfig().getDouble("WaveDefender."+WaveDefenderName+".SpawnZ");
            String World = AmonPackPlugin.getWaveConfig().getString("WaveDefender."+WaveDefenderName+".World");
            String Command = AmonPackPlugin.getWaveConfig().getString("WaveDefender."+WaveDefenderName+".Rollback");
            int StartHP = AmonPackPlugin.getWaveConfig().getInt("WaveDefender."+WaveDefenderName+".StartHP");
            int RestTimer = AmonPackPlugin.getWaveConfig().getInt("WaveDefender."+WaveDefenderName+".RestTimer");
            int DefendRadius = AmonPackPlugin.getWaveConfig().getInt("WaveDefender."+WaveDefenderName+".DefendRadius");
            int Range = AmonPackPlugin.getWaveConfig().getInt("WaveDefender."+WaveDefenderName+".Range");
            SimpleWorldGenerator.createAndSaveWorldOnLoad("MultiWorlds/Assault/"+World);
            World world = Bukkit.getWorld("MultiWorlds/Assault/"+World);
            Location SpawnLoc = new Location(world,SpawnX+0.5,SpawnY+0.5,SpawnZ+0.5);
            Location Shoploc = new Location(world,0.5,63,2.5);
            List<Location> ListOfLocs = new ArrayList<>();


            for (String WaveName : Objects.requireNonNull(AmonPackPlugin.getWaveConfig().getConfigurationSection("WaveDefender." + WaveDefenderName + ".Waves")).getKeys(false)) {
                for (String WaveSubName : Objects.requireNonNull(AmonPackPlugin.getWaveConfig().getConfigurationSection("WaveDefender." + WaveDefenderName + ".Waves." + WaveName)).getKeys(false)) {
                    if (WaveSubName.length() < 3) {
                        String path = "WaveDefender." + WaveDefenderName + ".Waves." + WaveName + "." + WaveSubName;
                        int AttackFromX = AmonPackPlugin.getWaveConfig().getInt(path + ".AttackFromX");
                        int AttackFromY = AmonPackPlugin.getWaveConfig().getInt(path + ".AttackFromY");
                        int AttackFromZ = AmonPackPlugin.getWaveConfig().getInt(path + ".AttackFromZ");
                        HashMap<String, Integer> MobTypes = new HashMap<>();
                        for (String Mobs : Objects.requireNonNull(AmonPackPlugin.getWaveConfig().getConfigurationSection(path + ".MobTypes")).getKeys(false)) {
                            int Number = AmonPackPlugin.getWaveConfig().getInt(path + ".MobTypes." + Mobs);
                            MobTypes.put(Mobs, Number);
                        }
                        int ExtraMobs = AmonPackPlugin.getWaveConfig().getInt(path + ".ExtraMobs");
                        int Offset = 0;
                        if (AmonPackPlugin.getWaveConfig().getInt(path + ".SpawnOffset") != 0) {
                            Offset = AmonPackPlugin.getWaveConfig().getInt(path + ".SpawnOffset");
                        }
                        HashMap<String, Integer> FriendlyMobs = new HashMap<>();
                        List<String>AllowedEntityTypes = new ArrayList<>();
                        if (AmonPackPlugin.getWaveConfig().getConfigurationSection(path + ".FriendlyMobTypes") != null){
                            for (String Mobs : Objects.requireNonNull(AmonPackPlugin.getWaveConfig().getConfigurationSection(path+".FriendlyMobTypes")).getKeys(false)) {
                                int Number = AmonPackPlugin.getWaveConfig().getInt(path+".FriendlyMobTypes."+Mobs);
                                FriendlyMobs.put(Mobs,Number);
                            }
                            AllowedEntityTypes=AmonPackPlugin.getWaveConfig().getStringList(path+".AllowedEntityTypes");
                        }
                        Location attackfromloc = new Location(SpawnLoc.getWorld(),AttackFromX,AttackFromY,AttackFromZ);
                        //Waves.add(new Wave(Integer.parseInt(WaveName),MobTypes, FriendlyMobs, attackfromloc,SpawnLoc,ExtraMobs, Offset,AllowedEntityTypes));
                    if (WaveName.equalsIgnoreCase("3")){
                        ListOfLocs.add(attackfromloc);
                    }
                    } else {
                        int AttackFromX = AmonPackPlugin.getWaveConfig().getInt("WaveDefender." + WaveDefenderName + ".Waves." + WaveName + ".AttackFromX");
                        int AttackFromY = AmonPackPlugin.getWaveConfig().getInt("WaveDefender." + WaveDefenderName + ".Waves." + WaveName + ".AttackFromY");
                        int AttackFromZ = AmonPackPlugin.getWaveConfig().getInt("WaveDefender." + WaveDefenderName + ".Waves." + WaveName + ".AttackFromZ");
                        HashMap<String, Integer> MobTypes = new HashMap<>();
                        for (String Mobs : Objects.requireNonNull(AmonPackPlugin.getWaveConfig().getConfigurationSection("WaveDefender." + WaveDefenderName + ".Waves." + WaveName+".MobTypes")).getKeys(false)) {
                            int Number = AmonPackPlugin.getWaveConfig().getInt("WaveDefender." + WaveDefenderName + ".Waves." + WaveName+".MobTypes."+Mobs);
                            MobTypes.put(Mobs,Number);
                        }
                        int Offset =0;
                        if (AmonPackPlugin.getWaveConfig().getInt("WaveDefender." + WaveDefenderName + ".Waves." + WaveName + ".SpawnOffset") != 0){
                            Offset = AmonPackPlugin.getWaveConfig().getInt("WaveDefender." + WaveDefenderName + ".Waves." + WaveName + ".SpawnOffset");
                        }
                        HashMap<String, Integer> FriendlyMobs = new HashMap<>();
                        List<String>AllowedEntityTypes = new ArrayList<>();
                        if (AmonPackPlugin.getWaveConfig().getConfigurationSection("WaveDefender." + WaveDefenderName + ".Waves." + WaveName + ".FriendlyMobTypes") != null) {
                            for (String Mobs : Objects.requireNonNull(AmonPackPlugin.getWaveConfig().getConfigurationSection("WaveDefender." + WaveDefenderName + ".Waves." + WaveName+".FriendlyMobTypes")).getKeys(false)) {
                                int Number = AmonPackPlugin.getWaveConfig().getInt("WaveDefender." + WaveDefenderName + ".Waves." + WaveName+".FriendlyMobTypes."+Mobs);
                                FriendlyMobs.put(Mobs,Number);
                            }
                            AllowedEntityTypes=AmonPackPlugin.getWaveConfig().getStringList("WaveDefender." + WaveDefenderName + ".Waves." + WaveName+".AllowedEntityTypes");
                        }
                        int ExtraMobs = AmonPackPlugin.getWaveConfig().getInt("WaveDefender." + WaveDefenderName + ".Waves." + WaveName + ".ExtraMobs");
                        Location attackfromloc = new Location(SpawnLoc.getWorld(),AttackFromX,AttackFromY,AttackFromZ);
                        //Waves.add(new Wave(Integer.parseInt(WaveName),MobTypes, FriendlyMobs, attackfromloc,SpawnLoc,ExtraMobs, Offset,AllowedEntityTypes));
                        break;
                    }}}
            List<String>MobTypes2 = new ArrayList<>();
            List<String>MobTypes3 = new ArrayList<>();
            List<String>MobTypes4 = new ArrayList<>();
            MobTypes2.add("WaveDefender_ChiBloker_1");
            MobTypes2.add("WaveDefender_MrocznyDuch_1");
            MobTypes2.add("WaveDefender_FireSentinel_1");

            MobTypes3.add("WaveDefender_ChiBloker_1");
            MobTypes3.add("WaveDefender_MrocznyDuch_1");
            MobTypes3.add("WaveDefender_FireSentinel_1");

            MobTypes4.add("WaveDefender_ChiBloker_1");
            MobTypes4.add("WaveDefender_MrocznyDuch_1");
            MobTypes4.add("WaveDefender_FireSentinel_1");
            List<Wave> WavesList = new ArrayList<>();
            System.out.println(ListOfLocs);
            List<Location>LocList1 = new ArrayList<>(ListOfLocs);
            LocList1.remove(2);
            LocList1.remove(1);
            List<Location>LocList2 = new ArrayList<>(ListOfLocs);
            LocList2.remove(1);
            WavesList.add(new Wave(3,3,1,MobTypes2,LocList1,SpawnLoc,2,20,0.34));
            WavesList.add(new Wave(6,3,2,MobTypes3,LocList2,SpawnLoc,3,40,0.5));
            WavesList.add(new Wave(9,3,3,MobTypes4,ListOfLocs,SpawnLoc,2,60,0.67));





            World Assault2World = Bukkit.getWorld("MultiWorlds/Assault/Assault2");
            List<String> MobsTypeList = List.of("WaveDefender_FireSentinel_1","WaveDefender_ChiBloker_1");
            List<Location> LocationsList = List.of(new Location(Assault2World,-31,59,0));
            List<AssaultMethods.Doors> DoorsList = List.of(new AssaultMethods.Doors(new Location(Assault2World,-6,65,1),new Location(Assault2World,-6,63,-1), Material.GLOWSTONE));
            //Objective TestObj1 = new Objective(MobsTypeList,5,2,LocationsList,DoorsList, Objective.ObjectiveType.Keys, ChatColor.YELLOW +"Naładujcie Pilar Duchów składając Klucze!",ChatColor.RED +"Klucznicy!",4);




            AssaultDef A = new AssaultDef(WaveDefenderName,SpawnLoc,Shoploc,Range,9,WavesList,StartHP,Command,RestTimer,DefendRadius);
            listOfAssaultDef.add(A);
            //Spawn//0.5,64,0.5 //World//Assault2
            //Door    //-6,65,1 //-6,63,-1
            //Arena//-31,59,0
        }
    }
    public static void StartAssault(Player p, String name){
        for (AssaultDef A: listOfAssaultDef) {
            if (A.getName().equalsIgnoreCase(name)){
                A.Start(p);
            }
        }
    }
}
*/