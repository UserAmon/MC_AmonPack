package CustomContent.Bosses;

import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class ActiveBossInstance {

    public static final NamespacedKey BOSS_KEY = new NamespacedKey(AmonPackPlugin.plugin, "custom_boss_id");

    private final UUID uuid;
    private final CustomBoss template;
    private final Mob entity;
    private ItemDisplay displayEntity;
    private BossBar bossBar;
    private BukkitTask task;

    private int tickCounter = 0;

    public ActiveBossInstance(CustomBoss template, Mob entity) {
        this.template = template;
        this.entity = entity;
        this.uuid = entity.getUniqueId();

        setupEntity();
        setupModelDisplay();
        setupBossBar();
        startTask();
    }

    private void setupEntity() {
        entity.setCustomName(template.getDisplayName());
        entity.setCustomNameVisible(false);
        entity.setRemoveWhenFarAway(false);
        entity.getPersistentDataContainer().set(BOSS_KEY, PersistentDataType.STRING, template.getId());

        // Statystyki
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(template.getMaxHealth());
            entity.setHealth(template.getMaxHealth());
        }
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(template.getAttackDamage());
        }
        if (entity.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(template.getMovementSpeed());
        }
        if (entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
            entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(template.getKnockbackResistance());
        }
        if (entity.getAttribute(Attribute.FOLLOW_RANGE) != null) {
            entity.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(template.getFollowRange());
        }

        // Ukrycie bazowego moba
        entity.setInvisible(true);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false, false));
    }

    private void setupModelDisplay() {
        try {
            Location loc = entity.getLocation();
            this.displayEntity = loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
                ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(template.getCustomModelData());
                    item.setItemMeta(meta);
                }
                d.setItemStack(item);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            });
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[AmonPack] Nie udało się zespawnować ItemDisplay dla Bossa: " + t.getMessage());
        }
    }

    private void setupBossBar() {
        if (template.isBossBarEnabled()) {
            this.bossBar = Bukkit.createBossBar(
                    formatTitle(),
                    template.getBossBarColor(),
                    template.getBossBarStyle()
            );
            bossBar.setVisible(true);
        }
    }

    private void startTask() {
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || entity.isDead() || !entity.isValid()) {
                    remove();
                    cancel();
                    return;
                }

                tickCounter++;

                // 1. Synchronizacja modelu 3D z pozycją i obrotem moba
                if (displayEntity != null && displayEntity.isValid()) {
                    Location loc = entity.getLocation().clone().add(0, 0.2, 0);
                    displayEntity.teleport(loc);
                }

                // 2. Aktualizacja paska BossBar
                if (bossBar != null) {
                    double currentHp = entity.getHealth();
                    double maxHp = template.getMaxHealth();
                    double progress = Math.max(0.0, Math.min(1.0, currentHp / maxHp));
                    bossBar.setProgress(progress);
                    bossBar.setTitle(formatTitle());

                    // Aktualizacja graczy w zasięgu
                    for (Player p : entity.getWorld().getPlayers()) {
                        double dist = p.getLocation().distance(entity.getLocation());
                        if (dist <= template.getBossBarRange()) {
                            if (!bossBar.getPlayers().contains(p)) {
                                bossBar.addPlayer(p);
                            }
                        } else {
                            bossBar.removePlayer(p);
                        }
                    }
                }

                // 3. Sprawdzanie umiejętności na timer
                if (tickCounter % 20 == 0) { // co 1 sekundę
                    int sec = tickCounter / 20;
                    for (CustomBoss.BossSkill skill : template.getSkills()) {
                        if ("TIMER".equalsIgnoreCase(skill.trigger) && skill.intervalSeconds > 0) {
                            if (sec % skill.intervalSeconds == 0) {
                                if (skill.announcement != null && !skill.announcement.isEmpty()) {
                                    broadcastNearby(skill.announcement);
                                }
                                BossSkillExecutor.executeSkill(entity, skill.ability, skill.range);
                            }
                        } else if ("HEALTH_BELOW".equalsIgnoreCase(skill.trigger) && !skill.executed) {
                            double hpPercent = (entity.getHealth() / template.getMaxHealth()) * 100.0;
                            if (hpPercent <= skill.healthPercent) {
                                skill.executed = true;
                                if (skill.announcement != null && !skill.announcement.isEmpty()) {
                                    broadcastNearby(skill.announcement);
                                }
                                BossSkillExecutor.executeSkill(entity, skill.ability, skill.range);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 1L, 1L);
    }

    private String formatTitle() {
        int hp = (int) Math.ceil(entity.getHealth());
        int max = (int) Math.ceil(template.getMaxHealth());
        String title = template.getBossBarTitle()
                .replace("{health}", String.valueOf(hp))
                .replace("{max_health}", String.valueOf(max));
        return ChatColor.translateAlternateColorCodes('&', title);
    }

    private void broadcastNearby(String message) {
        String msg = ChatColor.translateAlternateColorCodes('&', message);
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distance(entity.getLocation()) <= template.getBossBarRange()) {
                p.sendMessage(msg);
            }
        }
    }

    public void remove() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (displayEntity != null && displayEntity.isValid()) {
            displayEntity.remove();
            displayEntity = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public UUID getUuid() { return uuid; }
    public CustomBoss getTemplate() { return template; }
    public Mob getEntity() { return entity; }
}
