package RPG.Dungeons;

import Plugin.SimpleWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class DungeonWorldManager {

    /**
     * Generates a new, completely empty world for a dungeon run instance.
     * Uses SimpleWorldGenerator as the custom empty chunk generator.
     */
    public static World createDungeonWorld(String dungeonId) {
        String worldName = generateUniqueWorldName(dungeonId);
        
        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new SimpleWorldGenerator());
        
        World world = creator.createWorld();
        if (world == null) {
            System.err.println("[Dungeons] Blad podczas tworzenia swiata dla dungeonu: " + worldName);
            return null;
        }

        // Configure game rules for a controlled dungeon environment
        world.setAutoSave(false); // Do not save modifications (saves I/O and RAM)
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setSpawnFlags(false, false);
        world.setTime(6000L); // Set constant noon

        return world;
    }

    /**
     * Unloads the dungeon world and recursively deletes its directory from the server files.
     */
    public static void deleteDungeonWorld(World world) {
        if (world == null) return;
        
        String worldName = world.getName();
        
        // 1. Unload the world (false means do not save changes)
        boolean unloaded = Bukkit.unloadWorld(world, false);
        if (!unloaded) {
            System.err.println("[Dungeons] Nie udalo sie rozladowac swiata: " + worldName);
            return;
        }

        // 2. Delete the directory recursively
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists() && worldFolder.isDirectory()) {
            try {
                Files.walk(worldFolder.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                System.out.println("[Dungeons] Pomyslnie usunieto swiat instancji z dysku: " + worldName);
            } catch (IOException e) {
                System.err.println("[Dungeons] Blad podczas usuwania folderu swiata " + worldName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Finds a unique name for the dungeon instance world, e.g., dungeon_pyro_world_1.
     */
    private static String generateUniqueWorldName(String dungeonId) {
        String baseName = "dungeon_" + dungeonId.toLowerCase() + "_world_";
        int number = 1;
        
        while (new File(Bukkit.getWorldContainer(), baseName + number).exists() || Bukkit.getWorld(baseName + number) != null) {
            number++;
        }
        
        return baseName + number;
    }
}
