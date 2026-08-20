package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChiManager {

    private static final Map<UUID, ChiProfile> profiles = new ConcurrentHashMap<>();
    private static BukkitTask tickTask;

    public static void init() {
        if (tickTask != null) {
            tickTask.cancel();
        }

        // Ticks every 4 ticks (0.2s)
        tickTask = Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ChiProfile profile = getProfile(player);
                if (profile != null) {
                    profile.tick(0.2);
                }
            }
        }, 4L, 4L);
    }

    public static void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (ChiProfile profile : profiles.values()) {
            profile.removeBossBar();
        }
        profiles.clear();
    }

    public static ChiProfile getProfile(UUID uuid) {
        if (uuid == null) return null;
        return profiles.computeIfAbsent(uuid, id -> {
            double defaultMax = AmonPackPlugin.getAbilitiesConfig() != null ?
                    AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.System.DefaultMaxChi", 100.0) : 100.0;
            double defaultRegen = AmonPackPlugin.getAbilitiesConfig() != null ?
                    AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.System.DefaultRegenRate", 5.0) : 5.0;
            return new ChiProfile(id, defaultMax, defaultRegen);
        });
    }

    public static ChiProfile getProfile(Player player) {
        if (player == null) return null;
        return getProfile(player.getUniqueId());
    }

    public static double getChi(Player player) {
        ChiProfile profile = getProfile(player);
        return profile != null ? profile.getChi() : 0.0;
    }

    public static double getMaxChi(Player player) {
        ChiProfile profile = getProfile(player);
        return profile != null ? profile.getMaxChi() : 100.0;
    }

    public static double getRegenRate(Player player) {
        ChiProfile profile = getProfile(player);
        return profile != null ? profile.getRegenRate() : 5.0;
    }

    public static boolean hasChi(Player player, double amount) {
        ChiProfile profile = getProfile(player);
        return profile != null && profile.hasChi(amount);
    }

    public static boolean consumeChi(Player player, double amount) {
        if (player == null) return false;
        ChiProfile profile = getProfile(player);
        if (profile == null) return false;

        boolean success = profile.consumeChi(amount);
        if (!success) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(String.format("§c✖ Brak wystarczającej ilości Chi! §7(Wymagane: §e%.0f§7, Posiadasz: §c%.0f§7)", amount, profile.getChi())));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            return false;
        }
        return true;
    }

    public static void addChi(Player player, double amount) {
        ChiProfile profile = getProfile(player);
        if (profile != null) {
            profile.addChi(amount);
        }
    }

    public static void setChi(Player player, double amount) {
        ChiProfile profile = getProfile(player);
        if (profile != null) {
            profile.setChi(amount);
        }
    }

    public static void increaseMaxChi(Player player, double amount) {
        ChiProfile profile = getProfile(player);
        if (profile != null) {
            profile.increaseMaxChi(amount);
        }
    }

    public static void addTempMaxChi(Player player, String key, double amount, long durationTicks) {
        ChiProfile profile = getProfile(player);
        if (profile != null) {
            profile.addTempMaxChi(key, amount, durationTicks * 50L);
        }
    }

    public static void setRegenRate(Player player, double rate) {
        ChiProfile profile = getProfile(player);
        if (profile != null) {
            profile.setBaseRegenRate(rate);
        }
    }

    public static void increaseRegenRate(Player player, double amount) {
        ChiProfile profile = getProfile(player);
        if (profile != null) {
            profile.increaseRegenRate(amount);
        }
    }

    public static void addTempRegen(Player player, String key, double amount, long durationTicks) {
        ChiProfile profile = getProfile(player);
        if (profile != null) {
            profile.addTempRegenRate(key, amount, durationTicks * 50L);
        }
    }

    public static double getAbilityChiCost(String abilityName, double defaultCost) {
        if (AmonPackPlugin.getAbilitiesConfig() == null) return defaultCost;
        return AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi." + abilityName + ".ChiCost", defaultCost);
    }

    private static final Map<UUID, Long> paralyzedUntil = new ConcurrentHashMap<>();

    public static boolean isParalyzed(Player player) {
        if (player == null) return false;
        Long until = paralyzedUntil.get(player.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            paralyzedUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public static void paralyzeEntity(org.bukkit.entity.LivingEntity entity, long durationMs) {
        if (entity == null || durationMs <= 0) return;

        int ticks = (int) (durationMs / 50L);
        if (ticks <= 0) ticks = 1;

        if (entity instanceof Player targetPlayer) {
            long newUntil = System.currentTimeMillis() + durationMs;
            Long currentUntil = paralyzedUntil.get(targetPlayer.getUniqueId());
            if (currentUntil == null || newUntil > currentUntil) {
                paralyzedUntil.put(targetPlayer.getUniqueId(), newUntil);
            }

            com.projectkorra.projectkorra.BendingPlayer bTarget = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(targetPlayer);
            if (bTarget != null) {
                bTarget.blockChi();
            }

            targetPlayer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, ticks, 127, false, false, true));
            targetPlayer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, ticks, 200, false, false, false));
            targetPlayer.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

            targetPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§c✖ §lZOSTAŁEŚ SPARALIŻOWANY! §7(Brak możliwości ruchu i magii)"));
            targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.8f);
        } else {
            entity.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, ticks, 127, false, false, true));
            entity.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            if (entity instanceof org.bukkit.entity.Mob mob) {
                mob.setAI(false);
                Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                    if (mob.isValid()) {
                        mob.setAI(true);
                    }
                }, ticks);
            }
        }
    }
}
