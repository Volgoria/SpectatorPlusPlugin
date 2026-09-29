package fr.spectatorplus.storage;

import fr.spectatorplus.core.config.ConfigSection;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Stockage SQL (SQLite ou MySQL/MariaDB). Les drivers sont fournis par le serveur (Spigot/Paper) ou embarqués par le mod.
 */
public final class SqlStorage implements Storage {

    private final boolean mysql;
    private final String url;
    private final String user;
    private final String password;
    private final String table;
    private Connection connection;

    public SqlStorage(boolean mysql, ConfigSection cfg, File dataFolder) {
        this.mysql = mysql;
        this.table = cfg.getString("table-prefix", "spectatorplus_") + "preferences";
        if (mysql) {
            this.url = "jdbc:mysql://" + cfg.getString("host", "localhost") + ":" + cfg.getInt("port", 3306) + "/"
                    + cfg.getString("database", "spectatorplus") + "?" + cfg.getString("options", "useSSL=false&autoReconnect=true&characterEncoding=utf8");
            this.user = cfg.getString("username", "root");
            this.password = cfg.getString("password", "");
        } else {
            this.url = "jdbc:sqlite:" + new File(dataFolder, cfg.getString("file", "database.db")).getAbsolutePath();
            this.user = null;
            this.password = null;
        }
    }

    @Override
    public void init() throws Exception {
        if (mysql) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver");
            }
        } else {
            Class.forName("org.sqlite.JDBC");
        }
        try (Statement st = connection().createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + table
                    + " (uuid VARCHAR(36) NOT NULL PRIMARY KEY, data TEXT NOT NULL, updated BIGINT NOT NULL)");
        }
    }

    private synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed() || (mysql && !connection.isValid(2))) {
            connection = user == null ? DriverManager.getConnection(url) : DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    @Override
    public synchronized String load(UUID player) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("SELECT data FROM " + table + " WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    @Override
    public synchronized void save(UUID player, String data) throws SQLException {
        String sql = mysql
                ? "INSERT INTO " + table + " (uuid, data, updated) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE data = VALUES(data), updated = VALUES(updated)"
                : "INSERT OR REPLACE INTO " + table + " (uuid, data, updated) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setString(2, data);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized void delete(UUID player) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("DELETE FROM " + table + " WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {
        }
    }

    @Override
    public String name() {
        return mysql ? "MySQL" : "SQLite";
    }
}
