package abilities;
import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.BetterParticles;
import abilities.Util_Objects.SmokeSource;
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

import java.util.ArrayList;
import java.util.List;


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
	private List<AbilityProjectile> Projectiles;
	private int interval;
	Location loc2;
	public SmokeDaggers(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		origin = player.getLocation().clone().add(0,1.3,0);
		Projectiles=new ArrayList<>();
		interval=0;
		List<BetterParticles> Particles = new ArrayList<>();
		Particles.add(new BetterParticles(8,ParticleEffect.SMOKE_NORMAL,0.3,0.01,0.15));
		Location Projectile = origin.clone();
		Vector Dir = Projectile.clone().getDirection();
		Location tloc1 = new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY()+1, player.getLocation().getZ(), (player.getLocation().getYaw() - 15), player.getLocation().getPitch());
		Vector Loc1Dir = tloc1.clone().getDirection();
		Location tloc2 = new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY()+1, player.getLocation().getZ(), (player.getLocation().getYaw() + 15), player.getLocation().getPitch());
		Vector Loc2Dir = tloc2.clone().getDirection();
		Projectiles.add(new AbilityProjectile(Loc1Dir,tloc1,origin,Particles,1));
		Projectiles.add(new AbilityProjectile(Loc2Dir,tloc2,origin,Particles,1));
		Projectiles.add(new AbilityProjectile(Dir,Projectile,origin,Particles,1));
		bPlayer.addCooldown(this);
		start();
	}
	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
			for (AbilityProjectile Projectile : Projectiles) {
				Location location = Projectile.Advance().clone();
				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
					if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
						DamageHandler.damageEntity(entity, 1, this);
						SmokeSource SourceEnd = new SmokeSource(location.clone().add(0,1,0), 120, 3, 1,player);
						Projectiles.remove(Projectile);
						return;
					}
				}
				if (location.distance(origin) > 20 || !location.clone().add(0,0.8,0).getBlock().getType().isAir() || location.clone().add(0,0.8,0).getBlock().getType().isSolid()) {
					SmokeSource SourceEnd = new SmokeSource(location.clone().add(0,1,0), 120, 3, 1,player);
					Projectiles.remove(Projectile);
					return;
				}
			}
		if(Projectiles.isEmpty()){
			remove();
		}


/*
		if (!GeneralMethods.isSolid(location.getBlock()) && origin.distance(location) < (range)) {
			location.add(direction.multiply(1));
			location.add(direction.multiply(1));
			ParticleEffect.SMOKE_NORMAL.display(location, 6, 0.3, 0.3, 0.3, 0.1);
	   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.5)) {
	   			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
	   			DamageHandler.damageEntity(entity, dmg, this);
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowdur, slowpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisondur, poisonpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blinddur, 1));
	   			//Methods.CreateSmokeZone(player,location.clone(), this, zonerange, zonedur);
					SmokeSource Source = new SmokeSource(location,60,2,1,player);
	   			location.zero();
	   	    	}}
			for (Block b : GeneralMethods.getBlocksAroundPoint(location, 1.5)) {
				if (b.getType() == Material.FIRE) {
		   			//Methods.CreateSmokeZone(player,location.clone(), this, zonerange, zonedur);
					SmokeSource Source = new SmokeSource(location,60,2,1,player);
				}}}
		if (!GeneralMethods.isSolid(location2.getBlock()) && origin.distance(location2) < (range)) {
			location2.add(loc1.getDirection().multiply(1));
			location2.add(loc1.getDirection().multiply(1));
			ParticleEffect.SMOKE_NORMAL.display(location2, 6, 0.3, 0.3, 0.3, 0.1);
	   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location2, 1.5)) {
	   			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
	   			DamageHandler.damageEntity(entity, dmg, this);
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowdur, slowpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisondur, poisonpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blinddur, 1));
	   			//Methods.CreateSmokeZone(player,location2.clone(), this, zonerange, zonedur);
					SmokeSource Source = new SmokeSource(location2,60,2,1,player);
	   			location2.zero();
	   	    	}}
			for (Block b : GeneralMethods.getBlocksAroundPoint(location2, 1.5)) {
				if (b.getType() == Material.FIRE) {
				//Methods.CreateSmokeZone(player,location2.clone(), this, zonerange, zonedur);
					SmokeSource Source = new SmokeSource(location2,60,2,1,player);
				}}}
			if (!GeneralMethods.isSolid(location3.getBlock()) && origin.distance(location3) < (range)) {
			location3.add(loc2.getDirection().multiply(1));
			location3.add(loc2.getDirection().multiply(1));
				ParticleEffect.SMOKE_NORMAL.display(location3, 6, 0.3, 0.3, 0.3, 0.1);
	   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location3, 1.5)) {
	   			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
	   			DamageHandler.damageEntity(entity, dmg, this);
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowdur, slowpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisondur, poisonpower));
	   			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blinddur, 1));
	   			//Methods.CreateSmokeZone(player,location3.clone(), this, zonerange, zonedur);
					SmokeSource Source = new SmokeSource(location3,60,2,1,player);
	   			location3.zero();
	   	    	}}
			for (Block b : GeneralMethods.getBlocksAroundPoint(location3, 1.5)) {
				if (b.getType() == Material.FIRE) {
				//Methods.CreateSmokeZone(player,location3.clone(), this, zonerange, zonedur);
					SmokeSource Source = new SmokeSource(location3,60,2,1,player);
				}}}
		if ((GeneralMethods.isSolid(location.getBlock()) || origin.distance(location) > (range)) &&
		(GeneralMethods.isSolid(location2.getBlock()) || origin.distance(location2) > (range)) &&
		(GeneralMethods.isSolid(location3.getBlock()) || origin.distance(location3) > (range))) {
			SmokeSource Source = new SmokeSource(location,60,2,1,player);
			remove();
   			return;
		}
		*/
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