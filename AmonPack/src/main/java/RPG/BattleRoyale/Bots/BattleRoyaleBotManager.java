package RPG.BattleRoyale.Bots;

import CustomContent.Guns.AmmoType;
import CustomContent.Guns.GunType;
import CustomContent.Guns.GunUniqueMod;
import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Weapons.BattleRoyaleWeaponHelper;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class BattleRoyaleBotManager {

    public static final NamespacedKey KEY_BR_BOT = new NamespacedKey(AmonPackPlugin.plugin, "br_bot_combatant");

    private static final String[] BOT_NAMES = {
            "Stalker", "Snajper", "Maruder", "Ocalały", "Najemnik",
            "Bandyta", "Rusznikarz", "Włóczęga", "Łowca", "Zwiadowca",
            "Szturmowiec", "Cień", "Rebeliant", "Weteran", "Koczownik",
            "Rewolwerowiec", "Korsarz", "Traper", "Desperat"
    };

    private final Set<UUID> activeBotUuids = Collections.synchronizedSet(new HashSet<>());
    private final Random random = new Random();

    /**
     * Spawnuje bota bojowego na danym punkcie startowym.
     */
    public LivingEntity spawnBot(Location spawnLoc, int botIndex) {
        if (spawnLoc == null || spawnLoc.getWorld() == null) return null;

        String name = BOT_NAMES[botIndex % BOT_NAMES.length];

        // Wybór typu bytu
        EntityType[] types = { EntityType.WITHER_SKELETON, EntityType.SKELETON, EntityType.ZOMBIE, EntityType.VINDICATOR };
        EntityType selectedType = types[random.nextInt(types.length)];

        Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, selectedType);
        if (!(entity instanceof Monster monster)) return null;

        monster.setCustomName(ChatColor.RED + "[Bot] " + name);
        monster.setCustomNameVisible(true);
        monster.setCanPickupItems(true);
        monster.setRemoveWhenFarAway(false);

        // Zwiększenie punktów życia i prędkości
        if (monster.getAttribute(Attribute.MAX_HEALTH) != null) {
            monster.getAttribute(Attribute.MAX_HEALTH).setBaseValue(24.0 + random.nextInt(10));
            monster.setHealth(monster.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
        }
        if (monster.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            monster.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.28 + (random.nextDouble() * 0.05));
        }

        // Ekwipunek
        EntityEquipment eq = monster.getEquipment();
        if (eq != null) {
            // Hełm (chroni przed słońcem)
            eq.setHelmet(new ItemStack(Material.IRON_HELMET));
            eq.setHelmetDropChance(0.15f);

            // Zbroja
            if (random.nextBoolean()) eq.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
            if (random.nextBoolean()) eq.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
            eq.setBoots(new ItemStack(Material.IRON_BOOTS));

            // Broń
            if (random.nextDouble() < 0.60) {
                // Broń palna
                GunType[] gunTypes = { GunType.FLINTLOCK_PISTOL, GunType.FLINTLOCK_MUSKET, GunType.BLUNDERBUSS, GunType.PEPPERBOX };
                GunType gt = gunTypes[random.nextInt(gunTypes.length)];
                ItemStack gun = BattleRoyaleWeaponHelper.createGun(gt, 1 + random.nextInt(2), false, false, false, false, GunUniqueMod.NONE);
                eq.setItemInMainHand(gun);
                eq.setItemInMainHandDropChance(0.50f);
            } else {
                // Broń biała
                ItemStack sword = new ItemStack(random.nextBoolean() ? Material.IRON_SWORD : Material.DIAMOND_SWORD);
                eq.setItemInMainHand(sword);
                eq.setItemInMainHandDropChance(0.35f);
            }
        }

        // Flaga PDC
        monster.getPersistentDataContainer().set(KEY_BR_BOT, PersistentDataType.BYTE, (byte) 1);
        activeBotUuids.add(monster.getUniqueId());

        return monster;
    }

    public boolean isBot(Entity entity) {
        if (entity == null) return false;
        return activeBotUuids.contains(entity.getUniqueId()) ||
                entity.getPersistentDataContainer().has(KEY_BR_BOT, PersistentDataType.BYTE);
    }

    public void removeBot(UUID uuid) {
        activeBotUuids.remove(uuid);
    }

    public int getAliveBotCount() {
        return activeBotUuids.size();
    }

    public Set<UUID> getActiveBotUuids() {
        return Collections.unmodifiableSet(activeBotUuids);
    }

    public void cleanup() {
        activeBotUuids.clear();
    }
}
