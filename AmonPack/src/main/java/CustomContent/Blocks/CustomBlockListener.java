package CustomContent.Blocks;

import CustomContent.Items.CustomItemManager;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class CustomBlockListener implements Listener {

    private final CustomBlockManager blockManager;
    private final CustomItemManager itemManager;
    private final Random random = new Random();

    public CustomBlockListener(CustomBlockManager blockManager, CustomItemManager itemManager) {
        this.blockManager = blockManager;
        this.itemManager = itemManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String customItemId = itemManager.getCustomItemId(item);
        if (customItemId != null) {
            CustomBlock cb = blockManager.getCustomBlock(event.getBlock());
            if (cb == null) {
                // Sprawdzamy czy istnieje blok o takim samym ID
                CustomBlock match = blockManager.getAllCustomBlocks().get(customItemId);
                if (match != null) {
                    blockManager.placeBlock(event.getBlock(), match.getId());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        CustomBlock cb = blockManager.getCustomBlock(event.getBlock());
        if (cb == null) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // Sprawdzenie wymaganego poziomu narzędzia
        int playerTier = getToolTier(tool.getType());
        if (playerTier < cb.getRequiredTier()) {
            player.sendMessage("§c[AmonPack] Twoje narzędzie jest zbyt słabe, aby wydobyć ten surowiec!");
            event.setDropItems(false);
            blockManager.removeBlock(event.getBlock());
            return;
        }

        // Anulowanie standardowych dropów
        event.setDropItems(false);
        event.setExpToDrop(0);

        // Efekty dźwiękowe i cząsteczkowe
        Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 0.9f);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 25, 0.3, 0.3, 0.3, 0.1, cb.getBaseMaterial().createBlockData());

        // Wypadanie customowych dropów
        int amount = cb.getDropMin();
        if (cb.getDropMax() > cb.getDropMin()) {
            amount += random.nextInt(cb.getDropMax() - cb.getDropMin() + 1);
        }

        if (cb.getDropCustomItemId() != null) {
            ItemStack customDrop = itemManager.createItemStack(cb.getDropCustomItemId());
            if (customDrop != null) {
                customDrop.setAmount(amount);
                loc.getWorld().dropItemNaturally(loc, customDrop);
            }
        } else if (cb.getDropVanillaMaterial() != null) {
            ItemStack vanillaDrop = new ItemStack(cb.getDropVanillaMaterial(), amount);
            loc.getWorld().dropItemNaturally(loc, vanillaDrop);
        }

        // Integracja z expem górnictwa
        if (cb.getExp() > 0) {
            player.giveExp((int) cb.getExp());
        }

        blockManager.removeBlock(event.getBlock());
    }

    private int getToolTier(Material material) {
        String name = material.name();
        if (name.contains("WOODEN_") || name.contains("GOLDEN_")) return 0;
        if (name.contains("STONE_")) return 1;
        if (name.contains("IRON_")) return 2;
        if (name.contains("DIAMOND_")) return 3;
        if (name.contains("NETHERITE_")) return 4;
        return 0;
    }
}
