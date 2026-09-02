package com.example.it_robota.tracks;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.example.it_robota.R;
import com.example.it_robota.auth.AuthenticationGuard;
import com.example.it_robota.auth.AccountActivity;
import com.example.it_robota.auth.AccountSession;
import com.example.it_robota.downloader.TrackDownloadManager;
import com.example.it_robota.models.Track;
import com.example.it_robota.musicplayback.MusicPlayerManager;
import com.example.it_robota.repositories.TrackRepository;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Displays track information and delegates track actions to their existing managers.
 */
public class TrackDetailsActivity extends AccountActivity {

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
    private MaterialButton favoriteButton;
    private MaterialButton downloadButton;
    private boolean favoriteUpdateInProgress;
    private boolean downloadInProgress;
    private String trackId;

    /**
     * Requires a login and prepares track actions before enabling account refreshes.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthenticationGuard.requireLoggedIn(this)) {
            return;
        }

        setContentView(R.layout.activity_track_details);
        bindViews();

        trackRepository = new TrackRepository(this);
        musicPlayerManager = MusicPlayerManager.getInstance();
        trackDownloadManager = new TrackDownloadManager(this);

        findViewById(R.id.btnTrackDetailsBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnPlayTrack).setOnClickListener(view -> playTrack());
        favoriteButton.setOnClickListener(view -> toggleFavorite());
        downloadButton.setOnClickListener(view -> downloadTrack());

        trackId = getIntent().getStringExtra(EXTRA_TRACK_ID);
        accountUiReady();
    }

    /** Clears account-specific track state, stops playback and hides the previous content. */
    @Override
    protected void clearAccountContent() {
        currentTrack = null;
        favoriteUpdateInProgress = false;
        downloadInProgress = false;
        musicPlayerManager.stop();
        contentView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    /**
     * Checks the session and track ID before starting a details request.
     *
     * @param account session to display, or null to show the login-required message
     * @param revision content revision captured for this load
     */
    @Override
    protected void loadAccountContent(AccountSession account, int revision) {
        if (account == null) {
            showError(R.string.track_details_login_required);
            return;
        }
        if (trackId == null || trackId.trim().isEmpty()) {
            showError(R.string.track_details_missing_id);
            return;
        }
        loadTrack(trackId.trim(), account, revision);
    }

    /** Binds content, status and action views from the track-details layout. */
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
     * Loads details and account-owned favorite and download states outside the UI thread.
     * Results from an older session or content revision are discarded.
     *
     * @param trackId track identifier
     * @param account session captured before the request
     * @param revision content revision captured with the session
     */
    private void loadTrack(String trackId, AccountSession account, int revision) {
        showLoading();
        executorService.execute(() -> {
            try {
                Track track = trackRepository.getTrackDetails(trackId, account);
                if (track != null) {
                    track.setLocalFilePath(trackDownloadManager.getLocalFilePath(trackId, account));
                }
                runOnUiThread(() -> {
                    if (!acceptsResult(account, revision)) {
                        return;
                    }
                    if (track == null) {
                        showError(R.string.track_details_not_found);
                    } else {
                        currentTrack = track;
                        renderTrack();
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (acceptsResult(account, revision)) {
                        showError(R.string.track_details_load_error);
                    }
                });
            }
        });
    }

    /** Displays the loaded track and refreshes its account-specific action states. */
    private void renderTrack() {
        progressBar.setVisibility(View.GONE);
        stateTextView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        trackNameTextView.setText(valueOrFallback(
                currentTrack.getName(),
                R.string.track_details_unknown_track
        ));
        artistNameTextView.setText(valueOrFallback(
                currentTrack.getArtistName(),
                R.string.track_details_unknown_artist
        ));
        albumNameTextView.setText(valueOrFallback(
                currentTrack.getAlbumName(),
                R.string.track_details_unknown_album
        ));
        durationTextView.setText(formatDuration(currentTrack.getDuration()));
        updateStatuses();
    }

    /**
     * Updates status labels, icons and accessibility descriptions from the current track.
     * Disables action buttons while their operations are in progress.
     */
    private void updateStatuses() {
        if (currentTrack.isFavorite()) {
            favoriteButton.setIconResource(R.drawable.ic_heart_filled);
            favoriteButton.setContentDescription(getString(R.string.track_details_remove_favorite));
            favoriteStatusTextView.setText(R.string.track_details_favorite_yes);
        } else {
            favoriteButton.setIconResource(R.drawable.ic_heart_outline);
            favoriteButton.setContentDescription(getString(R.string.track_details_add_favorite));
            favoriteStatusTextView.setText(R.string.track_details_favorite_no);
        }
        favoriteButton.setEnabled(!favoriteUpdateInProgress);

        boolean downloaded = currentTrack.getLocalFilePath() != null
                && !currentTrack.getLocalFilePath().trim().isEmpty();
        downloadButton.setIconResource(downloaded
                ? R.drawable.ic_download_done
                : R.drawable.ic_download);
        downloadButton.setEnabled(!downloaded && !downloadInProgress);
        downloadButton.setContentDescription(getString(downloaded
                ? R.string.track_details_downloaded_button
                : R.string.track_details_download));
        downloadedStatusTextView.setText(downloaded
                ? R.string.track_details_downloaded_yes
                : R.string.track_details_downloaded_no);
    }

    /**
     * Plays the current account's local download, falling back to the track's streaming URL.
     * Ignores the action if the displayed session is no longer current.
     */
    private void playTrack() {
        if (currentTrack == null || !acceptsResult(displayedAccount, accountRevision())) {
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
     * Updates the captured account's favorite link without blocking the UI thread.
     * Ignores repeated clicks while a write is pending and discards stale UI callbacks.
     */
    private void toggleFavorite() {
        AccountSession account = displayedAccount;
        int revision = accountRevision();
        if (currentTrack == null || favoriteUpdateInProgress || !acceptsResult(account, revision)) {
            return;
        }

        boolean removeFavorite = currentTrack.isFavorite();
        Track track = currentTrack;
        favoriteUpdateInProgress = true;
        favoriteButton.setEnabled(false);
        executorService.execute(() -> {
            try {
                if (removeFavorite) {
                    trackRepository.removeFavorite(track.getId(), account);
                } else {
                    trackRepository.saveFavorite(track, account);
                }
                runOnUiThread(() -> {
                    if (!acceptsResult(account, revision)) { return; }
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
                    if (!acceptsResult(account, revision)) { return; }
                    favoriteUpdateInProgress = false;
                    updateStatuses();
                    Toast.makeText(
                            this,
                            R.string.track_details_favorite_error,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }

    /**
     * Starts a download for the displayed account and disables repeated requests while pending.
     * Completion updates the screen only if the captured session and revision still match.
     */
    private void downloadTrack() {
        AccountSession account = displayedAccount;
        int revision = accountRevision();
        if (currentTrack == null || downloadInProgress || !acceptsResult(account, revision)) {
            return;
        }
        downloadInProgress = true;
        Track track = currentTrack;
        downloadButton.setEnabled(false);
        executorService.execute(() -> {
            try {
                trackDownloadManager.downloadTrack(track, account);
                track.setLocalFilePath(
                        trackDownloadManager.getLocalFilePath(track.getId(), account)
                );
                runOnUiThread(() -> {
                    if (!acceptsResult(account, revision)) { return; }
                    downloadInProgress = false;
                    updateStatuses();
                    Toast.makeText(
                            this,
                            R.string.track_details_download_success,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (!acceptsResult(account, revision)) { return; }
                    downloadInProgress = false;
                    updateStatuses();
                    Toast.makeText(
                            this,
                            R.string.track_details_download_error,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }

    /** Hides track content and messages while showing the loading indicator. */
    private void showLoading() {
        contentView.setVisibility(View.GONE);
        stateTextView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Replaces the track content with an error or login-required message.
     *
     * @param messageResource string resource to display
     */
    private void showError(int messageResource) {
        contentView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        stateTextView.setText(messageResource);
        stateTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Supplies display text for optional track metadata.
     *
     * @param value metadata value, or null
     * @param fallbackResource string resource used for null or blank values
     * @return original value or the fallback text
     */
    private String valueOrFallback(String value, int fallbackResource) {
        return value == null || value.trim().isEmpty() ? getString(fallbackResource) : value;
    }

    /**
     * Formats a duration as minutes and seconds, treating negative values as zero.
     *
     * @param seconds duration in seconds
     * @return duration in m:ss format
     */
    private String formatDuration(int seconds) {
        int safeSeconds = Math.max(seconds, 0);
        return String.format(Locale.ROOT, "%d:%02d", safeSeconds / 60, safeSeconds % 60);
    }

    /** Invalidates account callbacks, releases playback and shuts down background work. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicPlayerManager != null) {
            musicPlayerManager.release();
        }
        executorService.shutdownNow();
    }
}
