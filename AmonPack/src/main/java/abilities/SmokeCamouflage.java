package abilities;

import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.BetterParticles;
import abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ParticleEffect;
import methods_plugins.Abilities.SmokeAbility;
import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class SmokeCamouflage extends SmokeAbility implements AddonAbility {
	//@Attribute("Cooldown")
	private long Cooldown = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeCamouflage.Cooldown");
	private double Force = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeCamouflage.DashForce");
	private double ForceY = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeCamouflage.DashForce-Y");
	//@Attribute("Duration")
	private long Duration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.Smoke.SmokeCamouflage.Duration");
	private int EffectsPower = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeCamouflage.Effects-Power");


	private State AbilityState;
	private enum State {
		BENDABLE,
		CHARGING,
		READY,
		USED
	}
    private long interval;
	private boolean UseSmokeSource;
	private SmokeSource Source;
	public SmokeCamouflage(Player player) {
		super(player);
			if (!player.isSneaking()) {
				if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
					SmokeSource source = SmokeAbility.UseSmokeSource(player,20);
					if(source!=null){
						UseSmokeSource=true;
						AbilityState = State.BENDABLE;
						Source=source;
						interval=0;
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
					if(Source.IsNearPlayer(player.getLocation(),1,player)){
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
			if (UseSmokeSource) {
				if(!player.isSneaking()){
					AbilityState = State.READY;
					Vector dir = player.getLocation().getDirection();
					dir.setY(dir.getY()+ForceY);
					player.setVelocity(dir.multiply(Force));
					bPlayer.addCooldown(this);
				}else{
					ParticleEffect.SMOKE_NORMAL.display(player.getLocation(), 5, 0.3, 0.3, 0.3, 0.1);
				}
			}}
		if (AbilityState == State.READY) {
			if (bPlayer.getBoundAbility() == null || bPlayer.getBoundAbilityName() == null || !bPlayer.getBoundAbilityName().equalsIgnoreCase(getName())) {
				remove();
				return;
			}
			interval++;
			if(interval>=Duration){
				remove();
				return;
			}
			player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,5,EffectsPower,false,false,false));
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,5,EffectsPower,false,false,false));
			ParticleEffect.CAMPFIRE_COSY_SMOKE.display(player.getLocation(),2,0.5,0.5,0.5);
			ParticleEffect.SMOKE_NORMAL.display(player.getLocation(),2,0.5,0.5,0.5);
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
		return "SmokeCamouflage";
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