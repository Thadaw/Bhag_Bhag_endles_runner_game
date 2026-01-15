package com.example.bhagbhag;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.AudioAttributes;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import com.example.bhagbhag.MusicManager;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "GameView";
    private static final String PREFS_NAME = "BhagBhagPrefs";
    private static final String SELECTED_CHARACTER = "selectedCharacter";
    private static final int NUM_OBSTACLES = 5;
    private static final int NUM_OBSTACLE_IMAGES = 4;
    private Bitmap[] obstacleImages = new Bitmap[NUM_OBSTACLE_IMAGES];
    private int[] obstacleImageIndices = new int[NUM_OBSTACLES];

    private Thread gameThread;
    private SurfaceHolder holder;
    private boolean isPlaying;
    private boolean isGameOver;
    private Context context;

    private GameOverListener gameOverListener;

    private Paint paint; // For general game elements
    private Paint backgroundPaint; // Specifically for backgrounds
    private Canvas canvas;
    // private Bitmap backgroundImage; // Will be replaced by transitioning backgrounds
    private Bitmap coinImage;
    private Bitmap obstacleImage;
    private Bitmap ceilingObstacleImage;

    private Bitmap[] ninjaRunFrames;
    private Bitmap[] ninjaJumpFrames;
    private Bitmap[] ninjaSlideFrames;
    private int ninjaRunFrameIndex = 0;
    private int ninjaJumpFrameIndex = 0;
    private int ninjaSlideFrameIndex = 0;
    private long lastNinjaFrameTime = 0;
    private final int NINJA_FRAME_DURATION = 60;
    private int ninjaCurrentAction = 0;


    private Drawable bgDrawable1;
    private Drawable bgDrawable2;
    private Bitmap background1;
    private Bitmap background2;
    private float masterBackgroundX = 0f;
    private final float BACKGROUND_SCROLL_SPEED = 3.0f;


    private int speedLevel = 1;
    private int baseMoveSpeed = 13;
    private int currentMoveSpeed;
    private float currentBackgroundScrollSpeed;
    private long lastSpeedIncreaseTime = 0;
    private final long SPEED_INCREASE_INTERVAL = 30000;


    private boolean gameStarted = false;
    private long gameStartTime = 0;
    private final long GAME_START_DELAY = 2000;
    private long gameStartedTime = 0;



    private int screenWidth;
    private int screenHeight;
    private int score = 0;
    private int coins;

    // Player properties
    private float playerX;
    private float playerY;
    private float playerSpeed;
    private float jumpForce;
    private float gravity;
    private boolean isJumping;
    private boolean isSliding;
    private long slideStartTime;
    private long jumpStartTime;
    private final long SLIDE_DURATION = 2000;
    private final long MAX_JUMP_DURATION = 3000;

    // Game objects
    private Rect playerRect;
    private Rect[] obstacles;
    private Rect[] coinsRect;
    private float groundY;

    private ScoreUpdateListener scoreUpdateListener;

    private Bitmap[] slideFrames;
    private int slideFrameIndex = 0;
    private long lastSlideFrameTime = 0;
    private final int SLIDE_FRAME_DURATION = 50;
    private final int slideFrameCount = 2;

    private boolean allSlideFramesLoaded = true;


    private String selectedCharacter = "Advanture_Girl";


    private final int GROUND_HEIGHT = 1;
    private final int BOTTOM_OFFSET = 100; // Reduced to move obstacles down a bit
    private final int CHARACTER_ALIGNMENT_OFFSET =-5; // Adjust this value (e.g., -5, 5) for perfect visual alignment


    boolean[] isCeilingObstacle = new boolean[NUM_OBSTACLES];


    int groundObstaclesSinceLastCeiling = 0;

    private boolean imagesLoadedSuccessfully = true;


    private boolean ceilingJustPlaced = false;


    private SoundPool soundPool;
    private int jumpSoundId;
    private boolean soundPoolLoaded = false;


    private Bitmap[] hatmanRunFrames;
    private Bitmap[] hatmanJumpFrames;
    private Bitmap[] hatmanSlideFrames;
    private int hatmanRunFrameIndex = 0;
    private int hatmanJumpFrameIndex = 0;
    private int hatmanSlideFrameIndex = 0;
    private long lastHatmanFrameTime = 0;
    private final int HATMAN_FRAME_DURATION = 60;
    private int hatmanCurrentAction = 0;


    private Bitmap[] advantureGirlRunFrames;
    private Bitmap[] advantureGirlJumpFrames;
    private Bitmap[] advantureGirlSlideFrames;
    private int advantureGirlRunFrameIndex = 0;
    private int advantureGirlJumpFrameIndex = 0;
    private int advantureGirlSlideFrameIndex = 0;
    private long lastAdvantureGirlFrameTime = 0;
    private final int ADVANTURE_GIRL_FRAME_DURATION = 60;
    private int advantureGirlCurrentAction = 0;


    private int obstaclesPassedSinceLastScore = 0;


    private float touchStartY = 0;
    private float touchStartX = 0;
    private boolean isTouchActive = false;
    private static final float MIN_SWIPE_DISTANCE = 80;
    private static final float MIN_SWIPE_VELOCITY = 200;
    private long touchStartTime = 0;

    public interface ScoreUpdateListener {
        void onScoreUpdate(int score);
        void onCoinUpdate(int coins);
    }

    public interface GameOverListener {
        void onGameOver(int finalScore, int finalCoins);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            this.context = context;
            init(context);
        } catch (Exception e) {
            Log.e("GameView", "Error in GameView constructor: " + e.getMessage(), e);
            imagesLoadedSuccessfully = false;
        }
    }

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
        Log.d(TAG, "GameOverListener set: " + (listener != null ? "not null" : "null"));
    }

    private void init(Context context) {
        Log.d(TAG, "Initializing GameView");
        holder = getHolder();
        holder.addCallback(this);

        paint = new Paint();
        paint.setColor(Color.WHITE);

        backgroundPaint = new Paint();
        backgroundPaint.setDither(true);

        // Initialize game objects
        playerRect = new Rect();
        obstacles = new Rect[NUM_OBSTACLES];
        for (int i = 0; i < NUM_OBSTACLES; i++) {
            obstacles[i] = new Rect();
        }
        coinsRect = new Rect[5];
        for (int i = 0; i < 5; i++) {
            coinsRect[i] = new Rect();
        }

        loadGameImages();


        int tallestObstacle = 0;
        for (int i = 0; i < obstacleImages.length; i++) {
            if (obstacleImages[i] != null && obstacleImages[i].getHeight() > tallestObstacle) {
                tallestObstacle = obstacleImages[i].getHeight();
            }
        }
        tallestObstacle = (int)(tallestObstacle * 0.8f);
        int margin = 40;


        float gravityValue = 0.7f;
        float jumpHeight = tallestObstacle + margin;
        float jumpForceValue = -(float)Math.sqrt(2 * gravityValue * jumpHeight);


        score = 0;
        coins = 0;
        playerSpeed = 0;
        jumpForce = jumpForceValue;
        gravity = gravityValue;
        Log.d(TAG, "Jump physics initialized - jumpForce: " + jumpForce + ", gravity: " + gravity + ", tallestObstacle: " + tallestObstacle);
        isSliding = false;
        slideStartTime = 0;


        speedLevel = 1;
        currentMoveSpeed = baseMoveSpeed;
        currentBackgroundScrollSpeed = BACKGROUND_SCROLL_SPEED;
        lastSpeedIncreaseTime = System.currentTimeMillis();

        for (int i = 0; i < obstacles.length; i++) {
            isCeilingObstacle[i] = false;
            obstacleImageIndices[i] = (int)(Math.random() * NUM_OBSTACLE_IMAGES);
        }

        for (int i = 0; i < coinsRect.length; i++) {
            coinsRect[i] = new Rect();
        }



        int targetWidth = (ninjaRunFrames != null && ninjaRunFrames[0] != null) ? (int)(ninjaRunFrames[0].getWidth() * 0.75f) : 96;
        int targetHeight = (ninjaRunFrames != null && ninjaRunFrames[0] != null) ? (int)(ninjaRunFrames[0].getHeight() * 0.75f) : 96;

        slideFrames = new Bitmap[slideFrameCount];
        String[] slideFrameNames = {"slide2", "slide3"};
        for (int i = 0; i < slideFrameCount; i++) {
            int resId = getResources().getIdentifier(slideFrameNames[i], "drawable", context.getPackageName());
            Bitmap original = BitmapFactory.decodeResource(getResources(), resId);
            if (original == null) {
                Log.e(TAG, "Failed to load slide frame: " + slideFrameNames[i]);
                allSlideFramesLoaded = false;
            } else {

                slideFrames[i] = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true);
                Log.d(TAG, "Loaded and scaled slide frame: " + slideFrameNames[i] + " to " + targetWidth + "x" + targetHeight);
            }
        }


        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0 && sampleId == jumpSoundId) {
                soundPoolLoaded = true;
            }
        });
        jumpSoundId = soundPool.load(context, R.raw.jump, 1);

        Log.d(TAG, "GameView initialization complete");
    }

