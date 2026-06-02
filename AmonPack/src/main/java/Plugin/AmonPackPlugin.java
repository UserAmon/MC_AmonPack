package Plugin;

import Abilities.Bending.AbilitiesListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.ElementType;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ProjectKorra;

import static Abilities.Bending.SoundAbility.StartDeafnessTimer;

public class AmonPackPlugin extends JavaPlugin {
	public static Plugin plugin;
	private static Element SmokeElement;
	private static Element SoundElement;
	private static Element BladesElement;
	private java.io.File configFile;
	private FileConfiguration config;

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
			configFile = new java.io.File(getDataFolder(), "abilities_config.yml");
		}
		config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);

		java.io.InputStream defConfigStream = getResource("abilities_config.yml");
		if (defConfigStream != null) {
			org.bukkit.configuration.file.YamlConfiguration defConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defConfigStream, java.nio.charset.StandardCharsets.UTF_8));
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
			configFile = new java.io.File(getDataFolder(), "abilities_config.yml");
		}
		if (!configFile.exists()) {            
			saveResource("abilities_config.yml", false);
		}
	}

	@Override
	public void onEnable() {
		plugin = this;
		getLogger().info("AmonPack włączony");

		BladesElement = new SubElement("Blades", Element.CHI, ElementType.BLOCKING, ProjectKorra.plugin);
		SmokeElement = new SubElement("Smoke", Element.FIRE, ElementType.BENDING, ProjectKorra.plugin);
		SoundElement = new SubElement("Sound", Element.AIR, ElementType.BENDING, ProjectKorra.plugin);
		CoreAbility.registerPluginAbilities(this, "Abilities.PK_Abilities");
		createconf();

		Commands cmdExecutor = new Commands();
		this.getCommand("Reload").setExecutor(cmdExecutor);
		this.getServer().getPluginManager().registerEvents(new AbilitiesListener(), this);
		StartDeafnessTimer();
		System.out.println("Amonpack Załadowany");
	}

	@Override
	public void onDisable() {
		getLogger().info("AmonPack wyłączony");
	}

	public static Element getSmokeElement() {
		return SmokeElement;
	}

	public static Element getSoundElement() {
		return SoundElement;
	}

	public static Element getBladesElement() {
		return BladesElement;
	}

	public static FileConfiguration getAbilitiesConfig() {
		return plugin.getConfig();
	}

	public void createconf() {
		getConfig();
		config.addDefault("AmonPack.Water.Ice.IcySpace.Cooldown", 12000);
		config.addDefault("AmonPack.Water.Ice.IcySpace.Range", 5);
		config.addDefault("AmonPack.Water.Ice.IcySpace.Duration", 10000);
		config.addDefault("AmonPack.Water.Ice.IcySpace.NightAugment.Cooldown", 5000);
		config.addDefault("AmonPack.Water.Ice.IcySpace.NightAugment.Range", 12);
		config.addDefault("AmonPack.Water.Ice.IcySpace.NightAugment.Duration", 60000);
		config.addDefault("AmonPack.Water.Ice.IcySpace.FullMoonAugment.Cooldown", 12000);
		config.addDefault("AmonPack.Water.Ice.IcySpace.FullMoonAugment.Range", 8);
		config.addDefault("AmonPack.Water.Ice.IcySpace.FullMoonAugment.Duration", 20000);
		config.addDefault("AmonPack.Water.Ice.IcySpace.1stPhaseDelay", 3);
		config.addDefault("AmonPack.Water.Ice.IcySpace.2ndPhaseDelay", 6);
		config.addDefault("AmonPack.Elemental.Water.DryGrassRevert", 15000);
		config.addDefault("AmonPack.Elemental.Water.DryGrassRange", 3);
		config.addDefault("AmonPack.Elemental.Smoke.BlindnessDuration", 40);
		config.addDefault("AmonPack.Elemental.Smoke.PoisonDuration", 40);
		config.addDefault("AmonPack.Elemental.Smoke.PoisonPower", 1);
		config.addDefault("AmonPack.Elemental.Smoke.SlowPower", 3);
		config.addDefault("AmonPack.Elemental.Smoke.SlowDuration", 40);
		config.addDefault("AmonPack.Elemental.Smoke.AffectUser", false);
		config.addDefault("AmonPack.Earth.Sand.SandWave.Cooldown", 6000);
		config.addDefault("AmonPack.Earth.Sand.SandWave.Range", 15);
		config.addDefault("AmonPack.Earth.Sand.SandWave.Duration", 4000);
		config.addDefault("AmonPack.Earth.Sand.SandWave.Size", 6);
		config.addDefault("AmonPack.Earth.Sand.SandWave.DeBuffPower", 2);
		config.addDefault("AmonPack.Earth.Sand.SandWave.DebuffDuration", 50);
		config.addDefault("AmonPack.Earth.Sand.SandWave.BurrowPower", 1);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.Cooldown", 6000);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.Range", 12);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.Duration", 4000);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.ChargeTime", 2000);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.ChargedBreathBuff", true);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.SpeedOnSand", 6);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.SpeedOnEarth", 2);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.DeBuffPower", 2);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.DebuffDuration", 60);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.Damage", 1);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.DurationToUseBreath", 120);
		config.addDefault("AmonPack.Earth.Sand.SandBreath.CanDebuffEnemy", true);
		config.addDefault("AmonPack.Earth.Metal.SteelShackles.Range", 35);
		config.addDefault("AmonPack.Earth.Metal.SteelShackles.Hitbox", 3);
		config.addDefault("AmonPack.Earth.Metal.SteelShackles.DamageFirst", 2);
		config.addDefault("AmonPack.Earth.Metal.SteelShackles.DamageSecond", 1);
		config.addDefault("AmonPack.Earth.Metal.SteelShackles.StunRange", 15);
		config.addDefault("AmonPack.Earth.Metal.SteelShackles.StunDuration", 60);
		config.addDefault("AmonPack.Earth.Metal.SteelShackles.TimeToEscape", 60);
		config.addDefault("AmonPack.Earth.Metal.SteelShackles.DurabilityCost", 20);
		config.addDefault("AmonPack.Earth.Metal.SteelShackles.Cooldown", 7000);
		config.addDefault("AmonPack.Earth.Metal.MetalFlex.CooldownNormal", 10000);
		config.addDefault("AmonPack.Earth.Metal.MetalFlex.CooldownCrysis", 40000);
		config.addDefault("AmonPack.Earth.Metal.MetalFlex.SpeedPower", 3);
		config.addDefault("AmonPack.Earth.Metal.MetalFlex.CrysisDuration", 100);
		config.addDefault("AmonPack.Earth.Metal.MetalFlex.LowLevel", 5);
		config.addDefault("AmonPack.Earth.Metal.MetalCompress.CooldownMin", 4000);
		config.addDefault("AmonPack.Earth.Metal.MetalCompress.CooldownMax", 8000);
		config.addDefault("AmonPack.Earth.Metal.MetalCompress.Damage", 2);
		config.addDefault("AmonPack.Earth.Metal.MetalCompress.MaxChargeTime", 1700);
		config.addDefault("AmonPack.Earth.Metal.MetalCompress.Duration", 5);
		config.addDefault("AmonPack.Earth.Metal.MetalCompress.DurabilityCostMin", 10);
		config.addDefault("AmonPack.Earth.Metal.MetalCompress.DurabilityCostMax", 30);
		config.addDefault("AmonPack.Earth.Metal.SteelSwing.Cooldown", 5000);
		config.addDefault("AmonPack.Earth.Metal.SteelSwing.Range", 25.0);
		config.addDefault("AmonPack.Earth.Metal.SteelSwing.Speed", 2.2);
		config.addDefault("AmonPack.Earth.Metal.SteelGrab.Cooldown", 6000);
		config.addDefault("AmonPack.Earth.Metal.SteelGrab.Range", 28.0);
		config.addDefault("AmonPack.Earth.Metal.SteelGrab.Speed", 2.2);
		config.addDefault("AmonPack.Earth.Metal.FerroAbsorb.Cooldown", 8000);
		config.addDefault("AmonPack.Earth.Metal.FerroClips.Cooldown", 8000);

		config.addDefault("AmonPack.Earth.EarthHammer.Cooldown", 7000);
		config.addDefault("AmonPack.Earth.EarthHammer.Damage", 1);
		config.addDefault("AmonPack.Earth.EarthHammer.ChargeTime", 2000);
		config.addDefault("AmonPack.Earth.EarthHammer.RevertTime", 10000);
		config.addDefault("AmonPack.Earth.EarthHammer.Range", 20);
		config.addDefault("AmonPack.Earth.EarthHammer.Radius", 2);

		config.addDefault("AmonPack.Chi.Blades.Slash.Dmg-1", 1);
		config.addDefault("AmonPack.Chi.Blades.Slash.Dmg-2", 1);
		config.addDefault("AmonPack.Chi.Blades.Slash.Dmg-3", 2);
		config.addDefault("AmonPack.Chi.Blades.Slash.Cooldown", 4000);
		config.addDefault("AmonPack.Chi.Blades.Slash.SpeedPower", 3);
		config.addDefault("AmonPack.Chi.Blades.Slash.SpeedDuration", 20);
		config.addDefault("AmonPack.Chi.Blades.Slash.InvDuration", 20);
		config.addDefault("AmonPack.Chi.Blades.Slash.EvadePower", 1);

		config.addDefault("AmonPack.Chi.Blades.Pierce.SpeedPower", 3);
		config.addDefault("AmonPack.Chi.Blades.Pierce.SpeedDuration", 60);
		config.addDefault("AmonPack.Chi.Blades.Pierce.Dmg-1", 1);
		config.addDefault("AmonPack.Chi.Blades.Pierce.Dmg-2", 3);
		config.addDefault("AmonPack.Chi.Blades.Pierce.Cooldown", 4000);
		config.addDefault("AmonPack.Chi.Blades.Pierce.DashPower", 2);

		config.addDefault("AmonPack.Chi.Blades.Stab.Uses", 4);
		config.addDefault("AmonPack.Chi.Blades.Stab.Dmg-Left", 2);
		config.addDefault("AmonPack.Chi.Blades.Stab.Dmg-Right", 2);
		config.addDefault("AmonPack.Chi.Blades.Stab.Cooldown", 4000);

		config.addDefault("AmonPack.Chi.Blades.Counter.MaxHoldTime", 2500);
		config.addDefault("AmonPack.Chi.Blades.Counter.EvadePower", 1);
		config.addDefault("AmonPack.Chi.Blades.Counter.Cooldown", 4000);
		config.addDefault("AmonPack.Chi.Blades.Counter.DashInAir", true);

		config.addDefault("AmonPack.Air.AirPressure.MaxHoldTime", 4000);
		config.addDefault("AmonPack.Air.AirPressure.Cooldown", 4000);
		config.addDefault("AmonPack.Air.AirPressure.Dmg", 2);
		config.addDefault("AmonPack.Air.AirPressure.Range-Sphere", 20);
		config.addDefault("AmonPack.Air.AirPressure.Range-Pull", 4);
		config.addDefault("AmonPack.Air.AirPressure.PushPower", 1.5);
		config.addDefault("AmonPack.Air.AirPressure.MinHoldTime", 500);
		config.addDefault("AmonPack.Air.AirPressure.CanControlSphere", false);

		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.Cooldown", 4000);
		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.Range", 30);
		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.Dmg", 1);
		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.SlowPower", 2);
		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.SlowDuration", 40);
		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.PoisonPower", 1);
		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.PoisonDuration", 40);
		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.BlindnessDuration", 40);
		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.SmokeZoneDuration", 100);
		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.SmokeZoneRange", 3);

		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.Cooldown", 4000);
		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.Range", 30);
		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.Dmg", 1);
		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.SlowPower", 2);
		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.SlowDuration", 40);
		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.PoisonPower", 1);
		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.PoisonDuration", 40);
		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.BlindnessDuration", 40);
		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.SmokeZoneDuration", 100);
		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.SmokeZoneRange", 2);

		config.addDefault("AmonPack.Water.Ice.IceArch.Cooldown", 4000);
		config.addDefault("AmonPack.Water.Ice.IceArch.Range", 20);
		config.addDefault("AmonPack.Water.Ice.IceArch.ChargeTime", 1500);
		config.addDefault("AmonPack.Water.Ice.IceArch.Damage", 2);
		config.addDefault("AmonPack.Water.Ice.IceArch.Arch-Width", 3);
		config.addDefault("AmonPack.Water.Ice.IceArch.Arch-Duration", 5000);
		config.addDefault("AmonPack.Water.Ice.IceArch.Arch-Thickness", 2);
		config.addDefault("AmonPack.Water.Ice.IceArch.CanFreeze", false);
		config.addDefault("AmonPack.Water.Ice.IceArch.FreezeDuration", 2000);

		config.addDefault("AmonPack.Water.Ice.IceThorn.FreezeDuration", 3000);
		config.addDefault("AmonPack.Water.Ice.IceThorn.Cooldown", 7000);
		config.addDefault("AmonPack.Water.Ice.IceThorn.Damage", 1);
		config.addDefault("AmonPack.Water.Ice.IceThorn.ChargeTime", 2000);
		config.addDefault("AmonPack.Water.Ice.IceThorn.RevertTime", 10000);
		config.addDefault("AmonPack.Water.Ice.IceThorn.Range", 20);
		config.addDefault("AmonPack.Water.Ice.IceThorn.Radius", 2);

		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Cooldown", 4000);
		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.ChargeTime", 1000);
		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Range", 30);
		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Damage", 3);
		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Width", 4);
		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Duration", 5000);
		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Thickness", 3);
		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.CanFreeze", false);
		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.FreezeDuration", 2000);

		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Cooldown", 1000);
		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.ChargeTime", 500);
		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Range", 40);
		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Damage", 5);
		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Width", 5);
		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Duration", 5000);
		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Thickness", 4);
		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.CanFreeze", false);
		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.FreezeDuration", 2000);

		config.options().copyDefaults(true);
		saveConfig();
	}
}
