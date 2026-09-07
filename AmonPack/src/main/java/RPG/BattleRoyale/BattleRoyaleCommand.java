package RPG.BattleRoyale;

import CustomContent.Guns.AmmoType;
import CustomContent.Guns.GunType;
import CustomContent.Guns.GunUniqueMod;
import RPG.BattleRoyale.Backpacks.BackpackManager;
import RPG.BattleRoyale.Events.BattleRoyaleEvent;
import RPG.BattleRoyale.Events.HydrationManager;
import RPG.BattleRoyale.GroundLoot.GroundLootManager;
import RPG.BattleRoyale.Items.BandageHandler;
import RPG.BattleRoyale.Keys.KeyType;
import RPG.BattleRoyale.Loot.ThemedChestType;
import RPG.BattleRoyale.Weapons.BattleRoyaleWeaponHelper;
import RPG.BattleRoyale.Zombies.CustomZombieManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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
                    sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames event <airdrop|blackout|bloodmoon|caralarm|NONE|SILENCE|DEHYDRATION>");
                    return true;
                }
                String evArg = args[1].toLowerCase();
                BattleRoyaleGame curGame = manager.getCurrentGame();

                if (evArg.equals("airdrop")) {
                    if (curGame == null || curGame.isEnded()) {
                        sender.sendMessage(ChatColor.RED + "[BattleRoyale] Brak aktywnego meczu do wywołania Air Dropu!");
                        return true;
                    }
                    if (sender instanceof Player p) {
                        curGame.getDynamicEventManager().triggerAirDropAt(p.getLocation());
                    } else {
                        curGame.getDynamicEventManager().triggerAirDrop(curGame.getWorld(), curGame.getArena().getInitialBorderRadius(), curGame.getArena().getCenterLocation());
                    }
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Wywołano Zrzut Zaopatrzenia (Air Drop)!");
                    return true;
                } else if (evArg.equals("blackout")) {
                    if (curGame == null || curGame.isEnded()) {
                        sender.sendMessage(ChatColor.RED + "[BattleRoyale] Brak aktywnego meczu do wywołania Blackoutu!");
                        return true;
                    }
                    if (sender instanceof Player p) {
                        curGame.getDynamicEventManager().triggerBlackoutAt(p.getLocation());
                    } else {
                        curGame.getDynamicEventManager().triggerBlackout(curGame.getWorld(), curGame.getArena().getInitialBorderRadius(), curGame.getArena().getCenterLocation());
                    }
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Wywołano Awarię Zasilania (Blackout)!");
                    return true;
                } else if (evArg.equals("bloodmoon")) {
                    if (curGame == null || curGame.isEnded()) {
                        sender.sendMessage(ChatColor.RED + "[BattleRoyale] Brak aktywnego meczu do wywołania Krwawego Księżyca!");
                        return true;
                    }
                    curGame.getDynamicEventManager().triggerBloodMoon();
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Wywołano Krwawy Księżyc (Blood Moon)!");
                    return true;
                } else if (evArg.equals("caralarm")) {
                    if (curGame == null || curGame.isEnded()) {
                        sender.sendMessage(ChatColor.RED + "[BattleRoyale] Brak aktywnego meczu!");
                        return true;
                    }
                    if (sender instanceof Player p) {
                        Location closest = null;
                        double minDist = Double.MAX_VALUE;
                        for (Location cLoc : curGame.getArena().getCarLocations()) {
                            if (cLoc.getWorld() != null && cLoc.getWorld().equals(p.getWorld())) {
                                double d = cLoc.distanceSquared(p.getLocation());
                                if (d < minDist) {
                                    minDist = d;
                                    closest = cLoc;
                                }
                            }
                        }
                        if (closest != null && minDist <= 900.0) {
                            curGame.getDynamicEventManager().triggerCarAlarm(closest);
                            sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Uruchomiono alarm najbliższego samochodu!");
                            return true;
                        }
                    }
                    curGame.getDynamicEventManager().triggerRandomCarAlarm();
                    sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Uruchomiono losowy alarm samochodowy!");
                    return true;
                }

                BattleRoyaleEvent ev;
                try {
                    ev = BattleRoyaleEvent.valueOf(args[1].toUpperCase());
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Nieznane wydarzenie! Dostępne: airdrop, blackout, bloodmoon, caralarm, NONE, SILENCE, DEHYDRATION");
                    return true;
                }
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

            case "car":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames car <add|remove|list>");
                    return true;
                }
                String carSub = args[1].toLowerCase();
                if (carSub.equals("add")) {
                    if (!(sender instanceof Player pCar)) {
                        sender.sendMessage(ChatColor.RED + "Tylko gracz może dodać lokalizację pojazdu!");
                        return true;
                    }
                    Block tb = pCar.getTargetBlockExact(5);
                    Location carLoc = tb != null ? tb.getLocation() : pCar.getLocation().getBlock().getLocation();
                    manager.getDefaultArena().getCarLocations().add(carLoc);
                    if (manager.getCurrentGame() != null) {
                        manager.getCurrentGame().getArena().getCarLocations().add(carLoc);
                    }
                    manager.saveConfig();
                    pCar.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zarejestrowano wrak pojazdu z alarmem na: " + carLoc.getBlockX() + ", " + carLoc.getBlockY() + ", " + carLoc.getBlockZ());
                    return true;
                } else if (carSub.equals("remove")) {
                    if (!(sender instanceof Player pCar)) {
                        sender.sendMessage(ChatColor.RED + "Tylko gracz może usunąć pojazd!");
                        return true;
                    }
                    Location pLoc = pCar.getLocation();
                    Location toRemove = null;
                    for (Location cl : manager.getDefaultArena().getCarLocations()) {
                        if (cl.getWorld() != null && cl.getWorld().equals(pLoc.getWorld()) && cl.distance(pLoc) < 4.0) {
                            toRemove = cl;
                            break;
                        }
                    }
                    if (toRemove != null) {
                        manager.getDefaultArena().getCarLocations().remove(toRemove);
                        if (manager.getCurrentGame() != null) {
                            manager.getCurrentGame().getArena().getCarLocations().remove(toRemove);
                        }
                        manager.saveConfig();
                        pCar.sendMessage(ChatColor.GREEN + "[BattleRoyale] Usunięto pojazd w promieniu 4 bloków!");
                    } else {
                        pCar.sendMessage(ChatColor.RED + "[BattleRoyale] Nie znaleziono zarejestrowanego pojazdu w promieniu 4 bloków.");
                    }
                    return true;
                } else if (carSub.equals("list")) {
                    sender.sendMessage(ChatColor.GOLD + "=== Zarejestrowane Pojazdy z Alarmem ===");
                    List<Location> cars = manager.getDefaultArena().getCarLocations();
                    if (cars.isEmpty()) {
                        sender.sendMessage(ChatColor.GRAY + "Brak zapisanych lokalizacji pojazdów.");
                    } else {
                        for (Location cl : cars) {
                            sender.sendMessage(ChatColor.YELLOW + " • " + ChatColor.WHITE + cl.getBlockX() + ", " + cl.getBlockY() + ", " + cl.getBlockZ());
                        }
                    }
                    return true;
                }
                return true;

            case "givekey":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                Player targetKey = getTargetPlayer(sender, args, 1);
                if (targetKey == null) return true;
                KeyType kt = KeyType.POLICE_STATION;
                if (args.length >= 3) {
                    kt = KeyType.fromString(args[2]);
                    if (kt == null) kt = KeyType.POLICE_STATION;
                }
                targetKey.getInventory().addItem(manager.getKeyManager().createKey(kt));
                sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Nadano " + kt.getDisplayName() + " graczowi " + targetKey.getName());
                return true;

            case "key":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames key <door|remove|list> [POLICE_STATION|PHARMACY|MILITARY_DEPOT|APARTMENT_204]");
                    return true;
                }
                String keySub = args[1].toLowerCase();
                if (keySub.equals("door")) {
                    if (!(sender instanceof Player pKey)) {
                        sender.sendMessage(ChatColor.RED + "Tylko gracz może zarejestrować drzwi!");
                        return true;
                    }
                    Block tb = pKey.getTargetBlockExact(5);
                    if (tb == null || tb.getType() != Material.IRON_DOOR) {
                        pKey.sendMessage(ChatColor.RED + "[BattleRoyale] Spójrz na drzwi żelazne (IRON_DOOR) w zasięgu 5 bloków!");
                        return true;
                    }
                    KeyType regType = KeyType.POLICE_STATION;
                    if (args.length >= 3) {
                        regType = KeyType.fromString(args[2]);
                        if (regType == null) regType = KeyType.POLICE_STATION;
                    }
                    manager.getKeyManager().registerDoor(tb.getLocation(), regType);
                    if (manager.getCurrentGame() != null) {
                        manager.getCurrentGame().getKeyManager().registerDoor(tb.getLocation(), regType);
                    }
                    manager.saveConfig();
                    pKey.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zarejestrowano żelazne drzwi jako zamknięte na: " + regType.getDisplayName());
                    return true;
                } else if (keySub.equals("remove")) {
                    if (!(sender instanceof Player pKey)) {
                        sender.sendMessage(ChatColor.RED + "Tylko gracz może usunąć drzwi!");
                        return true;
                    }
                    Block tb = pKey.getTargetBlockExact(5);
                    if (tb == null) {
                        pKey.sendMessage(ChatColor.RED + "[BattleRoyale] Spójrz na drzwi w zasięgu 5 bloków!");
                        return true;
                    }
                    manager.getKeyManager().removeDoor(tb.getLocation());
                    if (manager.getCurrentGame() != null) {
                        manager.getCurrentGame().getKeyManager().removeDoor(tb.getLocation());
                    }
                    manager.saveConfig();
                    pKey.sendMessage(ChatColor.GREEN + "[BattleRoyale] Usunięto rejestrację klucza z tych drzwi.");
                    return true;
                } else if (keySub.equals("list")) {
                    sender.sendMessage(ChatColor.GOLD + "=== Zarejestrowane Zamknięte Drzwi ===");
                    Map<Location, KeyType> doors = manager.getKeyManager().getLockedDoors();
                    if (doors.isEmpty()) {
                        sender.sendMessage(ChatColor.GRAY + "Brak zarejestrowanych zamkniętych drzwi.");
                    } else {
                        for (Map.Entry<Location, KeyType> e : doors.entrySet()) {
                            Location dl = e.getKey();
                            sender.sendMessage(ChatColor.YELLOW + " • [" + e.getValue().name() + "] " + ChatColor.WHITE + dl.getBlockX() + ", " + dl.getBlockY() + ", " + dl.getBlockZ());
                        }
                    }
                    return true;
                }
                return true;

            case "backpack":
            case "givebackpack":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                Player targetBp = getTargetPlayer(sender, args, 1);
                if (targetBp == null) return true;
                int bpTier = 1;
                if (args.length >= 3) {
                    try { bpTier = Integer.parseInt(args[2]); } catch (Exception ignored) {}
                }
                BackpackManager bpm = (manager.getCurrentGame() != null) ? manager.getCurrentGame().getBackpackManager() : new BackpackManager();
                targetBp.getInventory().addItem(bpm.createBackpack(bpTier));
                sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Nadano Plecak [Poziom " + bpTier + "] graczowi " + targetBp.getName());
                return true;

            case "spawnzombie":
            case "zombie":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames spawnzombie <stalker|bloater|survivor|leaper|gunner> [gracz]");
                    return true;
                }
                Player pTargetZombie = getTargetPlayer(sender, args, 2);
                if (pTargetZombie == null) return true;
                Location zLoc = pTargetZombie.getLocation();
                CustomZombieManager zm = (manager.getCurrentGame() != null) ? manager.getCurrentGame().getZombieManager() : new CustomZombieManager();
                String zType = args[1].toLowerCase();
                switch (zType) {
                    case "stalker":
                        zm.spawnStalker(zLoc, pTargetZombie);
                        sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zrespiono Prześladowcę (Stalker)!");
                        break;
                    case "bloater":
                        zm.spawnBloater(zLoc, pTargetZombie);
                        sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zrespiono Spuchlaka (Bloater)!");
                        break;
                    case "survivor":
                        zm.spawnSurvivor(zLoc);
                        sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zrespiono Ocalałego (Survivor jump-scare)!");
                        break;
                    case "leaper":
                        zm.spawnLeaper(zLoc, pTargetZombie);
                        sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zrespiono Skoczka (Leaper)!");
                        break;
                    case "gunner":
                        zm.spawnGunner(zLoc, pTargetZombie);
                        sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zrespiono Strzelca Zombie (Gunner)!");
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Nieznany typ zombie! Dostępne: stalker, bloater, survivor, leaper, gunner");
                        break;
                }
                return true;

            case "noise":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (!(sender instanceof Player pNoise)) {
                    sender.sendMessage(ChatColor.RED + "Tylko gracz może wygenerować hałas!");
                    return true;
                }
                double intensity = 50.0;
                if (args.length >= 2) {
                    try { intensity = Double.parseDouble(args[1]); } catch (Exception ignored) {}
                }
                if (manager.getCurrentGame() != null) {
                    manager.getCurrentGame().getNoiseManager().recordNoise(pNoise.getLocation(), intensity, intensity, "DEBUG", pNoise, manager.getCurrentGame().getZombieManager());
                    pNoise.sendMessage(ChatColor.GREEN + "[BattleRoyale] Wygenerowano hałas o sile " + intensity + " i zaalarmowano zombie w promieniu " + intensity + "m!");
                } else {
                    pNoise.sendMessage(ChatColor.RED + "[BattleRoyale] Brak aktywnego meczu do testowania hałasu.");
                }
                return true;

            case "blood":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (!(sender instanceof Player pBlood)) {
                    sender.sendMessage(ChatColor.RED + "Tylko gracz może zostawić krew!");
                    return true;
                }
                if (manager.getCurrentGame() != null) {
                    manager.getCurrentGame().getBloodTrailManager().onPlayerDamage(pBlood, 6.0);
                    pBlood.sendMessage(ChatColor.GREEN + "[BattleRoyale] Pozostawiono plamę krwi na ziemi!");
                } else {
                    pBlood.sendMessage(ChatColor.RED + "[BattleRoyale] Brak aktywnego meczu do testowania krwi.");
                }
                return true;

            case "tp":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (!(sender instanceof Player pTp)) {
                    sender.sendMessage(ChatColor.RED + "Tylko gracz może się teleportować!");
                    return true;
                }
                teleportAdminToArena(pTp);
                return true;

            case "map":
                if (!sender.hasPermission("amonpack.admin")) {
                    sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames map <relocate|shift|setworld|tp>");
                    return true;
                }
                String mapSub = args[1].toLowerCase();
                if (mapSub.equals("tp")) {
                    if (!(sender instanceof Player pTpMap)) {
                        sender.sendMessage(ChatColor.RED + "Tylko gracz może się teleportować!");
                        return true;
                    }
                    teleportAdminToArena(pTpMap);
                    return true;
                } else if (mapSub.equals("setworld")) {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames map setworld <nazwa_świata>");
                        return true;
                    }
                    String targetWorld = args[2];
                    BattleRoyaleManager.MapRelocationResult res = manager.shiftAllCoordinates(0, 0, 0, targetWorld);
                    if (res != null) {
                        sender.sendMessage(ChatColor.GREEN + "[BattleRoyale] Zmieniono świat areny na: " + targetWorld);
                    } else {
                        sender.sendMessage(ChatColor.RED + "[BattleRoyale] Wystąpił błąd podczas zmiany świata!");
                    }
                    return true;
                } else if (mapSub.equals("shift")) {
                    if (args.length < 5) {
                        sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames map shift <dx> <dy> <dz> [nowy_świat]");
                        return true;
                    }
                    try {
                        int dx = Integer.parseInt(args[2]);
                        int dy = Integer.parseInt(args[3]);
                        int dz = Integer.parseInt(args[4]);
                        String targetWorld = args.length >= 6 ? args[5] : null;

                        BattleRoyaleManager.MapRelocationResult res = manager.shiftAllCoordinates(dx, dy, dz, targetWorld);
                        sendRelocationReport(sender, res);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Parametry dx, dy, dz muszą być liczbami całkowitymi!");
                    }
                    return true;
                } else if (mapSub.equals("relocate")) {
                    if (args.length < 8) {
                        sender.sendMessage(ChatColor.YELLOW + "Użycie: /hungergames map relocate <x1> <y1> <z1> <x2> <y2> <z2> [nowy_świat]");
                        sender.sendMessage(ChatColor.GRAY + "x1, y1, z1 - koordynat punktu referencyjnego/kopiowania na świecie źródłowym");
                        sender.sendMessage(ChatColor.GRAY + "x2, y2, z2 - koordynat wklejenia na świecie areny (np. paste-location)");
                        return true;
                    }
                    try {
                        int x1 = Integer.parseInt(args[2]);
                        int y1 = Integer.parseInt(args[3]);
                        int z1 = Integer.parseInt(args[4]);

                        int x2 = Integer.parseInt(args[5]);
                        int y2 = Integer.parseInt(args[6]);
                        int z2 = Integer.parseInt(args[7]);

                        int dx = x2 - x1;
                        int dy = y2 - y1;
                        int dz = z2 - z1;

                        String targetWorld = args.length >= 9 ? args[8] : null;

                        BattleRoyaleManager.MapRelocationResult res = manager.shiftAllCoordinates(dx, dy, dz, targetWorld);
                        sendRelocationReport(sender, res);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Koordynaty muszą być liczbami całkowitymi!");
                    }
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Nieznana podkomenda! Dostępne: relocate, shift, setworld, tp");
                    return true;
                }

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void teleportAdminToArena(Player player) {
        String worldName = manager.getDefaultArena().getWorldName();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.YELLOW + "[BattleRoyale] Ładowanie świata '" + worldName + "' z dysku...");
            world = Bukkit.createWorld(new WorldCreator(worldName));
        }
        if (world == null) {
            player.sendMessage(ChatColor.RED + "[BattleRoyale] Nie można załadować świata '" + worldName + "'! Sprawdź czy folder świata istnieje.");
            return;
        }

        Location target = manager.getDefaultArena().getCenterLocation();
        if (target == null || target.getWorld() == null) {
            target = manager.getDefaultArena().getPasteLocation();
        }
        if (target == null || target.getWorld() == null) {
            target = world.getSpawnLocation();
        } else {
            target = new Location(world, target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
        }

        player.teleport(target);
        player.sendMessage(ChatColor.GREEN + "[BattleRoyale] Przeteleportowano do świata areny: " + worldName + " [" + target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ() + "]");
    }

    private void sendRelocationReport(CommandSender sender, BattleRoyaleManager.MapRelocationResult res) {
        if (res == null) {
            sender.sendMessage(ChatColor.RED + "[BattleRoyale] Błąd podczas przeliczania koordynatów!");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "========================================");
        sender.sendMessage(ChatColor.GREEN + "★ Pomyślnie zrelokowano mapę areny Battle Royale!");
        sender.sendMessage(ChatColor.YELLOW + "Wektor przesunięcia: " + ChatColor.WHITE + "ΔX: " + (res.dx >= 0 ? "+" : "") + res.dx
                + ", ΔY: " + (res.dy >= 0 ? "+" : "") + res.dy
                + ", ΔZ: " + (res.dz >= 0 ? "+" : "") + res.dz);
        sender.sendMessage(ChatColor.YELLOW + "Świat docelowy: " + ChatColor.WHITE + res.newWorld);
        sender.sendMessage(ChatColor.GRAY + "Zaktualizowano w konfiguracji:");
        sender.sendMessage(ChatColor.GRAY + " • Punkty spawnów graczy: " + ChatColor.WHITE + res.spawnsCount);
        sender.sendMessage(ChatColor.GRAY + " • Skrzynie tematyczne: " + ChatColor.WHITE + res.chestsCount);
        sender.sendMessage(ChatColor.GRAY + " • Punkty Ground Loot: " + ChatColor.WHITE + res.groundLootCount);
        sender.sendMessage(ChatColor.GRAY + " • Wraki samochodów (alarm): " + ChatColor.WHITE + res.carsCount);
        sender.sendMessage(ChatColor.GRAY + " • Zamknięte drzwi na klucz: " + ChatColor.WHITE + res.doorsCount);
        sender.sendMessage(ChatColor.GREEN + "Plik battleroyale.yml został automatycznie zaktualizowany!");
        sender.sendMessage(ChatColor.GOLD + "========================================");
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
            sender.sendMessage(ChatColor.YELLOW + "/hungergames tp " + ChatColor.GRAY + "- Teleportuje na świat areny BR");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames map relocate <x1> <y1> <z1> <x2> <y2> <z2> [świat] " + ChatColor.GRAY + "- Relokuje koordynaty ze schematica!");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames map shift <dx> <dy> <dz> [świat] " + ChatColor.GRAY + "- Przesuwa koordynaty o wektor");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames map setworld <świat> " + ChatColor.GRAY + "- Zmienia przypisany świat areny");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames chest <set|remove|list> " + ChatColor.GRAY + "- Skrzynie tematyczne (Food, Med, Guns...)");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames groundloot <add|remove|list> " + ChatColor.GRAY + "- Niewidzialne ramki z łupem");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames car <add|remove|list> " + ChatColor.GRAY + "- Zarządzanie wrakami aut z alarmem");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames key <door|remove|list> " + ChatColor.GRAY + "- Rejestracja zamkniętych drzwi żelaznych");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames givekey [gracz] [typ] " + ChatColor.GRAY + "- Daje klucz do drzwi");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames givebackpack [gracz] [1|2|3] " + ChatColor.GRAY + "- Daje plecak (18/27/36 slotów)");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames spawnzombie <stalker|bloater|survivor|leaper|gunner> " + ChatColor.GRAY + "- Respi specjalnego zombie");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames event <airdrop|blackout|bloodmoon|caralarm|NONE|SILENCE|DEHYDRATION> " + ChatColor.GRAY + "- Wydarzenia");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames noise [wartość] " + ChatColor.GRAY + "- Generuje impuls hałasu");
            sender.sendMessage(ChatColor.YELLOW + "/hungergames blood " + ChatColor.GRAY + "- Zostawia plamę krwi na podłodze");
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
                subs.addAll(Arrays.asList("wand", "tool", "tp", "map", "chest", "groundloot", "car", "key", "givekey",
                        "backpack", "givebackpack", "spawnzombie", "zombie", "event", "noise", "blood",
                        "givebandage", "givewater", "givecure", "givegun", "giveammo", "giveupgrade",
                        "forcestart", "stop", "reload"));
            }
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("map")) {
                return filter(Arrays.asList("relocate", "shift", "setworld", "tp"), args[1]);
            }
            if (sub.startsWith("give") || sub.equals("backpack")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());
                return filter(players, args[1]);
            }
            if (sub.equals("event")) {
                return filter(Arrays.asList("airdrop", "blackout", "bloodmoon", "caralarm", "NONE", "SILENCE", "DEHYDRATION"), args[1]);
            }
            if (sub.equals("car")) {
                return filter(Arrays.asList("add", "remove", "list"), args[1]);
            }
            if (sub.equals("key")) {
                return filter(Arrays.asList("door", "remove", "list"), args[1]);
            }
            if (sub.equals("spawnzombie") || sub.equals("zombie")) {
                return filter(Arrays.asList("stalker", "bloater", "survivor", "leaper", "gunner"), args[1]);
            }
            if (sub.equals("noise")) {
                return filter(Arrays.asList("15", "30", "50", "80"), args[1]);
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
            if (sub.equals("map") && args[1].equalsIgnoreCase("setworld")) {
                List<String> worlds = new ArrayList<>();
                for (World w : Bukkit.getWorlds()) worlds.add(w.getName());
                return filter(worlds, args[2]);
            }
            if (sub.equals("chest") && args[1].equalsIgnoreCase("set")) {
                List<String> types = new ArrayList<>();
                for (ThemedChestType t : ThemedChestType.values()) types.add(t.name());
                return filter(types, args[2]);
            }
            if (sub.equals("groundloot") && args[1].equalsIgnoreCase("add")) {
                return filter(Arrays.asList("RANDOM", "GUNS", "AMMO", "MEDICAL", "FOOD", "MELEE", "UPGRADE"), args[2]);
            }
            if (sub.equals("givekey") || (sub.equals("key") && args[1].equalsIgnoreCase("door"))) {
                List<String> keys = new ArrayList<>();
                for (KeyType kt : KeyType.values()) keys.add(kt.name());
                return filter(keys, args[2]);
            }
            if (sub.equals("givebackpack") || sub.equals("backpack")) {
                return filter(Arrays.asList("1", "2", "3"), args[2]);
            }
            if (sub.equals("spawnzombie") || sub.equals("zombie")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());
                return filter(players, args[2]);
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
