package RPG.BattleRoyale.Loot;

import CustomContent.Guns.AmmoType;
import CustomContent.Guns.GunType;
import CustomContent.Guns.GunUniqueMod;
import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Backpacks.BackpackManager;
import RPG.BattleRoyale.BattleRoyaleArena;
import RPG.BattleRoyale.Items.BandageHandler;
import RPG.BattleRoyale.Keys.KeyManager;
import RPG.BattleRoyale.Keys.KeyType;
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
        BLOCK,
        KEY
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
            double chance = ((Number) map.getOrDefault("chance", 0.5)).doubleValue();
            int min = ((Number) map.getOrDefault("min", 1)).intValue();
            int max = ((Number) map.getOrDefault("max", 1)).intValue();
            String param = (String) map.getOrDefault("param", "");

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

        // Zbroje (leather, chain, iron)
        chestLoot.add(new LootEntry(LootCategory.ARMOR, 0.35, 1, 1, "LEATHER_HELMET"));
        chestLoot.add(new LootEntry(LootCategory.ARMOR, 0.35, 1, 1, "LEATHER_CHESTPLATE"));
        chestLoot.add(new LootEntry(LootCategory.ARMOR, 0.30, 1, 1, "CHAINMAIL_CHESTPLATE"));
        chestLoot.add(new LootEntry(LootCategory.ARMOR, 0.25, 1, 1, "CHAINMAIL_LEGGINGS"));
        chestLoot.add(new LootEntry(LootCategory.ARMOR, 0.20, 1, 1, "IRON_HELMET"));
        chestLoot.add(new LootEntry(LootCategory.ARMOR, 0.20, 1, 1, "IRON_CHESTPLATE"));
        chestLoot.add(new LootEntry(LootCategory.ARMOR, 0.20, 1, 1, "IRON_BOOTS"));

        chestLoot.add(new LootEntry(LootCategory.FOOD, 0.50, 2, 5, "COOKED_BEEF"));
        chestLoot.add(new LootEntry(LootCategory.BLOCK, 0.30, 3, 6, "OAK_SLAB"));
        chestLoot.add(new LootEntry(LootCategory.KEY, 0.06, 1, 1, ""));
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
        barrelLoot.add(new LootEntry(LootCategory.BLOCK, 0.25, 2, 4, "OAK_SLAB"));
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
        medicalLoot.add(new LootEntry(LootCategory.KEY, 0.12, 1, 1, "PHARMACY"));
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

        // Zbroje militarne
        gunsLoot.add(new LootEntry(LootCategory.ARMOR, 0.35, 1, 1, "CHAINMAIL_CHESTPLATE"));
        gunsLoot.add(new LootEntry(LootCategory.ARMOR, 0.35, 1, 1, "IRON_CHESTPLATE"));
        gunsLoot.add(new LootEntry(LootCategory.ARMOR, 0.30, 1, 1, "IRON_LEGGINGS"));
        gunsLoot.add(new LootEntry(LootCategory.ARMOR, 0.30, 1, 1, "IRON_BOOTS"));
        gunsLoot.add(new LootEntry(LootCategory.KEY, 0.15, 1, 1, "MILITARY_DEPOT"));
    }

    private void loadDefaultMagicLoot() {
        magicLoot.add(new LootEntry(LootCategory.MAGIC, 0.70, 1, 2, "SCROLL_FIRE"));
        magicLoot.add(new LootEntry(LootCategory.MAGIC, 0.60, 1, 2, "SCROLL_FROST"));
        magicLoot.add(new LootEntry(LootCategory.MAGIC, 0.50, 1, 1, "SCROLL_LIGHTNING"));
        magicLoot.add(new LootEntry(LootCategory.POTION, 0.60, 1, 2, "SPEED"));
        magicLoot.add(new LootEntry(LootCategory.VANILLA, 0.40, 1, 1, "IRON_SWORD"));
    }

    private void loadDefaultMeleeLoot() {
        meleeLoot.add(new LootEntry(LootCategory.VANILLA, 0.75, 1, 1, "IRON_SWORD"));
        meleeLoot.add(new LootEntry(LootCategory.VANILLA, 0.60, 1, 1, "IRON_AXE"));
        meleeLoot.add(new LootEntry(LootCategory.VANILLA, 0.50, 1, 1, "SHIELD"));
        meleeLoot.add(new LootEntry(LootCategory.ARMOR, 0.40, 1, 1, "CHAINMAIL_CHESTPLATE"));
        meleeLoot.add(new LootEntry(LootCategory.ARMOR, 0.35, 1, 1, "CHAINMAIL_LEGGINGS"));
        meleeLoot.add(new LootEntry(LootCategory.ARMOR, 0.30, 1, 1, "IRON_CHESTPLATE"));
        meleeLoot.add(new LootEntry(LootCategory.ARMOR, 0.30, 1, 1, "IRON_BOOTS"));
        meleeLoot.add(new LootEntry(LootCategory.ARMOR, 0.35, 1, 1, "LEATHER_CHESTPLATE"));
    }

    /**
     * Wypełnia skrzynię zrzutu zaopatrzenia (Air Drop) elitarnym łupem.
     */
    public void populateAirDropChest(Inventory inv) {
        if (inv == null) return;
        inv.clear();

        // 1. Zestaw Ulepszenia Poziom III
        inv.setItem(11, BattleRoyaleWeaponHelper.createUpgradeKit(3));

        // 2. Broń wysokiego poziomu (Pepperbox lub Muszkiet)
        GunType topGun = random.nextBoolean() ? GunType.PEPPERBOX : GunType.FLINTLOCK_MUSKET;
        inv.setItem(13, BattleRoyaleWeaponHelper.createGun(topGun, 2, true, true, false, false, GunUniqueMod.NONE));

        // 3. Amunicja wysokiej klasy
        inv.setItem(12, BattleRoyaleWeaponHelper.createAmmo(AmmoType.DRAGON_CARTRIDGE, 6));
        inv.setItem(14, BattleRoyaleWeaponHelper.createAmmo(AmmoType.LEAD_BULLET, 16));

        // 4. Element zbroi żelaznej
        Material[] ironArmors = { Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS };
        inv.setItem(15, new ItemStack(ironArmors[random.nextInt(ironArmors.length)]));

        // 5. Lekarstwo na infekcję
        inv.setItem(4, BattleRoyaleWeaponHelper.createInfectionCure());

        // 6. Plecak Taktyczny lub Przetrwania (Poziom II lub III)
        int bpTier = random.nextDouble() < 0.40 ? 3 : 2;
        inv.setItem(22, new BackpackManager().createBackpack(bpTier));

        // 7. Bandaże i złote jabłko
        inv.setItem(21, BandageHandler.createBandage(3));
        inv.setItem(23, new ItemStack(Material.GOLDEN_APPLE, 2));
    }

    /**
     * Generuje losowy przedmiot przetrwania (dla zombie z plecakami).
     */
    public ItemStack generateRandomSurvivalItem() {
        int r = random.nextInt(6);
        switch (r) {
            case 0: return BandageHandler.createBandage(2);
            case 1: return BattleRoyaleWeaponHelper.createAmmo(AmmoType.LEAD_BULLET, 8);
            case 2: return new ItemStack(Material.COOKED_BEEF, 3);
            case 3: return BattleRoyaleWeaponHelper.createUpgradeKit(1);
            case 4:
                Material[] armors = { Material.CHAINMAIL_CHESTPLATE, Material.IRON_HELMET, Material.IRON_BOOTS, Material.LEATHER_CHESTPLATE };
                return new ItemStack(armors[random.nextInt(armors.length)]);
            case 5: default: return BattleRoyaleWeaponHelper.createInfectionCure();
        }
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

        ThemedChestType themedType = configuredThemedChests.get(loc);
        if (themedType == null) {
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

            case KEY:
                KeyType kt = KeyType.fromString(entry.getParam());
                if (kt == null) {
                    KeyType[] allKeys = KeyType.values();
                    kt = allKeys[random.nextInt(allKeys.length)];
                }
                return new KeyManager().createKey(kt);

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

    public void setThemedChest(Location loc, ThemedChestType type) {
        if (loc == null) return;
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        configuredThemedChests.put(blockLoc, type);
    }

    public void removeThemedChest(Location loc) {
        if (loc == null) return;
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        configuredThemedChests.remove(blockLoc);
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

    /**
     * Wypełnia skrzynię zrzutu zaopatrzenia (Air Drop) elitarnym łupem.
     */
    public void populateAirDropChest(Inventory inv) {
        if (inv == null) return;
        inv.clear();

        List<ItemStack> eliteLoot = new ArrayList<>();
        // 1. Zawsze plecak poziomu II lub III
        eliteLoot.add(new BackpackManager().createBackpack(random.nextDouble() < 0.4 ? 3 : 2));

        // 2. Wysokiej klasy broń palna z ulepszeniami
        GunType[] guns = { GunType.FLINTLOCK_RIFLE, GunType.FLINTLOCK_SHOTGUN, GunType.BLUNDERBUSS, GunType.REVOLVER };
        GunType selectedGun = guns[random.nextInt(guns.length)];
        eliteLoot.add(BattleRoyaleWeaponHelper.createGun(selectedGun, 2, true, true, false, false, GunUniqueMod.NONE));

        // 3. Duża paczka amunicji
        eliteLoot.add(BattleRoyaleWeaponHelper.createAmmo(AmmoType.LEAD_BULLET, 16 + random.nextInt(17)));

        // 4. Antidotum na wirusa
        eliteLoot.add(BattleRoyaleWeaponHelper.createInfectionCure());

        // 5. Zestaw bandaży
        eliteLoot.add(BandageHandler.createBandage(3 + random.nextInt(3)));

        // 6. Szansa na Zestaw ulepszeń broni poziomu II
        if (random.nextDouble() < 0.75) {
            eliteLoot.add(BattleRoyaleWeaponHelper.createUpgradeKit(2));
        }

        // 7. Pancerz żelazny
        Material[] armors = { Material.IRON_CHESTPLATE, Material.IRON_HELMET, Material.IRON_LEGGINGS, Material.IRON_BOOTS };
        eliteLoot.add(new ItemStack(armors[random.nextInt(armors.length)]));

        // 8. Eliksir leczenia lub regeneracji
        eliteLoot.add(createCustomPotion(random.nextBoolean() ? "HEALING" : "REGENERATION"));

        // Rozmieszczenie w losowych slotach
        for (ItemStack item : eliteLoot) {
            int slot = random.nextInt(inv.getSize());
            int attempts = 0;
            while (inv.getItem(slot) != null && attempts < 20) {
                slot = random.nextInt(inv.getSize());
                attempts++;
            }
            inv.setItem(slot, item);
        }
    }

    /**
     * Losowy przedmiot przetrwania do plecaków zombie lub skrzyń.
     */
    public ItemStack generateRandomSurvivalItem() {
        int roll = random.nextInt(7);
        switch (roll) {
            case 0:
                return BattleRoyaleWeaponHelper.createAmmo(AmmoType.LEAD_BULLET, 4 + random.nextInt(9));
            case 1:
                return BandageHandler.createBandage(1 + random.nextInt(2));
            case 2:
                return BattleRoyaleWeaponHelper.createInfectionCure();
            case 3:
                return BattleRoyaleWeaponHelper.createRandomGun(1, 2);
            case 4:
                Material[] foods = { Material.COOKED_BEEF, Material.BREAD, Material.GOLDEN_CARROT, Material.APPLE };
                return new ItemStack(foods[random.nextInt(foods.length)], 2 + random.nextInt(4));
            case 5:
                String[] pots = { "HEALING", "REGENERATION", "SPEED" };
                return createCustomPotion(pots[random.nextInt(pots.length)]);
            case 6:
            default:
                Material[] armors = { Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_HELMET, Material.IRON_BOOTS };
                return new ItemStack(armors[random.nextInt(armors.length)]);
        }
    }

    public void shiftChests(int dx, int dy, int dz, World targetWorld) {
        Map<Location, ThemedChestType> shifted = new ConcurrentHashMap<>();
        for (Map.Entry<Location, ThemedChestType> entry : configuredThemedChests.entrySet()) {
            Location old = entry.getKey();
            World w = targetWorld != null ? targetWorld : old.getWorld();
            Location newLoc = new Location(w, old.getBlockX() + dx, old.getBlockY() + dy, old.getBlockZ() + dz);
            shifted.put(newLoc, entry.getValue());
        }
        configuredThemedChests.clear();
        configuredThemedChests.putAll(shifted);
    }
}
