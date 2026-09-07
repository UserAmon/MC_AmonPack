package RPG.BattleRoyale;

import Plugin.AmonPackPlugin;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BattleRoyaleInventoryBackup {

    /**
     * Zapisuje stan gracza do pliku YAML i czyści jego ekwipunek na czas meczu.
     */
    public static void backupAndClear(Player player) {
        if (player == null || !player.isOnline()) return;

        File folder = new File(AmonPackPlugin.plugin.getDataFolder(), "br_backups");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, player.getUniqueId().toString() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();

        cfg.set("inventory.contents", player.getInventory().getContents());
        cfg.set("inventory.armor", player.getInventory().getArmorContents());
        cfg.set("inventory.extra", player.getInventory().getExtraContents());
        cfg.set("inventory.offhand", player.getInventory().getItemInOffHand());
        cfg.set("xp.level", player.getLevel());
        cfg.set("xp.exp", player.getExp());
        cfg.set("stats.health", player.getHealth());
        cfg.set("stats.food", player.getFoodLevel());
        cfg.set("stats.gamemode", player.getGameMode().name());

        List<String> effects = new ArrayList<>();
        for (PotionEffect pe : player.getActivePotionEffects()) {
            effects.add(pe.getType().getName() + ":" + pe.getDuration() + ":" + pe.getAmplifier());
        }
        cfg.set("effects", effects);

        try {
            cfg.save(file);
        } catch (IOException e) {
            AmonPackPlugin.plugin.getLogger().warning("[BattleRoyale] Błąd zapisu ekwipunku gracza " + player.getName() + ": " + e.getMessage());
        }

        // Czyszczenie gracza
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        for (PotionEffect pe : player.getActivePotionEffects()) {
            player.removePotionEffect(pe.getType());
        }
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setGameMode(GameMode.SURVIVAL);
    }

    /**
     * Przywraca stan i ekwipunek gracza z zapisanego pliku kopii.
     */
    public static void restore(Player player) {
        if (player == null || !player.isOnline()) return;

        File folder = new File(AmonPackPlugin.plugin.getDataFolder(), "br_backups");
        File file = new File(folder, player.getUniqueId().toString() + ".yml");

        if (!file.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);

        try {
            player.setLevel(cfg.getInt("xp.level", 0));
            player.setExp((float) cfg.getDouble("xp.exp", 0.0));

            List<?> armorList = cfg.getList("inventory.armor");
            if (armorList != null) {
                player.getInventory().setArmorContents(armorList.toArray(new ItemStack[0]));
            }

            List<?> mainList = cfg.getList("inventory.contents");
            if (mainList != null) {
                player.getInventory().setContents(mainList.toArray(new ItemStack[0]));
            }

            List<?> extraList = cfg.getList("inventory.extra");
            if (extraList != null) {
                player.getInventory().setExtraContents(extraList.toArray(new ItemStack[0]));
            }

            ItemStack offhand = cfg.getItemStack("inventory.offhand");
            if (offhand != null) {
                player.getInventory().setItemInOffHand(offhand);
            }

            double hp = cfg.getDouble("stats.health", player.getMaxHealth());
            player.setHealth(Math.min(player.getMaxHealth(), Math.max(1.0, hp)));
            player.setFoodLevel(cfg.getInt("stats.food", 20));

            String gm = cfg.getString("stats.gamemode", "SURVIVAL");
            try {
                player.setGameMode(GameMode.valueOf(gm));
            } catch (Exception ignored) {}

            for (PotionEffect pe : player.getActivePotionEffects()) {
                player.removePotionEffect(pe.getType());
            }

            file.delete();
        } catch (Exception e) {
            AmonPackPlugin.plugin.getLogger().warning("[BattleRoyale] Błąd przywracania ekwipunku dla " + player.getName() + ": " + e.getMessage());
        }
    }
}
