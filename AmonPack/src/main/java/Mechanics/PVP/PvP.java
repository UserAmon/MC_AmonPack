package Mechanics.PVP;
/*
import UtilObjects.PVP.FallingChest;
import UtilObjects.PVP.RandomEvents;
import commands.Commands;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PvP implements Listener {
public static Location PvPLoc = null;
public static Location PvPLoc1 = null;
private long Period;
public static String ActEvent;
private static long RESPeriod = 100;
public static double Radius = 100;
public static double Radius1 = 100;
public static final List<Block> LastFallChest= new ArrayList<>();;
public static final List<FallingChest> ChestList = new ArrayList<>();
public static final List<RandomEvents> REventsList = new ArrayList<>();
public static final List<Location> PLocList = new ArrayList<>();
public static final List<Location> RDLocList = new ArrayList<>();
    public static boolean Multibend;
    public static final Map<Player, Long> lastAttackTimes = new HashMap<>();
    public static final Map<Player, Player> fightParticipants = new HashMap<>();
    public static List<Material> WMats = new ArrayList<>();
    public static List<Material> FMats = new ArrayList<>();
    public PvP() {
        double X = AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Loc.X");
        double Y = AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Loc.Y");
        double Z = AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Loc.Z");
        String World = AmonPackPlugin.getPvPConfig().getString("AmonPack.PvP.Loc.World");
        Radius = AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Loc.Radius");
        Radius1 = AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP1.Loc.Radius");
        Period = AmonPackPlugin.getPvPConfig().getLong("AmonPack.PvP.FallingChest.FallPeriod");
        PvPLoc = new Location(Bukkit.getWorld(World), X, Y, Z);
        PvPLoc1 = new Location(Bukkit.getWorld(World), AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP1.Loc.X"), AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP1.Loc.Y"), AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP1.Loc.Z"));
        List<String> DefLoot = new ArrayList<>();
        List<Double> DefLootChance = new ArrayList<>();
        for(String key : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.FallingChest.Loot").getKeys(false)) {
        DefLoot.add(key);
        DefLootChance.add(AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Loot."+key));
        }
        LootCounter = 1;
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
                }
            }
            if (ChestType.equalsIgnoreCase("Command")) {
                for (String ChestName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.FallingChest.Occurance." + ChestType).getKeys(false)) {
                    String command = AmonPackPlugin.getPvPConfig().getString("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".Command");
                    List<String> Loot = new ArrayList<>(DefLoot);
                    List<Double> LootChance = new ArrayList<>(DefLootChance);
                    for (String LootName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".Loot").getKeys(false)) {
                        Loot.add(LootName);
                        LootChance.add(AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".Loot." + LootName));
                    }
                    ChestList.add(new FallingChest(ChestName, ChestType, Loot, LootChance, command));
                }}
            if (ChestType.equalsIgnoreCase("Parkour")) {
                for (String ChestName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.FallingChest.Occurance." + ChestType).getKeys(false)) {
                    Location SLoc = new Location(PvPLoc.getWorld(),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".StartLoc.X"),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".StartLoc.Y"),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".StartLoc.Z"));
                    Location ELoc = new Location(PvPLoc.getWorld(),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".EndLoc.X"),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".EndLoc.Y"),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".EndLoc.Z"));
                    List<String> Loot = new ArrayList<>(DefLoot);
                    List<Double> LootChance = new ArrayList<>(DefLootChance);
                    for (String LootName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".Loot").getKeys(false)) {
                        Loot.add(LootName);
                        LootChance.add(AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.FallingChest.Occurance." + ChestType + "." + ChestName + ".Loot." + LootName));
                    }
                    ChestList.add(new FallingChest(ChestName, ChestType,Loot  ,LootChance, SLoc, ELoc));
                    PLocList.add(ELoc);
                }}

        }
        RESPeriod = AmonPackPlugin.getPvPConfig().getLong("AmonPack.PvP.Events.RandomSpawns.Period");
        for(String REType : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.Events").getKeys(false)) {
            if (REType.equalsIgnoreCase("RandomSpawns")) {
                for (String REName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.Events." + REType).getKeys(false)) {
                    if (!REName.equalsIgnoreCase("Period")){
                    int EAmount = AmonPackPlugin.getPvPConfig().getInt("AmonPack.PvP.Events." + REType + "." + REName + ".EAmount");
                    int SpaAmount = AmonPackPlugin.getPvPConfig().getInt("AmonPack.PvP.Events." + REType + "." + REName + ".SpaAmount");
                    List<String> EType = AmonPackPlugin.getPvPConfig().getStringList("AmonPack.PvP.Events." + REType + "." + REName + ".EType");
                    REventsList.add(new RandomEvents(REName, REType,EAmount, EType,SpaAmount));
                }}
            }
            if (REType.equalsIgnoreCase("RaidBoss")) {
                for (String REName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.Events." + REType).getKeys(false)) {
                    String BossName = AmonPackPlugin.getPvPConfig().getString("AmonPack.PvP.Events." + REType + "." + REName + ".BossName");
                    int ArenaRadius = AmonPackPlugin.getPvPConfig().getInt("AmonPack.PvP.Events." + REType + "." + REName + ".ArenaRadius");
                    int ArenaHeight = AmonPackPlugin.getPvPConfig().getInt("AmonPack.PvP.Events." + REType + "." + REName + ".ArenaHeight");
                    Location ArenaLoc = new Location(PvPLoc.getWorld(),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Events." + REType + "." + REName + ".LocX"),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Events." + REType + "." + REName + ".LocY"),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Events." + REType + "." + REName + ".LocZ"));
                    Location BossSpawnLoc = new Location(PvPLoc.getWorld(),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Events." + REType + "." + REName + ".BossLocX"),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Events." + REType + "." + REName + ".BossLocY"),AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Events." + REType + "." + REName + ".BossLocZ"));
                    List<String> Loot = new ArrayList<>(DefLoot);
                    List<Double> LootChance = new ArrayList<>(DefLootChance);
                    for (String LootName : AmonPackPlugin.getPvPConfig().getConfigurationSection("AmonPack.PvP.Events." + REType + "." + REName + ".Loot").getKeys(false)) {
                        Loot.add(LootName);
                        LootChance.add(AmonPackPlugin.getPvPConfig().getDouble("AmonPack.PvP.Events." + REType + "." + REName + ".Loot." + LootName));
                    }
                        REventsList.add(new RandomEvents(REName, REType, BossName, Loot,LootChance,ArenaLoc,ArenaRadius,ArenaHeight,BossSpawnLoc));
                    RDLocList.add(BossSpawnLoc);
                }
            }

        }
        FallChest();
        WMats.add(Material.WATER);
        WMats.add(Material.ICE);
        FMats.add(Material.FIRE);
        FMats.add(Material.LAVA);
        AntyLogOutMultiplayer = 1;
        Multibend = false;
        ActEvent = "";
    }
    private void spawnFlyingFirework(Location location) {
        for (Player p:Bukkit.getOnlinePlayers()) {
            if (playerinzone(p.getLocation(), 100, location)){
                Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
                FireworkMeta fireworkMeta = firework.getFireworkMeta();
                FireworkEffect.Builder builder = FireworkEffect.builder();
                builder.withColor(Color.RED);
                builder.withColor(Color.GREEN);
                builder.with(FireworkEffect.Type.BURST);
                FireworkEffect effect = builder.build();
                fireworkMeta.addEffect(effect);
                fireworkMeta.setPower(3);
                firework.setFireworkMeta(fireworkMeta);
                Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, firework::remove, 100);
                break;
            }}
    }

    public static Location RTPPvP1() {
        double xOffset = (Math.random() * Radius1);
        double zOffset = (Math.random() * Radius1);
        double yOffset = (Math.random() * Radius1);
        double isnegative1 = (Math.random());
        if (isnegative1 >= 0.5){
            xOffset = xOffset*-1;
        }
        double isnegative2 = (Math.random());
        if (isnegative2 >= 0.5){
            zOffset = zOffset*-1;
        }
        return getBlockWithAirAbove(PvPLoc1.clone().add(xOffset, (yOffset/3), zOffset)).getLocation().clone().add(0,1,0);
    }

    public static Location RTP() {
        double xOffset = (Math.random() * Radius);
        double zOffset = (Math.random() * Radius);
        double yOffset = (Math.random() * Radius);
        double isnegative1 = (Math.random());
        if (isnegative1 >= 0.5){
            xOffset = xOffset*-1;
        }
        double isnegative2 = (Math.random());
        if (isnegative2 >= 0.5){
            zOffset = zOffset*-1;
        }
        return getBlockWithAirAbove(PvPLoc.clone().add(xOffset, (yOffset/3), zOffset)).getLocation().clone().add(0,1,0);
    }
    public static int taskId;
    public static int AntyLogOutMultiplayer;
    public static int LootCounter;
    private static int secondsElapsed;
    public void FireRandomEvent() {
        if (AmonPackPlugin.PvPEnabled){
            ActEvent = "";
        Bukkit.getScheduler().cancelTask(taskId);
        LootCounter = 1;
        AntyLogOutMultiplayer = 1;
        /*List<Player> Players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerinzone(PvPLoc,Radius+600,player.getLocation())) {
                Players.add(player);
            }}
        if (Players.size() != 0){
            int random = new Random().nextInt(5);
            //System.out.println("Randomowy Event!  " + random);
            if (random == 0){
                PvP.RaidBoss();
                ActEvent = "Boss";
            }else if (random == 1){
                for (Player player : Players) {
                    player.sendMessage(ChatColor.RED+"[Ogłoszenie]  "+ChatColor.DARK_PURPLE+"Żar leje się z nieba, trzymaj się blisko wody!");
                }
                PvP.ClimateChange(WMats, PotionEffectType.CONFUSION, 8);
                ActEvent = "Zar";
            }else if (random == 2){
                for (Player player : Players) {
                    player.sendMessage(ChatColor.RED+"[Ogłoszenie]  "+ChatColor.DARK_PURPLE+"Nadciągają chłody, trzymaj się blisko ognia!");
                }
                PvP.ClimateChange(FMats, PotionEffectType.SLOW, 3);
                ActEvent = "Chlod";
            }else if (random == 3){
                for (Player player : Players) {
                    player.sendMessage(ChatColor.RED+"[Ogłoszenie]  "+ChatColor.DARK_PURPLE+"Król **** **** dostrzegł wasze dokonania. Radujcie się jego darami!");
                    player.sendMessage(ChatColor.RED+"[Ogłoszenie]  "+ChatColor.DARK_PURPLE+"Zwiększony loot, spada więcej skrzyń, szybkość dla każdego... Oraz wydłużony AntyLogout");
                }
                LootCounter = 2;
                GlobalPotions(PotionEffectType.SPEED);
                AntyLogOutMultiplayer = 2;
                ActEvent = "Loot";
            }else{
                PvP.RandomSpawner();
            }
        }
            RandomSpawner();
        }
    }

    public static void GlobalPotions(PotionEffectType PET) {
        Bukkit.getScheduler().cancelTask(taskId);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, ()-> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (playerinzone(player.getLocation(),Radius,PvPLoc)){
                        player.addPotionEffect(new PotionEffect(PET, 20 * 6, 8)); // Slowness II for 5 seconds.
                    }}
        }, 0, 20*6);
    }
    public void FireRollBack() {
        MapRollBack();
    }

    public static void MapRollBack() {
        List<Player> PinRangeList = new ArrayList<>();
        for (Player p:Bukkit.getOnlinePlayers()) {
            if (playerinzone(p.getLocation(),Radius,PvPLoc)){
                p.sendMessage(ChatColor.RED+"[Ogłoszenie] "+ChatColor.GOLD+" Odnowienie mapy zacznie się za 30 sekund");
                PinRangeList.add(p);
            }
        }
        if (!PinRangeList.isEmpty()) {
        Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
            for (RandomEvents KillSpawns:REventsList) {
                if (KillSpawns.getType().equalsIgnoreCase("RandomSpawns")){
                    for (String st:KillSpawns.getEnemyTypes()) {
                        Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                        example.executeCommand("mm mobs kill " + st);
                    }
                }}
            Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
            example.executeCommand("rollback paste 702 50 666 SwiatPvPAs PvP1");
            //example.executeCommand("rollback paste -1516 38 -212 Kojlerek MapaPvPDlaNoobow");
        }, 30*20);
    }}

    public static void ClimateChange(List<Material> mats, PotionEffectType PET, int i) {
        secondsElapsed = 0;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, ()-> {
                if (secondsElapsed >= RESPeriod/60) {
                    Bukkit.getScheduler().cancelTask(taskId);
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!isNearWater(player,25,mats) && playerinzone(player.getLocation(),Radius,PvPLoc)) {
                            player.addPotionEffect(new PotionEffect(PET, 20 * 60, i));
                        }}
                    secondsElapsed++;
                }
        }, 0, 20*60);

    }
    public static boolean isNearWater(Player player, int radius, List<Material> mat) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    for (Material mats:mat) {
                        if (player.getLocation().add(x, y, z).getBlock().getType() == mats) {
                            return true;
                        }}}}}
        return false;
    }

    public void FallChest() {
        if (AmonPackPlugin.PvPEnabled){
            Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, PvP::checkFights, 0L, 20L);
        Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::FireRandomEvent, (60+RESPeriod)*20, RESPeriod*20);
        Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::FireRollBack, (60*20)*90, (60*90)*20);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, () -> {
            List<Player> Players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(PvPLoc.getWorld())) {
                    Players.add(player);
                }}
            int randomchest = (2 + new Random().nextInt(4 + Players.size()))*LootCounter;
            if (Players.size() != 0){
            DelAllChest();
            for (int i=0; i <= randomchest; i++){
                RandomBlock();
            }
            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, ()->{
                for (Player player : Players) {
                    if(playerinzone(PvPLoc,Radius+50,player.getLocation())){
                    player.setCompassTarget(findNearestChestLocation(player.getLocation()));
                    int distance = (int) Math.round(player.getLocation().distance(findNearestChestLocation(player.getLocation())));
                    sendTitleMessage(player,ChatColor.GREEN + "Skrzynie spadły!", ChatColor.YELLOW + "Najbliższa jest oddalona o: " + distance, 20,80,20);
                }}
                },40);
        }}, 0, (Period*20));
        Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, () -> {
            for (int i = 0; i < LastFallChest.size(); i++) {
                if (LastFallChest.get(i).getType() == Material.CHEST) {
                    spawnFlyingFirework(LastFallChest.get(i).getLocation());
                }else{
                    LastFallChest.remove(LastFallChest.get(i));
                }
            }
            }, 0, (100));
    }}
    public static void Fall() {
        List<Player> Players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerinzone(PvPLoc,Radius,player.getLocation()) && player.getWorld().getName().equalsIgnoreCase(PvPLoc.getWorld().getName())) {
                Players.add(player);
            }}
        int randomchest = (2 + new Random().nextInt(6 - 2 + Players.size()))*LootCounter;
        if (Players != null){
            DelAllChest();
            //System.out.println("Skrzynie Spadły: " + randomchest + " "+Players.size());
            for (int i=0; i < randomchest; i++){
                RandomBlock();
                RandomBlockPvP1();
            }
            for (Player player : Players) {
                player.setCompassTarget(findNearestChestLocation(player.getLocation()));
                int distance = (int) Math.round(player.getLocation().distance(findNearestChestLocation(player.getLocation())));
                sendTitleMessage(player,ChatColor.GREEN + "Skrzynie spadły!", ChatColor.YELLOW + "Najbliższa jest oddalona o: " + distance, 20,80,20);
            }
        }}

    public static void DelAllChest() {
        if (LastFallChest != null){
            for (Block b:LastFallChest) {
                b.setType(Material.AIR);
            }
        }}

    public static void sendTitleMessage(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public static void RandomBlock() {
        double xOffset = (Math.random() * Radius);
        double zOffset = (Math.random() * Radius);
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
            CreateOccuranceChest(PvPLoc.clone().add(xOffset, 0, zOffset), ChestList.get(randomIndex).getName());
        } else{
            CreateOccuranceChest(PvPLoc.clone().add(xOffset, 0, zOffset), ChestList.get(0).getName());
        }

    }
    public static void RandomBlockPvP1() {
        double xOffset = (Math.random() * Radius1);
        double zOffset = (Math.random() * Radius1);
        double isnegative1 = (Math.random());
        if (isnegative1 >= 0.5){
            xOffset = xOffset*-1;
        }
        double isnegative2 = (Math.random());
        if (isnegative2 >= 0.5){
            zOffset = zOffset*-1;
        }
        double ran = Math.random();
        if (ran > 1.0/(ChestList.size())){
            Random random = new Random();
            int randomIndex = 1 + random.nextInt(ChestList.size()-1);
            CreateOccuranceChest(PvPLoc1.clone().add(xOffset, 0, zOffset), ChestList.get(randomIndex).getName());
        } else{
            CreateOccuranceChest(PvPLoc1.clone().add(xOffset, 0, zOffset), ChestList.get(0).getName());
        }

    }
    public static void CreateOccuranceChest(Location loc, String st) {
        Block b = getBlockWithAirAbove(loc).getLocation().clone().add(0,1,0).getBlock();
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
                    for (int LC = 0; LC <= LootCounter; LC++) {
                for (int i = 0; i < fc.getLoot().size(); i++) {
                    if (fc.getLootchance().get(i) >= Math.random() && MaxLoot < 3) {
                        chest.getInventory().setItem(new Random().nextInt(chest.getInventory().getSize()),Commands.QuestItemConfig(fc.getLoot().get(i)));
                    MaxLoot++;
                    }}}}}
            if (!st.equalsIgnoreCase("Falling Chest")){
                chest.getInventory().addItem(new ItemStack(Material.BEDROCK,1));
            }}
    }

    public static Block getBlockWithAirAbove(Location location) {
        location.add(0,50,0);
        int i =0;
        Block blockbelow;
        do {
            i++;
            blockbelow = location.clone().subtract(0, i, 0).getBlock();
        }while (blockbelow.getType() == Material.AIR || blockbelow.getType() == Material.WATER);
        return blockbelow;
    }

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
    public static boolean playerinzone(Location l, double r, Location p){
        if (Objects.equals(l.getWorld(), p.getWorld())){
        if (p.getX() <= l.getX()+r && p.getX() >= l.getX()-r) {
        if (p.getZ() <= l.getZ()+r && p.getZ() >= l.getZ()-r) {
            if (Objects.equals(p.getWorld(), l.getWorld())){
                return true;
            }}}}
        return false;
    }

    public static boolean playerinzone(Player player){
        Location l = PvPLoc;
        double r=Radius;
        Location p = player.getLocation();
        if (Objects.equals(PvPLoc.getWorld(), p.getWorld())){
            if (p.getX() <= l.getX()+r && p.getX() >= l.getX()-r) {
                if (p.getZ() <= l.getZ()+r && p.getZ() >= l.getZ()-r) {
                    if (Objects.equals(p.getWorld(), l.getWorld())){
                        return true;
                    }}}}
        return false;
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
            example.executeCommand("mm mobs spawn " + MobName.get(randomIndex) + " 1 " + loc.getWorld().getName() + "," + loc.getX() + "," + (loc.getY() + 2) + "," + loc.getZ());
        }
    }

    public static Location findNearestChestLocation(Location playerLocation) {
        double dis = 1200.0;
        Location loc = PvPLoc;
        if (Objects.equals(playerLocation.getWorld(), PvPLoc.getWorld())){
        for (Block b:LastFallChest) {
            if (dis > b.getLocation().distance(playerLocation)){
                dis = b.getLocation().distance(playerLocation);
                loc = b.getLocation();
            }
        }}
        return loc;
    }
    public static void RaidBoss(){
        for (Player p: Bukkit.getOnlinePlayers()) {
            if (playerinzone(p.getLocation(),Radius,PvPLoc)){
                p.sendMessage(ChatColor.RED+"[Ogłoszenie]  "+ChatColor.DARK_PURPLE+"Czempion Króla wyzywa was na pojedynek! Przygotujcie się!");
            }}
        for (RandomEvents killboss:REventsList) {
            if (killboss.getType().equalsIgnoreCase("RaidBoss")){
                Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                example.executeCommand("mm mobs kill " + killboss.getBoss());
            }}
        Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, ()-> {
            RandomEvents RE;
            do{
                RE = REventsList.get(new Random().nextInt(REventsList.size()));
            }while (!RE.getType().equalsIgnoreCase("RaidBoss"));
            for (Player p: Bukkit.getOnlinePlayers()) {
                if (playerinzone(p.getLocation(),Radius,PvPLoc)){
                    p.sendMessage(ChatColor.RED+"[Ogłoszenie]  "+ChatColor.DARK_PURPLE+"Czempion Króla wyzywa was na pojedynek! Patroluje on środek Mapy!");
                    for (Location loc : RDLocList){
                        if (p.getLocation().distance(loc)>20){
                            p.teleport(new Location(RE.getBossLoc().getWorld(), RE.getBossLoc().getX(), RE.getBossLoc().getY(),RE.getBossLoc().getZ()));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 10));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 10));
                            break;
                        }}}}
            Location loc = RE.getBossSpawnLoc();
            Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
            //System.out.println("mm mobs spawn " + RE.getBoss() + " 1 " + Objects.requireNonNull(loc.getWorld()).getName() + "," + loc.getX() + "," + (loc.getY() + 1) + "," + loc.getZ());
            example.executeCommand("mm mobs spawn -s " + RE.getBoss() + " 1 " + loc.getWorld().getName() + "," + loc.getX() + "," + (loc.getY() + 1) + "," + loc.getZ());
        }, 20*10);
    }

    public static void RandomSpawner(){
        RandomEvents RE;
        int AmountOffset = 1;
        for (Player p: Bukkit.getOnlinePlayers()) {
            if (playerinzone(p.getLocation(),Radius,PvPLoc)){
                AmountOffset++;
            }}
        int RI = new Random().nextInt(AmountOffset);
        do{
            RE = REventsList.get(new Random().nextInt(REventsList.size()));
        }while (!RE.getType().equalsIgnoreCase("RandomSpawns"));
        List <String> MobName = RE.getEnemyTypes();
        int MobAmount = RE.getEnemyamount();
        for (int SA = 0; SA < RE.getSpawnsAmount()+RI; SA++) {
            Location RLoc = RTP();
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
        }}
        for (Player p: Bukkit.getOnlinePlayers()) {
            if (playerinzone(p.getLocation(),Radius,PvPLoc)){
                p.sendMessage(ChatColor.RED+"[Ogłoszenie]  "+ChatColor.DARK_PURPLE+"Pojawiły się zgraje magów! Zachowaj czujność!");
            }}
    }
    public static void Parkour(Player p, String chname){
        Location loc = null;
        for (FallingChest fc: ChestList) {
            if (fc.getName().equalsIgnoreCase(chname)){
                loc = fc.getStartLoc();
            }}
        if (loc != null){
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 10));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 10));
            p.teleport(loc);
        }else{
            System.out.println("Parkour Błąd Lokacji");
        }

    }




    public static Map<Player, BossBar> playerBossBars = new HashMap<>();
    public static void createPrivateBossBar(Player player) {
        BossBar bossBar = Bukkit.createBossBar("Walka", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setVisible(true);
        bossBar.addPlayer(player);
        playerBossBars.put(player, bossBar);
    }
    public static void updatePrivateBossBar(Player player, String message, float f) {
        BossBar bossBar = playerBossBars.get(player);
        if (playerinzone(player.getLocation(),Radius,PvPLoc)){
            bossBar.setProgress(f*((float)1/(float)(20*AntyLogOutMultiplayer)));
        }else{
            bossBar.setProgress(f*((float)1/(float)(20)));
        }
        if (bossBar != null) {
            bossBar.setTitle(message+(int)f);
        }}
    public static void removePrivateBossBar(Player player) {
        BossBar bossBar = playerBossBars.get(player);
        if (bossBar != null) {
            bossBar.removeAll();
            playerBossBars.remove(player);
        }}
    public static void checkFights() {
        Set<Player> playersInZone = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (playerinzone(p.getLocation(), Radius, PvPLoc)) {
                playersInZone.add(p);
                double maxHealth = 40.0;
                if (p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() != maxHealth) {
                    p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
                }
            } else {
                double maxHealth = 20.0;
                if (p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() != maxHealth) {
                    p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
                }}}
        if (!fightParticipants.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<Player, Player> entry : fightParticipants.entrySet()) {
                Player victim = entry.getKey();
                Player attacker = entry.getValue();
                boolean victimInZone = playersInZone.contains(victim);
                boolean attackerInZone = (attacker != null) && playersInZone.contains(attacker);
                long lastAttackTime = lastAttackTimes.getOrDefault(victim, currentTime);
                long timeSinceLastAttack = (currentTime - lastAttackTime) / 1000;
                if (victimInZone) {
                    if (!playerBossBars.containsKey(victim)) {
                        createPrivateBossBar(victim);
                    }
                    long timeout = timeSinceLastAttack;
                    if (timeout <= 0) {
                        fightParticipants.remove(victim);
                        lastAttackTimes.remove(victim);
                        removePrivateBossBar(victim);
                        if (attacker != null && !fightParticipants.containsValue(attacker)) {
                            removePrivateBossBar(attacker);
                        }
                    } else {
                        updatePrivateBossBar(victim, "Walka: ", timeout);
                    }
                } else {
                    long timeout = 20 - timeSinceLastAttack;
                    if (timeout <= 0) {
                        fightParticipants.remove(victim);
                        lastAttackTimes.remove(victim);
                        removePrivateBossBar(victim);
                        if (attacker != null && !fightParticipants.containsValue(attacker)) {
                            removePrivateBossBar(attacker);
                        }
                    } else {
                        updatePrivateBossBar(victim, "Walka: ", timeout);
                    }}
                if (attacker != null) {
                    if (!playerBossBars.containsKey(attacker)) {
                        createPrivateBossBar(attacker);
                    }
                    long lastAttackTime2 = lastAttackTimes.getOrDefault(attacker, currentTime);
                    long timeout = (currentTime - lastAttackTime2) / 1000;
                    if (attackerInZone) {
                        if (timeout <= 0) {
                            fightParticipants.remove(victim);
                            lastAttackTimes.remove(victim);
                            removePrivateBossBar(victim);
                            if (!fightParticipants.containsValue(attacker)) {
                                removePrivateBossBar(attacker);
                            }
                        } else {
                            updatePrivateBossBar(attacker, "Walka: ", timeout);
                        }
                    } else {
                        if (timeout <= 0) {
                            fightParticipants.remove(victim);
                            lastAttackTimes.remove(victim);
                            removePrivateBossBar(victim);
                            if (!fightParticipants.containsValue(attacker)) {
                                removePrivateBossBar(attacker);
                            }
                        } else {
                            updatePrivateBossBar(attacker, "Walka: ", timeout);
                        }}}}}}

}*/
