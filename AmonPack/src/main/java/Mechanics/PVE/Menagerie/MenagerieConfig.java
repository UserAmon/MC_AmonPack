package Mechanics.PVE.Menagerie;

import Mechanics.PVE.Menagerie.Objectives.Enemy;
import Mechanics.PVE.Menagerie.Objectives.ObjectiveConditions;
import Mechanics.PVE.Menagerie.Objectives.ObjectiveEffect;
import Mechanics.PVE.Menagerie.Objectives.Objectives;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.yaml.snakeyaml.scanner.Constant;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MenagerieConfig {
    FileConfiguration config= AmonPackPlugin.GetMenagerieConfig();
    FileConfiguration configdefault= AmonPackPlugin.getNewConfigz();
    public MenagerieConfig(){
    }

    public List<Menagerie> MenagerieFromConfig(){
        List<Menagerie>listofM=new ArrayList<>();
        for(String MenagerieName : Objects.requireNonNull(config.getConfigurationSection("Menagerie")).getKeys(false)) {
            List<Integer> CenterX = config.getIntegerList("Menagerie."+MenagerieName+".Center_Location");
            String BaseWorld = config.getString("Menagerie."+MenagerieName+".Base_World_Name");
            int RangeX = config.getInt("Menagerie."+MenagerieName+".Range_X");
            int RangeZ = config.getInt("Menagerie."+MenagerieName+".Range_Z");
            List<Encounter>ListOfEncounters=new ArrayList<>();
            for(String EncounterName : Objects.requireNonNull(config.getConfigurationSection("Menagerie."+MenagerieName+".Encounters")).getKeys(false)) {

                List<Integer> Spawn = config.getIntegerList("Menagerie."+MenagerieName+".Encounters."+EncounterName+".Spawn_Location");
                List<Integer> Doors1 = config.getIntegerList("Menagerie."+MenagerieName+".Encounters."+EncounterName+".Doors_1");
                List<Integer> Doors2 = config.getIntegerList("Menagerie."+MenagerieName+".Encounters."+EncounterName+".Doors_2");
                String DoorsMat = config.getString("Menagerie."+MenagerieName+".Encounters."+EncounterName+".Doors_Material");
                List<Objectives> Objectives=new ArrayList<>();
                Location s=new Location(Bukkit.getWorld(BaseWorld),Spawn.get(0),Spawn.get(1),Spawn.get(2));
                Location d1=new Location(Bukkit.getWorld(BaseWorld),Doors1.get(0),Doors1.get(1),Doors1.get(2));
                Location d2=new Location(Bukkit.getWorld(BaseWorld),Doors2.get(0),Doors2.get(1),Doors2.get(2));
                Menagerie.Doors d=new Menagerie.Doors(d1,d2,Material.getMaterial(DoorsMat));

                for(String ObjectiveName : Objects.requireNonNull(config.getConfigurationSection("Menagerie."+MenagerieName+".Encounters."+EncounterName+".Objectives")).getKeys(false)) {
                    List<ObjectiveConditions>Conditions=new ArrayList<>();
                    List<ObjectiveEffect>eff=new ArrayList<>();
                    String Path = "Menagerie."+MenagerieName+".Encounters."+EncounterName+".Objectives."+ObjectiveName;
                    List<String> NextActiveObjectives = new ArrayList<>(config.getStringList(Path + ".Next_Objectives"));
                    List<String> Req = new ArrayList<>(config.getStringList(Path + ".Req_Objectives"));
                    for (String OEffectName : Objects.requireNonNull(config.getConfigurationSection(Path + ".Effects")).getKeys(false)) {
                        int Time = config.getInt(Path + ".Effects." + OEffectName + ".Do_Every_Seconds");
                        List<String>ReqObjToEnd = config.getStringList(Path + ".Effects." + OEffectName + ".End_When_Obj_Done");
                       ObjectiveEffect objeff=parseEffect(Path + ".Effects." + OEffectName,BaseWorld);
                       if(objeff!=null){
                       if(Time>0){
                           objeff.setIntervals(Time);
                       }
                       if(!ReqObjToEnd.isEmpty()){
                           objeff.setReqObjToEnd(ReqObjToEnd);
                       }
                        eff.add(objeff);
                    }}
                    for(String OCon : Objects.requireNonNull(config.getConfigurationSection(Path+ ".Conditions")).getKeys(false)) {
                        Conditions.add(parseCondition(Path + ".Conditions." + OCon,BaseWorld));
                    }
                    List<String> Titles = new ArrayList<>(config.getStringList(Path + ".Scoreboard_Text"));

                    Objectives objective;
                    if(!NextActiveObjectives.isEmpty()){
                        objective=new Objectives(ObjectiveName,NextActiveObjectives,eff,Conditions);
                    }else{
                        objective=new Objectives(ObjectiveName,eff,Conditions);
                    }
                    if(!Req.isEmpty()){
                        objective.setReqObjectivesComplete(Req);
                    }
                    if(!Titles.isEmpty()){
                        objective.setDisplay(Titles);
                    }

                    ItemStack itemdrop =parseItemStack(Path + ".DropItems");
                    if(itemdrop!=null){
                        System.out.println("Znaleziono  "+itemdrop);
                        objective.setItemDropBoolean(true);
                        objective.setItemDrop(itemdrop);
                    }
                    Objectives.add(objective);
                }
                boolean islast = false;

                if(config.getString("Menagerie."+MenagerieName+".LastEncounter")!=null){
                    if(EncounterName.equalsIgnoreCase(config.getString("Menagerie."+MenagerieName+".LastEncounter"))){
                        islast = true;
                    }
                }

                ListOfEncounters.add(new Encounter(EncounterName,Objectives,s,d,islast));
            }
            String LastEncounter = null;
            if(config.getString("Menagerie."+MenagerieName+".LastEncounter")!=null){
                LastEncounter = config.getString("Menagerie."+MenagerieName+".LastEncounter");
            }

            boolean havecompass = config.getString("Menagerie." + MenagerieName + ".HaveCompass") == null;
            Location center=new Location(Bukkit.getWorld(BaseWorld),CenterX.get(0),CenterX.get(1),CenterX.get(2));
            Double X = configdefault.getDouble("AmonPack.Spawn.X");
            Double Y = configdefault.getDouble("AmonPack.Spawn.Y");
            Double Z = configdefault.getDouble("AmonPack.Spawn.Z");
            Location ned = new Location(null,X,Y,Z);
            Menagerie menagerie = new Menagerie(MenagerieName,center,ned,LastEncounter,havecompass,RangeX,RangeZ,ListOfEncounters);
            if(config.getString("Menagerie."+MenagerieName+".Return_World_Name")!=null){
                String ReturnWorld = config.getString("Menagerie."+MenagerieName+".Return_World_Name");
                List<Integer> ReturnLoc = config.getIntegerList("Menagerie."+MenagerieName+".Return_Location");
                try {
                    Location returnloc =new Location(Bukkit.getWorld(ReturnWorld),ReturnLoc.get(0),ReturnLoc.get(1),ReturnLoc.get(2));
                    menagerie.setReturnLocation(returnloc);
                }catch (Exception e){
                    System.out.println("cos sie zjebalo pewnie nie widzi swiata  "+e.getMessage());
                }
            }
            listofM.add(menagerie);
        }
        return listofM;
    }

    private ObjectiveEffect parseEffect(String path,String world) {
        Location effectLocation = parseLocation(path + ".EffectLocation",world);
        String message = config.getString(path + ".Message");
        String command = config.getString(path + ".Command");
        List<Enemy> enemies = parseEnemies(path + ".Enemies",world);
        Menagerie.Doors doors = parseDoors(path + ".Doors",world);
        boolean setDoorsAir = config.getBoolean(path + ".Doors.SetDoorsAir", false);
        boolean NEncounter = config.getBoolean(path + ".Next_Encounter", false);
        ItemStack giveItem = parseItemStack(path + ".GiveItem");
        if(NEncounter){
            return new ObjectiveEffect();
        }
        if(effectLocation!=null){
            return new ObjectiveEffect(effectLocation);
        }
        if(message!=null){
            return new ObjectiveEffect(message);
        }
        if(command!=null){
            return new ObjectiveEffect(command,true);
        }
        if(enemies!=null){
            return new ObjectiveEffect(enemies);
        }
        if(doors!=null){
            return new ObjectiveEffect(doors,setDoorsAir);
        }
        if(giveItem!=null){
            return new ObjectiveEffect(giveItem);
        }
        return null;
    }
    private ObjectiveConditions parseCondition(String path,String world) {
        if (config.isConfigurationSection(path + ".locationCondition")) {
            Location activationLoc = parseLocation(path + ".locationCondition.activationLoc",world);
            double activationRange = config.getDouble(path + ".locationCondition.activationRange", 0.0);
            return new ObjectiveConditions(activationLoc, activationRange);
        }
        else if (config.isConfigurationSection(path + ".zoneCondition")) {
            Location activationLoc = parseLocation(path + ".zoneCondition.activationLoc",world);
            double activationRange = config.getDouble(path + ".zoneCondition.activationRange", 0.0);
            int zoneChargeTime = config.getInt(path + ".zoneCondition.zoneChargeTime", 0);
            Material zoneMaterial = Material.getMaterial(config.getString(path + ".zoneCondition.zoneMaterial", ""));
            return new ObjectiveConditions(activationLoc, activationRange, zoneChargeTime, zoneMaterial);
        }
        else if (config.getBoolean(path + ".No_Mobs")) {
            return new ObjectiveConditions(ObjectiveConditions.ConditionType.NOMOBS);
        }
        else if (config.isConfigurationSection(path + ".killCondition")) {
            Enemy enemy = parseEnemy(path + ".killCondition.enemy",world);
            return new ObjectiveConditions(enemy);
        }
        else if (config.getBoolean(path + ".AllPlayersReady")) {
            return new ObjectiveConditions();
        }
        else if (config.isConfigurationSection(path + ".multiKillCondition")) {
            List<Enemy> enemies = parseEnemies(path + ".multiKillCondition.enemies",world);
            return new ObjectiveConditions(enemies);
        }

        else if (config.isConfigurationSection(path + ".interactCondition")) {
            Location activationLoc = parseLocation(path + ".interactCondition.activationLoc",world);
            double activationRange = config.getDouble(path + ".interactCondition.activationRange", 0.0);
            ItemStack collectItemInHand = parseItemStack(path + ".interactCondition.collectItemInHand");
            Material collectClickedMat = Material.getMaterial(config.getString(path + ".interactCondition.collectClickedMat", ""));
            return new ObjectiveConditions(activationLoc, activationRange, collectItemInHand, collectClickedMat);
        }

        else if (config.isConfigurationSection(path + ".collectCondition")) {
            Location activationLoc = parseLocation(path + ".collectCondition.activationLoc",world);
            double activationRange = config.getDouble(path + ".collectCondition.activationRange", 0.0);
            Material collectClickedMat = Material.getMaterial(config.getString(path + ".collectCondition.collectClickedMat", ""));
            ItemStack collectItemRecived = parseItemStack(path + ".collectCondition.collectItemRecived");
            return new ObjectiveConditions(activationLoc, activationRange, collectClickedMat, collectItemRecived);
        }
        return null;
    }
    private Menagerie.Doors parseDoors(String path,String world) {
        if (config.isConfigurationSection(path)) {
            Location l1 = parseLocation(path + ".Location1",world);
            Location l2 = parseLocation(path + ".Location2",world);
            String materialName = config.getString(path + ".Material");

            if (l1 != null && l2 != null && materialName != null) {
                Material mat = Material.getMaterial(materialName);
                if (mat != null) {
                    return new Menagerie.Doors(l1, l2, mat);
                }}}
        return null;
    }
    private Location parseLocation(String path,String world) {
        if (config.isSet(path)) {
            List<Integer> loc = config.getIntegerList(path);
            return new Location(Bukkit.getWorld(world), loc.get(0), loc.get(1), loc.get(2));
        }
        return null;
    }
    private Enemy parseEnemy(String path,String world) {
        Enemy enemy = null;
        if (config.isConfigurationSection(path)) {
                String enemyName = config.getString(path + ".Name");
                String enemyDName = config.getString(path + ".DisplayName");
                String enemyType = config.getString(path + ".Type");
                Location spawnLocation = parseLocation(path + ".SpawnLocation",world);
                int spawnLocationRange = config.getInt(path + ".SpawnLocationRange", 0);
                int spawnChance = config.getInt(path + ".SpawnChance", 100);
                int amount = config.getInt(path + ".Amount", 1);
                int maxLvl = config.getInt(path + ".MaxLvl", 1);
                enemy = new Enemy(enemyName, enemyDName, enemyType, spawnLocation, spawnLocationRange, spawnChance, amount, maxLvl);
        }
        if(enemy!=null){
            return enemy;
        }
        return null;
    }
    private List<Enemy> parseEnemies(String path,String world) {
        List<Enemy> enemies = new ArrayList<>();
        if (config.isConfigurationSection(path)) {
            for (String enemyKey : config.getConfigurationSection(path).getKeys(false)) {
                String enemyPath = path + "." + enemyKey;
                String enemyName = config.getString(enemyPath + ".Name");
                String enemyDName = config.getString(enemyPath + ".DisplayName");
                String enemyType = config.getString(enemyPath + ".Type");
                Location spawnLocation = parseLocation(enemyPath + ".SpawnLocation",world);
                int spawnLocationRange = config.getInt(enemyPath + ".SpawnLocationRange", 0);
                int spawnChance = config.getInt(enemyPath + ".SpawnChance", 100);
                int amount = config.getInt(enemyPath + ".Amount", 1);
                int maxLvl = config.getInt(enemyPath + ".MaxLvl", 1);
                int MaxSpawned = config.getInt(enemyPath + ".MaxSpawnedMobs");
                Enemy enemy = new Enemy(enemyName, enemyDName, enemyType, spawnLocation, spawnLocationRange, spawnChance, amount, maxLvl);
                if(MaxSpawned>0){
                    enemy.setMaxSpawned(MaxSpawned);
                }
                enemies.add(enemy);
            }
        }
        return enemies.isEmpty() ? null : enemies;
    }
    private ItemStack parseItemStack(String path) {
        if (config.isConfigurationSection(path)) {
            String materialName = config.getString(path + ".Material");
            System.out.println("test "+materialName);
            if (materialName == null) {
                return null;
            }
            Material material = Material.getMaterial(materialName);
            if (material == null) {
                return null;
            }
            int amount = config.getInt(path + ".Amount", 1);
            ItemStack itemStack = new ItemStack(material, amount);
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    String displayName = config.getString(path + ".Display_name");
                    if (displayName != null) {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                    }
                    List<String> lore = config.getStringList(path + ".Lore");
                    List<String> loreColor =new ArrayList<>();
                    for (String st:lore) {
                        loreColor.add(ChatColor.translateAlternateColorCodes('&', st));
                    }
                    if (!loreColor.isEmpty()) {
                        meta.setLore(loreColor);
                    }
                    itemStack.setItemMeta(meta);
            }
            return itemStack;
        }
        return null;
    }
}
