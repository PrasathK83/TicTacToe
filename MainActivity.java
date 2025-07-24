package com.example.webpage;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private VideoView videoView;
    private WebView webView;
    private MediaPlayer startMusic, loopMusic, gameOverMusic;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        webView = findViewById(R.id.webView);

        // Load user preferences
        preferences = getSharedPreferences("GameSettings", MODE_PRIVATE);
        boolean isMusicEnabled = preferences.getBoolean("MusicEnabled", true); // Default: music on

        // Initialize MediaPlayer
        startMusic = MediaPlayer.create(this, R.raw.gamestart6104);
        loopMusic = MediaPlayer.create(this, R.raw.gamemusicloop7145285);
        gameOverMusic = MediaPlayer.create(this, R.raw.gameover38511);

        // Play splash video
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash_video);
        videoView.setVideoURI(videoUri);
        videoView.start();

        // WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.setWebViewClient(new WebViewClient());

        // Add JavaScript interface for communication between WebView and Android
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.loadUrl("file:///android_asset/index.html");

        // Show WebView after splash video completion
        videoView.setOnCompletionListener(mp -> {
            videoView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);

            // Play start music after video completes
            if (isMusicEnabled && startMusic != null) {
                startMusic.start();
                startMusic.setOnCompletionListener(mp2 -> {
                    // Start looping background music if enabled
                    if (isMusicEnabled && loopMusic != null) {
                        loopMusic.setLooping(true);
                        loopMusic.start();
                    }
                });
            } else {
                // If music is disabled, still start loop music
                if (isMusicEnabled && loopMusic != null) {
                    loopMusic.setLooping(true);
                    loopMusic.start();
                }
            }
        });
    }

    // JavaScript interface to communicate between WebView and Android
    public class WebAppInterface {
        MainActivity activity;

        WebAppInterface(MainActivity activity) {
            this.activity = activity;
        }

        @android.webkit.JavascriptInterface
        public void gameOver() {
            activity.runOnUiThread(() -> activity.onGameOver());
        }

        @android.webkit.JavascriptInterface
        public boolean isMusicEnabled() {
            return activity.preferences.getBoolean("MusicEnabled", true);
        }

        @android.webkit.JavascriptInterface
        public void setMusicEnabled(boolean enabled) {
            // Save the music setting to SharedPreferences
            SharedPreferences.Editor editor = activity.preferences.edit();
            editor.putBoolean("MusicEnabled", enabled);
            editor.apply();

            // Update the music state in the app
            activity.runOnUiThread(() -> {
                if (enabled) {
                    // If enabling music and we're past the video, start the appropriate music
                    if (activity.videoView.getVisibility() == View.GONE) {
                        // Check if the game is over
                        if (activity.gameOverMusic.isPlaying()) {
                            // Don't restart music if game over music is playing
                            return;
                        }

                        // Start appropriate music based on what was playing before
                        if (!activity.startMusic.isPlaying() && !activity.loopMusic.isPlaying()) {
                            activity.loopMusic.setLooping(true);
                            activity.loopMusic.start();
                        }
                    }
                } else {
                    // If disabling music, stop all music
                    if (activity.startMusic.isPlaying()) {
                        activity.startMusic.pause();
                    }
                    if (activity.loopMusic.isPlaying()) {
                        activity.loopMusic.pause();
                    }
                    if (activity.gameOverMusic.isPlaying()) {
                        activity.gameOverMusic.pause();
                    }
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void restartGameMusic() {
            activity.runOnUiThread(() -> {
                // First stop game over music if it's playing
                if (activity.gameOverMusic.isPlaying()) {
                    activity.gameOverMusic.stop();
                    try {
                        activity.gameOverMusic.prepare();
                    } catch (Exception e) {
                        // Handle the exception
                    }
                }

                // Check if music is enabled before restarting
                boolean isMusicEnabled = activity.preferences.getBoolean("MusicEnabled", true);
                if (isMusicEnabled) {
                    // Start loop music
                    if (activity.loopMusic != null && !activity.loopMusic.isPlaying()) {
                        activity.loopMusic.setLooping(true);
                        activity.loopMusic.start();
                    }
                }
            });
        }
    }

    // Method to stop current music and play game over music
    public void onGameOver() {
        boolean isMusicEnabled = preferences.getBoolean("MusicEnabled", true);
        if (loopMusic != null && loopMusic.isPlaying()) {
            loopMusic.stop();
            try {
                loopMusic.prepare();
            } catch (Exception e) {
                // Handle the exception
            }
        }

        if (startMusic != null && startMusic.isPlaying()) {
            startMusic.stop();
            try {
                startMusic.prepare();
            } catch (Exception e) {
                // Handle the exception
            }
        }

        if (isMusicEnabled && gameOverMusic != null) {
            gameOverMusic.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (startMusic != null && startMusic.isPlaying()) {
            startMusic.pause();
        }
        if (loopMusic != null && loopMusic.isPlaying()) {
            loopMusic.pause();
        }
        if (gameOverMusic != null && gameOverMusic.isPlaying()) {
            gameOverMusic.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isMusicEnabled = preferences.getBoolean("MusicEnabled", true);
        if (isMusicEnabled) {
            if (gameOverMusic != null && gameOverMusic.isPlaying()) {
                // Don't interrupt game over music if it's playing
                return;
            }

            if (startMusic != null && !startMusic.isPlaying() && !videoView.isPlaying()) {
                // Only resume start music if video is complete and start music was playing
                if (videoView.getVisibility() == View.GONE) {
                    startMusic.start();
                }
            } else if (loopMusic != null && !loopMusic.isPlaying() && !videoView.isPlaying() &&
                    (startMusic == null || !startMusic.isPlaying())) {
                // Only resume loop music if video is complete and start music is not playing
                if (videoView.getVisibility() == View.GONE) {
                    loopMusic.start();
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseMediaPlayer(startMusic);
        pauseMediaPlayer(loopMusic);
        pauseMediaPlayer(gameOverMusic);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer(startMusic);
        releaseMediaPlayer(loopMusic);
        releaseMediaPlayer(gameOverMusic);
    }

    private void pauseMediaPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    private void releaseMediaPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}