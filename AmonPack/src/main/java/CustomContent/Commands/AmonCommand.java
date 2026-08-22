package CustomContent.Commands;

import CustomContent.Blocks.CustomBlock;
import CustomContent.Blocks.CustomBlockManager;
import CustomContent.Bosses.BossManager;
import CustomContent.Bosses.CustomBoss;
import CustomContent.Items.CustomItem;
import CustomContent.Items.CustomItemManager;
import CustomContent.Pack.PackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AmonCommand implements CommandExecutor {

    private final PackManager packManager;
    private final CustomItemManager itemManager;
    private final CustomBlockManager blockManager;
    private final BossManager bossManager;

    public AmonCommand(PackManager packManager, CustomItemManager itemManager, CustomBlockManager blockManager, BossManager bossManager) {
        this.packManager = packManager;
        this.itemManager = itemManager;
        this.blockManager = blockManager;
        this.bossManager = bossManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("amonpack.admin")) {
            sender.sendMessage("§c[AmonPack] Nie posiadasz uprawnień do tej komendy.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "item":
                handleItem(sender, args);
                break;

            case "block":
                handleBlock(sender, args);
                break;

            case "boss":
                handleBoss(sender, args);
                break;

            case "pack":
                handlePack(sender, args);
                break;

            case "reload":
                packManager.load();
                itemManager.load();
                blockManager.load();
                bossManager.load();
                sender.sendMessage("§a[AmonPack] Przeładowano wszystkie konfiguracje, modele i zasoby.");
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleItem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§6Użycie: §f/amon item give <gracz> <item_id> [ilość] §7lub §f/amon item list");
            return;
        }

        if (args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§6=== Dostępne Customowe Przedmioty ===");
            for (CustomItem ci : itemManager.getAllItems().values()) {
                sender.sendMessage(" §7- §e" + ci.getId() + " §7(" + ci.getDisplayName() + "§7)");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("give")) {
            if (args.length < 4) {
                sender.sendMessage("§cPodaj gracza i ID przedmiotu: /amon item give <gracz> <item_id> [ilość]");
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cGracz offline.");
                return;
            }
            String itemId = args[3];
            int amount = 1;
            if (args.length >= 5) {
                try { amount = Integer.parseInt(args[4]); } catch (Exception ignored) {}
            }

            ItemStack stack = itemManager.createItemStack(itemId);
            if (stack == null) {
                sender.sendMessage("§cNie znaleziono przedmiotu o ID: " + itemId);
                return;
            }
            stack.setAmount(amount);
            target.getInventory().addItem(stack);
            sender.sendMessage("§a[AmonPack] Przyznano " + amount + "x " + itemId + " dla " + target.getName());
            target.sendMessage("§a[AmonPack] Otrzymałeś " + amount + "x " + stack.getItemMeta().getDisplayName());
        }
    }

    private void handleBlock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§6Użycie: §f/amon block give <gracz> <block_id> [ilość] §7lub §f/amon block list");
            return;
        }

        if (args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§6=== Dostępne Customowe Bloki ===");
            for (CustomBlock cb : blockManager.getAllCustomBlocks().values()) {
                sender.sendMessage(" §7- §e" + cb.getId() + " §7(" + cb.getDisplayName() + "§7)");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("give")) {
            if (args.length < 4) {
                sender.sendMessage("§cPodaj gracza i ID bloku: /amon block give <gracz> <block_id> [ilość]");
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cGracz offline.");
                return;
            }
            String blockId = args[3];
            int amount = 1;
            if (args.length >= 5) {
                try { amount = Integer.parseInt(args[4]); } catch (Exception ignored) {}
            }

            CustomBlock cb = blockManager.getAllCustomBlocks().get(blockId.toLowerCase());
            if (cb == null) {
                sender.sendMessage("§cNie znaleziono bloku o ID: " + blockId);
                return;
            }

            ItemStack blockItem = itemManager.createItemStack(blockId);
            if (blockItem == null) {
                blockItem = new ItemStack(cb.getBaseMaterial());
                var meta = blockItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(cb.getDisplayName());
                    meta.setCustomModelData(cb.getCustomModelData());
                    meta.getPersistentDataContainer().set(CustomItemManager.ITEM_KEY, org.bukkit.persistence.PersistentDataType.STRING, cb.getId());
                    blockItem.setItemMeta(meta);
                }
            }
            blockItem.setAmount(amount);
            target.getInventory().addItem(blockItem);
            sender.sendMessage("§a[AmonPack] Przyznano " + amount + "x blok " + blockId + " dla " + target.getName());
        }
    }

    private void handleBoss(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§6Użycie: §f/amon boss spawn <boss_id> §7| §f/amon boss killall §7| §f/amon boss list");
            return;
        }

        if (args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§6=== Dostępne Szablony Bossów ===");
            for (CustomBoss cb : bossManager.getAllBosses().values()) {
                sender.sendMessage(" §7- §e" + cb.getId() + " §7(" + cb.getDisplayName() + "§7)");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("killall")) {
            bossManager.killAllBosses();
            sender.sendMessage("§a[AmonPack] Usunięto wszystkich aktywnych bossów ze świata gry.");
            return;
        }

        if (args[1].equalsIgnoreCase("spawn")) {
            if (args.length < 3) {
                sender.sendMessage("§cPodaj ID bossa: /amon boss spawn <boss_id>");
                return;
            }
            String bossId = args[2];
            Location loc = null;
            if (sender instanceof Player p) {
                loc = p.getLocation();
            } else {
                sender.sendMessage("§cMusisz być graczem lub podać koordynaty.");
                return;
            }

            var bossInstance = bossManager.spawnBoss(bossId, loc);
            if (bossInstance == null) {
                sender.sendMessage("§cNie znaleziono bossa o ID: " + bossId);
            }
        }
    }

    private void handlePack(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§6Użycie: §f/amon pack build §7| §f/amon pack apply [gracz] §7| §f/amon pack url");
            return;
        }

        if (args[1].equalsIgnoreCase("build")) {
            sender.sendMessage("§e[AmonPack] Rozpoczynanie przebudowy ResourcePacka...");
            packManager.buildResourcePack();
            sender.sendMessage("§a[AmonPack] ResourcePack został pomyślnie przebudowany!");
            return;
        }

        if (args[1].equalsIgnoreCase("url")) {
            String url = (sender instanceof Player p) ? packManager.getPackDownloadUrl(p) : packManager.getPackDownloadUrl();
            sender.sendMessage("§6[AmonPack] URL ResourcePacka: §f" + url);
            return;
        }

        if (args[1].equalsIgnoreCase("apply")) {
            if (args.length >= 3) {
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    packManager.applyToPlayer(target);
                    sender.sendMessage("§a[AmonPack] Wysłano paczkę do gracza " + target.getName());
                } else {
                    sender.sendMessage("§cGracz offline.");
                }
            } else if (sender instanceof Player p) {
                packManager.applyToPlayer(p);
                sender.sendMessage("§a[AmonPack] Wysłano paczkę zasobów do Ciebie.");
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Komendy AmonPack Custom Content ===");
        sender.sendMessage(" §e/amon item give <gracz> <item_id> [ilość] §7- Przywołuje customowy przedmiot/broń");
        sender.sendMessage(" §e/amon item list §7- Lista customowych itemów");
        sender.sendMessage(" §e/amon block give <gracz> <block_id> [ilość] §7- Przywołuje customowy blok/rudę");
        sender.sendMessage(" §e/amon block list §7- Lista customowych bloków");
        sender.sendMessage(" §e/amon boss spawn <boss_id> §7- Przywołuje customowego bossa 3D");
        sender.sendMessage(" §e/amon boss killall §7- Usuwa wszystkich bossów");
        sender.sendMessage(" §e/amon pack build §7- Kompiluje paczkę .bbmodel/.json do ZIP");
        sender.sendMessage(" §e/amon pack apply [gracz] §7- Wysyła paczkę graczowi");
        sender.sendMessage(" §e/amon reload §7- Przeładowuje konfiguracje");
    }
}
