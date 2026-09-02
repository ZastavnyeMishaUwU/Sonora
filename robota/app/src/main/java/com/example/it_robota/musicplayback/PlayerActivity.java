package com.example.it_robota.musicplayback;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.it_robota.R;
import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.models.Track;
import com.example.it_robota.tracks.TrackDetailsActivity;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles audio streaming playback and displays detailed track information.
 */
public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_TRACK_ID =
            "TRACK_ID";

    private ImageView trackCoverImage;

    private TextView trackTitle;
    private TextView artistName;
    private TextView currentTime;
    private TextView totalTime;

    private SeekBar seekBar;

    private MaterialButton playPauseButton;
    private MaterialButton stopButton;
    private MaterialButton backButton;
    private MaterialButton detailsButton;

    private MusicPlayerManager musicPlayer;
    private JamendoApiClient apiClient;
    private SessionManager sessionManager;
    private ExecutorService executorService;

    private final Handler handler =
            new Handler(
                    Looper.getMainLooper()
            );

    private Runnable updateProgressRunnable;

    private String trackId;
    private String currentTrackUrl;

    private boolean isUserTracking = false;
    private boolean trackDetailsOpening;

    /**
     * Initializes components, view bindings, and starts fetching track details.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_player
        );

        initViews();

        musicPlayer =
                new MusicPlayerManager();

        apiClient =
                new JamendoApiClient(this);

        sessionManager =
                new SessionManager(this);

        updateDetailsAvailability();

        executorService =
                Executors.newSingleThreadExecutor();

        if (getIntent() != null) {
            trackId = getIntent()
                    .getStringExtra(
                            EXTRA_TRACK_ID
                    );
        }

        setupListeners();
        setupProgressUpdater();

        if (trackId != null
                && !trackId.trim().isEmpty()) {

            trackId =
                    trackId.trim();

            trackTitle.setText(
                    "Loading...."
            );

            loadTrackAndPlay(trackId);

        } else {
            trackTitle.setText(
                    "Track ID is missing"
            );

            detailsButton.setEnabled(false);

            Toast.makeText(
                    this,
                    "Error: track ID not provided",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Finds and initializes view references from the layout.
     */
    private void initViews() {
        trackCoverImage =
                findViewById(
                        R.id.trackCoverImage
                );

        trackTitle =
                findViewById(
                        R.id.trackTitle
                );

        artistName =
                findViewById(
                        R.id.artistName
                );

        currentTime =
                findViewById(
                        R.id.currentTime
                );

        totalTime =
                findViewById(
                        R.id.totalTime
                );

        seekBar =
                findViewById(
                        R.id.seekBar
                );

        playPauseButton =
                findViewById(
                        R.id.playPauseButton
                );

        stopButton =
                findViewById(
                        R.id.stopButton
                );

        backButton =
                findViewById(
                        R.id.playerBackButton
                );

        detailsButton =
                findViewById(
                        R.id.playerDetailsButton
                );
    }

    /**
     * Sets up click listeners for player controls and seek bar interactions.
     */
    private void setupListeners() {
        backButton.setOnClickListener(
                view -> finish()
        );

        detailsButton.setOnClickListener(
                view -> openTrackDetails()
        );

        playPauseButton.setOnClickListener(v -> {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();

                playPauseButton.setText(
                        "Play"
                );

            } else {
                if (currentTrackUrl != null
                        && !currentTrackUrl.isEmpty()) {

                    musicPlayer.resume();

                    if (!musicPlayer.isPlaying()) {
                        musicPlayer.play(
                                currentTrackUrl
                        );
                    }

                    playPauseButton.setText(
                            "Pause"
                    );

                } else {
                    Toast.makeText(
                            this,
                            "Audio link unavailable",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });

        stopButton.setOnClickListener(v -> {
            musicPlayer.stop();

            playPauseButton.setText(
                    "Play"
            );

            seekBar.setProgress(0);

            currentTime.setText(
                    "00:00"
            );
        });

        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        if (fromUser) {
                            currentTime.setText(
                                    formatTime(progress)
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                        isUserTracking = true;
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                        isUserTracking = false;

                        musicPlayer.seekTo(
                                seekBar.getProgress()
                        );
                    }
                }
        );
    }

    /**
     * Opens the details screen for the currently playing track.
     */
    private void openTrackDetails() {
        if (trackDetailsOpening || isFinishing() || isDestroyed()
                || !sessionManager.isLoggedIn()) {
            return;
        }

        if (trackId == null
                || trackId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Track details are unavailable",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent = new Intent(
                PlayerActivity.this,
                TrackDetailsActivity.class
        );

        intent.putExtra(
                TrackDetailsActivity.EXTRA_TRACK_ID,
                trackId
        );

        trackDetailsOpening = true;
        updateDetailsAvailability();
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException exception) {
            trackDetailsOpening = false;
            updateDetailsAvailability();
            Toast.makeText(this, R.string.track_details_open_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Keeps the separate details screen unavailable in guest mode.
     */
    private void updateDetailsAvailability() {
        detailsButton.setVisibility(
                sessionManager.isLoggedIn()
                        ? View.VISIBLE
                        : View.GONE
        );
        detailsButton.setEnabled(!trackDetailsOpening
                && trackId != null && !trackId.trim().isEmpty());
    }

    @Override
    protected void onResume() {
        super.onResume();

        trackDetailsOpening = false;
        if (sessionManager != null) {
            updateDetailsAvailability();
        }
    }

    /**
     * Fetches track metadata from API and starts playback off the main thread.
     *
     * @param id target track identifier
     */
    private void loadTrackAndPlay(String id) {
        executorService.execute(() -> {
            try {
                Track track =
                        apiClient.getTrackDetails(id);

                runOnUiThread(() -> {
                    if (isFinishing()
                            || isDestroyed()) {
                        return;
                    }

                    if (track != null) {
                        currentTrackUrl =
                                track.getAudioUrl();

                        trackTitle.setText(
                                track.getName() != null
                                        ? track.getName()
                                        : "Untitled"
                        );

                        artistName.setText(
                                track.getArtistName() != null
                                        ? track.getArtistName()
                                        : "Unknown artist"
                        );

                        if (track.getImageUrl() != null
                                && !track.getImageUrl()
                                .trim()
                                .isEmpty()) {

                            Glide.with(
                                            PlayerActivity.this
                                    )
                                    .load(
                                            track.getImageUrl()
                                    )
                                    .placeholder(
                                            android.R.drawable.ic_menu_gallery
                                    )
                                    .error(
                                            android.R.drawable.ic_dialog_alert
                                    )
                                    .into(
                                            trackCoverImage
                                    );
                        }

                        if (currentTrackUrl != null
                                && !currentTrackUrl
                                .trim()
                                .isEmpty()) {

                            musicPlayer.play(
                                    currentTrackUrl
                            );

                            playPauseButton.setText(
                                    "Pause"
                            );

                            handler.post(
                                    updateProgressRunnable
                            );

                        } else {
                            Toast.makeText(
                                    PlayerActivity.this,
                                    "Error: Missing audio link",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } else {
                        trackTitle.setText(
                                "Track not found"
                        );
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() -> {
                    if (!isFinishing()
                            && !isDestroyed()) {

                        trackTitle.setText(
                                "Loading error"
                        );

                        Toast.makeText(
                                PlayerActivity.this,
                                "Network/API error "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });
    }

    /**
     * Initializes the periodic progress bar update task.
     */
    private void setupProgressUpdater() {
        updateProgressRunnable =
                new Runnable() {

                    @Override
                    public void run() {
                        if (musicPlayer != null
                                && musicPlayer.isPlaying()
                                && !isUserTracking) {

                            int currentPos =
                                    musicPlayer
                                            .getCurrentPosition();

                            int duration =
                                    musicPlayer
                                            .getDuration();

                            if (duration > 0) {
                                seekBar.setMax(
                                        duration
                                );

                                seekBar.setProgress(
                                        currentPos
                                );

                                currentTime.setText(
                                        formatTime(
                                                currentPos
                                        )
                                );

                                totalTime.setText(
                                        formatTime(
                                                duration
                                        )
                                );
                            }
                        }

                        handler.postDelayed(
                                this,
                                500
                        );
                    }
                };
    }

    /**
     * Formats milliseconds into mm:ss string representation.
     *
     * @param milliseconds time duration in milliseconds
     * @return formatted time string
     */
    private String formatTime(int milliseconds) {
        int seconds =
                (milliseconds / 1000) % 60;

        int minutes =
                (milliseconds / (1000 * 60)) % 60;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    /**
     * Cleans up player resources, handlers, and background executors on activity destruction.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (updateProgressRunnable != null) {
            handler.removeCallbacks(
                    updateProgressRunnable
            );
        }

        if (musicPlayer != null) {
            musicPlayer.release();
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
