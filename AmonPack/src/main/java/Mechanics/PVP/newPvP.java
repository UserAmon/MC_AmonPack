package Mechanics.PVP;

import UtilObjects.PVP.FallingChest;
import UtilObjects.PVP.RandomEvents;
import commands.Commands;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import Mechanics.PVP.PvPMethods.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static Mechanics.PVP.PvPMethods.RTP;
import static Mechanics.PVP.PvPMethods.sendTitleMessage;


public class newPvP {
    public static Location Loc = null;
    private static double radius;
    private long Period;
    public static final List<FallingChest> ChestList = new ArrayList<>();
    private final List<RandomEvents> REventsList = new ArrayList<>();
    public static final List<Block> LastFallChest= new ArrayList<>();

    public newPvP(){
        double X = AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Loc.X");
        double Y = AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Loc.Y");
        double Z = AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Loc.Z");
        String World = AmonPackPlugin.getPvPConfig().getString("AmonPack.PvP.Loc.World");
        radius = AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Loc.Radius");
        Period = AmonPackPlugin.getPvPConfig().getLong("AmonPack.PvP.FallingChest.FallPeriod");
        Loc = new Location(Bukkit.getWorld(World), X, Y, Z);
        List<String> DefLoot = new ArrayList<>();
        List<Double> DefLootChance = new ArrayList<>();
        for(String key : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.FallingChest.Loot").getKeys(false)) {
            DefLoot.add(key);
            DefLootChance.add(AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Loot."+key));
        }
        ChestList.add(new FallingChest("Falling Chest", "Default",DefLoot  ,DefLootChance));
        for(String ChestType : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.FallingChest.Occurance").getKeys(false)) {
            if (ChestType.equalsIgnoreCase("Combat")) {
                for (String ChestName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.FallingChest.Occurance." + ChestType).getKeys(false)) {
                    int EAmount;
                    List<String> EType;
                    List<String> Loot = new ArrayList<>(DefLoot);
                    List<Double> LootChance = new ArrayList<>(DefLootChance);
                    for (String LootName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".Loot").getKeys(false)) {
                        Loot.add(LootName);
                        LootChance.add(AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".Loot." + LootName));
                    }
                    EType = AmonPackPlugin.getPvPConfig().getStringList("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".EType");
                    EAmount = AmonPackPlugin.getPvPConfig().getInt("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".EAmount");
                    ChestList.add(new FallingChest(ChestName, ChestType, Loot, LootChance, EAmount, EType));
                }}}
        for(String REType : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.Events").getKeys(false)) {
            if (REType.equalsIgnoreCase("RandomSpawns")) {
                for (String REName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.Events." + REType).getKeys(false)) {
                    if (!REName.equalsIgnoreCase("Period")){
                        int EAmount = AmonPackPlugin.getPvPConfig().getInt("AmonPack.PvP.Events." + REType + "." + REName + ".EAmount");
                        int SpaAmount = AmonPackPlugin.getPvPConfig().getInt("AmonPack.PvP.Events." + REType + "." + REName + ".SpaAmount");
                        List<String> EType = AmonPackPlugin.getPvPConfig().getStringList("AmonPack.PvP.Events." + REType + "." + REName + ".EType");
                        REventsList.add(new RandomEvents(REName, REType,EAmount, EType,SpaAmount));
                    }}}}
        Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::FireTimer,Period*20 ,Period*20);
        Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::RandomSpawner,(Period+5)*20 ,(Period+5)*20);
        Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::Fireworks ,Period*20 ,200);
    }

    private void Fireworks() {
        if (!LastFallChest.isEmpty()){
        for (Block b:LastFallChest) {
                PvPMethods.spawnFlyingFirework(b.getLocation().add(0,1,0));
            }}}
    private void FireTimer() {
        if (AmonPackPlugin.PvPEnabled) {
            List<Player> List=PlayersInPvP();
            if(!List.isEmpty()){
                if (!LastFallChest.isEmpty()){
                    for (Block b:LastFallChest) {
                        b.setType(Material.AIR);
                    }
                    LastFallChest.clear();
                }
                for (int i=0; i < (1 + new Random().nextInt(List.size()*2)); i++){
                    RandomBlock();
                }
                Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, ()->{
                    for (Player p:List) {
                        Location loc=findNearestChestLocation(p.getLocation());
                        p.setCompassTarget(loc);
                        int distance = (int) Math.round(p.getLocation().distance(loc));
                        sendTitleMessage(p,ChatColor.GREEN + "Skrzynie spadły!", ChatColor.YELLOW + "Najbliższa jest oddalona o: " + distance, 20,80,20);
                    }
                },40);
            }}}
    public static boolean isInventoryEmpty(Inventory inventory) {
        int i = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.BEDROCK) {
                i++;
                if (i > 1){
                    return false;
                }}}
        return true;
    }
    public static void SpawnMobs(Location locationofchest, String chname) {
        List <String> MobName = new ArrayList<>();
        int MobAmount = 0;
        for (FallingChest fc: ChestList) {
            if (fc.getName().equalsIgnoreCase(chname)){
                MobName.addAll(fc.getEnemyTypes());
                MobAmount = fc.getEnemyamount();
            }}
        for (int i = 0; i < MobAmount; i++) {
            double xOffset = (Math.random() * 5);
            double zOffset = (Math.random() * 5);
            double yOffset = (Math.random() * 5);
            double isnegative1 = (Math.random());
            if (isnegative1 >= 0.5) {
                xOffset = xOffset * -1;
            }
            double isnegative2 = (Math.random());
            if (isnegative2 >= 0.5) {
                zOffset = zOffset * -1;
            }
            Random random = new Random();
            int randomIndex = random.nextInt(MobName.size());
            Location loc = new Location(locationofchest.getWorld(), (locationofchest.getX() + xOffset), (locationofchest.getY() + yOffset), (locationofchest.getZ() + zOffset));
            Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
            example.executeCommand("mm mobs spawn -s " + MobName.get(randomIndex) + " 1 " + loc.getWorld().getName() + "," + loc.getX() + "," + (loc.getY() + 1) + "," + loc.getZ());
        }
    }
    public static Location findNearestChestLocation(Location playerLocation) {
        double dis = 1200.0;
        Location loc = Loc;
        if (Objects.equals(playerLocation.getWorld(), Loc.getWorld())){
            for (Block b:LastFallChest) {
                if (dis > b.getLocation().distance(playerLocation)){
                    dis = b.getLocation().distance(playerLocation);
                    loc = b.getLocation();
                }}}
        return loc;
    }
    public List<Entity> GetEnemiesInDung(){
        List<Entity>List=new ArrayList<>();
        for (Entity entity : Objects.requireNonNull(Loc.getWorld()).getEntities()) {
            if (entity.getWorld().equals(Loc.getWorld()) && !entity.getType().equals(EntityType.PLAYER) && entity instanceof LivingEntity){
                Location l = entity.getLocation();
                if ((l.getX() < Loc.getX()+radius && l.getX() > Loc.getX()-radius)&&(l.getZ() < Loc.getZ()+radius && l.getZ() > Loc.getZ()-radius)) {
                    List.add(entity);
                }}}
        return List;
    }
    public void ClearPvP(){
        if(Loc.getWorld() != null){
        for (Entity entity : Loc.getWorld().getEntities()) {
            if (entity.getWorld().equals(Loc.getWorld()) && !entity.getType().equals(EntityType.PLAYER) && entity instanceof LivingEntity){
                entity.remove();
            }}
    }}
    public void RandomBlock() {
        double xOffset = (Math.random() * radius);
        double zOffset = (Math.random() * radius);
        double isnegative1 = (Math.random());
        if (isnegative1 >= 0.5){
            xOffset = xOffset*-1;
        }
        double isnegative2 = (Math.random());
        if (isnegative2 >= 0.5){
            zOffset = zOffset*-1;
        }
        double ran = Math.random();
        if (ran > 0.3 + 1.0/(ChestList.size())){
            Random random = new Random();
            int randomIndex = 1 + random.nextInt(ChestList.size()-1);
            CreateOccuranceChest(Loc.clone().add(xOffset, 0, zOffset), ChestList.get(randomIndex).getName());
        } else{
            CreateOccuranceChest(Loc.clone().add(xOffset, 0, zOffset), ChestList.get(0).getName());
        }

    }
    public void CreateOccuranceChest(Location loc, String st) {
        Block b = PvPMethods.getBlockWithAirAbove(loc).getLocation().clone().add(0,1,0).getBlock();
        b.getLocation().getChunk().load();
        if (b.getType() != Material.CHEST) {
            LastFallChest.add(b);
            b.setType(Material.CHEST);
            Chest chest = (Chest) b.getState();
            chest.setCustomName(st);
            chest.update();
            for (FallingChest fc: ChestList) {
                if (fc.getName().equalsIgnoreCase(st) && !fc.getType().equalsIgnoreCase("Parkour")){
                    int MaxLoot =0;
                        for (int i = 0; i < fc.getLoot().size(); i++) {
                            if (fc.getLootchance().get(i) >= Math.random() && MaxLoot < 3) {
                                chest.getInventory().setItem(new Random().nextInt(chest.getInventory().getSize()),Commands.QuestItemConfig(fc.getLoot().get(i)));
                                MaxLoot++;
                            }}}}
            if (!st.equalsIgnoreCase("Falling Chest")){
                chest.getInventory().addItem(new ItemStack(Material.BEDROCK,1));
            }}
    }
    public void RandomSpawner(){
        if(GetEnemiesInDung().size()<60 && !PlayersInPvP().isEmpty()){
        //wysokie zasoby
        //if(GetEnemiesInDung().size()<60){
        RandomEvents RE;
        int AmountOffset = PlayersInPvP().size();
        int RI = new Random().nextInt(AmountOffset);
        do{
            RE = REventsList.get(new Random().nextInt(REventsList.size()));
        }while (!RE.getType().equalsIgnoreCase("RandomSpawns"));
        List <String> MobName = RE.getEnemyTypes();
        int MobAmount = RE.getEnemyamount();
        for (int SA = 0; SA < RE.getSpawnsAmount()+RI; SA++) {
            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, ()->{
                Location RLoc = RTP(radius,Loc);
                for (int i = 0; i < MobAmount; i++) {
                    double xOffset = (Math.random() * 5);
                    double zOffset = (Math.random() * 5);
                    double yOffset = (Math.random() * 5);
                    double isnegative1 = (Math.random());
                    if (isnegative1 >= 0.5) {
                        xOffset = xOffset * -1;
                    }
                    double isnegative2 = (Math.random());
                    if (isnegative2 >= 0.5) {
                        zOffset = zOffset * -1;
                    }
                    Random random = new Random();
                    int randomIndex = random.nextInt(MobName.size());
                    Location loc = new Location(RLoc.getWorld(), (RLoc.getX() + xOffset), (RLoc.getY() + yOffset), (RLoc.getZ() + zOffset));
                    Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                    example.executeCommand("mm mobs spawn -s " + MobName.get(randomIndex) + " 1 " + Objects.requireNonNull(loc.getWorld()).getName() + "," + loc.getX() + "," + (loc.getY() + 2) + "," + loc.getZ());
                }
            }, 40L *SA);
            }
        for (Player p: PlayersInPvP()) {
                p.sendMessage(ChatColor.RED+"[Ogłoszenie]  "+ChatColor.DARK_PURPLE+"Pojawiły się zgraje magów! Zachowaj czujność!");
            }}
    }//}
    private List<Player>PlayersInPvP(){
        List<Player>List=new ArrayList<>();
        for (Player p: Bukkit.getOnlinePlayers()) {
            if (playerinzone(p.getLocation())){
                List.add(p);
            }}
        return List;
    }
     public static boolean playerinzone(Location p){
        if (Objects.equals(Loc.getWorld(), p.getWorld())){
            if (p.getX() <= Loc.getX()+radius && p.getX() >= Loc.getX()-radius) {
                return p.getZ() <= Loc.getZ() + radius && p.getZ() >= Loc.getZ() - radius;
            }}
        return false;
    }
}

/*

        ClearPvP();

            Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
            example.executeCommand("rollback paste 702 50 666 SwiatPvPAs MPvP1");
            example.executeCommand("rollback paste -1516 38 -212 Kojlerek MapaPvPDlaNoobow");


 */