private void loadGameImages() {
    imagesLoadedSuccessfully = true;
    try {
        float objectScaleFactor = 0.8f;


        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        selectedCharacter = prefs.getString(SELECTED_CHARACTER, "Advanture_Girl");
        Log.d(TAG, "Loading character gif: " + selectedCharacter);

        if ("Ninja".equals(selectedCharacter)) {
            ninjaRunFrames = new Bitmap[10];
            ninjaJumpFrames = new Bitmap[10];
            ninjaSlideFrames = new Bitmap[10];
            for (int i = 0; i < 10; i++) {
                int runResId = getResources().getIdentifier("ninja_run" + (i+1), "drawable", context.getPackageName());
                int jumpResId = getResources().getIdentifier("ninja_jump" + (i+1), "drawable", context.getPackageName());
                int slideResId = getResources().getIdentifier("ninja_slide" + (i+1), "drawable", context.getPackageName());
                ninjaRunFrames[i] = BitmapFactory.decodeResource(getResources(), runResId);
                ninjaJumpFrames[i] = BitmapFactory.decodeResource(getResources(), jumpResId);
                ninjaSlideFrames[i] = BitmapFactory.decodeResource(getResources(), slideResId);
                if (ninjaRunFrames[i] == null) Log.e(TAG, "Failed to load ninja_run" + (i+1));
                if (ninjaJumpFrames[i] == null) Log.e(TAG, "Failed to load ninja_jump" + (i+1));
                if (ninjaSlideFrames[i] == null) Log.e(TAG, "Failed to load ninja_slide" + (i+1));
            }
        }
        if ("Hatman".equals(selectedCharacter)) {
            hatmanRunFrames = new Bitmap[10];
            hatmanJumpFrames = new Bitmap[10];
            hatmanSlideFrames = new Bitmap[10];
            for (int i = 0; i < 10; i++) {
                int runResId = getResources().getIdentifier("hatman_run" + (i+1), "drawable", context.getPackageName());
                int jumpResId = getResources().getIdentifier("hatman_jump" + (i+1), "drawable", context.getPackageName());
                int slideResId = getResources().getIdentifier("hatman_slide" + (i+1), "drawable", context.getPackageName());
                hatmanRunFrames[i] = BitmapFactory.decodeResource(getResources(), runResId);
                hatmanJumpFrames[i] = BitmapFactory.decodeResource(getResources(), jumpResId);
                hatmanSlideFrames[i] = BitmapFactory.decodeResource(getResources(), slideResId);
                if (hatmanRunFrames[i] == null) Log.e(TAG, "Failed to load hatman_run" + (i+1));
                if (hatmanJumpFrames[i] == null) Log.e(TAG, "Failed to load hatman_jump" + (i+1));
                if (hatmanSlideFrames[i] == null) Log.e(TAG, "Failed to load hatman_slide" + (i+1));
            }
        }
        if ("Advanture_Girl".equals(selectedCharacter)) {
            advantureGirlRunFrames = new Bitmap[8];
            advantureGirlJumpFrames = new Bitmap[10];
            advantureGirlSlideFrames = new Bitmap[5];
            for (int i = 0; i < 8; i++) {
                int runResId = getResources().getIdentifier("advanturegirl_run" + (i+1), "drawable", context.getPackageName());
                advantureGirlRunFrames[i] = BitmapFactory.decodeResource(getResources(), runResId);
                if (advantureGirlRunFrames[i] == null) Log.e(TAG, "Failed to load advanturegirl_run" + (i+1));
            }
            for (int i = 0; i < 10; i++) {
                int jumpResId = getResources().getIdentifier("advanturegirl_jump" + (i+1), "drawable", context.getPackageName());
                advantureGirlJumpFrames[i] = BitmapFactory.decodeResource(getResources(), jumpResId);
                if (advantureGirlJumpFrames[i] == null) Log.e(TAG, "Failed to load advanturegirl_jump" + (i+1));
            }
            for (int i = 0; i < 5; i++) {
                int slideResId = getResources().getIdentifier("advanturegirl_slide" + (i+1), "drawable", context.getPackageName());
                advantureGirlSlideFrames[i] = BitmapFactory.decodeResource(getResources(), slideResId);
                if (advantureGirlSlideFrames[i] == null) Log.e(TAG, "Failed to load advanturegirl_slide" + (i+1));
            }
        }


        coinImage = BitmapFactory.decodeResource(getResources(), R.drawable.coin);
        if (coinImage == null) {
            Log.e(TAG, "Failed to load coin.png");
            imagesLoadedSuccessfully = false;
        } else {
            coinImage = Bitmap.createScaledBitmap(coinImage,
                    (int)(coinImage.getWidth() * objectScaleFactor),
                    (int)(coinImage.getHeight() * objectScaleFactor), true);
        }


        for (int i = 0; i < NUM_OBSTACLE_IMAGES; i++) {
            int resId = getResources().getIdentifier("obstacle" + (i+1), "drawable", context.getPackageName());
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId);
            if (bmp == null) {
                Log.e(TAG, "Failed to load obstacle" + (i+1) + ".png");
            } else {
                obstacleImages[i] = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth() * objectScaleFactor), (int)(bmp.getHeight() * objectScaleFactor), true);
            }
        }
        if (obstacleImages[0] == null) {
            Log.e(TAG, "No valid ground obstacle images loaded!");
            imagesLoadedSuccessfully = false;
        }

        // Load ceiling obstacle image
        ceilingObstacleImage = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("ceiling_obstacle", "drawable", context.getPackageName()));
        if (ceilingObstacleImage != null) {
            ceilingObstacleImage = Bitmap.createScaledBitmap(ceilingObstacleImage, (int)(ceilingObstacleImage.getWidth() * objectScaleFactor), (int)(ceilingObstacleImage.getHeight() * objectScaleFactor), true);
        } else {
            Log.e(TAG, "Failed to load ceiling_obstacle.png");
            imagesLoadedSuccessfully = false;
        }


        bgDrawable1 = ContextCompat.getDrawable(context, R.drawable.background1);
        bgDrawable2 = ContextCompat.getDrawable(context, R.drawable.background2);

        if (bgDrawable1 == null || bgDrawable2 == null) {
            Log.e(TAG, "Failed to load one or both background drawables.");
            imagesLoadedSuccessfully = false;
        }

        Log.d(TAG, "Successfully loaded all game images");
    } catch (Exception e) {
        Log.e(TAG, "Error loading game images: " + e.getMessage());
        imagesLoadedSuccessfully = false;
    }
}

        @Override
