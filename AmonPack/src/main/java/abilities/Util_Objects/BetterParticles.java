package abilities.Util_Objects;

import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;

public class BetterParticles {
    private ParticleEffect effect;
    private int amount;
    private double range;
    private double Yrange;
    private double speed;

    public BetterParticles(int amount, ParticleEffect effect, double range, double speed, double yrange) {
        this.amount = amount;
        this.effect = effect;
        this.range = range;
        this.speed = speed;
        Yrange = yrange;
    }
    public void Display(Location location){
        effect.display(location, amount, range, Yrange, range, speed);
    }
}
