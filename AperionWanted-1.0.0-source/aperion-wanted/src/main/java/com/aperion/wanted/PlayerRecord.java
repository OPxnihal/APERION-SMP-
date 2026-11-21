package com.aperion.wanted;

import java.util.UUID;

public final class PlayerRecord {
    public final UUID uuid;
    public String lastName;
    public int kills;
    public int streak;
    public int score;
    public int bounty;

    public PlayerRecord(UUID uuid, String lastName, int kills, int streak, int score, int bounty) {
        this.uuid = uuid;
        this.lastName = lastName;
        this.kills = kills;
        this.streak = streak;
        this.score = score;
        this.bounty = bounty;
    }
}
