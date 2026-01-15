package com.example.bhagbhag;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bhagbhag.CharacterActivity;
import com.example.bhagbhag.GameActivity;
import com.example.bhagbhag.LeaderboardActivity;
import com.example.bhagbhag.SettingsActivity;
import com.example.bhagbhag.MusicManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {
    private ImageButton startGameBtn, settingsBtn, leaderboardBtn;
    private Button characterBtn;
    private SignInButton googleSignInBtn;

    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        startGameBtn = findViewById(R.id.startGameBtn);
        characterBtn = findViewById(R.id.characterBtn);
        leaderboardBtn = findViewById(R.id.leaderboardBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        googleSignInBtn = findViewById(R.id.googleSignInBtn);

        Animation zoomAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_in_out);
        startGameBtn.startAnimation(zoomAnim);

        Animation characterZoomAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_in_out);
        characterBtn.startAnimation(characterZoomAnim);

        startGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("BhagBhagPrefs", Context.MODE_PRIVATE);
                String selectedCharacter = prefs.getString("selectedCharacter", "Advanture_Girl");
                Log.d("MainActivity", "Selected character: " + selectedCharacter);
                startActivity(new Intent(MainActivity.this, GameActivity.class));
            }
        });

        characterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CharacterActivity.class));
            }
        });

        leaderboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to LeaderboardActivity
                startActivity(new Intent(MainActivity.this, LeaderboardActivity.class));
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        googleSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in with Google
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUIWithGoogleAccount(account);
    }

    private void signIn() {
        // Check if Google Play Services is available
        if (!isGooglePlayServicesAvailable()) {
            Toast.makeText(this, "Google Play Services is not available", Toast.LENGTH_LONG).show();
            return;
        }

        // Sign out first to clear cache and force account picker
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, 9000).show();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign-In successful for: " + account.getEmail());

                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, authTask -> {
                        if (authTask.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "Firebase Auth successful for: " + (user != null ? user.getEmail() : "null"));
                        } else {
                            Log.w(TAG, "Firebase Auth failed", authTask.getException());
                        }
                    });

                updateUIWithGoogleAccount(account);
                Toast.makeText(this, "Signed in as: " + account.getEmail(), Toast.LENGTH_LONG).show();

            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                String errorMessage = "Google sign in failed: ";
                
                switch (e.getStatusCode()) {
                    case 12501:
                        errorMessage += "Sign in was cancelled by user";
                        break;
                    case 12500:
                        errorMessage += "Sign in currently in progress";
                        break;
                    case 12502:
                        errorMessage += "Sign in was cancelled";
                        break;
                    default:
                        errorMessage += "Error in sign in";
                        break;
                }
                
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateUIWithGoogleAccount(GoogleSignInAccount account) {
        if (account != null) {
            // User is signed in
            googleSignInBtn.setVisibility(View.GONE);

            SharedPreferences prefs = getSharedPreferences("BhagBhagPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("googleUserEmail", account.getEmail());

            String existingCustomName = prefs.getString("offlinePlayerName", null);
            if (existingCustomName == null || existingCustomName.trim().isEmpty()) {
                editor.putString("googleUserName", account.getDisplayName() != null ? account.getDisplayName() : account.getEmail());
                Log.d(TAG, "Saved Google user name: " + (account.getDisplayName() != null ? account.getDisplayName() : account.getEmail()));
            } else {
                Log.d(TAG, "Preserving existing custom name: " + existingCustomName);
            }
            
            editor.apply();
            Log.d(TAG, "User signed in: " + account.getEmail());
        } else {
            googleSignInBtn.setVisibility(View.VISIBLE);
        }
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
}
