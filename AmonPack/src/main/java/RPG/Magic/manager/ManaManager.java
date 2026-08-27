package RPG.Magic.manager;

import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ManaManager {

    private final Map<UUID, Double> currentMana = new ConcurrentHashMap<>();
    private final Map<UUID, Double> maxMana = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHudTimes = new ConcurrentHashMap<>();

    private final Map<UUID, org.bukkit.boss.BossBar> manaBossBars = new ConcurrentHashMap<>();

    private double regenRatePerSecond = 2.0;
    private double defaultMaxMana = 100.0;
    private BukkitTask regenTask;

    public ManaManager() {
    }

    public void start() {
        if (regenTask != null) regenTask.cancel();

        // Regeneracja many co 10 ticków (0.5 sekundy) -> +1.0 MP + aktualizacja BossBara
        regenTask = Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                double max = getMaxMana(uuid);
                double current = getMana(uuid);

                if (current < max) {
                    double next = Math.min(max, current + (regenRatePerSecond * 0.5));
                    currentMana.put(uuid, next);
                    updateManaBossBar(player, next, max);
                } else {
                    removeManaBossBar(uuid);
                }
            }
        }, 10L, 10L);
    }

    public void stop() {
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
        }
        for (org.bukkit.boss.BossBar bar : manaBossBars.values()) {
            bar.removeAll();
        }
        manaBossBars.clear();
        currentMana.clear();
        maxMana.clear();
        cooldowns.clear();
    }

    public void updateManaBossBar(Player player, double current, double max) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        if (current >= max) {
            removeManaBossBar(uuid);
            return;
        }

        org.bukkit.boss.BossBar bar = manaBossBars.computeIfAbsent(uuid, k -> {
            org.bukkit.boss.BossBar newBar = Bukkit.createBossBar(
                    "§b✦ MANA", 
                    org.bukkit.boss.BarColor.BLUE, 
                    org.bukkit.boss.BarStyle.SOLID
            );
            newBar.addPlayer(player);
            newBar.setVisible(true);
            return newBar;
        });

        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }

        double progress = Math.max(0.0, Math.min(1.0, current / max));
        bar.setProgress(progress);
        bar.setTitle(String.format("§b✦ MANA: §f%.0f§7/§b%.0f MP §8(§e%.0f%%§8)", current, max, progress * 100));
    }

    public void removeManaBossBar(UUID uuid) {
        org.bukkit.boss.BossBar bar = manaBossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    public double getMana(UUID uuid) {
        return currentMana.getOrDefault(uuid, defaultMaxMana);
    }

    public double getMana(Player player) {
        return player != null ? getMana(player.getUniqueId()) : defaultMaxMana;
    }

    public double getMaxMana(UUID uuid) {
        return maxMana.getOrDefault(uuid, defaultMaxMana);
    }

    public double getMaxMana(Player player) {
        return player != null ? getMaxMana(player.getUniqueId()) : defaultMaxMana;
    }

    public void setMana(UUID uuid, double mana) {
        currentMana.put(uuid, Math.max(0, Math.min(getMaxMana(uuid), mana)));
    }

    public boolean hasMana(UUID uuid, double amount) {
        return getMana(uuid) >= amount;
    }

    public boolean hasMana(Player player, double amount) {
        return player != null && hasMana(player.getUniqueId(), amount);
    }

    public boolean consumeMana(UUID uuid, double amount) {
        double current = getMana(uuid);
        if (current < amount) return false;
        double next = current - amount;
        currentMana.put(uuid, next);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            updateManaBossBar(p, next, getMaxMana(uuid));
        }
        return true;
    }

    public boolean consumeMana(Player player, double amount) {
        return player != null && consumeMana(player.getUniqueId(), amount);
    }

    public void restoreMana(UUID uuid, double amount) {
        double max = getMaxMana(uuid);
        double current = getMana(uuid);
        double next = Math.min(max, current + amount);
        currentMana.put(uuid, next);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            updateManaBossBar(p, next, max);
        }
    }

    public boolean isOnCooldown(UUID uuid, String spellId) {
        String key = uuid.toString() + ":" + spellId.toLowerCase();
        Long expire = cooldowns.get(key);
        return expire != null && expire > System.currentTimeMillis();
    }

    public double getRemainingCooldown(UUID uuid, String spellId) {
        String key = uuid.toString() + ":" + spellId.toLowerCase();
        Long expire = cooldowns.get(key);
        if (expire == null) return 0.0;
        long diff = expire - System.currentTimeMillis();
        return diff > 0 ? (diff / 1000.0) : 0.0;
    }

    public void setCooldown(UUID uuid, String spellId, double seconds) {
        String key = uuid.toString() + ":" + spellId.toLowerCase();
        cooldowns.put(key, System.currentTimeMillis() + (long) (seconds * 1000L));
    }

    public void sendManaBarHud(Player player, String activeSpellName) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        double current = getMana(uuid);
        double max = getMaxMana(uuid);
        double ratio = Math.max(0.0, Math.min(1.0, current / max));

        int totalSegments = 10;
        int filledSegments = (int) Math.round(ratio * totalSegments);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < totalSegments; i++) {
            if (i < filledSegments) {
                bar.append("§b■");
            } else {
                bar.append("§8□");
            }
        }

        String spellInfo = activeSpellName != null && !activeSpellName.isEmpty() ? " §8| §6" + activeSpellName + " §7(LPM)" : "";
        String msg = String.format("§9✦ MANA: §f[ %s §f] §b%.0f§7/§b%.0f MP%s", bar.toString(), current, max, spellInfo);

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
        lastHudTimes.put(uuid, System.currentTimeMillis());
    }

    public double getRegenRatePerSecond() { return regenRatePerSecond; }
    public void setRegenRatePerSecond(double regenRatePerSecond) { this.regenRatePerSecond = regenRatePerSecond; }
}
