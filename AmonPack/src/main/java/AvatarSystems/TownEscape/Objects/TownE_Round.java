package AvatarSystems.TownEscape.Objects;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TownE_Round {
    private int NumbersOfEnemies;
    private int RoundNumber;
    private List<EntityType> EnemyTypes;
    private int TimeBetweenSpawn;
    private int HowManyLocationFrom;

    public TownE_Round(List<EntityType> enemyTypes, int numbersOfEnemies, int roundNumber, int timeBetweenSpawn,
            int howManyLocationFrom) {
        EnemyTypes = enemyTypes;
        NumbersOfEnemies = numbersOfEnemies;
        RoundNumber = roundNumber;
        TimeBetweenSpawn = timeBetweenSpawn;
        HowManyLocationFrom = howManyLocationFrom;
    }

    public List<Entity> SpawnEnemies(Location location) {
        List<Entity> spawned = new ArrayList<>();
        if (EnemyTypes.isEmpty())
            return spawned;
        Random random = new Random();
        for (int i = 0; i < NumbersOfEnemies; i++) {
            EntityType type = EnemyTypes.get(random.nextInt(EnemyTypes.size()));
            spawned.add(location.getWorld().spawnEntity(location, type));
        }
        return spawned;
    }

    public int getRoundNumber() {
        return RoundNumber;
    }

    public int getTimeBetweenSpawn() {
        return TimeBetweenSpawn;
    }

    public int getHowManyLocationFrom() {
        return HowManyLocationFrom;
    }

    public int getNumbersOfEnemies() {
        return NumbersOfEnemies;
    }
}
