package dev.ayoangelo.databasemanager.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.ayoangelo.databasemanager.utils.Config;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

public class DatabaseAPI {
    private static final String JDBC_URL = "jdbc:mysql://" + Config.getData(String.class, "ip", "localhost") + ":" + Config.getData(String.class, "port", "3306") + "/";
    private static final String USER = Config.getData(String.class, "user", "root");
    private static final String PASSWORD = Config.getData(String.class, "password", "password");
    private static final int POOL_SIZE = Config.getData(Integer.class, "pool-size", 50);
    private static final HikariDataSource dataSource;
    private static final Gson gson = new Gson();

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(POOL_SIZE);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void createDatabasesIfNotExist(String... databaseName) {
        String sql = "CREATE DATABASE IF NOT EXISTS `%s`";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String db : databaseName) {
                stmt.executeUpdate(String.format(sql, db));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTablesIfNotExist(String databaseName, String... tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS `%s`.`%s` (`key` VARCHAR(255) PRIMARY KEY, `value` TEXT)";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String tbl : tableName) {
                stmt.executeUpdate(String.format(sql, databaseName, tbl));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTablesWithoutKeyIfNotExist(String databaseName, String... tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS `%s`.`%s` (id INT AUTO_INCREMENT PRIMARY KEY, `value` TEXT)";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String tbl : tableName) {
                stmt.executeUpdate(String.format(sql, databaseName, tbl));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createMultiColumnTableIfNotExist(String databaseName, String tableName, String... columns) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + databaseName + "`.`" + tableName + "` (`key` VARCHAR(255), ");
        for (int i = 0; i < columns.length; i++) {
            sql.append("`" + columns[i] + "` TEXT");
            if (i < columns.length - 1) sql.append(", ");
        }
        sql.append(", PRIMARY KEY(`key`))");
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static boolean databaseExist(String databaseName) {
        String sql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean tableExist(String databaseName, String tableName) {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void deleteDatabasesIfExist(String... databaseName) {
        String sql = "DROP DATABASE IF EXISTS `%s`";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String db : databaseName) {
                stmt.executeUpdate(String.format(sql, db));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteTablesIfExist(String databaseName, String... tableName) {
        String sql = "DROP TABLE IF EXISTS `%s`.`%s`";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String tbl : tableName) {
                stmt.executeUpdate(String.format(sql, databaseName, tbl));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getTableType(String databaseName, String tableName) {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = 'key'";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? "key" : "nokey";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getString(String databaseName, String tableName, String key) {
        String sql = "SELECT `value` FROM `" + databaseName + "`.`" + tableName + "` WHERE `key` = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getStringList(String databaseName, String tableName, String key) {
        String sql = "SELECT `value` FROM `" + databaseName + "`.`" + tableName + "` WHERE `key` = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ArrayList<>(Arrays.asList(rs.getString("value").split(",\\s*")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getData(Object example, String databaseName, String tableName, String key) {
        String json = getString(databaseName, tableName, key);
        if (json == null) return null;
        Type type;
        if (example instanceof Collection || example instanceof Map) {
            type = TypeToken.get(example.getClass()).getType();
        } else {
            type = example.getClass();
        }
        return gson.fromJson(json, type);
    }

    public static String getStringFromColumn(String databaseName, String tableName, String key, String columnName) {
        String sql = "SELECT `" + columnName + "` FROM `" + databaseName + "`.`" + tableName + "` WHERE `key` = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(columnName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getStringListFromColumn(String databaseName, String tableName, String key, String columnName) {
        String sql = "SELECT `" + columnName + "` FROM `" + databaseName + "`.`" + tableName + "` WHERE `key` = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ArrayList<>(Arrays.asList(rs.getString(columnName).split(",\\s*")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getObjectFromColumn(Object example, String databaseName, String tableName, String key, String columnName) {
        String json = getStringFromColumn(databaseName, tableName, key, columnName);
        if (json == null) return null;
        Type type;
        if (example instanceof Collection || example instanceof Map) {
            type = TypeToken.get(example.getClass()).getType();
        } else {
            type = example.getClass();
        }
        return gson.fromJson(json, type);
    }

    public static void saveString(String databaseName, String tableName, String key, String value) {
        String sql = "INSERT INTO `" + databaseName + "`.`" + tableName + "` (`key`,`value`) VALUES (?,?) " +
                "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveStringList(String databaseName, String tableName, String key, List<String> value) {
        String sql = "INSERT INTO `" + databaseName + "`.`" + tableName + "` (`key`,`value`) VALUES (?,?) " +
                "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, String.join(", ", value));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveString(String databaseName, String tableName, String value) {
        String sql = "INSERT INTO `" + databaseName + "`.`" + tableName + "` (`value`) VALUES (?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveData(String databaseName, String tableName, String key, Object value) {
        saveString(databaseName, tableName, key, gson.toJson(value));
    }

    public static void saveData(String databaseName, String tableName, Object value) {
        saveString(databaseName, tableName, gson.toJson(value));
    }

    public static void saveStringInColumn(String databaseName, String tableName, String key, String columnName, String value) {
        String sql = "INSERT INTO `" + databaseName + "`.`" + tableName + "` (`key`, `" + columnName + "`) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE `" + columnName + "` = VALUES(`" + columnName + "`)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveStringListInColumn(String databaseName, String tableName, String key, String columnName, List<String> value) {
        String sql = "INSERT INTO `" + databaseName + "`.`" + tableName + "` (`key`, `" + columnName + "`) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE `" + columnName + "` = VALUES(`" + columnName + "`)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, String.join(", ", value));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveObjectInColumn(String databaseName, String tableName, String key, String columnName, Object value) {
        saveStringInColumn(databaseName, tableName, key, columnName, gson.toJson(value));
    }

    public static void deleteValue(String databaseName, String tableName, String key) {
        String sql = "DELETE FROM `" + databaseName + "`.`" + tableName + "` WHERE `key` = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeString(String databaseName, String tableName, String value) {
        String sql = "DELETE FROM `" + databaseName + "`.`" + tableName + "` WHERE `value` = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeValue(String databaseName, String tableName, Object value) {
        removeString(databaseName, tableName, gson.toJson(value));
    }

    public static List<String> getTableKeys(String databaseName, String tableName) {
        List<String> keys = new ArrayList<>();
        String sql = "SELECT `key` FROM `" + databaseName + "`.`" + tableName + "`";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                keys.add(rs.getString("key"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return keys;
    }

    public static List<String> getTableStrings(String databaseName, String tableName) {
        List<String> values = new ArrayList<>();
        String sql = "SELECT `value` FROM `" + databaseName + "`.`" + tableName + "`";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString("value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return values;
    }

    public static List<Object> getTableObject(String databaseName, String tableName) {
        List<Object> list = new ArrayList<>();
        String sql = "SELECT `value` FROM `" + databaseName + "`.`" + tableName + "`";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(gson.fromJson(rs.getString("value"), Object.class));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getTables(String databaseName) {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tables;
    }

    public static boolean hasKey(String databaseName, String tableName, String key) {
        String query = "SELECT 1 FROM `" + databaseName + "`.`" + tableName + "` WHERE `key` = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean hasString(String databaseName, String tableName, String value) {
        String query = "SELECT 1 FROM `" + databaseName + "`.`" + tableName + "` WHERE `value` = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean hasObject(String databaseName, String tableName, Object value) {
        String json = gson.toJson(value);
        return hasString(databaseName, tableName, json);
    }
}
