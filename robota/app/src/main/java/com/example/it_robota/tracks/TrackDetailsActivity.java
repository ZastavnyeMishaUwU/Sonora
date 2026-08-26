package com.example.it_robota.tracks;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.R;
import com.example.it_robota.downloader.TrackDownloadManager;
import com.example.it_robota.models.Track;
import com.example.it_robota.musicplayback.MusicPlayerManager;
import com.example.it_robota.repositories.TrackRepository;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Displays track information and delegates track actions to their existing managers.
 */
public class TrackDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_TRACK_ID = "trackId";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private TrackRepository trackRepository;
    private MusicPlayerManager musicPlayerManager;
    private TrackDownloadManager trackDownloadManager;
    private Track currentTrack;
    private View contentView;
    private ProgressBar progressBar;
    private TextView stateTextView;
    private TextView trackNameTextView;
    private TextView artistNameTextView;
    private TextView albumNameTextView;
    private TextView durationTextView;
    private TextView favoriteStatusTextView;
    private TextView downloadedStatusTextView;
    private Button favoriteButton;
    private Button downloadButton;
    private boolean favoriteUpdateInProgress;

    /**
     * Initializes dependencies, view bindings, actions and track loading.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_details);
        bindViews();

        trackRepository = new TrackRepository(this);
        musicPlayerManager = new MusicPlayerManager();
        trackDownloadManager = new TrackDownloadManager(this);

        findViewById(R.id.btnPlayTrack).setOnClickListener(view -> playTrack());
        favoriteButton.setOnClickListener(view -> toggleFavorite());
        downloadButton.setOnClickListener(view -> downloadTrack());

        String trackId = getIntent().getStringExtra(EXTRA_TRACK_ID);
        if (trackId == null || trackId.trim().isEmpty()) {
            showError(R.string.track_details_missing_id);
            return;
        }
        loadTrack(trackId.trim());
    }

    /**
     * Resolves and stores all views used by the screen.
     */
    private void bindViews() {
        contentView = findViewById(R.id.trackDetailsContent);
        progressBar = findViewById(R.id.trackDetailsProgress);
        stateTextView = findViewById(R.id.tvTrackDetailsState);
        trackNameTextView = findViewById(R.id.tvTrackDetailsName);
        artistNameTextView = findViewById(R.id.tvTrackDetailsArtist);
        albumNameTextView = findViewById(R.id.tvTrackDetailsAlbum);
        durationTextView = findViewById(R.id.tvTrackDetailsDuration);
        favoriteStatusTextView = findViewById(R.id.tvTrackDetailsFavorite);
        downloadedStatusTextView = findViewById(R.id.tvTrackDetailsDownloaded);
        favoriteButton = findViewById(R.id.btnToggleTrackFavorite);
        downloadButton = findViewById(R.id.btnDownloadTrack);
    }

    /**
     * Loads track details and downloaded state outside the UI thread.
     *
     * @param trackId identifier of the track to load
     */
    private void loadTrack(String trackId) {
        showLoading();
        executorService.execute(() -> {
            try {
                Track track = trackRepository.getTrackDetails(trackId);
                if (track != null && trackDownloadManager.isTrackDownloaded(trackId)) {
                    track.setLocalFilePath(trackDownloadManager.getLocalFilePath(trackId));
                }
                runOnUiThread(() -> {
                    if (track == null) {
                        showError(R.string.track_details_not_found);
                    } else {
                        currentTrack = track;
                        renderTrack();
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> showError(R.string.track_details_load_error));
            }
        });
    }

    /**
     * Displays the loaded track and its current action states.
     */
    private void renderTrack() {
        progressBar.setVisibility(View.GONE);
        stateTextView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        trackNameTextView.setText(valueOrFallback(currentTrack.getName(), R.string.track_details_unknown_track));
        artistNameTextView.setText(valueOrFallback(
                currentTrack.getArtistName(),
                R.string.track_details_unknown_artist
        ));
        albumNameTextView.setText(valueOrFallback(currentTrack.getAlbumName(), R.string.track_details_unknown_album));
        durationTextView.setText(formatDuration(currentTrack.getDuration()));
        updateStatuses();
    }

    /**
     * Refreshes favorite and downloaded labels and button states.
     */
    private void updateStatuses() {
        favoriteStatusTextView.setText(currentTrack.isFavorite()
                ? R.string.track_details_favorite_yes : R.string.track_details_favorite_no);
        favoriteButton.setText(currentTrack.isFavorite()
                ? R.string.track_details_remove_favorite : R.string.track_details_add_favorite);
        favoriteButton.setEnabled(!favoriteUpdateInProgress);

        boolean downloaded = currentTrack.getLocalFilePath() != null
                && !currentTrack.getLocalFilePath().trim().isEmpty();
        downloadedStatusTextView.setText(downloaded
                ? R.string.track_details_downloaded_yes : R.string.track_details_downloaded_no);
        downloadButton.setEnabled(!downloaded);
        downloadButton.setText(downloaded
                ? R.string.track_details_downloaded_button : R.string.track_details_download);
    }

    /**
     * Starts playback using a downloaded file when available, otherwise streaming audio.
     */
    private void playTrack() {
        if (currentTrack == null) {
            return;
        }
        String audioSource = currentTrack.getLocalFilePath();
        if (audioSource == null || audioSource.trim().isEmpty()) {
            audioSource = currentTrack.getAudioUrl();
        }
        if (audioSource == null || audioSource.trim().isEmpty()) {
            Toast.makeText(this, R.string.track_details_audio_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        musicPlayerManager.play(audioSource);
        Toast.makeText(this, R.string.track_details_playing, Toast.LENGTH_SHORT).show();
    }

    /**
     * Adds or removes the current track from favorites and updates the screen.
     */
    private void toggleFavorite() {
        if (currentTrack == null || favoriteUpdateInProgress) {
            return;
        }

        boolean removeFavorite = currentTrack.isFavorite();
        favoriteUpdateInProgress = true;
        favoriteButton.setEnabled(false);
        executorService.execute(() -> {
            try {
                if (removeFavorite) {
                    trackRepository.removeFavorite(currentTrack.getId());
                } else {
                    trackRepository.saveFavorite(currentTrack);
                }
                runOnUiThread(() -> {
                    currentTrack.setFavorite(!removeFavorite);
                    favoriteUpdateInProgress = false;
                    updateStatuses();
                    Toast.makeText(
                            this,
                            removeFavorite
                                    ? R.string.track_details_favorite_removed
                                    : R.string.track_details_favorite_saved,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    favoriteUpdateInProgress = false;
                    updateStatuses();
                    Toast.makeText(this, R.string.track_details_favorite_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Downloads the current track in the background and refreshes its status.
     */
    private void downloadTrack() {
        if (currentTrack == null) {
            return;
        }
        downloadButton.setEnabled(false);
        downloadButton.setText(R.string.track_details_downloading);
        executorService.execute(() -> {
            try {
                trackDownloadManager.downloadTrack(currentTrack);
                runOnUiThread(() -> {
                    updateStatuses();
                    Toast.makeText(this, R.string.track_details_download_success, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    downloadButton.setEnabled(true);
                    downloadButton.setText(R.string.track_details_download);
                    Toast.makeText(this, R.string.track_details_download_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Shows the loading indicator while hiding content and messages.
     */
    private void showLoading() {
        contentView.setVisibility(View.GONE);
        stateTextView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Shows an error message while hiding content and progress.
     *
     * @param messageResource string resource describing the error
     */
    private void showError(int messageResource) {
        contentView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        stateTextView.setText(messageResource);
        stateTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Returns a display-safe value for an optional text field.
     *
     * @param value value returned by the track API
     * @param fallbackResource string resource used when the value is missing
     * @return original value or localized fallback text
     */
    private String valueOrFallback(String value, int fallbackResource) {
        return value == null || value.trim().isEmpty() ? getString(fallbackResource) : value;
    }

    /**
     * Formats a duration in seconds as minutes and seconds.
     *
     * @param seconds duration in seconds
     * @return duration formatted as m:ss
     */
    private String formatDuration(int seconds) {
        int safeSeconds = Math.max(seconds, 0);
        return String.format(Locale.ROOT, "%d:%02d", safeSeconds / 60, safeSeconds % 60);
    }

    /**
     * Releases playback and background-thread resources when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        musicPlayerManager.release();
        executorService.shutdownNow();
    }
}
