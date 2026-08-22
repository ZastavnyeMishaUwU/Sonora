package com.example.it_robota.musicplayback;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.it_robota.R;
import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.models.Track;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends AppCompatActivity {

    private ImageView trackCoverImage;
    private TextView trackTitle, artistName, currentTime, totalTime;
    private SeekBar seekBar;
    private MaterialButton playPauseButton, stopButton;

    private MusicPlayerManager musicPlayer;
    private JamendoApiClient apiClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateProgressRunnable;

    private String trackId;
    private String currentTrackUrl;
    private boolean isUserTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();

        musicPlayer = new MusicPlayerManager();
        apiClient = new JamendoApiClient(this);

        if (getIntent() != null) {
            trackId = getIntent().getStringExtra("TRACK_ID");
        }

        if (trackId != null && !trackId.isEmpty()) {
            trackTitle.setText("Loading...");
            loadTrackAndPlay(trackId);
        }

        setupListeners();
        setupProgressUpdater();
    }

    private void initViews() {
        trackCoverImage = findViewById(R.id.trackCoverImage);
        trackTitle = findViewById(R.id.trackTitle);
        artistName = findViewById(R.id.artistName);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        seekBar = findViewById(R.id.seekBar);
        playPauseButton = findViewById(R.id.playPauseButton);
        stopButton = findViewById(R.id.stopButton);
    }

    private void setupListeners() {
        playPauseButton.setOnClickListener(v -> {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
                playPauseButton.setText("Play");
            } else {
                musicPlayer.resume();
                if (!musicPlayer.isPlaying() && currentTrackUrl != null) {
                    musicPlayer.play(currentTrackUrl);
                }
                playPauseButton.setText("Pause");
            }
        });

        stopButton.setOnClickListener(v -> {
            musicPlayer.stop();
            playPauseButton.setText("Play");
            seekBar.setProgress(0);
            currentTime.setText("00:00");
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserTracking = false;
                musicPlayer.seekTo(seekBar.getProgress());
            }
        });
    }

    private void loadTrackAndPlay(String id) {
        executorService.execute(() -> {
            try {
                Track track = apiClient.getTrackDetails(id);

                runOnUiThread(() -> {
                    if (track != null) {
                        currentTrackUrl = track.getAudioUrl();
                        trackTitle.setText(track.getName());
                        artistName.setText(track.getArtistName());

                        // Завантаження обкладинки через Glide
                        if (track.getImageUrl() != null && !track.getImageUrl().isEmpty()) {
                            Glide.with(PlayerActivity.this)
                                    .load(track.getImageUrl())
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_dialog_alert)
                                    .into(trackCoverImage);
                        }

                        if (currentTrackUrl != null && !currentTrackUrl.isEmpty()) {
                            musicPlayer.play(currentTrackUrl);
                            playPauseButton.setText("Pause");
                            handler.post(updateProgressRunnable);
                        } else {
                            Toast.makeText(PlayerActivity.this, "Audio URL is empty", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace(); // Друкуємо детальну помилку в Logcat
                runOnUiThread(() -> {
                    trackTitle.setText("Error loading track");
                    // Виводимо точний текст помилки на екран
                    Toast.makeText(PlayerActivity.this, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupProgressUpdater() {
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicPlayer != null && musicPlayer.isPlaying() && !isUserTracking) {
                    int currentPos = musicPlayer.getCurrentPosition();
                    int duration = musicPlayer.getDuration();

                    if (duration > 0) {
                        seekBar.setMax(duration);
                        seekBar.setProgress(currentPos);
                        currentTime.setText(formatTime(currentPos));
                        totalTime.setText(formatTime(duration));
                    }
                }
                handler.postDelayed(this, 500);
            }
        };
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateProgressRunnable);
        if (musicPlayer != null) {
            musicPlayer.release();
        }
        executorService.shutdown();
    }
}