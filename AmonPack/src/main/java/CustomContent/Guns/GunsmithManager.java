package CustomContent.Guns;

import CustomContent.Blocks.CustomBlockManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class GunsmithManager implements Listener {

    private final CustomBlockManager blockManager;

    public GunsmithManager(CustomBlockManager blockManager) {
        this.blockManager = blockManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractGunsmithTable(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();

        // 1. Sprawdzenie Custom Block
        if (blockManager != null) {
            String blockId = blockManager.getCustomBlockAt(block.getLocation());
            if ("gunsmith_table".equalsIgnoreCase(blockId)) {
                event.setCancelled(true);
                GunsmithGui.open(player);
                return;
            }
        }

        // 2. Fallback note_block (instrument=bass, note=5)
        if (block.getType() == Material.NOTE_BLOCK && block.getBlockData() instanceof NoteBlock nb) {
            if (nb.getInstrument() == org.bukkit.Instrument.BASS_GUITAR && nb.getNote().getId() == 5) {
                event.setCancelled(true);
                GunsmithGui.open(player);
            }
        }
    }
}