public void surfaceCreated(SurfaceHolder holder) {
    Log.d(TAG, "Surface created");
    screenWidth = getWidth();
    screenHeight = getHeight();


    background1 = drawableToBitmap(bgDrawable1, screenWidth, screenHeight);
    background2 = drawableToBitmap(bgDrawable2, screenWidth, screenHeight);

    if (background1 == null || background2 == null) {
        Log.e(TAG, "Failed to convert one or both background drawables to bitmaps.");

    }


    groundY = screenHeight - GROUND_HEIGHT;


    if ("Ninja".equals(selectedCharacter) && ninjaRunFrames != null && ninjaRunFrames[0] != null) {
        int scaledHeight = (int)(ninjaRunFrames[0].getHeight() * 0.3f);
        // Running position: aligns character's feet with ground obstacle, plus fine-tune offset
        playerY = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
        playerX = screenWidth / 6;

        score = 0;
        Log.d(TAG, "Calling resetGame from surfaceCreated");
        resetGame();
        Log.d(TAG, "Calling resume from surfaceCreated");
        resume();
        Log.d(TAG, "Game started successfully");
    } else if ("Hatman".equals(selectedCharacter) && hatmanRunFrames != null && hatmanRunFrames[0] != null) {
        int scaledHeight = (int)(hatmanRunFrames[0].getHeight() * 0.3f);
        playerY = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
        playerX = screenWidth / 6;
        score = 0;
        Log.d(TAG, "Calling resetGame from surfaceCreated");
        resetGame();
        Log.d(TAG, "Calling resume from surfaceCreated");
        resume();
        Log.d(TAG, "Game started successfully");
    } else if ("Advanture_Girl".equals(selectedCharacter) && advantureGirlRunFrames != null && advantureGirlRunFrames[0] != null) {
        int scaledHeight = (int)(advantureGirlRunFrames[0].getHeight() * 0.3f);
        playerY = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
        playerX = screenWidth / 6;
        score = 0;
        Log.d(TAG, "Calling resetGame from surfaceCreated");
        resetGame();
        Log.d(TAG, "Calling resume from surfaceCreated");
        resume();
        Log.d(TAG, "Game started successfully");
    } else {
        Log.e(TAG, "Cannot start game - player gif is null");
    }
}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Recreate background bitmaps to match new surface size
        background1 = drawableToBitmap(bgDrawable1, width, height);
        background2 = drawableToBitmap(bgDrawable2, width, height);
        screenWidth = width;
        screenHeight = height;
        groundY = screenHeight - GROUND_HEIGHT;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
        // Release SoundPool resources
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

    }

    public void resume() {
        isPlaying = true;
        MusicManager.playGameMusic(context);
        gameThread = new Thread(this);
        gameThread.start();
    }

