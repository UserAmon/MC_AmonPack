package RPG.BattleRoyale.Loot;

import CustomContent.Guns.AmmoType;
import CustomContent.Guns.GunType;
import CustomContent.Guns.GunUniqueMod;
import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.BattleRoyaleArena;
import RPG.BattleRoyale.Items.BandageHandler;
import RPG.BattleRoyale.Weapons.BattleRoyaleWeaponHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BattleRoyaleLootManager {

    private final List<LootEntry> chestLoot = new ArrayList<>();
    private final List<LootEntry> barrelLoot = new ArrayList<>();
    private final List<LootEntry> shelfLoot = new ArrayList<>();

    // Tabele tematyczne
    private final List<LootEntry> foodLoot = new ArrayList<>();
    private final List<LootEntry> medicalLoot = new ArrayList<>();
    private final List<LootEntry> gunsLoot = new ArrayList<>();
    private final List<LootEntry> magicLoot = new ArrayList<>();
    private final List<LootEntry> meleeLoot = new ArrayList<>();

    // Zarejestrowane przez admina skrzynie tematyczne na mapie
    private final Map<Location, ThemedChestType> configuredThemedChests = new ConcurrentHashMap<>();

    private final Random random = new Random();

    public enum LootCategory {
        GUN,
        AMMO,
        UPGRADE_KIT,
        CURE,
        BANDAGE,
        POTION,
        VANILLA,
        FOOD,
        ARMOR,
        MAGIC,
        BLOCK
    }

    public static class LootEntry {
        private final LootCategory category;
        private final double chance;
        private final int minAmount;
        private final int maxAmount;
        private final String param;

        public LootEntry(LootCategory category, double chance, int minAmount, int maxAmount, String param) {
            this.category = category;
            this.chance = chance;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.param = param;
        }

        public LootCategory getCategory() { return category; }
        public double getChance() { return chance; }
        public int getMinAmount() { return minAmount; }
        public int getMaxAmount() { return maxAmount; }
        public String getParam() { return param; }
    }

    public void loadFromConfig(FileConfiguration config) {
        chestLoot.clear();
        barrelLoot.clear();
        shelfLoot.clear();
        foodLoot.clear();
        medicalLoot.clear();
        gunsLoot.clear();
        magicLoot.clear();
        meleeLoot.clear();

        loadTable(config.getMapList("loot-tables.chests"), chestLoot);
        loadTable(config.getMapList("loot-tables.barrels"), barrelLoot);
        loadTable(config.getMapList("loot-tables.shelves"), shelfLoot);
        loadTable(config.getMapList("loot-tables.food"), foodLoot);
        loadTable(config.getMapList("loot-tables.medical"), medicalLoot);
        loadTable(config.getMapList("loot-tables.guns"), gunsLoot);
        loadTable(config.getMapList("loot-tables.magic"), magicLoot);
        loadTable(config.getMapList("loot-tables.melee"), meleeLoot);

        if (chestLoot.isEmpty()) loadDefaultChestLoot();
        if (barrelLoot.isEmpty()) loadDefaultBarrelLoot();
        if (shelfLoot.isEmpty()) loadDefaultShelfLoot();
        if (foodLoot.isEmpty()) loadDefaultFoodLoot();
        if (medicalLoot.isEmpty()) loadDefaultMedicalLoot();
        if (gunsLoot.isEmpty()) loadDefaultGunsLoot();
        if (magicLoot.isEmpty()) loadDefaultMagicLoot();
        if (meleeLoot.isEmpty()) loadDefaultMeleeLoot();

        loadThemedChestsFromConfig(config);
    }

    private void loadTable(List<Map<?, ?>> list, List<LootEntry> target) {
        if (list == null) return;
        for (Map<?, ?> map : list) {
            String catStr = (String) map.get("type");
            LootCategory cat;
            try {
                cat = LootCategory.valueOf(catStr.toUpperCase());
            } catch (Exception e) {
                continue;
            }
            double chance = map.get("chance") instanceof Number ? ((Number) map.get("chance")).doubleValue() : 0.5;
            int min = map.get("min") instanceof Number ? ((Number) map.get("min")).intValue() : 1;
            int max = map.get("max") instanceof Number ? ((Number) map.get("max")).intValue() : 1;
            String param = map.get("param") != null ? map.get("param").toString() : "";
            target.add(new LootEntry(cat, chance, min, max, param));
        }
    }

    private void loadDefaultChestLoot() {
        chestLoot.add(new LootEntry(LootCategory.GUN, 0.40, 1, 1, "flintlock_pistol"));
        chestLoot.add(new LootEntry(LootCategory.GUN, 0.25, 1, 1, "pepperbox"));
        chestLoot.add(new LootEntry(LootCategory.GUN, 0.20, 1, 1, "flintlock_musket"));
        chestLoot.add(new LootEntry(LootCategory.GUN, 0.20, 1, 1, "blunderbuss"));
        chestLoot.add(new LootEntry(LootCategory.AMMO, 0.65, 4, 12, "lead_bullet"));
        chestLoot.add(new LootEntry(LootCategory.AMMO, 0.35, 3, 8, "scatter_shot"));
        chestLoot.add(new LootEntry(LootCategory.AMMO, 0.20, 2, 5, "dragon_cartridge"));
        chestLoot.add(new LootEntry(LootCategory.UPGRADE_KIT, 0.25, 1, 1, "1"));
        chestLoot.add(new LootEntry(LootCategory.CURE, 0.35, 1, 1, ""));
        chestLoot.add(new LootEntry(LootCategory.BANDAGE, 0.50, 1, 3, ""));
        chestLoot.add(new LootEntry(LootCategory.VANILLA, 0.35, 1, 1, "IRON_SWORD"));
        chestLoot.add(new LootEntry(LootCategory.VANILLA, 0.20, 1, 1, "BOW"));
        chestLoot.add(new LootEntry(LootCategory.ARMOR, 0.30, 1, 1, "IRON_CHESTPLATE"));
        chestLoot.add(new LootEntry(LootCategory.FOOD, 0.50, 2, 5, "COOKED_BEEF"));
    }

    private void loadDefaultBarrelLoot() {
        barrelLoot.add(new LootEntry(LootCategory.CURE, 0.50, 1, 2, ""));
        barrelLoot.add(new LootEntry(LootCategory.BANDAGE, 0.60, 2, 4, ""));
        barrelLoot.add(new LootEntry(LootCategory.FOOD, 0.80, 3, 8, "BREAD"));
        barrelLoot.add(new LootEntry(LootCategory.FOOD, 0.60, 2, 6, "GOLDEN_CARROT"));
        barrelLoot.add(new LootEntry(LootCategory.AMMO, 0.50, 4, 10, "lead_bullet"));
        barrelLoot.add(new LootEntry(LootCategory.AMMO, 0.30, 3, 6, "slug_cartridge"));
        barrelLoot.add(new LootEntry(LootCategory.UPGRADE_KIT, 0.15, 1, 1, "1"));
        barrelLoot.add(new LootEntry(LootCategory.GUN, 0.20, 1, 1, "flintlock_pistol"));
    }

    private void loadDefaultShelfLoot() {
        shelfLoot.add(new LootEntry(LootCategory.GUN, 0.65, 1, 1, "flintlock_musket"));
        shelfLoot.add(new LootEntry(LootCategory.GUN, 0.50, 1, 1, "blunderbuss"));
        shelfLoot.add(new LootEntry(LootCategory.GUN, 0.45, 1, 1, "pepperbox"));
        shelfLoot.add(new LootEntry(LootCategory.GUN, 0.40, 1, 1, "flintlock_pistol"));
        shelfLoot.add(new LootEntry(LootCategory.UPGRADE_KIT, 0.35, 1, 1, "2"));
        shelfLoot.add(new LootEntry(LootCategory.AMMO, 0.70, 6, 16, "lead_bullet"));
        shelfLoot.add(new LootEntry(LootCategory.AMMO, 0.45, 4, 10, "dragon_scatter_shot"));
        shelfLoot.add(new LootEntry(LootCategory.CURE, 0.25, 1, 1, ""));
        shelfLoot.add(new LootEntry(LootCategory.BANDAGE, 0.30, 1, 2, ""));
        shelfLoot.add(new LootEntry(LootCategory.VANILLA, 0.35, 1, 1, "CROSSBOW"));
    }

    private void loadDefaultFoodLoot() {
        foodLoot.add(new LootEntry(LootCategory.FOOD, 0.90, 4, 10, "BREAD"));
        foodLoot.add(new LootEntry(LootCategory.FOOD, 0.80, 3, 6, "COOKED_BEEF"));
        foodLoot.add(new LootEntry(LootCategory.FOOD, 0.60, 2, 5, "GOLDEN_CARROT"));
        foodLoot.add(new LootEntry(LootCategory.FOOD, 0.25, 1, 2, "GOLDEN_APPLE"));
        foodLoot.add(new LootEntry(LootCategory.POTION, 0.40, 1, 2, "REGENERATION"));
        foodLoot.add(new LootEntry(LootCategory.POTION, 0.40, 1, 2, "SPEED"));
    }

    private void loadDefaultMedicalLoot() {
        medicalLoot.add(new LootEntry(LootCategory.CURE, 0.75, 1, 2, ""));
        medicalLoot.add(new LootEntry(LootCategory.BANDAGE, 0.90, 2, 5, ""));
        medicalLoot.add(new LootEntry(LootCategory.POTION, 0.60, 1, 2, "HEALING"));
        medicalLoot.add(new LootEntry(LootCategory.POTION, 0.50, 1, 2, "REGENERATION"));
        medicalLoot.add(new LootEntry(LootCategory.FOOD, 0.40, 1, 2, "GOLDEN_APPLE"));
    }

    private void loadDefaultGunsLoot() {
        gunsLoot.add(new LootEntry(LootCategory.GUN, 0.70, 1, 1, "flintlock_pistol"));
        gunsLoot.add(new LootEntry(LootCategory.GUN, 0.50, 1, 1, "flintlock_musket"));
        gunsLoot.add(new LootEntry(LootCategory.GUN, 0.45, 1, 1, "blunderbuss"));
        gunsLoot.add(new LootEntry(LootCategory.GUN, 0.40, 1, 1, "pepperbox"));
        gunsLoot.add(new LootEntry(LootCategory.AMMO, 0.90, 8, 20, "lead_bullet"));
        gunsLoot.add(new LootEntry(LootCategory.AMMO, 0.50, 6, 12, "scatter_shot"));
        gunsLoot.add(new LootEntry(LootCategory.AMMO, 0.40, 4, 8, "slug_cartridge"));
        gunsLoot.add(new LootEntry(LootCategory.AMMO, 0.30, 3, 6, "dragon_cartridge"));
        gunsLoot.add(new LootEntry(LootCategory.UPGRADE_KIT, 0.50, 1, 2, "1"));
        gunsLoot.add(new LootEntry(LootCategory.UPGRADE_KIT, 0.25, 1, 1, "2"));
    }

    private void loadDefaultMagicLoot() {
        magicLoot.add(new LootEntry(LootCategory.MAGIC, 0.70, 1, 2, "SCROLL_FIRE"));
        magicLoot.add(new LootEntry(LootCategory.MAGIC, 0.60, 1, 2, "SCROLL_FROST"));
        magicLoot.add(new LootEntry(LootCategory.MAGIC, 0.50, 1, 1, "SCROLL_LIGHTNING"));
        magicLoot.add(new LootEntry(LootCategory.POTION, 0.60, 1, 2, "SPEED"));
        magicLoot.add(new LootEntry(LootCategory.VANILLA, 0.40, 1, 1, "DIAMOND_SWORD"));
    }

    private void loadDefaultMeleeLoot() {
        meleeLoot.add(new LootEntry(LootCategory.VANILLA, 0.80, 1, 1, "DIAMOND_SWORD"));
        meleeLoot.add(new LootEntry(LootCategory.VANILLA, 0.70, 1, 1, "IRON_SWORD"));
        meleeLoot.add(new LootEntry(LootCategory.VANILLA, 0.60, 1, 1, "IRON_AXE"));
        meleeLoot.add(new LootEntry(LootCategory.VANILLA, 0.50, 1, 1, "SHIELD"));
        meleeLoot.add(new LootEntry(LootCategory.ARMOR, 0.60, 1, 1, "IRON_CHESTPLATE"));
        meleeLoot.add(new LootEntry(LootCategory.ARMOR, 0.60, 1, 1, "IRON_LEGGINGS"));
        meleeLoot.add(new LootEntry(LootCategory.ARMOR, 0.60, 1, 1, "IRON_BOOTS"));
    }

    /**
     * Wypełnia kontener (skrzynię/beczkę) losowym lootem z uwzględnieniem motywu sklepowego.
     */
    public void populateContainer(Block block, BattleRoyaleArena arena) {
        if (block == null) return;
        BlockState state = block.getState();
        if (!(state instanceof Container container)) return;

        Inventory inv = container.getInventory();
        inv.clear();

        List<LootEntry> table = selectLootTable(block);

        int minItems = 2;
        int maxItems = Math.min(inv.getSize() / 3, 6);
        int itemsPlaced = 0;

        List<LootEntry> shuffled = new ArrayList<>(table);
        Collections.shuffle(shuffled);

        for (LootEntry entry : shuffled) {
            if (random.nextDouble() <= entry.getChance()) {
                ItemStack is = generateItem(entry, arena);
                if (is != null) {
                    int slot = findRandomEmptySlot(inv);
                    if (slot != -1) {
                        inv.setItem(slot, is);
                        itemsPlaced++;
                        if (itemsPlaced >= maxItems) break;
                    }
                }
            }
        }

        // Gwarancja przynajmniej 1 przedmiotu
        if (itemsPlaced == 0 && !table.isEmpty()) {
            LootEntry fallback = table.get(random.nextInt(table.size()));
            ItemStack is = generateItem(fallback, arena);
            if (is != null) {
                inv.setItem(random.nextInt(inv.getSize()), is);
            }
        }
    }

    private List<LootEntry> selectLootTable(Block block) {
        Location loc = block.getLocation();

        // 1. Sprawdź czy to oznaczona przez admina skrzynia tematyczna
        ThemedChestType themedType = configuredThemedChests.get(loc);
        if (themedType == null) {
            // Spróbuj z zaokrąglonymi koordynatami bloku
            Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            themedType = configuredThemedChests.get(blockLoc);
        }

        if (themedType != null) {
            if (themedType == ThemedChestType.RANDOM) {
                ThemedChestType[] all = { ThemedChestType.FOOD, ThemedChestType.MEDICAL, ThemedChestType.GUNS, ThemedChestType.MAGIC, ThemedChestType.MELEE, ThemedChestType.MIXED };
                themedType = all[random.nextInt(all.length)];
            }
            switch (themedType) {
                case FOOD: return foodLoot;
                case MEDICAL: return medicalLoot;
                case GUNS: return gunsLoot;
                case MAGIC: return magicLoot;
                case MELEE: return meleeLoot;
                case MIXED: return chestLoot;
                default: break;
            }
        }

        // 2. Domyślny wybór według typu bloku
        Material type = block.getType();
        if (type == Material.BARREL) {
            return barrelLoot;
        } else if (type.name().contains("BOOKSHELF") || type.name().contains("SHELF")) {
            return shelfLoot;
        }
        return chestLoot;
    }

    public ItemStack generateItem(LootEntry entry, BattleRoyaleArena arena) {
        int amount = entry.getMinAmount() + random.nextInt(Math.max(1, entry.getMaxAmount() - entry.getMinAmount() + 1));

        switch (entry.getCategory()) {
            case GUN:
                GunType gt = GunType.fromId(entry.getParam());
                if (gt == null) return BattleRoyaleWeaponHelper.createRandomGun(1, 2);
                int lvl = 1 + (random.nextDouble() < 0.25 ? 1 : 0);
                boolean rifling = random.nextDouble() < 0.30;
                boolean lock = random.nextDouble() < 0.25;
                boolean scope = gt == GunType.FLINTLOCK_MUSKET && random.nextDouble() < 0.35;
                boolean bayonet = random.nextDouble() < 0.20;
                return BattleRoyaleWeaponHelper.createGun(gt, lvl, rifling, lock, scope, bayonet, GunUniqueMod.NONE);

            case AMMO:
                AmmoType at = AmmoType.fromId(entry.getParam());
                if (at == null) at = AmmoType.LEAD_BULLET;
                return BattleRoyaleWeaponHelper.createAmmo(at, amount);

            case UPGRADE_KIT:
                int tier = 1;
                try {
                    tier = Integer.parseInt(entry.getParam());
                } catch (Exception ignored) {}
                return BattleRoyaleWeaponHelper.createUpgradeKit(tier);

            case CURE:
                return BattleRoyaleWeaponHelper.createInfectionCure(
                        arena.getCureMaterial(),
                        arena.getCureDisplayName(),
                        arena.getCureLore(),
                        arena.getCureCustomModelData()
                );

            case BANDAGE:
                return BandageHandler.createBandage(amount);

            case POTION:
                return createCustomPotion(entry.getParam());

            case MAGIC:
                return createMagicScroll(entry.getParam());

            case VANILLA:
                return BattleRoyaleWeaponHelper.createVanillaWeapon(entry.getParam().isEmpty() ? "IRON_SWORD" : entry.getParam());

            case ARMOR:
                Material armMat = Material.matchMaterial(entry.getParam());
                return new ItemStack(armMat != null ? armMat : Material.IRON_CHESTPLATE);

            case FOOD:
                Material foodMat = Material.matchMaterial(entry.getParam());
                return new ItemStack(foodMat != null ? foodMat : Material.BREAD, amount);

            case BLOCK:
                Material blockMat = Material.matchMaterial(entry.getParam());
                return new ItemStack(blockMat != null ? blockMat : Material.OAK_SLAB, amount);

            default:
                return null;
        }
    }

    private ItemStack createCustomPotion(String type) {
        ItemStack pot = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) pot.getItemMeta();
        if (meta != null) {
            if ("HEALING".equalsIgnoreCase(type)) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Mikstura Uzdrowienia");
                meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), true);
            } else if ("REGENERATION".equalsIgnoreCase(type)) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Mikstura Regeneracji");
                meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1), true);
            } else if ("SPEED".equalsIgnoreCase(type)) {
                meta.setDisplayName(ChatColor.AQUA + "Mikstura Zwinności");
                meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1), true);
            }
            pot.setItemMeta(meta);
        }
        return pot;
    }

    private ItemStack createMagicScroll(String param) {
        ItemStack scroll = new ItemStack(Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta meta = scroll.getItemMeta();
        if (meta != null) {
            String spellName = "Ognisty Płomień";
            if (param.contains("FROST")) spellName = "Mroźna Furia";
            else if (param.contains("LIGHTNING")) spellName = "Piorunujący Grom";
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Zwój Zaklęcia: " + ChatColor.YELLOW + spellName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Starożytny zwój bojowy.",
                    ChatColor.YELLOW + "Pojawia się w magicznych skrzyniach areny."
            ));
            scroll.setItemMeta(meta);
        }
        return scroll;
    }

    private int findRandomEmptySlot(Inventory inv) {
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack is = inv.getItem(i);
            if (is == null || is.getType().isAir()) {
                emptySlots.add(i);
            }
        }
        if (emptySlots.isEmpty()) return -1;
        return emptySlots.get(random.nextInt(emptySlots.size()));
    }

    // --- ZARZĄDZANIE SKRZYNIAMI TEMATYCZNYMI (ADMIN TOOLS) ---

    public void setThemedChest(Location loc, ThemedChestType type) {
        if (loc == null) return;
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        configuredThemedChests.put(blockLoc, type);
    }

    public boolean removeThemedChest(Location loc) {
        if (loc == null) return false;
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return configuredThemedChests.remove(blockLoc) != null || configuredThemedChests.remove(loc) != null;
    }

    public ThemedChestType getThemedChest(Location loc) {
        if (loc == null) return null;
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return configuredThemedChests.get(blockLoc);
    }

    public Map<Location, ThemedChestType> getConfiguredThemedChests() {
        return Collections.unmodifiableMap(configuredThemedChests);
    }

    public void loadThemedChestsFromConfig(FileConfiguration config) {
        configuredThemedChests.clear();
        ConfigurationSection sec = config.getConfigurationSection("arena.themed-chests");
        if (sec == null) return;

        String worldName = config.getString("arena.world-name", "hungergames_arena");
        World w = Bukkit.getWorld(worldName);

        for (String key : sec.getKeys(false)) {
            String typeStr = sec.getString(key + ".type", "MIXED");
            ThemedChestType type = ThemedChestType.fromString(typeStr);
            double x = sec.getDouble(key + ".x");
            double y = sec.getDouble(key + ".y");
            double z = sec.getDouble(key + ".z");
            Location loc = new Location(w, (int) x, (int) y, (int) z);
            configuredThemedChests.put(loc, type);
        }
    }

    public void saveThemedChestsToConfig(FileConfiguration config, File file) {
        config.set("arena.themed-chests", null);
        int index = 1;
        for (Map.Entry<Location, ThemedChestType> entry : configuredThemedChests.entrySet()) {
            Location loc = entry.getKey();
            String path = "arena.themed-chests.chest_" + index;
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".type", entry.getValue().name());
            index++;
        }
        try {
            config.save(file);
        } catch (IOException e) {
            AmonPackPlugin.plugin.getLogger().warning("[BattleRoyale] Błąd zapisu skrzyń tematycznych: " + e.getMessage());
        }
    }
}
