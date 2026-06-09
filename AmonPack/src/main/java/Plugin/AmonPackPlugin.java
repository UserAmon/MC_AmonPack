package Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import Abilities.Bending.AbilitiesListener;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.ElementType;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import static Abilities.Bending.SoundAbility.StartDeafnessTimer;

public class AmonPackPlugin extends JavaPlugin {
	public static Plugin plugin;
	private static Element SmokeElement;
	private static Element SoundElement;
	private File configFile;
	public FileConfiguration config;

	static File configpath;
	static File AbilitiesConfigFile;
	private static FileConfiguration AbilitiesConfig;

	@Override
	public FileConfiguration getConfig() {
		if (config == null) {
			reloadConfig();
		}
		return config;
	}

	@Override
	public void reloadConfig() {
		if (configFile == null) {
			configFile = new File(getDataFolder(), "abilities_config.yml");
		}
		config = YamlConfiguration.loadConfiguration(configFile);

		InputStream defConfigStream = getResource("abilities_config.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
			config.setDefaults(defConfig);
		}
	}

	@Override
	public void saveConfig() {
		if (config == null || configFile == null) {
			return;
		}
		try {
			getConfig().save(configFile);
		} catch (java.io.IOException ex) {
			getLogger().log(java.util.logging.Level.SEVERE, "Could not save config to " + configFile, ex);
		}
	}

	@Override
	public void saveDefaultConfig() {
		if (configFile == null) {
			configFile = new File(getDataFolder(), "abilities_config.yml");
		}
		if (!configFile.exists()) {            
			saveResource("abilities_config.yml", false);
		}
	}

	@Override
	public void onEnable() {
		plugin = this;
		getLogger().info("AmonPack włączony");

		SmokeElement = new SubElement("Smoke", Element.FIRE, ElementType.BENDING, ProjectKorra.plugin);
		SoundElement = new SubElement("Sound", Element.AIR, ElementType.BENDING, ProjectKorra.plugin);
		CoreAbility.registerPluginAbilities(this, "Abilities.PK_Abilities");
		createconf();

		configpath = getDataFolder();
		AbilitiesConfigFile = new File(getDataFolder(), "abilities_config.yml");
		AbilitiesConfig = YamlConfiguration.loadConfiguration(AbilitiesConfigFile);
		InputStream defAbilitiesStream = getResource("abilities_config.yml");
		if (defAbilitiesStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defAbilitiesStream, StandardCharsets.UTF_8));
			AbilitiesConfig.setDefaults(defConfig);
		}
		Abilities.PK_Abilities.Earth.SandWave.loadConfig();
		Abilities.PK_Abilities.Earth.SandBreath.loadConfig();

		this.getCommand("Reload").setExecutor(new Commands());
		this.getServer().getPluginManager().registerEvents(new AbilitiesListener(), this);
		StartDeafnessTimer();
		System.out.println("Amonpack Załadowany");
	}

	@Override
	public void onDisable() {
		getLogger().info("AmonPack wyłączony");
	}

	public void createconf() {
		saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		saveConfig();
	}

	public static void reloadAllConfigs() {
		try {
			plugin.reloadConfig();
			AbilitiesConfig = YamlConfiguration.loadConfiguration(new File(configpath, "abilities_config.yml"));
			InputStream defAbilitiesStream = plugin.getResource("abilities_config.yml");
			if (defAbilitiesStream != null) {
				YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defAbilitiesStream, StandardCharsets.UTF_8));
				AbilitiesConfig.setDefaults(defConfig);
			}
			Abilities.PK_Abilities.Earth.SandWave.loadConfig();
			Abilities.PK_Abilities.Earth.SandBreath.loadConfig();
			System.out.println("pomyślnie zrobiono reload!");
		} catch (Exception e) {
			System.out.println("ERROR!!!  " + e);
			System.out.println("ERROR!!!  " + e.getMessage());
		}
	}

	public static Element getSmokeElement() {
		return SmokeElement;
	}

	public static Element getSoundElement() {
		return SoundElement;
	}

	public static FileConfiguration getAbilitiesConfig() {
		return AbilitiesConfig;
	}

	public static Element ElementBasedOnSubElement(Element SubElement) {
		for (Element element : Element.getAllElements()) {
			if (element.equals(SubElement)) {
				return element;
			}
			for (Element subele : Element.getSubElements(element)) {
				if (subele.equals(SubElement)) {
					return element;
				}
			}
		}
		return null;
	}
}
