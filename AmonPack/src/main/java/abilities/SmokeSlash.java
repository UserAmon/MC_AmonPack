package abilities;

import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.BetterParticles;
import abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import methods_plugins.Abilities.SmokeAbility;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class SmokeSlash extends SmokeAbility implements AddonAbility {
	//@Attribute("Cooldown")
	private long Cooldown = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBlade.Cooldown");
	//@Attribute("Damage")
	private long Damage = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBlade.Damage");
	private int SmokeDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBlade.SmokeDuration");
	//@Attribute("Range")
	private long range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBlade.Range");
	//@Attribute("Radius")
	private long Radius = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBlade.BladeSize");
	private List<AbilityProjectile> Projectiles;
	private AbilityProjectile MainOrb;
	private Location origin;
    private long interval;
	public SmokeSlash(Player player) {
		super(player);
				if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
					Projectiles=new ArrayList<>();
					origin = player.getLocation().clone();
					interval=0;
					List<BetterParticles> Particles = new ArrayList<>();
					Particles.add(new BetterParticles(4,ParticleEffect.SMOKE_NORMAL,0.15,0.02,0.1));
					int Offset = 0;
					Location defaultLoc1 = player.getLocation().clone().add(0,0.5,0);
					defaultLoc1.setYaw(defaultLoc1.getYaw()- (float) (Radius * 5) /2);
					defaultLoc1.setPitch(defaultLoc1.getPitch()+5);
					Location defaultLoc2 = player.getLocation().clone().add(0,0.5,0);
					defaultLoc2.setYaw(defaultLoc2.getYaw()+ (float) (Radius * 5) /2);
					defaultLoc2.setPitch(defaultLoc2.getPitch()+5);
					for (int i = 1; i < Radius; i++) {
						Offset+=5;
						Location MultiProjectileL = defaultLoc1.clone();
						Location MultiProjectileR = defaultLoc2.clone();
						MultiProjectileL.setYaw(MultiProjectileL.getYaw()+Offset+2.5f);
						MultiProjectileL.setPitch(MultiProjectileL.getPitch()-((float) Offset /2));
						MultiProjectileR.setYaw(MultiProjectileR.getYaw()-Offset+2.5f);
						MultiProjectileR.setPitch(MultiProjectileR.getPitch()-((float) Offset /2));
						Vector DirL = MultiProjectileL.clone().getDirection();
						Vector DirR = MultiProjectileR.clone().getDirection();
						Projectiles.add(new AbilityProjectile(DirL,MultiProjectileL,origin,Particles,1));
						Projectiles.add(new AbilityProjectile(DirR,MultiProjectileR,origin,Particles,1));
					}
					List<BetterParticles> MainParticles = new ArrayList<>();
					MainParticles.add(new BetterParticles(10,ParticleEffect.SMOKE_NORMAL,0.6,0.02,0.2));
					MainParticles.add(new BetterParticles(1,ParticleEffect.CAMPFIRE_COSY_SMOKE,0.2,0.02,0.1));
					MainOrb=new AbilityProjectile(player.getLocation().clone().getDirection(),player.getLocation().clone().add(0,1,0),origin,MainParticles,1);
					bPlayer.addCooldown(this);
					start();
				}
	}
	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
			interval++;
			if(interval>0){
				interval=0;
				if(MainOrb!=null){
				Location location = MainOrb.Advance().clone();
				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
					if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
						SmokeSource SourceEnd = new SmokeSource(location.clone().add(0,1,0), SmokeDuration, 3, 1,player);
						DamageHandler.damageEntity(entity, Damage, this);
						MainOrb=null;
						return;
					}}
				if (location.distance(origin) > range || !location.clone().add(0,0.8,0).getBlock().getType().isAir() || location.clone().add(0,0.8,0).getBlock().getType().isSolid()) {
					SmokeSource SourceEnd = new SmokeSource(location.clone().add(0,1,0), SmokeDuration, 3, 1,player);
					MainOrb=null;
					return;
				}}
				for (AbilityProjectile Projectile : Projectiles) {
					Location location = Projectile.Advance().clone();
					for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 0.5)) {
						if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
							DamageHandler.damageEntity(entity, Damage, this);
							Projectiles.remove(Projectile);
							return;
						}}
					if (location.distance(origin) > range || !location.clone().add(0,0.8,0).getBlock().getType().isAir() || location.clone().add(0,0.8,0).getBlock().getType().isSolid()) {
						Projectiles.remove(Projectile);
						return;
					}}
				if(Projectiles.isEmpty()){
					remove();
				}
		}}

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
		return "SmokeSlash";
	}
	@Override
	public String getDescription() {
		return "Send forth horizontal blade made of Smoke, upon reaching its max range, or hitting an enemy - create Smoke Source";
	}
	@Override
	public String getInstructions() {
		return "Left-Click";
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