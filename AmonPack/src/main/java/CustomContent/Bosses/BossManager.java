package CustomContent.Bosses;

import CustomContent.Pack.PackManager;
import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.io.File;
import java.util.*;

public class BossManager {

    private final Map<String, CustomBoss> bosses = new LinkedHashMap<>();
    private final Map<UUID, ActiveBossInstance> activeBosses = new HashMap<>();
    private final PackManager packManager;

    public BossManager(PackManager packManager) {
        this.packManager = packManager;
    }

    public void load() {
        bosses.clear();
        File file = new File(AmonPackPlugin.plugin.getDataFolder(), "custom_bosses.yml");
        if (!file.exists()) {
            AmonPackPlugin.plugin.saveResource("custom_bosses.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getConfigurationSection("bosses") != null) {
            for (String key : cfg.getConfigurationSection("bosses").getKeys(false)) {
                String path = "bosses." + key;
                String name = ChatColor.translateAlternateColorCodes('&', cfg.getString(path + ".display_name", key));
                CustomBoss cb = new CustomBoss(key, name);

                String entStr = cfg.getString(path + ".base_entity", "HUSK");
                try {
                    cb.setBaseEntity(EntityType.valueOf(entStr.toUpperCase(Locale.ROOT)));
                } catch (Exception e) {
                    cb.setBaseEntity(EntityType.HUSK);
                }

                // Model
                cb.setModelPath(cfg.getString(path + ".model.model_path", "boss/" + key));
                cb.setCustomModelData(cfg.getInt(path + ".model.custom_model_data", 20001));
                cb.setScale(cfg.getDouble(path + ".model.scale", 2.0));

                // Stats
                cb.setMaxHealth(cfg.getDouble(path + ".stats.max_health", 500.0));
                cb.setAttackDamage(cfg.getDouble(path + ".stats.attack_damage", 14.0));
                cb.setMovementSpeed(cfg.getDouble(path + ".stats.movement_speed", 0.28));
                cb.setKnockbackResistance(cfg.getDouble(path + ".stats.knockback_resistance", 1.0));
                cb.setFollowRange(cfg.getDouble(path + ".stats.follow_range", 35.0));

                // BossBar
                cb.setBossBarEnabled(cfg.getBoolean(path + ".boss_bar.enabled", true));
                cb.setBossBarTitle(cfg.getString(path + ".boss_bar.title", "&c&lBoss &7- &e{health}&7/&e{max_health} HP"));
                try {
                    cb.setBossBarColor(BarColor.valueOf(cfg.getString(path + ".boss_bar.color", "RED").toUpperCase(Locale.ROOT)));
                    cb.setBossBarStyle(BarStyle.valueOf(cfg.getString(path + ".boss_bar.style", "SEGMENTED_10").toUpperCase(Locale.ROOT)));
                } catch (Exception ignored) {}
                cb.setBossBarRange(cfg.getDouble(path + ".boss_bar.range", 35.0));

                // Skills
                if (cfg.getConfigurationSection(path + ".skills") != null) {
                    for (String sKey : cfg.getConfigurationSection(path + ".skills").getKeys(false)) {
                        String sPath = path + ".skills." + sKey;
                        CustomBoss.BossSkill skill = new CustomBoss.BossSkill();
                        skill.trigger = cfg.getString(sPath + ".trigger", "TIMER");
                        skill.intervalSeconds = cfg.getInt(sPath + ".interval_seconds", 6);
                        skill.healthPercent = cfg.getDouble(sPath + ".health_percent", 50.0);
                        skill.ability = cfg.getString(sPath + ".ability", "EarthStrike");
                        skill.range = cfg.getDouble(sPath + ".range", 15.0);
                        skill.announcement = cfg.getString(sPath + ".announcement", null);
                        cb.getSkills().add(skill);
                    }
                }

                // Drops
                if (cfg.getConfigurationSection(path + ".drops") != null) {
                    for (String dKey : cfg.getConfigurationSection(path + ".drops").getKeys(false)) {
                        String dPath = path + ".drops." + dKey;
                        CustomBoss.BossDrop drop = new CustomBoss.BossDrop();
                        drop.customItemId = cfg.getString(dPath + ".custom_item", null);
                        drop.chance = cfg.getDouble(dPath + ".chance", 1.0);
                        drop.min = cfg.getInt(dPath + ".min", 1);
                        drop.max = cfg.getInt(dPath + ".max", 1);
                        cb.getDrops().add(drop);
                    }
                }
                cb.setExpDrop(cfg.getInt(path + ".exp", 250));

                bosses.put(key.toLowerCase(Locale.ROOT), cb);

                // Rejestracja w packu
                String modelKey = cb.getModelPath();
                if (!modelKey.startsWith("amonpack:")) modelKey = "amonpack:" + modelKey;
                packManager.registerModelOverride("carved_pumpkin", cb.getCustomModelData(), modelKey);
            }
        }
    }

    public ActiveBossInstance spawnBoss(String bossId, Location location) {
        CustomBoss template = bosses.get(bossId.toLowerCase(Locale.ROOT));
        if (template == null) return null;

        Mob mob = (Mob) location.getWorld().spawnEntity(location, template.getBaseEntity());
        ActiveBossInstance instance = new ActiveBossInstance(template, mob);
        activeBosses.put(mob.getUniqueId(), instance);

        Bukkit.broadcastMessage("§c§l[AmonPack] §6Boss §e" + template.getDisplayName() + " §6został przyzwany na koordynatach: §f" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        return instance;
    }

    public void killAllBosses() {
        for (ActiveBossInstance instance : activeBosses.values()) {
            if (instance.getEntity() != null && instance.getEntity().isValid()) {
                instance.getEntity().remove();
            }
            instance.remove();
        }
        activeBosses.clear();
    }

    public void unload() {
        killAllBosses();
    }

    public ActiveBossInstance getActiveBoss(LivingEntity entity) {
        if (entity == null) return null;
        return activeBosses.get(entity.getUniqueId());
    }

    public void removeActiveBoss(UUID uuid) {
        ActiveBossInstance removed = activeBosses.remove(uuid);
        if (removed != null) {
            removed.remove();
        }
    }

    public Map<String, CustomBoss> getAllBosses() {
        return Collections.unmodifiableMap(bosses);
    }
}
