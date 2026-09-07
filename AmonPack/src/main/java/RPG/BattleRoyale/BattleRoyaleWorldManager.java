package RPG.BattleRoyale;

import Plugin.AmonPackPlugin;
import Plugin.SimpleWorldGenerator;
import RPG.Dungeons.SchematicManager;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BattleRoyaleWorldManager {

    private final BattleRoyaleArena arena;
    private World world;
    private Clipboard clipboard;
    private final Map<Location, BlockData> modifiedBlocks = new ConcurrentHashMap<>();

    public BattleRoyaleWorldManager(BattleRoyaleArena arena) {
        this.arena = arena;
    }

    /**
     * Przygotowuje świat do meczu: ładuje świat, czyści byty, wkleja/naprawia schemat.
     */
    public World prepareWorld() {
        world = Bukkit.getWorld(arena.getWorldName());
        boolean newlyCreated = false;
        if (world == null) {
            WorldCreator creator = new WorldCreator(arena.getWorldName());
            creator.generator(new SimpleWorldGenerator());
            world = creator.createWorld();
            newlyCreated = true;
        }

        if (world != null) {
            configureWorldRules(world);
            cleanEntities(world);

            if (arena.getSchematic() != null && !arena.getSchematic().isEmpty()) {
                clipboard = SchematicManager.readClipboard(arena.getSchematic(), AmonPackPlugin.plugin);
                if (clipboard != null) {
                    Location paste = arena.getPasteLocation();
                    int px = paste != null ? paste.getBlockX() : 0;
                    int py = paste != null ? paste.getBlockY() : 100;
                    int pz = paste != null ? paste.getBlockZ() : 0;

                    if (newlyCreated) {
                        SchematicManager.pasteClipboard(world, clipboard, px, py, pz);
                    } else {
                        SchematicManager.repairClipboardDifferences(world, clipboard, px, py, pz);
                    }
                }
            }
        }
        return world;
    }

    /**
     * Resetuje świat po zakończeniu meczu: naprawia bloki, usuwa byty, czyści dropy.
     */
    public void resetWorld() {
        if (world == null) return;

        cleanEntities(world);
        restoreModifiedBlocks();

        if (clipboard != null && arena.getPasteLocation() != null) {
            Location paste = arena.getPasteLocation();
            SchematicManager.repairClipboardDifferences(world, clipboard, paste.getBlockX(), paste.getBlockY(), paste.getBlockZ());
        }
    }

    public void recordBlockChange(Block block) {
        if (block == null) return;
        modifiedBlocks.computeIfAbsent(block.getLocation(), loc -> block.getBlockData().clone());
    }

    public void restoreModifiedBlocks() {
        if (modifiedBlocks.isEmpty()) return;
        for (Map.Entry<Location, BlockData> entry : modifiedBlocks.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() != null) {
                loc.getBlock().setBlockData(entry.getValue(), false);
            }
        }
        modifiedBlocks.clear();
    }

    public void cleanEntities(World w) {
        if (w == null) return;
        for (Entity e : w.getEntities()) {
            if (!(e instanceof Player)) {
                e.remove();
            }
        }
    }

    private void configureWorldRules(World w) {
        w.setAutoSave(false);
        w.setKeepSpawnInMemory(false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, false);
        w.setGameRule(GameRule.MOB_GRIEFING, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setTime(6000L);
    }

    public World getWorld() { return world; }
}
