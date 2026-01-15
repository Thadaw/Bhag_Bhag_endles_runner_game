package com.example.bhagbhag;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Set;
import java.util.HashSet;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.bhagbhag.MusicManager;
import android.content.Intent;

public class GameActivity extends AppCompatActivity implements GameView.ScoreUpdateListener, GameView.GameOverListener {
    private static final String TAG = "GameActivity";
    private GameView gameView;
    private TextView scoreText, coinText;
    private TextView userNameText;
    private Button exitButton;
    private android.widget.ImageButton jumpButton;
    private android.widget.ImageButton slideButton;
    private android.widget.Button pauseButton;
    // Game Over Overlay elements
    private View gameOverOverlay;
    private TextView finalScoreText, finalCoinsText;
    private ImageButton restartGameButton;
    private ImageButton backToMenuButton;
    private DatabaseReference databaseReference;
    private boolean isGamePlaying = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MusicManager.stopMenuMusic(); // Stop menu music when entering the game
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_game);
            databaseReference = FirebaseDatabase.getInstance().getReference("game_scores");
            initializeViews();
            setUserName();
            if (gameView != null) {
                gameView.setGameOverListener(this);
            }
            onScoreUpdate(0);
            onCoinUpdate(0);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error starting game", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            gameView = findViewById(R.id.gameView);
            if (gameView == null) throw new RuntimeException("Failed to initialize GameView");
            scoreText = findViewById(R.id.scoreText);
            if (scoreText == null) throw new RuntimeException("Failed to initialize scoreText");
            scoreText.setText(getString(R.string.score, 0));
            coinText = findViewById(R.id.coinText);
            if (coinText == null) throw new RuntimeException("Failed to initialize coinText");
            coinText.setText(getString(R.string.coins, 0));
            userNameText = findViewById(R.id.userNameText);
            if (userNameText == null) throw new RuntimeException("Failed to initialize userNameText");
            exitButton = findViewById(R.id.exitButton);
            if (exitButton == null) throw new RuntimeException("Failed to initialize exitButton");
            pauseButton = findViewById(R.id.pauseButton);
            if (pauseButton == null) throw new RuntimeException("Failed to initialize pauseButton");
            jumpButton = findViewById(R.id.jumpButton);
            if (jumpButton == null) throw new RuntimeException("Failed to initialize jumpButton");
            slideButton = findViewById(R.id.slideButton);
            if (slideButton == null) throw new RuntimeException("Failed to initialize slideButton");
            gameOverOverlay = findViewById(R.id.gameOverOverlay);
            if (gameOverOverlay == null) throw new RuntimeException("Failed to initialize gameOverOverlay");
            finalScoreText = findViewById(R.id.finalScoreText);
            if (finalScoreText == null) throw new RuntimeException("Failed to initialize finalScoreText");
            finalCoinsText = findViewById(R.id.finalCoinsText);
            if (finalCoinsText == null) throw new RuntimeException("Failed to initialize finalCoinsText");
            restartGameButton = findViewById(R.id.restartGameButton);
            if (restartGameButton == null) throw new RuntimeException("Failed to initialize restartGameButton");
            backToMenuButton = findViewById(R.id.backToMenuButton);
            if (backToMenuButton == null) throw new RuntimeException("Failed to initialize backToMenuButton");

            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gameView != null) gameView.pause();
                    MusicManager.stopGameMusic();
                    MusicManager.releaseGameMusic();
                    Intent intent = new Intent(GameActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            });
            pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gameView != null) {
                        if (isGamePlaying) {
                            gameView.pause();
                            isGamePlaying = false;
                        } else {
                            gameView.resume();
                            isGamePlaying = true;
                        }
                    }
                }
            });
            jumpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gameView != null) {
                        gameView.performJump();
                    }
                }
            });
            slideButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gameView != null) {
                        gameView.performSlide();
                    }
                }
            });
            restartGameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gameView != null) {
                        hideGameOverOverlay();
                        showGameControls(true);
                        gameView.setShowGameOverText(true);
                        gameView.restartGame();
                        onScoreUpdate(0);
                        onCoinUpdate(0);
                    }
                }
            });
            backToMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            gameView.setScoreUpdateListener(this);
            Log.d(TAG, "Views initialized successfully");
            if (gameView != null) {
                gameView.setGameOverListener(this);
                gameView.setJumpPhysics(-180, 1.5f);
                gameView.restartGame();
            }
            showGameControls(true);
            hideGameOverOverlay();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            throw e;
        }
    }

    private void showGameControls(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        pauseButton.setVisibility(visibility);
        jumpButton.setVisibility(visibility);
        slideButton.setVisibility(visibility);
        exitButton.setVisibility(visibility);
    }

    private void setUserName() {
        SharedPreferences userPrefs = getSharedPreferences("BhagBhagPrefs", Context.MODE_PRIVATE);
        String googleUserName = userPrefs.getString("googleUserName", null);
        Log.d(TAG, "setUserName called - googleUserName: " + googleUserName);
        
        // Check for custom player name first (for both Google and offline users)
        String customPlayerName = userPrefs.getString("offlinePlayerName", null);
        Log.d(TAG, "Custom player name from prefs: " + customPlayerName);
        
        if (customPlayerName != null && !customPlayerName.trim().isEmpty() && userNameText != null) {
            // Use custom player name if set (for both Google and offline users)
            runOnUiThread(() -> userNameText.setText(customPlayerName));
            Log.d(TAG, "Using custom player name: " + customPlayerName);
        } else if (googleUserName != null && userNameText != null) {
            // Fallback to Google Sign-In user name
            runOnUiThread(() -> userNameText.setText(googleUserName));
            Log.d(TAG, "Using Google Sign-In user: " + googleUserName);
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && userNameText != null) {
                String name = user.getDisplayName();
                if (name == null || name.isEmpty()) {
                    name = user.getEmail();
                }
                final String displayName = name != null ? name : "Player";
                runOnUiThread(() -> userNameText.setText(displayName));
                Log.d(TAG, "Using Firebase user: " + displayName);
            } else {
                // Fallback to default player name
                runOnUiThread(() -> userNameText.setText("Player"));
                Log.d(TAG, "Using default player name: Player");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (gameView != null) {
                gameView.pause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (gameView != null) {
                gameView.resume();
                gameView.onSettingsChanged();
                gameView.setJumpPhysics(-180, 1.5f);
                gameView.restartGame();
            }
            // Always refresh the user name display when resuming
            setUserName();
            if (isNetworkAvailable()) {
                syncLocalScoresToFirebase();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage());
        }
    }

    @Override
    public void onScoreUpdate(final int score) {
        try {
            runOnUiThread(() -> {
                try {
                    if (scoreText != null) {
                        scoreText.setText(getString(R.string.score, score));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating score text: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onScoreUpdate: " + e.getMessage());
        }
    }

    @Override
    public void onCoinUpdate(final int coins) {
        try {
            runOnUiThread(() -> {
                try {
                    if (coinText != null) {
                        coinText.setText(getString(R.string.coins, coins));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating coin text: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCoinUpdate: " + e.getMessage());
        }
    }

    @Override
    public void onGameOver(int finalScore, int finalCoins) {
        Log.d(TAG, "onGameOver called with score: " + finalScore + ", coins: " + finalCoins);
        runOnUiThread(() -> {
            Log.d(TAG, "onGameOver UI thread - score: " + finalScore + ", coins: " + finalCoins);
            if (finalScoreText != null) {
                finalScoreText.setText(String.valueOf(finalScore));
            }
            if (finalCoinsText != null) {
                finalCoinsText.setText(String.valueOf(finalCoins));
            }
            setGameOverInnerBackground("game_over_background");
            if (gameOverOverlay != null) {
                gameOverOverlay.setVisibility(View.VISIBLE);
                Log.d(TAG, "Game over overlay set to VISIBLE");
            } else {
                Log.e(TAG, "Game over overlay is null in onGameOver");
            }
            showGameControls(false);
            if (gameView != null) {
                gameView.setShowGameOverText(false);
            }
            Log.d(TAG, "About to save score and coins...");
            saveScoreAndCoins(finalScore, finalCoins);
            Log.d(TAG, "Score and coins saved successfully");
        });
    }

    private void saveScoreAndCoins(int score, int coins) {
        Log.d(TAG, "=== saveScoreAndCoins START ===");
        Log.d(TAG, "Saving score: " + score + ", coins: " + coins);
        SharedPreferences prefs = getSharedPreferences("BhagBhagPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String userName;
        SharedPreferences userPrefs = getSharedPreferences("BhagBhagPrefs", Context.MODE_PRIVATE);
        String googleUserName = userPrefs.getString("googleUserName", null);
        
        // Check for custom player name first (for both Google and offline users)
        String customPlayerName = userPrefs.getString("offlinePlayerName", null);
        Log.d(TAG, "Custom player name from prefs: " + customPlayerName);
        Log.d(TAG, "Google user name from prefs: " + googleUserName);
        
        if (customPlayerName != null && !customPlayerName.trim().isEmpty()) {
            userName = customPlayerName;
            Log.d(TAG, "Using custom player name for score: " + userName);
        } else if (googleUserName != null) {
            userName = googleUserName;
            Log.d(TAG, "Using Google Sign-In user name for score: " + userName);
        } else {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String name = currentUser.getDisplayName();
                if (name == null || name.isEmpty()) {
                    name = currentUser.getEmail();
                }
                userName = name != null ? name : "Player";
                Log.d(TAG, "Using Firebase user name for score: " + userName);
            } else {
                // Generate a unique player name for offline users
                userName = generateUniquePlayerName(prefs);
                Log.d(TAG, "Using auto-generated name for score: " + userName);
            }
        }
        Set<String> highScoresSet = new java.util.HashSet<>(prefs.getStringSet("offlineScores", new java.util.HashSet<>()));
        Set<String> coinScoresSet = new java.util.HashSet<>(prefs.getStringSet("offlineCoins", new java.util.HashSet<>()));
        long currentTime = System.currentTimeMillis();
        String scoreEntry = userName + ":" + score + ":" + currentTime;
        String coinEntry = userName + ":" + coins + ":" + currentTime;
        highScoresSet.add(scoreEntry);
        coinScoresSet.add(coinEntry);
        Log.d(TAG, "Saving score entry: " + scoreEntry);
        Log.d(TAG, "Saving coin entry: " + coinEntry);
        editor.putStringSet("offlineScores", highScoresSet);
        editor.putStringSet("offlineCoins", coinScoresSet);
        if (!isNetworkAvailable()) {
            Set<String> unsyncedScores = new java.util.HashSet<>(prefs.getStringSet("unsyncedScores", new java.util.HashSet<>()));
            unsyncedScores.add(userName + ":" + score + ":" + coins + ":" + currentTime);
            editor.putStringSet("unsyncedScores", unsyncedScores);
        }
        editor.apply();
        if (isNetworkAvailable()) {
            syncLocalScoresToFirebase();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userId = user.getUid();
                DatabaseReference userRef = databaseReference.child(userId);
                
                // Use custom player name if available, otherwise fall back to Google name
                String displayName = userPrefs.getString("offlinePlayerName", null);
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = userPrefs.getString("googleUserName", null);
                    if (displayName == null) {
                        displayName = user.getDisplayName();
                        if (displayName == null || displayName.isEmpty()) {
                            displayName = user.getEmail();
                        }
                    }
                }
                Log.d(TAG, "Saving to Firebase with display name: " + displayName);
                userRef.child("displayName").setValue(displayName);
                String key = userRef.child("scores").push().getKey();
                if (key != null) {
                    java.util.Map<String, Object> scoreData = new java.util.HashMap<>();
                    scoreData.put("score", score);
                    scoreData.put("coins", coins);
                    scoreData.put("timestamp", currentTime);
                    userRef.child("scores").child(key).setValue(scoreData)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Score saved to Firebase for user: " + userId + ", key: " + key);
                            } else {
                                Log.e(TAG, "Failed to save score to Firebase for user: " + userId + ", key: " + key, task.getException());
                            }
                        });
                }
            }
        }
        Log.d(TAG, "=== saveScoreAndCoins END ===");
    }

    /**
     * Generates a unique player name for offline users
     * Uses a combination of device ID and random number for uniqueness
     */
    private String generateUniquePlayerName(SharedPreferences prefs) {
        // Try to get existing player name first (custom or auto-generated)
        String existingPlayerName = prefs.getString("offlinePlayerName", null);
        if (existingPlayerName != null && !existingPlayerName.trim().isEmpty()) {
            return existingPlayerName;
        }

        // Generate a new unique player name
        String deviceId = android.provider.Settings.Secure.getString(
            getContentResolver(), 
            android.provider.Settings.Secure.ANDROID_ID
        );
        
        // Create a shorter, more user-friendly identifier
        String shortDeviceId = deviceId != null ? deviceId.substring(0, Math.min(8, deviceId.length())) : "DEV";
        int randomNum = (int)(Math.random() * 1000);
        
        String playerName = "Player_" + shortDeviceId + "_" + randomNum;
        
        // Save the generated name for future use
        prefs.edit().putString("offlinePlayerName", playerName).apply();
        
        Log.d(TAG, "Generated unique player name: " + playerName);
        return playerName;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void syncLocalScoresToFirebase() {
        SharedPreferences prefs = getSharedPreferences("BhagBhagPrefs", Context.MODE_PRIVATE);
        Set<String> unsyncedScores = new java.util.HashSet<>(prefs.getStringSet("unsyncedScores", new java.util.HashSet<>()));
        if (unsyncedScores.isEmpty()) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        DatabaseReference userRef = databaseReference.child(user.getUid()).child("scores");
        for (String entry : unsyncedScores) {
            String[] parts = entry.split(":");
            if (parts.length >= 4) {
                String userName = parts[0];
                long score = Long.parseLong(parts[1]);
                long coins = Long.parseLong(parts[2]);
                long timestamp = Long.parseLong(parts[3]);
                java.util.Map<String, Object> scoreData = new java.util.HashMap<>();
                scoreData.put("score", score);
                scoreData.put("coins", coins);
                scoreData.put("timestamp", timestamp);
                String key = userRef.push().getKey();
                if (key != null) {
                    userRef.child(key).setValue(scoreData)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Unsynced score saved to Firebase for user: " + user.getUid() + ", key: " + key);
                            } else {
                                Log.e(TAG, "Failed to save unsynced score to Firebase for user: " + user.getUid() + ", key: " + key, task.getException());
                            }
                        });
                }
            }
        }
        prefs.edit().remove("unsyncedScores").apply();
    }

    private void hideGameOverOverlay() {
        if (gameOverOverlay != null) {
            gameOverOverlay.setVisibility(View.GONE);
        }
    }

    public void setGameOverBackground(int drawableResourceId) {
        if (gameOverOverlay != null) {
            gameOverOverlay.setBackgroundResource(drawableResourceId);
        }
    }

    public void setGameOverBackground(String drawableName) {
        if (gameOverOverlay != null) {
            try {
                int resourceId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
                if (resourceId != 0) {
                    gameOverOverlay.setBackgroundResource(resourceId);
                } else {
                    Log.e(TAG, "Drawable resource not found: " + drawableName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting background: " + e.getMessage());
            }
        }
    }

    public void setGameOverInnerBackground(String drawableName) {
        try {
            View innerLayout = findViewById(R.id.gameOverInnerLayout);
            if (innerLayout != null) {
                int resourceId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
                if (resourceId != 0) {
                    innerLayout.setBackgroundResource(resourceId);
                } else {
                    Log.e(TAG, "Drawable resource not found: " + drawableName);
                }
            } else {
                Log.e(TAG, "Inner layout not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting inner background: " + e.getMessage());
        }
    }

    public void resetGameOverBackground() {
        if (gameOverOverlay != null) {
            gameOverOverlay.setBackgroundColor(0x80000000); // Semi-transparent black
        }
    }

    public void setGameOverBackgroundByScore(int score) {
        if (score >= 1000) {
            setGameOverBackground("game_over_high_score");
        } else if (score >= 500) {
            setGameOverBackground("game_over_medium");
        } else {
            setGameOverBackground("game_over_default");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicManager.releaseGameMusic();
    }
}
