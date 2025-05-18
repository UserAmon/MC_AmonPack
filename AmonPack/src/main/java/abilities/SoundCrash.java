package abilities;

import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.BetterParticles;
import abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import methods_plugins.Abilities.SmokeAbility;
import methods_plugins.Abilities.SoundAbility;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class SoundCrash extends SoundAbility implements AddonAbility {
	Location origin;
	private List<AbilityProjectile> Projectiles;
	private List<Entity>Hited=new ArrayList<>();
	private int interval;
	public SoundCrash(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		origin = player.getLocation().clone().add(0,0.6,0);
		Projectiles=new ArrayList<>();
		interval=0;
		List<BetterParticles> Particles = new ArrayList<>();
		Particles.add(new BetterParticles(1,ParticleEffect.SPELL,0.05,0.01,0.15,Color.fromRGB(128, 128, 128)));
		Particles.add(new BetterParticles(4,ParticleEffect.SPELL_MOB_AMBIENT,0.15,0.01,0.15, Color.fromRGB(128, 128, 128)));
		Location temploc = origin.clone();
		temploc.setPitch(15);
		int Offset=0;
		for (int i = 1; i < 4; i++) {
			Offset+=4;
			Location MultiProjectileL = origin.clone();
			Location MultiProjectileR = origin.clone();
			MultiProjectileL.setYaw(MultiProjectileL.getYaw()+Offset);
			MultiProjectileL.setPitch(MultiProjectileL.getPitch()-(Offset+2));
			MultiProjectileR.setYaw(MultiProjectileR.getYaw()-Offset);
			MultiProjectileR.setPitch(MultiProjectileR.getPitch()- (Offset+2));
			Vector DirL = MultiProjectileL.clone().getDirection();
			Vector DirR = MultiProjectileR.clone().getDirection();
			Projectiles.add(new AbilityProjectile(DirL,MultiProjectileL,origin,Particles,1));
			Projectiles.add(new AbilityProjectile(DirR,MultiProjectileR,origin,Particles,1));
		}
		bPlayer.addCooldown(this);
		start();
	}
	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		interval++;
		if(interval>1){
			interval=0;
			for (AbilityProjectile Projectile : Projectiles) {
				Location location = Projectile.Advance().clone();
				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
					if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId() && !Hited.contains(entity)) {
						HandleDamage(entity,10);
						Hited.add(entity);
						Projectiles.remove(Projectile);
						return;
					}
				}
				if (location.distance(origin) > 20 || !location.clone().add(0,0.3,0).getBlock().getType().isAir() || location.clone().add(0,0.3,0).getBlock().getType().isSolid()) {
					Projectiles.remove(Projectile);
					return;
				}
			}}
		if(Projectiles.isEmpty()){
			remove();
		}
	}

	@Override
	public long getCooldown() {
		return 3000;
	}
	@Override
	public Location getLocation() {
		return null;
	}
	@Override
	public String getName() {
		return "SoundCrash";
	}
	@Override
	public String getDescription() {
		return "";
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
		return false;
	}
	@Override
	public void load() {}
	@Override
	public void stop() {
		super.remove();
	}
}