package RPG.Magic.spells.water;

import Plugin.AmonPackPlugin;
import RPG.Magic.elements.ElementStatusManager;
import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.model.Spell;
import RPG.Magic.model.SpellElement;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class EvaporateSpell extends Spell {

    private final Random random = new Random();

    public EvaporateSpell() {
        super("evaporate", "§f§lEvaporate", SpellElement.WATER, 40, 8.0);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        if (isOnCooldown(player)) {
            sendCooldownActionBar(player);
            return false;
        }

        RayTraceResult ray = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 12.0, FluidCollisionMode.ALWAYS, true);
        if (ray == null || ray.getHitBlock() == null) {
            sendActionBar(player, "§c✦ Wyceluj w wodę w zasięgu 10 bloków!");
            return false;
        }

        Block centerBlock = ray.getHitBlock();
        if (centerBlock.getType() != Material.WATER) {
            sendActionBar(player, "§c✦ Wyceluj w taflę wody!");
            return false;
        }

        if (!checkAndConsumeCost(player, tomeItem, manaManager)) {
            return false;
        }

        Location center = centerBlock.getLocation();
        center.getWorld().playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, 0.7f);
        center.getWorld().playSound(center, Sound.ITEM_BUCKET_EMPTY, 1.0f, 0.6f);

        int radius = 5;
        List<Block> waterBlocks = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    for (int y = -1; y <= 1; y++) {
                        Block b = center.clone().add(x, y, z).getBlock();
                        if (b.getType() == Material.WATER) {
                            waterBlocks.add(b);
                        }
                    }
                }
            }
        }

        // Faza 1: Woda unosi się w górę na przestrzeni 3 sekund (60 ticków)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 3;
                double heightOffset = (ticks / 60.0) * 4.0; // unosi się do 4 bloków w górę

                for (Block b : waterBlocks) {
                    Location loc = b.getLocation().add(0.5, heightOffset, 0.5);
                    loc.getWorld().spawnParticle(Particle.SPLASH, loc, 2, 0.2, 0.2, 0.2, 0.05);
                    loc.getWorld().spawnParticle(Particle.FALLING_WATER, loc, 1, 0.1, 0.1, 0.1, 0.0);
                }

                if (ticks >= 60) {
                    cancel();

                    // Faza 2: Wyparowywanie w ciągu kolejnych 3 sekund
                    new BukkitRunnable() {
                        int evapTicks = 0;
                        int blocksEvaporated = 0;

                        @Override
                        public void run() {
                            evapTicks += 3;

                            for (Block b : waterBlocks) {
                                Location loc = b.getLocation().add(0.5, 4.0, 0.5);
                                loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 3, 0.3, 0.3, 0.3, 0.03);
                                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.2, 0.2, 0.2, 0.01);
                                if (random.nextInt(10) == 0) {
                                    loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.4f);
                                }
                            }

                            if (evapTicks >= 60) {
                                cancel();

                                // Usunięcie wody i aplikacja efektów
                                int totalWater = waterBlocks.size();
                                int bottlesFilled = 0;

                                // Napełnianie butelek: co 2 bloki wody napełnia 1 pustą butelkę
                                for (int i = 0; i < totalWater / 2; i++) {
                                    if (fillOneEmptyBottle(player)) {
                                        bottlesFilled++;
                                    }
                                }

                                if (bottlesFilled > 0) {
                                    sendActionBar(player, "§b✦ Napełniono §f" + bottlesFilled + " §bButelek Wody z odparowanej wody!");
                                    player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0f, 1.2f);
                                }

                                // 10% szansy na drop ryby za każdy blok
                                for (Block b : waterBlocks) {
                                    if (random.nextDouble() < 0.10) {
                                        Material fishMat = random.nextBoolean() ? Material.COD : Material.SALMON;
                                        Item fishItem = b.getWorld().dropItem(b.getLocation().add(0.5, 1.0, 0.5), new ItemStack(fishMat));
                                        fishItem.setVelocity(new org.bukkit.util.Vector((random.nextDouble() - 0.5) * 0.3, 0.4, (random.nextDouble() - 0.5) * 0.3));
                                    }
                                }
                            }
                        }
                    }.runTaskTimer(AmonPackPlugin.plugin, 0L, 3L);
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 3L);

        return true;
    }

    private boolean fillOneEmptyBottle(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && stack.getType() == Material.GLASS_BOTTLE) {
                stack.setAmount(stack.getAmount() - 1);
                if (stack.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
                ItemStack waterBottle = new ItemStack(Material.POTION);
                PotionMeta pm = (PotionMeta) waterBottle.getItemMeta();
                if (pm != null) {
                    pm.setBasePotionData(new org.bukkit.potion.PotionData(PotionType.WATER));
                    waterBottle.setItemMeta(pm);
                }
                player.getInventory().addItem(waterBottle);
                return true;
            }
        }
        return false;
    }
}
