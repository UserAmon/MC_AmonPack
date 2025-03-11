package abilities;

import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.BetterParticles;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import methods_plugins.Abilities.SoundAbility;
import methods_plugins.Methods;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class NoisySlash extends SoundAbility implements AddonAbility {
	private Location origin;
	private List<Entity>Hited=new ArrayList<>();
	private int interval;
	private List<AbilityProjectile> Projectiles;
	private AbilityProjectile MainProjectile;
	private int offset;
	public NoisySlash(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		interval=0;
		offset=20;
		origin = player.getLocation().clone();
		Location selorigin = Methods.getTargetLocation(player,8).clone();
		Projectiles=new ArrayList<>();
		List<BetterParticles> Particles = new ArrayList<>();
		List<BetterParticles> ParticlesMain = new ArrayList<>();
		Particles.add(new BetterParticles(1,ParticleEffect.SPELL,0.15,0.01,0.1));
		Particles.add(new BetterParticles(3,ParticleEffect.SPELL_MOB_AMBIENT,0.25,0.01,0.1));
		ParticlesMain.add(new BetterParticles(2,ParticleEffect.SPELL,0.25,0.05,0.1));
		ParticlesMain.add(new BetterParticles(1,ParticleEffect.NOTE,0.25,0.01,0.1));
		ParticlesMain.add(new BetterParticles(6,ParticleEffect.SPELL_MOB_AMBIENT,0.25,0.1,0.1));
		Vector direction = selorigin.toVector().subtract(player.getLocation().toVector()).normalize();
		MainProjectile=new AbilityProjectile(direction,player.getLocation().clone(), origin,ParticlesMain,1);
		selorigin.setPitch(0);
		int Offset=0;
		for (int i = 1; i < 15; i++) {
			Offset+=24;
			Location MultiProjectileL = selorigin.clone();
			Location MultiProjectileR = selorigin.clone();
			MultiProjectileL.setYaw(MultiProjectileL.getYaw()+Offset);
			MultiProjectileR.setYaw(MultiProjectileR.getYaw()-Offset);
			Vector DirL = MultiProjectileL.clone().getDirection();
			Vector DirR = MultiProjectileR.clone().getDirection();
			Projectiles.add(new AbilityProjectile(DirL,MultiProjectileL, selorigin,Particles,1));
			Projectiles.add(new AbilityProjectile(DirR,MultiProjectileR, selorigin,Particles,1));
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
			if(MainProjectile.getLocation().distance(origin)>8){
				List<AbilityProjectile>ToRemove=new ArrayList<>();
				for (AbilityProjectile Projectile : Projectiles) {
					Location location = Projectile.Advance().clone();
					for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
						if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()&&!Hited.contains(entity)) {
							HandleDamage(entity,10);
							Hited.add(entity);
							ToRemove.add(Projectile);
						}}
					if (location.distance(Projectile.getOrigin()) > 5 || !location.clone().add(0,0.8,0).getBlock().getType().isAir() || location.clone().add(0,0.8,0).getBlock().getType().isSolid()) {
						ToRemove.add(Projectile);
					}}
				Projectiles.removeAll(ToRemove);
			}else{
				MainProjectile.Advance();
			}
			}
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
		return "NoisySlash";
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