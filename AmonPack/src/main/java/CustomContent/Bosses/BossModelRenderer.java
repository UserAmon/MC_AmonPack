package CustomContent.Bosses;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class BossModelRenderer {

    private final CustomBoss template;
    private final Mob entity;
    private final BossAnimator animator;
    private ItemDisplay displayEntity;

    public BossModelRenderer(CustomBoss template, Mob entity) {
        this.template = template;
        this.entity = entity;
        this.animator = new BossAnimator((float) Math.max(0.5, template.getScale()));
        spawnModel();
    }

    private void spawnModel() {
        try {
            Location loc = entity.getLocation();
            this.displayEntity = loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
                // LEATHER_HORSE_ARMOR jako główny stabilny nośnik modeli 3D
                ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof LeatherArmorMeta lam) {
                    lam.setColor(Color.WHITE);
                }
                if (meta != null) {
                    meta.setCustomModelData(template.getCustomModelData());
                    item.setItemMeta(meta);
                }
                d.setItemStack(item);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
                d.setBillboard(Display.Billboard.FIXED);

                // Pełna płynność i jasność
                d.setInterpolationDuration(1);
                d.setTeleportDuration(1);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setViewRange(64.0f);
                d.setShadowRadius(1.2f);
                d.setShadowStrength(1.0f);

                float sc = (float) Math.max(0.5, template.getScale());
                d.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 1f, 0f),
                        new Vector3f(sc, sc, sc),
                        new AxisAngle4f(0f, 0f, 1f, 0f)
                ));
            });
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[AmonPack] Błąd przy tworzeniu BossModelRenderer dla " + template.getId() + ": " + t.getMessage());
        }
    }

    public void tick() {
        if (displayEntity == null || !displayEntity.isValid() || entity == null || !entity.isValid()) {
            return;
        }

        Location loc = entity.getLocation();
        animator.tick(loc);

        // Synchronizacja pozycji i obrotu (Yaw)
        displayEntity.teleport(loc);

        // Dynamiczna transformacja animacji (Translacja, Rotacja, Skala)
        Vector3f translation = animator.computeTranslation();
        AxisAngle4f leftRotation = animator.computeRotation();
        Vector3f scale = animator.computeScale();
        AxisAngle4f rightRotation = new AxisAngle4f(0f, 0f, 1f, 0f);

        Transformation transformation = new Transformation(translation, leftRotation, scale, rightRotation);
        displayEntity.setTransformation(transformation);

        // Efekty cząsteczkowe w zależności od stanu animacji
        spawnStateParticles(loc);
    }

    private void spawnStateParticles(Location loc) {
        if (animator.getCurrentState() == BossAnimator.AnimationState.CAST) {
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 1.5, 0), 4, 0.6, 0.6, 0.6, 0.05);
            loc.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.0, 0), 6, 0.5, 0.5, 0.5, 0.1);
        } else if (animator.getCurrentState() == BossAnimator.AnimationState.ATTACK) {
            loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(loc.getDirection().multiply(1.5)).add(0, 1.0, 0), 1);
            loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(loc.getDirection().multiply(1.5)).add(0, 1.0, 0), 8, 0.4, 0.4, 0.4, 0.1);
        } else if (animator.getCurrentState() == BossAnimator.AnimationState.HURT) {
            loc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.clone().add(0, 1.5, 0), 4, 0.3, 0.4, 0.3, 0.05);
        } else if (animator.getCurrentState() == BossAnimator.AnimationState.DEATH) {
            loc.getWorld().spawnParticle(Particle.POOF, loc.clone().add(0, 1.0, 0), 6, 0.5, 0.5, 0.5, 0.05);
            loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 0.5, 0), 3, 0.4, 0.3, 0.4, 0.02);
        }
    }

    public void playAttackAnimation() {
        animator.triggerAttack();
    }

    public void playCastAnimation() {
        animator.triggerCast();
    }

    public void playHurtAnimation() {
        animator.triggerHurt();
    }

    public void playDeathAnimation() {
        animator.triggerDeath();
    }

    public boolean isDeathAnimationFinished() {
        return animator.isDead();
    }

    public void remove() {
        if (displayEntity != null && displayEntity.isValid()) {
            displayEntity.remove();
        }
    }

    public ItemDisplay getDisplayEntity() {
        return displayEntity;
    }

    public BossAnimator getAnimator() {
        return animator;
    }
}
