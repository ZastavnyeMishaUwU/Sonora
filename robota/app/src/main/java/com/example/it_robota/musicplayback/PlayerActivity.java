package com.example.it_robota.musicplayback;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.it_robota.R;

/**
 * Screen used to control playback of the selected track.
 */
public class PlayerActivity extends AppCompatActivity {

    /**
     * Displays the current track title.
     */
    private TextView trackTitle;

    /**
     * Displays the current playback state.
     */
    private TextView playbackStatus;

    /**
     * Button used to play or pause the current track.
     */
    private Button playPauseButton;

    /**
     * Button used to stop playback.
     */
    private Button stopButton;

    /**
     * Handles music playback.
     */
    private MusicPlayerManager musicPlayer;

    /**
     * Temporary track URL for testing.
     * Later this value should come from the selected track.
     */
    private final String currentTrackUrl = "TRACK_URL";

    /**
     * Initializes the player screen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        trackTitle = findViewById(R.id.trackTitle);
        playbackStatus = findViewById(R.id.playbackStatus);

        playPauseButton = findViewById(R.id.playPauseButton);
        stopButton = findViewById(R.id.stopButton);

        musicPlayer = new MusicPlayerManager();

        /**
         * Temporary track title.
         */
        trackTitle.setText("Selected Track");

        /**
         * Handles Play / Pause button clicks.
         */
        playPauseButton.setOnClickListener(v -> {

            if (musicPlayer.isPlaying()) {

                musicPlayer.pause();

                playbackStatus.setText("Paused");
                playPauseButton.setText("Play");

            } else {

                if ("Paused".contentEquals(playbackStatus.getText())) {

                    musicPlayer.resume();

                } else {

                    musicPlayer.play(currentTrackUrl);

                }

                playbackStatus.setText("Playing");
                playPauseButton.setText("Pause");
            }
        });

        /**
         * Handles Stop button clicks.
         */
        stopButton.setOnClickListener(v -> {

            musicPlayer.stop();

            playbackStatus.setText("Stopped");
            playPauseButton.setText("Play");
        });
    }

    /**
     * Releases MediaPlayer resources when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        musicPlayer.release();
    }
}