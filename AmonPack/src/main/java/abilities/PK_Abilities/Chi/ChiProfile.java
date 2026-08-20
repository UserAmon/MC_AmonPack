package Abilities.PK_Abilities.Chi;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChiProfile {

    public static class TempModifier {
        private final double amount;
        private final long expiryTimestamp;

        public TempModifier(double amount, long durationMillis) {
            this.amount = amount;
            this.expiryTimestamp = System.currentTimeMillis() + durationMillis;
        }

        public double getAmount() {
            return amount;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiryTimestamp;
        }
    }

    private final UUID playerUuid;
    private double currentChi;
    private double baseMaxChi;
    private double permanentBonusMaxChi;
    private final Map<String, TempModifier> tempMaxChiModifiers = new ConcurrentHashMap<>();

    private double baseRegenRate;
    private double permanentBonusRegenRate;
    private final Map<String, TempModifier> tempRegenModifiers = new ConcurrentHashMap<>();

    private BossBar bossBar;
    private long lastChiConsumeTime = 0;

    public ChiProfile(UUID playerUuid, double initialMaxChi, double initialRegenRate) {
        this.playerUuid = playerUuid;
        this.baseMaxChi = Math.max(1.0, initialMaxChi);
        this.permanentBonusMaxChi = 0.0;
        this.baseRegenRate = Math.max(0.0, initialRegenRate);
        this.permanentBonusRegenRate = 0.0;
        this.currentChi = this.getMaxChi();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public double getMaxChi() {
        double max = baseMaxChi + permanentBonusMaxChi;
        for (TempModifier mod : tempMaxChiModifiers.values()) {
            if (!mod.isExpired()) {
                max += mod.getAmount();
            }
        }
        return Math.max(1.0, max);
    }

    public double getChi() {
        double max = getMaxChi();
        if (currentChi > max) {
            currentChi = max;
        }
        return Math.max(0.0, currentChi);
    }

    public double getRegenRate() {
        double rate = baseRegenRate + permanentBonusRegenRate;
        for (TempModifier mod : tempRegenModifiers.values()) {
            if (!mod.isExpired()) {
                rate += mod.getAmount();
            }
        }
        return Math.max(0.0, rate);
    }

    public void setChi(double amount) {
        this.currentChi = Math.max(0.0, Math.min(amount, getMaxChi()));
        updateBossBar();
    }

    public void addChi(double amount) {
        setChi(this.currentChi + amount);
    }

    public boolean hasChi(double amount) {
        return getChi() >= amount - 0.001;
    }

    public boolean consumeChi(double amount) {
        if (amount <= 0) return true;
        if (!hasChi(amount)) {
            return false;
        }
        this.currentChi = Math.max(0.0, this.currentChi - amount);
        this.lastChiConsumeTime = System.currentTimeMillis();
        updateBossBar();
        return true;
    }

    public void setBaseMaxChi(double amount) {
        this.baseMaxChi = Math.max(1.0, amount);
        if (this.currentChi > getMaxChi()) {
            this.currentChi = getMaxChi();
        }
        updateBossBar();
    }

    public void increaseMaxChi(double amount) {
        this.permanentBonusMaxChi += amount;
        this.currentChi = Math.min(this.currentChi + amount, getMaxChi());
        updateBossBar();
    }

    public void addTempMaxChi(String key, double amount, long durationMillis) {
        tempMaxChiModifiers.put(key, new TempModifier(amount, durationMillis));
        this.currentChi = Math.min(this.currentChi + amount, getMaxChi());
        updateBossBar();
    }

    public void setBaseRegenRate(double rate) {
        this.baseRegenRate = Math.max(0.0, rate);
        updateBossBar();
    }

    public void increaseRegenRate(double amount) {
        this.permanentBonusRegenRate += amount;
        updateBossBar();
    }

    public void addTempRegenRate(String key, double amount, long durationMillis) {
        tempRegenModifiers.put(key, new TempModifier(amount, durationMillis));
        updateBossBar();
    }

    public void tick(double deltaSeconds) {
        // Clean expired temp modifiers
        tempMaxChiModifiers.entrySet().removeIf(e -> e.getValue().isExpired());
        tempRegenModifiers.entrySet().removeIf(e -> e.getValue().isExpired());

        double max = getMaxChi();
        if (currentChi < max) {
            double regenerated = getRegenRate() * deltaSeconds;
            currentChi = Math.min(max, currentChi + regenerated);
        } else if (currentChi > max) {
            currentChi = max;
        }

        updateBossBar();
    }

    public void updateBossBar() {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            removeBossBar();
            return;
        }

        double max = getMaxChi();
        double current = getChi();

        // If Chi is full and no recent consumption, hide bossbar
        if (current >= max - 0.01) {
            if (bossBar != null) {
                bossBar.removePlayer(player);
                bossBar.setVisible(false);
            }
            return;
        }

        // When Chi is not full, show bossbar
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("§6⚡ CHI: §e[ 100 / 100 ]", BarColor.YELLOW, BarStyle.SEGMENTED_10);
            bossBar.addPlayer(player);
        } else if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }

        double progress = Math.max(0.0, Math.min(1.0, current / max));
        bossBar.setProgress(progress);
        bossBar.setTitle(String.format("§6⚡ §lPOZIOM CHI: §e%.0f §7/ §e%.0f §a(+%.1f/s)", current, max, getRegenRate()));
        bossBar.setVisible(true);
    }

    public void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
    }
}
