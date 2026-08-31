package CustomContent.Guns;

import Plugin.AmonPackPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GunConfigManager {

    private static GunConfigManager instance;
    private File file;
    private FileConfiguration config;

    public GunConfigManager() {
        instance = this;
        load();
    }

    public static GunConfigManager getInstance() {
        if (instance == null) {
            instance = new GunConfigManager();
        }
        return instance;
    }

    public void load() {
        file = new File(AmonPackPlugin.plugin.getDataFolder(), "gun_config.yml");
        if (!file.exists()) {
            AmonPackPlugin.plugin.saveResource("pack/gun_config.yml", false);
            File savedFile = new File(AmonPackPlugin.plugin.getDataFolder(), "pack/gun_config.yml");
            if (savedFile.exists() && !file.exists()) {
                savedFile.renameTo(file);
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        // Fallback do zasobów wewnętrznych
        InputStream defStream = AmonPackPlugin.plugin.getResource("pack/gun_config.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }
    }

    public double getMovementSpeedPenalty() {
        return config.getDouble("global.movement_speed_penalty", -0.10);
    }

    public double getBaseDamage(GunType type) {
        return config.getDouble("guns." + type.getId() + ".base_damage", type.getBaseDamage());
    }

    public double getHeadshotMultiplier(GunType type) {
        return config.getDouble("guns." + type.getId() + ".headshot_multiplier", type.getHeadshotMultiplier());
    }

    public int getReloadTicks(GunType type) {
        return config.getInt("guns." + type.getId() + ".reload_ticks", type.getReloadTicks());
    }

    public double getEffectiveRange(GunType type) {
        return config.getDouble("guns." + type.getId() + ".effective_range", type.getMaxRange());
    }

    public double getBaseSpread(GunType type) {
        return config.getDouble("guns." + type.getId() + ".base_spread", type.getBaseSpread());
    }

    public int getMaxDurability(GunType type) {
        return config.getInt("guns." + type.getId() + ".max_durability", type.getMaxDurability());
    }

    public boolean isPiercing(GunType type) {
        return config.getBoolean("guns." + type.getId() + ".piercing", type == GunType.BLUNDERBUSS);
    }

    public int getMinPellets(GunType type) {
        return config.getInt("guns." + type.getId() + ".min_pellets", type == GunType.BLUNDERBUSS ? 6 : 1);
    }

    public int getMaxPellets(GunType type) {
        return config.getInt("guns." + type.getId() + ".max_pellets", type == GunType.BLUNDERBUSS ? 10 : 1);
    }

    public int getAmmoMinPellets(AmmoType ammo, int defaultMin) {
        if (ammo == null) return defaultMin;
        return config.getInt("ammo." + ammo.getId() + ".min_pellets", defaultMin);
    }

    public int getAmmoMaxPellets(AmmoType ammo, int defaultMax) {
        if (ammo == null) return defaultMax;
        return config.getInt("ammo." + ammo.getId() + ".max_pellets", defaultMax);
    }

    public double getKnockbackStrength(GunType type) {
        return config.getDouble("guns." + type.getId() + ".knockback_strength", type == GunType.BLUNDERBUSS ? 1.8 : 0.8);
    }

    public double getKnockbackVertical(GunType type) {
        return config.getDouble("guns." + type.getId() + ".knockback_vertical", 0.12);
    }

    public double getRiflingSpreadReduction() {
        return config.getDouble("mods.rifling.spread_reduction", 0.40);
    }

    public double getRiflingRangeBonus() {
        return config.getDouble("mods.rifling.range_bonus", 10.0);
    }

    public double getLockReloadReduction() {
        return config.getDouble("mods.reinforced_lock.reload_time_reduction", 0.30);
    }

    public double getBayonetDamage() {
        return config.getDouble("mods.bayonet.melee_damage", 7.0);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
