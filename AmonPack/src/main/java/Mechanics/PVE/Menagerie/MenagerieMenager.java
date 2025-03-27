package Mechanics.PVE.Menagerie;

import methods_plugins.AmonPackPlugin;
import Mechanics.PVE.Menagerie.Objectives.Enemy;
import Mechanics.PVE.Menagerie.Objectives.ObjectiveConditions;
import Mechanics.PVE.Menagerie.Objectives.ObjectiveEffect;
import Mechanics.PVE.Menagerie.Objectives.Objectives;
import Mechanics.Skills.BendingGuiMenu;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MenagerieMenager {
    public static List<Menagerie> ListOfAllMenageries;
    static MenagerieConfig mc = new MenagerieConfig();
    public MenagerieMenager() {
        ReloadMenageries();
    }
    public void ReloadMenageries(){
        ListOfAllMenageries = new ArrayList<>();
        List<String>Worlds=new ArrayList<>();
        File baseFolder = new File(Bukkit.getWorldContainer(), "MultiWorlds");
        if (baseFolder.exists() && baseFolder.isDirectory()) {
            for (File folder : baseFolder.listFiles()) {
                if (folder.isDirectory()) {
                    for (File worldFolder : folder.listFiles()) {
                        if (worldFolder.isDirectory() && new File(worldFolder, "level.dat").exists()) {
                            Worlds.add("MultiWorlds/" + folder.getName() + "/" + worldFolder.getName());
                        }}}}}
        ListOfAllMenageries.addAll(mc.LoadMenageriesUponStart(Worlds));
    }
    public static void StartMenagerie(List<Player> p, String name){
        Menagerie ToCopy=null;
        boolean copy =false;
        for (Menagerie mena:ListOfAllMenageries) {
            if(mena.getMenagerieName().equalsIgnoreCase(name)){
                ToCopy=mena;
                if(mena.GetPlayersList().isEmpty()){
                    mena.StartMenagerie(p);
                    return;
                }else{
                    copy=true;
                }
            }
        }
        if(copy)MenaerieCopy(ToCopy,p);
    }

    private static void MenaerieCopy(Menagerie mena, List<Player> p){
        if(mena==null)return;
        System.out.println("Menazeria zajeta, kopiowanie swiata...");
        String originalWorldName = mena.getCenterLocation().getWorld().getName();
        String newWorldName = generateNewWorldName(originalWorldName);
        try {
            File originalWorldFolder = new File(Bukkit.getWorldContainer(), originalWorldName);
            File newWorldFolder = new File(Bukkit.getWorldContainer(), newWorldName);
            copyWorldFolder(originalWorldFolder, newWorldFolder);
            World newWorld = Bukkit.createWorld(new WorldCreator(newWorldName));
            if (newWorld != null) {
                Menagerie menagerie = mc.MenagerieFromWorldName(newWorldName);
                ListOfAllMenageries.add(menagerie);
                menagerie.StartMenagerie(p);
            }else {
                System.err.println("Nie mozna zaladowac swiata: " + newWorldName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyWorldFolder(File source, File target) throws IOException {
        if (!source.exists()) throw new IllegalArgumentException("Source world folder does not exist!");
        if (target.exists()) throw new IllegalArgumentException("Target world folder already exists!");
        Files.walk(source.toPath()).forEach(path -> {
            try {
                Files.copy(path, target.toPath().resolve(source.toPath().relativize(path)));
            } catch (IOException e) {
                throw new RuntimeException("Error copying world folder", e);
            }
        });
        File uidFile = new File(target, "uid.dat");
        if (uidFile.exists()) {
            if (!uidFile.delete()) {
                throw new IOException("Failed to delete uid.dat in copied world folder!");
            }
        }
    }

    private static String generateNewWorldName(String baseName) {
        String basePart = baseName;
        int number = 2;
        int lastDigitIndex = findLastDigitIndex(baseName);
        if (lastDigitIndex != -1) {
            basePart = baseName.substring(0, lastDigitIndex + 1);
            String numberPart = baseName.substring(lastDigitIndex + 1);
            try {
                number = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException e) {
                number = 1;
            }
        }
        String newName = basePart + number;
        while (new File(Bukkit.getWorldContainer(), newName).exists()) {
            number++;
            newName = basePart + number;
        }
        return newName;
    }
    private static int findLastDigitIndex(String str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            if (!Character.isDigit(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

}