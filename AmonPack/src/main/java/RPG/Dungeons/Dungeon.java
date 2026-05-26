package RPG.Dungeons;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dungeon {
    private final String id;
    private final String name;
    private final String schematicFile;
    private final Vector pasteLocation;
    private final Vector spawnLocation;
    
    // Exit Location
    private final String exitWorld;
    private final Vector exitLocation;

    private final String initialEncounterId;
    private final Map<String, Encounter> encounters;
    private final DungeonRewards rewards;
    private final List<String> allowedStats;
    private final List<String> allowedBlessings;

    public Dungeon(String id, String name, String schematicFile, Vector pasteLocation, Vector spawnLocation, String exitWorld, Vector exitLocation, String initialEncounterId, Map<String, Encounter> encounters, DungeonRewards rewards, List<String> allowedStats, List<String> allowedBlessings) {
        this.id = id;
        this.name = name;
        this.schematicFile = schematicFile;
        this.pasteLocation = pasteLocation;
        this.spawnLocation = spawnLocation;
        this.exitWorld = exitWorld;
        this.exitLocation = exitLocation;
        this.initialEncounterId = initialEncounterId;
        this.encounters = encounters != null ? encounters : new HashMap<>();
        this.rewards = rewards != null ? rewards : new DungeonRewards();
        this.allowedStats = allowedStats != null ? allowedStats : new ArrayList<>();
        this.allowedBlessings = allowedBlessings != null ? allowedBlessings : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSchematicFile() {
        return schematicFile;
    }

    public Vector getPasteLocation() {
        return pasteLocation;
    }

    public Vector getSpawnLocation() {
        return spawnLocation;
    }

    public String getExitWorld() {
        return exitWorld;
    }

    public Vector getExitLocation() {
        return exitLocation;
    }

    public String getInitialEncounterId() {
        return initialEncounterId;
    }

    public Map<String, Encounter> getEncounters() {
        return encounters;
    }

    public DungeonRewards getRewards() {
        return rewards;
    }

    public List<String> getAllowedStats() {
        return allowedStats;
    }

    public List<String> getAllowedBlessings() {
        return allowedBlessings;
    }

    /**
     * Helper holder for Dungeon Completion Rewards.
     */
    public static class DungeonRewards {
        private int dungeonXp = 0;
        private double money = 0;
        private final List<String> commands = new ArrayList<>();
        private final List<ItemStack> items = new ArrayList<>();

        public DungeonRewards() {}

        public DungeonRewards(int dungeonXp, double money, List<String> commands, List<ItemStack> items) {
            this.dungeonXp = dungeonXp;
            this.money = money;
            if (commands != null) this.commands.addAll(commands);
            if (items != null) this.items.addAll(items);
        }

        public int getDungeonXp() {
            return dungeonXp;
        }

        public double getMoney() {
            return money;
        }

        public List<String> getCommands() {
            return commands;
        }

        public List<ItemStack> getItems() {
            return items;
        }
    }
}
