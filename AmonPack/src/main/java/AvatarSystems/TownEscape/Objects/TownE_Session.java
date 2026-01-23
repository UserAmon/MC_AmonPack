package AvatarSystems.TownEscape.Objects;

import dev.lone.itemsadder.Core.OtherPlugins.MythicMobs.MythicMobsHook;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TownE_Session {
    private final UUID SessionID = UUID.randomUUID();
    private final List<TownE_Room> Rooms = new ArrayList<>();
    private final List<TownE_Room> ActiveRooms = new ArrayList<>();
    private final Set<Player> players = new HashSet<Player>();
    private final Set<UUID> aliveEnemyIds = new HashSet<>();
    private final List<TownE_Round> Rounds = new ArrayList<>();
    private TownE_State SessionState = TownE_State.WAITING;
    private Location PlayerSpawn;
    private int RoundNumber;
    private boolean isSpawningFinished = false;

    private final Map<UUID, org.bukkit.inventory.ItemStack[]> savedInventories = new HashMap<>();

    public TownE_Session(Location playerSpawn, List<Player> Players, List<TownE_Room> rooms, List<TownE_Round> rounds) {
        PlayerSpawn = playerSpawn;
        Rooms.addAll(rooms);
        players.addAll(Players);
        Rounds.addAll(rounds);
        for (Player p : players) {
            savedInventories.put(p.getUniqueId(), p.getInventory().getContents());
            p.getInventory().clear();
            p.teleport(PlayerSpawn);
            spiritEnergy.put(p.getUniqueId(), 0);
            updateEnergyItem(p);
        }
        initializeDoors();
        StartCountdown(false);
    }

    private void NextRound() {
        if (ActiveRooms.isEmpty()) {
            System.out.println("Nie ma pierwszego pokoju wczytanego, terminacja!");
            end();
            return;
        }
        RoundNumber++;
        isSpawningFinished = false;
        TownE_Round ActiveRound = Rounds.stream().filter(townERound -> townERound.getRoundNumber() == RoundNumber)
                .findFirst().orElse(null);
        if (ActiveRound == null) {
            System.out.println("Koniec rund!, terminacja! Doszedles do: " + RoundNumber);
            end();
            return;
        }
        List<Location> PossibleSpawns = new ArrayList<>();
        for (TownE_Room activeroom : ActiveRooms) {
            PossibleSpawns.addAll(activeroom.getEnemySpawnAreas());
        }
        if (PossibleSpawns.isEmpty()) {
            System.out.println("Nie ma zadnego spawnu mobow, terminacja!");
            end();
            return;
        }
        spawnRoundEnemiesTimed(ActiveRound, PossibleSpawns);
    }

    public void start() {
        SessionState = TownE_State.STARTED;
        for (TownE_Room room : Rooms) {
            if (room.getArenaName().startsWith("TEStart")) {
                ActiveRooms.add(room);
                break;
            }
        }
        RoundNumber = 0;
        NextRound();
    }

    public void end() {
        for (Player p : players) {
            if (savedInventories.containsKey(p.getUniqueId())) {
                p.getInventory().setContents(savedInventories.get(p.getUniqueId()));
            }
        }
        SessionState = TownE_State.END;
        AvatarSystems.TownEscape.TownEscapeMenager.getInstance().removeSession(this);
    }

    public void removePlayer(Player p) {
        if (players.contains(p)) {
            if (savedInventories.containsKey(p.getUniqueId())) {
                p.getInventory().setContents(savedInventories.get(p.getUniqueId()));
                savedInventories.remove(p.getUniqueId());
            }
            players.remove(p);
            if (players.isEmpty()) {
                end();
            }
        }
    }

    public enum TownE_State {
        WAITING,
        STARTED,
        END
    }

    private void StartCountdown(boolean nextRound) {
        new BukkitRunnable() {
            int time = nextRound ? 10 : 20;

            @Override
            public void run() {
                if (players.isEmpty()) {
                    System.out.println("Anulowano Sesje gry " + SessionID);
                    cancel();
                    return;
                }
                if (time <= 0) {
                    if (nextRound) {
                        for (Player p : players) {
                            p.sendTitle("§6Runda " + (RoundNumber + 1), "§7Zaczyna się!", 0, 20, 0);
                        }
                        System.out.println("§4Teraz zaczęto kolejną runde!");
                        NextRound();
                    } else {
                        for (Player p : players) {
                            p.sendTitle("§4Do boju!", "§6Zaczeła się gra!", 0, 20, 0);
                        }
                        System.out.println("§6START GRY!");
                        start();
                    }
                    cancel();
                    return;
                }

                if (nextRound) {
                    Bukkit.broadcastMessage("§eKolejna runda za §c" + time + " §esekund!");
                } else {
                    Bukkit.broadcastMessage("§eStart za §c" + time + " §esekund!");
                }

                if (time <= 5) {
                    for (Player p : players) {
                        p.sendTitle("§c" + time, nextRound ? "§7Kolejna runda" : "§7Start gry", 0, 20, 0);
                    }
                }
                time--;
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 20L);
    }

    private void spawnRoundEnemiesTimed(TownE_Round round, List<Location> possibleSpawns) {
        List<Location> spawns = new ArrayList<>(possibleSpawns);
        new BukkitRunnable() {
            int spawned = 0;

            @Override
            public void run() {
                if (SessionState != TownE_State.STARTED) {
                    cancel();
                    return;
                }
                if (spawned >= round.getNumbersOfEnemies()) {
                    cancel();
                    System.out.println("Runda " + round.getRoundNumber() + " - zakończono spawn mobów.");
                    isSpawningFinished = true;
                    return;
                }
                if (spawns.isEmpty())
                    return;
                Collections.shuffle(spawns);
                int locationsToUse = Math.min(
                        round.getHowManyLocationFrom(),
                        spawns.size());
                int leftToSpawn = round.getNumbersOfEnemies() - spawned;
                for (int i = 0; i < locationsToUse && leftToSpawn > 0; i++) {
                    Location loc = spawns.get(i);
                    List<org.bukkit.entity.Entity> spawnedEntities = round.SpawnEnemies(loc);
                    System.out.println("§4Teraz spawn mobow jest! zrespiono:  " + spawnedEntities.size());
                    for (org.bukkit.entity.Entity e : spawnedEntities) {
                        aliveEnemyIds.add(e.getUniqueId());
                    }
                    int spawnedNow = spawnedEntities.size();
                    spawned += spawnedNow;
                    leftToSpawn -= spawnedNow;
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, round.getTimeBetweenSpawn() * 20L);
    }

    private final Map<UUID, Integer> spiritEnergy = new HashMap<>();

    public void addEnergy(Player p, int amount) {
        spiritEnergy.put(p.getUniqueId(), spiritEnergy.getOrDefault(p.getUniqueId(), 0) + amount);
        updateEnergyItem(p);
    }

    public void removeEnergy(Player p, int amount) {
        int current = spiritEnergy.getOrDefault(p.getUniqueId(), 0);
        if (current >= amount) {
            spiritEnergy.put(p.getUniqueId(), current - amount);
            updateEnergyItem(p);
        }
    }

    public int getEnergy(Player p) {
        return spiritEnergy.getOrDefault(p.getUniqueId(), 0);
    }

    private void updateEnergyItem(Player p) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_STAR);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bEnergia Duchowa: §e" + getEnergy(p));
        item.setItemMeta(meta);
        p.getInventory().setItem(8, item);
    }

    private void initializeDoors() {
        for (TownE_Room room : Rooms) {
            for (TownE_Interactable interactable : room.getInteractable()) {
                if (interactable.getType() == TownE_Interactable.TE_InteractableTypes.DOORS
                        && interactable.getLocation2() != null) {
                    fillDoor(interactable, interactable.getDoorMaterial());
                }
            }
        }
    }

    private void fillDoor(TownE_Interactable door, org.bukkit.Material mat) {
        Location loc1 = door.getLocation();
        Location loc2 = door.getLocation2();
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    loc1.getWorld().getBlockAt(x, y, z).setType(mat);
                }
            }
        }
    }

    public void handleInteract(Player p, org.bukkit.block.Block block) {
        for (TownE_Room room : ActiveRooms) {
            for (TownE_Interactable interactable : room.getInteractable()) {
                if (interactable.getType() == TownE_Interactable.TE_InteractableTypes.DOORS) {
                    if (interactable.PlayerHasInteracted(block)) {
                        if (getEnergy(p) >= interactable.getCost()) {
                            removeEnergy(p, interactable.getCost());
                            fillDoor(interactable, org.bukkit.Material.AIR);
                            p.sendMessage("§aOtworzyłeś drzwi!");
                            // Remove interactable from list so it can't be interacted with again?
                            // Or just check if it's already open (AIR)?
                            // Better to remove or mark as open.
                            // For now, let's just set to AIR. If they pay again, they pay for nothing (or
                            // we check block type).
                        } else {
                            p.sendMessage("§cNie masz wystarczająco energii! Koszt: " + interactable.getCost());
                        }
                        return;
                    }
                }
            }
        }
    }

    public void handleDamage(Player p, double damage) {
        addEnergy(p, (int) damage);
    }

    public void handleKill(Player p, org.bukkit.entity.Entity entity) {
        addEnergy(p, 10); // Bonus for kill
        handleEntityDeath(entity);
    }

    public void handleEntityDeath(org.bukkit.entity.Entity entity) {
        if (aliveEnemyIds.contains(entity.getUniqueId())) {
            aliveEnemyIds.remove(entity.getUniqueId());
            if (aliveEnemyIds.isEmpty() && isSpawningFinished) {
                StartCountdown(true);
            }
        }
    }
}
