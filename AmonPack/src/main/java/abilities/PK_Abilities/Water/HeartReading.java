package Abilities.PK_Abilities.Water;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HeartReading extends BloodAbility implements AddonAbility {

    private long cooldown;
    private long copyDurationMs;
    private String copiedAbilityName = null;
    private long copyTime = 0L;

    private Random random = new Random();

    public HeartReading(Player player) {
        super(player);

        if (hasAbility(player, HeartReading.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.HeartReading.Cooldown", 12000L);
        this.copyDurationMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.HeartReading.CopyDurationMs", 10000L);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (copiedAbilityName != null) {
            long elapsed = System.currentTimeMillis() - copyTime;
            if (elapsed >= copyDurationMs) {
                copiedAbilityName = null;
                bPlayer.addCooldown(this, cooldown);
                remove();
                return;
            }

            long remainingSec = Math.max(0, (copyDurationMs - elapsed) / 1000L);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§c🩸 Skopiowano: §e" + copiedAbilityName + " §7[§c" + remainingSec + "s§7] (Użyj LPM/Shift)"));
        }
    }

    public void onHitEntity(LivingEntity victim) {
        if (copiedAbilityName != null) {
            triggerCopiedAbility();
            return;
        }

        // Render "X" claw scratch lines on victim's chest
        renderClawScratch(victim.getLocation().add(0, 1.0, 0));
        player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);

        if (victim instanceof Player targetPlayer) {
            BendingPlayer targetBPlayer = BendingPlayer.getBendingPlayer(targetPlayer);
            if (targetBPlayer != null) {
                Map<Integer, String> targetAbilities = targetBPlayer.getAbilities();
                List<String> availableToCopy = new ArrayList<>();

                for (String abi : targetAbilities.values()) {
                    if (abi != null && !abi.isEmpty() && !abi.equalsIgnoreCase("HeartReading")) {
                        if (!bPlayer.getAbilities().containsValue(abi)) {
                            availableToCopy.add(abi);
                        }
                    }
                }

                if (!availableToCopy.isEmpty()) {
                    this.copiedAbilityName = availableToCopy.get(random.nextInt(availableToCopy.size()));
                    this.copyTime = System.currentTimeMillis();
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                } else {
                    bPlayer.addCooldown(this, cooldown);
                    remove();
                }
            } else {
                bPlayer.addCooldown(this, cooldown);
                remove();
            }
        } else {
            bPlayer.addCooldown(this, cooldown);
            remove();
        }
    }

    public void onUseSlot() {
        if (copiedAbilityName != null) {
            triggerCopiedAbility();
        }
    }

    private void triggerCopiedAbility() {
        if (copiedAbilityName == null) return;

        String abiToExec = copiedAbilityName;
        copiedAbilityName = null;

        CoreAbility coreAbility = CoreAbility.getAbility(abiToExec);
        if (coreAbility != null) {
            try {
                coreAbility.getClass().getConstructor(Player.class).newInstance(player);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.2f);
            } catch (Exception e) {
                // Fallback invocation
            }
        }

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    private void renderClawScratch(Location center) {
        Particle.DustOptions bloodRed = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f);
        // Line 1: Top-Left to Bottom-Right
        for (double d = -0.4; d <= 0.4; d += 0.1) {
            Location p = center.clone().add(d, d, 0);
            center.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, bloodRed);
        }
        // Line 2: Bottom-Left to Top-Right
        for (double d = -0.4; d <= 0.4; d += 0.1) {
            Location p = center.clone().add(d, -d, 0);
            center.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, bloodRed);
        }
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
        return "HeartReading";
    }

    @Override
    public boolean isSneakAbility() {
        return true;
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
        return "Uderzenie wroga łapką z tym ruchem tworzy na nim krwawy krzyżyk i kopiuje jeden z jego ruchów na X sekund, pozwalając go wywołać.";
    }

    @Override
    public String getInstructions() {
        return "Uderz wroga LPM aby odczytać jego serce i skopiować ruch, a następnie użyj go!";
    }
}
