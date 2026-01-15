package com.example.bhagbhag;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LeaderboardActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BhagBhagPrefs";
    private static final String HIGH_SCORES_KEY = "offlineScores";
    private static final String COIN_SCORES_KEY = "offlineCoins";
    private ListView leaderboardListView;
    private TextView noScoresTextView;
    private Button backButton;
    private Button refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        leaderboardListView = findViewById(R.id.leaderboardListView);
        noScoresTextView = findViewById(R.id.noScoresTextView);
        backButton = findViewById(R.id.backButton);
        refreshButton = findViewById(R.id.refreshButton);

        if (isNetworkAvailable() && isGoogleSignedIn()) {
            loadScoresFromFirebase();
        } else {
            loadAndDisplayScores();
        }

        backButton.setOnClickListener(v -> {
            finish();
        });

        refreshButton.setOnClickListener(v -> {
            if (isNetworkAvailable() && isGoogleSignedIn()) {
                loadScoresFromFirebase();
            } else {
                loadAndDisplayScores();
            }
            Toast.makeText(this, "Leaderboard refreshed!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the leaderboard to show latest scores based on network and Google sign-in status
        if (isNetworkAvailable() && isGoogleSignedIn()) {
            loadScoresFromFirebase();
        } else {
            loadAndDisplayScores();
        }
    }

    private void loadAndDisplayScores() {
        cleanupSharedPreferences();
        
        List<LeaderboardEntryWithCoins> scoreList = loadScoresFromSharedPreferences();
        displayScores(scoreList);
    }

    private void cleanupSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> highScoresSet = prefs.getStringSet(HIGH_SCORES_KEY, new java.util.HashSet<>());
        Set<String> coinScoresSet = prefs.getStringSet(COIN_SCORES_KEY, new java.util.HashSet<>());

        java.util.Map<String, java.util.List<ScoreEntry>> userScoreEntries = new java.util.HashMap<>();
        java.util.Map<String, java.util.List<ScoreEntry>> userCoinEntries = new java.util.HashMap<>();

        for (String entry : highScoresSet) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                try {
                    String userName = parts[0];
                    long score = Long.parseLong(parts[1]);
                    long timestamp = parts.length >= 3 ? Long.parseLong(parts[2]) : System.currentTimeMillis();
                    
                    userScoreEntries.computeIfAbsent(userName, k -> new java.util.ArrayList<>())
                        .add(new ScoreEntry(score, timestamp));
                } catch (NumberFormatException e) {
                    // Ignore invalid entries
                }
            }
        }

        for (String entry : coinScoresSet) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                try {
                    String userName = parts[0];
                    long coins = Long.parseLong(parts[1]);
                    long timestamp = parts.length >= 3 ? Long.parseLong(parts[2]) : System.currentTimeMillis();
                    
                    userCoinEntries.computeIfAbsent(userName, k -> new java.util.ArrayList<>())
                        .add(new ScoreEntry(coins, timestamp));
                } catch (NumberFormatException e) {
                    // Ignore invalid entries
                }
            }
        }

        Set<String> cleanHighScoresSet = new java.util.HashSet<>();
        Set<String> cleanCoinScoresSet = new java.util.HashSet<>();

        for (java.util.Map.Entry<String, java.util.List<ScoreEntry>> entry : userScoreEntries.entrySet()) {
            String userName = entry.getKey();
            java.util.List<ScoreEntry> scoreEntries = entry.getValue();

            scoreEntries.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
            if (scoreEntries.size() > 3) {
                scoreEntries = scoreEntries.subList(0, 3);
            }

            for (ScoreEntry scoreEntry : scoreEntries) {
                cleanHighScoresSet.add(userName + ":" + scoreEntry.score + ":" + scoreEntry.timestamp);
            }
        }

        for (java.util.Map.Entry<String, java.util.List<ScoreEntry>> entry : userCoinEntries.entrySet()) {
            String userName = entry.getKey();
            java.util.List<ScoreEntry> coinEntries = entry.getValue();

            coinEntries.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
            if (coinEntries.size() > 3) {
                coinEntries = coinEntries.subList(0, 3);
            }

            for (ScoreEntry coinEntry : coinEntries) {
                cleanCoinScoresSet.add(userName + ":" + coinEntry.score + ":" + coinEntry.timestamp);
            }
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet(HIGH_SCORES_KEY, cleanHighScoresSet);
        editor.putStringSet(COIN_SCORES_KEY, cleanCoinScoresSet);
        editor.apply();

        android.util.Log.d("LeaderboardActivity", "Cleaned SharedPreferences - Kept only 3 most recent scores per user");
    }

    private List<LeaderboardEntryWithCoins> loadScoresFromSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<LeaderboardEntryWithCoins> scoreList = new ArrayList<>();

        Set<String> highScoresSet = prefs.getStringSet(HIGH_SCORES_KEY, new java.util.HashSet<>());
        Set<String> coinScoresSet = prefs.getStringSet(COIN_SCORES_KEY, new java.util.HashSet<>());

        android.util.Log.d("LeaderboardActivity", "Loading scores - High scores: " + highScoresSet.size() + ", Coin scores: " + coinScoresSet.size());

        for (String entry : highScoresSet) {
            android.util.Log.d("LeaderboardActivity", "Score entry: " + entry);
        }

        java.util.Map<String, java.util.List<ScoreEntry>> userScoreEntries = new java.util.HashMap<>();
        java.util.Map<String, java.util.List<ScoreEntry>> userCoinEntries = new java.util.HashMap<>();

        for (String entry : highScoresSet) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                try {
                    String userName = parts[0];
                    long score = Long.parseLong(parts[1]);
                    long timestamp = parts.length >= 3 ? Long.parseLong(parts[2]) : System.currentTimeMillis();
                    
                    userScoreEntries.computeIfAbsent(userName, k -> new java.util.ArrayList<>())
                        .add(new ScoreEntry(score, timestamp));
                    
                    android.util.Log.d("LeaderboardActivity", "Added score entry for " + userName + ": " + score + " at " + timestamp);
                } catch (NumberFormatException e) {
                    android.util.Log.e("LeaderboardActivity", "Error parsing score: " + entry);
                }
            }
        }

        for (String entry : coinScoresSet) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                try {
                    String userName = parts[0];
                    long coins = Long.parseLong(parts[1]);
                    long timestamp = parts.length >= 3 ? Long.parseLong(parts[2]) : System.currentTimeMillis();
                    
                    userCoinEntries.computeIfAbsent(userName, k -> new java.util.ArrayList<>())
                        .add(new ScoreEntry(coins, timestamp));
                    
                    android.util.Log.d("LeaderboardActivity", "Added coin entry for " + userName + ": " + coins + " at " + timestamp);
                } catch (NumberFormatException e) {
                    android.util.Log.e("LeaderboardActivity", "Error parsing coins: " + entry);
                }
            }
        }

        java.util.Set<String> allUsers = new java.util.HashSet<>();
        allUsers.addAll(userScoreEntries.keySet());
        allUsers.addAll(userCoinEntries.keySet());

        for (String userName : allUsers) {
            java.util.List<ScoreEntry> scoreEntries = userScoreEntries.getOrDefault(userName, new java.util.ArrayList<>());
            java.util.List<ScoreEntry> coinEntries = userCoinEntries.getOrDefault(userName, new java.util.ArrayList<>());
            
            // Sort by timestamp (newest first) and keep only the 3 most recent
            scoreEntries.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
            coinEntries.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

            if (scoreEntries.size() > 3) {
                scoreEntries = scoreEntries.subList(0, 3);
            }
            if (coinEntries.size() > 3) {
                coinEntries = coinEntries.subList(0, 3);
            }

            int maxGames = Math.max(3, Math.max(scoreEntries.size(), coinEntries.size()));
            for (int i = 0; i < maxGames; i++) {
                long score = i < scoreEntries.size() ? scoreEntries.get(i).score : 0L;
                long coins = i < coinEntries.size() ? coinEntries.get(i).score : 0L;

                String displayName = formatPlayerDisplayName(userName, i + 1, scoreEntries.size(), coinEntries.size());
                LeaderboardEntryWithCoins entry = new LeaderboardEntryWithCoins(displayName, score, coins);
                scoreList.add(entry);
                android.util.Log.d("LeaderboardActivity", "Created entry: " + displayName + " - Score: " + score + ", Coins: " + coins);
            }
        }

        android.util.Log.d("LeaderboardActivity", "Total entries created: " + scoreList.size());
        return scoreList;
    }

    private void displayScores(List<LeaderboardEntryWithCoins> scoreList) {
        android.util.Log.d("LeaderboardActivity", "Displaying " + scoreList.size() + " scores");

        if (scoreList.isEmpty()) {
            noScoresTextView.setVisibility(View.VISIBLE);
            leaderboardListView.setVisibility(View.GONE);
            android.util.Log.d("LeaderboardActivity", "No scores to display");
        } else {
            noScoresTextView.setVisibility(View.GONE);
            leaderboardListView.setVisibility(View.VISIBLE);

            Collections.sort(scoreList, (a, b) -> Long.compare(b.getScore(), a.getScore()));

            LeaderboardAdapter adapter = new LeaderboardAdapter(this, R.layout.list_item_leaderboard, scoreList);
            leaderboardListView.setAdapter(adapter);

            android.util.Log.d("LeaderboardActivity", "Adapter set with " + scoreList.size() + " items");

            for (int i = 0; i < scoreList.size(); i++) {
                LeaderboardEntryWithCoins entry = scoreList.get(i);
                android.util.Log.d("LeaderboardActivity", "Entry " + (i+1) + ": " + entry.getUserName() + 
                    " - Score: " + entry.getScore() + ", Coins: " + entry.getCoins());
            }
        }
    }

    private void loadScoresFromFirebase() {
        DatabaseReference scoresRef = FirebaseDatabase.getInstance().getReference("game_scores");
        scoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<LeaderboardEntryWithCoins> firebaseScoreList = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    // Fetch display name if available, otherwise fallback to userId
                    String displayName = userSnapshot.child("displayName").getValue(String.class);
                    android.util.Log.d("LeaderboardActivity", "Firebase user " + userSnapshot.getKey() + " has display name: " + displayName);
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = userSnapshot.getKey();
                        android.util.Log.d("LeaderboardActivity", "Using userId as display name: " + displayName);
                    }
                    DataSnapshot scoresNode = userSnapshot.child("scores");
                    for (DataSnapshot scoreSnapshot : scoresNode.getChildren()) {
                        Long score = scoreSnapshot.child("score").getValue(Long.class);
                        Long coins = scoreSnapshot.child("coins").getValue(Long.class);
                        Long timestamp = scoreSnapshot.child("timestamp").getValue(Long.class);
                        if (score != null && coins != null && timestamp != null) {
                            firebaseScoreList.add(new LeaderboardEntryWithCoins(displayName, score, coins, timestamp));
                            android.util.Log.d("LeaderboardActivity", "Added Firebase entry: " + displayName + " - Score: " + score + ", Coins: " + coins);
                        }
                    }
                }
                // Sort by score descending
                Collections.sort(firebaseScoreList, (a, b) -> Long.compare(b.getScore(), a.getScore()));
                displayScores(firebaseScoreList);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(LeaderboardActivity.this, "Failed to load leaderboard from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    private boolean isGoogleSignedIn() {
        return com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null;
    }
    private static class ScoreEntry {
        long score;
        long timestamp;
        
        ScoreEntry(long score, long timestamp) {
            this.score = score;
            this.timestamp = timestamp;
        }
    }

    private String formatPlayerDisplayName(String userName, int gameNumber, int totalGamesPlayed, int totalGamesWon) {
        if (userName.startsWith("Player_")) {
            String shortName = userName.replace("Player_", "");
            if (shortName.length() > 12) {
                shortName = shortName.substring(0, 12) + "...";
            }
            return shortName + " (Game " + gameNumber + ")";
        } else {
            String displayName = userName;
            if (totalGamesPlayed > 1) {
                displayName += " (Game " + gameNumber + ")";
            }
            return displayName;
        }
    }
}
