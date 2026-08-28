package RPG.Magic.listener;

import CustomContent.Items.CustomItemManager;
import Plugin.AmonPackPlugin;
import RPG.Magic.gui.*;
import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.manager.SpellRegistry;
import RPG.Magic.model.Spell;
import RPG.Magic.spells.water.SplashSpell;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MagicItemListener implements Listener {

    private final SpellRegistry spellRegistry;
    private final ManaManager manaManager;
    private final CustomItemManager customItemManager;
    private final Map<UUID, Long> staffChargeStart = new ConcurrentHashMap<>();

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

        if (!isMagicItem(item)) return;

        // Jeśli gracz klika w blok Ołtarza Arkanów lub Magicznego Stołu
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            org.bukkit.block.Block b = event.getClickedBlock();
            if (b.getType() == Material.NOTE_BLOCK || b.getType() == Material.ENCHANTING_TABLE) {
                return;
            }
        }

        Player player = event.getPlayer();
        Action action = event.getAction();

        boolean isStaff = MagicItemManager.isMagicStaff(item);
        boolean isWand = MagicItemManager.isMagicWand(item);
        boolean isTome = MagicItemManager.isMagicTome(item);

        // --- 1. LASKA (STAFF) ---
        if (isStaff) {
            // Shift + PPM otwiera menu konfiguracji laski
            if (player.isSneaking() && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
                event.setCancelled(true);
                player.openInventory(new TomeSpellGui(player, item, spellRegistry, manaManager).getInventory());
                return;
            }

            // PPM bez Shiftu: naciąganie łuku z animacją spowolnienia i widocznym ładowaniem
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                String spellId = MagicItemManager.getPrimarySpellId(item);
                Spell spell = spellRegistry.getSpell(spellId);
                if (spell == null) {
                    event.setCancelled(true);
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§cBrak przypisanego zaklęcia!"));
                    return;
                }

                // Natychmiastowa blokada jeśli czar ma cooldown
                if (spell.isOnCooldown(player)) {
                    event.setCancelled(true);
                    spell.sendCooldownActionBar(player);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.2f);
                    return;
                }

                // Natychmiastowa blokada jeśli brak many (chyba że w Creative)
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    int effectiveMana = spell.getEffectiveMana(player, item);
                    if (!manaManager.hasMana(player, effectiveMana)) {
                        event.setCancelled(true);
                        spell.sendNoManaActionBar(player, effectiveMana, manaManager);
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.2f);
                        return;
                    }
                }

                ensureMagicArrow(player);
                startStaffChargeParticles(player);
                return;
            }
        }

        // --- 2. RÓŻDŻKA (WAND) ---
        if (isWand) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                player.openInventory(new TomeSpellGui(player, item, spellRegistry, manaManager).getInventory());
                return;
            }

            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                String spellId = MagicItemManager.getPrimarySpellId(item);
                Spell spell = spellRegistry.getSpell(spellId);
                if (spell != null) {
                    spell.cast(player, item, manaManager);
                } else {
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§cBrak przypisanego zaklęcia!"));
                }
                return;
            }
        }

        // --- 3. TOM (TOME) ---
        if (isTome) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                player.openInventory(new TomeSpellGui(player, item, spellRegistry, manaManager).getInventory());
                return;
            }

            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                String spellId = MagicItemManager.getPrimarySpellId(item);
                Spell spell = spellRegistry.getSpell(spellId);
                if (spell != null) {
                    spell.cast(player, item, manaManager);
                } else {
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§cBrak przypisanego zaklęcia!"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        // A. Tkanie czaru dla Tomu Magii przy rozpoczęciu Shiftowania
        if (MagicItemManager.isMagicTome(item)) {
            String secondaryId = MagicItemManager.getSecondarySpellId(item);
            if (secondaryId != null && !secondaryId.equalsIgnoreCase("none")) {
                Spell spell = spellRegistry.getSpell(secondaryId);
                if (spell != null) {
                    spell.cast(player, item, manaManager);
                }
            }
        }
        // B. Przyciąganie wody dla Różdżki Wody przy Shiftowaniu
        else if (MagicItemManager.isWaterWand(item)) {
            RayTraceResult ray = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 12.0, FluidCollisionMode.ALWAYS, true);
            if (ray != null && ray.getHitBlock() != null && (ray.getHitBlock().getType() == Material.WATER || ray.getHitBlock().getType() == Material.ICE)) {
                SplashSpell.pullWaterSphere(player, ray.getHitBlock().getLocation());
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§b✦ Przyciągnięto wodę! §eKliknij LPM aby wystrzelić pocisk Splash!"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || !MagicItemManager.isMagicStaff(bow)) return;

        // Zawsze anulujemy fizyczną strzałę i jej zużycie
        event.setCancelled(true);

        float force = event.getForce();
        if (force < 0.75f) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c✦ Ładowanie przerwane za wcześnie! (Przytrzymaj PPM do pełnego naciągnięcia)"));
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.6f);
            return;
        }

        // Rzucenie czaru z laski przy pełnym naładowaniu łuku
        String spellId = MagicItemManager.getPrimarySpellId(bow);
        Spell spell = spellRegistry.getSpell(spellId);
        if (spell != null) {
            spell.cast(player, bow, manaManager);
        }
    }

    private void ensureMagicArrow(Player player) {
        if (hasArrow(player)) return;
        ItemStack dummyArrow = new ItemStack(Material.ARROW, 1);
        var meta = dummyArrow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§8Magiczna Strzała");
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "magic_arrow"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            dummyArrow.setItemMeta(meta);
        }
        player.getInventory().addItem(dummyArrow);
    }

    private void startStaffChargeParticles(Player player) {
        UUID uuid = player.getUniqueId();
        if (staffChargeStart.containsKey(uuid)) return;
        staffChargeStart.put(uuid, System.currentTimeMillis());

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !MagicItemManager.isMagicStaff(player.getInventory().getItemInMainHand())) {
                    staffChargeStart.remove(uuid);
                    cancel();
                    return;
                }
                if (!player.isHandRaised() && ticks > 4) {
                    staffChargeStart.remove(uuid);
                    cancel();
                    return;
                }

                ticks += 2;
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1.2, 0), 4, 0.3, 0.4, 0.3, 0.05);

                if (ticks >= 40) {
                    staffChargeStart.remove(uuid);
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);
    }

    private boolean hasArrow(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.ARROW || item.getType() == Material.SPECTRAL_ARROW || item.getType() == Material.TIPPED_ARROW)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem != null && isMagicItem(newItem)) {
            String spellId = MagicItemManager.getPrimarySpellId(newItem);
            Spell spell = spellRegistry.getSpell(spellId);
            String name = spell != null ? spell.getName() : "Zaklęcie";
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
        } else if (event.getInventory().getHolder() instanceof SpellSelectUpgradeGui gui) {
            gui.handleClick(event);
        } else if (event.getInventory().getHolder() instanceof SingleSpellUpgradeGui gui) {
            gui.handleClick(event);
        }
    }

    private boolean isMagicItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        if (item.getType() == Material.NOTE_BLOCK || item.getType() == Material.ENCHANTING_TABLE || item.getType().isBlock()) {
            return false;
        }

        if (MagicItemManager.isMagicStaff(item) || MagicItemManager.isMagicWand(item) || MagicItemManager.isMagicTome(item)) {
            return true;
        }

        if (customItemManager != null) {
            String customId = customItemManager.getCustomItemId(item);
            if (customId != null) {
                return customId.startsWith("tome_") || customId.startsWith("wand_") || customId.startsWith("staff_");
            }
        }

        return false;
    }
}
