package RPG.Crafting.Objects;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import Plugin.AmonPackPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Craftable_Tool extends ItemMold {
    public Craftable_Tool(String weaponID, List<ItemStack> itemsRequiredToShapeMold, String itemName,
            Material itemMaterial, List<String> itemLore, Integer customModelID,
            List<MagicEffects> allowedMagicEffects) {
        super(weaponID, itemsRequiredToShapeMold, itemName, itemMaterial, itemLore, customModelID, allowedMagicEffects,
                ItemType.TOOL);
    }
    public void Effects(ItemStack item, Player player, Block b){
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            for (MagicEffects effects : MagicEffects.deserializeList(data)){
                effects.ExecuteTools(player,b);
            }
        }
    }
}
