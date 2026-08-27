package RPG.Magic.listener;

import CustomContent.Items.CustomItemManager;
import RPG.Magic.gui.TomeSpellGui;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.manager.SpellRegistry;
import RPG.Magic.model.Spell;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MagicItemListener implements Listener {

    private final SpellRegistry spellRegistry;
    private final ManaManager manaManager;
    private final CustomItemManager customItemManager;

    public MagicItemListener(SpellRegistry spellRegistry, ManaManager manaManager, CustomItemManager customItemManager) {
        this.spellRegistry = spellRegistry;
        this.manaManager = manaManager;
        this.customItemManager = customItemManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (!isMagicTome(item)) return;

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            new TomeSpellGui(player, item, spellRegistry, manaManager).open();
            return;
        }

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);

            String spellId;
            if (player.isSneaking()) {
                spellId = TomeSpellGui.getSecondarySpellId(item);
                if (spellId == null) {
                    spellId = TomeSpellGui.getPrimarySpellId(item);
                }
            } else {
                spellId = TomeSpellGui.getPrimarySpellId(item);
            }

            Spell spell = spellRegistry.getSpell(spellId);
            if (spell != null) {
                spell.cast(player, manaManager);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem != null && isMagicTome(newItem)) {
            String spellId = TomeSpellGui.getPrimarySpellId(newItem);
            Spell spell = spellRegistry.getSpell(spellId);
            String name = spell != null ? spell.getName() : "Fireblast";
            manaManager.sendManaBarHud(player, name);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof TomeSpellGui gui) {
            gui.handleClick(event);
        }
    }

    private boolean isMagicTome(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        if (customItemManager != null) {
            String customId = customItemManager.getCustomItemId(item);
            if (customId != null && (customId.equalsIgnoreCase("tome_fire") || customId.contains("tome"))) {
                return true;
            }
        }

        var meta = item.getItemMeta();
        if (meta.hasCustomModelData() && (meta.getCustomModelData() == 20001 || meta.getCustomModelData() == 10010)) {
            return true;
        }

        if (meta.hasDisplayName()) {
            String display = ChatColor.stripColor(meta.getDisplayName());
            if (display.contains("Tom Ognia") || display.contains("Tome of Fire") || display.contains("Księga Czarów")) {
                return true;
            }
        }

        return false;
    }
}
