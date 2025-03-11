package Mechanics.PVE;

import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.util.Objects;
import java.util.Random;

public class SimpleWorldGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);
        return chunkData;
    }
    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }

    public static void createAndSaveTemporaryWorld(Player player, String folder, String name, Location loc) {
        String worldName = "MultiWorlds/" + folder + "/" + name;
        if (Bukkit.getWorld(worldName) == null) {
            if (!loadExistingWorld(worldName)) {
                System.out.println("Creating new world...");
                CreateWorld(worldName);
            }}
        if (loc == null) {
            player.teleport(new Location(Bukkit.getWorld(worldName), 0, 64, 0));
        } else {
            player.teleport(loc);
        }
    }

    public static void createAndSaveWorldOnLoad(String worldName) {
        if (Bukkit.getWorld(worldName) == null) {
            if (!loadExistingWorld(worldName)) {
                CreateWorld(worldName);
            }}}

    public static void CreateWorld(String worldName) {
        World temporaryWorld = Bukkit.createWorld(new WorldCreator(worldName).generator(new SimpleWorldGenerator()));
        temporaryWorld.setAutoSave(true);
        temporaryWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        temporaryWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        temporaryWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        temporaryWorld.setSpawnFlags(false, false);
        new Location(temporaryWorld, 0, 61, 0).getBlock().setType(Material.STONE);
        new Location(temporaryWorld, 1, 61, 0).getBlock().setType(Material.STONE);
        new Location(temporaryWorld, -1, 61, 0).getBlock().setType(Material.STONE);
        new Location(temporaryWorld, 0, 61, 1).getBlock().setType(Material.STONE);
        new Location(temporaryWorld, 0, 61, -1).getBlock().setType(Material.STONE);
    }

    public static boolean loadExistingWorld(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists() && new File(worldFolder, "level.dat").exists()) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new SimpleWorldGenerator());
            World world = creator.createWorld();
            return world != null;
        }
        return false;
    }

    public static void loadAllWorlds() {
        String world = AmonPackPlugin.getPvPConfig().getString("AmonPack.PvP.Loc.World");
        loadExistingWorld(world);
        for(String key : Objects.requireNonNull(AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining")).getKeys(false)) {
            String World = AmonPackPlugin.getMinesConfig().getString("AmonPack.Mining." + key + ".World");
            loadExistingWorld(World);
        }
        File baseFolder = new File(Bukkit.getWorldContainer(), "MultiWorlds");
        if (baseFolder.exists() && baseFolder.isDirectory()) {
            for (File folder : baseFolder.listFiles()) {
                if (folder.isDirectory()) {
                    for (File worldFolder : folder.listFiles()) {
                        if (worldFolder.isDirectory() && new File(worldFolder, "level.dat").exists()) {
                            String worldName = "MultiWorlds/" + folder.getName() + "/" + worldFolder.getName();
                            loadExistingWorld(worldName);
                        }}}}}}

}
