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


public class EarthHammer extends EarthAbility implements AddonAbility {
	@Attribute("Cooldown")
	private long Cooldown = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthHammer.Cooldown");
	@Attribute("Damage")
	private int dmg = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthHammer.Damage");
	@Attribute("ChargeTime")
	private long TimeToCharge = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthHammer.ChargeTime");
	private final long RevertTime = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthHammer.RevertTime");
	@Attribute("Range")
	private int range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthHammer.Range");
	@Attribute("Radius")
	private int radius = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.EarthHammer.Radius");
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
	public EarthHammer(Player player) {
		super(player);
			if (!player.isSneaking()) {
				if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
                    interval=0;
					AbilityState = State.BENDABLE;
					//Firstloc = Methods.getTargetLocation(player,15);
					List<Location> shuffledList = new ArrayList<>();
				for (Block b : GeneralMethods.getBlocksAroundPoint(player.getLocation(), 10)) {
					if (b.getLocation().getY() <= player.getLocation().getY()+1 &&b.getLocation().distance(player.getLocation())>7  && EarthAbility.isEarthbendable(player, b)) {
						shuffledList.add(b.getLocation());
					}}
				if(shuffledList.size()>4){
					Collections.shuffle(shuffledList);
					NearBlocks = shuffledList.subList(0, 4);
					for(Location loc : NearBlocks){
						TempBlock tb1 = new TempBlock(loc.getBlock(), Material.AIR);
						tb1.setRevertTime(RevertTime);
						loc.setY(loc.getY()+2);
					}
					start();
				}
			}
		}
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
				if(interval>=2) {
					interval = 0;
					NearBlocks=Methods.BendableBlocksAnimation(NearBlocks,player.getLocation().clone(),Material.STONE,0.8);
					if(NearBlocks.isEmpty()|| NearBlocks.size()<2){
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
				ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 5, 0.5, 0.5, 0.5, 0.1, Material.DIRT.createBlockData());
				ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 5, 0.5, 0.5, 0.5, 0.1, Material.STONE.createBlockData());
			} else {
				AbilityState = State.USED;
				bPlayer.addCooldown(this);
			}}
		if (AbilityState == State.USED) {
			interval++;
			if(interval>=2){
				interval=0;
				Projectile.add(Direction).multiply(1);
				if(Projectile.getBlock().getType()!=Material.AIR && !PlantAbility.isPlant(Projectile.getBlock())){
					Projectile.setY(Projectile.getY()+1);
				}
				if(Projectile.clone().subtract(0,1,0).getBlock().getType()==Material.AIR){
					Projectile.setY(Projectile.getY()-1);
				}
				List<Block> BendableBlocks = new ArrayList<>();
				for (Block b : GeneralMethods.getBlocksAroundPoint(Projectile, radius)) {
					if (b.getY() < Projectile.getY() && (EarthAbility.isEarthbendable(player, b))) {
						BendableBlocks.add(b);
						int chance = getRandom(0, 15);
						if (chance <=  5) {
							TempBlock tb2 = new TempBlock(b, Material.STONE);
							tb2.setRevertTime(RevertTime);
						} else if (chance <=  10) {
							TempBlock tb2 = new TempBlock(b, Material.DIRT);
							tb2.setRevertTime(RevertTime);
						}else if (chance <=  13) {
							Methods.spawnFallingBlocks(b.getLocation(),Material.STONE,1,1,player);
						}else {
							Methods.spawnFallingBlocks(b.getLocation(),Material.DIRT,1,1,player);
						}
					}}
				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(Projectile, radius)) {
					if ((entity instanceof LivingEntity)) {
						if (entity.getUniqueId() != player.getUniqueId()) {
							if (entity.getLocation().getY() < Projectile.getY()+2) {
								Vector forceDir = GeneralMethods.getDirection(Projectile.clone().subtract(0,4,0),entity.getLocation());
								entity.setVelocity(forceDir.clone().normalize().multiply(1));
								DamageHandler.damageEntity(entity, dmg, this);
							}}}}
				if (BendableBlocks.size()<2){
					Methods.spawnFallingBlocks(Projectile,Material.DIRT,12,2.2,player);
					remove();
				}
			}
			Block stop = Projectile.clone().add(0,2,0).getBlock();
			if(!EarthAbility.isEarthbendable(player,stop) && !stop.getType().isAir()){
				remove();
			}
			if (Projectile.distance(origin) > range){
				Methods.spawnFallingBlocks(Projectile,Material.DIRT,12,2.2,player);
				remove();
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
		return "EarthHammer";
	}
	@Override
	public String getDescription() {
		return "";
	}
	@Override
	public String getInstructions() {
		return "Hold Shift near EarthBendable blocks to charge, then relase it";
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