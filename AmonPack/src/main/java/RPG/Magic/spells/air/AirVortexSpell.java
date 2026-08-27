package RPG.Magic.spells.air;

import Plugin.AmonPackPlugin;
import RPG.Magic.elements.ElementStatusManager;
import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.model.Spell;
import RPG.Magic.model.SpellElement;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AirVortexSpell extends Spell {

    private static final Map<UUID, GestureSession> activeSessions = new ConcurrentHashMap<>();

    public AirVortexSpell() {
        super("air_vortex", "§b§lWir Powietrza", SpellElement.AIR, 50, 8.0);
    }

    @Override
    public boolean cast(Player player, ItemStack tomeItem, ManaManager manaManager) {
        boolean hasManaRed = MagicItemManager.hasUpgrade(tomeItem, "mana_reduction");
        boolean hasCdRed = MagicItemManager.hasUpgrade(tomeItem, "cooldown_reduction");

        int effectiveMana = hasManaRed ? Math.max(15, getManaCost() - 10) : getManaCost();
        double effectiveCd = hasCdRed ? Math.max(2.0, getCooldownSeconds() - 1.0) : getCooldownSeconds();

        if (isOnCooldown(player)) {
            player.sendMessage("§cZaklęcie " + getName() + " §codnawia się (" + String.format("%.1f", getRemainingCooldown(player)) + "s)!");
            return false;
        }

        if (!manaManager.hasMana(player, effectiveMana)) {
            player.sendMessage("§cBrak many! Wymagane: " + effectiveMana + " MP (" + manaManager.getMana(player) + "/" + manaManager.getMaxMana(player) + ")");
            return false;
        }

        if (activeSessions.containsKey(player.getUniqueId())) {
            return false;
        }

        GestureSession session = new GestureSession(player, tomeItem, manaManager, effectiveMana, effectiveCd, this);
        activeSessions.put(player.getUniqueId(), session);
        session.start();
        return true;
    }

    private static class GestureSession {
        private final Player player;
        private final ItemStack tomeItem;
        private final ManaManager manaManager;
        private final int manaCost;
        private final double cooldownSec;
        private final Spell spell;

        private final List<Vector> starOffsets = new ArrayList<>();
        private final boolean[] collected = new boolean[5];
        private int collectedCount = 0;
        private boolean fullyCharged = false;

        public GestureSession(Player player, ItemStack tomeItem, ManaManager manaManager, int manaCost, double cooldownSec, Spell spell) {
            this.player = player;
            this.tomeItem = tomeItem;
            this.manaManager = manaManager;
            this.manaCost = manaCost;
            this.cooldownSec = cooldownSec;
            this.spell = spell;
            initStarPoints();
        }

        private void initStarPoints() {
            Location eye = player.getEyeLocation();
            Vector forward = eye.getDirection().normalize();
            Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
            if (right.lengthSquared() < 0.01) right = new Vector(1, 0, 0);
            Vector up = right.clone().crossProduct(forward).normalize();

            // 5 wierzchołków gwiazdy w płaszczyźnie przed graczem (w odległości 3.5 bloku)
            double r = 1.8;
            double dist = 3.5;
            int[] order = new int[]{0, 2, 4, 1, 3}; // Kolejność rysowania gwiazdy (góra -> dół-lewo -> góra-prawo -> dół-prawo -> góra-lewo)
            for (int idx : order) {
                double angle = Math.toRadians(-90 + idx * 72);
                double ox = Math.cos(angle) * r;
                double oy = -Math.sin(angle) * r;
                Vector pt = forward.clone().multiply(dist).add(right.clone().multiply(ox)).add(up.clone().multiply(oy));
                starOffsets.add(pt);
            }
        }

        public void start() {
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || player.isDead() || ticks++ > 200) {
                        activeSessions.remove(player.getUniqueId());
                        cancel();
                        return;
                    }

                    // Jeśli gracz puścił Shift
                    if (!player.isSneaking()) {
                        if (fullyCharged) {
                            releaseVortex();
                        } else {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§cPrzerwano ładowanie Wiru Powietrza!"));
                        }
                        activeSessions.remove(player.getUniqueId());
                        cancel();
                        return;
                    }

                    Location eye = player.getEyeLocation();
                    Vector dir = eye.getDirection().normalize();

                    if (!fullyCharged) {
                        // Sprawdzanie czy celownik najeżdża na punkty gwiazdy
                        for (int i = 0; i < 5; i++) {
                            if (!collected[i]) {
                                Location ptLoc = eye.clone().add(starOffsets.get(i));
                                Vector toPt = ptLoc.toVector().subtract(eye.toVector()).normalize();
                                if (dir.dot(toPt) > 0.94) {
                                    collected[i] = true;
                                    collectedCount++;
                                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f + (collectedCount * 0.15f));
                                    if (collectedCount >= 5) {
                                        fullyCharged = true;
                                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
                                    }
                                }
                            }
                        }

                        // Rysowanie punktów i linii
                        for (int i = 0; i < 5; i++) {
                            Location ptLoc = eye.clone().add(starOffsets.get(i));
                            if (collected[i]) {
                                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ptLoc, 1, 0, 0, 0, 0);
                            } else {
                                player.getWorld().spawnParticle(Particle.CLOUD, ptLoc, 2, 0.05, 0.05, 0.05, 0.01);
                                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, ptLoc, 1, 0, 0, 0, 0);
                            }
                        }

                        if (!fullyCharged) {
                            String msg = "§f[Wir Powietrza] §bNarysuj gwiazdę kamerą: §e" + collectedCount + "/5";
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
                        }
                    }

                    if (fullyCharged) {
                        Location center = eye.clone().add(dir.clone().multiply(2.0));
                        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 2, 0.2, 0.2, 0.2, 0.05);
                        player.getWorld().spawnParticle(Particle.CLOUD, center, 4, 0.1, 0.1, 0.1, 0.02);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§a§l✦ WIR POWIETRZA NAŁADOWANY! ✦ §fPuść Shift, aby uwolnić wir!"));
                    }
                }
            }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
        }

        private void releaseVortex() {
            if (!manaManager.hasMana(player, manaCost)) {
                player.sendMessage("§cBrak many na uwolnienie wiru!");
                return;
            }

            manaManager.consumeMana(player, manaCost);
            spell.setCooldown(player, (long) (cooldownSec * 1000));

            Location targetLoc = player.getTargetBlockExact(16) != null ?
                    player.getTargetBlockExact(16).getLocation().add(0.5, 1.0, 0.5) :
                    player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(10.0));

            targetLoc.getWorld().playSound(targetLoc, Sound.ITEM_ELYTRA_FLYING, 1.5f, 0.8f);
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PHANTOM_SWOOP, 1.2f, 1.4f);

            // Wir działający przez 4 sekundy (80 ticków)
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks++ > 80) {
                        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);
                        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 2);
                        cancel();
                        return;
                    }

                    // Cząsteczki wiru
                    double radius = 3.5;
                    for (int i = 0; i < 4; i++) {
                        double angle = (ticks * 0.35) + (i * Math.PI * 0.5);
                        double x = Math.cos(angle) * radius * (1.0 - (ticks % 20) * 0.04);
                        double z = Math.sin(angle) * radius * (1.0 - (ticks % 20) * 0.04);
                        double y = (ticks % 30) * 0.1;
                        Location pLoc = targetLoc.clone().add(x, y, z);
                        targetLoc.getWorld().spawnParticle(Particle.CLOUD, pLoc, 2, 0.05, 0.05, 0.05, 0.02);
                        targetLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pLoc, 1, 0, 0, 0, 0);
                    }

                    // Przyciąganie wrogów w promieniu 8 bloków i nakładanie Stanu Powietrza
                    for (org.bukkit.entity.Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, 8.0, 5.0, 8.0)) {
                        if (e instanceof LivingEntity target && !e.equals(player)) {
                            Vector pull = targetLoc.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.45).setY(0.2);
                            target.setVelocity(pull);
                            ElementStatusManager.applyElementStatus(target, SpellElement.AIR, 6.0);
                        }
                    }
                }
            }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
        }
    }
}
