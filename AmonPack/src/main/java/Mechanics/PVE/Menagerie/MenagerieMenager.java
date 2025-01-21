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
    public static final List<Menagerie> ListOfAllMenageries  = new ArrayList<>();
    public MenagerieMenager() {
        MenagerieConfig mc = new MenagerieConfig();
        ListOfAllMenageries.addAll(mc.MenagerieFromConfig());

    }
    public static void StartMenagerie(Player p, String name){
        System.out.println("Start Menagerie");
        for (Menagerie mena:ListOfAllMenageries) {
            if(mena.getMenagerieName().equalsIgnoreCase(name)){
                boolean empty = true;
                    for (Player pla:Bukkit.getOnlinePlayers()) {
                        if (pla.getWorld().equals(mena.getCenterLocation().getWorld())) {
                            MenaerieCopy(mena,p);
                            empty=false;
                            break;
                        }
                        }
                    if(empty)mena.StartMenagerie(p);
                    break;
            }
        }
    }

    private static void MenaerieCopy(Menagerie mena, Player p){
        p.sendMessage("Menazeria jest zajeta, tworze nowa kopie...");
        System.out.println("Menazeria zajeta, kopiowanie swiata...");
        String originalWorldName = mena.getCenterLocation().getWorld().getName();
        String newWorldName = generateNewWorldName(originalWorldName);
        try {
            File originalWorldFolder = new File(Bukkit.getWorldContainer(), originalWorldName);
            File newWorldFolder = new File(Bukkit.getWorldContainer(), newWorldName);
            copyWorldFolder(originalWorldFolder, newWorldFolder);
            World newWorld = Bukkit.createWorld(new WorldCreator(newWorldName));
            if (newWorld != null) {
                p.teleport(mena.getCenterLocation());//dodac menagerie bez wgzledu na swiat, byle by zaczynal sie poprawnie
                p.sendMessage("Zostales przeniesiony do nowej menazerii: " + newWorldName);
            } else {
                p.sendMessage("Blad podczas ladowania nowej menazerii.");
                System.err.println("Nie mozna zaladowac swiata: " + newWorldName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            p.sendMessage("Wystapil blad podczas kopiowania swiata.");
        }
    }

    // Metoda do kopiowania folderu świata
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

        // Usuń plik uid.dat z nowego folderu świata
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