package abilities;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;
import methods_plugins.Abilities.SmokeAbility;



public class SmokeDaggers extends SmokeAbility implements AddonAbility {
	private int Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.Cooldown");
	private int dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.Dmg");
	private int range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.Range");
	private int slowpower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.SlowPower");
	private int slowdur = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.SlowDuration");
	private int poisonpower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.PoisonPower");
	private int poisondur = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.PoisonDuration");
	private int blinddur = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.BlindnessDuration");
	private int zonerange = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.SmokeZoneRange");
	private int zonedur = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.SmokeZoneDuration");
	Location origin;
	Location location;
	Location location2;
	Location location3;
	Vector direction;
	Location loc1;
	Location loc2;
	public SmokeDaggers(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		origin = player.getLocation().clone().add(0,1,0);
		location = origin.clone();
		location2 = origin.clone();
		location3 = origin.clone();
		loc1 = new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), (player.getLocation().getYaw() + 20), player.getLocation().getPitch());
		loc2 = new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), (player.getLocation().getYaw() - 20), player.getLocation().getPitch());
		direction = player.getLocation().getDirection().clone();
		bPlayer.addCooldown(this);
		start();
		
	        
	}
	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (!GeneralMethods.isSolid(location.getBlock()) && origin.distance(location) < (range)) {
			location.add(direction.multiply(1));
			location.add(direction.multiply(1));
			ParticleEffect.SMOKE_NORMAL.display(location, 3, 0.3, 0.3, 0.3, 0);
	   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.5)) {
	   			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
	   			DamageHandler.damageEntity(entity, dmg, this);
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowdur, slowpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisondur, poisonpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blinddur, 1));
	   			Methods.CreateSmokeZone(player,location.clone(), this, zonerange, zonedur);
	   			location.zero();
	   	    	}}
			for (Block b : GeneralMethods.getBlocksAroundPoint(location, 1.5)) {
				if (b.getType() == Material.FIRE) {
		   			Methods.CreateSmokeZone(player,location.clone(), this, zonerange, zonedur);
				}}}
		if (!GeneralMethods.isSolid(location2.getBlock()) && origin.distance(location2) < (range)) {
			location2.add(loc1.getDirection().multiply(1));
			location2.add(loc1.getDirection().multiply(1));
			ParticleEffect.SMOKE_NORMAL.display(location2, 3, 0.3, 0.3, 0.3, 0);
	   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location2, 1.5)) {
	   			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
	   			DamageHandler.damageEntity(entity, dmg, this);
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowdur, slowpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisondur, poisonpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blinddur, 1));
	   			Methods.CreateSmokeZone(player,location2.clone(), this, zonerange, zonedur);
	   			location2.zero();
	   	    	}}
			for (Block b : GeneralMethods.getBlocksAroundPoint(location2, 1.5)) {
				if (b.getType() == Material.FIRE) {
				Methods.CreateSmokeZone(player,location2.clone(), this, zonerange, zonedur);
				}}}
			if (!GeneralMethods.isSolid(location3.getBlock()) && origin.distance(location3) < (range)) {
			location3.add(loc2.getDirection().multiply(1));
			location3.add(loc2.getDirection().multiply(1));
			ParticleEffect.SMOKE_NORMAL.display(location3, 3, 0.3, 0.3, 0.3, 0);
	   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location3, 1.5)) {
	   			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
	   			DamageHandler.damageEntity(entity, dmg, this);
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowdur, slowpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisondur, poisonpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blinddur, 1));
	   			Methods.CreateSmokeZone(player,location3.clone(), this, zonerange, zonedur);
	   			location3.zero();
	   	    	}}
			for (Block b : GeneralMethods.getBlocksAroundPoint(location3, 1.5)) {
				if (b.getType() == Material.FIRE) {
				Methods.CreateSmokeZone(player,location3.clone(), this, zonerange, zonedur);
				}}}
		if ((GeneralMethods.isSolid(location.getBlock()) || origin.distance(location) > (range)) &&
		(GeneralMethods.isSolid(location2.getBlock()) || origin.distance(location2) > (range)) &&
		(GeneralMethods.isSolid(location3.getBlock()) || origin.distance(location3) > (range))) {
			remove();
   			return;
		}
		
	}
	
	@Override
	public long getCooldown() {
		return Cooldown;
	}
	@Override
	public Location getLocation() {
		return null;
	}
	@Override
	public String getName() {
		return "SmokeDaggers";
	}
	@Override
	public String getDescription() {
		return "";
	}
	@Override
	public String getInstructions() {
		return "";
	}
	@Override
	public String getAuthor() {
		return "AmonPack";
	}
	@Override
	public String getVersion() {
		return "1.0";
	}
	@Override
	public boolean isHarmlessAbility() {
		return false;
	}
	@Override
	public boolean isSneakAbility() {
		return false;
	}
	@Override
	public void load() {}
	@Override
	public void stop() {
		super.remove();
	}
}