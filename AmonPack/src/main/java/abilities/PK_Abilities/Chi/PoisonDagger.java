package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

public class PoisonDagger extends ChiAbility implements AddonAbility {

    private long cooldown;
    private double damage;
    private double speed;
    private double range;
    private int poisonDuration;
    private int poisonAmplifier;
    private int slownessDuration;
    private int slownessAmplifier;
    private double chiCost;

    private Item thrownItem;
    private Location origin;
    private Location lastLoc;
    private long launchTime;

    public PoisonDagger(Player player) {
        super(player);

        if (hasAbility(player, PoisonDagger.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        if (!ChiManager.consumeChi(player, chiCost)) {
            return;
        }

        launchDagger();
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PoisonDagger.Cooldown", 6000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PoisonDagger.Damage", 3.5);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PoisonDagger.Speed", 1.8);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PoisonDagger.Range", 30.0);
        this.poisonDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonDagger.PoisonDuration", 100);
        this.poisonAmplifier = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonDagger.PoisonAmplifier", 1);
        this.slownessDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonDagger.SlownessDuration", 100);
        this.slownessAmplifier = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonDagger.SlownessAmplifier", 1);
        this.chiCost = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PoisonDagger.ChiCost", 40.0);
    }

    private void launchDagger() {
        this.origin = player.getEyeLocation();
        this.lastLoc = origin.clone();
        this.launchTime = System.currentTimeMillis();

        ItemStack daggerStack = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = daggerStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§lZatruty Sztylet");
            daggerStack.setItemMeta(meta);
        }

        Vector direction = origin.getDirection().normalize();
        this.thrownItem = player.getWorld().dropItem(origin.clone().add(direction.clone().multiply(0.5)), daggerStack);
        this.thrownItem.setPickupDelay(Integer.MAX_VALUE);
        this.thrownItem.setGravity(false);
        this.thrownItem.setVelocity(direction.multiply(speed));

        player.getWorld().playSound(origin, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.4f);
        player.getWorld().playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.6f);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            cleanUp();
            remove();
            return;
        }

        if (thrownItem == null || !thrownItem.isValid() || thrownItem.isDead()) {
            finishWithCooldown();
            return;
        }

        Location currentLoc = thrownItem.getLocation();

        // Particle trail
        currentLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
        Particle.DustOptions greenDust = new Particle.DustOptions(Color.fromRGB(80, 220, 50), 0.9f);
        currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 2, 0.05, 0.05, 0.05, 0, greenDust);

        // Check for solid block collision
        if (currentLoc.getBlock().getType().isSolid()) {
            currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_ITEM_BREAK, 0.9f, 1.2f);
            currentLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, currentLoc, 10, 0.2, 0.2, 0.2, 0.05);
            finishWithCooldown();
            return;
        }

        // Entity hit detection along the path
        Collection<Entity> nearby = GeneralMethods.getEntitiesAroundPoint(currentLoc, 1.4);
        for (Entity e : nearby) {
            if (e instanceof LivingEntity victim && e.getEntityId() != player.getEntityId()) {
                DamageHandler.damageEntity(victim, damage, this);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, poisonAmplifier, false, true));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, slownessAmplifier, false, true));

                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 0.7f, 1.6f);
                victim.getWorld().spawnParticle(Particle.ITEM_SLIME, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);

                finishWithCooldown();
                return;
            }
        }

        // Check max range or timeout
        if (currentLoc.distanceSquared(origin) > range * range || (System.currentTimeMillis() - launchTime) > 3500L) {
            currentLoc.getWorld().spawnParticle(Particle.SMOKE, currentLoc, 4, 0.1, 0.1, 0.1, 0.02);
            finishWithCooldown();
            return;
        }

        this.lastLoc = currentLoc;
    }

    private void finishWithCooldown() {
        cleanUp();
        if (bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
        }
        remove();
    }

    private void cleanUp() {
        if (thrownItem != null && thrownItem.isValid()) {
            thrownItem.remove();
        }
    }

    @Override
    public void remove() {
        cleanUp();
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return thrownItem != null ? thrownItem.getLocation() : (player != null ? player.getLocation() : null);
    }

    @Override
    public String getName() {
        return "PoisonDagger";
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
        return "1.1";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        cleanUp();
        remove();
    }

    @Override
    public String getDescription() {
        return "Rzucasz zatrutym sztyletem (LPM). Trafiony przeciwnik otrzymuje obrażenia oraz efekty Trucizny (Poison) i Spowolnienia (Slowness).";
    }

    @Override
    public String getInstructions() {
        return "Kliknij LPM, aby cisnąć zatrutym sztyletem w stronę przeciwnika!";
    }
}
