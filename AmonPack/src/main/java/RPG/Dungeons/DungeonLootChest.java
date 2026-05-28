package RPG.Dungeons;

import RPG.Levels.BendingTree.PlayerBendingBranch;
import RPG.Levels.BendingTree.ElementTree;
import RPG.Levels.BendingTree.SkillTree_Ability;
import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static Plugin.AmonPackPlugin.FastEasyStack;

import org.bukkit.Location;

public class DungeonLootChest implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, RewardOption> options = new HashMap<>();
    private final Location chestLocation;
    private final String blessingType;
    private final int slotsCount;

    public DungeonLootChest(Player player, DungeonPlayerStats stats, Dungeon template, Location chestLocation) {
        this(player, stats, template, chestLocation, "Chest_General", 3);
    }

    public DungeonLootChest(Player player, DungeonPlayerStats stats, Dungeon template, Location chestLocation, String blessingType, int slotsCount) {
        this.chestLocation = chestLocation;
        this.blessingType = blessingType;
        this.slotsCount = slotsCount;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_PURPLE + "Wybierz Swoja Nagrode");
        
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, FastEasyStack(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        
        generateRewardOptions(player, stats, template);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Map<Integer, RewardOption> getOptions() {
        return options;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    private void generateRewardOptions(Player player, DungeonPlayerStats stats, Dungeon template) {
        List<RewardOption> eligibleSkills = new ArrayList<>();
        List<RewardOption> eligibleOthers = new ArrayList<>();

        if (blessingType.equalsIgnoreCase("Chest_General") || blessingType.equalsIgnoreCase("Chest_Abilities")) {
            PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
            List<String> globallyUnlocked = branch != null ? branch.getUnlockedAbilities() : new ArrayList<>();
            List<String> alreadyBound = stats.getBoundDungeonSkills();

            org.bukkit.configuration.file.FileConfiguration skillTreeConfig = AmonPackPlugin.getSkillTreeConfig();
            if (branch != null && skillTreeConfig != null && skillTreeConfig.getConfigurationSection("AmonPack.Tree") != null) {
                for (String elName : skillTreeConfig.getConfigurationSection("AmonPack.Tree").getKeys(false)) {
                    com.projectkorra.projectkorra.Element pkEl = com.projectkorra.projectkorra.Element.getElement(elName);
                    if (pkEl != null) {
                        ElementTree tree = AmonPackPlugin.levelsBending.GetElement(pkEl);
                        if (tree != null) {
                            for (SkillTree_Ability ability : tree.getAbilities()) {
                                String skillName = ability.getName();
                                if (!ability.isUpgrade() && !ability.isdef() && !globallyUnlocked.contains(skillName) && !alreadyBound.contains(skillName)) {
                                    ItemStack icon = createSkillIcon(skillName, branch);
                                    eligibleSkills.add(new RewardOption(RewardOption.RewardType.SKILL, skillName, icon));
                                }
                            }
                        }
                    }
                }
            }
        }

        org.bukkit.configuration.file.FileConfiguration config = AmonPackPlugin.getDungeonConfig();
        if (config != null) {
            if (blessingType.equalsIgnoreCase("Chest_General") || blessingType.equalsIgnoreCase("Chest_Stats")) {
                org.bukkit.configuration.ConfigurationSection statsSec = config.getConfigurationSection("stats");
                if (statsSec != null) {
                    for (String statKey : statsSec.getKeys(false)) {
                        if (template != null && template.getAllowedStats() != null && !template.getAllowedStats().isEmpty()) {
                            if (!template.getAllowedStats().contains(statKey)) {
                                continue;
                            }
                        }
                        
                        String dName = statsSec.getString(statKey + ".display-name", statKey);
                        Material mat = Material.getMaterial(statsSec.getString(statKey + ".material", "RED_DYE"));
                        List<String> lore = statsSec.getStringList(statKey + ".lore");
                        double value = statsSec.getDouble(statKey + ".value", 0.0);
                        
                        ItemStack item = new ItemStack(mat == null ? Material.RED_DYE : mat);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', dName));
                            if (lore != null && !lore.isEmpty()) {
                                List<String> coloredLore = new ArrayList<>();
                                for (String line : lore) {
                                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                                }
                                meta.setLore(coloredLore);
                            }
                            item.setItemMeta(meta);
                        }
                        
                        eligibleOthers.add(new RewardOption(RewardOption.RewardType.STAT, statKey, String.valueOf(value), item));
                    }
                }
            }

            if (blessingType.equalsIgnoreCase("Chest_General") || blessingType.equalsIgnoreCase("Chest_Blessings")) {
                org.bukkit.configuration.ConfigurationSection blessingsSec = config.getConfigurationSection("blessings");
                if (blessingsSec != null) {
                    for (String blessingKey : blessingsSec.getKeys(false)) {
                        if (template != null && template.getAllowedBlessings() != null && !template.getAllowedBlessings().isEmpty()) {
                            if (!template.getAllowedBlessings().contains(blessingKey)) {
                                continue;
                            }
                        }
                        
                        String dName = blessingsSec.getString(blessingKey + ".display-name", blessingKey);
                        Material mat = Material.getMaterial(blessingsSec.getString(blessingKey + ".material", "GHAST_TEAR"));
                        List<String> lore = blessingsSec.getStringList(blessingKey + ".lore");
                        
                        ItemStack item = new ItemStack(mat == null ? Material.GHAST_TEAR : mat);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', dName));
                            if (lore != null && !lore.isEmpty()) {
                                List<String> coloredLore = new ArrayList<>();
                                for (String line : lore) {
                                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                                }
                                meta.setLore(coloredLore);
                            }
                            item.setItemMeta(meta);
                        }
                        
                        eligibleOthers.add(new RewardOption(RewardOption.RewardType.BLESSING, blessingKey, item));
                    }
                }
            }
        }

        List<RewardOption> selected = new ArrayList<>();

        if (!eligibleSkills.isEmpty()) {
            Collections.shuffle(eligibleSkills);
            selected.add(eligibleSkills.remove(0));
        }

        List<RewardOption> combinedPool = new ArrayList<>();
        combinedPool.addAll(eligibleSkills);
        combinedPool.addAll(eligibleOthers);

        if (combinedPool.isEmpty() && selected.isEmpty()) {
            if (config != null) {
                org.bukkit.configuration.ConfigurationSection statsSec = config.getConfigurationSection("stats");
                if (statsSec != null) {
                    for (String statKey : statsSec.getKeys(false)) {
                        String dName = statsSec.getString(statKey + ".display-name", statKey);
                        Material mat = Material.getMaterial(statsSec.getString(statKey + ".material", "RED_DYE"));
                        List<String> lore = statsSec.getStringList(statKey + ".lore");
                        double value = statsSec.getDouble(statKey + ".value", 0.0);
                        ItemStack item = new ItemStack(mat == null ? Material.RED_DYE : mat);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', dName));
                            if (lore != null && !lore.isEmpty()) {
                                List<String> coloredLore = new ArrayList<>();
                                for (String line : lore) {
                                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                                }
                                meta.setLore(coloredLore);
                            }
                            item.setItemMeta(meta);
                        }
                        combinedPool.add(new RewardOption(RewardOption.RewardType.STAT, statKey, String.valueOf(value), item));
                    }
                }
            }
        }

        Collections.shuffle(combinedPool);
        while (selected.size() < slotsCount && !combinedPool.isEmpty()) {
            selected.add(combinedPool.remove(0));
        }

        int[] slots;
        if (slotsCount == 1) {
            slots = new int[]{13};
        } else if (slotsCount == 2) {
            slots = new int[]{11, 15};
        } else if (slotsCount == 3) {
            slots = new int[]{11, 13, 15};
        } else if (slotsCount == 4) {
            slots = new int[]{10, 12, 14, 16};
        } else if (slotsCount == 5) {
            slots = new int[]{11, 12, 13, 14, 15};
        } else {
            slots = new int[]{11, 13, 15};
        }

        for (int i = 0; i < selected.size() && i < slots.length; i++) {
            RewardOption option = selected.get(i);
            inventory.setItem(slots[i], option.item);
            options.put(slots[i], option);
        }
    }

    private ItemStack createSkillIcon(String skillName, PlayerBendingBranch branch) {
        Material mat = Material.BOOK;
        int customModelData = 0;
        
        try {
            com.projectkorra.projectkorra.Element skillElement = null;
            org.bukkit.configuration.file.FileConfiguration skillTreeConfig = AmonPackPlugin.getSkillTreeConfig();
            if (skillTreeConfig != null && skillTreeConfig.getConfigurationSection("AmonPack.Tree") != null) {
                for (String elName : skillTreeConfig.getConfigurationSection("AmonPack.Tree").getKeys(false)) {
                    com.projectkorra.projectkorra.Element pkEl = com.projectkorra.projectkorra.Element.getElement(elName);
                    if (pkEl != null) {
                        ElementTree tree = AmonPackPlugin.levelsBending.GetElement(pkEl);
                        if (tree != null) {
                            for (SkillTree_Ability ability : tree.getAbilities()) {
                                if (ability.getName().equalsIgnoreCase(skillName)) {
                                    skillElement = pkEl;
                                    break;
                                }
                            }
                        }
                    }
                    if (skillElement != null) break;
                }
            }

            if (skillElement != null) {
                String elementname = skillElement.getName().toLowerCase();
                String matName = AmonPackPlugin.getSkillTreeConfig().getString("AmonPack.Menu." + elementname + ".Material");
                if (matName != null) {
                    mat = Material.getMaterial(matName);
                }
                customModelData = AmonPackPlugin.getSkillTreeConfig().getInt("AmonPack.Menu." + elementname + ".Green");
            }
        } catch (Exception e) {
        }

        ItemStack item = new ItemStack(mat == null ? Material.BOOK : mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Ruch: " + ChatColor.GOLD + skillName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Dodaje ten ruch do Twojego");
            lore.add(ChatColor.GRAY + "dungeonowego paska umiejetnosci.");
            meta.setLore(lore);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class RewardOption {
        public enum RewardType { SKILL, STAT, BLESSING }
        
        public final RewardType type;
        public final String key;
        public final String value;
        public final ItemStack item;

        public RewardOption(RewardType type, String key, String value, ItemStack item) {
            this.type = type;
            this.key = key;
            this.value = value;
            this.item = item;
        }

        public RewardOption(RewardType type, String key, ItemStack item) {
            this.type = type;
            this.key = key;
            this.value = "";
            this.item = item;
        }
    }
}
