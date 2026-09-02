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

    @Override
    protected void clearAccountContent() {
        favoriteTracks.clear();
        favoritesAdapter.notifyDataSetChanged();
        favoritesListView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    /**
     * Loads favorites for the current user outside the UI thread.
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
     * Removes a track from the current user's favorites in the background.
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

    private void openPlayer(Track track) {
        if (!acceptsResult(displayedAccount, accountRevision())
                || track == null || track.getId() == null || track.getId().trim().isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_TRACK_ID, track.getId());
        startActivity(intent);
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        favoritesListView.setVisibility(View.GONE);
        emptyStateTextView.setVisibility(View.GONE);
    }

    private void showEmptyState(int messageResource) {
        favoriteTracks.clear();
        favoritesAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        favoritesListView.setVisibility(View.GONE);
        emptyStateTextView.setText(messageResource);
        emptyStateTextView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    private class FavoritesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return favoriteTracks.size();
        }

        @Override
        public Track getItem(int position) {
            return favoriteTracks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

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

    private String valueOrFallback(String value, int fallbackResource) {
        return value == null || value.trim().isEmpty() ? getString(fallbackResource) : value;
    }
}