public void pause() {
    isPlaying = false;
    MusicManager.stopGameMusic();
    try {
        gameThread.join();
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}

    private void resetGame() {
        Log.d(TAG, "Resetting game state");
        isGameOver = false;
        score = 0;

        gameStarted = false;
        gameStartTime = System.currentTimeMillis();

        coins = 0;

        if (scoreUpdateListener != null) {
            scoreUpdateListener.onScoreUpdate(score);
            scoreUpdateListener.onCoinUpdate(coins);
        }

        playerSpeed = 0;
        isJumping = false;
        isSliding = false;
        slideStartTime = 0;


        speedLevel = 1;
        currentMoveSpeed = baseMoveSpeed;
        currentBackgroundScrollSpeed = BACKGROUND_SCROLL_SPEED;
        lastSpeedIncreaseTime = System.currentTimeMillis();
        masterBackgroundX = 0f;


        int tallestObstacle = 0;
        for (int i = 0; i < obstacleImages.length; i++) {
            if (obstacleImages[i] != null && obstacleImages[i].getHeight() > tallestObstacle) {
                tallestObstacle = obstacleImages[i].getHeight();
            }
        }
        tallestObstacle = (int)(tallestObstacle * 0.8f);
        int margin = 200;
        float gravityValue = 0.7f;
        float jumpHeight = tallestObstacle + margin;
        float jumpForceValue = -(float)Math.sqrt(2 * gravityValue * jumpHeight);
        jumpForce = jumpForceValue;
        gravity = gravityValue;
        Log.d(TAG, "[resetGame] Jump physics updated - jumpForce: " + jumpForce + ", gravity: " + gravity + ", tallestObstacle: " + tallestObstacle);


        if ("Ninja".equals(selectedCharacter) && ninjaRunFrames != null && ninjaRunFrames[0] != null) {
            playerX = screenWidth * 0.1f;
            int scaledHeight = (int)(ninjaRunFrames[0].getHeight() * 0.3f);
            playerY = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
        } else if ("Hatman".equals(selectedCharacter) && hatmanRunFrames != null && hatmanRunFrames[0] != null) {
            playerX = screenWidth * 0.1f;
            int scaledHeight = (int)(hatmanRunFrames[0].getHeight() * 0.3f);
            playerY = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
        } else if ("Advanture_Girl".equals(selectedCharacter) && advantureGirlRunFrames != null && advantureGirlRunFrames[0] != null) {
            playerX = screenWidth * 0.1f;
            int scaledHeight = (int)(advantureGirlRunFrames[0].getHeight() * 0.3f);
            playerY = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
        } else {
            playerX = screenWidth * 0.1f;
            playerY = groundY - BOTTOM_OFFSET - 29 + CHARACTER_ALIGNMENT_OFFSET; // fallback for unknown character
        }


        int obstacleSpacing = screenWidth / 2;
        for (int i = 0; i < obstacles.length; i++) {
            obstacles[i].set(
                    screenWidth + (i * obstacleSpacing),
                    (int)groundY - obstacleImages[obstacleImageIndices[i]].getHeight() - BOTTOM_OFFSET,
                    screenWidth + (i * obstacleSpacing) + obstacleImages[obstacleImageIndices[i]].getWidth(),
                    (int)groundY - BOTTOM_OFFSET
            );
            isCeilingObstacle[i] = false;
            obstacleImageIndices[i] = (int)(Math.random() * NUM_OBSTACLE_IMAGES);
        }
        groundObstaclesSinceLastCeiling = 0;
        obstaclesPassedSinceLastScore = 0;

        isTouchActive = false;
        touchStartX = 0;
        touchStartY = 0;
        touchStartTime = 0;

        int coinSpacing = screenWidth / 3;
        int obstacleTop = (int)groundY - obstacleImages[obstacleImageIndices[0]].getHeight() - BOTTOM_OFFSET; // Use first obstacle image for coin position
        int coinMargin = 40;
        for (int i = 0; i < coinsRect.length; i++) {
            int coinX = screenWidth + (i * coinSpacing);
            int minCoinX = (int)(playerX + (ninjaRunFrames != null && ninjaRunFrames[0] != null ? ninjaRunFrames[0].getWidth() : 96) + 50); // 50px buffer ahead of player
            if (coinX < minCoinX) {
                coinX = minCoinX + (i * coinSpacing);
            }
            int coinY = obstacleTop - coinImage.getHeight() - coinMargin;
            if (coinY < 0) coinY = 0;
            coinsRect[i].set(
                    coinX,
                    coinY,
                    coinX + coinImage.getWidth(),
                    coinY + coinImage.getHeight()
            );
        }


        Log.d(TAG, "Game reset complete");
    }

    private void increaseSpeedAndChangeBackground() {
        speedLevel++;
        playLevelUpSound();
        currentMoveSpeed = baseMoveSpeed + (speedLevel - 1);
        currentBackgroundScrollSpeed = BACKGROUND_SCROLL_SPEED + (speedLevel - 1);

        Log.d(TAG, "Speed increased to level " + speedLevel +
              " (Move speed: " + currentMoveSpeed +
              ", Background speed: " + currentBackgroundScrollSpeed + ")");
    }

    public void restartGame() {
        Log.d(TAG, "Restarting game with new physics - jumpForce: " + jumpForce + ", gravity: " + gravity);
        resetGame();
        MusicManager.stopGameMusic();
        resume();

        int scaledHeight = getCurrentCharacterScaledHeight();
        float groundLevel = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
        playerY = groundLevel;
        Log.d(TAG, "Character positioned at ground level after restart: " + groundLevel);
    }

@Override
public boolean onTouchEvent(MotionEvent event) {
    if (isGameOver) {
        return true;
    }

    switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            touchStartX = event.getX();
            touchStartY = event.getY();
            touchStartTime = System.currentTimeMillis();
            isTouchActive = true;
            Log.d(TAG, "Touch started at: (" + touchStartX + ", " + touchStartY + ")");
            break;

        case MotionEvent.ACTION_MOVE:
            if (!isTouchActive) break;

            float currentX = event.getX();
            float currentY = event.getY();
            float deltaX = currentX - touchStartX;
            float deltaY = currentY - touchStartY;

            float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (deltaY > MIN_SWIPE_DISTANCE && Math.abs(deltaY) > Math.abs(deltaX)) {
                long swipeTime = System.currentTimeMillis() - touchStartTime;
                float velocity = deltaY / (swipeTime / 1000f); // pixels per second

                if (!isJumping && !isSliding) {
                    isSliding = true;
                    slideStartTime = System.currentTimeMillis();
                    slideFrameIndex = 0;
                    lastSlideFrameTime = System.currentTimeMillis();
                    Log.d(TAG, "Slide initiated via downward swipe - deltaY: " + deltaY + ", velocity: " + velocity + " px/s");
                    playSlideSound();
                    performHapticFeedback();
                }
                isTouchActive = false;
            }
            break;

        case MotionEvent.ACTION_UP:
            if (isTouchActive) {
                if (!isJumping && !isSliding) {
                    isJumping = true;
                    jumpStartTime = System.currentTimeMillis();
                    playerSpeed = jumpForce;
                    Log.d(TAG, "Jump initiated with force: " + jumpForce + ", gravity: " + gravity + ", initial playerY: " + playerY);
                    playJumpSound();
                }
            }
            isTouchActive = false;
            break;

        case MotionEvent.ACTION_CANCEL:
            isTouchActive = false;
            break;
    }

    return true;
}

    public void performJump() {
        if (!isJumping && !isSliding) {
            isJumping = true;
            jumpStartTime = System.currentTimeMillis();
            playerSpeed = jumpForce;
            Log.d(TAG, "Jump initiated via button with force: " + jumpForce + ", gravity: " + gravity + ", initial playerY: " + playerY);
            playJumpSound();
        }
    }

    public void performSlide() {
        if (!isJumping && !isSliding) {
            isSliding = true;
            slideStartTime = System.currentTimeMillis();
            slideFrameIndex = 0;
            lastSlideFrameTime = System.currentTimeMillis();
            Log.d(TAG, "Slide started");
            playSlideSound();
            performHapticFeedback();
        }
    }

    @Override
    public void run() {
        while (isPlaying) {
            if (imagesLoadedSuccessfully) {
                update();
                draw();
            } else {
                draw();
                control();
            }
        }
    }

    private void update() {
        if (!isGameOver) {
            long currentTime = System.currentTimeMillis();
            if (!gameStarted && (currentTime - gameStartTime) >= GAME_START_DELAY) {
                gameStarted = true;
                gameStartedTime = currentTime; // Record the time when the game actually starts
                Log.d(TAG, "Game started after delay");
            }

            if (currentTime - lastSpeedIncreaseTime > SPEED_INCREASE_INTERVAL) {
                increaseSpeedAndChangeBackground();
                lastSpeedIncreaseTime = currentTime;
            }

            masterBackgroundX -= currentBackgroundScrollSpeed;
            if (masterBackgroundX <= -screenWidth) {
                masterBackgroundX += screenWidth; // Loop the background
            }

            if (isJumping) {
                playerSpeed += gravity;
                playerY += playerSpeed;

                Log.d(TAG, "Jumping - playerY: " + playerY + ", playerSpeed: " + playerSpeed + ", groundY: " + groundY + ", jumpForce: " + jumpForce + ", gravity: " + gravity);

                int scaledHeight = getCurrentCharacterScaledHeight();
                float groundLevel = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
                if (playerY >= groundLevel) {
                    playerY = groundLevel;
                    isJumping = false;
                    playerSpeed = 0;
                    Log.d(TAG, "Jump ended - landed on ground");
                }

                if (System.currentTimeMillis() - jumpStartTime > MAX_JUMP_DURATION) {
                    playerY = groundLevel;
                    isJumping = false;
                    playerSpeed = 0;
                    Log.d(TAG, "Jump ended - maximum duration reached");
                }
            } else {
                int scaledHeight = getCurrentCharacterScaledHeight();
                float groundLevel = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
                if (playerY != groundLevel) {
                    playerY = groundLevel;
                    playerSpeed = 0;
                    Log.d(TAG, "Character repositioned to ground level: " + groundLevel);
                }
            }

            if (isSliding) {
                if (currentTime - slideStartTime > SLIDE_DURATION) {
                    isSliding = false;
                    Log.d(TAG, "Slide ended");
                }
            }

            if ("Ninja".equals(selectedCharacter)) {
                Bitmap frame = isJumping ? ninjaJumpFrames[ninjaJumpFrameIndex] : isSliding ? ninjaSlideFrames[ninjaSlideFrameIndex] : ninjaRunFrames[ninjaRunFrameIndex];
                if (frame != null) {
                    int targetWidth = (int) (frame.getWidth() * 0.3f);
                    int targetHeight = (int) (frame.getHeight() * 0.3f);
                    int bottomY = (int)playerY + targetHeight;
                    int topY = (int)playerY;
                    if (isSliding) {
                        int slideOffset = (int)(targetHeight * 0.3f);
                        topY += slideOffset;
                        bottomY += slideOffset;
                    }
                    playerRect.set((int)playerX, topY, (int)playerX + targetWidth, bottomY);
                }
            } else if ("Hatman".equals(selectedCharacter)) {
                Bitmap frame = isJumping ? hatmanJumpFrames[hatmanJumpFrameIndex] : isSliding ? hatmanSlideFrames[hatmanSlideFrameIndex] : hatmanRunFrames[hatmanRunFrameIndex];
                if (frame != null) {
                    int targetWidth = (int) (frame.getWidth() * 0.3f);
                    int targetHeight = (int) (frame.getHeight() * 0.3f);
                    int bottomY = (int)playerY + targetHeight;
                    int topY = (int)playerY;
                    // For sliding, position the collision a little bit lower
                    if (isSliding) {
                        int slideOffset = (int)(targetHeight * 0.3f);
                        topY += slideOffset;
                        bottomY += slideOffset;
                    }
                    playerRect.set((int)playerX, topY, (int)playerX + targetWidth, bottomY);
                }
            } else if ("Advanture_Girl".equals(selectedCharacter)) {
                Bitmap frame = isJumping ? advantureGirlJumpFrames[advantureGirlJumpFrameIndex] : isSliding ? advantureGirlSlideFrames[advantureGirlSlideFrameIndex] : advantureGirlRunFrames[advantureGirlRunFrameIndex];
                if (frame != null) {
                    int targetWidth = (int) (frame.getWidth() * 0.3f);
                    int targetHeight = (int) (frame.getHeight() * 0.3f);
                    int bottomY;
                    int topY;
                    if (isSliding) {
                        bottomY = (int)groundY - BOTTOM_OFFSET + 15; // Added 15 pixels to move sliding character down a bit more
                        topY = bottomY - targetHeight;
                    } else {
                        bottomY = (int)playerY + targetHeight;
                        topY = (int)playerY;
                    }
                    playerRect.set((int)playerX, topY, (int)playerX + targetWidth, bottomY);
                }
            }

            if (gameStarted) {
                int lastObstacleX = Integer.MIN_VALUE;
                for (int i = 0; i < obstacles.length; i++) {
                    if (isCeilingObstacle[i]) {
                        if (ceilingObstacleImage == null) {
                            Log.e(TAG, "ceilingObstacleImage is null! Skipping obstacle " + i);
                            continue;
                        }
                    } else {
                        if (obstacleImages[obstacleImageIndices[i]] == null) {
                            Log.e(TAG, "obstacleImages[" + obstacleImageIndices[i] + "] is null! Skipping obstacle " + i);
                            imagesLoadedSuccessfully = false;
                            continue;
                        }
                    }
                    obstacles[i].left -= currentMoveSpeed;  // Use dynamic speed
                    obstacles[i].right = obstacles[i].left + (isCeilingObstacle[i] ? ceilingObstacleImage.getWidth() : obstacleImages[obstacleImageIndices[i]].getWidth());

                    if (obstacles[i].right < 0) {
                        for (Rect obs : obstacles) {
                            if (obs.left > lastObstacleX) {
                                lastObstacleX = obs.left;
                            }
                        }
                        boolean ceiling = false;
                        if (groundObstaclesSinceLastCeiling == 3) {
                            if (Math.random() < 0.5) ceiling = true;
                        } else if (groundObstaclesSinceLastCeiling >= 4) {
                            ceiling = true;
                        }
                        if (ceiling) {
                            isCeilingObstacle[i] = true;
                            groundObstaclesSinceLastCeiling = 0;
                            int newLeft = Math.max(lastObstacleX + (screenWidth / 2), screenWidth) + (int)(Math.random() * 100);
                            obstacles[i].left = newLeft;
                            obstacles[i].right = obstacles[i].left + ceilingObstacleImage.getWidth();
                            int bottom = (int)(groundY - (screenHeight * 0.3));
                            int top = bottom - ceilingObstacleImage.getHeight();
                            obstacles[i].top = top;
                            obstacles[i].bottom = bottom;
                            ceilingJustPlaced = true;
                            if ((currentTime - gameStartedTime) > 500) {
                                obstaclesPassedSinceLastScore++;
                                if (obstaclesPassedSinceLastScore >= 3) {
                                    score++;
                                    obstaclesPassedSinceLastScore = 0;
                                    if (scoreUpdateListener != null) {
                                        scoreUpdateListener.onScoreUpdate(score);
                                    }
                                }
                            }
                        } else {
                            isCeilingObstacle[i] = false;
                            groundObstaclesSinceLastCeiling++;
                            int idx = (int)(Math.random() * NUM_OBSTACLE_IMAGES);
                            obstacleImageIndices[i] = idx;
                            int extraSpacing = 0;
                            if (ceilingJustPlaced) {
                                extraSpacing = screenWidth / 4;
                                ceilingJustPlaced = false;
                            }
                            obstacles[i].left = Math.max(lastObstacleX + (screenWidth / 2) + extraSpacing, screenWidth);
                            obstacles[i].right = obstacles[i].left + obstacleImages[idx].getWidth();
                            obstacles[i].top = (int)groundY - obstacleImages[idx].getHeight() - BOTTOM_OFFSET;
                            obstacles[i].bottom = (int)groundY - BOTTOM_OFFSET;
                            if ((currentTime - gameStartedTime) > 500) {
                                obstaclesPassedSinceLastScore++;
                                if (obstaclesPassedSinceLastScore >= 3) {
                                    score++;
                                    obstaclesPassedSinceLastScore = 0;
                                    if (scoreUpdateListener != null) {
                                        scoreUpdateListener.onScoreUpdate(score);
                                    }
                                }
                            }
                        }
                    }

                    if (isCeilingObstacle[i]) {
                        if (Rect.intersects(playerRect, obstacles[i]) && !isSliding) {
                            Bitmap playerBmp = null;
                            if ("Ninja".equals(selectedCharacter)) playerBmp = isJumping ? ninjaJumpFrames[ninjaJumpFrameIndex] : isSliding ? ninjaSlideFrames[ninjaSlideFrameIndex] : ninjaRunFrames[ninjaRunFrameIndex];
                            if ("Hatman".equals(selectedCharacter)) playerBmp = isJumping ? hatmanJumpFrames[hatmanJumpFrameIndex] : isSliding ? hatmanSlideFrames[hatmanSlideFrameIndex] : hatmanRunFrames[hatmanRunFrameIndex];
                            if ("Advanture_Girl".equals(selectedCharacter)) playerBmp = isJumping ? advantureGirlJumpFrames[advantureGirlJumpFrameIndex] : isSliding ? advantureGirlSlideFrames[advantureGirlSlideFrameIndex] : advantureGirlRunFrames[advantureGirlRunFrameIndex];
                            Bitmap obstacleBmp = ceilingObstacleImage;
                            if (playerBmp != null && obstacleBmp != null && pixelPerfectCollision(playerBmp, playerRect, obstacleBmp, obstacles[i])) {
                                isGameOver = true;
                                Log.d(TAG, "Game Over! Final score: " + score + ", Coins: " + coins);
                                playGameOverSound();
                                // Stop in-game music on game over
                                MusicManager.stopGameMusic();
                                if (gameOverListener != null) {
                                    Log.d(TAG, "Calling onGameOver with score: " + score + ", coins: " + coins);
                                    gameOverListener.onGameOver(score, coins);
                                    Log.d(TAG, "onGameOver call completed");
                                } else {
                                    Log.e(TAG, "GameOverListener is null! Cannot call onGameOver");
                                }
                            }
                        }
                    } else {
                        if (Rect.intersects(playerRect, obstacles[i])) {
                            Bitmap playerBmp = null;
                            if ("Ninja".equals(selectedCharacter)) playerBmp = isJumping ? ninjaJumpFrames[ninjaJumpFrameIndex] : isSliding ? ninjaSlideFrames[ninjaSlideFrameIndex] : ninjaRunFrames[ninjaRunFrameIndex];
                            if ("Hatman".equals(selectedCharacter)) playerBmp = isJumping ? hatmanJumpFrames[hatmanJumpFrameIndex] : isSliding ? hatmanSlideFrames[hatmanSlideFrameIndex] : hatmanRunFrames[hatmanRunFrameIndex];
                            if ("Advanture_Girl".equals(selectedCharacter)) playerBmp = isJumping ? advantureGirlJumpFrames[advantureGirlJumpFrameIndex] : isSliding ? advantureGirlSlideFrames[advantureGirlSlideFrameIndex] : advantureGirlRunFrames[advantureGirlRunFrameIndex];
                            Bitmap obstacleBmp = obstacleImages[obstacleImageIndices[i]];
                            if (playerBmp != null && obstacleBmp != null && pixelPerfectCollision(playerBmp, playerRect, obstacleBmp, obstacles[i])) {
                                isGameOver = true;
                                Log.d(TAG, "Game Over! Final score: " + score + ", Coins: " + coins);
                                playGameOverSound();
                                // Stop in-game music on game over
                                MusicManager.stopGameMusic();
                                if (gameOverListener != null) {
                                    Log.d(TAG, "Calling onGameOver with score: " + score + ", coins: " + coins);
                                    gameOverListener.onGameOver(score, coins);
                                    Log.d(TAG, "onGameOver call completed");
                                } else {
                                    Log.e(TAG, "GameOverListener is null! Cannot call onGameOver");
                                }
                            }
                        }
                    }
                }
            }

            // Update coins with dynamic speed (only after game has started)
            if (gameStarted) {
                int lastCoinX = Integer.MIN_VALUE;
                for (int i = 0; i < coinsRect.length; i++) {
                    coinsRect[i].left -= currentMoveSpeed;
                    coinsRect[i].right = coinsRect[i].left + coinImage.getWidth();

                    if (coinsRect[i].right < 0) {
                        // Find the rightmost coin
                        for (Rect coin : coinsRect) {
                            if (coin.left > lastCoinX) {
                                lastCoinX = coin.left;
                            }
                        }

                        int newX = Math.max(lastCoinX + (screenWidth / 3), screenWidth);
                        int obstacleTopUpdate = (int)groundY - obstacleImages[obstacleImageIndices[0]].getHeight() - BOTTOM_OFFSET; // Use first obstacle image for coin position
                        int coinMarginUpdate = 30;
                        int newY = obstacleTopUpdate - coinImage.getHeight() - coinMarginUpdate;
                        if (newY < 0) newY = 0;
                        Rect newCoinRect = new Rect(
                                newX,
                                newY,
                                newX + coinImage.getWidth(),
                                newY + coinImage.getHeight()
                        );

                        // Adjust coin position to avoid overlap with obstacles and keep minimum vertical distance
                        newCoinRect = adjustCoinPosition(newCoinRect, obstacleTopUpdate);

                        coinsRect[i].set(newCoinRect);
                    }

                    // Add grace period: ignore coin collision for 500ms after game start
                    if ((currentTime - gameStartedTime) > 500) {
                        // Use precise collision: bounding box + pixel-perfect
                        if (Rect.intersects(playerRect, coinsRect[i])) {
                            Bitmap playerBmp = null;
                            if ("Ninja".equals(selectedCharacter)) playerBmp = isJumping ? ninjaJumpFrames[ninjaJumpFrameIndex] : isSliding ? ninjaSlideFrames[ninjaSlideFrameIndex] : ninjaRunFrames[ninjaRunFrameIndex];
                            if ("Hatman".equals(selectedCharacter)) playerBmp = isJumping ? hatmanJumpFrames[hatmanJumpFrameIndex] : isSliding ? hatmanSlideFrames[hatmanSlideFrameIndex] : hatmanRunFrames[hatmanRunFrameIndex];
                            if ("Advanture_Girl".equals(selectedCharacter)) playerBmp = isJumping ? advantureGirlJumpFrames[advantureGirlJumpFrameIndex] : isSliding ? advantureGirlSlideFrames[advantureGirlSlideFrameIndex] : advantureGirlRunFrames[advantureGirlRunFrameIndex];
                            Bitmap coinBmp = coinImage;
                            if (playerBmp != null && coinBmp != null && pixelPerfectCollision(playerBmp, playerRect, coinBmp, coinsRect[i])) {
                                coins++;
                                // Move coin off screen to be reset
                                coinsRect[i].left = screenWidth + 100;
                                coinsRect[i].right = coinsRect[i].left + coinImage.getWidth();
                                if (scoreUpdateListener != null) {
                                    scoreUpdateListener.onCoinUpdate(coins);
                                }
                                playCoinSound();
                            }
                        }
                    }
                }
            }

            // Update player animation frame for Ninja
            if ("Ninja".equals(selectedCharacter)) {
                long now = System.currentTimeMillis();
                if (isJumping) {
                    ninjaCurrentAction = 1;
                    if (now - lastNinjaFrameTime > NINJA_FRAME_DURATION) {
                        ninjaJumpFrameIndex = (ninjaJumpFrameIndex + 1) % 10;
                        lastNinjaFrameTime = now;
                    }
                } else if (isSliding) {
                    ninjaCurrentAction = 2;
                    if (now - lastNinjaFrameTime > NINJA_FRAME_DURATION) {
                        ninjaSlideFrameIndex = (ninjaSlideFrameIndex + 1) % 10;
                        lastNinjaFrameTime = now;
                    }
                } else {
                    ninjaCurrentAction = 0;
                    if (now - lastNinjaFrameTime > NINJA_FRAME_DURATION) {
                        ninjaRunFrameIndex = (ninjaRunFrameIndex + 1) % 10;
                        lastNinjaFrameTime = now;
                    }
                }
            }
            if ("Hatman".equals(selectedCharacter)) {
                long now = System.currentTimeMillis();
                if (isJumping) {
                    hatmanCurrentAction = 1;
                    if (now - lastHatmanFrameTime > HATMAN_FRAME_DURATION) {
                        hatmanJumpFrameIndex = (hatmanJumpFrameIndex + 1) % 10;
                        lastHatmanFrameTime = now;
                    }
                } else if (isSliding) {
                    hatmanCurrentAction = 2;
                    if (now - lastHatmanFrameTime > HATMAN_FRAME_DURATION) {
                        hatmanSlideFrameIndex = (hatmanSlideFrameIndex + 1) % 10;
                        lastHatmanFrameTime = now;
                    }
                } else {
                    hatmanCurrentAction = 0;
                    if (now - lastHatmanFrameTime > HATMAN_FRAME_DURATION) {
                        hatmanRunFrameIndex = (hatmanRunFrameIndex + 1) % 10;
                        lastHatmanFrameTime = now;
                    }
                }
            }
            if ("Advanture_Girl".equals(selectedCharacter)) {
                long now = System.currentTimeMillis();
                if (isJumping) {
                    advantureGirlCurrentAction = 1;
                    if (now - lastAdvantureGirlFrameTime > ADVANTURE_GIRL_FRAME_DURATION) {
                        advantureGirlJumpFrameIndex = (advantureGirlJumpFrameIndex + 1) % 10;
                        lastAdvantureGirlFrameTime = now;
                    }
                } else if (isSliding) {
                    advantureGirlCurrentAction = 2;
                    if (now - lastAdvantureGirlFrameTime > ADVANTURE_GIRL_FRAME_DURATION) {
                        advantureGirlSlideFrameIndex = (advantureGirlSlideFrameIndex + 1) % 5;
                        lastAdvantureGirlFrameTime = now;
                    }
                } else {
                    advantureGirlCurrentAction = 0;
                    if (now - lastAdvantureGirlFrameTime > ADVANTURE_GIRL_FRAME_DURATION) {
                        advantureGirlRunFrameIndex = (advantureGirlRunFrameIndex + 1) % 8;
                        lastAdvantureGirlFrameTime = now;
                    }
                }
            }
        }
    }

    private boolean showGameOverText = true;

    public void setShowGameOverText(boolean show) {
        this.showGameOverText = show;
    }

    private void draw() {
        if (!imagesLoadedSuccessfully) {
            if (holder.getSurface().isValid()) {
                canvas = holder.lockCanvas();
                canvas.drawColor(Color.BLACK);
                paint.setColor(Color.RED);
                paint.setTextSize(60);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Error: Missing or invalid game images!", screenWidth/2, screenHeight/2, paint);
                paint.setTextSize(30);
                canvas.drawText("Check your drawable folder and logcat.", screenWidth/2, screenHeight/2 + 60, paint);
                holder.unlockCanvasAndPost(canvas);
            }
            return;
        }
        if (holder.getSurface().isValid()) {
            canvas = holder.lockCanvas();

            // Draw alternating backgrounds in seamless loop: Background1 → Background2 → Background1 → Background2...
            if (background1 != null && background2 != null) {
                backgroundPaint.setAlpha(255);

                // Draw alternating pattern: Background1, Background2, Background1, Background2...
                canvas.drawBitmap(background1, masterBackgroundX, 0, backgroundPaint);
                canvas.drawBitmap(background2, masterBackgroundX + screenWidth, 0, backgroundPaint);
                canvas.drawBitmap(background1, masterBackgroundX + screenWidth * 2, 0, backgroundPaint);
                canvas.drawBitmap(background2, masterBackgroundX + screenWidth * 3, 0, backgroundPaint);
                canvas.drawBitmap(background1, masterBackgroundX + screenWidth * 4, 0, backgroundPaint);
                canvas.drawBitmap(background2, masterBackgroundX + screenWidth * 5, 0, backgroundPaint);
            } else {
                canvas.drawColor(Color.rgb(30, 60, 114));
            }

            paint.setAlpha(255); // Ensure main paint is opaque for other elements

            paint.setColor(Color.WHITE);
            canvas.drawRect(0, groundY, screenWidth, screenHeight, paint);

if (isSliding && slideFrames != null && slideFrameCount > 0 && allSlideFramesLoaded && "adventure_girl".equals(selectedCharacter)) {
    long now = System.currentTimeMillis();
    if (now - lastSlideFrameTime > SLIDE_FRAME_DURATION) {
        slideFrameIndex = (slideFrameIndex + 1) % slideFrameCount;
        lastSlideFrameTime = now;
    }
    canvas.save();
    int slideFrameHeight = slideFrames[slideFrameIndex].getHeight();
    int gifHeight = ninjaRunFrames != null && ninjaRunFrames[0] != null ? ninjaRunFrames[0].getHeight() : slideFrameHeight;
    canvas.translate(playerX, playerY);
    if (slideFrames[slideFrameIndex] != null) {
        canvas.drawBitmap(slideFrames[slideFrameIndex], 0, 0, null);
    } else {
        Log.e(TAG, "slideFrames[" + slideFrameIndex + "] is null during draw");
    }
    canvas.restore();
} else if ("Ninja".equals(selectedCharacter)) {
    // Draw Ninja character using frame animation, scaled to 40% of original frame size
    Bitmap frame = null;
    if (isJumping) {
        frame = ninjaJumpFrames[ninjaJumpFrameIndex];
    } else if (isSliding) {
        frame = ninjaSlideFrames[ninjaSlideFrameIndex];
    } else {
        frame = ninjaRunFrames[ninjaRunFrameIndex];
    }
    if (frame != null) {
        int targetWidth = (int) (frame.getWidth() * 0.3f);
        int targetHeight = (int) (frame.getHeight() * 0.3f);
        int bottomY = (int)playerY + targetHeight;
        int topY = (int)playerY;
        // For sliding, position the character a little bit lower
        if (isSliding) {
            int slideOffset = (int)(targetHeight * 0.3f); // Increased from 0.3f to 0.5f for more downward movement
            topY += slideOffset;
            bottomY += slideOffset;
        }
        Rect destRect = new Rect((int)playerX, topY, (int)playerX + targetWidth, bottomY);
        canvas.save();
        canvas.drawBitmap(frame, null, destRect, null);
        canvas.restore();
    } else if (frame == null) {
        Log.e(TAG, "Ninja frame is null in draw(), index: run=" + ninjaRunFrameIndex + ", jump=" + ninjaJumpFrameIndex + ", slide=" + ninjaSlideFrameIndex);
    }
}
else if ("Hatman".equals(selectedCharacter)) {
    Bitmap frame = null;
    if (isJumping) {
        frame = hatmanJumpFrames[hatmanJumpFrameIndex];
    } else if (isSliding) {
        frame = hatmanSlideFrames[hatmanSlideFrameIndex];
    } else {
        frame = hatmanRunFrames[hatmanRunFrameIndex];
    }
    if (frame != null) {
        int targetWidth = (int) (frame.getWidth() * 0.3f);
        int targetHeight = (int) (frame.getHeight() * 0.3f);
        int bottomY = (int)playerY + targetHeight;
        int topY = (int)playerY;
        // For sliding, position the character a little bit lower
        if (isSliding) {
            int slideOffset = (int)(targetHeight * 0.5f); // Increased from 0.3f to 0.5f for more downward movement
            topY += slideOffset;
            bottomY += slideOffset;
        }
        Rect destRect = new Rect((int)playerX, topY, (int)playerX + targetWidth, bottomY);
        canvas.save();
        canvas.drawBitmap(frame, null, destRect, null);
        canvas.restore();
    } else if (frame == null) {
        Log.e(TAG, "Hatman frame is null in draw(), index: run=" + hatmanRunFrameIndex + ", jump=" + hatmanJumpFrameIndex + ", slide=" + hatmanSlideFrameIndex);
    }
}
else if ("Advanture_Girl".equals(selectedCharacter)) {
    Bitmap frame = null;
    if (isJumping) {
        frame = advantureGirlJumpFrames[advantureGirlJumpFrameIndex];
    } else if (isSliding) {
        frame = advantureGirlSlideFrames[advantureGirlSlideFrameIndex];
    } else {
        frame = advantureGirlRunFrames[advantureGirlRunFrameIndex];
    }
    if (frame != null) {
        int targetWidth = (int) (frame.getWidth() * 0.3f);
        int targetHeight = (int) (frame.getHeight() * 0.3f);
        int bottomY;
        int topY;
        if (isSliding) {
            bottomY = (int)groundY - BOTTOM_OFFSET + 15; // Added 15 pixels to move sliding character down a bit more
            topY = bottomY - targetHeight;
        } else {
            bottomY = (int)playerY + targetHeight;
            topY = (int)playerY;
        }
        Rect destRect = new Rect((int)playerX, topY, (int)playerX + targetWidth, bottomY);
        canvas.save();
        canvas.drawBitmap(frame, null, destRect, null);
        canvas.restore();
    } else if (frame == null) {
        Log.e(TAG, "Advanture_Girl frame is null in draw(), index: run=" + advantureGirlRunFrameIndex + ", jump=" + advantureGirlJumpFrameIndex + ", slide=" + advantureGirlSlideFrameIndex);
    }
}

            // Draw obstacles
            for (int i = 0; i < obstacles.length; i++) {
                if (isCeilingObstacle[i] && ceilingObstacleImage != null) {
                    canvas.drawBitmap(ceilingObstacleImage, null, obstacles[i], paint);
                } else if (obstacleImages[obstacleImageIndices[i]] != null) {
                    canvas.drawBitmap(obstacleImages[obstacleImageIndices[i]], null, obstacles[i], paint);
                }
            }

            // Draw coins
            if (coinImage != null && !coinImage.isRecycled()) {
                for (Rect coin : coinsRect) {
                    canvas.drawBitmap(coinImage, null, coin, paint);
                }
            }

            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void control() {
        try {
            Thread.sleep(17); // ~60 FPS
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onSettingsChanged() {
        // Remove all MediaPlayer gameMusicPlayer fields and methods
    }


    public void setScoreUpdateListener(ScoreUpdateListener listener) {
        this.scoreUpdateListener = listener;
    }

    private Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        if (drawable == null) {
            Log.e(TAG, "drawableToBitmap: Input drawable is null");
            return null;
        }
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "drawableToBitmap: Invalid dimensions (" + width + "x" + height + ")");
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Rect adjustCoinPosition(Rect coinRect, int maxHeight) {
        final int MIN_VERTICAL_DISTANCE = 40;
        final int MAX_ATTEMPTS = 10;
        int attempts = 0;
        boolean tooClose;

        do {
            tooClose = false;
            for (Rect obstacle : obstacles) {
                if (Rect.intersects(coinRect, obstacle) ||
                    (Math.abs(coinRect.top - obstacle.bottom) < MIN_VERTICAL_DISTANCE &&
                     coinRect.left < obstacle.right && coinRect.right > obstacle.left)) {
                    tooClose = true;
                    coinRect.offset(0, -20);
                    // Prevent coin from going above maxHeight
                    if (coinRect.top < (int)(groundY - maxHeight)) {
                        coinRect.offsetTo(coinRect.left, (int)(groundY - maxHeight));
                    }
                    break;
                }
            }
            attempts++;
        } while (tooClose && attempts < MAX_ATTEMPTS);

        return coinRect;
    }
    public int getCurrentSpeedLevel() {
        return speedLevel;
    }
    public int getCurrentMoveSpeed() {
        return currentMoveSpeed;
    }
    private void playCoinSound() {
        try {
            MediaPlayer coinPlayer = MediaPlayer.create(context, R.raw.coin);
            if (coinPlayer != null) {
                coinPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                });
                coinPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing coin sound: " + e.getMessage());
        }
    }
    private void playJumpSound() {
        try {
            if (soundPool != null && soundPoolLoaded) {
                soundPool.play(jumpSoundId, 1f, 1f, 1, 0, 1f);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing jump sound: " + e.getMessage());
        }
    }
    private void playGameOverSound() {
        try {
            MediaPlayer gameOverPlayer = MediaPlayer.create(context, R.raw.gameover);
            if (gameOverPlayer != null) {

                float userVolume = 0.7f; // Default
                try {
                    android.content.SharedPreferences prefs = context.getSharedPreferences("BhagBhagPrefs", android.content.Context.MODE_PRIVATE);
                    int musicVolumePercent = prefs.getInt("musicVolume", 70);
                    userVolume = musicVolumePercent / 100f;
                } catch (Exception e) {
                    // fallback to default
                }
                float loudVolume = Math.min(userVolume * 1.5f, 1.0f);
                gameOverPlayer.setVolume(loudVolume, loudVolume);
                gameOverPlayer.setOnCompletionListener(mp -> mp.release());
                gameOverPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing game over sound: " + e.getMessage());
        }
    }
    private void playLevelUpSound() {
        try {
            MediaPlayer levelUpPlayer = MediaPlayer.create(context, R.raw.levelup);
            if (levelUpPlayer != null) {
                levelUpPlayer.setOnCompletionListener(mp -> mp.release());
                levelUpPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing level up sound: " + e.getMessage());
        }
    }
    private void playSlideSound() {
        try {
            // Use the same sound as jump for now, or you can add a specific slide sound
            if (soundPool != null && soundPoolLoaded) {
                soundPool.play(jumpSoundId, 0.7f, 0.7f, 1, 0, 1f);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing slide sound: " + e.getMessage());
        }
    }
    private void performHapticFeedback() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.os.VibrationEffect effect = android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(effect);
                }
            } else {
                android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing haptic feedback: " + e.getMessage());
        }
    }
    private boolean pixelPerfectCollision(Bitmap playerBitmap, Rect playerRect, Bitmap obstacleBitmap, Rect obstacleRect) {
        Rect intersection = new Rect();
        if (!Rect.intersects(playerRect, obstacleRect)) return false;
        intersection.set(
            Math.max(playerRect.left, obstacleRect.left),
            Math.max(playerRect.top, obstacleRect.top),
            Math.min(playerRect.right, obstacleRect.right),
            Math.min(playerRect.bottom, obstacleRect.bottom)
        );
        if (intersection.width() <= 0 || intersection.height() <= 0) return false;

        float playerScaleX = playerBitmap.getWidth() / (float)playerRect.width();
        float playerScaleY = playerBitmap.getHeight() / (float)playerRect.height();
        float obstacleScaleX = obstacleBitmap.getWidth() / (float)obstacleRect.width();
        float obstacleScaleY = obstacleBitmap.getHeight() / (float)obstacleRect.height();

        for (int y = intersection.top; y < intersection.bottom; y++) {
            for (int x = intersection.left; x < intersection.right; x++) {
                int playerPx = (int)((x - playerRect.left) * playerScaleX);
                int playerPy = (int)((y - playerRect.top) * playerScaleY);
                int obstaclePx = (int)((x - obstacleRect.left) * obstacleScaleX);
                int obstaclePy = (int)((y - obstacleRect.top) * obstacleScaleY);
                if (playerPx < 0 || playerPx >= playerBitmap.getWidth() || playerPy < 0 || playerPy >= playerBitmap.getHeight()) continue;
                if (obstaclePx < 0 || obstaclePx >= obstacleBitmap.getWidth() || obstaclePy < 0 || obstaclePy >= obstacleBitmap.getHeight()) continue;
                int playerAlpha = (playerBitmap.getPixel(playerPx, playerPy) >>> 24);
                int obstacleAlpha = (obstacleBitmap.getPixel(obstaclePx, obstaclePy) >>> 24);
                if (playerAlpha > 20 && obstacleAlpha > 20) {
                    return true;
                }
            }
        }
        return false;
    }
    private int getCurrentCharacterFrameHeight() {
        if ("Ninja".equals(selectedCharacter) && ninjaRunFrames != null && ninjaRunFrames[0] != null) {
            return ninjaRunFrames[0].getHeight();
        } else if ("Hatman".equals(selectedCharacter) && hatmanRunFrames != null && hatmanRunFrames[0] != null) {
            return hatmanRunFrames[0].getHeight();
        } else if ("Advanture_Girl".equals(selectedCharacter) && advantureGirlRunFrames != null && advantureGirlRunFrames[0] != null) {
            return advantureGirlRunFrames[0].getHeight();
        }
        return 96; // fallback
    }
    private int getCurrentCharacterScaledHeight() {
        if ("Ninja".equals(selectedCharacter) && ninjaRunFrames != null && ninjaRunFrames[0] != null) {
            return (int)(ninjaRunFrames[0].getHeight() * 0.3f);
        } else if ("Hatman".equals(selectedCharacter) && hatmanRunFrames != null && hatmanRunFrames[0] != null) {
            return (int)(hatmanRunFrames[0].getHeight() * 0.3f);
        } else if ("Advanture_Girl".equals(selectedCharacter) && advantureGirlRunFrames != null && advantureGirlRunFrames[0] != null) {
            return (int)(advantureGirlRunFrames[0].getHeight() * 0.3f);
        }
        return 29;
    }
    public void setJumpPhysics(float jumpForce, float gravity) {
        this.jumpForce = jumpForce;
        this.gravity = gravity;
        Log.d(TAG, "Jump physics updated - jumpForce: " + jumpForce + ", gravity: " + gravity);

        if (!isJumping) {
            int scaledHeight = getCurrentCharacterScaledHeight();
            float groundLevel = groundY - BOTTOM_OFFSET - scaledHeight + CHARACTER_ALIGNMENT_OFFSET;
            playerY = groundLevel;
            playerSpeed = 0;
            Log.d(TAG, "Character repositioned after physics update: " + groundLevel);
        }
    }
}
