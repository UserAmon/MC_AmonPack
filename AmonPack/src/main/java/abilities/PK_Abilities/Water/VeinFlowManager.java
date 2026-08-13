package Abilities.PK_Abilities.Water;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VeinFlowManager {

    private static final Map<UUID, VeinFlow> activeStances = new HashMap<>();

    public static void registerStance(Player player, VeinFlow stance) {
        if (player != null && stance != null) {
            activeStances.put(player.getUniqueId(), stance);
        }
    }

    public static void unregisterStance(Player player) {
        if (player != null) {
            activeStances.remove(player.getUniqueId());
        }
    }

    public static boolean isActive(Player player) {
        if (player == null) return false;
        VeinFlow stance = activeStances.get(player.getUniqueId());
        return stance != null && stance.isAlive();
    }

    public static VeinFlow getStance(Player player) {
        if (player == null) return null;
        return activeStances.get(player.getUniqueId());
    }
}
