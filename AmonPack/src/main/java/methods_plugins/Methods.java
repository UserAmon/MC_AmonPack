package methods_plugins;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.ice.PhaseChange;

import abilities.SandBreath;

public class Methods {
	public Methods() {
		super();
    }
	
	public static void CreateSmokeZone(Player player, Location loc, Ability abi, double range, long duration) {
		int i = 0;
		CreateSmokeZoneSub(player,loc,abi,range,duration);
		for (Block b : GeneralMethods.getBlocksAroundPoint(loc, range)) {
		if (b.getType() == Material.FIRE) {
		b.setType(Material.AIR);
		i++;
		if (i == 4) {
		CreateSmokeZoneSub(player ,b.getLocation(), abi, range, duration);
		i=0;
		}}}}
	public static int getRandom(int lower, int upper) {
		Random random = new Random();
		return random.nextInt((upper - lower) + 1) + lower;
	}

	public static List<FallingBlock> SpawnedByMe = new ArrayList<>();
	public static void spawnFallingBlocks(Location location,Material mat, int amount,double factor,Player player) {
		if (location == null || location.getWorld() == null) return;
		World world = location.getWorld();
		Random random = new Random();
		for (int i = 0; i < amount; i++) {
			FallingBlock fallingBlock = world.spawnFallingBlock(location, mat.createBlockData());
			fallingBlock.setDropItem(false);
			double x = (random.nextDouble() - 0.5) * (0.5*factor);
			double z = (random.nextDouble() - 0.5) * (0.5*factor);
			double y = 0.3 + random.nextDouble() * (0.1*factor);
			fallingBlock.setVelocity(new Vector(x, y, z));
			SpawnedByMe.add(fallingBlock);
			startDamageTask(fallingBlock,player);
		}
	}
	private static void startDamageTask(FallingBlock fallingBlock, Player player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (fallingBlock.isDead() || !fallingBlock.isValid()) {
					this.cancel();
					SpawnedByMe.remove(fallingBlock.getUniqueId());
					return;
				}
				for (Entity entity : fallingBlock.getNearbyEntities(1.5, 1.5, 1.5)) {
					if (entity instanceof LivingEntity && entity!=player) {
						((LivingEntity) entity).damage(1.0);
					}
				}
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 5);
	}

	public static void CreateSmokeZoneSub(Player player, Location loc, Ability abi, double range, long duration) {
		int slowpower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Elemental.Smoke.SlowPower");
		int slowdur = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Elemental.Smoke.SlowDuration");
		int poisonpower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Elemental.Smoke.PoisonPower");
		int poisondur = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Elemental.Smoke.PoisonDuration");
		int blinddur = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Elemental.Smoke.BlindnessDuration");
		boolean affect = AmonPackPlugin.plugin.getConfig().getBoolean("AmonPack.Elemental.Smoke.AffectUser");
		HashMap<String, Integer> taskID = new HashMap<String, Integer>();
    	taskID.put("Task",Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, new Runnable() {
    		   public void run() {
    			ParticleEffect.SMOKE_NORMAL.display(loc, (int) (range*5), (range/2), 0.5, (range/2), 0.1);
    			ParticleEffect.SMOKE_LARGE.display(loc, (int) (range*2), (range/2), 0.5, (range/2), 0.1);
    			for (Entity entity : loc.getWorld().getNearbyEntities(loc, range, range , range)) {
    	 		if ((entity instanceof LivingEntity)) {
    	 		if (affect == true) {
    				((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowdur, slowpower));
    				((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisondur, poisonpower));
    				((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blinddur, 1));
    	 			} else if (affect == false) {
    	 			if (entity.getUniqueId() != player.getUniqueId()) {
        			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowdur, slowpower));
        			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisondur, poisonpower));
        			((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blinddur, 1));
        	 		}}}}
    		   }},0, 2));
  		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getScheduler().cancelTask(taskID.get("Task"));
			}}.runTaskLater(AmonPackPlugin.plugin,duration);
	}
	
    
    public static void Spin(Player player) {
   		   int l = 0;
   		   do {
          		new BukkitRunnable() {
       			@Override
       			public void run() {
      			   Vector pvector = player.getVelocity();
             	   ParticleEffect.CRIT.display(player.getLocation(), 1, 1, 1, 0, 3);
             		  Location loc = player.getLocation();
     		  	      loc.setYaw(loc.getYaw() + 36);
     		  	      player.teleport(loc);
     		  	      player.setVelocity(pvector);
               	}}.runTaskLater(AmonPackPlugin.plugin,10 - l);
    	l = l+1;
   		   }while (l != 11);
   	   }




	public static void FreezeTarget(Location location, int range, int rangeY,int maxy, long Duration,Material mat) {
		for (int i = 0; i < rangeY; i++) {
		for (Block b : GeneralMethods.getBlocksAroundPoint(location.add(0,i,0), range)) {
			if ((b.getType().isAir()||WaterAbility.isWaterbendable(b.getType())||EarthAbility.isEarthbendable(b.getType(),false,true,false)) &&
					b.getY()<=location.getY()+maxy) {
						TempBlock tb1 = new TempBlock(b, mat);
						addFrozenBlock(tb1);
						tb1.setRevertTime(Duration);
					}}}
	}

