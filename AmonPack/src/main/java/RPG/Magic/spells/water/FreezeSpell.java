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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class FreezeSpell extends Spell {

    public FreezeSpell() {
        super("freeze", "§b§lFreeze", SpellElement.WATER, 35, 6.0);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "freeze_mana") || MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "freeze_cd") || MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");

        int effectiveMana = hasManaRed ? Math.max(15, getManaCost() - 10) : getManaCost();
        double effectiveCd = hasCdRed ? Math.max(2.5, getCooldownSeconds() - 1.5) : getCooldownSeconds();

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
        if (centerBlock.getType() != Material.WATER && centerBlock.getType() != Material.ICE) {
            sendActionBar(player, "§c✦ Wyceluj w taflę wody!");
            return false;
        }

        if (!manaManager.hasMana(player, effectiveMana)) {
            sendNoManaActionBar(player, effectiveMana, manaManager);
            return false;
        }

        manaManager.consumeMana(player, effectiveMana);
        setCooldown(player, (long) (effectiveCd * 1000));

        Location center = centerBlock.getLocation();
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.5f);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 1.0f);

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

        // Animacja zamrażania na przestrzeni 2 sekund (40 ticków)
        final Map<Block, Material> frozenBlocks = new HashMap<>();
        new BukkitRunnable() {
            int index = 0;
            int stepSize = Math.max(1, waterBlocks.size() / 15);

            @Override
            public void run() {
                int count = 0;
                while (index < waterBlocks.size() && count < stepSize) {
                    Block b = waterBlocks.get(index++);
                    if (b.getType() == Material.WATER) {
                        frozenBlocks.put(b, Material.WATER);
                        b.setType(Material.FROSTED_ICE);
                        b.getWorld().spawnParticle(Particle.SNOWFLAKE, b.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.02);
                        if (index % 4 == 0) {
                            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 1.8f);
                        }
                    }
                    count++;
                }

                // Zamrożenie i spowolnienie mobów
                for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 5.0, 3.0, 5.0)) {
                    if (e instanceof LivingEntity le && !e.equals(player)) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                        le.setFreezeTicks(100);
                        ElementStatusManager.applyElementStatus(le, SpellElement.WATER, 6.0);
                    }
                }

                if (index >= waterBlocks.size()) {
                    cancel();

                    // Po 5 sekundach: roztrzaskanie lodu i zadanie obrażeń
                    BukkitRunnable shatterTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
                            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);
                            center.getWorld().spawnParticle(Particle.BLOCK, center.clone().add(0, 0.5, 0), 150, 2.5, 0.5, 2.5, 0.1, Material.ICE.createBlockData());

                            for (Map.Entry<Block, Material> entry : frozenBlocks.entrySet()) {
                                Block b = entry.getKey();
                                if (b.getType() == Material.FROSTED_ICE || b.getType() == Material.ICE) {
                                    b.setType(Material.WATER);
                                }
                            }

                            for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 5.5, 3.0, 5.5)) {
                                if (e instanceof LivingEntity le && !e.equals(player)) {
                                    ElementStatusManager.triggerDamageAndReaction(player, le, getBaseDamage(), SpellElement.WATER, tomeItem);
                                    le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                                }
                            }
                        }
                    };
                    shatterTask.runTaskLater(AmonPackPlugin.plugin, 100L); // 5 sekund (100 ticks)
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);

        return true;
    }
}
