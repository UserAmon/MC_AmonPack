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
    protected String name;
    protected final SpellElement element;
    protected int manaCost;
    protected double cooldownSeconds;
    protected double baseDamage;
    protected String description;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public Spell(String id, String name, SpellElement element, int manaCost, double cooldownSeconds) {
        this(id, name, element, manaCost, cooldownSeconds, 10.0);
    }

    public Spell(String id, String name, SpellElement element, int manaCost, double cooldownSeconds, double baseDamage) {
        this.id = id;
        this.name = name;
        this.element = element;
        this.manaCost = manaCost;
        this.cooldownSeconds = cooldownSeconds;
        this.baseDamage = baseDamage;
        this.description = "Magiczne zaklęcie żywiołu " + element.name();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SpellElement getElement() { return element; }
    public int getManaCost() { return manaCost; }
    public void setManaCost(int manaCost) { this.manaCost = manaCost; }
    public double getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(double cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public double getBaseDamage() { return baseDamage; }
    public void setBaseDamage(double baseDamage) { this.baseDamage = baseDamage; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public void sendCooldownActionBar(Player player) {
        if (player == null) return;
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c✦ Zaklęcie " + getName() + " §codnawia się: §e" + String.format("%.1f", getRemainingCooldown(player)) + "s"));
    }

    public void sendNoManaActionBar(Player player, int requiredMana, ManaManager manaManager) {
        if (player == null) return;
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c✦ Brak many! §7(Wymagane: §b" + requiredMana + " MP§7, Masz: §b" + (int) manaManager.getMana(player) + " MP§7)"));
    }

    public void sendActionBar(Player player, String message) {
        if (player == null) return;
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    public abstract boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager);
}
