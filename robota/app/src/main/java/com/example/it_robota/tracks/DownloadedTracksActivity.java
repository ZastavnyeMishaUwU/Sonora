package com.example.it_robota.tracks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.it_robota.R;
import com.example.it_robota.auth.AuthenticationGuard;
import com.example.it_robota.auth.AccountActivity;
import com.example.it_robota.auth.AccountSession;
import com.example.it_robota.downloader.TrackDownloadManager;
import com.example.it_robota.database.DownloadedTrackEntity;
import com.example.it_robota.musicplayback.MusicPlayerManager;
import com.example.it_robota.storage.LocalFileStorageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Displays tracks downloaded by the current user and plays their local audio files.
 */
public class DownloadedTracksActivity extends AccountActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<DownloadedTrackEntity> downloadedTracks = new ArrayList<>();

    private TrackDownloadManager downloadManager;
    private LocalFileStorageManager localFileStorageManager;
    private MusicPlayerManager musicPlayerManager;
    private DownloadedTrackAdapter downloadedTrackAdapter;
    private TextView emptyStateTextView;
    private AlertDialog removalDialog;

    /**
     * Initializes dependencies, list rendering and local playback behavior.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthenticationGuard.requireLoggedIn(this)) {
            return;
        }

        setContentView(R.layout.activity_downloaded_tracks);

        downloadManager = new TrackDownloadManager(this);
        localFileStorageManager = new LocalFileStorageManager(this);
        musicPlayerManager = MusicPlayerManager.getInstance();

        bindList();
        accountUiReady();
    }

    /**
     * Clears private rows, playback and any removal dialog when the account changes or the screen stops.
     * Saved download records and files are left untouched.
     */
    @Override
    protected void clearAccountContent() {
        downloadedTracks.clear();
        downloadedTrackAdapter.notifyDataSetChanged();
        musicPlayerManager.stop();
        if (removalDialog != null) { removalDialog.dismiss(); removalDialog = null; }
    }

    /**
     * Connects the downloaded-track list to its adapter and click action.
     */
    private void bindList() {
        ListView downloadedTracksListView = findViewById(R.id.downloadedTracksListView);
        emptyStateTextView = findViewById(R.id.downloadedTracksEmptyState);
        downloadedTrackAdapter = new DownloadedTrackAdapter();

        downloadedTracksListView.setEmptyView(emptyStateTextView);
        downloadedTracksListView.setAdapter(downloadedTrackAdapter);
        downloadedTracksListView.setOnItemClickListener((parent, view, position, id) ->
                playDownloadedTrack(downloadedTracks.get(position))
        );
    }

    /**
     * Reads downloads in the background for the captured session.
     *
     * @param account session to display, or null to show the login-required message
     * @param revision content revision captured for this load
     */
    @Override
    protected void loadAccountContent(AccountSession account, int revision) {
        emptyStateTextView.setText(R.string.downloaded_tracks_loading);
        downloadedTracks.clear();
        downloadedTrackAdapter.notifyDataSetChanged();
        if (account == null) {
            emptyStateTextView.setText(R.string.downloaded_tracks_login_required);
            return;
        }

        executorService.execute(() -> {
            try {
                List<DownloadedTrackEntity> tracks = downloadManager.getDownloadedTracks(account);
                showTracks(tracks, R.string.downloaded_tracks_empty, account, revision);
            } catch (Exception exception) {
                showTracks(new ArrayList<>(), R.string.downloaded_tracks_load_error, account, revision);
            }
        });
    }

    /**
     * Updates the visible list and its empty-state message on the UI thread.
     *
     * @param tracks downloaded tracks to display
     * @param emptyMessageResource message shown when the supplied list is empty
     * @param account session that owns the result
     * @param revision revision to check before updating the list
     */
    private void showTracks(List<DownloadedTrackEntity> tracks, int emptyMessageResource,
                            AccountSession account, int revision) {
        runOnUiThread(() -> {
            if (!acceptsResult(account, revision)) {
                return;
            }

            downloadedTracks.clear();
            if (tracks != null) {
                downloadedTracks.addAll(tracks);
            }
            emptyStateTextView.setText(emptyMessageResource);
            downloadedTrackAdapter.notifyDataSetChanged();
        });
    }

    /**
     * Checks ownership and file availability before starting offline playback.
     * A session or revision change prevents the background lookup from starting playback.
     *
     * @param downloadedTrack selected downloaded-track record
     */
    private void playDownloadedTrack(DownloadedTrackEntity downloadedTrack) {
        AccountSession account = displayedAccount;
        int revision = accountRevision();
        if (!ownsTrack(downloadedTrack, account) || !acceptsResult(account, revision)) { return; }
        executorService.execute(() -> {
            String localPath;
            try {
                localPath = downloadManager.getLocalFilePath(downloadedTrack.getTrackId(), account);
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    if (acceptsResult(account, revision)) {
                        Toast.makeText(this, R.string.downloaded_tracks_load_error, Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            boolean fileAvailable = localFileStorageManager.fileExists(localPath);

            runOnUiThread(() -> {
                if (!acceptsResult(account, revision)) {
                    return;
                }

                if (!fileAvailable) {
                    Toast.makeText(
                            this,
                            R.string.downloaded_tracks_file_missing,
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                musicPlayerManager.play(localPath);
                Toast.makeText(
                        this,
                        R.string.downloaded_tracks_playing,
                        Toast.LENGTH_SHORT
                ).show();
            });
        });
    }

    /**
     * Asks for removal confirmation while retaining the selected account and revision.
     * The dialog is dismissed when account content is cleared.
     *
     * @param downloadedTrack track selected for removal
     */
    private void confirmTrackRemoval(DownloadedTrackEntity downloadedTrack) {
        AccountSession account = displayedAccount;
        int revision = accountRevision();
        if (!ownsTrack(downloadedTrack, account) || !acceptsResult(account, revision)) { return; }
        String trackName = valueOrFallback(
                downloadedTrack.getTrackName(),
                getString(R.string.downloaded_tracks_unknown_track, downloadedTrack.getTrackId())
        );

        removalDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.downloaded_tracks_remove_title)
                .setMessage(getString(R.string.downloaded_tracks_remove_message, trackName))
                .setNegativeButton(R.string.downloaded_tracks_remove_cancel, null)
                .setPositiveButton(
                        R.string.downloaded_tracks_remove_confirm,
                        (dialog, which) -> removeDownloadedTrack(downloadedTrack, account, revision)
                )
                .show();
    }

    /**
     * Removes the captured account's download in the background without affecting other owners.
     *
     * @param downloadedTrack track selected for removal
     * @param account session captured when the confirmation dialog was opened
     * @param revision content revision captured with the session
     */
    private void removeDownloadedTrack(DownloadedTrackEntity downloadedTrack, AccountSession account, int revision) {
        if (!acceptsResult(account, revision)) { return; }
        executorService.execute(() -> {
            try {
                downloadManager.removeDownload(downloadedTrack.getTrackId(), account);
                showRemovalSuccess(downloadedTrack, account, revision);
            } catch (Exception exception) {
                showRemovalError(account, revision);
            }
        });
    }

    /**
     * Removes a deleted download from the visible list if its session and revision still match.
     *
     * @param downloadedTrack track whose account-owned download was removed
     * @param account session that requested removal
     * @param revision content revision captured before removal
     */
    private void showRemovalSuccess(DownloadedTrackEntity downloadedTrack, AccountSession account, int revision) {
        runOnUiThread(() -> {
            if (!acceptsResult(account, revision)) {
                return;
            }

            downloadedTracks.remove(downloadedTrack);
            emptyStateTextView.setText(R.string.downloaded_tracks_empty);
            downloadedTrackAdapter.notifyDataSetChanged();
            Toast.makeText(
                    this,
                    R.string.downloaded_tracks_remove_success,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    /**
     * Displays a non-fatal message when a track cannot be fully removed.
     *
     * @param account session that requested removal
     * @param revision revision to check before displaying the message
     */
    private void showRemovalError(AccountSession account, int revision) {
        runOnUiThread(() -> {
            if (!acceptsResult(account, revision)) {
                return;
            }

            Toast.makeText(
                    this,
                    R.string.downloaded_tracks_remove_error,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    /**
     * Compares a download's stored owner with a captured account.
     * This does not check whether that session is still current.
     *
     * @param track non-null downloaded-track record
     * @param account account to compare, or null
     * @return true when both user ID and owner email match
     */
    private boolean ownsTrack(DownloadedTrackEntity track, AccountSession account) {
        return account != null && track.getUserId() == account.getUserId()
                && track.getOwnerEmail().equals(account.getEmail());
    }

    /**
     * Returns display-safe text for optional downloaded-track metadata.
     *
     * @param value stored metadata value
     * @param fallback fallback text
     * @return stored value or fallback when the value is missing
     */
    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    /**
     * Releases playback and background-thread resources when the screen is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicPlayerManager != null) {
            musicPlayerManager.release();
        }
        executorService.shutdownNow();
    }

    /**
     * Adapts downloaded-track records into name-and-artist list rows.
     */
    private class DownloadedTrackAdapter extends BaseAdapter {

        /**
         * Returns the number of downloaded tracks currently displayed.
         *
         * @return downloaded-track count
         */
        @Override
        public int getCount() {
            return downloadedTracks.size();
        }

        /**
         * Returns the downloaded-track record at a list position.
         *
         * @param position list position
         * @return downloaded-track record
         */
        @Override
        public DownloadedTrackEntity getItem(int position) {
            return downloadedTracks.get(position);
        }

        /**
         * Uses the current list position as the row identifier.
         *
         * @param position list position
         * @return list position as the row identifier
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Creates or reuses a row and binds its track and artist names.
         *
         * @param position list position
         * @param convertView reusable row view, or null
         * @param parent parent list
         * @return populated row view
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_downloaded_track,
                        parent,
                        false
                );
            }

            DownloadedTrackEntity track = getItem(position);
            TextView trackNameTextView = row.findViewById(R.id.downloadedTrackName);
            TextView artistNameTextView = row.findViewById(R.id.downloadedTrackArtist);
            Button removeButton = row.findViewById(R.id.removeDownloadedTrackButton);

            trackNameTextView.setText(valueOrFallback(
                    track.getTrackName(),
                    getString(R.string.downloaded_tracks_unknown_track, track.getTrackId())
            ));
            artistNameTextView.setText(valueOrFallback(
                    track.getArtistName(),
                    getString(R.string.downloaded_tracks_unknown_artist)
            ));
            removeButton.setOnClickListener(view -> confirmTrackRemoval(track));

            return row;
        }
    }
}
