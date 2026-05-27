package RPG.Dungeons;

import Plugin.SimpleWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;

public class DungeonWorldManager {

    /**
     * Generates or reuses a completely empty world for a dungeon run instance.
     * Uses SimpleWorldGenerator as the custom empty chunk generator.
     */
    public static World createDungeonWorld(String dungeonId) {
        String baseName = "dungeon_" + dungeonId.toLowerCase() + "_world_";
        int number = 1;
        
        while (true) {
            String worldName = baseName + number;
            
            // Check if this world is currently in active use
            boolean inUse = false;
            if (DungeonManager.getInstance() != null) {
                inUse = DungeonManager.getInstance().isWorldInUse(worldName);
            }
            
            if (!inUse) {
                // We can reuse or create this world name!
                World loadedWorld = Bukkit.getWorld(worldName);
                if (loadedWorld != null) {
                    // World is already loaded! Just clean and configure it.
                    configureWorldRules(loadedWorld);
                    cleanWorldEntities(loadedWorld);
                    System.out.println("[Dungeons] Ponownie uzyto załadowanego swiata lochu: " + worldName);
                    return loadedWorld;
                }
                
                // Check if directory exists on disk (unloaded world)
                File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                if (worldFolder.exists() && worldFolder.isDirectory()) {
                    // Load the existing world from disk! Very fast!
                    WorldCreator creator = new WorldCreator(worldName);
                    creator.generator(new SimpleWorldGenerator());
                    World world = creator.createWorld();
                    if (world != null) {
                        configureWorldRules(world);
                        cleanWorldEntities(world);
                        System.out.println("[Dungeons] Załadowano i uzyto ponownie swiat lochu z dysku: " + worldName);
                        return world;
                    }
                }
                
                // If neither loaded nor exists on disk, create a brand new one!
                WorldCreator creator = new WorldCreator(worldName);
                creator.generator(new SimpleWorldGenerator());
                World world = creator.createWorld();
                if (world != null) {
                    configureWorldRules(world);
                    System.out.println("[Dungeons] Utworzono nowy swiat instancji lochu: " + worldName);
                    return world;
                }
            }
            
            number++;
        }
    }

    private static void configureWorldRules(World world) {
        world.setAutoSave(false); // Do not save modifications (saves I/O and RAM)
        world.setKeepSpawnInMemory(false); // Optimize memory and prevent spawn chunk load lag
        world.setSpawnLocation(0, 60, 0); // Hardcode spawn point to prevent searching safe spawn lag
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setSpawnFlags(false, false);
        world.setTime(6000L); // Set constant noon
    }

    /**
     * Clears all entities in a world, except players.
     */
    public static void cleanWorldEntities(World world) {
        if (world == null) return;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
    }

    /**
     * Unloads the dungeon world to save memory but preserves it on disk.
     */
    public static void deleteDungeonWorld(World world) {
        if (world == null) return;
        
        String worldName = world.getName();
        
        // Clear all remaining entities (monsters, item drops) first
        cleanWorldEntities(world);
        
        // 1. Unload the world (false means do not save changes, reverting player modifications!)
        boolean unloaded = Bukkit.unloadWorld(world, false);
        if (unloaded) {
            System.out.println("[Dungeons] Pomyslnie rozladowano swiat instancji i zachowano go na dysku: " + worldName);
        } else {
            System.err.println("[Dungeons] Nie udalo sie rozladowac swiata: " + worldName);
        }
    }
}
