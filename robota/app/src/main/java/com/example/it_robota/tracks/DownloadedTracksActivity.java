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
import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.R;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.DownloadedTrackDao;
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
public class DownloadedTracksActivity extends AppCompatActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<DownloadedTrackEntity> downloadedTracks = new ArrayList<>();

    private DownloadedTrackDao downloadedTrackDao;
    private SessionManager sessionManager;
    private LocalFileStorageManager localFileStorageManager;
    private MusicPlayerManager musicPlayerManager;
    private DownloadedTrackAdapter downloadedTrackAdapter;
    private TextView emptyStateTextView;

    /**
     * Initializes dependencies, list rendering and local playback behavior.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded_tracks);

        downloadedTrackDao = AppDatabase.getInstance(this).downloadedTrackDao();
        sessionManager = new SessionManager(this);
        localFileStorageManager = new LocalFileStorageManager(this);
        musicPlayerManager = new MusicPlayerManager();

        bindList();
    }

    /**
     * Reloads downloads whenever the screen becomes visible.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadDownloadedTracks();
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
     * Reads downloaded tracks for the active user outside the UI thread.
     */
    private void loadDownloadedTracks() {
        emptyStateTextView.setText(R.string.downloaded_tracks_loading);
        downloadedTracks.clear();
        downloadedTrackAdapter.notifyDataSetChanged();

        executorService.execute(() -> {
            try {
                long userId = sessionManager.getCurrentUserId();
                if (userId < 0) {
                    showTracks(new ArrayList<>(), R.string.downloaded_tracks_login_required);
                    return;
                }

                List<DownloadedTrackEntity> tracks = downloadedTrackDao.getDownloadedTracks(userId);
                showTracks(tracks, R.string.downloaded_tracks_empty);
            } catch (Exception exception) {
                showTracks(new ArrayList<>(), R.string.downloaded_tracks_load_error);
            }
        });
    }

    /**
     * Updates the visible list and its empty-state message on the UI thread.
     *
     * @param tracks downloaded tracks to display
     * @param emptyMessageResource message shown when the supplied list is empty
     */
    private void showTracks(List<DownloadedTrackEntity> tracks, int emptyMessageResource) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
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
     * Checks the stored local path and starts playback without a network request.
     *
     * @param downloadedTrack selected downloaded-track record
     */
    private void playDownloadedTrack(DownloadedTrackEntity downloadedTrack) {
        executorService.execute(() -> {
            String localPath = downloadedTrack.getLocalPath();
            boolean fileAvailable = localFileStorageManager.fileExists(localPath);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
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
     * Asks the user to confirm removal of a downloaded track.
     *
     * @param downloadedTrack track selected for removal
     */
    private void confirmTrackRemoval(DownloadedTrackEntity downloadedTrack) {
        String trackName = valueOrFallback(
                downloadedTrack.getTrackName(),
                getString(R.string.downloaded_tracks_unknown_track, downloadedTrack.getTrackId())
        );

        new AlertDialog.Builder(this)
                .setTitle(R.string.downloaded_tracks_remove_title)
                .setMessage(getString(R.string.downloaded_tracks_remove_message, trackName))
                .setNegativeButton(R.string.downloaded_tracks_remove_cancel, null)
                .setPositiveButton(
                        R.string.downloaded_tracks_remove_confirm,
                        (dialog, which) -> removeDownloadedTrack(downloadedTrack)
                )
                .show();
    }

    /**
     * Deletes the local file and then removes its Room record in the background.
     * A missing local file is treated as an already completed file deletion.
     *
     * @param downloadedTrack track selected for removal
     */
    private void removeDownloadedTrack(DownloadedTrackEntity downloadedTrack) {
        executorService.execute(() -> {
            try {
                boolean fileRemoved = localFileStorageManager.deleteFile(
                        downloadedTrack.getLocalPath()
                );
                if (!fileRemoved) {
                    showRemovalError();
                    return;
                }

                downloadedTrackDao.deleteDownloadedTrack(
                        downloadedTrack.getTrackId(),
                        downloadedTrack.getUserId()
                );
                showRemovalSuccess(downloadedTrack);
            } catch (Exception exception) {
                showRemovalError();
            }
        });
    }

    /**
     * Removes a successfully deleted track from the visible list immediately.
     *
     * @param downloadedTrack track whose file and database record were deleted
     */
    private void showRemovalSuccess(DownloadedTrackEntity downloadedTrack) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
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
     */
    private void showRemovalError() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
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
        musicPlayerManager.release();
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
         * Returns a stable numeric identifier for a list position.
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
