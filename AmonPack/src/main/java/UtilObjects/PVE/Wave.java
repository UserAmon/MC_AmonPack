package UtilObjects.PVE;
/*
import commands.Commands;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Wave {
    private final int WaveDifficultyLvL;
    private final List<String> EnemyMobTypes;
    private double defaultmobs;
    private final int MobMaxLevel;
    private final Location Defendloc;
    private final List<Location> EnemyLocList;
    private final double PerRoundMobModyfier;
    private final double ExtraMobModyfier;
    private final int HowManyRounds;

    public Wave(int UpToDifficulty, int howmanyrounds, int mobmaxlvl, List<String> enemyMobTypes, List<Location> enemyloc, Location defendloc, int DefaultMobs, double ExtraMobChance, double MobsPerRound) {
        WaveDifficultyLvL = UpToDifficulty;
        EnemyMobTypes = enemyMobTypes;
        EnemyLocList = enemyloc;
        Defendloc = defendloc;
        MobMaxLevel=mobmaxlvl;
        defaultmobs=DefaultMobs;
        PerRoundMobModyfier=MobsPerRound;
        ExtraMobModyfier=ExtraMobChance;
        HowManyRounds =howmanyrounds;
    }

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

    public int SpawnMobAndTP(int currentround) {
        Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
        List<Location> remainingLocations = new ArrayList<>(EnemyLocList);
        int SpawnedMobs = 0;
        for (int ml = 0; ml < EnemyLocList.size(); ml++) {
                int randomIndex = new Random().nextInt(remainingLocations.size());
                Location chosenLocation = remainingLocations.remove(randomIndex);
                    for (int i = 0; i < defaultmobs; i++) {
                        int MobType = new Random().nextInt(EnemyMobTypes.size());
                        SpawnedMobs++;
                        Spawn(EnemyMobTypes.get(MobType), 4, example, chosenLocation);
                        int Chance = new Random().nextInt(100);
                        if (Chance<ExtraMobModyfier){
                            SpawnedMobs++;
                            Spawn(EnemyMobTypes.get(MobType), 4, example, chosenLocation);
                        }}

            double chance2= PerRoundMobModyfier*(HowManyRounds-(WaveDifficultyLvL-currentround));
            for (int i = 0; i < (int)chance2; i++) {
                SpawnedMobs++;
                int MobType = new Random().nextInt(EnemyMobTypes.size());
                Spawn(EnemyMobTypes.get(MobType), 4, example, chosenLocation);
            }
        }
        return SpawnedMobs;
    }

    public int getWaveDifficultyLvL() {
        return WaveDifficultyLvL;
    }
}
*/