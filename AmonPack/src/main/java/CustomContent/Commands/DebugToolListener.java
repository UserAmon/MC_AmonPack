package CustomContent.Commands;

import CustomContent.Blocks.CustomBlock;
import CustomContent.Blocks.CustomBlockManager;
import CustomContent.Bosses.ActiveBossInstance;
import CustomContent.Bosses.BossManager;
import CustomContent.Items.CustomItem;
import CustomContent.Items.CustomItemManager;
import CustomContent.Pack.PackManager;
import Plugin.AmonPackPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class DebugToolListener implements Listener {

    public static final NamespacedKey DEBUG_TOOL_KEY = new NamespacedKey(AmonPackPlugin.plugin, "is_debug_tool");

    private final PackManager packManager;
    private final CustomItemManager itemManager;
    private final CustomBlockManager blockManager;
    private final BossManager bossManager;

    public DebugToolListener(PackManager packManager, CustomItemManager itemManager, CustomBlockManager blockManager, BossManager bossManager) {
        this.packManager = packManager;
        this.itemManager = itemManager;
        this.blockManager = blockManager;
        this.bossManager = bossManager;
    }

    public static ItemStack createDebugTool() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l[AmonPack] §6Inspektor Modeli 3D");
            meta.setLore(java.util.Arrays.asList(
                    "§7Kliknij PPM na:",
                    " §8- §fBlok §7aby sprawdzić customowy model/dane",
                    " §8- §fMoba/Bossa §7aby sprawdzić ID i CustomModelData",
                    " §8- §fPowietrze §7aby zbadać przedmiot w drugiej ręce"
            ));
            meta.getPersistentDataContainer().set(DEBUG_TOOL_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isDebugTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(DEBUG_TOOL_KEY, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = event.getItem();
        if (!isDebugTool(hand)) return;

        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            inspectBlock(player, event.getClickedBlock());
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Raytrace entity w zasięgu 12 bloków
            org.bukkit.util.RayTraceResult entityTrace = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 12.0, 0.5, e -> !e.getUniqueId().equals(player.getUniqueId()));
            if (entityTrace != null && entityTrace.getHitEntity() != null) {
                inspectEntity(player, entityTrace.getHitEntity());
                return;
            }

            // Raytrace block w zasięgu 12 bloków
            org.bukkit.util.RayTraceResult blockTrace = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 12.0);
            if (blockTrace != null && blockTrace.getHitBlock() != null) {
                inspectBlock(player, blockTrace.getHitBlock());
                return;
            }

            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand != null && offHand.getType() != Material.AIR) {
                player.sendMessage("§e=== Badanie przedmiotu w drugiej ręce (Offhand) ===");
                inspectItem(player, offHand);
            } else {
                player.sendMessage("§e=== Badanie przedmiotu w głównej ręce (Mainhand) ===");
                inspectItem(player, player.getInventory().getItemInMainHand());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isDebugTool(hand)) return;

        event.setCancelled(true);
        inspectEntity(player, event.getRightClicked());
    }

    public void inspectItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§c[Debug] Ręka jest pusta!");
            return;
        }

        player.sendMessage("§6──────── §e[AmonPack Item Debug] §6────────");
        player.sendMessage(" §7- Materiał bazowy: §f" + item.getType().name());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            player.sendMessage(" §7- Nazwa: §r" + meta.getDisplayName());

            if (meta.hasCustomModelData()) {
                int cmd = meta.getCustomModelData();
                player.sendMessage(" §7- CustomModelData: §a" + cmd);
            } else {
                player.sendMessage(" §7- CustomModelData: §cBRAK (Brak wartości!)");
            }

            String customId = meta.getPersistentDataContainer().get(CustomItemManager.ITEM_KEY, PersistentDataType.STRING);
            if (customId != null) {
                player.sendMessage(" §7- Persistent ID: §e" + customId);
                CustomItem ci = itemManager.getCustomItem(customId);
                if (ci != null) {
                    player.sendMessage(" §7- Status w configu: §aZarejestrowany w custom_items.yml");
                    player.sendMessage("   §8• §7Oczekiwany CMD: §f" + ci.getCustomModelData());
                    player.sendMessage("   §8• §7Zgodność CMD: " + (meta.hasCustomModelData() && meta.getCustomModelData() == ci.getCustomModelData() ? "§a✓ PRAWIDŁOWY" : "§c✗ NIEZGODNY"));
                } else {
                    player.sendMessage(" §7- Status w configu: §cNie znaleziono w custom_items.yml!");
                }
            } else {
                player.sendMessage(" §7- Persistent ID: §7Brak klucza amonpack:custom_item_id");
            }
        }
        player.sendMessage("§6───────────────────────────────────────");
    }

    public void inspectBlock(Player player, Block block) {
        player.sendMessage("§6──────── §e[AmonPack Block Debug] §6────────");
        player.sendMessage(" §7- Blok waniliowy: §f" + block.getType().name());
        player.sendMessage(" §7- Koordynaty: §f" + block.getX() + ", " + block.getY() + ", " + block.getZ());

        CustomBlock cb = blockManager.getCustomBlock(block);
        if (cb != null) {
            player.sendMessage(" §7- CustomBlock ID: §a" + cb.getId() + " §7(" + cb.getDisplayName() + "§7)");
            player.sendMessage(" §7- Wymagany CMD: §e" + cb.getCustomModelData());
            player.sendMessage(" §7- Dropy: §f" + (cb.getDropCustomItemId() != null ? cb.getDropCustomItemId() : cb.getDropVanillaMaterial()));
            player.sendMessage(" §7- Status: §aAktywny customowy blok w świecie");
        } else {
            player.sendMessage(" §7- Status: §7Zwykły blok waniliowy (brak wpisu w AmonPack)");
        }
        player.sendMessage("§6───────────────────────────────────────");
    }

    public void inspectEntity(Player player, Entity entity) {
        player.sendMessage("§6──────── §e[AmonPack Entity Debug] §6────────");
        player.sendMessage(" §7- Typ bytu: §f" + entity.getType().name());

        if (entity instanceof LivingEntity living) {
            ActiveBossInstance boss = bossManager.getActiveBoss(living);
            if (boss != null) {
                player.sendMessage(" §7- Custom Boss ID: §a" + boss.getTemplate().getId() + " §7(" + boss.getTemplate().getDisplayName() + "§7)");
                player.sendMessage(" §7- HP: §c" + (int)living.getHealth() + "§7/§c" + (int)boss.getTemplate().getMaxHealth());
                player.sendMessage(" §7- Boss Model CMD: §e" + boss.getTemplate().getCustomModelData());
                player.sendMessage(" §7- Skala modelu 3D: §f" + boss.getTemplate().getScale());
                player.sendMessage(" §7- Status: §aAktywny byt Bossa z modelem 3D i AI");
            } else {
                player.sendMessage(" §7- Status: §7Zwykły byt gry (brak powiązania z BossManagerem)");
            }
        }
        player.sendMessage("§6───────────────────────────────────────");
    }
}
