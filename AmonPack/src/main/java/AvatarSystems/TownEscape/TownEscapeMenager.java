package AvatarSystems.TownEscape;

import AvatarSystems.TownEscape.Objects.TownE_GameMap;
import AvatarSystems.TownEscape.Objects.TownE_Room;
import AvatarSystems.TownEscape.Objects.TownE_Round;
import AvatarSystems.TownEscape.Objects.TownE_Interactable;
import AvatarSystems.TownEscape.Objects.TownE_Session;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.io.File;
import java.util.*;

public class TownEscapeMenager {
    private static TownEscapeMenager instance;
    private final File dataFolder;
    private final Map<String, TownE_GameMap> loadedMaps = new HashMap<>();
    private final List<TownE_Session> ActiveSesions = new ArrayList<>();
    private final Map<UUID, TownE_Session> playerSession = new HashMap<>();

    public TownEscapeMenager(File dataFolder) {
        this.dataFolder = dataFolder;
        instance = this;
        LoadConfig();
    }

    public static TownEscapeMenager getInstance() {
        return instance;
    }

    public void LoadConfig() {
        loadedMaps.clear();
        File rpgFolder = new File(dataFolder, "RPG");
        if (!rpgFolder.exists())
            return;

        File[] files = rpgFolder.listFiles((dir, name) -> name.startsWith("TownEscape_") && name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            try {
                org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                        .loadConfiguration(file);
                String mapName = file.getName().replace("TownEscape_", "").replace(".yml", "");
                Location playerSpawn = null;
                if (config.contains("PlayerSpawn")) {
                    List<?> spawnList = config.getList("PlayerSpawn");
                    if (spawnList != null && spawnList.size() >= 4) {
                        double x = ((Number) spawnList.get(0)).doubleValue();
                        double y = ((Number) spawnList.get(1)).doubleValue();
                        double z = ((Number) spawnList.get(2)).doubleValue();
                        String worldName = (String) spawnList.get(3);
                        playerSpawn = new Location(org.bukkit.Bukkit.getWorld(worldName), x, y, z);
                    }
                }
                List<TownE_Room> rooms = new ArrayList<>();
                if (config.contains("Rooms")) {
                    for (String roomKey : config.getConfigurationSection("Rooms").getKeys(false)) {
                        String path = "Rooms." + roomKey;
                        String name = config.getString(path + ".Name");

                        List<Location> spawns = new ArrayList<>();
                        if (config.contains(path + ".Spawns")) {
                            List<?> spawnLists = config.getList(path + ".Spawns");
                            if (spawnLists != null) {
                                for (Object obj : spawnLists) {
                                    if (obj instanceof List) {
                                        List<?> coords = (List<?>) obj;
                                        if (coords.size() >= 3 && playerSpawn != null) {
                                            double x = ((Number) coords.get(0)).doubleValue();
                                            double y = ((Number) coords.get(1)).doubleValue();
                                            double z = ((Number) coords.get(2)).doubleValue();
                                            spawns.add(new Location(playerSpawn.getWorld(), x, y, z));
                                        }
                                    }
                                }
                            }
                        }

                        List<AvatarSystems.TownEscape.Objects.TownE_Interactable> interactables = new ArrayList<>();
                        if (config.contains(path + ".Interactables")) {
                            List<Map<?, ?>> interactableList = config.getMapList(path + ".Interactables");
                            for (Map<?, ?> map : interactableList) {
                                String typeStr = (String) map.get("Type");
                                AvatarSystems.TownEscape.Objects.TownE_Interactable.TE_InteractableTypes type = AvatarSystems.TownEscape.Objects.TownE_Interactable.TE_InteractableTypes
                                        .valueOf(typeStr);
                                int radius = (Integer) map.get("Radius");
                                List<?> locList = (List<?>) map.get("Location");
                                double x = ((Number) locList.get(0)).doubleValue();
                                double y = ((Number) locList.get(1)).doubleValue();
                                double z = ((Number) locList.get(2)).doubleValue();
                                Location loc = new Location(playerSpawn.getWorld(), x, y, z);

                                int cost = 0;
                                Location loc2 = null;
                                org.bukkit.Material mat = null;

                                if (type == AvatarSystems.TownEscape.Objects.TownE_Interactable.TE_InteractableTypes.DOORS) {
                                    if (map.containsKey("Cost"))
                                        cost = (Integer) map.get("Cost");
                                    if (map.containsKey("Material"))
                                        mat = org.bukkit.Material.valueOf((String) map.get("Material"));
                                    if (map.containsKey("Location2")) {
                                        List<?> locList2 = (List<?>) map.get("Location2");
                                        double x2 = ((Number) locList2.get(0)).doubleValue();
                                        double y2 = ((Number) locList2.get(1)).doubleValue();
                                        double z2 = ((Number) locList2.get(2)).doubleValue();
                                        loc2 = new Location(playerSpawn.getWorld(), x2, y2, z2);
                                    }
                                }

                                interactables.add(
                                        new AvatarSystems.TownEscape.Objects.TownE_Interactable(radius, loc, type, cost,
                                                loc2, mat));
                            }
                        }
                        rooms.add(new TownE_Room(name, spawns, interactables));
                    }
                }
                List<AvatarSystems.TownEscape.Objects.TownE_Round> rounds = new ArrayList<>();
                if (config.contains("Rounds")) {
                    for (String roundKey : config.getConfigurationSection("Rounds").getKeys(false)) {
                        String path = "Rounds." + roundKey;
                        int enemies = config.getInt(path + ".Enemies");
                        int timeBetween = config.getInt(path + ".TimeBetweenSpawn");
                        int locsFrom = config.getInt(path + ".LocationsFrom");
                        List<String> typesStr = config.getStringList(path + ".Types");
                        List<org.bukkit.entity.EntityType> types = new ArrayList<>();
                        for (String t : typesStr) {
                            types.add(org.bukkit.entity.EntityType.valueOf(t));
                        }
                        int roundNum = Integer.parseInt(roundKey.replace("Round", ""));
                        rounds.add(new AvatarSystems.TownEscape.Objects.TownE_Round(types, enemies, roundNum,
                                timeBetween, locsFrom));
                    }
                }
                if (playerSpawn != null) {
                    loadedMaps.put(mapName, new TownE_GameMap(mapName, playerSpawn, rooms, rounds));
                    System.out.println("Loaded TownEscape map: " + mapName);
                }
            } catch (Exception e) {
                System.out.println("Error loading TownEscape config: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    public void StartGame(Player p, String mapName) {
        TownE_GameMap map = loadedMaps.get(mapName);
        if (map == null) {
            p.sendMessage("§cMapa nie znaleziona: " + mapName);
            return;
        }

        TownE_Session session = new TownE_Session(map.getPlayerSpawn(), Collections.singletonList(p), map.getRooms(),
                map.getRounds());
        assignPlayer(p, session);
        ActiveSesions.add(session);
        session.start();
    }

    public TownE_Session getSessionByPlayer(Player p) {
        return playerSession.get(p.getUniqueId());
    }

    public void assignPlayer(Player p, TownE_Session session) {
        playerSession.put(p.getUniqueId(), session);
    }

    public List<TownE_Session> getActiveSessions() {
        return ActiveSesions;
    }

    public void removeSession(TownE_Session session) {
        ActiveSesions.remove(session);
        // Also remove from playerSession map?
        // Iterating map to remove values is slow, but safer.
        playerSession.values().removeIf(s -> s.equals(session));
    }

    public void shutdown() {
        for (TownE_Session session : new ArrayList<>(ActiveSesions)) {
            session.end();
        }
        ActiveSesions.clear();
        playerSession.clear();
    }
}
