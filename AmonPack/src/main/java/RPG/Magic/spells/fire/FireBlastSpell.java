package RPG.Magic.spells.fire;

import Plugin.AmonPackPlugin;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.model.Spell;
import RPG.Magic.model.SpellElement;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireBlastSpell extends Spell {

    public FireBlastSpell() {
        super(
                "fireblast",
                "§c§lFireblast",
                "Wystrzeliwuje małą kulę ognia spadającą z grawitacją, która podpala wrogów i zadaje obrażenia obszarowe.",
                SpellElement.FIRE,
                40,    // 40 MP
                4.0    // 4.0s cooldown
        );
    }

    @Override
    public boolean cast(Player player, ManaManager manaManager) {
        if (!canCast(player, manaManager)) {
            return false;
        }

        // Zużycie many i nałożenie cooldownu
        manaManager.consumeMana(player.getUniqueId(), manaCost);
        manaManager.setCooldown(player.getUniqueId(), id, cooldownSeconds);
        manaManager.sendManaBarHud(player, name);

        Location startLoc = player.getEyeLocation().clone().add(player.getLocation().getDirection().multiply(0.5));
        Vector velocity = player.getLocation().getDirection().clone().normalize().multiply(1.25);
        velocity.setY(velocity.getY() + 0.12);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            Location current = startLoc.clone();
            Vector vel = velocity.clone();

            @Override
            public void run() {
                ticks++;
                if (ticks > 60 || !player.isOnline()) {
                    cancel();
                    return;
                }

                current.add(vel);
                // Grawitacja
                vel.subtract(new Vector(0, 0.035, 0));

                // Efekty cząsteczkowe pocisku
                current.getWorld().spawnParticle(Particle.FLAME, current, 3, 0.05, 0.05, 0.05, 0.02);
                current.getWorld().spawnParticle(Particle.SMOKE, current, 1, 0.02, 0.02, 0.02, 0.01);
                current.getWorld().spawnParticle(Particle.SMALL_FLAME, current, 2, 0.03, 0.03, 0.03, 0.01);

                // Sprawdzenie kolizji z blokiem
                if (current.getBlock().getType().isSolid()) {
                    explode(current, player);
                    cancel();
                    return;
                }

                // Sprawdzenie kolizji z bytami
                for (Entity entity : current.getWorld().getNearbyEntities(current, 1.2, 1.2, 1.2)) {
                    if (entity instanceof LivingEntity target && !entity.getUniqueId().equals(player.getUniqueId())) {
                        explode(current, player);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 1L, 1L);

        return true;
    }

    private void explode(Location loc, Player caster) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 8, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 18, 0.4, 0.4, 0.4, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (entity instanceof LivingEntity target && !entity.getUniqueId().equals(caster.getUniqueId())) {
                target.damage(6.0, caster);
                target.setFireTicks(80); // 4 sekundy podpalenia
            }
        }
    }
}
