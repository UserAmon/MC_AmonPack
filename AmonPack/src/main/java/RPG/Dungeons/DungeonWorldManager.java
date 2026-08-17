package RPG.Dungeons;

import Plugin.AmonPackPlugin;
import Plugin.SimpleWorldGenerator;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DungeonWorldManager {

    public enum InstanceState {
        PREPARING,
        READY,
        IN_USE,
        CLEANUP
    }

    public static class PreWarmedInstance {
        private final String dungeonId;
        private final String worldName;
        private final World world;
        private final Clipboard clipboard;
        private InstanceState state;

        public PreWarmedInstance(String dungeonId, String worldName, World world, Clipboard clipboard) {
            this.dungeonId = dungeonId;
            this.worldName = worldName;
            this.world = world;
            this.clipboard = clipboard;
            this.state = InstanceState.PREPARING;
        }

        public String getDungeonId() {
            return dungeonId;
        }

        public String getWorldName() {
            return worldName;
        }

        public World getWorld() {
            return world;
        }

        public Clipboard getClipboard() {
            return clipboard;
        }

        public InstanceState getState() {
            return state;
        }

        public void setState(InstanceState state) {
            this.state = state;
        }
    }

    private static final Map<String, Queue<PreWarmedInstance>> readyPools = new ConcurrentHashMap<>();
    private static final Map<String, PreWarmedInstance> activeInstances = new ConcurrentHashMap<>();

    private static synchronized int findNextAvailableSlot(String dungeonId) {
        String key = dungeonId.toLowerCase();
        int slot = 1;
        while (true) {
            String worldName = "dungeon_" + key + "_inst_" + slot;
            if (!activeInstances.containsKey(worldName)) {
                Queue<PreWarmedInstance> pool = readyPools.get(key);
                boolean inPool = false;
                if (pool != null) {
                    for (PreWarmedInstance pwi : pool) {
                        if (pwi.getWorldName().equalsIgnoreCase(worldName)) {
                            inPool = true;
                            break;
                        }
                    }
                }
                if (!inPool) {
                    return slot;
                }
            }
            slot++;
        }
    }

    /**
     * Initializes pre-warmed instance pools for all loaded dungeon templates.
     */
    public static void initPools() {
        if (DungeonManager.getInstance() == null) return;
        for (String dungId : DungeonManager.getInstance().getTemplates().keySet()) {
            ensurePreWarmed(dungId);
        }
    }

    /**
     * Ensures that at least 1 pre-warmed instance is READY for the given dungeonId.
     */
    public static synchronized void ensurePreWarmed(String dungeonId) {
        if (DungeonManager.getInstance() == null) return;
        String key = dungeonId.toLowerCase();
        Dungeon template = DungeonManager.getInstance().getTemplates().get(key);
        if (template == null) return;

        Queue<PreWarmedInstance> pool = readyPools.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        long availableOrPreparing = pool.stream().filter(inst -> inst.getState() == InstanceState.READY || inst.getState() == InstanceState.PREPARING).count();

        if (availableOrPreparing < 1) {
            int num = findNextAvailableSlot(key);
            String worldName = "dungeon_" + key + "_inst_" + num;

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                WorldCreator creator = new WorldCreator(worldName);
                creator.generator(new SimpleWorldGenerator());
                world = creator.createWorld();
            }
            if (world == null) return;

            configureWorldRules(world);
            cleanWorldEntities(world);

            Clipboard clipboard = SchematicManager.readClipboard(template.getSchematicFile(), AmonPackPlugin.plugin);
            PreWarmedInstance instance = new PreWarmedInstance(key, worldName, world, clipboard);
            pool.add(instance);

            if (clipboard != null) {
                Vector paste = template.getPasteLocation();
                int x = paste.getBlockX();
                int y = paste.getBlockY();
                int z = paste.getBlockZ();

                BlockVector3 min = clipboard.getMinimumPoint();
                BlockVector3 max = clipboard.getMaximumPoint();
                BlockVector3 origin = clipboard.getOrigin();

                int minX = x + (min.x() - origin.x());
                int maxX = x + (max.x() - origin.x());
                int minZ = z + (min.z() - origin.z());
                int maxZ = z + (max.z() - origin.z());

                int minChunkX = Math.min(minX, maxX) >> 4;
                int maxChunkX = Math.max(minX, maxX) >> 4;
                int minChunkZ = Math.min(minZ, maxZ) >> 4;
                int maxChunkZ = Math.max(minZ, maxZ) >> 4;

                List<CompletableFuture<Chunk>> futures = new ArrayList<>();
                for (int cx = minChunkX - 1; cx <= maxChunkX + 1; cx++) {
                    for (int cz = minChunkZ - 1; cz <= maxChunkZ + 1; cz++) {
                        futures.add(loadChunkAsync(world, cx, cz));
                    }
                }

                World finalWorld = world;
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                    Bukkit.getScheduler().runTask(AmonPackPlugin.plugin, () -> {
                        SchematicManager.pasteClipboard(finalWorld, clipboard, x, y, z);
                        instance.setState(InstanceState.READY);
                        System.out.println("[Dungeons] Świat instancji gotowy (READY): " + worldName);
                    });
                });
            } else {
                instance.setState(InstanceState.READY);
            }
        }
    }

    /**
     * Acquires a ready pre-warmed instance or creates one on demand.
     */
    public static synchronized PreWarmedInstance acquireInstance(String dungeonId) {
        String key = dungeonId.toLowerCase();
        Queue<PreWarmedInstance> pool = readyPools.get(key);
        if (pool != null) {
            PreWarmedInstance inst = pool.poll();
            if (inst != null && (inst.getState() == InstanceState.READY || inst.getState() == InstanceState.PREPARING)) {
                inst.setState(InstanceState.IN_USE);
                activeInstances.put(inst.getWorldName(), inst);
                cleanWorldEntities(inst.getWorld());
                Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> ensurePreWarmed(dungeonId), 20L);
                return inst;
            }
        }

        int num = findNextAvailableSlot(key);
        String worldName = "dungeon_" + key + "_inst_" + num;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new SimpleWorldGenerator());
            world = creator.createWorld();
        }
        if (world != null) {
            configureWorldRules(world);
            cleanWorldEntities(world);
        }

        Dungeon template = DungeonManager.getInstance().getTemplates().get(key);
        Clipboard clipboard = template != null ? SchematicManager.readClipboard(template.getSchematicFile(), AmonPackPlugin.plugin) : null;
        if (clipboard != null && world != null) {
            Vector paste = template.getPasteLocation();
            SchematicManager.pasteClipboard(world, clipboard, paste.getBlockX(), paste.getBlockY(), paste.getBlockZ());
        }

        PreWarmedInstance inst = new PreWarmedInstance(key, worldName, world, clipboard);
        inst.setState(InstanceState.IN_USE);
        activeInstances.put(worldName, inst);

        Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> ensurePreWarmed(dungeonId), 20L);
        return inst;
    }

    /**
     * Cleans up and recycles a finished instance world for future runs.
     */
    public static void deleteDungeonWorld(World world) {
        if (world == null) return;
        String worldName = world.getName();
        PreWarmedInstance inst = activeInstances.remove(worldName);

        cleanWorldEntities(world);

        if (inst != null) {
            String key = inst.getDungeonId();
            Dungeon template = DungeonManager.getInstance().getTemplates().get(key);
            if (template != null && inst.getClipboard() != null) {
                Vector paste = template.getPasteLocation();
                SchematicManager.pasteClipboard(world, inst.getClipboard(), paste.getBlockX(), paste.getBlockY(), paste.getBlockZ());
            }
            inst.setState(InstanceState.READY);
            Queue<PreWarmedInstance> pool = readyPools.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
            pool.add(inst);
            System.out.println("[Dungeons] Zresetowano i zwolniono świat instancji do ponownego użycia: " + worldName);
        }
    }

    private static void configureWorldRules(World world) {
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
        world.setSpawnLocation(0, 60, 0);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setSpawnFlags(false, false);
        world.setTime(6000L);
    }

    public static void cleanWorldEntities(World world) {
        if (world == null) return;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
    }

    private static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        return dir.delete();
    }

    private static CompletableFuture<Chunk> loadChunkAsync(World world, int cx, int cz) {
        try {
            java.lang.reflect.Method method = world.getClass().getMethod("getChunkAtAsync", int.class, int.class);
            Object obj = method.invoke(world, cx, cz);
            if (obj instanceof CompletableFuture) {
                return (CompletableFuture<Chunk>) obj;
            }
        } catch (Exception ignored) {
        }

        CompletableFuture<Chunk> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(AmonPackPlugin.plugin, () -> {
            future.complete(world.getChunkAt(cx, cz));
        });
        return future;
    }
}
