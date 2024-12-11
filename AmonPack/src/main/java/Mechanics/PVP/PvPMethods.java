package Mechanics.PVP;

import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

public class PvPMethods {


    public static void sendTitleMessage(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
    public static Location RTP(double Radius, Location loc) {
        double xOffset = (Math.random() * Radius);
        double zOffset = (Math.random() * Radius);
        double yOffset = (Math.random() * Radius);
        double isnegative1 = (Math.random());
        if (isnegative1 >= 0.5){
            xOffset = xOffset*-1;
        }
        double isnegative2 = (Math.random());
        if (isnegative2 >= 0.5){
            zOffset = zOffset*-1;
        }
        return getBlockWithAirAbove(loc.clone().add(xOffset, (yOffset/3), zOffset)).getLocation().clone().add(0,1,0);
    }
    public static void spawnFlyingFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        FireworkEffect.Builder builder = FireworkEffect.builder();
        builder.withColor(Color.RED);
        builder.withColor(Color.GREEN);
        builder.with(FireworkEffect.Type.BURST);
        FireworkEffect effect = builder.build();
        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(3);
        firework.setFireworkMeta(fireworkMeta);
        Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, firework::remove, 120);
    }
    public static Block getBlockWithAirAbove(Location location) {
        location.add(0,40,0);
        int i =0;
        Block blockbelow;
        do {
            i++;
            blockbelow = location.clone().subtract(0, i, 0).getBlock();
        }while (blockbelow.getType() == Material.AIR || blockbelow.getType() == Material.WATER);
        return blockbelow;
    }
}
