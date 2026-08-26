package RPG.Progression.service;

import CustomContent.Items.CustomItem;
import CustomContent.Items.CustomItemManager;
import Plugin.AmonPackPlugin;
import RPG.Levels.Objects.LevelSkill;
import RPG.Levels.PlayerLevelMenager;
import RPG.Progression.model.QuestReward;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class RewardService {

    public RewardService() {
    }

    public void giveReward(Player player, QuestReward reward) {
        if (player == null || reward == null || reward.isEmpty()) return;

        // 1. Money (via command or console)
        if (reward.getMoney() > 0) {
            String cmd = "eco give " + player.getName() + " " + reward.getMoney();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            player.sendMessage(ChatColor.GOLD + "[Nagroda] " + ChatColor.YELLOW + "+" + reward.getMoney() + "$");
        }

        // 2. Player Experience
        if (reward.getPlayerExp() > 0) {
            player.giveExp(reward.getPlayerExp());
            player.sendMessage(ChatColor.GREEN + "[Nagroda] " + ChatColor.AQUA + "+" + reward.getPlayerExp() + " EXP");
        }

        // 3. Items (Vanilla or Custom Items)
        if (!reward.getItems().isEmpty()) {
            for (Map.Entry<String, Integer> entry : reward.getItems().entrySet()) {
                String itemKey = entry.getKey();
                int amount = entry.getValue();
                ItemStack stack = null;

                // Check Custom Items first
                if (AmonPackPlugin.customItemManager != null) {
                    CustomItem customItem = AmonPackPlugin.customItemManager.getCustomItem(itemKey);
                    if (customItem != null) {
                        stack = AmonPackPlugin.customItemManager.createItemStack(itemKey);
                        if (stack != null) {
                            stack.setAmount(amount);
                        }
                    }
                }

                // Check Vanilla Material
                if (stack == null) {
                    Material mat = Material.matchMaterial(itemKey);
                    if (mat != null) {
                        stack = new ItemStack(mat, amount);
                    }
                }

                if (stack != null) {
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
                    if (!leftover.isEmpty()) {
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                        player.sendMessage(ChatColor.YELLOW + "[Ekwipunek] Przedmioty nie zmieściły się i spadły na ziemię!");
                    }
                }
            }
        }

        // 4. Skill Experience (RPG Levels)
        if (!reward.getSkillExp().isEmpty() && AmonPackPlugin.ENABLE_SKILL_TREE) {
            for (Map.Entry<LevelSkill.SkillType, Double> entry : reward.getSkillExp().entrySet()) {
                LevelSkill.SkillType type = entry.getKey();
                double exp = entry.getValue();
                if (type != null && exp > 0 && AmonPackPlugin.getPlayerMenager() != null) {
                    AmonPackPlugin.getPlayerMenager().AddPoints(type, player, (int) exp);
                    player.sendMessage(ChatColor.GOLD + "[Umiejętności] " + ChatColor.YELLOW + "+" + exp + " EXP dla " + type.name());
                }
            }
        }

        // 5. Commands
        if (!reward.getCommands().isEmpty()) {
            for (String cmd : reward.getCommands()) {
                String formatted = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
            }
        }

        // 6. Broadcast message
        if (reward.getBroadcastMessage() != null && !reward.getBroadcastMessage().isEmpty()) {
            String msg = ChatColor.translateAlternateColorCodes('&', reward.getBroadcastMessage().replace("%player%", player.getName()));
            Bukkit.broadcastMessage(msg);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
    }
}
