package AvatarSystems.Crafting.Objects;

import AvatarSystems.Crafting.CraftingMenager;
import abilities.*;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.attribute.AttributeModifier;
import com.projectkorra.projectkorra.attribute.AttributePriority;
import com.projectkorra.projectkorra.util.ParticleEffect;
import methods_plugins.Abilities.SoundAbility;
import methods_plugins.Methods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

import static com.projectkorra.projectkorra.attribute.AttributeModifier.ADDITION;
import static com.projectkorra.projectkorra.attribute.AttributeModifier.SUBTRACTION;
import static methods_plugins.Abilities.SoundAbility.AfffectedEntities;

public class MagicEffects {
    private final List<MagicEffectsConditions> conditions;
    private final List<ItemStack> cost;
    private final String name;
    private final List<String> lore;
    private final String id;
    private final boolean isMajor;

    public MagicEffects(List<MagicEffectsConditions> conditions, List<ItemStack> cost, String name, List<String> lore,
            String id, boolean isMajor) {
        this(conditions, cost, name, lore, id, isMajor, false);
    }

    public double ExecuteOnTakinHit(Entity attacker, Player player) {
        double DamageReduction = 0;
        switch (id) {
            case "Monster_Hunter":
                if (attacker instanceof Monster) {
                    DamageReduction = DamageReduction + 1;
                }
                break;
            case "Earth_Resolve_Dmg_Taking":

                List<Block> NearBlocks = new ArrayList<>();
                for (Block b : GeneralMethods.getBlocksAroundPoint(player.getLocation(), 3)) {
                    if (b.getLocation().getY() <= player.getLocation().getY() + 1
                            && b.getLocation().distance(player.getLocation()) > 7
                            && EarthAbility.isEarthbendable(player, b)) {
                        NearBlocks.add(b);
                    }
                }
                if (NearBlocks.size() > 3) {
                    new EarthHammer(player, 0);
                    DamageReduction = DamageReduction + 1;
                }
                break;
        }
        return DamageReduction;
    }

    public double ExecuteOnHit(Entity victim, Player player) {
        double DamageBoosts = 0;
        switch (id) {
            case "Monster_Hunter":
                if (victim instanceof Monster) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Earth_Damage_Boost_Absorb":
                if (player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Earth_Damage_Boost_Hight":
                if (player.getLocation().getY() > victim.getLocation().getY()) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Fire_Damage_Boost":
                if (victim.getFireTicks() > 10) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Fire_Speed_Boost":
                if (victim.getFireTicks() > 10) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false));
                    victim.setFireTicks(0);
                }
                break;
            case "Knockback":
                Vector direction = victim.getLocation().toVector().subtract(player.getLocation().toVector())
                        .normalize();
                double knockbackStrength = 0.8;
                victim.setVelocity(direction.multiply(knockbackStrength).setY(0.45));
                break;
            case "Burrow":
                new SandWave(player, true, victim);
                break;
            case "Fire_Aspect":
                victim.setFireTicks(50);
                break;
            case "Smoke_Aspect":
                new SmokeSurge(player, true);
                break;
            case "Minor_Air_Sound_Damage_Buff":
                if (AfffectedEntities != null && !AfffectedEntities.keySet().isEmpty()
                        && AfffectedEntities.containsKey(victim)) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Major_Air_Sound_Hit":
                new SoundCrash(player, victim, 0);
                break;
            case "Earth_Hammer_Aspect":
                new EarthHammer(player, 1);
                break;
            case "Minor_Water_Icy_Slowness_Hit":
                new IceThorn(player, victim, 2);
                break;
            case "Air_Thrust":
                victim.setVelocity(new Vector(0, 1, 0));
                new AirPressure(player, victim, 0);
                ParticleEffect.CLOUD.display(victim.getLocation(), 5, 0.5, 0.5, 0.5, 0.05);
                break;
            case "Air_Damage_Boost_Downward":
                DamageBoosts = DamageBoosts + 1;
                new AirPressure(player, victim, 1);
                break;
            case "Earth_1":
                Methods.spawnFallingBlocks(victim.getLocation(), Material.DIRT, 6, 1.5, player);
                break;
            case "Ice_Thorn_Ability_Aspect":
                new IceThorn(player, victim, 0);
                break;
            case "Ice_Encase":
                new IceThorn(player, victim, 1);
                break;
            case "Lightning_Aspect":
                Methods.LightningProjectile(victim.getLocation().clone().add(0, 1.5, 0), player);
                // player.setVelocity(player.getLocation().getDirection().add(new
                // Vector(0,0.4,0)).multiply(1));
                // player.addPotionEffect(new
                // PotionEffect(PotionEffectType.SPEED,40,2,false,false,false));
                break;
        }
        return DamageBoosts;
    }

    public String getName() {
        return id;
    }

    public static String serializeList(List<MagicEffects> effects) {
        if (effects == null || effects.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (MagicEffects effect : effects) {
            sb.append(effect.getName()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static List<MagicEffects> deserializeList(String data) {
        List<MagicEffects> list = new ArrayList<>();
        if (data == null || data.isEmpty())
            return list;

        String[] names = data.split(",");
        for (String name : names) {
            MagicEffects effect = CraftingMenager.GetMagicEfectByName(name.trim());
            if (effect != null) {
                list.add(effect);
            } else {
                System.out.println("[Crafting] Nie znaleziono efektu o nazwie: " + name);
            }
        }
        return list;
    }

    public List<MagicEffectsConditions> getConditions() {
        return conditions;
    }

    public String getDisplayName() {
        return name;
    }

    public List<String> getLoreDescription() {
        return lore;
    }

    public boolean isMajorRune() {
        return isMajor;
    }

    public List<ItemStack> getCost() {
        return cost;
    }

    public static List<String> AffectedAbilities = new ArrayList<>(List.of("Torrent",
            "IceArch",
            "IceThorn",
            "Geyser",
            "FrostBreath",
            "AirSwipe",
            "AirBlade",
            "SonicBlast"));

    public boolean isItemEffect() {
        return isItemEffect;
    }

    private final boolean isItemEffect;

    public MagicEffects(List<MagicEffectsConditions> conditions, List<ItemStack> cost, String name, List<String> lore,
            String id, boolean isMajor, boolean isItemEffect) {
        this.conditions = conditions;
        this.cost = cost;
        this.name = name;
        this.lore = lore;
        this.id = id;
        this.isMajor = isMajor;
        this.isItemEffect = isItemEffect;
    }

    public double ExecuteOnUse(Player player) {
        double value = 0;
        switch (id) {
            case "Heal":
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 4));
                break;
            case "Speed":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
                break;
            case "Ice_Wall":
                // Example implementation for Ice Wall
                break;
        }
        return value;
    }
}
