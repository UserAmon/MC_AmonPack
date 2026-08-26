package RPG.Progression.service;

import CustomContent.Items.CustomItemManager;
import Plugin.AmonPackPlugin;
import RPG.Progression.model.ContentRestriction;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.StageType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RestrictionService {

    public static final String BYPASS_PERMISSION = "amonpack.progression.bypass";
    private final ContentRestriction restriction = new ContentRestriction();
    private final Map<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();

    public RestrictionService() {
    }

    public void load(FileConfiguration config) {
        restriction.clear();
        if (config == null) return;

        ConfigurationSection restSec = config.getConfigurationSection("restrictions");
        if (restSec == null) return;

        // 1. Items
        ConfigurationSection itemsSec = restSec.getConfigurationSection("items");
        if (itemsSec != null) {
            for (String matKey : itemsSec.getKeys(false)) {
                Material mat = Material.matchMaterial(matKey);
                StageType stage = StageType.fromName(itemsSec.getString(matKey));
                if (mat != null) {
                    restriction.addItemRestriction(mat, stage);
                }
            }
        }

        // 2. Blocks
        ConfigurationSection blocksSec = restSec.getConfigurationSection("blocks");
        if (blocksSec != null) {
            for (String matKey : blocksSec.getKeys(false)) {
                Material mat = Material.matchMaterial(matKey);
                StageType stage = StageType.fromName(blocksSec.getString(matKey));
                if (mat != null) {
                    restriction.addBlockRestriction(mat, stage);
                }
            }
        }

        // 3. Custom Items
        ConfigurationSection customItemsSec = restSec.getConfigurationSection("custom_items");
        if (customItemsSec != null) {
            for (String cKey : customItemsSec.getKeys(false)) {
                StageType stage = StageType.fromName(customItemsSec.getString(cKey));
                restriction.addCustomItemRestriction(cKey, stage);
            }
        }

        // 4. Craftable Molds
        ConfigurationSection moldsSec = restSec.getConfigurationSection("craftable_molds");
        if (moldsSec != null) {
            for (String mKey : moldsSec.getKeys(false)) {
                StageType stage = StageType.fromName(moldsSec.getString(mKey));
                restriction.addCraftableMoldRestriction(mKey, stage);
            }
        }

        // 5. Dimensions
        ConfigurationSection dimSec = restSec.getConfigurationSection("dimensions");
        if (dimSec != null) {
            for (String dKey : dimSec.getKeys(false)) {
                StageType stage = StageType.fromName(dimSec.getString(dKey));
                try {
                    World.Environment env = World.Environment.valueOf(dKey.toUpperCase(Locale.ROOT));
                    restriction.addDimensionRestriction(env, stage);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public boolean isAllowedItem(Player player, PlayerProgressionData data, ItemStack stack) {
        if (player.hasPermission(BYPASS_PERMISSION)) return true;
        if (stack == null || stack.getType() == Material.AIR) return true;

        // Check custom item first
        if (AmonPackPlugin.customItemManager != null) {
            String customId = AmonPackPlugin.customItemManager.getCustomItemId(stack);
            if (customId != null) {
                StageType req = restriction.getRequiredStageForCustomItem(customId);
                if (req != null && !data.getCurrentStage().isAtLeast(req)) {
                    sendRestrictedMessage(player, "Ten przedmiot wymaga odblokowania etapu " + req.getDisplayName() + "§c!");
                    return false;
                }
            }
        }

        // Check vanilla material
        StageType req = restriction.getRequiredStageForItem(stack.getType());
        if (req != null && !data.getCurrentStage().isAtLeast(req)) {
            sendRestrictedMessage(player, "Ten przedmiot wymaga odblokowania etapu " + req.getDisplayName() + "§c!");
            return false;
        }

        return true;
    }

    public boolean isAllowedBlock(Player player, PlayerProgressionData data, Material blockType) {
        if (player.hasPermission(BYPASS_PERMISSION)) return true;
        if (blockType == null || blockType == Material.AIR) return true;

        StageType req = restriction.getRequiredStageForBlock(blockType);
        if (req != null && !data.getCurrentStage().isAtLeast(req)) {
            sendRestrictedMessage(player, "Wydobycie tego bloku wymaga odblokowania etapu " + req.getDisplayName() + "§c!");
            return false;
        }

        return true;
    }

    public boolean isAllowedCraftableMold(Player player, PlayerProgressionData data, String moldId) {
        if (player.hasPermission(BYPASS_PERMISSION)) return true;
        if (moldId == null) return true;

        StageType req = restriction.getRequiredStageForCraftableMold(moldId);
        if (req != null && !data.getCurrentStage().isAtLeast(req)) {
            sendRestrictedMessage(player, "Wytworzenie tego przedmiotu w kuźni wymaga odblokowania etapu " + req.getDisplayName() + "§c!");
            return false;
        }

        return true;
    }

    public boolean isAllowedDimension(Player player, PlayerProgressionData data, World.Environment env) {
        if (player.hasPermission(BYPASS_PERMISSION)) return true;
        if (env == null || env == World.Environment.NORMAL) return true;

        StageType req = restriction.getRequiredStageForDimension(env);
        if (req != null && !data.getCurrentStage().isAtLeast(req)) {
            sendRestrictedMessage(player, "Podróż do tego wymiaru wymaga odblokowania etapu " + req.getDisplayName() + "§c!");
            return false;
        }

        return true;
    }

    public void sendRestrictedMessage(Player player, String message) {
        if (player == null) return;
        long now = System.currentTimeMillis();
        long last = messageCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 2000) return; // 2s cooldown

        messageCooldowns.put(player.getUniqueId(), now);
        player.sendMessage(ChatColor.RED + "[Progresja] " + ChatColor.translateAlternateColorCodes('&', message));
    }

    public ContentRestriction getRestriction() {
        return restriction;
    }
}
