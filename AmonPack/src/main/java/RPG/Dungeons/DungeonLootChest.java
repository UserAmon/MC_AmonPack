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

    public DungeonLootChest(Player player, DungeonPlayerStats stats, Dungeon template, Location chestLocation) {
        this.chestLocation = chestLocation;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_PURPLE + "Wybierz Swoja Nagrode");
        
        // Fill inventory with black stained glass panes
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

    /**
     * Generates 3 distinct reward options for the player.
     */
    private void generateRewardOptions(Player player, DungeonPlayerStats stats, Dungeon template) {
        List<RewardOption> eligibleSkills = new ArrayList<>();
        List<RewardOption> eligibleOthers = new ArrayList<>();

        PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
        List<String> globallyUnlocked = branch != null ? branch.getUnlockedAbilities() : new ArrayList<>();
        List<String> alreadyBound = stats.getBoundDungeonSkills();

        // 1. Gather eligible locked skills from ALL element trees in the skill tree config
        org.bukkit.configuration.file.FileConfiguration skillTreeConfig = AmonPackPlugin.getSkillTreeConfig();
        if (branch != null && skillTreeConfig != null && skillTreeConfig.getConfigurationSection("AmonPack.Tree") != null) {
            for (String elName : skillTreeConfig.getConfigurationSection("AmonPack.Tree").getKeys(false)) {
                com.projectkorra.projectkorra.Element pkEl = com.projectkorra.projectkorra.Element.getElement(elName);
                if (pkEl != null) {
                    ElementTree tree = AmonPackPlugin.levelsBending.GetElement(pkEl);
                    if (tree != null) {
                        for (SkillTree_Ability ability : tree.getAbilities()) {
                            String skillName = ability.getName();
                            // Offer skills that are NOT upgrades, NOT default/starting skills, NOT unlocked globally, and not already bound
                            if (!ability.isUpgrade() && !ability.isdef() && !globallyUnlocked.contains(skillName) && !alreadyBound.contains(skillName)) {
                                ItemStack icon = createSkillIcon(skillName, branch);
                                eligibleSkills.add(new RewardOption(RewardOption.RewardType.SKILL, skillName, icon));
                            }
                        }
                    }
                }
            }
        }

        // 2. Generate dynamic Stat Boost options from dungeon_config.yml
        org.bukkit.configuration.file.FileConfiguration config = AmonPackPlugin.getDungeonConfig();
        if (config != null) {
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
                    
                    RewardOption.RewardType type = getStatType(statKey);
                    if (type != null) {
                        eligibleOthers.add(new RewardOption(type, String.valueOf(value), item));
                    }
                }
            }

            // 3. Generate dynamic Blessing options from dungeon_config.yml
            org.bukkit.configuration.ConfigurationSection blessingsSec = config.getConfigurationSection("blessings");
            if (blessingsSec != null) {
                for (String blessingKey : blessingsSec.getKeys(false)) {
                    if (template != null && template.getAllowedBlessings() != null && !template.getAllowedBlessings().isEmpty()) {
                        if (!template.getAllowedBlessings().contains(blessingKey)) {
                            continue;
                        }
                    }
                    
                    if (stats.hasBlessing(blessingKey)) {
                        continue;
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

        // 4. Assemble the final 3 choices
        List<RewardOption> selected = new ArrayList<>();

        // Guarantee at least one skill in the choices if any are available
        if (!eligibleSkills.isEmpty()) {
            Collections.shuffle(eligibleSkills);
            selected.add(eligibleSkills.remove(0));
        }

        // Combine remaining eligible items
        List<RewardOption> combinedPool = new ArrayList<>();
        combinedPool.addAll(eligibleSkills);
        combinedPool.addAll(eligibleOthers);

        // Fallback to defaults if combined pool is completely empty
        if (combinedPool.isEmpty() && selected.isEmpty()) {
            combinedPool.add(createHpOption());
            combinedPool.add(createDefOption());
            combinedPool.add(createDmgOption());
            combinedPool.add(createSpeedOption());
        }

        // Shuffle combined pool and pick items until we have 3 choices
        Collections.shuffle(combinedPool);
        while (selected.size() < 3 && !combinedPool.isEmpty()) {
            selected.add(combinedPool.remove(0));
        }

        // Set slots 11, 13, and 15 with these options
        int[] slots = {11, 13, 15};
        for (int i = 0; i < selected.size() && i < slots.length; i++) {
            RewardOption option = selected.get(i);
            inventory.setItem(slots[i], option.item);
            options.put(slots[i], option);
        }
    }

    private RewardOption.RewardType getStatType(String statKey) {
        String key = statKey.toUpperCase();
        if (key.equals("HP")) return RewardOption.RewardType.STAT_HP;
        if (key.equals("DEF")) return RewardOption.RewardType.STAT_DEF;
        if (key.equals("DMG")) return RewardOption.RewardType.STAT_DMG;
        if (key.equals("SPEED")) return RewardOption.RewardType.STAT_SPEED;
        return null;
    }

    private ItemStack createSkillIcon(String skillName, PlayerBendingBranch branch) {
        Material mat = Material.BOOK;
        int customModelData = 0;
        
        try {
            // Find actual element of this skill instead of player's current element
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
            // Safe fallback
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

    private RewardOption createHpOption() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Ulepszenie: Maksymalne HP");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Statystyka: " + ChatColor.GREEN + "+4 HP (+2 Serca)");
            lore.add(ChatColor.GRAY + "Zwieksza Twoja przezywalnosc lochach.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return new RewardOption(RewardOption.RewardType.STAT_HP, "+4HP", item);
    }

    private RewardOption createDefOption() {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.BLUE + "Ulepszenie: Obrona (DEF)");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Statystyka: " + ChatColor.GREEN + "+10 DEF");
            lore.add(ChatColor.GRAY + "Zmniejsza otrzymywane obrazenia.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return new RewardOption(RewardOption.RewardType.STAT_DEF, "+10DEF", item);
    }

    private RewardOption createDmgOption() {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Ulepszenie: Obrazenia (DAMAGE)");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Statystyka: " + ChatColor.GREEN + "+15% Obrazen");
            lore.add(ChatColor.GRAY + "Zwieksza moc Twoich atakow i czarow.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return new RewardOption(RewardOption.RewardType.STAT_DMG, "+0.15DMG", item);
    }

    private RewardOption createSpeedOption() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Ulepszenie: Predkosc (SPEED)");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Statystyka: " + ChatColor.GREEN + "+10% Predkosci");
            lore.add(ChatColor.GRAY + "Szybciej unikasz potworow i pulapek.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return new RewardOption(RewardOption.RewardType.STAT_SPEED, "+0.02SPEED", item);
    }

    private RewardOption createVampirismBlessing() {
        ItemStack item = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Blogoslawienstwo: Wampiryzm Plomieni");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Efekt: " + ChatColor.GREEN + "Leczenie przy zabiciu");
            lore.add(ChatColor.GRAY + "Zabicie podpalonego wroga");
            lore.add(ChatColor.GRAY + "natychmiast leczy o 2 serca (4 HP).");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return new RewardOption(RewardOption.RewardType.BLESSING, "VAMPIRISM", item);
    }

    private RewardOption createDodgeBlessing() {
        ItemStack item = new ItemStack(Material.RABBIT_FOOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Blogoslawienstwo: Unik");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Efekt: " + ChatColor.GREEN + "+10% szansy na unik");
            lore.add(ChatColor.GRAY + "Masz stala szanse na calkowite");
            lore.add(ChatColor.GRAY + "zablokowanie wrogich obrazen.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return new RewardOption(RewardOption.RewardType.BLESSING, "DODGE", item);
    }

    private RewardOption createAdrenalineBlessing() {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Blogoslawienstwo: Adrenalina");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Efekt: " + ChatColor.GREEN + "Wscieklosc przy niskim zdrowiu");
            lore.add(ChatColor.GRAY + "Zadajesz +35% obrazen, gdy");
            lore.add(ChatColor.GRAY + "Twoje zdrowie spadnie ponizej 20% max.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return new RewardOption(RewardOption.RewardType.BLESSING, "ADRENALINE", item);
    }

    public static class RewardOption {
        public enum RewardType { SKILL, STAT_HP, STAT_DEF, STAT_DMG, STAT_SPEED, BLESSING }
        
        public final RewardType type;
        public final String value;
        public final ItemStack item;

        public RewardOption(RewardType type, String value, ItemStack item) {
            this.type = type;
            this.value = value;
            this.item = item;
        }
    }
}
