package com.example.it_robota.tracks;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.example.it_robota.R;
import com.example.it_robota.auth.AuthenticationGuard;
import com.example.it_robota.auth.AccountActivity;
import com.example.it_robota.auth.AccountSession;
import com.example.it_robota.models.Track;
import com.example.it_robota.musicplayback.PlayerActivity;
import com.example.it_robota.repositories.TrackRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lists favorite tracks saved for the currently logged-in user.
 */
public class FavoritesActivity extends AccountActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<Track> favoriteTracks = new ArrayList<>();
    private TrackRepository trackRepository;
    private FavoritesAdapter favoritesAdapter;
    private ListView favoritesListView;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;

    /**
     * Requires a login and prepares the list before enabling account refreshes.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthenticationGuard.requireLoggedIn(this)) {
            return;
        }

        setContentView(R.layout.activity_favorites);

        trackRepository = new TrackRepository(this);
        favoritesListView = findViewById(R.id.lvFavorites);
        progressBar = findViewById(R.id.favoritesProgress);
        emptyStateTextView = findViewById(R.id.tvFavoritesEmptyState);
        favoritesAdapter = new FavoritesAdapter();
        favoritesListView.setAdapter(favoritesAdapter);

        findViewById(R.id.btnBackFavorites).setOnClickListener(view -> finish());
        accountUiReady();
    }

    /** Clears the visible favorites without removing saved links from the database. */
    @Override
    protected void clearAccountContent() {
        favoriteTracks.clear();
        favoritesAdapter.notifyDataSetChanged();
        favoritesListView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    /**
     * Loads favorites in the background and ignores results from an older session or revision.
     *
     * @param account session to display, or null to show the login-required message
     * @param revision content revision captured for this load
     */
    @Override
    protected void loadAccountContent(AccountSession account, int revision) {
        if (account == null) {
            showEmptyState(R.string.favorites_login_required);
            return;
        }

        showLoading();
        executorService.execute(() -> {
            try {
                List<Track> tracks = trackRepository.getSavedTracks(account);
                runOnUiThread(() -> {
                    if (acceptsResult(account, revision)) { showTracks(tracks); }
                });
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    if (acceptsResult(account, revision)) { showEmptyState(R.string.favorites_load_error); }
                });
            }
        });
    }

    /**
     * Replaces the visible list on the UI thread after the caller has checked the session.
     *
     * @param tracks favorites to display
     */
    private void showTracks(List<Track> tracks) {
        favoriteTracks.clear();
        favoriteTracks.addAll(tracks);
        favoritesAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        favoritesListView.setVisibility(tracks.isEmpty() ? View.GONE : View.VISIBLE);
        emptyStateTextView.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.GONE);
        emptyStateTextView.setText(R.string.favorites_empty_state);
    }

    /**
     * Removes a favorite for the displayed account in the background.
     * The result is applied only while the captured session and revision still match.
     *
     * @param track favorite selected for removal
     */
    private void removeFavorite(Track track) {
        AccountSession account = displayedAccount;
        int revision = accountRevision();
        if (!acceptsResult(account, revision)) { return; }
        executorService.execute(() -> {
            try {
                trackRepository.removeFavorite(track.getId(), account);
                runOnUiThread(() -> {
                    if (!acceptsResult(account, revision)) { return; }
                    favoriteTracks.remove(track);
                    favoritesAdapter.notifyDataSetChanged();
                    if (favoriteTracks.isEmpty()) {
                        showEmptyState(R.string.favorites_empty_state);
                    }
                    Toast.makeText(this, R.string.favorites_removed, Toast.LENGTH_SHORT).show();
                });
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    if (acceptsResult(account, revision)) {
                        Toast.makeText(this, R.string.favorites_remove_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Opens the selected track in the player if the displayed account is still current.
     *
     * @param track selected favorite; missing tracks or IDs are ignored
     */
    private void openPlayer(Track track) {
        if (!acceptsResult(displayedAccount, accountRevision())
                || track == null || track.getId() == null || track.getId().trim().isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_TRACK_ID, track.getId());
        startActivity(intent);
    }

    /** Shows the loading indicator and hides the previous list and state message. */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        favoritesListView.setVisibility(View.GONE);
        emptyStateTextView.setVisibility(View.GONE);
    }

    /**
     * Clears the visible list and shows an empty, error or login-required message.
     *
     * @param messageResource string resource to display
     */
    private void showEmptyState(int messageResource) {
        favoriteTracks.clear();
        favoritesAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        favoritesListView.setVisibility(View.GONE);
        emptyStateTextView.setText(messageResource);
        emptyStateTextView.setVisibility(View.VISIBLE);
    }

    /** Invalidates account callbacks and shuts down the screen's background executor. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    private class FavoritesAdapter extends BaseAdapter {

        /** @return number of favorites in the visible list */
        @Override
        public int getCount() {
            return favoriteTracks.size();
        }

        /**
         * Returns a favorite at the given list position.
         *
         * @param position zero-based list position
         * @return selected favorite
         */
        @Override
        public Track getItem(int position) {
            return favoriteTracks.get(position);
        }

        /**
         * Uses the current list position as the row identifier.
         *
         * @param position zero-based list position
         * @return row identifier
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Creates or reuses a row and binds its metadata, playback and removal actions.
         *
         * @param position zero-based list position
         * @param convertView reusable row, or null
         * @param parent parent list
         * @return populated favorite row
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = LayoutInflater.from(FavoritesActivity.this)
                        .inflate(R.layout.item_favorite_track, parent, false);
            }

            Track track = getItem(position);
            TextView nameTextView = itemView.findViewById(R.id.tvFavoriteTrackName);
            TextView artistTextView = itemView.findViewById(R.id.tvFavoriteTrackArtist);
            Button removeButton = itemView.findViewById(R.id.btnRemoveFavorite);
            nameTextView.setText(valueOrFallback(track.getName(), R.string.favorites_unknown_track));
            artistTextView.setText(valueOrFallback(
                    track.getArtistName(),
                    R.string.favorites_unknown_artist
            ));
            itemView.setOnClickListener(view -> openPlayer(track));
            removeButton.setOnClickListener(view -> removeFavorite(track));
            return itemView;
        }
    }

    /**
     * Supplies display text when optional track metadata is missing.
     *
     * @param value metadata value, or null
     * @param fallbackResource string resource used for null or blank values
     * @return original value or the fallback text
     */
    private String valueOrFallback(String value, int fallbackResource) {
        return value == null || value.trim().isEmpty() ? getString(fallbackResource) : value;
    }
}
