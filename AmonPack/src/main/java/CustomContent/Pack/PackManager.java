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
    private String hostAddress = "127.0.0.1";
    private boolean autoSendOnJoin = true;
    private boolean forcePack = false;
    private String promptMessage = "§6§lAmonPack §7- Pobierz tekstury i modele 3D serwera!";

    // Rejestr CustomModelData dla poszczególnych materiałów bazowych
    // Format: MaterialName -> Map<CustomModelData, ModelPath (np. amonpack:item/meteor_scythe)>
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

    private String detectedPublicIp = null;

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

        // Kopiowanie wbudowanych zasobów z resources/custom/ do folderu pack/
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
            if (!target.exists()) {
                try (InputStream in = AmonPackPlugin.plugin.getResource("custom/" + file)) {
                    if (in != null) {
                        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().fine("[AmonPack] Note: Could not auto-export " + file);
                }
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
            packObj.addProperty("pack_format", 34); // 1.21
            packObj.addProperty("description", "AmonPack Custom 3D Models & Textures");
            mcMetaJson.add("pack", packObj);
            try (FileWriter writer = new FileWriter(mcmeta, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(mcMetaJson));
            }

            // 2. Struktura folderów
            File assetsAmonModels = new File(tempBuildDir, "assets/amonpack/models/item");
            File assetsAmonBlocks = new File(tempBuildDir, "assets/amonpack/models/block");
            File assetsAmonBosses = new File(tempBuildDir, "assets/amonpack/models/boss");
            File assetsAmonTextures = new File(tempBuildDir, "assets/amonpack/textures/item");
            File assetsAmonBlockTextures = new File(tempBuildDir, "assets/amonpack/textures/block");
            File assetsAmonBossTextures = new File(tempBuildDir, "assets/amonpack/textures/boss");
            File assetsMinecraftModels = new File(tempBuildDir, "assets/minecraft/models/item");

            assetsAmonModels.mkdirs();
            assetsAmonBlocks.mkdirs();
            assetsAmonBosses.mkdirs();
            assetsAmonTextures.mkdirs();
            assetsAmonBlockTextures.mkdirs();
            assetsAmonBossTextures.mkdirs();
            assetsMinecraftModels.mkdirs();

            // 3. Kopiowanie i konwersja plików z plugins/AmonPack/pack/custom/
            File customDir = new File(packDir, "custom");
            if (customDir.exists() && customDir.isDirectory()) {
                File[] files = customDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName().toLowerCase(Locale.ROOT);
                        if (name.endsWith(".bbmodel")) {
                            // Konwersja .bbmodel
                            String baseName = f.getName().substring(0, f.getName().length() - 8);
                            boolean isBoss = baseName.toLowerCase(Locale.ROOT).contains("spirit") || baseName.toLowerCase(Locale.ROOT).contains("boss");
                            boolean isBlock = baseName.toLowerCase(Locale.ROOT).contains("ore") || baseName.toLowerCase(Locale.ROOT).contains("block");

                            String texNamespace = isBoss ? "amonpack:boss/" + baseName : (isBlock ? "amonpack:block/" + baseName : "amonpack:item/" + baseName);
                            try {
                                BbmodelParser.ConversionResult result = BbmodelParser.convertBbmodel(f, texNamespace);
                                File targetModelDir = isBoss ? assetsAmonBosses : (isBlock ? assetsAmonBlocks : assetsAmonModels);
                                File outModelFile = new File(targetModelDir, baseName + ".json");
                                try (FileWriter fw = new FileWriter(outModelFile, StandardCharsets.UTF_8)) {
                                    fw.write(result.modelJson);
                                }

                                if (result.textureBytes != null && result.textureBytes.length > 0) {
                                    File targetTexDir = isBoss ? assetsAmonBossTextures : (isBlock ? assetsAmonBlockTextures : assetsAmonTextures);
                                    File outTex = new File(targetTexDir, baseName + ".png");
                                    Files.write(outTex.toPath(), result.textureBytes);
                                }
                            } catch (Exception e) {
                                Bukkit.getLogger().warning("[AmonPack] Błąd konwersji .bbmodel " + f.getName() + ": " + e.getMessage());
                            }
                        } else if (name.endsWith(".json")) {
                            // Plik JSON
                            String baseName = f.getName().substring(0, f.getName().length() - 5);
                            boolean isBoss = baseName.contains("spirit") || baseName.contains("boss");
                            boolean isBlock = baseName.contains("ore") || baseName.contains("block");
                            File targetDir = isBoss ? assetsAmonBosses : (isBlock ? assetsAmonBlocks : assetsAmonModels);
                            Files.copy(f.toPath(), new File(targetDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } else if (name.endsWith(".png")) {
                            // Plik PNG
                            String baseName = f.getName().substring(0, f.getName().length() - 4);
                            boolean isBoss = baseName.contains("spirit") || baseName.contains("boss");
                            boolean isBlock = baseName.contains("ore") || baseName.contains("block");
                            File targetDir = isBoss ? assetsAmonBossTextures : (isBlock ? assetsAmonBlockTextures : assetsAmonTextures);
                            Files.copy(f.toPath(), new File(targetDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }

            // 4. Generowanie plików Minecraft Vanilla Item Overrides
            // Domyślne mapowania dla przykładowych modeli jeśli nie zarejestrowane ręcznie
            registerModelOverride("diamond_sword", 10001, "amonpack:item/meteor_scythe");
            registerModelOverride("diamond_axe", 10002, "amonpack:item/meteor_axe");
            registerModelOverride("flint", 10003, "amonpack:item/meteor_shard");
            registerModelOverride("carved_pumpkin", 20001, "amonpack:boss/Spirit_Earth_2");
            registerModelOverride("note_block", 30001, "amonpack:block/meteoryt_ore");
            registerModelOverride("iron_nugget", 30001, "amonpack:block/meteoryt_ore");

            for (Map.Entry<String, Map<Integer, String>> entry : vanillaOverrides.entrySet()) {
                String mat = entry.getKey();
                Map<Integer, String> cmdMap = entry.getValue();

                JsonObject modelRoot = new JsonObject();
                modelRoot.addProperty("parent", "minecraft:item/handheld");
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", "minecraft:item/" + mat);
                modelRoot.add("textures", textures);

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

                File outOverrideFile = new File(assetsMinecraftModels, mat + ".json");
                try (FileWriter fw = new FileWriter(outOverrideFile, StandardCharsets.UTF_8)) {
                    fw.write(GSON.toJson(modelRoot));
                }
            }

            // 5. Pakowanie do pliku ZIP
            if (!generatedZip.getParentFile().exists()) {
                generatedZip.getParentFile().mkdirs();
            }
            if (generatedZip.exists()) {
                generatedZip.delete();
            }

            zipDirectory(tempBuildDir, generatedZip);
            deleteDirectory(tempBuildDir);

            // 6. Obliczanie SHA-1
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
        // 1. Jeśli w configu ustawiono jawne IP/domenę inną niż auto/127.0.0.1/localhost
        if (hostAddress != null && !hostAddress.isEmpty()
                && !hostAddress.equalsIgnoreCase("auto")
                && !hostAddress.equalsIgnoreCase("127.0.0.1")
                && !hostAddress.equalsIgnoreCase("localhost")) {
            return hostAddress;
        }

        // 2. Jeśli gracz jest połączony na Paper/Purpur, sprawdź domenę/IP przez którą wszedł
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

        // 3. Sprawdź IP serwera z server.properties (Bukkit.getIp())
        String serverIp = Bukkit.getIp();
        if (serverIp != null && !serverIp.isEmpty() && !serverIp.equals("0.0.0.0") && !serverIp.equals("127.0.0.1")) {
            return serverIp;
        }

        // 4. Użyj wykrytego publicznego IP
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
