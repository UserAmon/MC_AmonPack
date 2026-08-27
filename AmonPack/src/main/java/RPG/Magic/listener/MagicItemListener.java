package RPG.Magic.listener;

import CustomContent.Items.CustomItemManager;
import RPG.Magic.gui.*;
import RPG.Magic.manager.MagicItemManager;
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

        // Jeśli gracz klika w blok Ołtarza Arkanów lub Magicznego Stołu, pozwalamy zadziałać CustomBlockListener
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            org.bukkit.block.Block b = event.getClickedBlock();
            if (b.getType() == Material.NOTE_BLOCK || b.getType() == Material.ENCHANTING_TABLE) {
                // Jeśli to interakcja z blokiem specjalnym, CustomBlockListener zajmie się otwarciem Ołtarza lub Stołu
                return;
            }
        }

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            player.openInventory(new TomeSpellGui(player, item, spellRegistry, manaManager).getInventory());
            return;
        }

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);

            String spellId;
            if (player.isSneaking()) {
                spellId = MagicItemManager.getSecondarySpellId(item);
                if (spellId == null || spellId.equalsIgnoreCase("none")) {
                    spellId = MagicItemManager.getPrimarySpellId(item);
                }
            } else {
                spellId = MagicItemManager.getPrimarySpellId(item);
            }

            Spell spell = spellRegistry.getSpell(spellId);
            if (spell != null) {
                spell.cast(player, item, manaManager);
            } else {
                player.sendMessage("§cBrak przypisanego zaklęcia!");
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem != null && isMagicTome(newItem)) {
            String spellId = MagicItemManager.getPrimarySpellId(newItem);
            Spell spell = spellRegistry.getSpell(spellId);
            String name = spell != null ? spell.getName() : "Fire Blast";
            manaManager.sendManaBarHud(player, name);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof TomeSpellGui gui) {
            gui.handleClick(event);
        } else if (event.getInventory().getHolder() instanceof PrimarySpellSelectGui gui) {
            gui.handleClick(event);
        } else if (event.getInventory().getHolder() instanceof SecondarySpellSelectGui gui) {
            gui.handleClick(event);
        } else if (event.getInventory().getHolder() instanceof ArcaneAltarGui gui) {
            gui.handleClick(event);
        } else if (event.getInventory().getHolder() instanceof SpellUpgradeTreeGui gui) {
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