/*
			if(intervals>500){
		intervals=0;
		List<Location>UsedLoc=new ArrayList<>();
		for (Location b : NearBlocks) {
			if (player.getLocation().distance(b) >= 2) {
				b.add(GeneralMethods.getDirection(b, player.getLocation().clone().add(0,1,0)).normalize().multiply(0.2));
				TempBlock tb1 = new TempBlock(b.getBlock(), b.getBlock().getType());
				tb1.setRevertTime(200);
			} else {
				UsedLoc.add(b);
				neededblocks = neededblocks - 1;
			}}
		NearBlocks.removeAll(UsedLoc);
	}else{
		intervals++;
	}
*/



    public static void FreezeField(Location location, int range, int Duration2, int Delay1, int Delay2) {
    	if (Duration2 <= 2700) {
    		Duration2 = Duration2 + 2700;
    	}
    		long Duration = Duration2;
		new BukkitRunnable() {
			@Override
			public void run() {
				
		    	for (Block b : GeneralMethods.getBlocksAroundPoint(location, range/4)) {
		    		  if (!b.getType().isSolid()) {
		    			  if (!b.isLiquid()) {
		    				  
		  			  if (b.getY() == location.getY()) {
		    			  TempBlock tb1 = new TempBlock(b, Material.SNOW);
		    			  addFrozenBlock(tb1);
		  	    		tb1.setRevertTime(Duration);
		       			ParticleEffect.SNOW_SHOVEL.display(location, 10, range/4, 1, range/4, 0);
		  		}}}}
		  		new BukkitRunnable() {
		  			@Override
		  			public void run() {
		  				
		        	for (Block b : GeneralMethods.getBlocksAroundPoint(location.subtract(0,1,0), range/2)) {
		    			  if (b.getY() == location.getY()) {
		    				  if (WaterAbility.isEarth(b) || WaterAbility.isWater(b) || WaterAbility.isSand(b) || WaterAbility.isSnow(b)) {
			    				  
		      			  TempBlock tb1 = new TempBlock(b, Material.ICE);
	    				  addFrozenBlock(tb1);
		  	    		tb1.setRevertTime((Delay2*1000) - (Delay1*1000));
		         			ParticleEffect.SNOW_SHOVEL.display(location, 10, range/2, 1, range/2, 0);
		    		}}}
		  				
		  			}}.runTaskLater(ProjectKorra.plugin,((Delay1*20)+1));
		  		new BukkitRunnable() {
		  			@Override
		  			public void run() {
		  				
		        	for (Block b : GeneralMethods.getBlocksAroundPoint(location, range)) {
		    			  if (b.getY() == location.getY()) {  
			    			  if (WaterAbility.isEarth(b) || WaterAbility.isWater(b) || WaterAbility.isSand(b) || WaterAbility.isSnow(b)) {
		    				  TempBlock tb1 = new TempBlock(b, Material.ICE);
		    				  addFrozenBlock(tb1);
		  	    		tb1.setRevertTime(Duration-(Delay2*1000));
				  		new BukkitRunnable() {
				  			@Override
				  			public void run() {
				  				ParticleEffect.SNOW_SHOVEL.display(location, 15, range, 2, range, 0);
				  				
				  			}}.runTaskLater(ProjectKorra.plugin,((Duration/1000)*20)-(Delay2*20));
		  	    		
		  	    		
		  	    		
		  	    		
		         			ParticleEffect.SNOW_SHOVEL.display(location, 10, range, 1, range, 0);
		    		}}}
		  				
		  			}}.runTaskLater(ProjectKorra.plugin,((Delay2*20)+2));
		      	
				
			}}.runTaskLater(ProjectKorra.plugin,10);
    }
    
    
    public static Location getTargetLocation(Player player, double range) {
        Vector direction = player.getLocation().getDirection().clone().multiply(0.1D);
        Location loc = player.getEyeLocation().clone();
        Location startLoc = loc.clone();
        do {
            loc.add(direction);
        } while(startLoc.distance(loc) < range && loc.getBlock().getBlockData().getMaterial() == Material.AIR);
        return loc;
    }
    public static void SmoothBlock(Player player, Location loc, Material mat, boolean WaterAbility) {
	    int DryRevert = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Elemental.Water.DryGrassRevert");
	    int DryRange = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Elemental.Water.DryGrassRange");
    	HashMap<String, Integer> taskID = new HashMap<String, Integer>();
		Location temploc = loc.clone().add(0,1,0);
   			taskID.put("Tasknr1",Bukkit.getScheduler().scheduleSyncRepeatingTask(ProjectKorra.plugin, new Runnable() {
	    		   public void run() {
	    			   if (player.getLocation().distance(temploc) >= 1) {
	    			temploc.add(GeneralMethods.getDirection(temploc, player.getLocation()).normalize().multiply(1));
	    		  TempBlock tb1 = new TempBlock(temploc.getBlock(), mat);
	    		tb1.setRevertTime(100);
	    		   } else Bukkit.getScheduler().cancelTask(taskID.get("Tasknr1"));
	    			   }
         		}, 0, 1));
   			if (WaterAbility == true) {
   			if (loc.getBlock().getBlockData().getMaterial() == Material.GRASS_BLOCK) {
   				for (Block b : GeneralMethods.getBlocksAroundPoint(loc, DryRange)) {
	    		   			if (b.getBlockData().getMaterial() == Material.GRASS_BLOCK) {
	    		   				TempBlock tb1 = new TempBlock(b, Material.PODZOL);
	    		   	    		tb1.setRevertTime(DryRevert);
	    		   			}
	    				  
		    			  }
   	    		
   	    		
   	    		
   			} else {
   				TempBlock tb1 = new TempBlock(loc.getBlock(), Material.AIR);
   	    		tb1.setRevertTime(5000);
   			}} else {
   				TempBlock tb1 = new TempBlock(loc.getBlock(), Material.AIR);
   	    		tb1.setRevertTime(5000);
   			}
    }



	public static List<Location> BendableBlocksAnimation(List<Location> NearBlocks,Location playerloc, Material mat, double speed) {
		//int DryRevert = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Elemental.Water.DryGrassRevert");
		//int DryRange = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Elemental.Water.DryGrassRange");
		List<Location> ToAdd = new ArrayList<>();
		for(Location tloc : NearBlocks){
			if(tloc.distance(playerloc)>2){
				Location temploc = tloc.clone();
				temploc.add(GeneralMethods.getDirection(temploc, playerloc.clone().add(0,1,0)).normalize().multiply(speed));
				TempBlock tb1 = new TempBlock(temploc.getBlock(), mat);
				tb1.setRevertTime(100);
				ToAdd.add(temploc);
			}}
		return ToAdd;
		/*if (WaterAbility == true) {
			if (loc.getBlock().getBlockData().getMaterial() == Material.GRASS_BLOCK) {
				for (Block b : GeneralMethods.getBlocksAroundPoint(loc, DryRange)) {
					if (b.getBlockData().getMaterial() == Material.GRASS_BLOCK) {
						TempBlock tb1 = new TempBlock(b, Material.PODZOL);
						tb1.setRevertTime(DryRevert);
					}

				}
			} else {
				TempBlock tb1 = new TempBlock(loc.getBlock(), Material.AIR);
				tb1.setRevertTime(5000);
			}} else {*/

		//}
	}

    private static void addFrozenBlock(TempBlock tempBlock) {
        PhaseChange.getFrozenBlocksMap().put(tempBlock, null);
    }
	
	public static void stream(Location location, Vector dir, Player p, Ability abi, Material mat, int range, int damage) {
		Location loc = location.clone();
		Location originloc = p.getLocation().clone();
		do {
			if (loc.distance(originloc) > range) {
				return;
			}
			if (loc.getBlock().getType().isSolid()) {
				return;
			}
		double i = (loc.distance(originloc)/5);
		loc.add(dir.multiply(1));
		//if (location.distance(p.getLocation()) < 9) {
			ParticleEffect.BLOCK_CRACK.display(loc, 2, i, i, i, 0, mat.createBlockData());
		//}// else {
		//	ParticleEffect.SMOKE_NORMAL.display(location, 5, 2, 2, 2, 0);
		//}
   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1+i)) {
			if ((entity instanceof LivingEntity) && entity.getUniqueId() != p.getUniqueId()) {
				DamageHandler.damageEntity(entity, damage, abi);
				if (SandBreath.push == true) {
					
					((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, SandBreath.DeBuffsDuration , SandBreath.DeBuffsPower , false , false));
					((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SandBreath.DeBuffsDuration, SandBreath.DeBuffsPower , false , false));
            } }}
		} while (loc.distance(originloc) < range || !loc.getBlock().getType().isSolid());
		return;
	}
	
	public static void expansion(Location location, Player p,int times, int range, long duration, int sec, Material mat, boolean bendable, boolean waterbendalble) {
		Location loc = location.clone();
		int i = 0;
		int timeout = 0;
		do {
		int timetofull = times*sec;
		int multi = i;
		int time = timeout;
		new BukkitRunnable() {
	        public void run() {
	        	for (Block blocks : GeneralMethods.getBlocksAroundPoint(loc.clone().subtract(0,1,0), range*multi)) {
	        		if (blocks.getLocation().getY() == loc.clone().subtract(0,1,0).getY()) {
	        			if (EarthAbility.isEarthbendable(p,blocks)) {
			        		TempBlock tb1 = new TempBlock(blocks, mat);
			   	    		tb1.setRevertTime((((timetofull/20)*1000)+ duration)-((time/20)*1000));
			   	    		if (bendable == true) {
			   	    			addFrozenBlock(tb1);
			   	    		}
	        			} else if (WaterAbility.isWaterbendable(blocks.getType()) && waterbendalble == true) {
			        		TempBlock tb1 = new TempBlock(blocks, mat);
			   	    		tb1.setRevertTime((((timetofull/20)*1000)+ duration)-((time/20)*1000));
			   	    		if (bendable == true) {
			   	    			addFrozenBlock(tb1);
			   	    		}
	        			}
	        		}
	        	}
	        
	        }}.runTaskLater(ProjectKorra.plugin,timeout);
		timeout = timeout+sec;
		i = i+1;
		}while (i < times);
	}

    public static void displayLineBetweenPoints(Location source, Location destination, int particleAmount, Material mat, int duration) {
    	int i = 0;
    	int time = 0;
		do {
		new BukkitRunnable() {
	        public void run() {
	        	
	            if (!source.getWorld().equals(destination.getWorld()))
	                return;
	            double distance = source.clone().add(0,1,0).distance(destination);
	            Vector direction = destination.clone().add(0,1,0).toVector().subtract(source.clone().add(0,1,0).toVector()).normalize().multiply(distance / particleAmount);
	            Location particleLocation = source.clone().add(0,1,0);
	            for (int i = 0; i < particleAmount; i++) {
	            	
	            	ParticleEffect.BLOCK_CRACK.display(particleLocation, 1, 0, 0, 0, 0.1, mat.createBlockData());
	            	//player.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, new Particle.DustOptions(Color.fromRGB(50, 50, 50), 1));
	            	
	    				//ParticleEffect.CRIT.display(particleLocation, 3, 0.1, 0.1, 0.1, 0);
	                particleLocation.add(direction);
	            }
	        }}.runTaskLater(ProjectKorra.plugin,time);
	        time = time + 2;
		i = i+1;
		}while (i < duration);
    }

	public static List<Block> getBlocksInRadius(Location center, int radius) {
		List<Block> blocks = new ArrayList<>();
		World world = center.getWorld();
		if (world == null) {
			return blocks;
		}
		int minX = center.getBlockX() - radius;
		int minY = center.getBlockY() - radius;
		int minZ = center.getBlockZ() - radius;
		int maxX = center.getBlockX() + radius;
		int maxY = center.getBlockY() + radius;
		int maxZ = center.getBlockZ() + radius;
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = world.getBlockAt(x, y, z);
					blocks.add(block);
				}
			}
		}

		return blocks;
	}


	
}