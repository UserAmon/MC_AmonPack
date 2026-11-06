package Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Niezależny zarządca lokalnej bazy danych SQLite dla AmonPack.
 * Zastępuje wcześniejszą twardą zależność od com.projectkorra.projectkorra.storage.SQLite.
 */
public class SQLite {

    private final Logger log;
    private final String dbName;
    private final String folderPath;
    private Connection connection;

    public SQLite(Logger log, String dbName, String folderPath) {
        this.log = log;
        this.dbName = dbName;
        this.folderPath = folderPath;
    }

    public Connection open() {
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String fileName = dbName;
            if (!fileName.endsWith(".db")) {
                fileName += ".db";
            }

            File dbFile = new File(folder, fileName);
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            return this.connection;
        } catch (Exception e) {
            if (log != null) {
                log.severe("[AmonPack] Nie udało się otworzyć bazy SQLite (" + dbName + "): " + e.getMessage());
            }
            return null;
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                return open();
            }
        } catch (SQLException e) {
            return open();
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        } finally {
            connection = null;
        }
    }

    public boolean isOpen() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
