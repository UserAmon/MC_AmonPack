package abilities;

import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.BetterParticles;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import methods_plugins.Abilities.SoundAbility;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class EchoJab extends SoundAbility implements AddonAbility {
	Location origin;
	private List<AbilityProjectile> Projectiles;
	private List<Entity>Hited=new ArrayList<>();
	private int Interval;
	public EchoJab(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		origin = player.getLocation().clone().add(0,0.4,0);
		Projectiles=new ArrayList<>();
		List<BetterParticles> Particles = new ArrayList<>();
		Particles.add(new BetterParticles(1,ParticleEffect.SPELL,0.15,0,0.1, Color.fromRGB(128, 128, 128)));
		Particles.add(new BetterParticles(2,ParticleEffect.SPELL_MOB_AMBIENT,0.15,0,0.1,Color.fromRGB(128, 128, 128)));
		int Offset=0;
		Interval=0;
		origin.setPitch(0);
		for (int i = 1; i < 20; i++) {
			Offset+=5;
			Location MultiProjectileL = origin.clone();
			Location MultiProjectileR = origin.clone();
			MultiProjectileL.setYaw(MultiProjectileL.getYaw()+Offset);
			MultiProjectileR.setYaw(MultiProjectileR.getYaw()-Offset);
			Vector DirL = MultiProjectileL.clone().getDirection();
			Vector DirR = MultiProjectileR.clone().getDirection();
			AbilityProjectile projectile1 = new AbilityProjectile(DirL,MultiProjectileL,origin,Particles,1);
			AbilityProjectile projectile2 = new AbilityProjectile(DirR,MultiProjectileR,origin,Particles,1);
			projectile1.setEcho(false);
			projectile2.setEcho(false);
			Projectiles.add(projectile1);
			Projectiles.add(projectile2);
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
		Interval++;
		if(Interval>1){
			Interval=0;
			List<AbilityProjectile>ToRemove=new ArrayList<>();
			for (AbilityProjectile Projectile : Projectiles) {
				Location location;
				if(Projectile.isEcho()){
					location = Projectile.Revert().clone();
				}else{
					location = Projectile.Advance().clone();
				}
				if(location.clone().getBlock().getType()!= Material.AIR && location.getBlock().getType().isSolid()){
					if(!Projectile.isEcho()){
						Projectile.setEcho(true);
					}else{
						ToRemove.add(Projectile);
					}}
				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
					if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId() && !Hited.contains(entity)) {
						HandleDamage(entity,10);
						Hited.add(entity);
						ToRemove.add(Projectile);
					}}
				if (location.distance(origin) > 12) {
					ToRemove.add(Projectile);
				}
			}
		Projectiles.removeAll(ToRemove);
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
		return "EchoJab";
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