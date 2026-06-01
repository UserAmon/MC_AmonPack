package Abilities.PK_Abilities.Earth;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

import Plugin.AmonPackPlugin;

public class FerroClips extends MetalAbility implements AddonAbility {

    private long cooldown;
    private double range;
    private double speed;
    private int slot;

    private int shotsFired;
    private int hitsSucceeded;
    private long startTime;

    private LivingEntity targetEnemy;
    private boolean isPulling;

    private ItemStack origHelmet;
    private ItemStack origChestplate;
    private ItemStack origLeggings;
    private ItemStack origBoots;

    private List<Location> projLocs;
    private List<Vector> projDirs;
    private List<Double> projDists;

    public FerroClips(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        if (!bPlayer.canBend(this)) {
            return;
        }

        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || chest.getType() != Material.IRON_CHESTPLATE) {
            return;
        }

        this.cooldown = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.FerroClips.Cooldown", 8000);
        this.range = 20.0;
        this.speed = 1.2;
        this.slot = player.getInventory().getHeldItemSlot();

        this.shotsFired = 0;
        this.hitsSucceeded = 0;
        this.startTime = 0;
        this.targetEnemy = null;
        this.isPulling = false;

        this.projLocs = new ArrayList<>();
        this.projDirs = new ArrayList<>();
        this.projDists = new ArrayList<>();

