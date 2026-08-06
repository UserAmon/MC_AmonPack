package Abilities.PK_Abilities.Water;

import Abilities.Bending.SpecialTriggerable;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import Plugin.AmonPackPlugin;
import Plugin.Methods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class WaterWhip extends WaterAbility implements AddonAbility, SpecialTriggerable {

    private int state = 0; // 0 = Ładowanie i nagrywanie kamery, 1 = Wypuszczenie bicza, 2 = Zakończono
    private int chargeTicks = 0;
    private int maxChargeTicks = 20;
    private final List<Vector> trajectoryDirections = new ArrayList<>();
    private final List<Location> trajectoryLocations = new ArrayList<>();

    private int whipIndex = 0;
    private double damage;
    private double range;
    private double knockback;
    private long cooldown;

    public WaterWhip(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Damage", 5.0);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Range", 12.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Knockback", 0.7);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.WaterWhip.Cooldown", 5000);
        this.maxChargeTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.WaterWhip.ChargeTicks", 20); // 20 ticków = 1 sekunda

        start();
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        // --- FAZA 1: 1 Sekunda ładowania + śledzenie obrotu kamery ---
        if (state == 0) {
            chargeTicks++;

            Location eyeLoc = player.getEyeLocation();
            Vector dir = eyeLoc.getDirection().clone().normalize();

            // Zapisujemy pozycję i kierunek patrzania z każdego ticku (tworząc parabolę obrotów kamery)
            trajectoryLocations.add(eyeLoc.clone());
            trajectoryDirections.add(dir.clone());

            // Efekty cząsteczkowe wody w ręce i przed oczami gracza
            Location handLoc = getHandLocation();
            ParticleEffect.WATER_WAKE.display(handLoc, 4, 0.08, 0.08, 0.08, 0.02);
            ParticleEffect.WATER_SPLASH.display(handLoc, 4, 0.08, 0.08, 0.08, 0.02);
            ParticleEffect.WATER_DROP.display(handLoc, 2, 0.05, 0.05, 0.05, 0.01);

            if (chargeTicks % 5 == 0) {
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 0.7f, 1.3f);
            }

            // Po 1 sekundzie (20 tickach) przechodzimy do wystrzelenia bicza
            if (chargeTicks >= maxChargeTicks) {
                state = 1;
                whipIndex = 0;
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.2f, 1.1f);
            }
        }
        // --- FAZA 2: Wystrzelenie bicza po zarejestrowanej paraboli ---
        else if (state == 1) {
            if (whipIndex >= trajectoryDirections.size()) {
                bPlayer.addCooldown(this);
                remove();
                return;
            }

            // Pobieramy zapisany punkt i kierunek z paraboli
            Location startLoc = trajectoryLocations.get(whipIndex);
            Vector dirVec = trajectoryDirections.get(whipIndex);

            // Tworzymy punkt bicza wysunięty wzdłuż paraboli
            double distMult = (whipIndex + 1) * (range / maxChargeTicks);
            Location whipPoint = startLoc.clone().add(dirVec.clone().multiply(distMult));

            // Efekty cząsteczkowe i bloki wody bicza (stylizowane jak w WaterFist)
            ParticleEffect.WATER_WAKE.display(whipPoint, 6, 0.12, 0.12, 0.12, 0.03);
            ParticleEffect.WATER_SPLASH.display(whipPoint, 6, 0.12, 0.12, 0.12, 0.03);

            Block b = whipPoint.getBlock();
            if (b.getType() == Material.AIR) {
                new TempBlock(b, Material.WATER).setRevertTime(220);
            }

            // Detekcja trafień w przeciwników
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(whipPoint, 1.8)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    DamageHandler.damageEntity(target, damage, this);
                    target.setVelocity(dirVec.clone().multiply(knockback).setY(0.25));
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
                    ParticleEffect.WATER_SPLASH.display(target.getLocation().add(0, 1, 0), 12, 0.2, 0.2, 0.2, 0.1);
                }
            }

            whipIndex++;
        }
    }

    private Location getHandLocation() {
        Location base = player.getLocation().clone().add(0, 1.2, 0);
        Vector forward = player.getLocation().getDirection().clone().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        return base.add(forward.multiply(0.4)).add(right.multiply(0.4));
    }

    @Override
    public TriggerType getSupportedTriggerType() {
        return TriggerType.BOTH; // WaterWhip może być przypisany do SWAP (F) oraz DROP (Q)
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
        return "WaterWhip";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public String getAuthor() {
        return "Amon";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}
}
