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
        File cfgFile = new File(AmonPackPlugin.plugin.getDataFolder(), "pack_config.yml");
        if (!cfgFile.exists()) {
            AmonPackPlugin.plugin.saveResource("pack_config.yml", false);
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
                "meteoryt_ore.bbmodel", "meteoryt_ore.json", "meteoryt_ore.png"
        };

        for (String file : sampleFiles) {
            File target = new File(customDir, file);
            try (InputStream in = AmonPackPlugin.plugin.getResource("custom/" + file)) {
                if (in != null) {
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                Bukkit.getLogger().fine("[AmonPack] Note: Could not auto-export " + file);
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
            packObj.addProperty("description", "AmonPack Custom 3D Models & Textures");
            mcMetaJson.add("pack", packObj);
            try (FileWriter writer = new FileWriter(mcmeta, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(mcMetaJson));
            }

            // 2. Struktura katalogów w paczce
            File amonpackModelsItem = new File(tempBuildDir, "assets/amonpack/models/item");
            File amonpackModelsBlock = new File(tempBuildDir, "assets/amonpack/models/block");
            File amonpackModelsBoss = new File(tempBuildDir, "assets/amonpack/models/boss");

            File amonpackTexItem = new File(tempBuildDir, "assets/amonpack/textures/item");
            File amonpackTexBlock = new File(tempBuildDir, "assets/amonpack/textures/block");
            File amonpackTexBoss = new File(tempBuildDir, "assets/amonpack/textures/boss");

            File amonTexMining = new File(tempBuildDir, "assets/amon/textures/items/mining");
            File amonTexBlocks = new File(tempBuildDir, "assets/amon/textures/blocks");
            File amonTexItem = new File(tempBuildDir, "assets/amon/textures/item");
            File amonTexBlock = new File(tempBuildDir, "assets/amon/textures/block");

            File mcModelsItem = new File(tempBuildDir, "assets/minecraft/models/item");
            File mcItems121 = new File(tempBuildDir, "assets/minecraft/items");

            amonpackModelsItem.mkdirs();
            amonpackModelsBlock.mkdirs();
            amonpackModelsBoss.mkdirs();
            amonpackTexItem.mkdirs();
            amonpackTexBlock.mkdirs();
            amonpackTexBoss.mkdirs();
            amonTexMining.mkdirs();
            amonTexBlocks.mkdirs();
            amonTexItem.mkdirs();
            amonTexBlock.mkdirs();
            mcModelsItem.mkdirs();
            mcItems121.mkdirs();

            // 3. Kopiowanie i konwersja plików z folderu custom/
            File customDir = new File(packDir, "custom");
            if (customDir.exists() && customDir.isDirectory()) {
                File[] files = customDir.listFiles();
                if (files != null) {
                    // Najpierw kopiujemy tekstury PNG
                    for (File f : files) {
                        String name = f.getName().toLowerCase(Locale.ROOT);
                        if (name.endsWith(".png")) {
                            byte[] pngBytes = Files.readAllBytes(f.toPath());
                            Files.write(new File(amonpackTexItem, f.getName()).toPath(), pngBytes);
                            Files.write(new File(amonpackTexBlock, f.getName()).toPath(), pngBytes);
                            Files.write(new File(amonpackTexBoss, f.getName()).toPath(), pngBytes);
                            Files.write(new File(amonTexMining, f.getName()).toPath(), pngBytes);
                            Files.write(new File(amonTexBlocks, f.getName()).toPath(), pngBytes);
                            Files.write(new File(amonTexItem, f.getName()).toPath(), pngBytes);
                            Files.write(new File(amonTexBlock, f.getName()).toPath(), pngBytes);
                        }
                    }

                    // Następnie przetwarzamy pliki .bbmodel oraz .json
                    for (File f : files) {
                        String name = f.getName().toLowerCase(Locale.ROOT);

                        if (name.endsWith(".bbmodel")) {
                            String baseName = f.getName().substring(0, f.getName().length() - 8);
                            boolean isBoss = baseName.toLowerCase(Locale.ROOT).contains("spirit") || baseName.toLowerCase(Locale.ROOT).contains("boss");
                            boolean isBlock = baseName.toLowerCase(Locale.ROOT).contains("ore") || baseName.toLowerCase(Locale.ROOT).contains("block");

                            String texCategory = isBoss ? "boss" : (isBlock ? "block" : "item");
                            String texNamespace = "amonpack:" + texCategory + "/" + baseName;

                            try {
                                BbmodelParser.ConversionResult result = BbmodelParser.convertBbmodel(f, texNamespace);
                                File targetModelDir = isBoss ? amonpackModelsBoss : (isBlock ? amonpackModelsBlock : amonpackModelsItem);
                                File outModelFile = new File(targetModelDir, baseName + ".json");
                                try (FileWriter fw = new FileWriter(outModelFile, StandardCharsets.UTF_8)) {
                                    fw.write(result.modelJson);
                                }

                                for (BbmodelParser.TextureData td : result.textures) {
                                    if (td.bytes != null && td.bytes.length > 0) {
                                        String texFileName = baseName + (td.id.equals("0") ? "" : "_" + td.id) + ".png";
                                        File mainTexDir = isBoss ? amonpackTexBoss : (isBlock ? amonpackTexBlock : amonpackTexItem);
                                        Files.write(new File(mainTexDir, texFileName).toPath(), td.bytes);
                                        Files.write(new File(mainTexDir, td.fileName).toPath(), td.bytes);
                                        Files.write(new File(amonTexMining, texFileName).toPath(), td.bytes);
                                        Files.write(new File(amonTexBlocks, texFileName).toPath(), td.bytes);
                                    }
                                }
                            } catch (Exception e) {
                                Bukkit.getLogger().warning("[AmonPack] Błąd konwersji .bbmodel " + f.getName() + ": " + e.getMessage());
                            }
                        } else if (name.endsWith(".json")) {
                            String baseName = f.getName().substring(0, f.getName().length() - 5);
                            boolean isBoss = baseName.toLowerCase(Locale.ROOT).contains("spirit") || baseName.toLowerCase(Locale.ROOT).contains("boss");
                            boolean isBlock = baseName.toLowerCase(Locale.ROOT).contains("ore") || baseName.toLowerCase(Locale.ROOT).contains("block");
                            String category = isBoss ? "boss" : (isBlock ? "block" : "item");
                            File targetDir = isBoss ? amonpackModelsBoss : (isBlock ? amonpackModelsBlock : amonpackModelsItem);

                            try {
                                String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                                JsonObject jsonModel = JsonParser.parseString(content).getAsJsonObject();

                                // Rewriting texture dictionary to guarantee 100% resolution
                                if (jsonModel.has("textures") && jsonModel.get("textures").isJsonObject()) {
                                    JsonObject origTex = jsonModel.getAsJsonObject("textures");
                                    JsonObject rewrittenTex = new JsonObject();
                                    for (Map.Entry<String, JsonElement> te : origTex.entrySet()) {
                                        rewrittenTex.addProperty(te.getKey(), "amonpack:" + category + "/" + baseName);
                                    }
                                    rewrittenTex.addProperty("particle", "amonpack:" + category + "/" + baseName);
                                    jsonModel.add("textures", rewrittenTex);
                                }

                                Files.write(new File(targetDir, f.getName()).toPath(), GSON.toJson(jsonModel).getBytes(StandardCharsets.UTF_8));
                            } catch (Exception e) {
                                Files.copy(f.toPath(), new File(targetDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            }

            // 4. Rejestracja domyślnych modeli
            registerModelOverride("diamond_sword", 10001, "amonpack:item/meteor_scythe");
            registerModelOverride("diamond_axe", 10002, "amonpack:item/meteor_axe");
            registerModelOverride("flint", 10003, "amonpack:item/meteor_shard");
            registerModelOverride("carved_pumpkin", 20001, "amonpack:boss/Spirit_Earth_2");
            registerModelOverride("note_block", 30001, "amonpack:block/meteoryt_ore");
            registerModelOverride("iron_nugget", 30001, "amonpack:block/meteoryt_ore");

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

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
