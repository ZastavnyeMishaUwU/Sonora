package com.example.it_robota.musicplayback;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.R;
import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.models.Track;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends AppCompatActivity {

    private TextView trackTitle;
    private TextView playbackStatus;
    private Button playPauseButton;
    private Button stopButton;

    private MusicPlayerManager musicPlayer;
    private JamendoApiClient apiClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private String trackId;
    private String currentTrackUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        trackTitle = findViewById(R.id.trackTitle);
        playbackStatus = findViewById(R.id.playbackStatus);
        playPauseButton = findViewById(R.id.playPauseButton);
        stopButton = findViewById(R.id.stopButton);

        musicPlayer = new MusicPlayerManager();
        apiClient = new JamendoApiClient(this);
        if (getIntent() != null) {
            trackId = getIntent().getStringExtra("TRACK_ID");
        }

        if (trackId != null && !trackId.isEmpty()) {
            trackTitle.setText("Loading track " + trackId + "...");
            loadTrackAndPlay(trackId);
        } else {
            trackTitle.setText("Track ID is missing");
        }
        playPauseButton.setOnClickListener(v -> {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
                playbackStatus.setText("Paused");
                playPauseButton.setText("Play");
            } else {
                if ("Paused".contentEquals(playbackStatus.getText())) {
                    musicPlayer.resume();
                    playbackStatus.setText("Playing");
                    playPauseButton.setText("Pause");
                } else if (currentTrackUrl != null && !currentTrackUrl.isEmpty()) {
                    musicPlayer.play(currentTrackUrl);
                    playbackStatus.setText("Playing");
                    playPauseButton.setText("Pause");
                }
            }
        });
        stopButton.setOnClickListener(v -> {
            musicPlayer.stop();
            playbackStatus.setText("Stopped");
            playPauseButton.setText("Play");
        });
    }
    private void loadTrackAndPlay(String id) {
        executorService.execute(() -> {
            try {
                Track track = apiClient.getTrackDetails(id);

                runOnUiThread(() -> {
                    if (track != null) {
                        // Отримуємо прямий URL на MP3 з вашої моделі Track
                        currentTrackUrl = track.getAudioUrl();

                        // Формуємо гарну назву (Назва - Виконавець)
                        String titleText = track.getName() != null ? track.getName() : "Track ID: " + id;
                        if (track.getArtistName() != null && !track.getArtistName().isEmpty()) {
                            titleText += " - " + track.getArtistName();
                        }
                        trackTitle.setText(titleText);

                        // Запускаємо відтворення
                        if (currentTrackUrl != null && !currentTrackUrl.isEmpty()) {
                            musicPlayer.play(currentTrackUrl);
                            playbackStatus.setText("Playing");
                            playPauseButton.setText("Pause");
                        } else {
                            Toast.makeText(PlayerActivity.this, "Audio URL is empty", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    trackTitle.setText("Error loading track");
                    Toast.makeText(PlayerActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicPlayer != null) {
            musicPlayer.release();
        }
        executorService.shutdown();
    }
}