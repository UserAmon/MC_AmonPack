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

        File customDir = new File(packDir, "custom");
        if (!customDir.exists()) {
            customDir.mkdirs();
        }

        String[] sampleFiles = {
                "Spirit_Earth_2.bbmodel",
                "meteor_axe.bbmodel", "meteor_axe.json", "meteor_axe.png",
                "meteor_scythe.bbmodel", "meteor_scythe.json", "meteor_scythe.png",
                "meteor_shard.bbmodel", "meteor_shard.json", "meteor_shard.png",
                "meteoryt_ore.bbmodel", "meteoryt_ore.json", "meteoryt_ore.png",
                "magic_crafting_table.json", "magic_crafting_table.png",
                "arcane_altar.json", "arcane_altar.png",
                "tome_fire.json", "tome_fire.png",
                "wand_fen.json", "wand_fen.png",
                "bone_sword.json", "bone_sword.png",
                "custom_bow.json", "custom_bow.png",
                "meteor_pickaxe.json", "meteor_pickaxe.png"
        };

        for (String file : sampleFiles) {
            File target = new File(customDir, file);
            InputStream in = AmonPackPlugin.plugin.getResource("pack/custom/" + file);
            if (in == null) {
                in = AmonPackPlugin.plugin.getResource("custom/" + file);
            }
            if (in != null) {
                try {
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    in.close();
                } catch (Exception ignored) {}
            }
        }
    }

    public void buildResourcePack() {
        try {
            Bukkit.getLogger().info("[AmonPack] Rozpoczynanie budowania ResourcePacka...");

            File tempBuildDir = new File(AmonPackPlugin.plugin.getDataFolder(), "temp_pack_build");
            if (tempBuildDir.exists()) {
                deleteDirectory(tempBuildDir);
            }
            tempBuildDir.mkdirs();

            // 1. pack.mcmeta
            File mcmeta = new File(tempBuildDir, "pack.mcmeta");
            JsonObject mcMetaJson = new JsonObject();
            JsonObject packObj = new JsonObject();
            packObj.addProperty("pack_format", 34); // Kompatybilny z 1.21 - 1.21.x
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
            File mcItems121 = new File(tempBuildDir, "assets/minecraft/items");
            File mcTexGui = new File(tempBuildDir, "assets/minecraft/textures/gui");

            amonpackModels.mkdirs();
            amonpackTex.mkdirs();
            mcModelsItem.mkdirs();
            mcItems121.mkdirs();
            mcTexGui.mkdirs();

            // A. Wyodrębnienie wszystkich zasobów wbudowanych w JAR (pack/amonpack oraz pack/custom)
            extractJarResources("pack/amonpack/textures", amonpackTex);
            extractJarResources("pack/amonpack/models", amonpackModels);
            extractJarResources("pack/custom", new File(packDir, "custom"));
            extractJarResources("pack/amonpack/textures", new File(tempBuildDir, "assets/minecraft/textures"));

            // B. Kopiowanie z lokalnego folderu amonpack na dysku serwera (jeśli istnieje)
            File amonpackDir = new File(AmonPackPlugin.plugin.getDataFolder(), "amonpack");
            if (!amonpackDir.exists()) {
                amonpackDir = new File(AmonPackPlugin.plugin.getDataFolder().getParentFile().getParentFile(), "amonpack");
            }
            if (!amonpackDir.exists()) {
                amonpackDir = new File("amonpack");
            }

            File amonTexSrc = new File(amonpackDir, "textures");
            if (amonTexSrc.exists() && amonTexSrc.isDirectory()) {
                copyDirectoryRecursive(amonTexSrc, amonpackTex);
                copyDirectoryRecursive(amonTexSrc, new File(tempBuildDir, "assets/minecraft/textures"));
            }

            File amonModelsSrc = new File(amonpackDir, "models");
            if (amonModelsSrc.exists() && amonModelsSrc.isDirectory()) {
                copyDirectoryRecursive(amonModelsSrc, amonpackModels);
            }

            // 3. Kopiowanie i konwersja plików z folderu pack/custom/
            File customDir = new File(packDir, "custom");
            if (customDir.exists() && customDir.isDirectory()) {
                File[] files = customDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName().toLowerCase(Locale.ROOT);
                        if (name.endsWith(".png")) {
                            byte[] pngBytes = Files.readAllBytes(f.toPath());
                            Files.write(new File(amonpackTex, f.getName()).toPath(), pngBytes);
                            File blockTexDir = new File(amonpackTex, "block");
                            blockTexDir.mkdirs();
                            Files.write(new File(blockTexDir, f.getName()).toPath(), pngBytes);
                            File blocksTexDir = new File(amonpackTex, "blocks");
                            blocksTexDir.mkdirs();
                            Files.write(new File(blocksTexDir, f.getName()).toPath(), pngBytes);
                            File itemTexDir = new File(amonpackTex, "item");
                            itemTexDir.mkdirs();
                            Files.write(new File(itemTexDir, f.getName()).toPath(), pngBytes);
                            File magicTexDir = new File(amonpackTex, "magic");
                            magicTexDir.mkdirs();
                            Files.write(new File(magicTexDir, f.getName()).toPath(), pngBytes);
                        } else if (name.endsWith(".bbmodel")) {
                            String baseName = f.getName().substring(0, f.getName().length() - 8);
                            boolean isBoss = baseName.toLowerCase(Locale.ROOT).contains("spirit") || baseName.toLowerCase(Locale.ROOT).contains("boss");
                            boolean isBlock = baseName.toLowerCase(Locale.ROOT).contains("ore") || baseName.toLowerCase(Locale.ROOT).contains("block") || baseName.toLowerCase(Locale.ROOT).contains("table");

                            String texCategory = isBoss ? "boss" : (isBlock ? "block" : "item");
                            String texNamespace = "amonpack:" + texCategory + "/" + baseName;

                            try {
                                BbmodelParser.ConversionResult result = BbmodelParser.convertBbmodel(f, texNamespace);
                                File targetModelDir = isBoss ? new File(amonpackModels, "boss") : (isBlock ? new File(amonpackModels, "block") : new File(amonpackModels, "item"));
                                targetModelDir.mkdirs();
                                File outModelFile = new File(targetModelDir, baseName + ".json");
                                try (FileWriter fw = new FileWriter(outModelFile, StandardCharsets.UTF_8)) {
                                    fw.write(result.modelJson);
                                }

                                for (BbmodelParser.TextureData td : result.textures) {
                                    if (td.bytes != null && td.bytes.length > 0) {
                                        String texFileName = baseName + (td.id.equals("0") ? "" : "_" + td.id) + ".png";
                                        File mainTexDir = isBoss ? new File(amonpackTex, "boss") : (isBlock ? new File(amonpackTex, "block") : new File(amonpackTex, "item"));
                                        mainTexDir.mkdirs();
                                        Files.write(new File(mainTexDir, texFileName).toPath(), td.bytes);
                                    }
                                }
                            } catch (Exception e) {
                                Bukkit.getLogger().warning("[AmonPack] Błąd konwersji .bbmodel " + f.getName() + ": " + e.getMessage());
                            }
                        } else if (name.endsWith(".json")) {
                            String baseName = f.getName().substring(0, f.getName().length() - 5);
                            boolean isBoss = baseName.toLowerCase(Locale.ROOT).contains("spirit") || baseName.toLowerCase(Locale.ROOT).contains("boss");
                            boolean isBlock = baseName.toLowerCase(Locale.ROOT).contains("ore") || baseName.toLowerCase(Locale.ROOT).contains("block") || baseName.toLowerCase(Locale.ROOT).contains("table");
                            boolean isMagic = baseName.toLowerCase(Locale.ROOT).contains("tome") || baseName.toLowerCase(Locale.ROOT).contains("spell");
                            String category = isBoss ? "boss" : (isBlock ? "block" : (isMagic ? "magic" : "item"));
                            File targetDir = new File(amonpackModels, category);
                            targetDir.mkdirs();

                            try {
                                Files.copy(f.toPath(), new File(targetDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                                // Mirror to root models and blocks/ if applicable
                                Files.copy(f.toPath(), new File(amonpackModels, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                                if (isBlock) {
                                    File blocksDir = new File(amonpackModels, "blocks");
                                    blocksDir.mkdirs();
                                    Files.copy(f.toPath(), new File(blocksDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            // Tworzenie aliasów katalogowych block <-> blocks, item <-> items, magic, weapons, crafting
            mirrorDirectory(new File(amonpackModels, "blocks"), new File(amonpackModels, "block"));
            mirrorDirectory(new File(amonpackModels, "block"), new File(amonpackModels, "blocks"));
            mirrorDirectory(new File(amonpackModels, "crafting"), new File(amonpackModels, "item"));
            mirrorDirectory(new File(amonpackModels, "weapons"), new File(amonpackModels, "item"));
            mirrorDirectory(new File(amonpackModels, "magic"), new File(amonpackModels, "item"));
            mirrorDirectory(new File(amonpackModels, "item"), new File(amonpackModels, "magic"));
            mirrorDirectory(new File(amonpackModels, "item"), new File(amonpackModels, "weapons"));
            mirrorDirectory(new File(amonpackModels, "item"), new File(amonpackModels, "crafting"));
            mirrorDirectory(new File(amonpackModels, "item"), new File(amonpackModels, "block"));

            mirrorDirectory(new File(amonpackTex, "blocks"), new File(amonpackTex, "block"));
            mirrorDirectory(new File(amonpackTex, "block"), new File(amonpackTex, "blocks"));
            mirrorDirectory(new File(amonpackTex, "crafting"), new File(amonpackTex, "item"));
            mirrorDirectory(new File(amonpackTex, "weapons"), new File(amonpackTex, "item"));
            mirrorDirectory(new File(amonpackTex, "magic"), new File(amonpackTex, "item"));
            mirrorDirectory(new File(amonpackTex, "item"), new File(amonpackTex, "magic"));
            mirrorDirectory(new File(amonpackTex, "item"), new File(amonpackTex, "weapons"));
            mirrorDirectory(new File(amonpackTex, "item"), new File(amonpackTex, "crafting"));
            mirrorDirectory(new File(amonpackTex, "item"), new File(amonpackTex, "block"));

            // Normalizacja prefiksów "minecraft:" w modelach JSON
            fixModelParentsRecursive(tempBuildDir);

            // Mirror textures do minecraft/textures
            copyDirectoryRecursive(amonpackTex, new File(tempBuildDir, "assets/minecraft/textures"));

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

            // Różdżka i magia (STICK, BOOK, ENCHANTED_BOOK)
            registerModelOverride("stick", 20002, "amonpack:magic/wand_fen");
            registerModelOverride("book", 10010, "amonpack:magic/tome_fire");
            registerModelOverride("book", 20001, "amonpack:magic/tome_fire");
            registerModelOverride("book", 20002, "amonpack:magic/wand_fen");
            registerModelOverride("enchanted_book", 10010, "amonpack:magic/tome_fire");
            registerModelOverride("enchanted_book", 20001, "amonpack:magic/tome_fire");
            registerModelOverride("enchanted_book", 20002, "amonpack:magic/wand_fen");

            // Przedmioty rzemieślnicze (PAPER)
            registerModelOverride("paper", 10001, "amonpack:crafting/mold_empty");
            registerModelOverride("paper", 10002, "amonpack:crafting/mold_full");
            registerModelOverride("paper", 10003, "amonpack:crafting/meteor_shard");
            registerModelOverride("paper", 10004, "amonpack:crafting/basalt_shard");
            registerModelOverride("paper", 10005, "amonpack:crafting/firescroll");

            // Narzędzia i zbroje
            registerModelOverride("diamond_sword", 10001, "amonpack:item/meteor_scythe");
            registerModelOverride("diamond_axe", 10002, "amonpack:item/meteor_axe");
            registerModelOverride("netherite_axe", 10002, "amonpack:item/meteor_axe");
            registerModelOverride("diamond_pickaxe", 10014, "amonpack:weapons/meteor_pickaxe");
            registerModelOverride("netherite_pickaxe", 10014, "amonpack:weapons/meteor_pickaxe");
            registerModelOverride("flint", 10003, "amonpack:crafting/meteor_shard");
            registerModelOverride("carved_pumpkin", 20001, "amonpack:boss/Spirit_Earth_2");

            // Bloki (NOTE_BLOCK & IRON_NUGGET)
            registerModelOverride("note_block", 30001, "amonpack:block/meteoryt_ore");
            registerModelOverride("iron_nugget", 30001, "amonpack:block/meteoryt_ore");
            registerModelOverride("note_block", 30002, "amonpack:block/magic_crafting_table");
            registerModelOverride("iron_nugget", 30002, "amonpack:block/magic_crafting_table");
            registerModelOverride("note_block", 30003, "amonpack:block/basalt_ore");
            registerModelOverride("iron_nugget", 30003, "amonpack:block/basalt_ore");
            registerModelOverride("note_block", 30004, "amonpack:block/arcane_altar");
            registerModelOverride("iron_nugget", 30004, "amonpack:block/arcane_altar");

            // 5. Generowanie plików dla 1.14-1.21.1 ORAZ 1.21.2+ (assets/minecraft/models/item oraz assets/minecraft/items)
            for (Map.Entry<String, Map<Integer, String>> entry : vanillaOverrides.entrySet()) {
                String mat = entry.getKey();
                Map<Integer, String> cmdMap = entry.getValue();

                boolean isWeapon = mat.contains("sword") || mat.contains("axe") || mat.contains("pickaxe") || mat.contains("shovel") || mat.contains("hoe");
                boolean isBlock = mat.contains("note_block") || mat.contains("pumpkin") || mat.contains("ore") || mat.contains("stone");

                // A. assets/minecraft/models/item/<mat>.json (1.14 - 1.21.1)
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

                // B. assets/minecraft/items/<mat>.json (1.21.2 - 1.21.4+)
                JsonObject itemModelRoot = new JsonObject();
                JsonObject rangeDispatch = new JsonObject();
                rangeDispatch.addProperty("type", "minecraft:range_dispatch");
                rangeDispatch.addProperty("property", "minecraft:custom_model_data");

                JsonObject fallback = new JsonObject();
                fallback.addProperty("type", "minecraft:model");
                fallback.addProperty("model", isBlock ? "minecraft:block/" + mat : "minecraft:item/" + mat);
                rangeDispatch.add("fallback", fallback);

                JsonArray entries = new JsonArray();
                for (Map.Entry<Integer, String> cmdEntry : cmdMap.entrySet()) {
                    JsonObject entryObj = new JsonObject();
                    entryObj.addProperty("threshold", cmdEntry.getKey());
                    JsonObject mObj = new JsonObject();
                    mObj.addProperty("type", "minecraft:model");
                    mObj.addProperty("model", cmdEntry.getValue());
                    entryObj.add("model", mObj);
                    entries.add(entryObj);
                }
                rangeDispatch.add("entries", entries);
                itemModelRoot.add("model", rangeDispatch);

                File outItemDefFile = new File(mcItems121, mat + ".json");
                try (FileWriter fw = new FileWriter(outItemDefFile, StandardCharsets.UTF_8)) {
                    fw.write(GSON.toJson(itemModelRoot));
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
        if (hostAddress != null && !hostAddress.isEmpty()
                && !hostAddress.equalsIgnoreCase("auto")
                && !hostAddress.equalsIgnoreCase("127.0.0.1")
                && !hostAddress.equalsIgnoreCase("localhost")) {
            return hostAddress;
        }

        if (player != null && player.isOnline()) {
            try {
                java.lang.reflect.Method m = player.getClass().getMethod("getVirtualHost");
                Object vHostObj = m.invoke(player);
                if (vHostObj instanceof InetSocketAddress isa && isa.getHostString() != null) {
                    String vHost = isa.getHostString();
                    if (!vHost.isEmpty() && !vHost.equalsIgnoreCase("127.0.0.1") && !vHost.equalsIgnoreCase("localhost")) {
                        return vHost;
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

        return hostAddress != null && !hostAddress.isEmpty() && !hostAddress.equalsIgnoreCase("auto") ? hostAddress : "127.0.0.1";
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

    private void copyDirectoryRecursive(File source, File destination) {
        if (!source.exists()) return;
        if (source.isDirectory()) {
            if (!destination.exists()) destination.mkdirs();
            File[] files = source.listFiles();
            if (files != null) {
                for (File f : files) {
                    copyDirectoryRecursive(f, new File(destination, f.getName()));
                }
            }
        } else {
            try {
                if (!destination.getParentFile().exists()) destination.getParentFile().mkdirs();
                Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {}
        }
    }

    private void fixModelParentsRecursive(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                fixModelParentsRecursive(f);
            } else if (f.getName().endsWith(".json")) {
                try {
                    String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                    boolean changed = false;
                    if (content.contains("\"parent\": \"block/cube_all\"")) {
                        content = content.replace("\"parent\": \"block/cube_all\"", "\"parent\": \"minecraft:block/cube_all\"");
                        changed = true;
                    }
                    if (content.contains("\"parent\": \"item/handheld\"")) {
                        content = content.replace("\"parent\": \"item/handheld\"", "\"parent\": \"minecraft:item/handheld\"");
                        changed = true;
                    }
                    if (content.contains("\"parent\": \"item/generated\"")) {
                        content = content.replace("\"parent\": \"item/generated\"", "\"parent\": \"minecraft:item/generated\"");
                        changed = true;
                    }
                    if (changed) {
                        Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void mirrorDirectory(File source, File target) {
        if (!source.exists() || !source.isDirectory()) return;
        if (!target.exists()) target.mkdirs();
        File[] files = source.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.isDirectory()) {
                    try {
                        Files.copy(f.toPath(), new File(target, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception ignored) {}
                }
            }
        }
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
