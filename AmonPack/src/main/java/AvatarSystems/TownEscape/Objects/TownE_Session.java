package AvatarSystems.TownEscape.Objects;

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
    private final List<TownE_Round> Rounds = new ArrayList<>();
    private TownE_State SessionState =  TownE_State.WAITING;
    private Location PlayerSpawn;
    private int RoundNumber;

    public TownE_Session(Location playerSpawn,List<Player> Players, List<TownE_Room> rooms, List<TownE_Round> rounds) {
        PlayerSpawn = playerSpawn;
        Rooms.addAll(rooms);
        players.addAll(Players);
        Rounds.addAll(rounds);
        for (Player p : players){
            p.teleport(PlayerSpawn);
        }
        StartCountdown();
    }



    private void NextRound(){
        if(ActiveRooms.isEmpty()){
            System.out.println("Nie ma pierwszego pokoju wczytanego, terminacja!");
            end();
            return;
        }
        RoundNumber++;
        TownE_Round ActiveRound = Rounds.stream().filter(townERound -> townERound.getRoundNumber()==RoundNumber).findFirst().orElse(null);
        if(ActiveRound==null){
            System.out.println("Koniec rund!, terminacja! Doszedles do: "+RoundNumber);
            end();
            return;
        }
        List<Location> PossibleSpawns = new ArrayList<>();
        for (TownE_Room activeroom : ActiveRooms){
            PossibleSpawns.addAll(activeroom.getEnemySpawnAreas());
        }
        if(PossibleSpawns.isEmpty()){
            System.out.println("Nie ma zadnego spawnu mobow, terminacja!");
            end();
            return;
        }
        spawnRoundEnemiesTimed(ActiveRound, PossibleSpawns);
    }


    public void start() {
        SessionState = TownE_State.STARTED;
        for (TownE_Room room : Rooms){
            if(room.getArenaName().startsWith("TEStart")){
                room.SetActive();
                ActiveRooms.add(room);
                break;
            }}
        RoundNumber=0;
        NextRound();
    }
    public void end() {
        SessionState = TownE_State.END;
    }
    public enum TownE_State{
        WAITING,
        STARTED,
        END
    }
    private void StartCountdown(){
        new BukkitRunnable() {
            int time = 20;
            @Override
            public void run() {
                if(players.isEmpty()){
                    System.out.println("Anulowano Sesje gry "+SessionID);
                    cancel();
                    return;
                }
                if (time <= 0) {
                    for (Player p : players) {
                        p.sendTitle("§4Do boju!", "§6Zaczeła się gra!", 0, 20, 0);
                    }
                    start();
                    cancel();
                    return;
                }
                Bukkit.broadcastMessage("§eStart za §c" + time + " §esekund!");
                if (time <= 5) {
                    for (Player p : players) {
                        p.sendTitle("§c" + time, "§7Start gry", 0, 20, 0);
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
                    return;
                }
                if (spawns.isEmpty()) return;
                Collections.shuffle(spawns);
                int locationsToUse = Math.min(
                        round.getHowManyLocationFrom(),
                        spawns.size()
                );
                int leftToSpawn = round.getNumbersOfEnemies() - spawned;
                for (int i = 0; i < locationsToUse && leftToSpawn > 0; i++) {
                    Location loc = spawns.get(i);
                    int spawnedNow = round.SpawnEnemies(loc);
                    spawned += spawnedNow;
                    leftToSpawn -= spawnedNow;
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, round.getTimeBetweenSpawn() * 20L);
    }


}
