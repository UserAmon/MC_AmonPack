package UtilObjects.PVE;

import org.bukkit.Color;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

import static Mechanics.Skills.UpgradesMenager.MoonBow;

public class BowAbility {
    private static final Map<Player, Integer> playerShots = new HashMap<>();
    public BowAbility(EntityShootBowEvent event) {
        Player player = (Player) event.getEntity();
        if (event.getEntity() instanceof Player && event.getBow() != null) {
        if (event.getBow().isSimilar(MoonBow) && event.getProjectile() instanceof Arrow) {
            event.setConsumeItem(false);
            float force = event.getForce();
            event.setCancelled(true);
            if (force > 0.70) {
                Vector direction = player.getLocation().getDirection();
                Vector directionLeft = direction.clone().rotateAroundY(Math.toRadians(-10));
                Vector directionRight = direction.clone().rotateAroundY(Math.toRadians(10));
                ShootArrow(direction,player);
                ShootArrow(directionLeft,player);
                ShootArrow(directionRight,player);
            }
        }
        }}
    public BowAbility(Player player, AbilityType type) {
        if (type == AbilityType.MoonBow) {
            int shots = playerShots.getOrDefault(player, 0) + 1;
            playerShots.put(player, shots);
            if (shots >= 3) {
                playerShots.remove(player);
                ShootArrow(player.getLocation().getDirection(),player);
            }}
    }

    public void ShootArrow(Vector direction,Player player) {
        Arrow arrow = player.getWorld().spawn(player.getLocation().add(0,1,0), Arrow.class);
        arrow.setCustomName("MoonArrow");
        arrow.setShooter(player);
        arrow.setCritical(true);
        arrow.setKnockbackStrength(1);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.setVelocity(direction.multiply(3));
        arrow.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 40, 1, false, false), true);
        arrow.addCustomEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4, false, false), true);
        arrow.setColor(Color.PURPLE);
    }

    public enum AbilityType {
        MoonBow
    }
    public int GetStacks(Player p){
        if(playerShots.get(p)!=null){
            return playerShots.get(p);
        }else{
            return 0;
        }
    }
}
