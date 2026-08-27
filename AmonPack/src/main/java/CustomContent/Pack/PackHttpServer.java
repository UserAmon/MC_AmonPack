package CustomContent.Pack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Wbudowany, lekki serwer HTTP do bezpośredniego hostowania i wysyłania ResourcePacka graczom.
 */
public class PackHttpServer {

    private HttpServer server;
    private int port;
    private File packFile;

    public PackHttpServer(int port, File packFile) {
        this.port = port;
        this.packFile = packFile;
    }

    public void start() {
        try {
            stop();
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/resourcepack.zip", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!packFile.exists()) {
                        String response = "Resource pack file not generated yet.";
                        exchange.sendResponseHeaders(404, response.length());
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                        return;
                    }

                    Bukkit.getLogger().info("[AmonPack] Odebrano żądanie pobrania ResourcePacka od: " + exchange.getRemoteAddress());

                    exchange.getResponseHeaders().set("Content-Type", "application/zip");
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"AmonPack_ResourcePack.zip\"");
                    exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");

                    if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(200, -1);
                        return;
                    }

                    exchange.sendResponseHeaders(200, packFile.length());

                    try (FileInputStream fis = new FileInputStream(packFile);
                         OutputStream os = exchange.getResponseBody()) {
                        byte[] buffer = new byte[16384];
                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                        os.flush();
                    }
                }
            });

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            Bukkit.getLogger().info("[AmonPack] Serwer ResourcePacka uruchomiony na porcie " + port + " (ścieżka: /resourcepack.zip)");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AmonPack] Nie udało się uruchomić serwera HTTP ResourcePacka: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop(0);
                server = null;
            } catch (Exception ignored) {}
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    public int getPort() {
        return port;
    }
}
