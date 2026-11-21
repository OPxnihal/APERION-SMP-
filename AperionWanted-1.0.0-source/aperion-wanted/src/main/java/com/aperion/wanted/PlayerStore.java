package com.aperion.wanted;

import java.io.File;
import java.sql.*;
import java.util.UUID;

final class PlayerStore implements AutoCloseable {
    private final Connection connection;

    PlayerStore(File file) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("PRAGMA journal_mode=WAL");
            s.executeUpdate("PRAGMA synchronous=NORMAL");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, name TEXT NOT NULL, kills INTEGER NOT NULL DEFAULT 0, streak INTEGER NOT NULL DEFAULT 0, score INTEGER NOT NULL DEFAULT 0, bounty INTEGER NOT NULL DEFAULT 0)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS claims (killer TEXT NOT NULL, target TEXT NOT NULL, claimed_at INTEGER NOT NULL, PRIMARY KEY(killer,target))");
        }
    }

    synchronized PlayerRecord get(UUID id, String name) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("SELECT name,kills,streak,score,bounty FROM players WHERE uuid=?")) {
            p.setString(1, id.toString());
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    PlayerRecord record = new PlayerRecord(id, r.getString(1), r.getInt(2), r.getInt(3), r.getInt(4), r.getInt(5));
                    if (name != null && !name.equals(record.lastName)) { record.lastName = name; save(record); }
                    return record;
                }
            }
        }
        PlayerRecord fresh = new PlayerRecord(id, name == null ? "Unknown" : name, 0, 0, 0, 0);
        save(fresh);
        return fresh;
    }

    synchronized void save(PlayerRecord r) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("INSERT INTO players(uuid,name,kills,streak,score,bounty) VALUES(?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,kills=excluded.kills,streak=excluded.streak,score=excluded.score,bounty=excluded.bounty")) {
            p.setString(1, r.uuid.toString()); p.setString(2, r.lastName); p.setInt(3, r.kills); p.setInt(4, r.streak); p.setInt(5, r.score); p.setInt(6, r.bounty); p.executeUpdate();
        }
    }

    synchronized boolean mayClaim(UUID killer, UUID target, long cooldownMillis) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("SELECT claimed_at FROM claims WHERE (killer=? AND target=?) OR (killer=? AND target=?)")) {
            p.setString(1, killer.toString()); p.setString(2, target.toString()); p.setString(3, target.toString()); p.setString(4, killer.toString());
            try (ResultSet r = p.executeQuery()) { return !r.next() || System.currentTimeMillis() - r.getLong(1) >= cooldownMillis; }
        }
    }

    synchronized void recordClaim(UUID killer, UUID target) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("INSERT INTO claims(killer,target,claimed_at) VALUES(?,?,?) ON CONFLICT(killer,target) DO UPDATE SET claimed_at=excluded.claimed_at")) {
            p.setString(1, killer.toString()); p.setString(2, target.toString()); p.setLong(3, System.currentTimeMillis()); p.executeUpdate();
        }
    }

    public synchronized void close() throws SQLException { connection.close(); }
}
