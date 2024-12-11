package OLD;
/*
import commands.Commands;
import General.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

import static OLD.Assault.AssaultMethods.spawnParticleCircle;

public class Dungeon {
    private final Location DungStartLoc;
    private final Location CenterOfArena;
    private final Location RollBackLoc;
    private final int XRange;
    private final int ZRange;
    private final String RollbackCommand;
    private final String Name;
    public  int offset;
     final HashMap<Location,Doors> DoorsList = new HashMap<>();
     final List<DungEvent> UsedDungEvents = new ArrayList<>();final List<DungEvent> TempZone = new ArrayList<>();
    private List<DungEvent> DEList = new ArrayList<>();
    private Dungeon thisdung;
    public  final Map<Player, BossBar> playerBossBars = new HashMap<>();
    public  int taskIdZone;
    public  int taskIdSpawn;
    public  int ZoneChargingSeconds;
    public  int MobSPawnSeconds;
    public  int MobSpawnPeriod;
    public  String CurrentObjective;
    public Dungeon(String st , List<DungEvent> del,String n,Location rollloc, Location Startloc, Location MiddleArena, int x, int z, String roll) {
        this.DungStartLoc = Startloc;
        this.CenterOfArena = MiddleArena;
        this.XRange = x;
        this.ZRange = z;
        this.RollbackCommand = roll;
        this.RollBackLoc = rollloc;
        this.Name = n;
        this.DEList = del;
        thisdung = this;
        CurrentObjective = st;
        Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::DungObjective, 0L, 20L);
    }
    public void DungObjective(){
        if (!playerBossBars.isEmpty()){
        for (Player p:playerBossBars.keySet()) {
            if (!Dungeons.InDungeonRange(p.getLocation(),this,getXRange(),getZRange())){
                removePrivateBossBar(p);
                p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }else if (!playerBossBars.get(p).getTitle().equalsIgnoreCase(CurrentObjective) && !playerBossBars.get(p).getTitle().startsWith("Postęp:")){
                CurrentObjectiveUpdater(p);
            }
        }}
    }

    public void ChangeWorld(World w){
        this.DungStartLoc.setWorld(w);
        this.CenterOfArena.setWorld(w);
        this.RollBackLoc.setWorld(w);
        for (Location loc:DoorsList.keySet()) {
            loc.setWorld(w);
        }
        for (Doors d:DoorsList.values()) {
            d.l2.setWorld(w);
            d.l1.setWorld(w);
        }
        for (DungEvent de:this.DEList) {
            de.ChangeWorldE(w);
        }
    }

    public void AddToEffects(int i, World w){
        offset = i;
        for (DungEvent de:this.DEList) {
            de.changeloc(i,w);
        }
    }
    public void ActivateByMove(Player p,List<Player> lp){
        for (DungEvent de:DEList) {
            if (de.getType().equals("Location")){
                if (!UsedDungEvents.contains(de)){
                if (de.checkconditions(de,p.getLocation(),thisdung)){
                        de.exeeffects(thisdung);
                        UsedDungEvents.add(de);
                    }}}
            if (de.getType().equals("Zone")){
                if (!UsedDungEvents.contains(de) && !TempZone.contains(de)){
                if (de.checkconditions(de,p.getLocation(),thisdung)){
                        ZoneCharging(de,lp);
                        TempZone.add(de);
                    }}}}
    }
    public void ActivateByKill(Entity ent){
        for (DungEvent de:DEList) {
            if (de.getType().equals("Kill")){
                if (ent.getLocation().distance(de.getLocOfEvent())<=de.getRadius() && !UsedDungEvents.contains(de)){
                    if (ent.getType().equals(de.getEntity())){
                        if (de.getEntityname().equalsIgnoreCase("Default")){
                            de.KillCounter(thisdung);
                        }else{
                            if (ent.getName().substring(4).equalsIgnoreCase(de.getEntityname())){
                                de.KillCounter(thisdung);
                            }
                        }}}}}
    }
    public void ActivateByInteract(Player p){
        for (DungEvent de:DEList) {
            if (de.getType().equals("Interact")){
                if (de.checkconditions(de,p.getLocation(),thisdung)){
                    if (!UsedDungEvents.contains(de)){
                        de.exeeffects(thisdung);
                        UsedDungEvents.add(de);
                    }
                }}}
    }
    public static class Doors {
        final Location l1;
        final Location l2;
        final Material m;
        public Doors(Location l1, Location l2, Material m) {
            this.l1=l1;
            this.l2=l2;
            this.m=m;
        }
    }

    public  void CurrentObjectiveUpdater(Player player) {
        if (!playerBossBars.containsKey(player)){
            createPrivateBossBar(player);
        }
        updatePrivateBossBar(player,1, 1,CurrentObjective);
    }

    public  void createPrivateBossBar(Player player) {
        BossBar bossBar = Bukkit.createBossBar("Cel", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setVisible(true);
        bossBar.addPlayer(player);
        playerBossBars.put(player, bossBar);
    }
    public  void updatePrivateBossBar(Player player, float f, float max, String st) {
        BossBar bossBar = playerBossBars.get(player);
        bossBar.setProgress(f*((float)1/(max)));
        if (bossBar != null) {
            bossBar.setTitle(st);
        }}
    public  void removePrivateBossBar(Player player) {
        BossBar bossBar = playerBossBars.get(player);
        if (bossBar != null) {
            bossBar.removeAll();
            playerBossBars.remove(player);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }}
    public  void ZoneCharging(DungEvent de,List<Player> lp) {
        ZoneChargingSeconds = 1;
        taskIdZone = Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, ()-> {
            for (Player isanyone:lp) {
                if (isanyone.getWorld().equals(de.getLocOfEvent().getWorld())){
                if (isanyone.getLocation().distance(de.getLocOfEvent()) <= 150){
            if (ZoneChargingSeconds > de.getTime()*4) {
                Bukkit.getScheduler().cancelTask(taskIdZone);
                for (Player p:lp) {
                    if (p.getLocation().distance(de.getLocOfEvent()) <= de.getRadius()){
                        CurrentObjectiveUpdater(p);
                    }}
                UsedDungEvents.add(de);
                de.exeeffects(thisdung);
            } else {
                spawnParticleCircle(de.getLocOfEvent(), de.getRadius(), de.getRadius()*10,Material.getMaterial(de.getParticle()));
                if (de.checkconditions(de,de.getLocOfEvent(),thisdung)){
                for (Player p:lp) {
                    if (p.getLocation().distance(de.getLocOfEvent()) <= de.getRadius()){
                        updatePrivateBossBar(p,ZoneChargingSeconds/4, de.getTime(),"Postęp: "+ZoneChargingSeconds/4+"/"+de.getTime());
                        ZoneChargingSeconds++;
                        break;
                    }else{
                        CurrentObjectiveUpdater(p);
                    }}}}}
                break;
            }}
        }, 0, 5);

    }
    public  void createTemporaryBlock(Location location, Material blockType, int durationTicks) {
        World world = location.getWorld();
        if (world != null) {
            ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
            armorStand.setVisible(false);
            armorStand.setInvulnerable(true);
            armorStand.getEquipment().setHelmet(new ItemStack(blockType));
            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> armorStand.remove(), durationTicks);
        }}
        public void SpawnMobAndTP(DungEvent de, int X, int Y, int Z, List<String> MobTypes, int amount) {
            Location LocToTp = new Location(de.getLocOfEvent().getWorld(), (de.getLocOfEvent().getX()), (de.getLocOfEvent().getY()+1), (de.getLocOfEvent().getZ()));
            Location locToSpawn = LocToTp.clone();
            locToSpawn.setX(X);
            locToSpawn.setY(Y);
            locToSpawn.setZ(Z);
            Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
            int randomIndex = new Random().nextInt(MobTypes.size());
            for (Entity entity : Objects.requireNonNull(LocToTp.getWorld()).getEntities()) {
                if (entity instanceof Player &&  entity.getLocation().distance(LocToTp)<10){
                    entity.teleport(locToSpawn);
                    Player p = (Player) entity;
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 8)); // Slowness II for 5 seconds.
                }}
            for (int i = 0; i <= amount; i++) {
                example.executeCommand("mm mobs spawn " + MobTypes.get(randomIndex) + " 1 " + locToSpawn.getWorld().getName() + "," + locToSpawn.getX() + "," + locToSpawn.getY() + "," + locToSpawn.getZ());
            }
            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, ()->{
                for (Entity entity : locToSpawn.getWorld().getEntities()) {
                    if (entity.getLocation().distance(locToSpawn)<10){
                        entity.teleport(LocToTp);
                    }}
            }, 1);

}
        public void SpawnMobsForX(DungEvent de, int seconds, int amount, int Xoffset, int Zoffset, int period, List<String> MobTypes) {
        MobSPawnSeconds = 0;
        MobSpawnPeriod = period;
        Bukkit.getScheduler().cancelTask(taskIdSpawn);
        taskIdSpawn = Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, ()-> {
            MobSpawnPeriod++;
            if (MobSpawnPeriod >= period){
                MobSpawnPeriod = 0;
            if (MobSPawnSeconds >= seconds) {
                Bukkit.getScheduler().cancelTask(taskIdSpawn);
            } else {
                for (int i = 0; i < amount; i++) {
                    Random random = new Random();
                    int randomIndex = random.nextInt(MobTypes.size());
                    Location loc;
                    do {
                        double xOffset = (Math.random() * Xoffset);
                        double zOffset = (Math.random() * Zoffset);
                        double isnegative1 = (Math.random());
                        if (isnegative1 >= 0.5) {
                            xOffset = xOffset * -1;
                        }
                        double isnegative2 = (Math.random());
                        if (isnegative2 >= 0.5) {
                            zOffset = zOffset * -1;
                        }
                        loc = new Location(de.getLocOfEvent().getWorld(), (de.getLocOfEvent().getX() + xOffset), (de.getLocOfEvent().getY()+1), (de.getLocOfEvent().getZ() + zOffset));
                    }while (!loc.add(0,-1,0).getBlock().getType().equals(Material.AIR) && !loc.getBlock().getType().equals(Material.AIR) && !loc.add(0,1,0).getBlock().getType().equals(Material.AIR));
                    Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                    example.executeCommand("mm mobs spawn " + MobTypes.get(randomIndex) + " 1 " + loc.getWorld().getName() + "," + loc.getX() + "," + (loc.getY()) + "," + loc.getZ());
                }}}
            MobSPawnSeconds++;
        }, 0,  20L);

    }
    public  void DoorsMechanics(Location location1, Location location2, Material mat, int i) {
        World world = location1.getWorld();
        int minX = Math.min(location1.getBlockX(), location2.getBlockX());
        int minY = Math.min(location1.getBlockY(), location2.getBlockY());
        int minZ = Math.min(location1.getBlockZ(), location2.getBlockZ());
        int maxX = Math.max(location1.getBlockX(), location2.getBlockX());
        int maxY = Math.max(location1.getBlockY(), location2.getBlockY());
        int maxZ = Math.max(location1.getBlockZ(), location2.getBlockZ());
        for (int x = minX; x <= maxX; x++) {
        for (int y = minY; y <= maxY; y++) {
        for (int z = minZ; z <= maxZ; z++) {
            Block block = world.getBlockAt(x, y, z);
            if (i==1){
            if (block.getType().equals(Material.AIR)){
                block.setType(mat);
            }}else{if (block.getType().equals(mat)) {
                block.setType(org.bukkit.Material.AIR);
            }}}}}}
    public void ResetActive(List<Player> PList){
        for (Location loc:this.DoorsList.keySet()) {
            Doors d1 = DoorsList.get(loc);
            DoorsMechanics(d1.l1,d1.l2,d1.m,1);
            }
        String st = RollbackCommand.replaceAll("%Location%", (int)RollBackLoc.getX() + " " + ((int)RollBackLoc.getY()) + " " + (int)RollBackLoc.getZ() +" "+DungStartLoc.getWorld().getName());
        Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
        System.out.println(st);
        example.executeCommand(st);
        UsedDungEvents.clear();
        TempZone.clear();
        Bukkit.getScheduler().cancelTask(taskIdSpawn);
        Bukkit.getScheduler().cancelTask(taskIdZone);
        for (DungEvent de:DEList) {
            de.setActkills(0);
        }
        for (Player p:PList) {
            CurrentObjectiveUpdater(p);
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
        }
    }
     static boolean EnemiesAround(Location loc, int i, int y, List<String> stlist){
            if (i==0){
                return true;
            }
        for (Entity entity : Objects.requireNonNull(loc.getWorld()).getNearbyEntities(loc, i, y, i)) {
            if (entity.getType() == EntityType.VINDICATOR || entity.getType() == EntityType.HUSK || entity.getType() == EntityType.SKELETON || entity.getType() == EntityType.ZOMBIE){
                if (!stlist.contains(entity.getName().substring(4))){
                    return false;
                }}}
        return true;
    }
    static boolean ReqMob(Location loc, int i, int y, String st){
        for (Entity entity : Objects.requireNonNull(loc.getWorld()).getNearbyEntities(loc, i, y, i)) {
            if (entity.getType() == EntityType.VINDICATOR || entity.getType() == EntityType.HUSK || entity.getType() == EntityType.SKELETON || entity.getType() == EntityType.ZOMBIE){
                if (entity.getName().substring(4).equalsIgnoreCase(st)){
                    return true;
                }}}
        return false;
    }
    public Location getDungStartLoc() {
        return DungStartLoc;
    }
    public int getXRange() {
        return XRange;
    }
    public int getZRange() {
        return ZRange;
    }
    public Location getCenterOfArena() {
        return CenterOfArena;
    }
    public String getRollbackCommand() {
        return RollbackCommand;
    }
    public Location getRollBackLoc() {
        return RollBackLoc;
    }
    public String getName() {
        return Name;
    }
    public List<DungEvent> getDEList() {
        return DEList;
    }
    public  void setCurrentObjective(String currentObjective) {
        CurrentObjective = currentObjective;
    }
    public  String getCurrentObjective() {
            return CurrentObjective;
    }
}
*/