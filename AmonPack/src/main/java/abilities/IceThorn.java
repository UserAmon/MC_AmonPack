package abilities;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.firebending.FireBlastCharged;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static methods_plugins.Methods.getRandom;


public class IceThorn extends IceAbility implements AddonAbility {
	@Attribute("Cooldown")
	private long Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceThorn.Cooldown");
	@Attribute("Damage")
	private int dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceThorn.Damage");
	@Attribute("ChargeTime")
	private long TimeToCharge = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceThorn.ChargeTime");
	private final long RevertTime = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Water.Ice.IceThorn.RevertTime");
	private final long FreezeDuration = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Water.Ice.IceThorn.FreezeDuration");
	@Attribute("Range")
	private int range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceThorn.Range");
	@Attribute("Radius")
	private int radius = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceThorn.Radius");
	private State AbilityState;
	private enum State {
		BENDABLE,
		CHARGING,
		READY,
		USED
	}
	private List<Location> NearBlocks;
	private Location Projectile;
	private Vector Direction;
	private Location origin;
	private long interval;
	private long spikeinterval;
	public IceThorn(Player player) {
		super(player);
		if (!player.isSneaking()) {
			if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
				Location TargetedBlock = Methods.getTargetLocation(player,15);
				if(WaterAbility.isWaterbendable(TargetedBlock.getBlock().getType()) ){
					interval=0;
					AbilityState = State.BENDABLE;
					NearBlocks=new ArrayList<>();
					NearBlocks.add(TargetedBlock);
					start();
				}}}
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (AbilityState == State.BENDABLE) {
			if (player.isSneaking()) {
				interval++;
				if(interval>=3) {
					interval = 0;
					NearBlocks=Methods.BendableBlocksAnimation(NearBlocks,player.getLocation().clone(),Material.WATER,1);
					if(NearBlocks.isEmpty()){
						AbilityState = State.CHARGING;
					}}
			}else{
				bPlayer.addCooldown(this);
				remove();
				return;
			}}
		if (AbilityState == State.CHARGING) {
			if (bPlayer.getBoundAbility() == null || bPlayer.getBoundAbilityName() == null || !bPlayer.getBoundAbilityName().equalsIgnoreCase(getName())) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}
			if (System.currentTimeMillis() > getStartTime() + TimeToCharge) {
				AbilityState = State.READY;
			}
			if (!player.isSneaking()) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}}
		if (AbilityState == State.READY) {
			origin = player.getLocation();
			origin.setPitch(0);
			Direction = origin.getDirection();
			Projectile = origin.clone();
			if (player.isSneaking()) {
				ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 5, 0.5, 0.5, 0.5, 0.1, Material.ICE.createBlockData());
				ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 5, 0.5, 0.5, 0.5, 0.1, Material.SNOW_BLOCK.createBlockData());
			} else {
				AbilityState = State.USED;
				bPlayer.addCooldown(this);
			}}
		if (AbilityState == State.USED) {
			interval++;
			if(interval>=2){
				spikeinterval++;
				interval=0;
				Projectile.add(Direction).multiply(1);
				if(Projectile.getBlock().getType()!=Material.AIR && Projectile.getBlock().getType()==Material.WATER&& !Projectile.getBlock().getType().isSolid()){
					Projectile.setY(Projectile.getY()+1);
				}
				if(Projectile.clone().subtract(0,1,0).getBlock().getType()==Material.AIR){
					Projectile.setY(Projectile.getY()-1);
				}
				List<Block> BendableBlocks = new ArrayList<>();
				for (Block b : GeneralMethods.getBlocksAroundPoint(Projectile, radius)) {
					if (b.getY() < Projectile.getY() &&b.getY()>Projectile.getY()-2 && (WaterAbility.isWaterbendable(b.getType())||EarthAbility.isEarthbendable(player,b))) {
						BendableBlocks.add(b);
						int chance = getRandom(0, 15);
						if (chance <=  6) {
							TempBlock tb2 = new TempBlock(b, Material.SNOW_BLOCK);
							tb2.setRevertTime(RevertTime);
						} else if (chance <=  12) {
							TempBlock tb2 = new TempBlock(b, Material.ICE);
							tb2.setRevertTime(RevertTime);
						}else {
							Methods.spawnFallingBlocks(b.getLocation(),Material.ICE,1,0.75,player);
						}
					}}
				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(Projectile, radius)) {
					if ((entity instanceof LivingEntity)) {
						if (entity.getUniqueId() != player.getUniqueId()) {
							if (entity.getLocation().getY() < Projectile.getY()+2) {
								Methods.FreezeTarget(entity.getLocation(),3,1,3,FreezeDuration,Material.ICE);
								DamageHandler.damageEntity(entity, dmg, this);
								Methods.FreezeTarget(Projectile.clone().subtract(0,1,0),3,1,0,FreezeDuration,Material.ICE);
								remove();
								break;
							}}}}
				if(spikeinterval>5){
					spikeinterval=0;
					Methods.FreezeTarget(Projectile,1,2,4,RevertTime,Material.ICE);
					Location tempprojectile=Projectile.clone().add(Direction).multiply(1);
					Methods.spawnFallingBlocks(tempprojectile,Material.ICE,6,1.2,player);
				}
				Block stop = Projectile.clone().add(0,2,0).getBlock();
				if(!WaterAbility.isWaterbendable(stop.getType()) && !stop.getType().isAir()){
					remove();
				}
				if (Projectile.distance(origin) > range||BendableBlocks.size()<2){
					remove();
				}
			}
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
		return "IceThorn";
	}
	@Override
	public String getDescription() {
		return "";
	}
	@Override
	public String getInstructions() {
		return "Hold Shift near WaterBendable blocks to charge, then relase it";
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
		return true;
	}
	@Override
	public void load() {
	}
	@Override
	public void stop() {
		super.remove();
	}
}