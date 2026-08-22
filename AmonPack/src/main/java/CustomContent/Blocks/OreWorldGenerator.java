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
                int startX = chunk.getX() * 16 + random.nextInt(16);
                int startZ = chunk.getZ() * 16 + random.nextInt(16);
                int rangeY = Math.max(1, cb.getMaxY() - cb.getMinY());
                int startY = cb.getMinY() + random.nextInt(rangeY);

                generateVein(world, startX, startY, startZ, cb);
            }
        }
    }

    private void generateVein(World world, int x, int y, int z, CustomBlock cb) {
        int count = cb.getVeinSize();
        for (int i = 0; i < count; i++) {
            int bx = x + random.nextInt(3) - 1;
            int by = y + random.nextInt(3) - 1;
            int bz = z + random.nextInt(3) - 1;

            Block target = world.getBlockAt(bx, by, bz);
            if (target.getType() == Material.STONE || target.getType() == Material.DEEPSLATE || target.getType() == Material.ANDESITE || target.getType() == Material.DIORITE) {
                blockManager.placeBlock(target, cb.getId());
            }
        }
    }
}
