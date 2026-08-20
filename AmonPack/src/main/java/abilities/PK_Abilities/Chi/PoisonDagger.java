package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PoisonDagger extends ChiAbility implements AddonAbility {

    private static final Map<UUID, Long> MULTI_THROWS = new ConcurrentHashMap<>();

    private long cooldown;
    private double damage;
    private double speed;
    private int poisonDuration;
    private int poisonAmplifier;
    private int slownessDuration;
    private int slownessAmplifier;
    private double chiCost;

    private boolean hasTwin = false;
    private boolean hasSynergy = false;
    private boolean hasNeurotoxin = false;

    private Arrow firedArrow;

    public PoisonDagger(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        if (branch != null) {
            hasTwin = branch.hasUpgrade("PoisonDaggerTwin");
            hasSynergy = branch.hasUpgrade("PoisonDaggerSynergy");
            hasNeurotoxin = branch.hasUpgrade("PoisonDaggerNeurotoxin");
        }

        if (!ChiManager.consumeChi(player, chiCost)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (hasTwin) {
            Long lastThrow = MULTI_THROWS.get(uuid);
            if (lastThrow != null && (System.currentTimeMillis() - lastThrow <= 3000L)) {
                MULTI_THROWS.remove(uuid);
                bPlayer.addCooldown(this, cooldown);
            } else {
                MULTI_THROWS.put(uuid, System.currentTimeMillis());
                AmonPackPlugin.plugin.getServer().getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                    Long t = MULTI_THROWS.remove(uuid);
                    if (t != null && bPlayer != null && !bPlayer.isOnCooldown(this)) {
                        bPlayer.addCooldown(this, cooldown);
                    }
                }, 60L);
            }
        } else {
            bPlayer.addCooldown(this, cooldown);
        }

        launchDagger();
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PoisonDagger.Cooldown", 6000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PoisonDagger.Damage", 3.5);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PoisonDagger.Speed", 2.5);
        this.poisonDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonDagger.PoisonDuration", 100);
        this.poisonAmplifier = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonDagger.PoisonAmplifier", 1);
        this.slownessDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonDagger.SlownessDuration", 100);
        this.slownessAmplifier = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PoisonDagger.SlownessAmplifier", 1);
        this.chiCost = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PoisonDagger.ChiCost", 40.0);
    }

    private void launchDagger() {
        Vector dir = player.getEyeLocation().getDirection().normalize();

        this.firedArrow = player.launchProjectile(Arrow.class, dir);
        this.firedArrow.setVelocity(dir.multiply(speed));
        this.firedArrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        this.firedArrow.setDamage(0.0); // No vanilla damage!
        this.firedArrow.setMetadata("PoisonDaggerArrow", new FixedMetadataValue(AmonPackPlugin.plugin, damage));
        this.firedArrow.setMetadata("PoisonDaggerCaster", new FixedMetadataValue(AmonPackPlugin.plugin, player.getUniqueId().toString()));
        if (hasNeurotoxin) {
            this.firedArrow.setMetadata("PoisonDaggerNeurotoxin", new FixedMetadataValue(AmonPackPlugin.plugin, true));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.7f);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            cleanUp();
            remove();
            return;
        }

        if (firedArrow == null || !firedArrow.isValid() || firedArrow.isDead() || firedArrow.isInBlock() || firedArrow.isOnGround()) {
            remove();
            return;
        }

        // Particle trail
        Location loc = firedArrow.getLocation();
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc, 2, 0.05, 0.05, 0.05, 0.01);
        Particle.DustOptions greenDust = new Particle.DustOptions(Color.fromRGB(60, 220, 40), 1.0f);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 2, 0.05, 0.05, 0.05, 0, greenDust);

        if (System.currentTimeMillis() - getStartTime() > 3000L) {
            remove();
        }
    }

    public void handleArrowHit(Arrow arrow, Entity hitEntity) {
        if (arrow == null) return;

        if (hitEntity instanceof LivingEntity victim && hitEntity.getEntityId() != player.getEntityId()) {
            DamageHandler.damageEntity(victim, damage, this);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, poisonAmplifier, false, true));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, slownessAmplifier, false, true));

            if (hasNeurotoxin) {
                if (victim instanceof Player targetPlayer) {
                    BendingPlayer bTarget = BendingPlayer.getBendingPlayer(targetPlayer);
                    if (bTarget != null) {
                        bTarget.blockChi();
                    }
                }
            }

            if (hasSynergy && bPlayer != null) {
                if (bPlayer.isOnCooldown("DaggerTrick")) {
                    bPlayer.removeCooldown("DaggerTrick");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.8f);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText("§a🗡 §lSYNERGIA: §eZresetowano cooldown DaggerTrick!"));
                }
            }

            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 0.8f, 1.5f);
            victim.getWorld().spawnParticle(Particle.ITEM_SLIME, victim.getLocation().add(0, 1, 0), 12, 0.2, 0.2, 0.2, 0.1);
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 8, 0.2, 0.2, 0.2, 0.1);
        }

        remove();
    }

    private void cleanUp() {
        if (firedArrow != null && firedArrow.isValid()) {
            firedArrow.remove();
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
        return firedArrow != null ? firedArrow.getLocation() : (player != null ? player.getLocation() : null);
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
        return "1.0";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        remove();
    }

    @Override
    public String getDescription() {
        return "Rzucasz trującym sztyletem w stronę wroga (LPM). Trafiony cel otrzymuje obrażenia oraz efekty Trucizny II i Spowolnienia II na 5 sekund.";
    }

    @Override
    public String getInstructions() {
        return "Kliknij LPM, aby rzucić zatrutym nożem!";
    }
}
