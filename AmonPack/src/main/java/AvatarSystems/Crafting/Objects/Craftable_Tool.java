package AvatarSystems.Crafting.Objects;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Craftable_Tool extends ItemMold {
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private int TickToBreak = 80;
    private final Map<Block, BukkitRunnable> activeMinings = new HashMap<>();

    public Craftable_Tool(String weaponID, List<ItemStack> itemsRequiredToShapeMold, String itemName,
            Material itemMaterial, List<String> itemLore, Integer customModelID,
            List<MagicEffects> allowedMagicEffects) {
        super(weaponID, itemsRequiredToShapeMold, itemName, itemMaterial, itemLore, customModelID, allowedMagicEffects,
                ItemType.TOOL);
    }

    public void startMining(Player player, Block block) {
        if (activeMinings.containsKey(block))
            return;

        BlockPosition pos = new BlockPosition(block.getX(), block.getY(), block.getZ());

        BukkitRunnable task = new BukkitRunnable() {
            int progress = 0;
            final int animationId = player.getEntityId() * -1;

            @Override
            public void run() {
                if (block.getType().isAir()) {
                    cancelMining(player, block, pos, animationId);
                    return;
                }
                int stage = (int) ((progress / (double) TickToBreak) * 9);
                sendBreakAnimation(player, pos, animationId, stage);

                if (progress >= TickToBreak) {
                    block.setType(Material.AIR); // niszczenie bloków ręcznie
                    sendBreakAnimation(player, pos, animationId, stage);
                    cancel();
                    cancelMining(player, block, pos, animationId);
                }
                progress++;
            }
        };
        task.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
        activeMinings.put(block, task);
    }

    public void cancelMining(Player player, Block block, BlockPosition pos, int animationId) {
        sendBreakAnimation(player, pos, animationId, -1); // reset animacji
        BukkitRunnable task = activeMinings.remove(block);
        if (task != null)
            task.cancel();
    }

    private void sendBreakAnimation(Player player, BlockPosition pos, int animationId, int stage) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        packet.getIntegers().write(0, animationId); // unikalne ID animacji
        packet.getBlockPositionModifier().write(0, pos);
        packet.getIntegers().write(1, stage);

        protocolManager.sendServerPacket(player, packet);
    }
}
