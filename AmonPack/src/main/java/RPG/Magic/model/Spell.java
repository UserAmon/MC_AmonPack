package RPG.Magic.model;

import RPG.Magic.manager.ManaManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public abstract class Spell {

    protected final String id;
    protected final String name;
    protected final String description;
    protected final SpellElement element;
    protected final int manaCost;
    protected final double cooldownSeconds;

    public Spell(String id, String name, String description, SpellElement element, int manaCost, double cooldownSeconds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.element = element;
        this.manaCost = manaCost;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public SpellElement getElement() { return element; }
    public int getManaCost() { return manaCost; }
    public double getCooldownSeconds() { return cooldownSeconds; }

    public boolean canCast(Player player, ManaManager manaManager) {
        if (player == null || manaManager == null) return false;

        // Check cooldown
        if (manaManager.isOnCooldown(player.getUniqueId(), id)) {
            double remaining = manaManager.getRemainingCooldown(player.getUniqueId(), id);
            player.sendMessage(String.format("§c[Magia] Czar §e%s §cjest w trakcie odnawiania! (%.1fs)", name, remaining));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return false;
        }

        // Check mana
        if (!manaManager.hasMana(player.getUniqueId(), manaCost)) {
            double current = manaManager.getMana(player.getUniqueId());
            player.sendMessage(String.format("§c[Magia] Brak wystarczającej ilości many! (Wymagane: §b%d MP§c, Posiadasz: §b%.0f MP§c)", manaCost, current));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return false;
        }

        return true;
    }

    public abstract boolean cast(Player player, ManaManager manaManager);
}
