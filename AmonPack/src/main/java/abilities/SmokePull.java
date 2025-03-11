package abilities;

import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.ability.AddonAbility;
import methods_plugins.Abilities.SmokeAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SmokePull extends SmokeAbility implements AddonAbility {
	/*@Attribute("Cooldown")
	private long Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.Cooldown");
	@Attribute("Damage")
	private int dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.Damage");
	@Attribute("ChargeTime")
	private long TimeToCharge = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.ChargeTime");
	private final long RevertTime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.RevertTime");
	@Attribute("Range")
	private int range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.Range");
	@Attribute("Radius")
	private int radius = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.Radius");*/
	private static Map<Player, Integer> absorbedMap = new HashMap<>();
	private List<AbilityProjectile> Projectiles;
	private Location origin;
    private long interval;
	int Absorbed;
	private State AbilityState;
	private enum State {
		BENDABLE,
		CHARGING,
		READY,
		USED
	}	private SmokeSource Source;
	public SmokePull(Player player) {
		super(player);
				if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
					SmokeSource source = SmokeAbility.UseSmokeSource(player,20);
					if(source!=null){
						Source = source;
						bPlayer.addCooldown(this);
						start();
					}}
	}

	/*private Location HandLoc(){
		Location eyeLocation = player.getEyeLocation();
		Vector direction = eyeLocation.getDirection().normalize();
		Location handLocation = eyeLocation.clone().subtract(0, 0.4, 0).add(direction.multiply(0.45));
		Vector sideOffset = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.3);
		if(player.getMainHand().equals(MainHand.RIGHT)){
			handLocation.add(sideOffset);
		}else{
			handLocation.subtract(sideOffset);
		}
		return handLocation;
	}*/
	@Override
	public void progress() {
		/*if(AbilityState==State.BENDABLE){
			if (System.currentTimeMillis() > getStartTime() + getCooldown()+100) {
				if(absorbedMap.containsKey(player) && bPlayer.isOnCooldown(getName())){
					absorbedMap.remove(player);
				}
				remove();
				return;
			}
			if(!Source.isEmpty()){
				for (SmokeSource s : Source){
					interval++;
					if(interval>=2) {
						interval = 0;
						if(s.IsNearPlayer(player.getLocation(),0.75,player)){
							this.Absorbed = absorbedMap.getOrDefault(player, 0);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,40,3,false,false,false));
							Source.remove(s);
							break;
						}}}}
		}*/
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > getStartTime() + getCooldown()-100) {
			remove();
			return;
		}
		interval++;
		if(interval>=1) {
			interval = 0;
			if(Source.IsNearPlayer(player.getLocation(),0.75,player)){
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,40,3,false,false,false));
				remove();
			}}
		/*
		if(AbilityState==State.CHARGING){

			ParticleEffect.SMOKE_NORMAL.display(HandLoc(),3,0.05,0.05,0.05);
			if(!Source.isEmpty()){
			for (SmokeSource s : Source){
}}
		if (bPlayer.getBoundAbility() != null && bPlayer.getBoundAbilityName() != null && bPlayer.getBoundAbilityName().equalsIgnoreCase(getName())) {
			if(player.isSneaking()){
				origin = player.getLocation().clone();
				List<BetterParticles> Particles = new ArrayList<>();
				Particles.add(new BetterParticles(7,ParticleEffect.SMOKE_NORMAL,0.4,0.02,0.1));
				int Offset = 0;
				for (int i = 1; i < 2+(absorbedMap.getOrDefault(player, 0)*2); i++) {
					Offset+=4;
					Location MultiProjectileL = player.getLocation().clone().add(0,1,0);
					Location MultiProjectileR = player.getLocation().clone().add(0,1,0);
					MultiProjectileL.setYaw(MultiProjectileL.getYaw()+Offset);
					MultiProjectileR.setYaw(MultiProjectileR.getYaw()-Offset);
					Vector DirL = MultiProjectileL.clone().getDirection();
					Vector DirR = MultiProjectileR.clone().getDirection();
					Projectiles.add(new AbilityProjectile(DirL,MultiProjectileL,origin,Particles,1));
					Projectiles.add(new AbilityProjectile(DirR,MultiProjectileR,origin,Particles,1));
				}
				AbilityState=State.USED;
				bPlayer.addCooldown(this);
				absorbedMap.remove(player);
			}}}
		if(AbilityState==State.USED){
			interval++;
			if(interval>1){
				interval=0;
			for (AbilityProjectile Projectile : Projectiles) {
				Location location = Projectile.Advance().clone();
				if (location.distance(origin) > 30 || !location.getBlock().getType().isAir() || location.getBlock().getType().isSolid()) {
					remove();
					return;
				}else{
					for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
						if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
							DamageHandler.damageEntity(entity, 1, this);
						}
					}
				}
			}
		}
		}
	*/
	}

	@Override
	public long getCooldown() {
		return 5000;
	}
	@Override
	public Location getLocation() {
		return null;
	}
	@Override
	public String getName() {
		return "SmokePull";
	}
	@Override
	public String getDescription() {
		return "";
	}
	@Override
	public String getInstructions() {
		return "Click on Smoke Source to pull it";
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