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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;

import Plugin.AmonPackPlugin;
import Plugin.Methods;

public class FerroAbsorb extends MetalAbility implements AddonAbility {

    private long cooldown;
    private int slot;
    private double targetAngle;
    private boolean charged;
    private boolean isPlateActive;
    private ItemStack originalChestplate;
    private ItemStack ferroChestplate;
    private int ticks;

    private long lastWhipTime;
    private long lastAbsorbTime;

    private boolean whipActive;
    private int whipTicks;
    private Location whipP0;
    private Location whipP1;
    private Location whipP2;
    private List<LivingEntity> whipHitEntities;

    public FerroAbsorb(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.cooldown = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.FerroAbsorb.Cooldown", 8000);
        this.slot = player.getInventory().getHeldItemSlot();
        this.targetAngle = 90.0;
        this.charged = false;
        this.isPlateActive = false;
        this.whipActive = false;
        this.ticks = 0;
        this.lastWhipTime = 0;
        this.lastAbsorbTime = 0;

        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            cleanup();
            remove();
            return;
        }

        if (!isPlateActive && player.getInventory().getHeldItemSlot() != slot) {
            remove();
            return;
        }

        if (!isPlateActive && !charged) {
            if (!player.isSneaking()) {
                remove();
                return;
            }

            Vector look = player.getEyeLocation().getDirection().normalize();
            Location center = player.getEyeLocation().add(look.clone().multiply(1.5));

            Vector right = look.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            if (right.lengthSquared() == 0) {
                right = new Vector(1, 0, 0);
            }
            Vector up = right.clone().crossProduct(look).normalize();
            double radius = 0.6;

            for (double a = 90.0; a < 450.0; a += 15.0) {
                double r = Math.toRadians(a);
                Vector offset = right.clone().multiply(Math.cos(r) * radius).add(up.clone().multiply(Math.sin(r) * radius));
                Location pLoc = center.clone().add(offset);
                if (a < targetAngle) {
                    player.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(50, 205, 50), 0.8f));
                } else {
                    player.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(192, 192, 192), 0.5f));
                }
            }

            double rad = Math.toRadians(targetAngle);
            Vector activeOffset = right.clone().multiply(Math.cos(rad) * radius).add(up.clone().multiply(Math.sin(rad) * radius));
            Location activeLoc = center.clone().add(activeOffset);
            player.spawnParticle(Particle.DUST, activeLoc, 4, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.6f));

            Vector toActive = activeLoc.toVector().subtract(player.getEyeLocation().toVector()).normalize();
            double dot = player.getEyeLocation().getDirection().normalize().dot(toActive);
            if (dot > 0.94) {
                targetAngle += 6.0;
                if (((int) targetAngle) % 30 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.4f);
                }
            }

            int cX = center.getBlockX();
            int cY = center.getBlockY();
            int cZ = center.getBlockZ();
            for (int x = -4; x <= 4; x++) {
                for (int y = -4; y <= 4; y++) {
                    for (int z = -4; z <= 4; z++) {
                        Block b = player.getWorld().getBlockAt(cX + x, cY + y, cZ + z);
                        if (EarthAbility.isEarthbendable(player, b) && Math.random() < 0.04) {
                            Location pStart = b.getLocation().add(0.5, 0.5, 0.5);
                            Vector travel = center.toVector().subtract(pStart.toVector());
                            double d = Math.random();
                            Location pLoc = pStart.clone().add(travel.clone().multiply(d));
                            player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(139, 115, 85), 0.6f));
                        }
                    }
                }
            }

            if (targetAngle >= 450.0) {
                charged = true;
            }
        }

        if (charged && !isPlateActive) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(ChatColor.GOLD + "Release Shift to Forge Plate"));
            Vector look = player.getEyeLocation().getDirection().normalize();
            Location center = player.getEyeLocation().add(look.clone().multiply(1.5));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 3, 0.2, 0.2, 0.2, 0);

            if (!player.isSneaking()) {
                isPlateActive = true;
                originalChestplate = player.getInventory().getChestplate();

                ItemStack plate = new ItemStack(Material.IRON_CHESTPLATE);
                ItemMeta meta = plate.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "Ferro-Absorb Plate");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "A custom plate forged from earth.");
                meta.setLore(lore);
                plate.setItemMeta(meta);

                ferroChestplate = plate;
                player.getInventory().setChestplate(ferroChestplate);

                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.2f, 0.9f);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 20, 0.4, 0.4, 0.4, 0.05);
            }
        }

        if (isPlateActive) {
            ItemStack current = player.getInventory().getChestplate();
            if (current == null || current.getType() != Material.IRON_CHESTPLATE || current.getItemMeta() == null || !current.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Ferro-Absorb Plate")) {
                if (current != null && current.getType() != Material.AIR) {
                    player.getInventory().addItem(current);
                }
                player.getInventory().setChestplate(ferroChestplate);
            } else {
                ferroChestplate = current;
            }

            ticks++;
            if (ticks % 20 == 0) {
                ItemStack chest = player.getInventory().getChestplate();
                if (chest != null && chest.getType() == Material.IRON_CHESTPLATE) {
                    org.bukkit.inventory.meta.Damageable dmgMeta = (org.bukkit.inventory.meta.Damageable) chest.getItemMeta();
                    int maxDurability = Material.IRON_CHESTPLATE.getMaxDurability();
                    int loss = (int) Math.ceil(maxDurability * 0.01);
                    int newDmg = dmgMeta.getDamage() + loss;
                    if (newDmg >= maxDurability) {
                        player.getInventory().setChestplate(originalChestplate);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.2f, 1.0f);
                        remove();
                        bPlayer.addCooldown(this);
                        return;
                    } else {
                        dmgMeta.setDamage(newDmg);
                        chest.setItemMeta(dmgMeta);
                        ferroChestplate = chest;
                    }
                }
            }

            if (whipActive) {
                whipTicks++;
                double t = (double) whipTicks / 10.0;
                if (t > 1.0) {
                    whipActive = false;
                } else {
                    double oneMinusT = 1.0 - t;
                    double x = oneMinusT * oneMinusT * whipP0.getX() + 2 * oneMinusT * t * whipP1.getX() + t * t * whipP2.getX();
                    double y = oneMinusT * oneMinusT * whipP0.getY() + 2 * oneMinusT * t * whipP1.getY() + t * t * whipP2.getY();
                    double z = oneMinusT * oneMinusT * whipP0.getZ() + 2 * oneMinusT * t * whipP1.getZ() + t * t * whipP2.getZ();
                    Location currentWhipLoc = new Location(player.getWorld(), x, y, z);

                    Location hand = getHandLocation();
                    if (hand.getWorld().equals(currentWhipLoc.getWorld())) {
                        double dist = hand.distance(currentWhipLoc);
                        Vector dir = currentWhipLoc.toVector().subtract(hand.toVector()).normalize();
                        for (double d = 0; d < dist; d += 0.4) {
                            Location pt = hand.clone().add(dir.clone().multiply(d));
                            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(160, 160, 160), 0.7f));
                        }
                    }

                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentWhipLoc, 1.5)) {
                        if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId() && !whipHitEntities.contains(entity)) {
                            LivingEntity target = (LivingEntity) entity;
                            whipHitEntities.add(target);
                            DamageHandler.damageEntity(target, 4.0, this);

                            Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.1).setY(0.3);
                            target.setVelocity(pull);
                            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 0.8f);
                        }
                    }
                }
            }
        }
    }

    public void onLeftClick() {
        if (!isPlateActive || whipActive) {
            return;
        }

        if (System.currentTimeMillis() - lastWhipTime < 5000) {
            return;
        }

        ItemStack chest = player.getInventory().getChestplate();
        if (chest != null && chest.getType() == Material.IRON_CHESTPLATE) {
            org.bukkit.inventory.meta.Damageable dmgMeta = (org.bukkit.inventory.meta.Damageable) chest.getItemMeta();
            int maxDurability = Material.IRON_CHESTPLATE.getMaxDurability();
            int loss = (int) (maxDurability * 0.10);
            int newDmg = dmgMeta.getDamage() + loss;
            if (newDmg >= maxDurability) {
                player.getInventory().setChestplate(originalChestplate);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.2f, 1.0f);
                remove();
                bPlayer.addCooldown(this);
                return;
            } else {
                dmgMeta.setDamage(newDmg);
                chest.setItemMeta(dmgMeta);
                ferroChestplate = chest;
            }
        }

        lastWhipTime = System.currentTimeMillis();
        whipActive = true;
        whipTicks = 0;
        whipHitEntities = new ArrayList<>();

        whipP0 = getHandLocation();
        Vector lookDir = player.getEyeLocation().getDirection().normalize();
        whipP2 = player.getEyeLocation().add(lookDir.clone().multiply(10.0));

        Vector rightVec = lookDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (rightVec.lengthSquared() == 0) {
            rightVec = new Vector(1, 0, 0);
        }
        Location midpoint = whipP0.clone().add(whipP2.clone().subtract(whipP0).multiply(0.5));
        whipP1 = midpoint.add(rightVec.multiply(2.5));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.7f);
    }

    public void onShift() {
        if (!isPlateActive) {
            return;
        }

        if (System.currentTimeMillis() - lastAbsorbTime < 10000) {
            return;
        }

        Block targetBlock = player.getTargetBlockExact(15);
        if (targetBlock == null || !EarthAbility.isEarthbendable(player, targetBlock)) {
            return;
        }

        new TempBlock(targetBlock, Material.AIR).setRevertTime(15000);
        lastAbsorbTime = System.currentTimeMillis();

        ItemStack chest = player.getInventory().getChestplate();
        if (chest != null && chest.getType() == Material.IRON_CHESTPLATE) {
            org.bukkit.inventory.meta.Damageable dmgMeta = (org.bukkit.inventory.meta.Damageable) chest.getItemMeta();
            int maxDurability = Material.IRON_CHESTPLATE.getMaxDurability();
            int heal = (int) (maxDurability * 0.20);
            int newDmg = Math.max(0, dmgMeta.getDamage() - heal);
            dmgMeta.setDamage(newDmg);
            chest.setItemMeta(dmgMeta);
            ferroChestplate = chest;
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        Location targetLoc = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        new BukkitRunnable() {
            int subTicks = 0;
            @Override
            public void run() {
                if (subTicks > 10 || !player.isOnline()) {
                    cancel();
                    return;
                }
                subTicks++;
                Location playerChest = player.getLocation().add(0, 1.2, 0);
                Vector toChest = playerChest.toVector().subtract(targetLoc.toVector());
                double dist = toChest.length();
                if (dist > 0.2) {
                    for (double d = 0; d < dist; d += 0.4) {
                        Location pt = targetLoc.clone().add(toChest.clone().normalize().multiply(d));
                        pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(150, 120, 90), 0.7f));
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    public void cleanup() {
        if (isPlateActive && player.isOnline()) {
            ItemStack current = player.getInventory().getChestplate();
            if (current != null && current.getType() == Material.IRON_CHESTPLATE && current.getItemMeta() != null && current.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Ferro-Absorb Plate")) {
                player.getInventory().setChestplate(originalChestplate);
            }
        }
    }

    private Location getHandLocation() {
        Location hand = player.getLocation().clone().add(0, 1.1, 0);
        Vector right = player.getLocation().getDirection().clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (right.lengthSquared() == 0) {
            right = new Vector(0.35, 0, 0);
        } else {
            right.multiply(0.35);
        }
        return hand.add(right);
    }

    public ItemStack getOriginalChestplate() {
        return originalChestplate;
    }

    public void setPlateActive(boolean b) {
        this.isPlateActive = b;
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
        return "FerroAbsorb";
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
        cleanup();
        remove();
    }

    @Override
    public String getDescription() {
        return "Forge a custom metal armor plate by tracing a magnetic disk in front of you. Once active, left-click to launch a steel whip pulling enemies, and hold shift at earth sources to absorb them and repair your plate.";
    }

    @Override
    public String getInstructions() {
        return "Hold Shift at earth blocks. Trace the okrąg (up -> left -> down -> right -> up) with your crosshair. Release Shift to forge. While active: LPM to launch whip, Shift to absorb blocks.";
    }
}
