package Mechanics.PVE.Menagerie.Objectives;

import commands.Commands;
import org.bukkit.Location;

import java.util.Random;


public class Enemy {
    private String EnemyName;
    private String EnemyDisplayName;
    private String EnemyType;
    private Location SpawnLocation;
    private int SpawnLocationRange;
    private int SpawnChance;
    private int Amount;
    private int MaxLvl;
    private int SpawnedAmount;
    private Commands.ExecuteCommandExample command;
    private int MaxSpawned;


    public Enemy(String enemyName,String enemyDName, String enemyType, Location spawnLocation, int spawnLocationRange, int spawnChance, int amount, int maxLvl) {
        EnemyName = enemyName;
        EnemyDisplayName = enemyDName;
        EnemyType = enemyType;
        SpawnLocation = spawnLocation;
        SpawnLocationRange = spawnLocationRange;
        SpawnChance = spawnChance;
        Amount = amount;
        MaxLvl = maxLvl;
        command = new Commands.ExecuteCommandExample();
        SpawnedAmount=0;
    }

    public void Spawn(){
        SpawnedAmount=0;
        for (int i = 0; i < Amount; i++) {
        if (new Random().nextInt(100)<=SpawnChance){
        int LvL = new Random().nextInt(MaxLvl)+1;
        int offset = new Random().nextInt(SpawnLocationRange)+1;
        int offset2 = new Random().nextInt(SpawnLocationRange)+1;
        offset = new Random().nextBoolean() ? -offset : offset;
        offset2 = new Random().nextBoolean() ? -offset2 : offset2;
        command.executeCommand("mm mobs spawn -s " + EnemyName + ":"+ LvL +" "+"1"+" " + SpawnLocation.getWorld().getName() + "," + (SpawnLocation.getX()+offset) + "," + (SpawnLocation.getY()+1) + "," + (SpawnLocation.getZ()+offset2));
        SpawnedAmount++;
        }}}

    public String getEnemyDisplayName() {
        return EnemyDisplayName;
    }
    public String getEnemyType() {
        return EnemyType;
    }
    public int getSpawnedAmount() {
        return SpawnedAmount;
    }
    public int getAmount() {
        return Amount;
    }
    public void setMaxSpawned(int maxSpawned) {
        MaxSpawned = maxSpawned;
    }
    public int getMaxSpawned() {
        return MaxSpawned;
    }
}

/*




    public void Spawn(String st, int off,Commands.ExecuteCommandExample command,Location loc){
        int i = new Random().nextInt(MobMaxLevel);
        i++;
        int offset = new Random().nextInt(off);
        offset = new Random().nextBoolean() ? -offset : offset;
        command.executeCommand("mm mobs spawn -s " + st + ":"+ i +" "+"1"+" " + Defendloc.getWorld().getName() + "," + (Defendloc.getX()) + "," + (Defendloc.getY()-3) + "," + (Defendloc.getZ()));
        for (Entity entity : Defendloc.getWorld().getEntities()) {
            if (!(entity instanceof Player) &&!(entity instanceof ArmorStand) &&entity instanceof LivingEntity&& entity.getLocation().distance(Defendloc.clone().subtract(0,2,0))<3){
                entity.setInvulnerable(true);
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,10,1));
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW,40,3));
                entity.teleport(loc.add(offset,1,offset));
                entity.setFallDistance(0);
                entity.setInvulnerable(false);
            }}
    }

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

 */