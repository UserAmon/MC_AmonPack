package RPG.BattleRoyale.Events;

import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zarządza poziomem nawodnienia graczy podczas ewenty "Fala Upałów / Odwodnienie" (DEHYDRATION).
 * Poziom nawodnienia spada stopniowo.
 * Gracze mogą uzupełniać poziom kucając w wodzie (+5%/s) lub pijąc z Butelki Czystej Wody (+40%).
 * Spadek do 0 skutkuje osłabieniem, spowolnieniem i powolną utratą życia (odwodnieniem).
 */
public class HydrationManager {

    public static final NamespacedKey KEY_WATER_BOTTLE = new NamespacedKey(AmonPackPlugin.plugin, "br_water_bottle");

    private final Map<UUID, Double> playerHydration = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> damageCooldown = new ConcurrentHashMap<>();

    public void initPlayer(Player player) {
        if (player == null) return;
        playerHydration.put(player.getUniqueId(), 100.0);
    }

    public double getHydration(UUID uuid) {
        return playerHydration.getOrDefault(uuid, 100.0);
    }

    public void addHydration(Player player, double amount) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        double current = playerHydration.getOrDefault(uuid, 100.0);
        double updated = Math.min(100.0, Math.max(0.0, current + amount));
        playerHydration.put(uuid, updated);
    }

    public static ItemStack createWaterBottle(int amount) {
        ItemStack item = new ItemStack(Material.POTION, Math.max(1, amount));
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setBasePotionData(new PotionData(PotionType.WATER));
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Butelka Czystej Wody");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Zdatna do picia woda zebrana przed apokalipsą.",
                    "",
                    ChatColor.YELLOW + "Wypij, aby odnowić:",
                    ChatColor.AQUA + " • +40% poziomu nawodnienia organizmu",
                    ChatColor.GREEN + " • Usuwa debuffy odwodnienia"
            ));
            meta.getPersistentDataContainer().set(KEY_WATER_BOTTLE, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isWaterBottle(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_WATER_BOTTLE, PersistentDataType.BYTE);
    }

    public boolean handleDrinkBottle(Player player, ItemStack item) {
        if (!isWaterBottle(item)) return false;

        addHydration(player, 40.0);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SPLASH, player.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.1);
        player.sendMessage(ChatColor.AQUA + "[BattleRoyale] Ugaszono pragnienie! (+40% nawodnienia)");

        // Zwróć pustą szklaną butelkę
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.GLASS_BOTTLE));
        } else {
            player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
        }
        return true;
    }

    /**
     * Taktowanie co 1 sekundę z pętli gry.
     */
    public void tick(World world, List<Player> players) {
        if (players == null || players.isEmpty()) return;

        for (Player player : players) {
            if (player == null || !player.isOnline() || player.isDead()) continue;
            UUID uuid = player.getUniqueId();
            double hyd = playerHydration.getOrDefault(uuid, 100.0);

            // Sprawdzenie czy gracz kuca w wodzie (uzupełnianie ze zbiorników naturalnych)
            Block footBlock = player.getLocation().getBlock();
            Block eyeBlock = player.getEyeLocation().getBlock();
            boolean inWater = footBlock.getType() == Material.WATER || eyeBlock.getType() == Material.WATER || player.isInWater();

            if (player.isSneaking() && inWater) {
                // Odnawianie nawodnienia
                hyd = Math.min(100.0, hyd + 5.0);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.6f, 1.1f);
                player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 0.5, 0), 6, 0.3, 0.2, 0.3, 0.1);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.AQUA + "💧 Pijesz wodę ze zbiornika... " + buildHydrationBar(hyd) + " " + (int) hyd + "%"));
            } else {
                // Naturalny ubytek nawodnienia (szybszy przy sprincie)
                double loss = player.isSprinting() ? 0.7 : 0.45;
                hyd = Math.max(0.0, hyd - loss);

                String bar = buildHydrationBar(hyd);
                ChatColor color = hyd > 50.0 ? ChatColor.AQUA : hyd > 20.0 ? ChatColor.GOLD : ChatColor.RED;
                String text = color + "Nawodnienie: " + bar + " " + ChatColor.WHITE + (int) hyd + "%";

                if (hyd <= 0.0) {
                    text = ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ ODWODNIENIE! TRACISZ ŻYCIE! ☠";
                } else if (hyd <= 20.0) {
                    text += ChatColor.RED + " ⚠ Jesteś spragniony! Znajdź wodę!";
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
            }

            playerHydration.put(uuid, hyd);

            // Debuffy i obrażenia od skrajnego odwodnienia
            if (hyd <= 20.0 && hyd > 0.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 30, 0, false, false, false));
            } else if (hyd <= 0.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 0, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30, 1, false, false, false));

                // Obrażenia co 3 sekundy
                int cd = damageCooldown.getOrDefault(uuid, 0) + 1;
                if (cd >= 3) {
                    cd = 0;
                    player.damage(1.5);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 0.9f);
                    player.sendMessage(ChatColor.RED + "[BattleRoyale] Cierpisz z powodu krytycznego odwodnienia! Kucnij w wodzie lub wypij Butelkę Wody!");
                }
                damageCooldown.put(uuid, cd);
            }
        }
    }

    private String buildHydrationBar(double hyd) {
        int total = 10;
        int filled = (int) Math.round((hyd / 100.0) * total);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                if (i <= 2) sb.append(ChatColor.RED);
                else if (i <= 5) sb.append(ChatColor.YELLOW);
                else sb.append(ChatColor.AQUA);
                sb.append("▮");
            } else {
                sb.append(ChatColor.DARK_GRAY).append("▯");
            }
        }
        return "[" + sb + ChatColor.RESET + "]";
    }

    public void cleanup() {
        playerHydration.clear();
        damageCooldown.clear();
    }
}
