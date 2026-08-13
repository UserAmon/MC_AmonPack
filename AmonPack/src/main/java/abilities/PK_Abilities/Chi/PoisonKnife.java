package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PoisonKnife extends ChiAbility implements AddonAbility {

    private static final String KEY_NAME = "poisonknife_ability";
    public static final NamespacedKey KNIFE_KEY = new NamespacedKey(AmonPackPlugin.plugin, KEY_NAME);

    private long cooldown;
    private double damage;
    private int poisonDuration;
    private int poisonAmplifier;
    private int blindnessDuration;
    private long chiBlockDuration;

    private int slot;
    private ItemStack knifeItem;

    public PoisonKnife(Player player) {
        super(player);

        if (hasAbility(player, PoisonKnife.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        this.slot = player.getInventory().getHeldItemSlot();
        this.knifeItem = createKnifeItem();

        equipKnife();
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PoisonKnife.Cooldown", 8000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PoisonKnife.Damage", 3.0);
        this.poisonDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonKnife.PoisonDuration", 100);
        this.poisonAmplifier = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonKnife.PoisonAmplifier", 1);
        this.blindnessDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonKnife.BlindnessDuration", 60);
        this.chiBlockDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PoisonKnife.ChiBlockDuration", 4000L);
    }

    public static ItemStack createKnifeItem() {
        ItemStack item = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aKnife");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(KNIFE_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isPoisonKnife(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(KNIFE_KEY, PersistentDataType.BYTE);
    }

    public static void purgeUnregisteredKnives(Player player) {
        if (player == null) return;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.WOODEN_SWORD) {
                if (isPoisonKnife(item)) {
                    if (!hasAbility(player, PoisonKnife.class)) {
                        player.getInventory().setItem(i, null);
                    }
                }
            }
        }
    }

    private void equipKnife() {
        ItemStack currentItem = player.getInventory().getItem(slot);
        if (currentItem != null && currentItem.getType() != Material.AIR && !isPoisonKnife(currentItem)) {
            int emptySlot = player.getInventory().firstEmpty();
            if (emptySlot != -1) {
                player.getInventory().setItem(emptySlot, currentItem);
                player.getInventory().setItem(slot, null);
            }
        }
        player.getInventory().setItem(slot, knifeItem);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8f, 1.5f);
    }

    public void removeKnifeFromPlayer() {
        if (player == null) return;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isPoisonKnife(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            removeWithoutCooldown();
            return;
        }

        if (player.getInventory().getHeldItemSlot() != slot) {
            removeWithoutCooldown();
            return;
        }

        ItemStack current = player.getInventory().getItemInMainHand();
        if (current == null || !isPoisonKnife(current)) {
            player.getInventory().setItem(slot, knifeItem);
        }
    }

    public void onHitEntity(LivingEntity victim) {
        if (victim == null || player == null) return;

        DamageHandler.damageEntity(victim, damage, this);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, poisonAmplifier, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, 0, false, false));

        BendingPlayer targetBPlayer = BendingPlayer.getBendingPlayer(victim);
        if (targetBPlayer != null) {
            targetBPlayer.blockChi(chiBlockDuration);
        }

        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        victim.getWorld().spawnParticle(org.bukkit.Particle.ITEM_CRACK, victim.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1, new ItemStack(Material.SLIME_BALL));

        removeWithCooldown();
    }

    public void removeWithoutCooldown() {
        removeKnifeFromPlayer();
        super.remove();
    }

    private void removeWithCooldown() {
        removeKnifeFromPlayer();
        if (bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
        }
        super.remove();
    }

    @Override
    public void remove() {
        removeKnifeFromPlayer();
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player != null ? player.getLocation() : null;
    }

    @Override
    public String getName() {
        return "PoisonKnife";
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
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
        removeWithoutCooldown();
    }

    @Override
    public String getDescription() {
        return "Daje w rękę zatruty nożyk przy wyborze slotu. Zaatakowanie wroga tym nożem zadaje obrażenia, nakłada truciznę, oślepienie oraz blokuje chi na określony czas.";
    }

    @Override
    public String getInstructions() {
        return "Wybierz slot z PoisonKnife i uderz przeciwnika mieczem!";
    }
}
