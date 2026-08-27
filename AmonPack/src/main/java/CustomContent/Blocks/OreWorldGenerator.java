package CustomContent.Blocks;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;

import java.util.Random;

public class OreWorldGenerator implements Listener {

    private final CustomBlockManager blockManager;
    private final Random random = new Random();

    public OreWorldGenerator(CustomBlockManager blockManager) {
        this.blockManager = blockManager;
    }

    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        Chunk chunk = event.getChunk();
        World world = event.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        for (CustomBlock cb : blockManager.getAllCustomBlocks().values()) {
            if (!cb.isGenerateInWorld()) continue;

            int veins = cb.getVeinsPerChunk();
            for (int v = 0; v < veins; v++) {
                int startX = random.nextInt(14) + 1;
                int startZ = random.nextInt(14) + 1;
                int rangeY = Math.max(1, cb.getMaxY() - cb.getMinY());
                int startY = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, cb.getMinY() + random.nextInt(rangeY)));

                generateVein(chunk, world, startX, startY, startZ, cb);
            }
        }
    }

    private void generateVein(Chunk chunk, World world, int localX, int startY, int localZ, CustomBlock cb) {
        int count = cb.getVeinSize();
        for (int i = 0; i < count; i++) {
            int bx = Math.max(0, Math.min(15, localX + random.nextInt(3) - 1));
            int bz = Math.max(0, Math.min(15, localZ + random.nextInt(3) - 1));
            int by = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, startY + random.nextInt(3) - 1));

            Block target = chunk.getBlock(bx, by, bz);
            if (target.getType() == Material.STONE || target.getType() == Material.DEEPSLATE || target.getType() == Material.ANDESITE || target.getType() == Material.DIORITE) {
                blockManager.placeBlock(target, cb.getId(), false, false);
            }
        }
    }
}
