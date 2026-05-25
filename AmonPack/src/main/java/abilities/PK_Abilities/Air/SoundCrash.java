package Abilities.PK_Abilities.Air;

import Abilities.Util_Objects.AbilityProjectile;
import Abilities.Util_Objects.BetterParticles;
import Abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import Abilities.Bending.SmokeAbility;
import Abilities.Bending.SoundAbility;
import Plugin.AmonPackPlugin;
import Plugin.Methods;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
						applySoundCrashEffect((LivingEntity) entity);
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

	private void applySoundCrashEffect(LivingEntity target) {
		double S = 0.0;
		if (AfffectedEntities.containsKey(target)) {
			S = AfffectedEntities.get(target);
		}

		if (S < 10.0) {
			HandleDamage(target, 5.0);
			Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector());
			if (push.lengthSquared() > 0) {
				push.normalize().multiply(0.8).setY(0.2);
				target.setVelocity(push);
			}
		} else {
			HandleDamage(target, 20.0);
			for (Entity near : GeneralMethods.getEntitiesAroundPoint(target.getLocation(), 4.5)) {
				if (near instanceof LivingEntity && near.getUniqueId() != player.getUniqueId()) {
					Vector shock = near.getLocation().toVector().subtract(target.getLocation().toVector());
					if (shock.lengthSquared() > 0) {
						shock.normalize().multiply(1.2).setY(0.35);
						near.setVelocity(shock);
					} else {
						Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector());
						if (push.lengthSquared() > 0) {
							push.normalize().multiply(1.2).setY(0.35);
							target.setVelocity(push);
						}
					}
				}
			}
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

	public SoundCrash(Player player, Entity victim, int use) {
		super(player);
		switch (use){
			case 0:
				if (!bPlayer.isOnCooldown("Major_Sound_OnHit")) {
					if (victim instanceof LivingEntity) {
						applySoundCrashEffect((LivingEntity) victim);
					} else {
						HandleDamage(victim, 10);
					}
					bPlayer.addCooldown("Major_Sound_OnHit",5000);
					break;
				}
		}}

	@Override
	public String getDescription() {
		return "Unleashes a powerful sonic boom. Targets below 10 sound stacks receive +5 stacks and are pushed away. Targets with 10 or more stacks instantly detonate, releasing a powerful shockwave.";
	}

	@Override
	public String getInstructions() {
		return "Left-click to cause a sound crash.";
	}
}