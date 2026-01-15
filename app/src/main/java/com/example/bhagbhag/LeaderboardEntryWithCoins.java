package com.example.bhagbhag;

public class LeaderboardEntryWithCoins extends LeaderboardEntry {
    private long coins;
    private long timestamp;

    public LeaderboardEntryWithCoins(String userName, long score, long coins) {
        super(userName, score);
        this.coins = coins;
    }

    public LeaderboardEntryWithCoins(String userName, long score, long coins, long timestamp) {
        super(userName, score);
        this.coins = coins;
        this.timestamp = timestamp;
    }

    public long getCoins() {
        return coins;
    }

    public long getScore() {
        return super.getScore();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
