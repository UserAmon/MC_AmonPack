package RPG.Magic.model;

import RPG.Magic.manager.ManaManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Spell {

    protected final String id;
    protected final String name;
    protected final SpellElement element;
    protected final int manaCost;
    protected final double cooldownSeconds;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public Spell(String id, String name, SpellElement element, int manaCost, double cooldownSeconds) {
        this.id = id;
        this.name = name;
        this.element = element;
        this.manaCost = manaCost;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public SpellElement getElement() { return element; }
    public int getManaCost() { return manaCost; }
    public double getCooldownSeconds() { return cooldownSeconds; }
    public String getDescription() { return "Magiczne zaklęcie żywiołu " + element.name(); }

    public boolean isOnCooldown(Player player) {
        if (player == null) return false;
        Long expiry = cooldowns.get(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public double getRemainingCooldown(Player player) {
        if (player == null) return 0.0;
        Long expiry = cooldowns.get(player.getUniqueId());
        if (expiry == null) return 0.0;
        long diff = expiry - System.currentTimeMillis();
        return diff > 0 ? (diff / 1000.0) : 0.0;
    }

    public void setCooldown(Player player, long cooldownMs) {
        if (player == null) return;
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMs);
    }

    public boolean cast(Player player, ManaManager manaManager) {
        return cast(player, player.getInventory().getItemInMainHand(), manaManager);
    }

    public abstract boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager);
}
