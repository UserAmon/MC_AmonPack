package CustomContent.Items;

import Plugin.AmonPackPlugin;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class CustomItemListener implements Listener {

    private final CustomItemManager itemManager;
    private final Random random = new Random();

    public CustomItemListener(CustomItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        if (matrix == null || matrix.length < 9) return;

        // Sprawdź czy 3 papier na górze i 6 bruku na dole
        boolean isPaperTop = true;
        for (int i = 0; i < 3; i++) {
            if (matrix[i] == null || matrix[i].getType() != Material.PAPER) {
                isPaperTop = false;
                break;
            }
        }
        boolean isCobbleBottom = true;
        for (int i = 3; i < 9; i++) {
            if (matrix[i] == null || matrix[i].getType() != Material.COBBLESTONE) {
                isCobbleBottom = false;
                break;
            }
        }

        if (isPaperTop && isCobbleBottom) {
            ItemStack result = itemManager.createItemStack("magic_crafting_table");
            if (result == null) {
                result = itemManager.createDefaultMagicCraftingTable();
            }
            inv.setResult(result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        String customId = itemManager.getCustomItemId(hand);
        if (customId == null) return;

        CustomItem item = itemManager.getCustomItem(customId);
        if (item == null) return;

        // Obsługa efektów on-hit
        String effectType = item.getOnHitEffectType();
        if (effectType != null && !effectType.isEmpty()) {
            double chance = item.getEffectChance();
            if (chance <= 0 || random.nextDouble() <= chance) {
                applyOnHitEffect(player, victim, effectType, item.getEffectDuration(), item.getEffectValue());
            }
        }
    }

    private void applyOnHitEffect(Player attacker, LivingEntity victim, String effectType, int durationSeconds, double value) {
        switch (effectType.toUpperCase()) {
            case "BLEED":
                attacker.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
                new BukkitRunnable() {
                    int ticks = 0;
                    final int maxTicks = durationSeconds * 20;

                    @Override
                    public void run() {
                        ticks += 10;
                        if (ticks > maxTicks || victim.isDead() || !victim.isValid()) {
                            cancel();
                            return;
                        }
                        double dmg = value > 0 ? value : 1.5;
                        victim.damage(dmg, attacker);
                        Particle.DustOptions blood = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f);
                        victim.getWorld().spawnParticle(Particle.DUST, victim.getEyeLocation(), 6, 0.2, 0.2, 0.2, 0, blood);
                    }
                }.runTaskTimer(AmonPackPlugin.plugin, 10L, 10L);
                break;

            case "LIFESTEAL":
                double healAmount = value > 0 ? value : 2.0;
                double newHealth = Math.min(attacker.getMaxHealth(), attacker.getHealth() + healAmount);
                attacker.setHealth(newHealth);
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
                attacker.getWorld().spawnParticle(Particle.HEART, attacker.getEyeLocation(), 3, 0.3, 0.3, 0.3, 0.1);
                break;

            case "METEOR_SMASH":
                victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.1);
                victim.getWorld().spawnParticle(Particle.LAVA, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                victim.setFireTicks(60);
                break;
        }
    }
}
