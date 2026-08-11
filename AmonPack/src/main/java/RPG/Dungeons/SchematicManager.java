package RPG.Dungeons;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SchematicManager {

    /**
     * Reads a schematic file into a WorldEdit Clipboard.
     */
    public static Clipboard readClipboard(String schematicName, Plugin plugin) {
        File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }

        File file = new File(schematicsFolder, schematicName);
        if (!file.exists()) {
            if (!schematicName.contains(".")) {
                file = new File(schematicsFolder, schematicName + ".schem");
                if (!file.exists()) {
                    file = new File(schematicsFolder, schematicName + ".schematic");
                }
            }
        }

        if (!file.exists()) {
            System.err.println("[Dungeons] Plik schematic nie istnieje: " + file.getAbsolutePath());
            return null;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            System.err.println("[Dungeons] Nieznany format pliku schematic: " + file.getName());
            return null;
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException e) {
            System.err.println("[Dungeons] Blad I/O podczas czytania schematu " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Pastes a loaded Clipboard into the specified Bukkit world at coordinates (x, y, z).
     */
    public static boolean pasteClipboard(World world, Clipboard clipboard, int x, int y, int z) {
        if (world == null || clipboard == null) {
            return false;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            editSession.setFastMode(true);

            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(x, y, z))
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(operation);
            System.out.println("[Dungeons] Pomyślnie wklejono schematic na świecie: " + world.getName());
            return true;
        } catch (Exception e) {
            System.err.println("[Dungeons] Błąd podczas wklejania sesji EditSession: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Pastes a schematic file onto the specified Bukkit world at coordinates (x, y, z).
     * Retained for backward compatibility.
     */
    public static boolean pasteSchematic(World world, String schematicName, int x, int y, int z, Plugin plugin) {
        Clipboard clipboard = readClipboard(schematicName, plugin);
        if (clipboard == null) {
            return false;
        }
        return pasteClipboard(world, clipboard, x, y, z);
    }
}
