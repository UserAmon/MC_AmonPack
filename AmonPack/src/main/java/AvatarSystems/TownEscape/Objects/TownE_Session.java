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

    public TownE_Session(Location playerSpawn, List<Player> Players, List<TownE_Room> rooms, List<TownE_Round> rounds) {
        PlayerSpawn = playerSpawn;
        Rooms.addAll(rooms);
        players.addAll(Players);
        Rounds.addAll(rounds);
        for (Player p : players) {
            p.teleport(PlayerSpawn);
        }
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
        SessionState = TownE_State.END;
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
                        NextRound();
                    } else {
                        for (Player p : players) {
                            p.sendTitle("§4Do boju!", "§6Zaczeła się gra!", 0, 20, 0);
                        }
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
    public void handleEntityDeath(org.bukkit.entity.Entity entity) {
        if (aliveEnemyIds.contains(entity.getUniqueId())) {
            aliveEnemyIds.remove(entity.getUniqueId());
            if (aliveEnemyIds.isEmpty() && isSpawningFinished) {
                StartCountdown(true);
            }
        }
    }
}
