package CustomContent.Pack;

import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class PackListener implements Listener {

    private final PackManager packManager;

    public PackListener(PackManager packManager) {
        this.packManager = packManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (packManager.isAutoSendOnJoin()) {
            // Krótkie opóźnienie 20 ticków (1 sekunda) po wejściu gracza na serwer
            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                if (player.isOnline()) {
                    packManager.applyToPlayer(player);
                }
            }, 20L);
        }
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        Bukkit.getLogger().info("[AmonPack] Status pobierania ResourcePacka gracza " + player.getName() + ": " + status);

        if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            player.sendMessage("§a[AmonPack] §7Paczka zasobów i modeli 3D została pomyślnie załadowana!");
        } else if (status == PlayerResourcePackStatusEvent.Status.DECLINED) {
            player.sendMessage("§e[AmonPack] §7Odrzuciłeś paczkę zasobów. Możesz ją włączyć wpisując §f/amon pack apply");
        } else if (status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            player.sendMessage("§c[AmonPack] §7Nie udało się pobrać paczki zasobów. Sprawdź połączenie.");
        }
    }
}
