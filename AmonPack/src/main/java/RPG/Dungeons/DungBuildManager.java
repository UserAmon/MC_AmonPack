package RPG.Dungeons;

import RPG.Levels.Objects.LevelSkill;
import RPG.Levels.PlayerLevelMenager;
import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class DungBuildManager implements Listener {

    private static File configFile;
    private static FileConfiguration config;
    private static File playersFile;
    private static FileConfiguration playersConfig;

    public static void init() {
        File dungDir = new File(AmonPackPlugin.plugin.getDataFolder(), "dungeons");
        if (!dungDir.exists()) {
            dungDir.mkdirs();
        }

        if (configFile == null) {
            configFile = new File(dungDir, "dung_build.yml");
            if (!configFile.exists()) {
                AmonPackPlugin.plugin.saveResource("dungeons/dung_build.yml", false);
            }
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        if (playersFile == null) {
            playersFile = new File(dungDir, "dung_build_players.yml");
            if (!playersFile.exists()) {
                try {
                    playersFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        }
    }

    public static void reload() {
        configFile = null;
        playersFile = null;
        init();
    }

    public static List<String> getPlayerSelections(Player player) {
        init();
        if (playersConfig == null) return new ArrayList<>();
        return playersConfig.getStringList(player.getUniqueId().toString());
    }

    public static void savePlayerSelections(Player player, List<String> selections) {
        init();
        if (playersConfig == null) return;
        playersConfig.set(player.getUniqueId().toString(), selections);
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getMaxChoicesForLevel(int playerLevel) {
        init();
        int max = 1;
        if (config == null) return max;
        org.bukkit.configuration.ConfigurationSection levelsSec = config.getConfigurationSection("DungBuild.Levels");
        if (levelsSec != null) {
            for (String lvlKey : levelsSec.getKeys(false)) {
                try {
                    int lvl = Integer.parseInt(lvlKey);
                    if (playerLevel >= lvl) {
                        int limit = levelsSec.getInt(lvlKey + ".MaxChoices", 1);
                        if (limit > max) {
                            max = limit;
                        }
                    }
                } catch (NumberFormatException e) {}
            }
        }
        return max;
    }

    public static void openDungBuildGui(Player player) {
        init();
        int playerLevel = PlayerLevelMenager.GetSkillByPlayer(LevelSkill.SkillType.DUNGEON, player);
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("DungBuild.Gui.Title", "&d&lStartowe Ulepszenia Lochu"));
        int size = config.getInt("DungBuild.Gui.Size", 27);
        Inventory inv = Bukkit.createInventory(new DungBuildGuiHolder(), size, title);

        ItemStack blank = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta blankMeta = blank.getItemMeta();
        if (blankMeta != null) {
            blankMeta.setDisplayName(" ");
            blank.setItemMeta(blankMeta);
        }
        for (int i = 0; i < size; i++) {
            inv.setItem(i, blank);
        }

        List<String> selections = getPlayerSelections(player);
        int maxChoices = getMaxChoicesForLevel(playerLevel);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Twoje Ulepszenia Startowe");
            List<String> infoLore = new ArrayList<>();
            infoLore.add(ChatColor.GRAY + "Poziom Dungeoneering: " + ChatColor.AQUA + playerLevel);
            infoLore.add(ChatColor.GRAY + "Wybrane: " + ChatColor.GREEN + selections.size() + ChatColor.GRAY + " / " + ChatColor.GREEN + maxChoices);
            infoMeta.setLore(infoLore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        org.bukkit.configuration.ConfigurationSection levelsSec = config.getConfigurationSection("DungBuild.Levels");
        if (levelsSec != null) {
            for (String lvlKey : levelsSec.getKeys(false)) {
                int reqLvl = 1;
                try {
                    reqLvl = Integer.parseInt(lvlKey.replaceAll("\\D+", ""));
                } catch (Exception ignored) {}
                org.bukkit.configuration.ConfigurationSection upgrades = levelsSec.getConfigurationSection(lvlKey + ".Upgrades");
                if (upgrades != null) {
                    for (String key : upgrades.getKeys(false)) {
                        org.bukkit.configuration.ConfigurationSection upgrade = upgrades.getConfigurationSection(key);
                        if (upgrade != null) {
                            int slot = upgrade.getInt("slot", 0);
                            String name = ChatColor.translateAlternateColorCodes('&', upgrade.getString("name", key));
                            Material mat = Material.getMaterial(upgrade.getString("material", "RED_DYE"));
                            if (mat == null) mat = Material.RED_DYE;
                            List<String> lore = upgrade.getStringList("lore");
                            ItemStack item;
                            ItemMeta meta;

                            if (playerLevel >= reqLvl) {
                                boolean isSelected = selections.contains(key);
                                item = new ItemStack(mat);
                                meta = item.getItemMeta();
                                if (meta != null) {
                                    meta.setDisplayName(name);
                                    List<String> finalLore = new ArrayList<>();
                                    for (String line : lore) {
                                        finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
                                    }
                                    finalLore.add(" ");
                                    if (isSelected) {
                                        finalLore.add(ChatColor.GREEN + "▶ Wybrano (Kliknij, aby odznaczyć)");
                                        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                                    } else {
                                        finalLore.add(ChatColor.YELLOW + "▶ Kliknij, aby wybrać");
                                    }
                                    meta.setLore(finalLore);
                                    item.setItemMeta(meta);
                                }
                            } else {
                                item = new ItemStack(Material.GRAY_DYE);
                                meta = item.getItemMeta();
                                if (meta != null) {
                                    meta.setDisplayName(ChatColor.RED + ChatColor.stripColor(name) + " (Zablokowane)");
                                    List<String> finalLore = new ArrayList<>();
                                    for (String line : lore) {
                                        finalLore.add(ChatColor.DARK_GRAY + ChatColor.stripColor(line));
                                    }
                                    finalLore.add(" ");
                                    finalLore.add(ChatColor.RED + "Wymagany poziom Dungeoneering: " + reqLvl);
                                    meta.setLore(finalLore);
                                    item.setItemMeta(meta);
                                }
                            }
                            inv.setItem(slot, item);
                        }
                    }
                }
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getHolder() instanceof DungBuildGuiHolder) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            init();
            if (config == null) return;

            int playerLevel = PlayerLevelMenager.GetSkillByPlayer(LevelSkill.SkillType.DUNGEON, player);
            int maxChoices = getMaxChoicesForLevel(playerLevel);
            List<String> selections = getPlayerSelections(player);
            int slot = event.getRawSlot();

            org.bukkit.configuration.ConfigurationSection levelsSec = config.getConfigurationSection("DungBuild.Levels");
            if (levelsSec != null) {
                for (String lvlKey : levelsSec.getKeys(false)) {
                    int reqLvl = 1;
                    try {
                        reqLvl = Integer.parseInt(lvlKey.replaceAll("\\D+", ""));
                    } catch (Exception ignored) {}
                    org.bukkit.configuration.ConfigurationSection upgrades = levelsSec.getConfigurationSection(lvlKey + ".Upgrades");
                    if (upgrades != null) {
                        for (String key : upgrades.getKeys(false)) {
                            org.bukkit.configuration.ConfigurationSection upgrade = upgrades.getConfigurationSection(key);
                            if (upgrade != null && upgrade.getInt("slot") == slot) {
                                if (playerLevel < reqLvl) {
                                    player.sendMessage(ChatColor.RED + "Nie posiadasz wystarczajacego poziomu Dungeoneering!");
                                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                                    return;
                                }

                                if (selections.contains(key)) {
                                    selections.remove(key);
                                    savePlayerSelections(player, selections);
                                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                                    openDungBuildGui(player);
                                } else {
                                    if (selections.size() >= maxChoices) {
                                        player.sendMessage(ChatColor.RED + "Maksymalnie mozesz wybrac " + maxChoices + " ulepszen na Twoim poziomie!");
                                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                                        return;
                                    }
                                    selections.add(key);
                                    savePlayerSelections(player, selections);
                                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                                    openDungBuildGui(player);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void applyStartingUpgrades(Player player, DungeonPlayerStats stats, DungeonInstance instance) {
        if (player == null || stats == null) return;
        List<String> selections = getPlayerSelections(player);
        init();
        if (config == null) return;
        int playerLevel = PlayerLevelMenager.GetSkillByPlayer(LevelSkill.SkillType.DUNGEON, player);
        int maxChoices = getMaxChoicesForLevel(playerLevel);

        org.bukkit.configuration.ConfigurationSection levelsSec = config.getConfigurationSection("DungBuild.Levels");
        int applied = 0;
        for (String upgradeId : selections) {
            if (applied >= maxChoices) break;

            org.bukkit.configuration.ConfigurationSection upgradeSec = null;
            if (levelsSec != null) {
                for (String lvlKey : levelsSec.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection upgrades = levelsSec.getConfigurationSection(lvlKey + ".Upgrades");
                    if (upgrades != null && upgrades.contains(upgradeId)) {
                        upgradeSec = upgrades.getConfigurationSection(upgradeId);
                        break;
                    }
                }
            }

            if (upgradeSec != null) {
                String type = upgradeSec.getString("type", "");
                String key = upgradeSec.getString("key", "");
                double value = upgradeSec.getDouble("value", 0.0);

                if ("STAT".equalsIgnoreCase(type)) {
                    DungeonManager.getInstance().applyUniversalStat(stats, key, value, player);
                    applied++;
                } else if ("BLESSING".equalsIgnoreCase(type)) {
                    if (!stats.hasBlessing(key)) {
                        stats.addActiveBlessing(key);
                    }
                    stats.setBlessingLevel(key, (int) value);

                    if (key.equals("WATER_STAFF")) {
                        int level = stats.getBlessingLevel("WATER_STAFF");
                        if (level == 1) {
                            stats.addHpBoost(4.0);
                            stats.addMCritRate(0.10);
                        } else if (level == 2) {
                            for (Player p : instance.getOnlinePlayers()) {
                                DungeonPlayerStats ps = instance.getPlayerStats(p);
                                if (ps != null) {
                                    ps.addMCritRate(0.10);
                                }
                            }
                        } else if (level == 3) {
                            stats.addHpBoost(4.0);
                            stats.addMCritRate(0.10);
                            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
                            if (bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Earth"))) {
                                stats.addHpBoost(-12.0);
                            }
                        }
                    } else if (key.equals("EARTH_MACE")) {
                        int level = stats.getBlessingLevel("EARTH_MACE");
                        if (level == 3) {
                            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
                            if (bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Earth"))) {
                                stats.addRegenLevel(1);
                            }
                        }
                    }
                    if (key.equals("POUHAI_BOW") || key.equals("AMON_GLOVE") || key.equals("MAI_DAGGERS") || key.equals("BLUE_SPIRIT_SWORDS") || key.equals("EARTH_MACE") || key.equals("WIND_SICKLE") || key.equals("WATER_STAFF")) {
                        DungeonManager.getInstance().giveOrUpdateLegendaryWeapon(player, stats, key);
                    }
                    applied++;
                }
            }
        }
    }

    public static class DungBuildGuiHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
