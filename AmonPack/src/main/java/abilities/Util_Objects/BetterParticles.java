package abilities.Util_Objects;

import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;

public class BetterParticles {
    private ParticleEffect effect;
    private int amount;
    private double range;
    private double Yrange;
    private double speed;
    private Color color;

    public BetterParticles(int amount, ParticleEffect effect, double range, double speed, double yrange) {
        this(amount, effect, range, speed, yrange, null);
    }

    public BetterParticles(int amount, ParticleEffect effect, double range, double speed, double yrange, Color color) {
        this.amount = amount;
        this.effect = effect;
        this.range = range;
        this.speed = speed;
        this.Yrange = yrange;
        this.color = color;
    }

    public void Display(Location location) {
        if (effect == ParticleEffect.SPELL_MOB ||effect == ParticleEffect.REDSTONE || effect == ParticleEffect.SPELL_MOB_AMBIENT || effect == ParticleEffect.SPELL|| effect == ParticleEffect.NOTE) {
            effect.display(location, amount, range, Yrange, range, speed,Color.fromRGB(192,192,192));
        }else{
            effect.display(location, amount, range, Yrange, range, speed);
        }
    }
    public void DisplayDustOption(Location location) {
        if(color!=null){
            DustOptions dust = new DustOptions(Color.fromRGB(192,192,192), 1.0f);
            location.getWorld().spawnParticle(Particle.DUST, location, amount, range, Yrange, range, speed, dust);
        }else{
            effect.display(location, amount, range, Yrange, range, speed);
        }
    }
}
