package RPG.BattleRoyale;

import CustomContent.Guns.AmmoType;
import CustomContent.Guns.GunType;
import CustomContent.Guns.GunUniqueMod;
import RPG.BattleRoyale.Events.BattleRoyaleEvent;
import RPG.BattleRoyale.Events.HydrationManager;
import RPG.BattleRoyale.GroundLoot.GroundLootManager;
import RPG.BattleRoyale.Items.BandageHandler;
import RPG.BattleRoyale.Loot.ThemedChestType;
import RPG.BattleRoyale.Weapons.BattleRoyaleWeaponHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BattleRoyaleCommand implements CommandExecutor, TabCompleter {

    private final BattleRoyaleManager manager;

    public BattleRoyaleCommand(BattleRoyaleManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start":
            case "create":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Tylko gracz może utworzyć lobby!");
                    return true;
                }
                manager.createLobby(player);
                return true;

            case "join":
                if (!(sender instanceof Player pJoin)) {
                    sender.sendMessage(ChatColor.RED + "Tylko gracz może dołączyć do gry!");
                    return true;
                }
                BattleRoyaleLobby lobby = manager.getCurrentLobby();
                if (lobby == null) {
                    pJoin.sendMessage(ChatColor.RED + "[BattleRoyale] Obecnie nie ma otwartego lobby! Użyj /hungergames start.");
                    return true;
                }
                lobby.addPlayer(pJoin);
                return true;

            case "leave":
                if (!(sender instanceof Player pLeave)) {
                    sender.sendMessage(ChatColor.RED + "Tylko gracz może opuścić grę!");
                    return true;
                }
                if (manager.getCurrentLobby() != null && manager.getCurrentLobby().containsPlayer(pLeave.getUniqueId())) {
                    manager.getCurrentLobby().removePlayer(pLeave);
                    return true;
                }
                BattleRoyaleGame game = manager.getGameByPlayer(pLeave);
                if (game != null) {
                    game.handlePlayerLeave(pLeave);
                    pLeave.sendMessage(ChatColor.YELLOW + "[BattleRoyale] Opuściłeś trwający mecz.");
                    return true;
                }
                pLeave.sendMessage(ChatColor.RED + "[BattleRoyale] Nie jesteś w żadnym lobby ani grze!");
                return true;

            case "forcestart":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (manager.getCurrentLobby() != null) {
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Wymuszono natychmiastowy start gry!");
                    manager.getCurrentLobby().forceStart();
                } else if (sender instanceof Player pHost) {
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Tworzenie i natychmiastowy start gry...");
                    manager.createLobby(pHost);
                    if (manager.getCurrentLobby() != null) {
                        manager.getCurrentLobby().forceStart();
                    }
                }
                return true;

            case "stop":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (manager.getCurrentLobby() != null) {
                    manager.getCurrentLobby().cancelLobby("Zatrzymano przez administratora.");
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zatrzymano lobby.");
                } else if (manager.getCurrentGame() != null) {
                    manager.getCurrentGame().endGame();
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zatrzymano aktywny mecz.");
                } else {
                    sender.sendMessage(ChatColor.RED + "[BattleRoyale] Brak aktywnej gry lub lobby do zatrzymania.");
                }
                return true;

            case "reload":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                manager.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Przeładowano plik battleroyale.yml pomyślnie!");
                return true;

            case "givecure":
                Player targetCure = getTargetPlayer(sender, args, 1);
                if (targetCure == null) return true;

                BattleRoyaleArena arena = manager.getDefaultArena();
                ItemStack cure = BattleRoyaleWeaponHelper.createInfectionCure(
                        arena.getCureMaterial(),
                        arena.getCureDisplayName(),
                        arena.getCureLore(),
                        arena.getCureCustomModelData()
                );
                targetCure.getInventory().addItem(cure);
                sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Nadano Lekarstwo na Infekcję graczowi " + targetCure.getName());
                return true;

            case "givegun":
                Player targetGun = getTargetPlayer(sender, args, 1);
                if (targetGun == null) return true;

                GunType gt = GunType.FLINTLOCK_PISTOL;
                if (args.length >= 3) {
                    gt = GunType.fromId(args[2]);
                    if (gt == null) gt = GunType.FLINTLOCK_PISTOL;
                }
                int lvl = 1;
                if (args.length >= 4) {
                    try { lvl = Integer.parseInt(args[3]); } catch (Exception ignored) {}
                }
                ItemStack gun = BattleRoyaleWeaponHelper.createGun(gt, lvl, false, false, false, false, GunUniqueMod.NONE);
                targetGun.getInventory().addItem(gun);
                sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Nadano broń " + gt.getDisplayName() + " [Poz. " + lvl + "] graczowi " + targetGun.getName());
                return true;

            case "giveammo":
                Player targetAmmo = getTargetPlayer(sender, args, 1);
                if (targetAmmo == null) return true;

                AmmoType at = AmmoType.LEAD_BULLET;
                if (args.length >= 3) {
                    at = AmmoType.fromId(args[2]);
                    if (at == null) at = AmmoType.LEAD_BULLET;
                }
                int amount = 16;
                if (args.length >= 4) {
                    try { amount = Integer.parseInt(args[3]); } catch (Exception ignored) {}
                }
                ItemStack ammo = BattleRoyaleWeaponHelper.createAmmo(at, amount);
                targetAmmo.getInventory().addItem(ammo);
                sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Nadano " + amount + "x " + at.getDisplayName() + " graczowi " + targetAmmo.getName());
                return true;

            case "giveupgrade":
                Player targetUpg = getTargetPlayer(sender, args, 1);
                if (targetUpg == null) return true;

                int tier = 1;
                if (args.length >= 3) {
                    try { tier = Integer.parseInt(args[2]); } catch (Exception ignored) {}
                }
                ItemStack kit = BattleRoyaleWeaponHelper.createUpgradeKit(tier);
                targetUpg.getInventory().addItem(kit);
                sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Nadano Zestaw Ulepszenia Broni [★ Tier " + tier + "] graczowi " + targetUpg.getName());
                return true;

            case "givebandage":
                Player targetBandage = getTargetPlayer(sender, args, 1);
                if (targetBandage == null) return true;
                int bAmount = 2;
                if (args.length >= 3) {
                    try { bAmount = Integer.parseInt(args[2]); } catch (Exception ignored) {}
                }
                targetBandage.getInventory().addItem(BandageHandler.createBandage(bAmount));
                sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Nadano " + bAmount + "x Bandaż Medyczny graczowi " + targetBandage.getName());
                return true;

            case "givewater":
                Player targetWater = getTargetPlayer(sender, args, 1);
                if (targetWater == null) return true;
                int wAmount = 1;
                if (args.length >= 3) {
                    try { wAmount = Integer.parseInt(args[2]); } catch (Exception ignored) {}
                }
                targetWater.getInventory().addItem(HydrationManager.createWaterBottle(wAmount));
                sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Nadano " + wAmount + "x Butelka Czystej Wody graczowi " + targetWater.getName());
                return true;

            case "wand":
            case "tool":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (!(sender instanceof Player pWand)) {
                    sender.sendMessage(ChatColor.RED + "Tylko gracz może otrzymać różdżkę!");
                    return true;
                }
                pWand.getInventory().addItem(GroundLootManager.createAdminWand());
                pWand.sendMessage(ChatColor.GREEN + "[BattleRoyale] Otrzymałeś Różdżkę Budowy Mapy BR!");
                pWand.sendMessage(ChatColor.YELLOW + "PPM na blok: " + ChatColor.WHITE + "Tworzy Ground Loot / Skrzynię tematyczną");
                pWand.sendMessage(ChatColor.YELLOW + "Kucnij + PPM: " + ChatColor.WHITE + "Zmienia kategorię lootu");
                pWand.sendMessage(ChatColor.RED + "LPM na blok: " + ChatColor.WHITE + "Usuwa punkt lootu lub skrzynię");
                return true;

            case "event":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames event <NONE|SILENCE|DEHYDRATION>");
                    return true;
                }
                BattleRoyaleEvent ev;
                try {
                    ev = BattleRoyaleEvent.valueOf(args[1].toUpperCase());
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Nieznane wydarzenie! Dostępne: NONE, SILENCE, DEHYDRATION");
                    return true;
                }
                BattleRoyaleGame curGame = manager.getCurrentGame();
                if (curGame != null) {
                    curGame.setCurrentEvent(ev);
                    curGame.broadcastMessage(ChatColor.GOLD + "========================================");
                    curGame.broadcastMessage(ChatColor.YELLOW + "★ Administrator ustawił wydarzenie: " + ChatColor.BOLD + ev.getDisplayName());
                    curGame.broadcastMessage(ChatColor.GRAY + "   " + ev.getDescription());
                    curGame.broadcastMessage(ChatColor.GOLD + "========================================");
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Ustawiono aktywne wydarzenie na: " + ev.name());
                } else {
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Ustawiono wydarzenie na kolejny mecz: " + ev.name());
                }
                return true;

            case "chest":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames chest <set|remove|list> [FOOD|MEDICAL|GUNS|MAGIC|MELEE|MIXED|RANDOM]");
                    return true;
                }
                String chestSub = args[1].toLowerCase();
                if (chestSub.equals("set")) {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage(ChatColor.RED + "Tylko gracz może użyć tej komendy celując w blok!");
                        return true;
                    }
                    Block targetBlock = p.getTargetBlockExact(5);
                    if (targetBlock == null) {
                        p.sendMessage(ChatColor.RED + "[BattleRoyale] Spójrz na skrzynię/beczkę w zasięgu 5 bloków!");
                        return true;
                    }
                    String tStr = args.length >= 3 ? args[2] : "MIXED";
                    ThemedChestType tcType = ThemedChestType.fromString(tStr);
                    manager.getLootManager().setThemedChest(targetBlock.getLocation(), tcType);
                    manager.saveConfig();
                    p.sendMessage(ChatColor.GREEN + "[BattleRoyale] Pomyślnie oznaczono skrzynię jako: " + tcType.getDisplayName() + " [" + tcType.name() + "]");
                    return true;
                } else if (chestSub.equals("remove")) {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage(ChatColor.RED + "Tylko gracz może użyć tej komendy celując w blok!");
                        return true;
                    }
                    Block targetBlock = p.getTargetBlockExact(5);
                    if (targetBlock == null) {
                        p.sendMessage(ChatColor.RED + "[BattleRoyale] Spójrz na blok w zasięgu 5 bloków!");
                        return true;
                    }
                    boolean rem = manager.getLootManager().removeThemedChest(targetBlock.getLocation());
                    if (rem) {
                        manager.saveConfig();
                        p.sendMessage(ChatColor.GREEN + "[BattleRoyale] Usunięto motyw ze wskazanej skrzyni.");
                    } else {
                        p.sendMessage(ChatColor.RED + "[BattleRoyale] Ten blok nie był zarejestrowany jako skrzynia tematyczna.");
                    }
                    return true;
                } else if (chestSub.equals("list")) {
                    sender.sendMessage(ChatColor.GOLD + "=== Zarejestrowane Skrzynie Tematyczne ===");
                    Map<Location, ThemedChestType> chests = manager.getLootManager().getConfiguredThemedChests();
                    if (chests.isEmpty()) {
                        sender.sendMessage(ChatColor.GRAY + "Brak zapisanych skrzyń tematycznych.");
                    } else {
                        for (Map.Entry<Location, ThemedChestType> entry : chests.entrySet()) {
                            Location l = entry.getKey();
                            sender.sendMessage(ChatColor.YELLOW + " • [" + entry.getValue().name() + "] " + ChatColor.WHITE + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
                        }
                    }
                    return true;
                }
                return true;

            case "groundloot":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames groundloot <add|remove|list|tool> [kategoria]");
                    return true;
                }
                String glSub = args[1].toLowerCase();
                if (glSub.equals("add")) {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage(ChatColor.RED + "Tylko gracz może użyć tej komendy!");
                        return true;
                    }
                    String cat = args.length >= 3 ? args[2].toUpperCase() : "RANDOM";
                    Location loc = p.getLocation();
                    Block tb = p.getTargetBlockExact(5);
                    if (tb != null) {
                        loc = tb.getLocation().add(0, 1, 0);
                    }
                    manager.getGroundLootManager().addPoint(loc, cat);
                    manager.saveConfig();
                    p.sendMessage(ChatColor.GREEN + "[BattleRoyale] Dodano punkt Ground Loot (" + cat + ") na koordynatach: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                    return true;
                } else if (glSub.equals("remove")) {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage(ChatColor.RED + "Tylko gracz może użyć tej komendy!");
                        return true;
                    }
                    Location loc = p.getLocation();
                    Block tb = p.getTargetBlockExact(5);
                    if (tb != null) loc = tb.getLocation();
                    boolean rem = manager.getGroundLootManager().removePoint(loc) || manager.getGroundLootManager().removePoint(loc.clone().add(0, 1, 0));
                    if (rem) {
                        manager.saveConfig();
                        p.sendMessage(ChatColor.GREEN + "[BattleRoyale] Usunięto punkt Ground Loot!");
                    } else {
                        p.sendMessage(ChatColor.RED + "[BattleRoyale] Nie znaleziono punktu Ground Loot w tym miejscu.");
                    }
                    return true;
                } else if (glSub.equals("list")) {
                    sender.sendMessage(ChatColor.GOLD + "=== Zarejestrowane Punkty Ground Loot ===");
                    List<GroundLootManager.GroundLootPoint> points = manager.getGroundLootManager().getConfiguredPoints();
                    if (points.isEmpty()) {
                        sender.sendMessage(ChatColor.GRAY + "Brak zapisanych punktów Ground Loot.");
                    } else {
                        for (GroundLootManager.GroundLootPoint p : points) {
                            Location l = p.getLocation();
                            sender.sendMessage(ChatColor.YELLOW + " • [" + p.getCategory() + "] " + ChatColor.WHITE + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
                        }
                    }
                    return true;
                } else if (glSub.equals("tool")) {
                    if (sender instanceof Player p) {
                        p.getInventory().addItem(GroundLootManager.createAdminWand());
                        p.sendMessage(ChatColor.GREEN + "[BattleRoyale] Otrzymałeś Różdżkę Budowy Mapy BR!");
                    }
                    return true;
                }
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private Player getTargetPlayer(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player p = Bukkit.getPlayer(args[index]);
            if (p != null) return p;
            sender.sendMessage(ChatColor.RED + "Gracz '" + args[index] + "' nie został znaleziony online!");
            return null;
        }
        if (sender instanceof Player p) {
            return p;
        }
        sender.sendMessage(ChatColor.RED + "Podaj nazwę gracza docelowego!");
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== " + ChatColor.AQUA + "BATTLEROYALE / ZOMBIE APOCALYPSE" + ChatColor.GOLD + " ==========");
        sender.sendMessage(ChatColor.YELLOW + "/hungergames start " + ChatColor.GRAY + "- Tworzy lobby gry (czas dołączenia: 60s)");
        sender.sendMessage(ChatColor.YELLOW + "/hungergames join " + ChatColor.GRAY + "- Dołącza do otwartego lobby gry");
        sender.sendMessage(ChatColor.YELLOW + "/hungergames leave " + ChatColor.GRAY + "- Opuszcza obecne lobby lub mecz");
        if (sender.hasPermission("amonpack.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/hungergames wand " + ChatColor.GRAY + "- Daje Różdżkę Budowy Mapy BR (PPM/LPM/Shift)");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames chest <set|remove|list> " + ChatColor.GRAY + "- Skrzynie tematyczne (Food, Med, Guns...)");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames groundloot <add|remove|list> " + ChatColor.GRAY + "- Niewidzialne ramki z łupem");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames event <NONE|SILENCE|DEHYDRATION> " + ChatColor.GRAY + "- Ustawia wydarzenie mapy");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames givebandage [gracz] [ilość] " + ChatColor.GRAY + "- Daje Bandaże Medyczne");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames givewater [gracz] [ilość] " + ChatColor.GRAY + "- Daje Butelkę Czystej Wody");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames givecure [gracz] " + ChatColor.GRAY + "- Daje Lekarstwo na Infekcję");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames givegun [gracz] [typ] [poz] " + ChatColor.GRAY + "- Daje broń palną");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames giveammo [gracz] [typ] [ilość] " + ChatColor.GRAY + "- Daje amunicję");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames giveupgrade [gracz] [tier] " + ChatColor.GRAY + "- Daje zestaw ulepszenia broni");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames forcestart " + ChatColor.GRAY + "- Natychmiastowy start gry");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames stop " + ChatColor.GRAY + "- Zatrzymuje obecną grę/lobby");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames reload " + ChatColor.GRAY + "- Przeładowuje battleroyale.yml");
        }
        sender.sendMessage(ChatColor.GOLD + "=====================================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("start", "join", "leave"));
            if (sender.hasPermission("amonpack.admin")) {
                subs.addAll(Arrays.asList("wand", "tool", "chest", "groundloot", "event", "givebandage", "givewater",
                        "givecure", "givegun", "giveammo", "giveupgrade", "forcestart", "stop", "reload"));
            }
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.startsWith("give")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());
                return filter(players, args[1]);
            }
            if (sub.equals("event")) {
                return filter(Arrays.asList("NONE", "SILENCE", "DEHYDRATION"), args[1]);
            }
            if (sub.equals("chest")) {
                return filter(Arrays.asList("set", "remove", "list"), args[1]);
            }
            if (sub.equals("groundloot")) {
                return filter(Arrays.asList("add", "remove", "list", "tool"), args[1]);
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("chest") && args[1].equalsIgnoreCase("set")) {
                List<String> types = new ArrayList<>();
                for (ThemedChestType t : ThemedChestType.values()) types.add(t.name());
                return filter(types, args[2]);
            }
            if (sub.equals("groundloot") && args[1].equalsIgnoreCase("add")) {
                return filter(Arrays.asList("RANDOM", "GUNS", "AMMO", "MEDICAL", "FOOD", "MELEE", "UPGRADE"), args[2]);
            }
            if (sub.equals("givegun")) {
                List<String> guns = new ArrayList<>();
                for (GunType gt : GunType.values()) guns.add(gt.getId());
                return filter(guns, args[2]);
            }
            if (sub.equals("giveammo")) {
                List<String> ammos = new ArrayList<>();
                for (AmmoType at : AmmoType.values()) ammos.add(at.getId());
                return filter(ammos, args[2]);
            }
            if (sub.equals("giveupgrade")) {
                return filter(Arrays.asList("1", "2", "3"), args[2]);
            }
            if (sub.equals("givebandage") || sub.equals("givewater")) {
                return filter(Arrays.asList("1", "2", "5", "10"), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        List<String> res = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(input.toLowerCase())) {
                res.add(s);
            }
        }
        return res;
    }
}
