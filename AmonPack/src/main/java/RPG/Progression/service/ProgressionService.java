package RPG.Progression.service;

import Plugin.AmonPackPlugin;
import RPG.Progression.event.QuestCompleteEvent;
import RPG.Progression.model.ObjectiveType;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.Quest;
import RPG.Progression.model.StageType;
import RPG.Progression.storage.IProgressionStorage;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProgressionService {

    private final QuestRegistry questRegistry;
    private final StageService stageService;
    private final RewardService rewardService;
    private final IProgressionStorage storage;
    private final Map<UUID, PlayerProgressionData> playerDataMap = new ConcurrentHashMap<>();

    public ProgressionService(QuestRegistry questRegistry, StageService stageService, RewardService rewardService, IProgressionStorage storage) {
        this.questRegistry = questRegistry;
        this.stageService = stageService;
        this.rewardService = rewardService;
        this.storage = storage;
    }

    public PlayerProgressionData getPlayerData(Player player) {
        if (player == null) return null;
        return playerDataMap.computeIfAbsent(player.getUniqueId(), uuid -> storage.loadPlayer(uuid, player.getName()));
    }

    public PlayerProgressionData getPlayerData(UUID uuid) {
        if (uuid == null) return null;
        return playerDataMap.get(uuid);
    }

    public void loadPlayer(Player player) {
        if (player == null) return;
        PlayerProgressionData data = storage.loadPlayer(player.getUniqueId(), player.getName());
        playerDataMap.put(player.getUniqueId(), data);
        checkRetroactiveObjectives(player, data);
    }

    public void unloadPlayer(Player player) {
        if (player == null) return;
        PlayerProgressionData data = playerDataMap.remove(player.getUniqueId());
        if (data != null && data.isDirty()) {
            storage.savePlayer(data);
        }
    }

    public void saveAllOnline() {
        storage.saveAll(playerDataMap.values());
    }

    public void handleObjective(Player player, ObjectiveType type, String target, int amount) {
        if (player == null || type == null || amount <= 0) return;
        PlayerProgressionData data = getPlayerData(player);
        if (data == null) return;

        StageType currentStage = data.getCurrentStage();
        List<Quest> stageQuests = questRegistry.getQuestsForStage(currentStage);

        for (Quest quest : stageQuests) {
            if (quest.getObjectiveType() != type) continue;
            if (data.isQuestCompleted(quest.getId())) continue;
            if (!isQuestUnlocked(data, quest)) continue;

            if (!matchesTarget(quest, type, target)) continue;

            // Process progress
            boolean completed = false;

            if (type == ObjectiveType.MINE_TO_DEPTH) {
                try {
                    int targetDepth = Integer.parseInt(quest.getTarget());
                    int currentY = Integer.parseInt(target);
                    if (currentY <= targetDepth) {
                        data.setProgress(quest.getId(), 1);
                        completed = true;
                    }
                } catch (Exception ignored) {
                }
            } else if (type == ObjectiveType.TRAVEL_DISTANCE) {
                int currentDist = amount;
                int reqDist = quest.getRequiredAmount();
                data.setProgress(quest.getId(), Math.min(reqDist, currentDist));
                if (currentDist >= reqDist) {
                    completed = true;
                }
            } else if (type == ObjectiveType.REACH_SKILL_LEVEL) {
                int currentLvl = amount;
                int reqLvl = quest.getRequiredAmount();
                data.setProgress(quest.getId(), Math.min(reqLvl, currentLvl));
                if (currentLvl >= reqLvl) {
                    completed = true;
                }
            } else {
                // Cumulative progress
                int newProg = data.addProgress(quest.getId(), amount);
                if (newProg >= quest.getRequiredAmount()) {
                    completed = true;
                } else {
                    // Send actionbar progress feedback
                    sendActionBarProgress(player, quest, newProg);
                }
            }

            if (completed) {
                completeQuest(player, data, quest);
            }
        }
    }

    private boolean matchesTarget(Quest quest, ObjectiveType type, String target) {
        if (quest.getTarget() == null || quest.getTarget().isEmpty()) return true;
        if (target == null) return false;

        String qTarget = quest.getTarget().trim();
        String t = target.trim();

        if (qTarget.equalsIgnoreCase("ANY") || qTarget.equalsIgnoreCase("*")) return true;

        if (qTarget.contains(",")) {
            for (String part : qTarget.split(",")) {
                if (matchesSingleTarget(part.trim(), type, t)) return true;
            }
            return false;
        }

        return matchesSingleTarget(qTarget, type, t);
    }

    private boolean matchesSingleTarget(String qTarget, ObjectiveType type, String t) {
        if (qTarget.equalsIgnoreCase(t)) return true;

        // custom: prefix compatibility
        if (qTarget.toLowerCase(Locale.ROOT).startsWith("custom:") && !t.toLowerCase(Locale.ROOT).startsWith("custom:")) {
            if (qTarget.substring(7).equalsIgnoreCase(t)) return true;
        }
        if (t.toLowerCase(Locale.ROOT).startsWith("custom:") && !qTarget.toLowerCase(Locale.ROOT).startsWith("custom:")) {
            if (t.substring(7).equalsIgnoreCase(qTarget)) return true;
        }

        // Hostile mob category alias
        if (qTarget.equalsIgnoreCase("HOSTILE")) {
            return isHostileMob(t);
        }

        // Wood / Log alias
        if (qTarget.equalsIgnoreCase("WOOD") || qTarget.equalsIgnoreCase("LOG")) {
            return t.toUpperCase(Locale.ROOT).endsWith("_LOG") || t.toUpperCase(Locale.ROOT).endsWith("_WOOD")
                    || t.toUpperCase(Locale.ROOT).endsWith("_STEM") || t.toUpperCase(Locale.ROOT).endsWith("_HYPHAE");
        }

        // Planks alias
        if (qTarget.equalsIgnoreCase("PLANKS")) {
            return t.toUpperCase(Locale.ROOT).endsWith("_PLANKS");
        }

        // Sapling alias
        if (qTarget.equalsIgnoreCase("SAPLING")) {
            return t.toUpperCase(Locale.ROOT).endsWith("_SAPLING") || t.toUpperCase(Locale.ROOT).contains("PROPAGULE");
        }

        // Seeds alias
        if (qTarget.equalsIgnoreCase("SEEDS")) {
            return t.equalsIgnoreCase("WHEAT_SEEDS") || t.equalsIgnoreCase("PUMPKIN_SEEDS")
                    || t.equalsIgnoreCase("MELON_SEEDS") || t.equalsIgnoreCase("BEETROOT_SEEDS")
                    || t.equalsIgnoreCase("TORCHFLOWER_SEEDS") || t.equalsIgnoreCase("PITCHER_POD");
        }

        // Crops alias
        if (qTarget.equalsIgnoreCase("CROP") || qTarget.equalsIgnoreCase("CROPS")) {
            return t.equalsIgnoreCase("WHEAT") || t.equalsIgnoreCase("CARROTS") || t.equalsIgnoreCase("CARROT")
                    || t.equalsIgnoreCase("POTATOES") || t.equalsIgnoreCase("POTATO")
                    || t.equalsIgnoreCase("BEETROOTS") || t.equalsIgnoreCase("BEETROOT")
                    || t.equalsIgnoreCase("NETHER_WART") || t.equalsIgnoreCase("SWEET_BERRY_BUSH")
                    || t.equalsIgnoreCase("SWEET_BERRIES") || t.equalsIgnoreCase("SUGAR_CANE")
                    || t.equalsIgnoreCase("BAMBOO") || t.equalsIgnoreCase("PUMPKIN") || t.equalsIgnoreCase("MELON");
        }

        // Specific crops alias
        if (qTarget.equalsIgnoreCase("WHEAT")) {
            return t.equalsIgnoreCase("WHEAT") || t.equalsIgnoreCase("WHEAT_SEEDS");
        }
        if (qTarget.equalsIgnoreCase("CARROT") || qTarget.equalsIgnoreCase("CARROTS")) {
            return t.equalsIgnoreCase("CARROT") || t.equalsIgnoreCase("CARROTS");
        }
        if (qTarget.equalsIgnoreCase("POTATO") || qTarget.equalsIgnoreCase("POTATOES")) {
            return t.equalsIgnoreCase("POTATO") || t.equalsIgnoreCase("POTATOES") || t.equalsIgnoreCase("POISONOUS_POTATO");
        }
        if (qTarget.equalsIgnoreCase("BEETROOT") || qTarget.equalsIgnoreCase("BEETROOTS")) {
            return t.equalsIgnoreCase("BEETROOT") || t.equalsIgnoreCase("BEETROOTS") || t.equalsIgnoreCase("BEETROOT_SEEDS");
        }

        // Fish alias
        if (qTarget.equalsIgnoreCase("FISH")) {
            return t.equalsIgnoreCase("COD") || t.equalsIgnoreCase("SALMON") || t.equalsIgnoreCase("TROPICAL_FISH") || t.equalsIgnoreCase("PUFFERFISH");
        }

        // Meat/Food alias
        if (qTarget.equalsIgnoreCase("FOOD") || qTarget.equalsIgnoreCase("MEAT")) {
            return t.contains("BEEF") || t.contains("PORKCHOP") || t.contains("CHICKEN") || t.contains("MUTTON") || t.contains("RABBIT") || t.contains("FISH") || t.contains("BREAD");
        }

        // Coal / Charcoal alias
        if (qTarget.equalsIgnoreCase("COAL") || qTarget.equalsIgnoreCase("CHARCOAL")) {
            return t.equalsIgnoreCase("COAL") || t.equalsIgnoreCase("CHARCOAL") || t.equalsIgnoreCase("COAL_ORE") || t.equalsIgnoreCase("DEEPSLATE_COAL_ORE");
        }

        // Iron alias
        if (qTarget.equalsIgnoreCase("IRON_ORE") || qTarget.equalsIgnoreCase("RAW_IRON")) {
            return t.equalsIgnoreCase("IRON_ORE") || t.equalsIgnoreCase("DEEPSLATE_IRON_ORE") || t.equalsIgnoreCase("RAW_IRON")
                    || t.equalsIgnoreCase("IRON_INGOT") || t.equalsIgnoreCase("RAW_IRON_BLOCK") || t.equalsIgnoreCase("IRON_BLOCK");
        }

        // Copper alias
        if (qTarget.equalsIgnoreCase("COPPER_ORE") || qTarget.equalsIgnoreCase("RAW_COPPER")) {
            return t.equalsIgnoreCase("COPPER_ORE") || t.equalsIgnoreCase("DEEPSLATE_COPPER_ORE") || t.equalsIgnoreCase("RAW_COPPER")
                    || t.equalsIgnoreCase("COPPER_INGOT") || t.equalsIgnoreCase("RAW_COPPER_BLOCK") || t.equalsIgnoreCase("COPPER_BLOCK");
        }

        // Gold alias
        if (qTarget.equalsIgnoreCase("GOLD_ORE") || qTarget.equalsIgnoreCase("RAW_GOLD")) {
            return t.equalsIgnoreCase("GOLD_ORE") || t.equalsIgnoreCase("DEEPSLATE_GOLD_ORE") || t.equalsIgnoreCase("RAW_GOLD")
                    || t.equalsIgnoreCase("GOLD_INGOT") || t.equalsIgnoreCase("NETHER_GOLD_ORE") || t.equalsIgnoreCase("RAW_GOLD_BLOCK") || t.equalsIgnoreCase("GOLD_BLOCK");
        }

        // Diamond alias
        if (qTarget.equalsIgnoreCase("DIAMOND") || qTarget.equalsIgnoreCase("DIAMOND_ORE")) {
            return t.equalsIgnoreCase("DIAMOND") || t.equalsIgnoreCase("DIAMOND_ORE") || t.equalsIgnoreCase("DEEPSLATE_DIAMOND_ORE") || t.equalsIgnoreCase("DIAMOND_BLOCK");
        }

        // Cobblestone / Stone alias
        if (qTarget.equalsIgnoreCase("COBBLESTONE") || qTarget.equalsIgnoreCase("STONE")) {
            return t.equalsIgnoreCase("COBBLESTONE") || t.equalsIgnoreCase("STONE") || t.equalsIgnoreCase("COBBLED_DEEPSLATE") || t.equalsIgnoreCase("DEEPSLATE")
                    || t.equalsIgnoreCase("GRANITE") || t.equalsIgnoreCase("DIORITE") || t.equalsIgnoreCase("ANDESITE")
                    || t.equalsIgnoreCase("TUFF") || t.equalsIgnoreCase("CALCITE");
        }

        // Ores general alias
        if (qTarget.equalsIgnoreCase("ORE") || qTarget.equalsIgnoreCase("ORES")) {
            return t.toUpperCase(Locale.ROOT).endsWith("_ORE") || t.equalsIgnoreCase("ANCIENT_DEBRIS")
                    || t.equalsIgnoreCase("RAW_IRON") || t.equalsIgnoreCase("RAW_COPPER") || t.equalsIgnoreCase("RAW_GOLD") || t.equalsIgnoreCase("AMETHYST_CLUSTER");
        }

        // Ingot general alias
        if (qTarget.equalsIgnoreCase("INGOT") || qTarget.equalsIgnoreCase("INGOTS")) {
            return t.toUpperCase(Locale.ROOT).endsWith("_INGOT");
        }

        // Full armor set check aliases
        if (qTarget.toUpperCase(Locale.ROOT).startsWith("ARMOR_SET_")) {
            String armorType = qTarget.substring("ARMOR_SET_".length()); // e.g. LEATHER, IRON, DIAMOND, NETHERITE
            return t.equalsIgnoreCase(armorType);
        }

        // Biome discover aliases
        if (type == ObjectiveType.DISCOVER_BIOME) {
            String cleanQ = qTarget.replace(" ", "_").toUpperCase(Locale.ROOT);
            String cleanT = t.replace(" ", "_").toUpperCase(Locale.ROOT);
            if (cleanQ.equalsIgnoreCase(cleanT) || cleanQ.contains(cleanT) || cleanT.contains(cleanQ)) return true;
            if (cleanQ.contains("BIRCH") && cleanT.contains("BIRCH")) return true;
            if (cleanQ.contains("MEADOW") && cleanT.contains("MEADOW")) return true;
            if (cleanQ.contains("TAIGA") && cleanT.contains("TAIGA")) return true;
            if (cleanQ.contains("SWAMP") && cleanT.contains("SWAMP")) return true;
            if (cleanQ.contains("SAVANNA") && cleanT.contains("SAVANNA")) return true;
            if (cleanQ.contains("DARK_FOREST") && cleanT.contains("DARK_FOREST")) return true;
            if (cleanQ.contains("PEAKS") && cleanT.contains("PEAKS")) return true;
        }

        // Boss aliases (Pirate, Zombie Raider)
        if (isPirateBossMatch(qTarget, t)) return true;
        if (isZombieRaiderBossMatch(qTarget, t)) return true;

        return false;
    }

    private String normalizeBossName(String str) {
        if (str == null) return "";
        String s = org.bukkit.ChatColor.stripColor(str).toLowerCase(Locale.ROOT).trim();
        s = s.replace("ą", "a")
             .replace("ć", "c")
             .replace("ę", "e")
             .replace("ł", "l")
             .replace("ń", "n")
             .replace("ó", "o")
             .replace("ś", "s")
             .replace("ź", "z")
             .replace("ż", "z");
        s = s.replaceAll("[^a-z0-9]", "_").replaceAll("_+", "_");
        if (s.startsWith("_")) s = s.substring(1);
        if (s.endsWith("_")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private boolean isPirateBoss(String norm) {
        return norm.contains("pirat") || norm.contains("sniper") || norm.contains("snajper");
    }

    private boolean isZombieRaiderBoss(String norm) {
        return (norm.contains("zombie") && norm.contains("raider"))
                || norm.contains("najezdzca")
                || norm.contains("zza_grobu")
                || (norm.contains("martwy") && norm.contains("najezdzca"));
    }

    private boolean isPirateBossMatch(String qTarget, String t) {
        String normQ = normalizeBossName(qTarget);
        String normT = normalizeBossName(t);
        return isPirateBoss(normQ) && isPirateBoss(normT);
    }

    private boolean isZombieRaiderBossMatch(String qTarget, String t) {
        String normQ = normalizeBossName(qTarget);
        String normT = normalizeBossName(t);
        return isZombieRaiderBoss(normQ) && isZombieRaiderBoss(normT);
    }

    private boolean isHostileMob(String mobType) {
        if (mobType == null) return false;
        String m = mobType.toUpperCase(Locale.ROOT);
        return m.contains("ZOMBIE") || m.contains("SKELETON") || m.contains("SPIDER") || m.contains("CREEPER")
                || m.contains("SLIME") || m.contains("ENDERMAN") || m.contains("WITCH") || m.contains("PILLAGER")
                || m.contains("VINDICATOR") || m.contains("EVOKER") || m.contains("RAVAGER") || m.contains("GUARDIAN")
                || m.contains("BLAZE") || m.contains("GHAST") || m.contains("MAGMA_CUBE") || m.contains("PIGLIN_BRUTE")
                || m.contains("WITHER") || m.contains("WARDEN") || m.contains("SHULKER") || m.contains("DROWNED")
                || m.contains("HUSK") || m.contains("STRAY") || m.contains("PHANTOM");
    }

    private void sendActionBarProgress(Player player, Quest quest, int progress) {
        String msg = "§6[Zadanie] " + quest.getTitle() + " §7- §e" + progress + "§7/§e" + quest.getRequiredAmount();
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    public void completeQuest(Player player, PlayerProgressionData data, Quest quest) {
        if (player == null || data == null || quest == null) return;
        if (data.isQuestCompleted(quest.getId())) return;

        data.markQuestCompleted(quest.getId());
        data.setProgress(quest.getId(), quest.getRequiredAmount());

        // Fire event
        QuestCompleteEvent event = new QuestCompleteEvent(player, quest);
        Bukkit.getPluginManager().callEvent(event);

        // Give reward
        rewardService.giveReward(player, quest.getReward());

        // Notify
        int completedReq = stageService.getCompletedRequiredQuestsCount(data, quest.getStage());
        int totalReq = stageService.getRequiredQuestsCount(quest.getStage());

        player.sendMessage("§a§l✔ [UKOŃCZONO ZADANIE] §e" + quest.getTitle() + " §8(§b" + completedReq + "§7/§b" + totalReq + " wymaganych§8)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);

        // Check if newly unlocked quests can be retroactively completed (e.g. already discovered biomes)
        checkRetroactiveObjectives(player, data);

        // Check if all required quests for the stage are completed
        if (stageService.canAdvanceStage(data)) {
            stageService.advanceStage(player, data);
        }
    }

    public void checkRetroactiveObjectives(Player player, PlayerProgressionData data) {
        if (player == null || data == null) return;
        List<Quest> stageQuests = questRegistry.getQuestsForStage(data.getCurrentStage());
        for (Quest q : stageQuests) {
            if (data.isQuestCompleted(q.getId())) continue;
            if (!isQuestUnlocked(data, q)) continue;

            if (q.getObjectiveType() == ObjectiveType.DISCOVER_BIOME) {
                for (String discoveredBiome : data.getDiscoveredBiomes()) {
                    if (matchesTarget(q, ObjectiveType.DISCOVER_BIOME, discoveredBiome)) {
                        completeQuest(player, data, q);
                        break;
                    }
                }
            }
        }
    }

    public boolean isQuestUnlocked(PlayerProgressionData data, Quest quest) {
        if (data == null || quest == null) return false;
        if (quest.getStage() != data.getCurrentStage()) {
            return data.getCurrentStage().getOrder() >= quest.getStage().getOrder();
        }
        for (String prereqId : quest.getPrerequisites()) {
            if (!data.isQuestCompleted(prereqId)) {
                return false;
            }
        }
        return true;
    }

    public QuestRegistry getQuestRegistry() {
        return questRegistry;
    }

    public StageService getStageService() {
        return stageService;
    }

    public RewardService getRewardService() {
        return rewardService;
    }

    public IProgressionStorage getStorage() {
        return storage;
    }
}
