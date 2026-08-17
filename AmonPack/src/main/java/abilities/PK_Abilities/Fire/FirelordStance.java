package Abilities.PK_Abilities.Fire;

import Abilities.Util_Objects.LightningBolt;
import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FirelordStance extends FireAbility implements AddonAbility {

    private long durationMs;
    private long cooldown;
    private double broadcastRadius;
    private int speedAmplifier;
    private int boltDirections;
    private double boltDamage;
    private double boltRange;
    private int boltBounces;

    private double cooldownMultiplier;
    private double rangeMultiplier;
    private double damageMultiplier;
    private double speedMultiplier;

    private long startTime;
    private Random random = new Random();
    private boolean alive = true;
    private BossBar bossBar;

    public FirelordStance(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        if (FirelordStanceManager.isActive(player)) {
            FirelordStance current = FirelordStanceManager.getStance(player);
            if (current != null) {
                current.remove();
            }
            return;
        }

        loadConfig();

        this.startTime = System.currentTimeMillis();
        this.bossBar = Bukkit.createBossBar("§2⚡§4 FIRELORD STANCE §2⚡", BarColor.RED, BarStyle.SOLID);
        this.bossBar.addPlayer(player);
        this.bossBar.setVisible(true);

        FirelordStanceManager.registerStance(player, this);

        onActivateVisualsAndBroadcast();

        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FirelordStance.Cooldown", 30000L);
        this.durationMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FirelordStance.Duration", 15L)
                * 1000L;
        this.broadcastRadius = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Fire.FirelordStance.BroadcastRadius", 50.0);
        this.speedAmplifier = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FirelordStance.SpeedAmplifier",
                1);
        this.boltDirections = AmonPackPlugin.getAbilitiesConfig()
                .getInt("AmonPack.Fire.FirelordStance.BoltBurst.Directions", 8);
        this.boltDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FirelordStance.BoltBurst.Damage",
                3.0);
        this.boltRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FirelordStance.BoltBurst.Range",
                12.0);
        this.boltBounces = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FirelordStance.BoltBurst.Bounces",
                2);

        this.cooldownMultiplier = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Fire.FirelordStance.Boost.CooldownMultiplier", 0.6);
        this.rangeMultiplier = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Fire.FirelordStance.Boost.RangeMultiplier", 1.4);
        this.damageMultiplier = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Fire.FirelordStance.Boost.DamageMultiplier", 1.5);
        this.speedMultiplier = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Fire.FirelordStance.Boost.SpeedMultiplier", 1.3);
    }

    private void onActivateVisualsAndBroadcast() {
        Location loc = player.getLocation();
        for (int i = 0; i < 5; i++) {
            double rx = (random.nextDouble() - 0.5) * 6.0;
            double rz = (random.nextDouble() - 0.5) * 6.0;
            Location strikeLoc = loc.clone().add(rx, 0, rz);
            player.getWorld().strikeLightningEffect(strikeLoc);
        }

        player.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.8f);
        player.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 1.5f, 1.2f);

        String broadcastMsg = "§2[§4Firelord§2] §c" + player.getName()
                + " §ewszedł w stan Władcy Ognia — §6strzeżcie się!";
        double radiusSq = broadcastRadius * broadcastRadius;
        for (Player p : player.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= radiusSq) {
                p.sendMessage(broadcastMsg);
            }
        }
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= durationMs) {
            remove();
            return;
        }

        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.05, 0), 5, 0.4, 0.2, 0.4, 0.05);

        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(180, 220, 255), 1.0f);
        player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.1, 0), 3, 0.3, 0.4, 0.3, 0, dust);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, speedAmplifier, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 10, 0, false, false));

        if (bossBar != null) {
            double pct = 1.0 - ((double) elapsed / durationMs);
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, pct)));
        }
    }

    public void triggerBoltBurst() {
        if (player == null || !player.isOnline())
            return;

        Location center = player.getLocation().add(0, 1.0, 0);
        center.getWorld().strikeLightningEffect(player.getLocation());
        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 1.0f);

        List<LightningBolt> bolts = new ArrayList<>();
        double angleStep = 360.0 / Math.max(1, boltDirections);
        for (int i = 0; i < boltDirections; i++) {
            double angleRad = Math.toRadians(i * angleStep);
            Vector dir = new Vector(Math.cos(angleRad), 0.1, Math.sin(angleRad)).normalize();
            bolts.add(new LightningBolt(player, this, center, dir, boltDamage, boltRange, boltBounces, true));
        }

        bolts.add(new LightningBolt(player, this, center, new Vector(0, 1, 0), boltDamage, boltRange, boltBounces,
                false));

        new BukkitRunnable() {
            @Override
            public void run() {
                bolts.removeIf(LightningBolt::isDead);
                if (bolts.isEmpty()) {
                    cancel();
                    return;
                }
                List<LightningBolt> nextBranches = new ArrayList<>();
                for (LightningBolt bolt : bolts) {
                    List<LightningBolt> branches = bolt.progress();
                    if (branches != null) {
                        nextBranches.addAll(branches);
                    }
                }
                bolts.addAll(nextBranches);
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    public boolean isAlive() {
        return alive;
    }

    public double getCooldownMultiplier() {
        return cooldownMultiplier;
    }

    public double getRangeMultiplier() {
        return rangeMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    @Override
    public void remove() {
        if (!alive)
            return;
        alive = false;
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        FirelordStanceManager.unregisterStance(player);
        bPlayer.addCooldown(this, cooldown);
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
        return "FirelordStance";
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
        return "Aktywuje stan Władcy Ognia, potęgujący wszystkie umiejętności ognia i piorunów, nadający efekty oraz chroniący przed obrażeniami od upadku poprzez wyzwolenie BoltBurstu.";
    }

    @Override
    public String getInstructions() {
        return "Kliknij LPM aby włączyć/wyłączyć stan Władcy Ognia.";
    }
}
