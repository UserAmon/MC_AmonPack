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
     * Pastes a schematic file onto the specified Bukkit world at coordinates (x, y, z).
     * Integrates with WorldEdit/FAWE.
     */
    public static boolean pasteSchematic(World world, String schematicName, int x, int y, int z, Plugin plugin) {
        File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }

        File file = new File(schematicsFolder, schematicName);
        if (!file.exists()) {
            // Check if schematicName has extension, if not try adding .schem and .schematic
            if (!schematicName.contains(".")) {
                file = new File(schematicsFolder, schematicName + ".schem");
                if (!file.exists()) {
                    file = new File(schematicsFolder, schematicName + ".schematic");
                }
            }
        }

        if (!file.exists()) {
            System.err.println("[Dungeons] Plik schematic nie istnieje: " + file.getAbsolutePath());
            return false;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            System.err.println("[Dungeons] Nieznany format pliku schematic: " + file.getName());
            return false;
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            
            // Adapt the Bukkit world to WorldEdit's World interface
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                // Paste the schematic
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(x, y, z))
                        .ignoreAirBlocks(false) // Paste air blocks as well to clear existing terrain if any
                        .build();
                
                Operations.complete(operation);
                System.out.println("[Dungeons] Pomyslnie wklejono schematic: " + file.getName() + " na swiecie: " + world.getName());
                return true;
            } catch (Exception e) {
                System.err.println("[Dungeons] Blad podczas wklejania sesji EditSession dla schematu " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } catch (IOException e) {
            System.err.println("[Dungeons] Blad I/O podczas czytania schematu " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
