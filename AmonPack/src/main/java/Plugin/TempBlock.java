package Plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Niezależny system tymczasowych bloków dla efektów magii i lochów.
 * Zastępuje com.projectkorra.projectkorra.util.TempBlock.
 */
public class TempBlock {

    private static final Map<Block, TempBlock> INSTANCES = new ConcurrentHashMap<>();

    private final Block block;
    private final BlockData originalData;
    private BukkitTask revertTask;

    public TempBlock(Block block, Material newMaterial) {
        this(block, newMaterial.createBlockData());
    }

    public TempBlock(Block block, BlockData newData) {
        this.block = block;
        TempBlock existing = INSTANCES.get(block);
        if (existing != null) {
            this.originalData = existing.originalData;
            existing.cancelTask();
        } else {
            this.originalData = block.getBlockData();
        }
        block.setBlockData(newData, false);
        INSTANCES.put(block, this);
    }

    public TempBlock(Block block, BlockData newData, long revertTimeMs) {
        this(block, newData);
        setRevertTime(revertTimeMs);
    }

    public void setRevertTime(long timeMs) {
        cancelTask();
        long ticks = Math.max(1L, (timeMs / 50L));
        if (AmonPackPlugin.plugin != null && AmonPackPlugin.plugin.isEnabled()) {
            this.revertTask = Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, this::revertBlock, ticks);
        }
    }

    public void revertBlock() {
        cancelTask();
        if (INSTANCES.get(block) == this) {
            INSTANCES.remove(block);
            block.setBlockData(originalData, false);
        }
    }

    private void cancelTask() {
        if (revertTask != null) {
            try {
                revertTask.cancel();
            } catch (Exception ignored) {}
            revertTask = null;
        }
    }

    public static boolean isTempBlock(Block block) {
        return block != null && INSTANCES.containsKey(block);
    }

    public static TempBlock getTempBlock(Block block) {
        return block != null ? INSTANCES.get(block) : null;
    }

    public static void revertAll() {
        for (TempBlock tb : INSTANCES.values()) {
            tb.cancelTask();
            tb.block.setBlockData(tb.originalData, false);
        }
        INSTANCES.clear();
    }
}
