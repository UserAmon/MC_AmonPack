package RPG.BattleRoyale.Items;

import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Infection.InfectionManager;
import RPG.BattleRoyale.Infection.InfectionState;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BandageHandler {

    public static final NamespacedKey KEY_BANDAGE = new NamespacedKey(AmonPackPlugin.plugin, "br_bandage");
    private static final int CHARGE_TICKS_REQUIRED = 30; // 1.5 sekundy trzymania

    private static class ChargeSession {
        int ticksCharged = 0;
        BukkitTask task;
        long lastInteractTime;
        EquipmentSlot slot;
    }

    private final Map<UUID, ChargeSession> activeSessions = new ConcurrentHashMap<>();

    public static ItemStack createBandage(int amount) {
        ItemStack item = new ItemStack(Material.PAPER, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "Bandaż Medyczny");
            meta.setCustomModelData(22002);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Sterylny opatrunek polowy na rany cięte i zakażenia.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Przytrzymaj PPM, aby w pełni naładować:");
            lore.add(ChatColor.GREEN + " • Leczy 4.0 HP (2 serca)");
            lore.add(ChatColor.DARK_GREEN + " • Cofa 10% infekcji wirusa zombie");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(KEY_BANDAGE, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isBandage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_BANDAGE, PersistentDataType.BYTE);
    }

    /**
     * Wywoływane przy interakcji PPM z bandażem.
     */
    public void handleInteract(Player player, EquipmentSlot slot, InfectionManager infectionManager) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        ChargeSession session = activeSessions.get(uuid);
        if (session != null) {
            session.lastInteractTime = now;
            return;
        }

        // Nowa sesja ładowania bandażowania
        ChargeSession newSession = new ChargeSession();
        newSession.lastInteractTime = now;
        newSession.slot = slot;

        newSession.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancelSession(uuid);
                    return;
                }

                ItemStack held = slot == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
                if (!isBandage(held)) {
                    cancelSession(uuid);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Przerwano bandażowanie!"));
                    return;
                }

                // Sprawdzenie czy gracz nadal przytrzymuje PPM (rejestrowane w oknie 450ms)
                if (System.currentTimeMillis() - newSession.lastInteractTime > 550) {
                    cancelSession(uuid);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Przerwano bandażowanie! Musisz przytrzymać PPM do 100%."));
                    return;
                }

                newSession.ticksCharged += 2;

                // Dźwięk opatrywania co 6 ticków
                if (newSession.ticksCharged % 6 == 0) {
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8f, 1.2f + (newSession.ticksCharged * 0.02f));
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.0, 0), 3, 0.2, 0.2, 0.2,
                            new Particle.DustOptions(Color.fromRGB(240, 240, 240), 1.0f));
                }

                // Pasek postępu na Action Barze
                int percent = Math.min(100, (newSession.ticksCharged * 100) / CHARGE_TICKS_REQUIRED);
                String bar = buildProgressBar(percent);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Bandażowanie: " + bar + " " + ChatColor.WHITE + percent + "%"));

                // Zakończenie pełnego naładowania
                if (newSession.ticksCharged >= CHARGE_TICKS_REQUIRED) {
                    cancelSession(uuid);
                    applyBandage(player, held, slot, infectionManager);
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);

        activeSessions.put(uuid, newSession);
    }

    private void applyBandage(Player player, ItemStack bandageItem, EquipmentSlot slot, InfectionManager infectionManager) {
        // Zużycie 1 sztuki
        bandageItem.setAmount(bandageItem.getAmount() - 1);
        if (bandageItem.getAmount() <= 0) {
            if (slot == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(null);
            else player.getInventory().setItemInOffHand(null);
        }

        // 1. Uleczenie 4.0 HP (2 serca)
        double maxHp = player.getMaxHealth();
        player.setHealth(Math.min(maxHp, player.getHealth() + 4.0));

        // 2. Redukcja 10% infekcji (dodanie 30 sekund lub złagodzenie stanu)
        if (infectionManager != null && infectionManager.isInfected(player)) {
            InfectionState inf = infectionManager.getInfection(player.getUniqueId());
            if (inf != null) {
                // Dodajemy +30 sekund (10% z 300s), dając graczowi więcej czasu na znalezienie pełnego leku
                inf.reduceSeconds(-30);
            }
        }

        // Efekty
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.4f);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation().add(0, 0.4, 0), 4, 0.3, 0.2, 0.3, 0.05);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.0, 0), 10, 0.4, 0.5, 0.4, 0.1);

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "✔ RANY ZABANDAŻOWANE! " + ChatColor.WHITE + "(+4 HP, -10% wirusa)"));
    }

    private String buildProgressBar(int percent) {
        int totalBars = 10;
        int filled = (percent * totalBars) / 100;
        StringBuilder sb = new StringBuilder(ChatColor.GREEN.toString());
        for (int i = 0; i < totalBars; i++) {
            if (i == filled) sb.append(ChatColor.GRAY);
            sb.append("■");
        }
        return "[" + sb + ChatColor.YELLOW + "]";
    }

    public void cancelSession(UUID uuid) {
        ChargeSession s = activeSessions.remove(uuid);
        if (s != null && s.task != null) {
            s.task.cancel();
        }
    }

    public void cleanup() {
        for (ChargeSession s : activeSessions.values()) {
            if (s.task != null) s.task.cancel();
        }
        activeSessions.clear();
    }
}
