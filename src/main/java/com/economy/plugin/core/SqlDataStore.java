package com.economy.plugin.core;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class SqlDataStore implements DataStore {
    private final String jdbcUrl;
    private final boolean mysql;
    private final String username;
    private final String password;

    private SqlDataStore(String jdbcUrl, boolean mysql, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.mysql = mysql;
        this.username = username;
        this.password = password;
    }

    public static SqlDataStore sqlite(File dbFile) {
        if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }
        return new SqlDataStore("jdbc:sqlite:" + dbFile.getAbsolutePath(), false, null, null);
    }

    public static SqlDataStore mysql(String host, int port, String database, String username, String password) {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return new SqlDataStore(url, true, username, password);
    }

    private Connection connect() throws SQLException {
        if (mysql) {
            Properties props = new Properties();
            props.setProperty("user", username == null ? "" : username);
            props.setProperty("password", password == null ? "" : password);
            return DriverManager.getConnection(jdbcUrl, props);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    public void init() {
        try (Connection conn = connect();
             Statement st = conn.createStatement()) {
            if (mysql) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS accounts ("
                        + "owner VARCHAR(64) PRIMARY KEY,"
                        + "balance DOUBLE NOT NULL DEFAULT 0,"
                        + "bank DOUBLE NOT NULL DEFAULT 0,"
                        + "credit INT NOT NULL DEFAULT 500)");
            } else {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS accounts ("
                        + "owner TEXT PRIMARY KEY,"
                        + "balance REAL NOT NULL DEFAULT 0,"
                        + "bank REAL NOT NULL DEFAULT 0,"
                        + "credit INTEGER NOT NULL DEFAULT 500)");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise database schema", e);
        }
    }

    @Override
    public void saveAccount(Account account) {
        String upsert = mysql
                ? "INSERT INTO accounts (owner, balance, bank, credit) VALUES (?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE balance=VALUES(balance), bank=VALUES(bank), credit=VALUES(credit)"
                : "INSERT OR REPLACE INTO accounts (owner, balance, bank, credit) VALUES (?,?,?,?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(upsert)) {
            ps.setString(1, account.getOwner());
            ps.setDouble(2, account.getBalance());
            ps.setDouble(3, account.getBankBalance());
            ps.setInt(4, account.getCreditScore());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save account for " + account.getOwner(), e);
        }
    }

    @Override
    public Optional<Account> loadAccount(String owner) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT owner, balance, bank, credit FROM accounts WHERE owner = ?")) {
            ps.setString(1, owner);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Account acc = new Account(rs.getString("owner"), 0.0);
                acc.restoreForLoad(rs.getDouble("balance"), rs.getDouble("bank"), rs.getInt("credit"));
                return Optional.of(acc);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load account for " + owner, e);
        }
    }

    @Override
    public Map<String, Account> loadAllAccounts(double startingBalance) {
        Map<String, Account> result = new HashMap<>();
        try (Connection conn = connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT owner, balance, bank, credit FROM accounts")) {
            while (rs.next()) {
                String owner = rs.getString("owner");
                Account acc = new Account(owner, startingBalance);
                acc.restoreForLoad(rs.getDouble("balance"), rs.getDouble("bank"), rs.getInt("credit"));
                result.put(owner, acc);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all accounts", e);
        }
        return result;
    }

    @Override
    public void deleteAccount(String owner) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM accounts WHERE owner = ?")) {
            ps.setString(1, owner);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete account for " + owner, e);
        }
    }

    @Override
    public void shutdown() {
        // connections are opened per operation
    }
}