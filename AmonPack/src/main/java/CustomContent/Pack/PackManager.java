package CustomContent.Pack;

import Plugin.AmonPackPlugin;
import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class PackManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private File packDir;
    private File generatedZip;
    private byte[] packHash;
    private String packHashHex;
    private PackHttpServer httpServer;

    private int httpPort = 8085;
    private String hostAddress = "auto";
    private boolean autoSendOnJoin = true;
    private boolean forcePack = false;
    private String promptMessage = "§6§lAmonPack §7- Pobierz tekstury i modele 3D serwera!";
    private String detectedPublicIp = null;

    // Rejestr CustomModelData: MaterialName -> Map<CustomModelData, ModelPath>
    private final Map<String, Map<Integer, String>> vanillaOverrides = new LinkedHashMap<>();

    public PackManager() {
        this.packDir = new File(AmonPackPlugin.plugin.getDataFolder(), "pack");
        this.generatedZip = new File(AmonPackPlugin.plugin.getDataFolder(), "generated/AmonPack_ResourcePack.zip");
    }

    public void load() {
        loadConfig();
        exportDefaultAssets();
        buildResourcePack();

        if (httpServer == null || !httpServer.isRunning()) {
            httpServer = new PackHttpServer(httpPort, generatedZip);
            httpServer.start();
        }
    }

    public void unload() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
    }

    private void loadConfig() {
        File packFolder = new File(AmonPackPlugin.plugin.getDataFolder(), "pack");
        if (!packFolder.exists()) packFolder.mkdirs();

        File cfgFile = new File(packFolder, "pack_config.yml");
        if (!cfgFile.exists()) {
            AmonPackPlugin.plugin.saveResource("pack/pack_config.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(cfgFile);
        this.httpPort = cfg.getInt("PackServer.Port", 8085);
        this.hostAddress = cfg.getString("PackServer.Host", "auto");
        this.autoSendOnJoin = cfg.getBoolean("ResourcePack.AutoSendOnJoin", true);
        this.forcePack = cfg.getBoolean("ResourcePack.ForcePack", false);
        this.promptMessage = ChatColor.translateAlternateColorCodes('&', cfg.getString("ResourcePack.PromptMessage", "&6&lAmonPack &7- Pobierz paczkę zasobów z modelami 3D!"));
        detectPublicIpAsync();
    }

    private void detectPublicIpAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(AmonPackPlugin.plugin, () -> {
            String[] services = {
                    "https://api.ipify.org",
                    "https://checkip.amazonaws.com",
                    "https://icanhazip.com"
            };
            for (String s : services) {
                try {
                    java.net.URL url = new java.net.URL(s);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String ip = reader.readLine().trim();
                        if (ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                            detectedPublicIp = ip;
                            Bukkit.getLogger().info("[AmonPack] Wykryto publiczne IP serwera dla ResourcePacka: " + detectedPublicIp);
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    public void registerModelOverride(String baseMaterial, int customModelData, String modelPath) {
        String matKey = baseMaterial.toLowerCase(Locale.ROOT);
        vanillaOverrides.computeIfAbsent(matKey, k -> new TreeMap<>()).put(customModelData, modelPath);
    }

    private void exportDefaultAssets() {
        if (!packDir.exists()) {
            packDir.mkdirs();
        }
    }

    public void buildResourcePack() {
        try {
            Bukkit.getLogger().info("[AmonPack] Rozpoczynanie budowania czystego ResourcePacka...");

            File tempBuildDir = new File(AmonPackPlugin.plugin.getDataFolder(), "temp_pack_build");
            if (tempBuildDir.exists()) {
                deleteDirectory(tempBuildDir);
            }
            tempBuildDir.mkdirs();

            // 1. pack.mcmeta
            File mcmeta = new File(tempBuildDir, "pack.mcmeta");
            JsonObject mcMetaJson = new JsonObject();
            JsonObject packObj = new JsonObject();
            packObj.addProperty("pack_format", 34);
            JsonObject supportedFormats = new JsonObject();
            supportedFormats.addProperty("min_inclusive", 15);
            supportedFormats.addProperty("max_inclusive", 55);
            packObj.add("supported_formats", supportedFormats);
            packObj.addProperty("description", "AmonPack Custom 3D Models & Textures");
            mcMetaJson.add("pack", packObj);
            try (FileWriter writer = new FileWriter(mcmeta, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(mcMetaJson));
            }

            // 2. Struktura katalogów w paczce
            File amonpackModels = new File(tempBuildDir, "assets/amonpack/models");
            File amonpackTex = new File(tempBuildDir, "assets/amonpack/textures");
            File mcModelsItem = new File(tempBuildDir, "assets/minecraft/models/item");
            File mcAtlases = new File(tempBuildDir, "assets/minecraft/atlases");

            amonpackModels.mkdirs();
            amonpackTex.mkdirs();
            mcModelsItem.mkdirs();
            mcAtlases.mkdirs();

            // 2b. Atlas tekstur dla Minecraft 1.19.3 - 1.21.4+ (rejestracja wszystkich folderów w atlasie spriteów)
            JsonObject atlasRoot = new JsonObject();
            JsonArray sources = new JsonArray();
            String[] atlasDirs = {"item", "block", "weapons", "magic", "crafting", "gui"};
            for (String d : atlasDirs) {
                JsonObject srcObj = new JsonObject();
                srcObj.addProperty("type", "directory");
                srcObj.addProperty("source", d);
                srcObj.addProperty("prefix", d + "/");
                sources.add(srcObj);
            }
            atlasRoot.add("sources", sources);
            try (FileWriter writer = new FileWriter(new File(mcAtlases, "blocks.json"), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(atlasRoot));
            }

            // 3. Wyodrębnienie czystych modeli i tekstur wprost z JAR (pack/amonpack)
            extractJarResources("pack/amonpack/textures", amonpackTex);
            extractJarResources("pack/amonpack/models", amonpackModels);

            // 4. Rejestracja modeli z konfiguracji i domyślnych modeli
            // Broń (WOODEN_SWORD)
            registerModelOverride("wooden_sword", 10000, "amonpack:weapons/boomerang");
            registerModelOverride("wooden_sword", 10001, "amonpack:weapons/wachlarz");
            registerModelOverride("wooden_sword", 10002, "amonpack:weapons/laska_aanga");
            registerModelOverride("wooden_sword", 10003, "amonpack:weapons/bambus");
            registerModelOverride("wooden_sword", 10004, "amonpack:weapons/earth_hammer");
            registerModelOverride("wooden_sword", 10005, "amonpack:weapons/msokka");
            registerModelOverride("wooden_sword", 10006, "amonpack:weapons/wlocznia_ognia");
            registerModelOverride("wooden_sword", 10007, "amonpack:weapons/sztylet");
            registerModelOverride("wooden_sword", 10020, "amonpack:weapons/bone_sword");

            // Łuk (BOW)
            registerModelOverride("bow", 10021, "amonpack:weapons/custom_bow");
            registerModelOverride("bow", 20003, "amonpack:magic/staff_lightning");

            // Różdżka i magia (STICK, BOOK, ENCHANTED_BOOK, PRISMARINE_SHARD)
            registerModelOverride("stick", 20002, "amonpack:magic/wand_fen");
            registerModelOverride("stick", 20004, "amonpack:magic/wand_water");
            registerModelOverride("prismarine_shard", 20004, "amonpack:magic/wand_water");
            registerModelOverride("book", 10010, "amonpack:magic/tome_fire");
            registerModelOverride("book", 20001, "amonpack:magic/tome_fire");
            registerModelOverride("book", 20002, "amonpack:magic/wand_fen");
            registerModelOverride("book", 20004, "amonpack:magic/wand_water");
            registerModelOverride("enchanted_book", 10010, "amonpack:magic/tome_fire");
            registerModelOverride("enchanted_book", 20001, "amonpack:magic/tome_fire");
            registerModelOverride("enchanted_book", 20002, "amonpack:magic/wand_fen");
            registerModelOverride("enchanted_book", 20004, "amonpack:magic/wand_water");

            // Przedmioty rzemieślnicze (PAPER, FLINT)
            registerModelOverride("paper", 10001, "amonpack:crafting/mold_empty");
            registerModelOverride("paper", 10002, "amonpack:crafting/mold_full");
            registerModelOverride("paper", 10003, "amonpack:crafting/meteor_shard");
            registerModelOverride("paper", 10004, "amonpack:crafting/basalt_shard");
            registerModelOverride("paper", 10005, "amonpack:crafting/firescroll");
            registerModelOverride("flint", 10003, "amonpack:crafting/meteor_shard");

            // Narzędzia i zbroje
            registerModelOverride("diamond_sword", 10001, "amonpack:item/meteor_scythe");
            registerModelOverride("diamond_axe", 10002, "amonpack:item/meteor_axe");
            registerModelOverride("netherite_axe", 10002, "amonpack:item/meteor_axe");
            registerModelOverride("diamond_pickaxe", 10014, "amonpack:weapons/meteor_pickaxe");
            registerModelOverride("netherite_pickaxe", 10014, "amonpack:weapons/meteor_pickaxe");

            // Bloki (NOTE_BLOCK & IRON_NUGGET)
            registerModelOverride("note_block", 30001, "amonpack:block/meteoryt_ore");
            registerModelOverride("iron_nugget", 30001, "amonpack:block/meteoryt_ore");
            registerModelOverride("note_block", 30002, "amonpack:block/magic_crafting_table");
            registerModelOverride("iron_nugget", 30002, "amonpack:block/magic_crafting_table");
            registerModelOverride("note_block", 30003, "amonpack:block/basalt_ore");
            registerModelOverride("iron_nugget", 30003, "amonpack:block/basalt_ore");
            registerModelOverride("note_block", 30004, "amonpack:block/arcane_altar");
            registerModelOverride("iron_nugget", 30004, "amonpack:block/arcane_altar");

            File mcItems = new File(tempBuildDir, "assets/minecraft/items");
            mcItems.mkdirs();

            // 4b. Blockstates dla natywnego renderowania bloków w świecie gry (NOTE_BLOCK)
            File mcBlockstates = new File(tempBuildDir, "assets/minecraft/blockstates");
            mcBlockstates.mkdirs();
            JsonObject blockstatesRoot = new JsonObject();
            JsonObject variants = new JsonObject();

            JsonObject defaultModel = new JsonObject();
            defaultModel.addProperty("model", "minecraft:block/note_block");
            variants.add("", defaultModel);

            JsonObject tableModel = new JsonObject();
            tableModel.addProperty("model", "amonpack:block/magic_crafting_table");
            variants.add("instrument=bass,note=1,powered=false", tableModel);
            variants.add("instrument=bass,note=1,powered=true", tableModel);

            JsonObject meteorModel = new JsonObject();
            meteorModel.addProperty("model", "amonpack:block/meteoryt_ore");
            variants.add("instrument=bass,note=2,powered=false", meteorModel);
            variants.add("instrument=bass,note=2,powered=true", meteorModel);

            JsonObject basaltModel = new JsonObject();
            basaltModel.addProperty("model", "amonpack:block/basalt_ore");
            variants.add("instrument=bass,note=3,powered=false", basaltModel);
            variants.add("instrument=bass,note=3,powered=true", basaltModel);

            JsonObject altarModel = new JsonObject();
            altarModel.addProperty("model", "amonpack:block/arcane_altar");
            variants.add("instrument=bass,note=4,powered=false", altarModel);
            variants.add("instrument=bass,note=4,powered=true", altarModel);

            blockstatesRoot.add("variants", variants);
            try (FileWriter writer = new FileWriter(new File(mcBlockstates, "note_block.json"), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(blockstatesRoot));
            }

            // 5. Generowanie plików assets/minecraft/models/item/<mat>.json ORAZ assets/minecraft/items/<mat>.json
            for (Map.Entry<String, Map<Integer, String>> entry : vanillaOverrides.entrySet()) {
                String mat = entry.getKey();
                Map<Integer, String> cmdMap = entry.getValue();

                boolean isWeapon = mat.contains("sword") || mat.contains("axe") || mat.contains("pickaxe") || mat.contains("shovel") || mat.contains("hoe") || mat.contains("bow") || mat.contains("stick");
                boolean isBlock = mat.contains("note_block");

                // Format A: 1.14 - 1.21.1 (models/item/<mat>.json)
                JsonObject modelRoot = new JsonObject();
                if (isWeapon) {
                    modelRoot.addProperty("parent", "minecraft:item/handheld");
                    JsonObject textures = new JsonObject();
                    textures.addProperty("layer0", "minecraft:item/" + mat);
                    modelRoot.add("textures", textures);
                } else if (isBlock) {
                    modelRoot.addProperty("parent", "minecraft:block/" + mat);
                } else {
                    modelRoot.addProperty("parent", "minecraft:item/generated");
                    JsonObject textures = new JsonObject();
                    textures.addProperty("layer0", "minecraft:item/" + mat);
                    modelRoot.add("textures", textures);
                }

                JsonArray overrides = new JsonArray();
                for (Map.Entry<Integer, String> cmdEntry : cmdMap.entrySet()) {
                    JsonObject ov = new JsonObject();
                    JsonObject pred = new JsonObject();
                    pred.addProperty("custom_model_data", cmdEntry.getKey());
                    ov.add("predicate", pred);
                    ov.addProperty("model", cmdEntry.getValue());
                    overrides.add(ov);
                }
                modelRoot.add("overrides", overrides);

                File outOverrideFile = new File(mcModelsItem, mat + ".json");
                try (FileWriter fw = new FileWriter(outOverrideFile, StandardCharsets.UTF_8)) {
                    fw.write(GSON.toJson(modelRoot));
                }

                // Format B: 1.21.2 - 1.21.4+ (items/<mat>.json)
                JsonObject itemDefRoot = new JsonObject();
                JsonObject rangeDispatch = new JsonObject();
                rangeDispatch.addProperty("type", "minecraft:range_dispatch");
                rangeDispatch.addProperty("property", "minecraft:custom_model_data");

                JsonObject fallback = new JsonObject();
                fallback.addProperty("type", "minecraft:model");
                fallback.addProperty("model", isBlock ? "minecraft:block/" + mat : "minecraft:item/" + mat);
                rangeDispatch.add("fallback", fallback);

                JsonArray entriesList = new JsonArray();
                for (Map.Entry<Integer, String> cmdEntry : cmdMap.entrySet()) {
                    JsonObject entryObj = new JsonObject();
                    entryObj.addProperty("threshold", cmdEntry.getKey());
                    JsonObject modelObj = new JsonObject();
                    modelObj.addProperty("type", "minecraft:model");
                    modelObj.addProperty("model", cmdEntry.getValue());
                    entryObj.add("model", modelObj);
                    entriesList.add(entryObj);
                }
                rangeDispatch.add("entries", entriesList);
                itemDefRoot.add("model", rangeDispatch);

                File outItemDefFile = new File(mcItems, mat + ".json");
                try (FileWriter fw = new FileWriter(outItemDefFile, StandardCharsets.UTF_8)) {
                    fw.write(GSON.toJson(itemDefRoot));
                }
            }

            // 6. Pakowanie do ZIP
            if (!generatedZip.getParentFile().exists()) {
                generatedZip.getParentFile().mkdirs();
            }
            if (generatedZip.exists()) {
                generatedZip.delete();
            }

            zipDirectory(tempBuildDir, generatedZip);
            deleteDirectory(tempBuildDir);

            // Kopia do głównego folderu wtyczki dla wygody
            try {
                File rootZip = new File(AmonPackPlugin.plugin.getDataFolder(), "AmonPack_ResourcePack.zip");
                Files.copy(generatedZip.toPath(), rootZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {}

            // 7. Obliczanie SHA-1
            this.packHash = calculateSha1(generatedZip);
            this.packHashHex = bytesToHex(packHash);

            Bukkit.getLogger().info("[AmonPack] ResourcePack został pomyślnie zbudowany! Rozmiar: " + (generatedZip.length() / 1024) + " KB, SHA1: " + packHashHex);

        } catch (Exception e) {
            Bukkit.getLogger().severe("[AmonPack] Błąd podczas budowania ResourcePacka: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void applyToPlayer(Player player) {
        if (!generatedZip.exists()) {
            buildResourcePack();
        }

        String url = getPackDownloadUrl(player);
        if (url != null && !url.isEmpty()) {
            try {
                Bukkit.getLogger().info("[AmonPack] Wysyłanie ResourcePacka do gracza " + player.getName() + " (URL: " + url + ")");
                if (packHash != null) {
                    player.setResourcePack(url, packHash, promptMessage, forcePack);
                } else {
                    player.setResourcePack(url);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AmonPack] Nie udało się wysłać ResourcePacka graczowi " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    public String getPackDownloadUrl(Player player) {
        String host = resolveHost(player);
        return "http://" + host + ":" + httpPort + "/resourcepack.zip";
    }

    public String getPackDownloadUrl() {
        return getPackDownloadUrl(null);
    }

    private String resolveHost(Player player) {
        if (hostAddress != null && !hostAddress.isEmpty() && !hostAddress.equalsIgnoreCase("auto")) {
            return hostAddress;
        }

        if (player != null && player.isOnline()) {
            try {
                java.lang.reflect.Method m = player.getClass().getMethod("getVirtualHost");
                Object vHostObj = m.invoke(player);
                if (vHostObj instanceof InetSocketAddress isa && isa.getHostString() != null) {
                    String vHost = isa.getHostString();
                    if (!vHost.isEmpty() && !vHost.equals("0.0.0.0")) {
                        return vHost;
                    }
                }
            } catch (Throwable ignored) {}

            try {
                if (player.getAddress() != null && player.getAddress().getAddress() != null) {
                    String clientIp = player.getAddress().getAddress().getHostAddress();
                    if (clientIp != null && (clientIp.equals("127.0.0.1") || clientIp.equals("0:0:0:0:0:0:0:1") || clientIp.startsWith("192.168.") || clientIp.startsWith("10."))) {
                        return clientIp;
                    }
                }
            } catch (Throwable ignored) {}
        }

        String serverIp = Bukkit.getIp();
        if (serverIp != null && !serverIp.isEmpty() && !serverIp.equals("0.0.0.0") && !serverIp.equals("127.0.0.1")) {
            return serverIp;
        }

        if (detectedPublicIp != null && !detectedPublicIp.isEmpty()) {
            return detectedPublicIp;
        }

        return "127.0.0.1";
    }

    public boolean isAutoSendOnJoin() {
        return autoSendOnJoin;
    }

    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Path sourcePath = sourceDir.toPath();
            Files.walk(sourcePath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString().replace("\\", "/"));
                try {
                    zos.putNextEntry(zipEntry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    private byte[] calculateSha1(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return digest.digest();
    }



    private void extractJarResources(String resourcePrefix, File targetDirectory) {
        // Metoda 1: Bezpośrednie odczytanie ZipFile z pliku JAR wtyczki
        try {
            File pluginJar = null;
            try {
                java.lang.reflect.Method getFileMethod = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredMethod("getFile");
                getFileMethod.setAccessible(true);
                pluginJar = (File) getFileMethod.invoke(AmonPackPlugin.plugin);
            } catch (Throwable ignored) {}

            if (pluginJar == null || !pluginJar.exists()) {
                java.security.CodeSource src = AmonPackPlugin.class.getProtectionDomain().getCodeSource();
                if (src != null && src.getLocation() != null) {
                    pluginJar = new File(src.getLocation().toURI());
                }
            }

            if (pluginJar != null && pluginJar.exists()) {
                try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(pluginJar)) {
                    java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zf.entries();
                    while (entries.hasMoreElements()) {
                        java.util.zip.ZipEntry e = entries.nextElement();
                        String name = e.getName();
                        if (name.startsWith(resourcePrefix) && !e.isDirectory()) {
                            String relPath = name.substring(resourcePrefix.length());
                            if (relPath.startsWith("/")) relPath = relPath.substring(1);
                            File outFile = new File(targetDirectory, relPath);
                            outFile.getParentFile().mkdirs();
                            try (InputStream is = zf.getInputStream(e);
                                 FileOutputStream fos = new FileOutputStream(outFile)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = is.read(buf)) > 0) {
                                    fos.write(buf, 0, len);
                                }
                            }
                        }
                    }
                    return;
                }
            }
        } catch (Throwable ignored) {}

        // Metoda 2: Fallback na ZipInputStream
        try {
            java.security.CodeSource src = AmonPackPlugin.class.getProtectionDomain().getCodeSource();
            if (src != null && src.getLocation() != null) {
                java.net.URL jar = src.getLocation();
                try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(jar.openStream())) {
                    java.util.zip.ZipEntry e;
                    while ((e = zip.getNextEntry()) != null) {
                        String name = e.getName();
                        if (name.startsWith(resourcePrefix) && !e.isDirectory()) {
                            String relPath = name.substring(resourcePrefix.length());
                            if (relPath.startsWith("/")) relPath = relPath.substring(1);
                            File outFile = new File(targetDirectory, relPath);
                            outFile.getParentFile().mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = zip.read(buf)) > 0) {
                                    fos.write(buf, 0, len);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AmonPack] Błąd wyodrębniania zasobów z JAR (" + resourcePrefix + "): " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
