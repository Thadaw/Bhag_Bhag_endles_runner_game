package com.example.bhagbhag;

public class LeaderboardEntry {
    private String userName;
    private long score;

    public LeaderboardEntry(String userName, long score) {
        this.userName = userName;
        this.score = score;
    }

    public String getUserName() {
        return userName;
    }

    public long getScore() {
        return score;
    }
} 