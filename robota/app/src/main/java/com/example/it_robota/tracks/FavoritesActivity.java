package com.example.it_robota.tracks;

import android.content.ActivityNotFoundException;
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

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.R;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.models.Track;
import com.example.it_robota.repositories.TrackRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lists favorite tracks saved for the currently logged-in user.
 */
public class FavoritesActivity extends AppCompatActivity {

    private static final String TRACK_DETAILS_CLASS = "com.example.it_robota.tracks.TrackDetailsActivity";
    private static final String EXTRA_TRACK_ID = "trackId";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<Track> favoriteTracks = new ArrayList<>();
    private TrackRepository trackRepository;
    private SessionManager sessionManager;
    private FavoritesAdapter favoritesAdapter;
    private ListView favoritesListView;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;

    /**
     * Initializes dependencies, view bindings and list interactions.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        trackRepository = new TrackRepository(this);
        sessionManager = new SessionManager(this);
        favoritesListView = findViewById(R.id.lvFavorites);
        progressBar = findViewById(R.id.favoritesProgress);
        emptyStateTextView = findViewById(R.id.tvFavoritesEmptyState);
        favoritesAdapter = new FavoritesAdapter();
        favoritesListView.setAdapter(favoritesAdapter);
        favoritesListView.setOnItemClickListener((parent, view, position, id) -> {
            openTrackDetails(favoriteTracks.get(position));
        });
    }

    /**
     * Reloads favorites whenever the screen becomes active.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    /**
     * Loads favorites for the current user outside the UI thread.
     */
    private void loadFavorites() {
        if (!sessionManager.isLoggedIn()) {
            showEmptyState(R.string.favorites_login_required);
            return;
        }

        showLoading();
        executorService.execute(() -> {
            List<Track> tracks = trackRepository.getSavedTracks();
            runOnUiThread(() -> {
                favoriteTracks.clear();
                favoriteTracks.addAll(tracks);
                favoritesAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                favoritesListView.setVisibility(tracks.isEmpty() ? View.GONE : View.VISIBLE);
                emptyStateTextView.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.GONE);
                emptyStateTextView.setText(R.string.favorites_empty_state);
            });
        });
    }

    /**
     * Removes a track from the current user's favorites and refreshes the list.
     *
     * @param track favorite track to remove
     */
    private void removeFavorite(Track track) {
        executorService.execute(() -> {
            trackRepository.removeFavorite(track.getId());
            runOnUiThread(() -> {
                favoriteTracks.remove(track);
                favoritesAdapter.notifyDataSetChanged();
                if (favoriteTracks.isEmpty()) {
                    showEmptyState(R.string.favorites_empty_state);
                }
                Toast.makeText(this, R.string.favorites_removed, Toast.LENGTH_SHORT).show();
            });
        });
    }

    /**
     * Opens the details screen for a selected track when that screen is available.
     *
     * @param track selected favorite track
     */
    private void openTrackDetails(Track track) {
        Intent intent = new Intent();
        intent.setClassName(this, TRACK_DETAILS_CLASS);
        intent.putExtra(EXTRA_TRACK_ID, track.getId());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.favorites_details_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows the loading indicator while hiding list content and messages.
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        favoritesListView.setVisibility(View.GONE);
        emptyStateTextView.setVisibility(View.GONE);
    }

    /**
     * Shows a message in place of the favorites list.
     *
     * @param messageResource string resource to display
     */
    private void showEmptyState(int messageResource) {
        progressBar.setVisibility(View.GONE);
        favoritesListView.setVisibility(View.GONE);
        emptyStateTextView.setText(messageResource);
        emptyStateTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Stops pending background work when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    /**
     * Binds favorite tracks and their remove actions to ListView rows.
     */
    private class FavoritesAdapter extends BaseAdapter {

        /**
         * Returns the number of favorite tracks currently displayed.
         *
         * @return favorite track count
         */
        @Override
        public int getCount() {
            return favoriteTracks.size();
        }

        /**
         * Returns the favorite track at a list position.
         *
         * @param position item position
         * @return favorite track at the position
         */
        @Override
        public Track getItem(int position) {
            return favoriteTracks.get(position);
        }

        /**
         * Returns a stable list identifier for an item position.
         *
         * @param position item position
         * @return item identifier
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Creates or reuses a list row and binds its track data and remove action.
         *
         * @param position item position
         * @param convertView reusable row view, or null
         * @param parent parent list view
         * @return bound favorite-track row
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
            artistTextView.setText(valueOrFallback(track.getArtistName(), R.string.favorites_unknown_artist));
            removeButton.setOnClickListener(view -> removeFavorite(track));
            return itemView;
        }
    }

    /**
     * Returns a display-safe value for optional track text.
     *
     * @param value track text value
     * @param fallbackResource string resource used when the value is missing
     * @return original value or localized fallback text
     */
    private String valueOrFallback(String value, int fallbackResource) {
        return value == null || value.trim().isEmpty() ? getString(fallbackResource) : value;
    }
}
