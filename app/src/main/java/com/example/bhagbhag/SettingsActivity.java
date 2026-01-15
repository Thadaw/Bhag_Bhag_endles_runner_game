package com.example.bhagbhag;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.example.bhagbhag.MusicManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private Button backButtonSettings;
    private Switch switchMusic;
    private SeekBar seekBarVolume;
    private android.widget.EditText editTextPlayerName;
    private Button buttonSavePlayerName;
    private Button buttonResetPlayerName;
    private Button logoutButton;
    private Button exitGameButton;
    private GoogleSignInClient mGoogleSignInClient;

    public static final String PREFS_NAME = "BhagBhagPrefs";
    public static final String MUSIC_ENABLED_KEY = "musicEnabled";
    public static final String MUSIC_VOLUME_KEY = "musicVolume";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        backButtonSettings = findViewById(R.id.backButtonSettings);
        switchMusic = findViewById(R.id.switchMusic);
        seekBarVolume = findViewById(R.id.seekBarVolume);
        editTextPlayerName = findViewById(R.id.editTextPlayerName);
        buttonSavePlayerName = findViewById(R.id.buttonSavePlayerName);
        buttonResetPlayerName = findViewById(R.id.buttonResetPlayerName);
        logoutButton = findViewById(R.id.logoutButton);
        exitGameButton = findViewById(R.id.exitGameButton);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        loadSettings();

        backButtonSettings.setOnClickListener(v -> {
            finish();
        });

        switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveMusicEnabled(isChecked);
            seekBarVolume.setEnabled(isChecked); // Enable/disable volume seekbar based on music switch
            if (isChecked) {
                MusicManager.playMenuMusic(SettingsActivity.this);
            } else {
                MusicManager.stopMenuMusic();
            }
        });

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    saveMusicVolume(progress);
                    MusicManager.updateAllMusicVolumes(SettingsActivity.this);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
        });

        logoutButton.setOnClickListener(v -> {
            // Firebase sign out
            FirebaseAuth.getInstance().signOut();

            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                // After sign out, revoke access to fully disconnect the user
                mGoogleSignInClient.revokeAccess().addOnCompleteListener(this, revokeTask -> {
                    // Clear any local user session or preferences if needed
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();

                    // Redirect to MainActivity (Google Sign-In screen)
                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            });
        });

        buttonSavePlayerName.setOnClickListener(v -> {
            String playerName = editTextPlayerName.getText().toString().trim();
            if (playerName.isEmpty()) {
                Toast.makeText(SettingsActivity.this, "Please enter a player name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (playerName.length() > 15) {
                Toast.makeText(SettingsActivity.this, "Player name too long (max 15 characters)", Toast.LENGTH_SHORT).show();
                return;
            }
            savePlayerName(playerName);
            Toast.makeText(SettingsActivity.this, "Custom name saved! It will appear in the game.", Toast.LENGTH_SHORT).show();
        });

        buttonResetPlayerName.setOnClickListener(v -> {
            // Clear the custom player name to use default one
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove("offlinePlayerName").apply();
            editTextPlayerName.setText("");
            
            // Update hint based on current user type
            String googleUserName = prefs.getString("googleUserName", null);
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            
            if (googleUserName != null) {
                editTextPlayerName.setHint("Google user: " + googleUserName + " (enter custom name)");
            } else if (firebaseUser != null) {
                String firebaseName = firebaseUser.getDisplayName();
                if (firebaseName == null || firebaseName.isEmpty()) {
                    firebaseName = firebaseUser.getEmail();
                }
                editTextPlayerName.setHint("Firebase user: " + firebaseName + " (enter custom name)");
            } else {
                editTextPlayerName.setHint("Enter your player name");
            }
            
            Toast.makeText(SettingsActivity.this, "Custom name reset! Default name will be used.", Toast.LENGTH_SHORT).show();
        });

        exitGameButton.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Exiting app...", Toast.LENGTH_SHORT).show();
            // Stop music before exiting
            MusicManager.stopMenuMusic();
            MusicManager.releaseMenuMusic();
            // Finish all activities and quit app
            finishAffinity(); // Finishes all activities in the task
            System.exit(0); // Ensures the process is killed
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.playMenuMusic(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.stopMenuMusic();
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean musicEnabled = prefs.getBoolean(MUSIC_ENABLED_KEY, true); // Default to true
        int musicVolume = prefs.getInt(MUSIC_VOLUME_KEY, 70); // Default to 70%
        String playerName = prefs.getString("offlinePlayerName", ""); // Load saved player name

        switchMusic.setChecked(musicEnabled);
        seekBarVolume.setProgress(musicVolume);
        seekBarVolume.setEnabled(musicEnabled); // Initial state of seekbar
        
        // Check if user is Google signed in
        String googleUserName = prefs.getString("googleUserName", null);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Set the current player name and show appropriate hint
        if (playerName != null && !playerName.trim().isEmpty()) {
            editTextPlayerName.setText(playerName);
            editTextPlayerName.setHint("Current custom name: " + playerName);
        } else if (googleUserName != null) {
            editTextPlayerName.setText("");
            editTextPlayerName.setHint("Google user: " + googleUserName + " (enter custom name)");
        } else if (firebaseUser != null) {
            String firebaseName = firebaseUser.getDisplayName();
            if (firebaseName == null || firebaseName.isEmpty()) {
                firebaseName = firebaseUser.getEmail();
            }
            editTextPlayerName.setText("");
            editTextPlayerName.setHint("Firebase user: " + firebaseName + " (enter custom name)");
        } else {
            editTextPlayerName.setText("");
            editTextPlayerName.setHint("Enter your player name");
        }
    }

    private void saveMusicEnabled(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(MUSIC_ENABLED_KEY, enabled);
        editor.apply();
    }

    private void saveMusicVolume(int volume) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(MUSIC_VOLUME_KEY, volume);
        editor.apply();
    }

    private void savePlayerName(String playerName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("offlinePlayerName", playerName);
        editor.apply();
    }
}
