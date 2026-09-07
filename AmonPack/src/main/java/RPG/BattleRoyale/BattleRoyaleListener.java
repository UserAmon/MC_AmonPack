package RPG.BattleRoyale;

import CustomContent.Guns.AmmoType;
import CustomContent.Guns.GunData;
import CustomContent.Guns.GunType;
import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Events.BattleRoyaleEvent;
import RPG.BattleRoyale.Events.HydrationManager;
import RPG.BattleRoyale.GroundLoot.GroundLootManager;
import RPG.BattleRoyale.Items.BandageHandler;
import RPG.BattleRoyale.Loot.ThemedChestType;
import RPG.BattleRoyale.Weapons.BattleRoyaleWeaponHelper;
import RPG.BattleRoyale.Zombies.CustomZombieManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class BattleRoyaleListener implements Listener {

    private final BattleRoyaleManager manager;
    private final Random random = new Random();
    private final Map<java.util.UUID, String> wandCategories = new ConcurrentHashMap<>();
    private static final List<String> CATEGORIES = Arrays.asList("RANDOM", "GUNS", "AMMO", "MEDICAL", "FOOD", "MELEE", "UPGRADE");

    public BattleRoyaleListener(BattleRoyaleManager manager) {
        this.manager = manager;
    }

    /**
     * Blokada poruszania się podczas 20-sekundowego odliczania na starcie meczu.
     * Rejestracja skoków dla systemu hałasu.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BattleRoyaleGame game = manager.getGameByPlayer(player);
        if (game == null) return;

        if (game.isFreezeActive()) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY())) {
                Location loc = from.clone();
                loc.setYaw(to.getYaw());
                loc.setPitch(to.getPitch());
                event.setTo(loc);
            }
            return;
        }

        // Skok gracza w Strefie Ciszy zwiększa hałas
        if (game.getCurrentEvent() == BattleRoyaleEvent.SILENCE) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && to.getY() > from.getY() + 0.35 && !player.isInsideVehicle()) {
                game.getNoiseManager().addNoise(player, 6.0);
            }
        }
    }

    /**
     * Interakcje: Różdżka admina, Bandaż, Woda, Lekarstwo, Zestaw Ulepszeń, Kontenery.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = event.getItem();

        // 1. Różdżka budowy mapy BR dla admina (działa również poza aktywną grą!)
        if (hand != null && hand.getType() == Material.BLAZE_ROD && hand.hasItemMeta() &&
                hand.getItemMeta().getDisplayName().contains("Różdżka Budowy Mapy BR")) {
            if (player.hasPermission("amonpack.admin")) {
                handleAdminWandInteract(player, hand, event.getAction(), event.getClickedBlock());
                event.setCancelled(true);
                return;
            }
        }

        BattleRoyaleGame game = manager.getGameByPlayer(player);
        if (game == null) return;

        // 2. Użycie Bandażu Medycznego (przytrzymanie PPM i ładowanie)
        if (hand != null && BandageHandler.isBandage(hand)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                game.getBandageHandler().handleInteract(player, event.getHand(), game.getInfectionManager());
                return;
            }
        }

        // 3. Użycie Butelki Czystej Wody (Odwodnienie)
        if (hand != null && HydrationManager.isWaterBottle(hand)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                game.getHydrationManager().handleDrinkBottle(player, hand);
                return;
            }
        }

        // 4. Użycie Lekarstwa na Infekcję
        if (hand != null && BattleRoyaleWeaponHelper.isInfectionCure(hand)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                boolean cured = game.getInfectionManager().cure(player);
                if (cured) {
                    hand.setAmount(hand.getAmount() - 1);
                    if (hand.getAmount() <= 0) {
                        player.getInventory().setItem(event.getHand(), null);
                    }
                } else {
                    player.sendMessage(ChatColor.YELLOW + "[BattleRoyale] Nie jesteś zakażony wirusem zombie! Zachowaj antidotum na później.");
                }
                return;
            }
        }

        // 5. Użycie Zestawu Ulepszenia Broni (Upgrade Kit)
        if (hand != null && BattleRoyaleWeaponHelper.isUpgradeKit(hand)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                ItemStack offHand = player.getInventory().getItemInOffHand();
                if (GunData.isGun(offHand)) {
                    event.setCancelled(true);
                    upgradeHeldGun(player, offHand, hand, event.getHand());
                    return;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "[BattleRoyale] Aby ulepszyć broń palną, umieść ją w drugiej ręce (offhand) i kliknij PPM Zestawem Ulepszenia!");
                }
            }
        }

        // 6. Strzał z broni generuje potężny hałas w Strefie Ciszy
        if (hand != null && GunData.isGun(hand) && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (game.getCurrentEvent() == BattleRoyaleEvent.SILENCE) {
                game.getNoiseManager().addNoise(player, 45.0);
            }
        }

        // 7. Otwieranie kontenerów z lootem
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
                Material t = block.getType();
                if (t == Material.CHEST || t == Material.TRAPPED_CHEST || t == Material.BARREL ||
                        t.name().contains("BOOKSHELF") || t.name().contains("SHELF")) {
                    game.handleContainerOpen(block, player);
                }
            }
        }
    }

    private void handleAdminWandInteract(Player player, ItemStack wand, Action action, Block clickedBlock) {
        if (action == Action.LEFT_CLICK_BLOCK && clickedBlock != null) {
            // Usuwanie Ground Loot lub Skrzyni tematycznej
            Location blockLoc = clickedBlock.getLocation();
            Location lootLoc = blockLoc.clone().add(0, 1, 0);
            boolean glRemoved = manager.getGroundLootManager().removePoint(lootLoc) || manager.getGroundLootManager().removePoint(blockLoc);
            boolean tcRemoved = manager.getLootManager().removeThemedChest(blockLoc);

            if (glRemoved || tcRemoved) {
                manager.saveConfig();
                player.sendMessage(ChatColor.RED + "[Wand] Usunięto punkt (" + (glRemoved ? "Ground Loot" : "Skrzynia Tematyczna") + ") na koordynatach: "
                        + blockLoc.getBlockX() + ", " + blockLoc.getBlockY() + ", " + blockLoc.getBlockZ());
                player.playSound(blockLoc, Sound.BLOCK_ANVIL_BREAK, 0.8f, 1.2f);
            } else {
                player.sendMessage(ChatColor.YELLOW + "[Wand] W tym miejscu nie ma zapisanego punktu lootu ani skrzyni.");
            }
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK && clickedBlock != null) {
            String currentCat = wandCategories.getOrDefault(player.getUniqueId(), "RANDOM");

            if (player.isSneaking()) {
                // Zmiana kategorii lootu
                int curIdx = CATEGORIES.indexOf(currentCat);
                int nextIdx = (curIdx + 1) % CATEGORIES.size();
                String nextCat = CATEGORIES.get(nextIdx);
                wandCategories.put(player.getUniqueId(), nextCat);
                player.sendMessage(ChatColor.GOLD + "[Wand] Aktywna kategoria: " + ChatColor.AQUA + "" + ChatColor.BOLD + nextCat);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.4f);
                return;
            }

            Material t = clickedBlock.getType();
            if (t == Material.CHEST || t == Material.TRAPPED_CHEST || t == Material.BARREL || t.name().contains("SHELF")) {
                // Ustawienie skrzyni tematycznej
                ThemedChestType type = ThemedChestType.fromString(currentCat);
                manager.getLootManager().setThemedChest(clickedBlock.getLocation(), type);
                manager.saveConfig();
                player.sendMessage(ChatColor.GREEN + "[Wand] Ustawiono skrzynię tematyczną: " + type.getDisplayName() + " [" + type.name() + "]");
                player.playSound(clickedBlock.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.2f);
                clickedBlock.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, clickedBlock.getLocation().add(0.5, 1.0, 0.5), 10, 0.3, 0.3, 0.3, 0.1);
            } else {
                // Ustawienie leżącego na ziemi przedmiotu (Ground Loot Point)
                Location spawnLoc = clickedBlock.getLocation().add(0, 1, 0);
                manager.getGroundLootManager().addPoint(spawnLoc, currentCat);
                manager.saveConfig();
                player.sendMessage(ChatColor.GREEN + "[Wand] Utworzono leżący na ziemi Ground Loot (" + currentCat + ") na bloku "
                        + clickedBlock.getX() + ", " + clickedBlock.getY() + ", " + clickedBlock.getZ());
                player.playSound(spawnLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.4f);
                spawnLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, spawnLoc.clone().add(0.5, 0.5, 0.5), 12, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    private void upgradeHeldGun(Player player, ItemStack gunStack, ItemStack kitStack, EquipmentSlot kitSlot) {
        GunData data = GunData.fromItemStack(gunStack);
        if (data == null) return;

        int currentLvl = data.getLevel();
        if (currentLvl >= 5) {
            player.sendMessage(ChatColor.RED + "[BattleRoyale] Ta broń osiągnęła już maksymalny poziom ulepszenia (Poz. 5)!");
            return;
        }

        int tier = BattleRoyaleWeaponHelper.getUpgradeKitTier(kitStack);
        data.setLevel(currentLvl + tier);
        data.applyToItemStack(gunStack);

        kitStack.setAmount(kitStack.getAmount() - 1);
        if (kitStack.getAmount() <= 0) {
            player.getInventory().setItem(kitSlot, null);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.4f);
        player.spawnParticle(org.bukkit.Particle.CRIT, player.getLocation().add(0, 1.2, 0), 15, 0.3, 0.3, 0.3, 0.1);
        player.sendMessage(ChatColor.GREEN + "[BattleRoyale] Pomyślnie ulepszono broń " + data.getGunType().getDisplayName() + ChatColor.GREEN + " na Poziom " + data.getLevel() + "!");
    }

    /**
     * Ulepszanie broni poprzez upuszczenie Zestawu Ulepszenia na broń w GUI ekwipunku.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BattleRoyaleGame game = manager.getGameByPlayer(player);
        if (game == null) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor != null && BattleRoyaleWeaponHelper.isUpgradeKit(cursor) && current != null && GunData.isGun(current)) {
            event.setCancelled(true);
            GunData data = GunData.fromItemStack(current);
            if (data == null) return;

            if (data.getLevel() >= 5) {
                player.sendMessage(ChatColor.RED + "[BattleRoyale] Ta broń ma już maksymalny poziom (Poz. 5)!");
                return;
            }

            int tier = BattleRoyaleWeaponHelper.getUpgradeKitTier(cursor);
            data.setLevel(data.getLevel() + tier);
            data.applyToItemStack(current);

            cursor.setAmount(cursor.getAmount() - 1);
            if (cursor.getAmount() <= 0) {
                event.getView().setCursor(null);
            }

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.4f);
            player.sendMessage(ChatColor.GREEN + "[BattleRoyale] Pomyślnie ulepszono broń na Poziom " + data.getLevel() + "!");
        }
    }

    /**
     * Wypicie leku jeśli jest płynem consumable.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        BattleRoyaleGame game = manager.getGameByPlayer(player);
        if (game == null) return;

        if (BattleRoyaleWeaponHelper.isInfectionCure(event.getItem())) {
            game.getInfectionManager().cure(player);
        }
    }

    /**
     * Podnoszenie leżących na ziemi przedmiotów (Ground Loot) z ItemFrame przez kliknięcie PPM.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame frame) {
            Player player = event.getPlayer();
            BattleRoyaleGame game = manager.getGameByPlayer(player);
            if (game != null) {
                if (game.getGroundLootManager().handlePickup(frame, player)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Obrażenia Melee od potworów/bossów -> szansa na nałożenie infekcji na gracza.
     * Podnoszenie lootu z ItemFrame przez uderzenie (LPM) i naliczanie hałasu za ataki.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 1. Uderzenie w ItemFrame leżący na ziemi
        if (event.getEntity() instanceof ItemFrame frame && event.getDamager() instanceof Player player) {
            BattleRoyaleGame game = manager.getGameByPlayer(player);
            if (game != null) {
                if (game.getGroundLootManager().handlePickup(frame, player)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 2. Gracz atakujący wręcz generuje hałas w Strefie Ciszy
        if (event.getDamager() instanceof Player attacker) {
            BattleRoyaleGame game = manager.getGameByPlayer(attacker);
            if (game != null && game.getCurrentEvent() == BattleRoyaleEvent.SILENCE) {
                game.getNoiseManager().addNoise(attacker, 10.0);
            }
        }

        if (!(event.getEntity() instanceof Player victim)) return;

        BattleRoyaleGame game = manager.getGameByPlayer(victim);
        if (game == null) return;

        Entity damager = event.getDamager();

        // Atak Melee potwora, bota lub bossa
        if (damager instanceof Monster || damager instanceof LivingEntity) {
            EntityDamageEvent.DamageCause cause = event.getCause();
            if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                double chance = game.getArena().getMeleeInfectChance();
                if (random.nextDouble() <= chance) {
                    game.getInfectionManager().infect(victim);
                }
            }
        }
    }

    /**
     * Śmierć gracza lub potwora w świecie Battle Royale.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // 1. Śmierć gracza
        if (entity instanceof Player player) {
            BattleRoyaleGame game = manager.getGameByPlayer(player);
            if (game != null) {
                game.handlePlayerDeath(player);
            }
            return;
        }

        // 2. Śmierć w świecie BR
        BattleRoyaleGame game = manager.getGameByWorld(entity.getWorld());
        if (game == null) return;

        // Śmierć bota AI
        if (game.getBotManager().isBot(entity)) {
            game.getBotManager().removeBot(entity.getUniqueId());
            game.broadcastMessage(ChatColor.GRAY + "[BattleRoyale] " + entity.getCustomName() + " został wyeliminowany!");

            // Szansa na upuszczenie amunicji, lekarstwa, bandaża lub zestawu ulepszeń
            if (random.nextDouble() < 0.60) {
                event.getDrops().add(BattleRoyaleWeaponHelper.createAmmo(AmmoType.LEAD_BULLET, 4 + random.nextInt(6)));
            }
            if (random.nextDouble() < 0.40) {
                event.getDrops().add(BandageHandler.createBandage(1 + random.nextInt(2)));
            }
            if (random.nextDouble() < 0.30) {
                event.getDrops().add(BattleRoyaleWeaponHelper.createInfectionCure(
                        game.getArena().getCureMaterial(),
                        game.getArena().getCureDisplayName(),
                        game.getArena().getCureLore(),
                        game.getArena().getCureCustomModelData()
                ));
            }
            if (random.nextDouble() < 0.25) {
                event.getDrops().add(BattleRoyaleWeaponHelper.createUpgradeKit(1));
            }
            return;
        }

        // Śmierć specjalnego zombie (Leaper lub Gunner)
        if (game.getZombieManager().isSpecialZombie(entity)) {
            if (entity.getCustomName() != null && entity.getCustomName().contains("Strzelec")) {
                if (random.nextDouble() < 0.75) {
                    event.getDrops().add(BattleRoyaleWeaponHelper.createAmmo(AmmoType.LEAD_BULLET, 6 + random.nextInt(8)));
                }
                if (random.nextDouble() < 0.25) {
                    event.getDrops().add(BattleRoyaleWeaponHelper.createGun(GunType.FLINTLOCK_PISTOL, 1, false, false, false, false, CustomContent.Guns.GunUniqueMod.NONE));
                }
            } else if (entity.getCustomName() != null && entity.getCustomName().contains("Skoczek")) {
                if (random.nextDouble() < 0.70) {
                    event.getDrops().add(BandageHandler.createBandage(1 + random.nextInt(2)));
                }
            }
            return;
        }

        // Śmierć zwykłego moba PVE (Zombie/Skeleton itp.)
        if (entity instanceof Monster) {
            // Szansa na dodatkowy drop broni, bandaży, pocisków lub lekarstwa
            if (random.nextDouble() < 0.40) {
                event.getDrops().add(BattleRoyaleWeaponHelper.createAmmo(AmmoType.LEAD_BULLET, 2 + random.nextInt(4)));
            }
            if (random.nextDouble() < 0.30) {
                event.getDrops().add(BandageHandler.createBandage(1));
            }
            if (random.nextDouble() < 0.20) {
                event.getDrops().add(BattleRoyaleWeaponHelper.createInfectionCure(
                        game.getArena().getCureMaterial(),
                        game.getArena().getCureDisplayName(),
                        game.getArena().getCureLore(),
                        game.getArena().getCureCustomModelData()
                ));
            }
            if (random.nextDouble() < 0.10) {
                event.getDrops().add(BattleRoyaleWeaponHelper.createRandomGun(1, 2));
            }
        }
    }

    /**
     * Ochrona mapy, reguły niszczenia dozwolonych bloków i opcjonalny drop łupu.
     * Niszczenie bloku w Strefie Ciszy generuje hałas.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        BattleRoyaleGame game = manager.getGameByPlayer(player);
        if (game == null) return;

        Block block = event.getBlock();
        BattleRoyaleArena arena = game.getArena();

        // Jeśli niszczenie jest ograniczone do zdefiniowanych bloków
        if (arena.isRestrictBlockBreaking()) {
            // Sprawdź czy to barykada postawiona przez gracza w tej sesji
            boolean isBarricade = game.getBarricadeManager().isBarricade(block.getLocation());
            BattleRoyaleArena.BreakableBlockRule rule = arena.getBreakableBlocks().get(block.getType());

            if (!isBarricade && rule == null) {
                // Blok jest chroniony! Nie wolno go niszczyć.
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "[BattleRoyale] Nie możesz niszczyć struktury areny!");
                return;
            }

            // Dozwolone niszczenie:
            game.getWorldManager().recordBlockChange(block);

            if (isBarricade) {
                game.getBarricadeManager().removeBarricade(block.getLocation());
            }

            if (rule != null) {
                if (rule.hasLoot()) {
                    event.setDropItems(false);
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), rule.getOptionalLoot().clone());
                } else {
                    event.setDropItems(false); // Brak lootu
                }
            } else {
                event.setDropItems(false);
            }

            if (game.getCurrentEvent() == BattleRoyaleEvent.SILENCE) {
                game.getNoiseManager().addNoise(player, 12.0);
            }
            return;
        }

        // Domyślna ochrona bez restrykcji
        game.getWorldManager().recordBlockChange(block);
        if (game.getCurrentEvent() == BattleRoyaleEvent.SILENCE) {
            game.getNoiseManager().addNoise(player, 12.0);
        }
    }

    /**
     * Ochrona mapy, reguły stawiania bloków barykad (domyślnie tylko zdefiniowane np. OAK_SLAB).
     * Rejestracja barykady dla AI zombie/bossów.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        BattleRoyaleGame game = manager.getGameByPlayer(player);
        if (game == null) return;

        Block block = event.getBlockPlaced();
        BattleRoyaleArena arena = game.getArena();

        if (arena.isRestrictBlockPlacing()) {
            if (!arena.getPlaceableBlocks().contains(block.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "[BattleRoyale] Nie możesz postawić tego bloku na arenie! Dozwolone są tylko barykady.");
                return;
            }
        }

        // Zarejestruj zmianę do cofnięcia schematem
        game.getWorldManager().recordBlockChange(block);

        // Zarejestruj barykadę jako cel dla zombie i bossów
        game.getBarricadeManager().registerBarricade(block.getLocation(), arena.getBarricadeHitsToDestroy());
        player.playSound(block.getLocation(), Sound.BLOCK_WOOD_PLACE, 1.0f, 1.2f);
    }

    /**
     * Bezpieczne opuszczenie gry przy wyjściu gracza z serwera.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Wyjście z lobby
        BattleRoyaleLobby lobby = manager.getCurrentLobby();
        if (lobby != null) {
            lobby.removePlayer(player);
        }

        // Wyjście z aktywnej gry
        BattleRoyaleGame game = manager.getGameByPlayer(player);
        if (game != null) {
            game.handlePlayerLeave(player);
        }
    }
}
