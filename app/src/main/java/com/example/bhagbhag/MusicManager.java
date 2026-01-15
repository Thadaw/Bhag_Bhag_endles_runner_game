package com.example.bhagbhag;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;

public class MusicManager {
    private static MediaPlayer menuPlayer;
    private static boolean isMenuPrepared = false;

    private static MediaPlayer gamePlayer;
    private static boolean isGamePrepared = false;

    private static final String PREFS_NAME = "BhagBhagPrefs";
    private static final String MUSIC_VOLUME_KEY = "musicVolume";
    private static final int DEFAULT_VOLUME = 50;
    private static final String MUSIC_ENABLED_KEY = "musicEnabled";

    private static boolean isMusicEnabled(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(MUSIC_ENABLED_KEY, true);
    }

    private static float getUserVolume(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int musicVolumePercent = prefs.getInt(MUSIC_VOLUME_KEY, DEFAULT_VOLUME);
        return musicVolumePercent / 100f;
    }

    public static void playMenuMusic(Context context) {
        Context appContext = context.getApplicationContext();
        if (!isMusicEnabled(appContext)) {
            stopMenuMusic();
            return;
        }
        float volume = getUserVolume(appContext);
        if (menuPlayer == null) {
            menuPlayer = MediaPlayer.create(appContext, R.raw.menu_music);
            if (menuPlayer == null) {
                Log.e("MusicManager", "Failed to create MediaPlayer for menu music.");
                return;
            }
            menuPlayer.setLooping(true);
            menuPlayer.setVolume(volume, volume);
            isMenuPrepared = true;
            menuPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("MusicManager", "MenuPlayer error: " + what + ", " + extra);
                stopMenuMusic();
                return true;
            });
        } else {
            menuPlayer.setVolume(volume, volume);
        }
        if (!menuPlayer.isPlaying()) {
            try {
                if (!isMenuPrepared) {
                    menuPlayer.prepare();
                    isMenuPrepared = true;
                }
                menuPlayer.start();
            } catch (Exception e) {
                Log.e("MusicManager", "Error starting menu music: " + e.getMessage());
                stopMenuMusic();
            }
        }
    }

    public static void stopMenuMusic() {
        if (menuPlayer != null) {
            try {
                if (menuPlayer.isPlaying()) {
                    menuPlayer.pause();
                }
                menuPlayer.seekTo(0);
            } catch (Exception e) {
                Log.e("MusicManager", "Error pausing menu music: " + e.getMessage());
            }
        }
    }

    public static void releaseMenuMusic() {
        if (menuPlayer != null) {
            try {
                menuPlayer.release();
            } catch (Exception e) {
                Log.e("MusicManager", "Error releasing menu music: " + e.getMessage());
            }
            menuPlayer = null;
            isMenuPrepared = false;
        }
    }

    public static void playGameMusic(Context context) {
        Context appContext = context.getApplicationContext();
        if (!isMusicEnabled(appContext)) {
            stopGameMusic();
            return;
        }
        float volume = getUserVolume(appContext);
        if (gamePlayer == null) {
            gamePlayer = MediaPlayer.create(appContext, R.raw.game_music);
            if (gamePlayer == null) {
                Log.e("MusicManager", "Failed to create MediaPlayer for game music.");
                return;
            }
            gamePlayer.setLooping(true);
            gamePlayer.setVolume(volume, volume);
            isGamePrepared = true;
            gamePlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("MusicManager", "GamePlayer error: " + what + ", " + extra);
                stopGameMusic();
                return true;
            });
        } else {
            gamePlayer.setVolume(volume, volume);
        }
        if (!gamePlayer.isPlaying()) {
            try {
                if (!isGamePrepared) {
                    gamePlayer.prepare();
                    isGamePrepared = true;
                }
                gamePlayer.start();
            } catch (Exception e) {
                Log.e("MusicManager", "Error starting game music: " + e.getMessage());
                stopGameMusic();
            }
        }
    }

    public static void stopGameMusic() {
        if (gamePlayer != null) {
            try {
                if (gamePlayer.isPlaying()) {
                    gamePlayer.pause();
                }
                gamePlayer.seekTo(0);
            } catch (Exception e) {
                Log.e("MusicManager", "Error pausing game music: " + e.getMessage());
            }
        }
    }

    public static void releaseGameMusic() {
        if (gamePlayer != null) {
            try {
                gamePlayer.release();
            } catch (Exception e) {
                Log.e("MusicManager", "Error releasing game music: " + e.getMessage());
            }
            gamePlayer = null;
            isGamePrepared = false;
        }
    }
    public static void updateAllMusicVolumes(Context context) {
        float volume = getUserVolume(context.getApplicationContext());
        if (menuPlayer != null) menuPlayer.setVolume(volume, volume);
        if (gamePlayer != null) gamePlayer.setVolume(volume, volume);
    }
} 