        onClick();
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            restoreTargetArmor();
            remove();
            return;
        }

        if (player.getInventory().getHeldItemSlot() != slot) {
            restoreTargetArmor();
            remove();
            bPlayer.addCooldown(this);
            return;
        }

        if (shotsFired > 0) {
            if (System.currentTimeMillis() - startTime > 8000) {
                restoreTargetArmor();
                remove();
                bPlayer.addCooldown(this);
                return;
            }

            for (int i = projLocs.size() - 1; i >= 0; i--) {
                Location loc = projLocs.get(i);
                Vector dir = projDirs.get(i);
                double dist = projDists.get(i);

                loc.add(dir.clone().multiply(speed));
                dist += speed;
                projDists.set(i, dist);

                loc.getWorld().spawnParticle(Particle.ITEM, loc, 3, 0.15, 0.15, 0.15, 0.02, new ItemStack(Material.IRON_INGOT));

                Block b = loc.getBlock();
                if (b.getType().isSolid() && b.getType() != Material.WATER && b.getType() != Material.LAVA) {
                    projLocs.remove(i);
                    projDirs.remove(i);
                    projDists.remove(i);
                    continue;
                }

                boolean hit = false;
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.2)) {
                    if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                        LivingEntity target = (LivingEntity) entity;
                        onHit(target);
                        projLocs.remove(i);
                        projDirs.remove(i);
                        projDists.remove(i);
                        hit = true;
                        break;
                    }
                }

                if (hit) {
                    continue;
                }

                if (dist > range) {
                    projLocs.remove(i);
                    projDirs.remove(i);
                    projDists.remove(i);
                }
            }

            if (shotsFired == 4 && projLocs.isEmpty() && !isPulling) {
                if (hitsSucceeded == 4) {
                    restoreTargetArmor();
                    remove();
                    return;
                } else {
                    restoreTargetArmor();
                    remove();
                    bPlayer.addCooldown(this);
                    return;
                }
            }
        }

        if (targetEnemy != null) {
            if (targetEnemy.isDead() || !targetEnemy.isValid() || player.getLocation().distance(targetEnemy.getLocation()) > 25.0) {
                restoreTargetArmor();
                remove();
                bPlayer.addCooldown(this);
                return;
            }

            if (player.isSneaking()) {
                isPulling = true;
                double force = hitsSucceeded * 0.08;
                Vector toPlayer = player.getLocation().toVector().subtract(targetEnemy.getLocation().toVector());
                double dist = toPlayer.length();

                if (dist <= 2.0) {
                    DamageHandler.damageEntity(targetEnemy, 12.0, this);
                    targetEnemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10));
                    targetEnemy.getWorld().playSound(targetEnemy.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.2f, 1.0f);
                    restoreTargetArmor();
                    remove();
                    bPlayer.addCooldown(this);
                    return;
                } else {
                    Vector vel = targetEnemy.getVelocity();
                    vel.add(toPlayer.normalize().multiply(force));
                    vel.multiply(0.90);
                    if (vel.getY() < 0) {
                        vel.setY(vel.getY() * 0.90 + 0.04);
                    }
                    targetEnemy.setVelocity(vel);
                    player.getWorld().spawnParticle(Particle.DUST, targetEnemy.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0, new Particle.DustOptions(Color.fromRGB(180, 180, 180), 0.8f));
                }
            }
        }
    }

    public void onClick() {
        if (shotsFired >= 4 || isPulling) {
            return;
        }

        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || chest.getType() != Material.IRON_CHESTPLATE) {
            return;
        }

        org.bukkit.inventory.meta.Damageable dmgMeta = (org.bukkit.inventory.meta.Damageable) chest.getItemMeta();
        int maxDurability = Material.IRON_CHESTPLATE.getMaxDurability();
        int loss = (int) (maxDurability * 0.05);
        int newDmg = dmgMeta.getDamage() + loss;
        if (newDmg >= maxDurability) {
            player.getInventory().setChestplate(null);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.2f, 1.0f);
            restoreTargetArmor();
            remove();
            bPlayer.addCooldown(this);
            return;
        } else {
            dmgMeta.setDamage(newDmg);
            chest.setItemMeta(dmgMeta);
        }

        if (shotsFired == 0) {
            startTime = System.currentTimeMillis();
        }

        shotsFired++;

        projLocs.add(player.getEyeLocation());
        projDirs.add(player.getEyeLocation().getDirection().normalize());
        projDists.add(0.0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);
    }

    private void onHit(LivingEntity target) {
        if (targetEnemy == null) {
            targetEnemy = target;
        }

        if (target == targetEnemy) {
            hitsSucceeded++;
            targetEnemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));

            ItemStack item = new ItemStack(Material.AIR);
            if (hitsSucceeded == 1) {
                origBoots = targetEnemy.getEquipment().getBoots();
                item = new ItemStack(Material.IRON_BOOTS);
            } else if (hitsSucceeded == 2) {
                origLeggings = targetEnemy.getEquipment().getLeggings();
                item = new ItemStack(Material.IRON_LEGGINGS);
            } else if (hitsSucceeded == 3) {
                origChestplate = targetEnemy.getEquipment().getChestplate();
                item = new ItemStack(Material.IRON_CHESTPLATE);
            } else if (hitsSucceeded == 4) {
                origHelmet = targetEnemy.getEquipment().getHelmet();
                item = new ItemStack(Material.IRON_HELMET);
                targetEnemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
            }

            if (item.getType() != Material.AIR) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "Ferro-Clip Plate");
                item.setItemMeta(meta);

                if (hitsSucceeded == 1) {
                    targetEnemy.getEquipment().setBoots(item);
                } else if (hitsSucceeded == 2) {
                    targetEnemy.getEquipment().setLeggings(item);
                } else if (hitsSucceeded == 3) {
                    targetEnemy.getEquipment().setChestplate(item);
                } else if (hitsSucceeded == 4) {
                    targetEnemy.getEquipment().setHelmet(item);
                }
            }

            targetEnemy.getWorld().playSound(targetEnemy.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.2f);
            targetEnemy.getWorld().spawnParticle(Particle.FLAME, targetEnemy.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
        }
    }

    public void restoreTargetArmor() {
        if (targetEnemy != null && !targetEnemy.isDead()) {
            if (hitsSucceeded >= 1) {
                targetEnemy.getEquipment().setBoots(origBoots);
            }
            if (hitsSucceeded >= 2) {
                targetEnemy.getEquipment().setLeggings(origLeggings);
            }
            if (hitsSucceeded >= 3) {
                targetEnemy.getEquipment().setChestplate(origChestplate);
            }
            if (hitsSucceeded >= 4) {
                targetEnemy.getEquipment().setHelmet(origHelmet);
            }
        }
    }

    public LivingEntity getTargetEnemy() {
        return targetEnemy;
    }

    public int getHitsSucceeded() {
        return hitsSucceeded;
    }

    public ItemStack getOrigHelmet() {
        return origHelmet;
    }

    public ItemStack getOrigChestplate() {
        return origChestplate;
    }

    public ItemStack getOrigLeggings() {
        return origLeggings;
    }

    public ItemStack getOrigBoots() {
        return origBoots;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "FerroClips";
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
    public String getAuthor() {
        return "AmonPack";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        restoreTargetArmor();
        remove();
    }

    @Override
    public String getDescription() {
        return "Fires 4 iron clip projectiles over 8 seconds. Each hit equips the target with iron armor. Hold shift to pull the target with force scaling with equipped armor. No cooldown on 4 perfect hits.";
    }

    @Override
    public String getInstructions() {
        return "Wear an iron chestplate. Left-click to fire clips. Once hit, hold shift to pull the target close.";
    }
}
