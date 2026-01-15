package com.example.bhagbhag;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;

public class CharacterActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "BhagBhagPrefs";
    private static final String SELECTED_CHARACTER = "selectedCharacter";

    private ImageButton advantureGirlCharacter;
    private ImageButton ninjaCharacter;
    private ImageButton hatmanCharacter;
    private Button backButton;
    private Button selectAdvantureGirlButton;
    private Button selectNinjaButton;
    private Button selectHatmanButton;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);


        advantureGirlCharacter = findViewById(R.id.advantureGirlCharacter);
        ninjaCharacter = findViewById(R.id.ninjaCharacter);
        hatmanCharacter = findViewById(R.id.hatmanCharacter);
        backButton = findViewById(R.id.backButton);
        selectAdvantureGirlButton = findViewById(R.id.selectAdvantureGirlButton);
        selectNinjaButton = findViewById(R.id.selectNinjaButton);
        selectHatmanButton = findViewById(R.id.selectHatmanButton);


        advantureGirlCharacter.setImageResource(R.drawable.advanture_girl_character);
        ninjaCharacter.setImageResource(R.drawable.ninja_character);
        hatmanCharacter.setImageResource(R.drawable.hatman_character);


        selectAdvantureGirlButton.setOnClickListener(v -> selectCharacter("Advanture_Girl"));
        selectNinjaButton.setOnClickListener(v -> selectCharacter("Ninja"));
        selectHatmanButton.setOnClickListener(v -> selectCharacter("Hatman"));
        backButton.setOnClickListener(v -> finish());


        String currentCharacter = preferences.getString(SELECTED_CHARACTER, "Advanture_Girl");
        updateSelectedCharacter(currentCharacter);
        updateSelectButtonBackgrounds(currentCharacter);
    }

    private void selectCharacter(String character) {
        // Save selection
        preferences.edit().putString(SELECTED_CHARACTER, character).apply();

        // Update UI
        updateSelectedCharacter(character);
        updateSelectButtonBackgrounds(character);

        // Show confirmation
        Toast.makeText(this, "Selected " + character + " character!", Toast.LENGTH_SHORT).show();
    }

    private void updateSelectButtonBackgrounds(String selectedCharacter) {
        selectAdvantureGirlButton.setBackgroundResource(selectedCharacter.equals("Advanture_Girl") ? R.drawable.button_background_red : R.drawable.button_background_blue);
        selectNinjaButton.setBackgroundResource(selectedCharacter.equals("Ninja") ? R.drawable.button_background_red : R.drawable.button_background_blue);
        selectHatmanButton.setBackgroundResource(selectedCharacter.equals("Hatman") ? R.drawable.button_background_red : R.drawable.button_background_blue);
    }

    private void updateSelectedCharacter(String character) {
        // Reset all borders
        advantureGirlCharacter.setBackgroundResource(R.drawable.character_unselected);
        ninjaCharacter.setBackgroundResource(R.drawable.character_unselected);
        hatmanCharacter.setBackgroundResource(R.drawable.character_unselected);


        switch (character) {
            case "Advanture_Girl":
                advantureGirlCharacter.setBackgroundResource(R.drawable.character_selected);
                break;
            case "Ninja":
                ninjaCharacter.setBackgroundResource(R.drawable.character_selected);
                break;
            case "Hatman":
                hatmanCharacter.setBackgroundResource(R.drawable.character_selected);
                break;
        }
        updateSelectButtonBackgrounds(character);
    }
}