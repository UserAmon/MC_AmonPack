package CustomContent.Blocks;

import CustomContent.Items.CustomItemManager;
import CustomContent.Pack.PackManager;
import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class CustomBlockManager {

    private final Map<String, CustomBlock> customBlocks = new LinkedHashMap<>();
    private final Map<String, String> placedBlocks = new HashMap<>(); // "world,x,y,z" -> block_id
    private final Map<String, ItemDisplay> activeDisplays = new HashMap<>();

    private final PackManager packManager;
    private final CustomItemManager itemManager;
    private File storageFile;
    private boolean isDirty = false;
    private org.bukkit.scheduler.BukkitTask autoSaveTask;

    public CustomBlockManager(PackManager packManager, CustomItemManager itemManager) {
        this.packManager = packManager;
        this.itemManager = itemManager;
        File packFolder = new File(AmonPackPlugin.plugin.getDataFolder(), "pack");
        if (!packFolder.exists()) packFolder.mkdirs();
        this.storageFile = new File(packFolder, "placed_blocks.yml");
    }

    public void load() {
        customBlocks.clear();
        File packFolder = new File(AmonPackPlugin.plugin.getDataFolder(), "pack");
        if (!packFolder.exists()) packFolder.mkdirs();

        File file = new File(packFolder, "custom_blocks.yml");
        if (!file.exists()) {
            AmonPackPlugin.plugin.saveResource("pack/custom_blocks.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getConfigurationSection("blocks") != null) {
            for (String key : cfg.getConfigurationSection("blocks").getKeys(false)) {
                String path = "blocks." + key;
                String name = ChatColor.translateAlternateColorCodes('&', cfg.getString(path + ".display_name", key));
                String matStr = cfg.getString(path + ".base_material", "NOTE_BLOCK");
                Material mat = Material.matchMaterial(matStr);
                if (mat == null) mat = Material.NOTE_BLOCK;

                int cmd = cfg.getInt(path + ".custom_model_data", 30001);
                CustomBlock cb = new CustomBlock(key, name, mat, cmd);
                cb.setHardness(cfg.getDouble(path + ".hardness", 3.0));
                cb.setRequiredTool(cfg.getString(path + ".required_tool", "PICKAXE"));
                cb.setRequiredTier(cfg.getInt(path + ".required_tier", 2));

                // Drops
                cb.setDropCustomItemId(cfg.getString(path + ".drops.custom_item", null));
                String vanillaDrop = cfg.getString(path + ".drops.vanilla_material", null);
                if (vanillaDrop != null) cb.setDropVanillaMaterial(Material.matchMaterial(vanillaDrop));
                cb.setDropMin(cfg.getInt(path + ".drops.min", 1));
                cb.setDropMax(cfg.getInt(path + ".drops.max", 2));
                cb.setExp(cfg.getDouble(path + ".drops.exp", 10.0));

                // Generation
                cb.setGenerateInWorld(cfg.getBoolean(path + ".generation.enabled", true));
                cb.setVeinSize(cfg.getInt(path + ".generation.vein_size", 4));
                cb.setVeinsPerChunk(cfg.getInt(path + ".generation.veins_per_chunk", 4));
                cb.setMinY(cfg.getInt(path + ".generation.min_y", -64));
                cb.setMaxY(cfg.getInt(path + ".generation.max_y", 32));

                customBlocks.put(key.toLowerCase(Locale.ROOT), cb);

                // Rejestracja w packu
                String modelPath = cfg.getString(path + ".model", "block/" + key);
                if (!modelPath.startsWith("amonpack:")) {
                    modelPath = "amonpack:" + modelPath;
                }
                packManager.registerModelOverride(mat.name(), cmd, modelPath);
            }
        }

        if (!customBlocks.containsKey("magic_crafting_table")) {
            CustomBlock tableBlock = new CustomBlock("magic_crafting_table", "§d§lMagiczny Stół Warsztatowy", Material.NOTE_BLOCK, 30002);
            tableBlock.setHardness(2.5);
            tableBlock.setRequiredTool("PICKAXE");
            tableBlock.setRequiredTier(0);
            tableBlock.setDropCustomItemId("magic_crafting_table");
            tableBlock.setDropMin(1);
            tableBlock.setDropMax(1);
            tableBlock.setExp(5.0);
            tableBlock.setGenerateInWorld(false);
            customBlocks.put("magic_crafting_table", tableBlock);
            packManager.registerModelOverride("note_block", 30002, "amonpack:block/magic_crafting_table");
        }

        loadPlacedBlocks();

        // Okresowy autosave co 60 sekund jeśli były modyfikacje
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(AmonPackPlugin.plugin, () -> {
            if (isDirty) {
                savePlacedBlocksSync();
            }
        }, 1200L, 1200L);
    }

    public void unload() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        if (isDirty) {
            savePlacedBlocksSync();
        }
        for (ItemDisplay display : activeDisplays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        activeDisplays.clear();
    }

    public void placeBlock(Block block, String customBlockId) {
        placeBlock(block, customBlockId, true, true);
    }

    public void placeBlock(Block block, String customBlockId, boolean applyPhysics, boolean saveImmediately) {
        CustomBlock cb = customBlocks.get(customBlockId.toLowerCase(Locale.ROOT));
        if (cb == null) return;

        block.setType(cb.getBaseMaterial(), applyPhysics);
        String key = locKey(block.getLocation());
        placedBlocks.put(key, cb.getId());
        isDirty = true;

        // Spawn visual ItemDisplay z modelem 3D
        spawnDisplay(block.getLocation(), cb);

        if (saveImmediately) {
            savePlacedBlocksAsync();
        }
    }

    public CustomBlock removeBlock(Block block) {
        String key = locKey(block.getLocation());
        String customBlockId = placedBlocks.remove(key);
        if (customBlockId == null) return null;

        isDirty = true;
        ItemDisplay display = activeDisplays.remove(key);
        if (display != null && display.isValid()) {
            display.remove();
        }
        savePlacedBlocksAsync();
        return customBlocks.get(customBlockId.toLowerCase(Locale.ROOT));
    }

    public CustomBlock getCustomBlock(Block block) {
        if (block == null) return null;
        String key = locKey(block.getLocation());
        String id = placedBlocks.get(key);
        if (id == null) return null;
        return customBlocks.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean isCustomBlock(Block block) {
        return getCustomBlock(block) != null;
    }

    private void spawnDisplay(Location loc, CustomBlock cb) {
        try {
            Location center = loc.getBlock().getLocation().add(0.5, 0.5, 0.5);
            ItemDisplay display = loc.getWorld().spawn(center, ItemDisplay.class, d -> {
                ItemStack item = new ItemStack(cb.getBaseMaterial());
                var meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(cb.getCustomModelData());
                    item.setItemMeta(meta);
                }
                d.setItemStack(item);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            });
            activeDisplays.put(locKey(loc), display);
        } catch (Throwable ignored) {
        }
    }

    private String locKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private void loadPlacedBlocks() {
        if (!storageFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(storageFile);
        if (cfg.getConfigurationSection("blocks") != null) {
            for (String key : cfg.getConfigurationSection("blocks").getKeys(false)) {
                String id = cfg.getString("blocks." + key);
                placedBlocks.put(key, id);
            }
        }
    }

    public void savePlacedBlocksAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(AmonPackPlugin.plugin, this::savePlacedBlocksSync);
    }

    private synchronized void savePlacedBlocksSync() {
        try {
            FileConfiguration cfg = new YamlConfiguration();
            Map<String, String> copy = new HashMap<>(placedBlocks);
            for (Map.Entry<String, String> entry : copy.entrySet()) {
                cfg.set("blocks." + entry.getKey(), entry.getValue());
            }
            cfg.save(storageFile);
            isDirty = false;
        } catch (Exception ignored) {}
    }

    public Map<String, CustomBlock> getAllCustomBlocks() {
        return Collections.unmodifiableMap(customBlocks);
    }
}
