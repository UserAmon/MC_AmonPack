package abilities;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;

import methods_plugins.AmonPackPlugin;
@SuppressWarnings("deprecation")
public class MetalFlex extends MetalAbility implements AddonAbility {
	protected static final Ability MetalFlex = null;
	private long cooldown1 = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.MetalFlex.CooldownNormal");
	private long cooldown2 = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.MetalFlex.CooldownCrysis");
	private int speedpower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.MetalFlex.SpeedPower");
	private int crysisduration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.MetalFlex.CrysisDuration");
	private int low = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.MetalFlex.LowLevel");
	public MetalFlex(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (player.getInventory().getChestplate() == null)  {
            end();
			return;
		}else if (!(player.getInventory().getChestplate().getType() == Material.IRON_CHESTPLATE))  {
			remove();
			return;
		}else if (player.getInventory().getChestplate().getDurability() > 240)  {			
			remove();
			return;
	    } else
        	player.setLastDamageCause(null);
			start();
	}
	@Override
	public void progress() {
		if (player.getInventory().getChestplate() == null)  {
            this.bPlayer.addCooldown(this);
            end();
			return;
		}else
		if (!(player.getInventory().getChestplate().getType() == Material.IRON_CHESTPLATE))  {
            this.bPlayer.addCooldown(this);
            end();
			return;
		}else
		if (player.getInventory().getChestplate().getDurability() > 240)  {			
            this.bPlayer.addCooldown(this);
            end();
			return;
    	}else
    	if (player.getInventory().getChestplate().getDurability() == 0)  {			
            this.bPlayer.addCooldown(this);
            end();
    		return;
        }else
		if (player.isDead() || !player.isOnline()) {
            end();
			return;
		}else {
		
        	if (player.isOnGround()) {	 
        		if (!player.hasPotionEffect(PotionEffectType.SPEED)){
        			this.player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, speedpower, true));
        		} else if (player.hasPotionEffect(PotionEffectType.SPEED)){
                    Collection<PotionEffect> pe = player.getActivePotionEffects();
                    for(PotionEffect effect : pe) {
                        if(effect.getType().equals(PotionEffectType.SPEED)) {
                            if(effect.getAmplifier() <= speedpower) {
                            	this.player.removePotionEffect(PotionEffectType.SPEED);
                            	this.player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, speedpower, true));
                            }}}}}
        	            if (!this.player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
        	            	if (player.getHealth() < low) {
        	                this.player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, crysisduration, 20, true));
        	                this.player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, crysisduration, 20, true));
        	                this.player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, crysisduration, 1, true));
        	                crysis();
        	            }}else{
        	            end(); 
        	            }}}
    public void end() {
        this.bPlayer.addCooldown(this, cooldown1);
        super.remove();
        if (this.player != null) {
            this.player.removePotionEffect(PotionEffectType.SPEED);
        }
    }
    public void crysis() {
        this.bPlayer.addCooldown(this, cooldown2);
        super.remove();
        if (this.player != null) {
            this.player.removePotionEffect(PotionEffectType.SPEED);
        }
    }
	@Override
	public long getCooldown() {
		return cooldown1;
	}
	@Override
	public String getName() {
		return "MetalFlex";
	}
	@Override
	public String getDescription() {
		return "Bend the metal around your body to gain new reinforcements - speed on the ground and immunity to damage after reducing health to a low level.You can also tap the shift key when you have 1 piece of iron in your inventory to create an iron chestplate. It also works the other way around.";
	}
	@Override
	public String getInstructions() {
		return "Left Click or Tap Shift.";
	}
	@Override
	public String getAuthor() {
		return "AmonPack";
	}
	@Override
	public String getVersion() {
		return "2.1";
	}
	@Override
	public boolean isHarmlessAbility() {
		return true;
	}
	@Override
	public boolean isSneakAbility() {
		return true;
	}
	@Override
	public void load() {}
	@Override
	public void stop() {
		super.remove();
	}
	@Override
	public Location getLocation() {
		return null;
	}
}
