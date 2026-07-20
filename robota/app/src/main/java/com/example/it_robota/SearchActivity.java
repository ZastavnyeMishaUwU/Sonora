package com.example.it_robota;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.models.Track;
import com.example.it_robota.repositories.TrackRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for searching tracks through TrackRepository.
 * Displays search results in a simple styled list.
 */
public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private Button searchButton;
    private ListView tracksListView;
    private TextView emptyStateTextView;
    private TextView resultCountTextView;

    private TrackRepository trackRepository;
    private TrackListAdapter trackListAdapter;

    private ExecutorService executorService;
    private Handler mainHandler;

    /**
     * Initializes the search screen and connects UI actions.
     *
     * @param savedInstanceState saved activity state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        trackRepository = new TrackRepository(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupList();
        setupClickListeners();
    }

    /**
     * Finds all views used by the search screen.
     */
    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        tracksListView = findViewById(R.id.tracksListView);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        resultCountTextView = findViewById(R.id.resultCountTextView);
    }

    /**
     * Prepares the ListView adapter and item click behavior.
     */
    private void setupList() {
        trackListAdapter = new TrackListAdapter();
        tracksListView.setAdapter(trackListAdapter);

        tracksListView.setOnItemClickListener((parent, view, position, id) -> {
            Track selectedTrack = trackListAdapter.getItem(position);

            if (selectedTrack == null || selectedTrack.getId() == null || selectedTrack.getId().trim().isEmpty()) {
                Toast.makeText(this, "Track ID is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(SearchActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TRACK_ID, selectedTrack.getId());
            startActivity(intent);
        });
    }

    /**
     * Connects search button with search action.
     */
    private void setupClickListeners() {
        searchButton.setOnClickListener(v -> searchTracks());
    }

    /**
     * Reads query from input and searches tracks through TrackRepository.
     */
    private void searchTracks() {
        String query = searchEditText.getText().toString().trim();

        if (query.isEmpty()) {
            trackListAdapter.setTracks(new ArrayList<>());
            showEmptyState("Enter track or artist name.");
            resultCountTextView.setText("");
            return;
        }

        searchButton.setEnabled(false);
        searchButton.setText("Searching...");
        showEmptyState("Searching tracks...");
        resultCountTextView.setText("");

        executorService.execute(() -> {
            try {
                List<Track> tracks = trackRepository.searchTracks(query);

                mainHandler.post(() -> {
                    searchButton.setEnabled(true);
                    searchButton.setText("Search");
                    showTracks(tracks);
                });

            } catch (Exception e) {
                e.printStackTrace();

                mainHandler.post(() -> {
                    searchButton.setEnabled(true);
                    searchButton.setText("Search");
                    trackListAdapter.setTracks(new ArrayList<>());
                    showEmptyState("Search error. Check internet or try another query.");
                    resultCountTextView.setText("");
                    Toast.makeText(this, "Search error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Displays found tracks or empty-state message.
     *
     * @param tracks found tracks
     */
    private void showTracks(List<Track> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            trackListAdapter.setTracks(new ArrayList<>());
            showEmptyState("No tracks found.");
            resultCountTextView.setText("");
            return;
        }

        emptyStateTextView.setVisibility(View.GONE);
        tracksListView.setVisibility(View.VISIBLE);

        resultCountTextView.setText("Found tracks: " + tracks.size());
        trackListAdapter.setTracks(tracks);
    }

    /**
     * Shows empty-state or loading message.
     *
     * @param message message to display
     */
    private void showEmptyState(String message) {
        tracksListView.setVisibility(View.GONE);
        emptyStateTextView.setVisibility(View.VISIBLE);
        emptyStateTextView.setText(message);
    }

    /**
     * Releases background executor when activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * Adapter for rendering Track objects inside ListView.
     */
    private class TrackListAdapter extends BaseAdapter {

        private final List<Track> tracks = new ArrayList<>();

        /**
         * Updates adapter data and refreshes the list.
         *
         * @param newTracks new track list
         */
        public void setTracks(List<Track> newTracks) {
            tracks.clear();

            if (newTracks != null) {
                tracks.addAll(newTracks);
            }

            notifyDataSetChanged();
        }

        /**
         * Returns tracks count.
         *
         * @return number of tracks
         */
        @Override
        public int getCount() {
            return tracks.size();
        }

        /**
         * Returns track by position.
         *
         * @param position item position
         * @return track object
         */
        @Override
        public Track getItem(int position) {
            return tracks.get(position);
        }

        /**
         * Returns item ID.
         *
         * @param position item position
         * @return item ID
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Creates or reuses a list item view.
         *
         * @param position item position
         * @param convertView reusable view
         * @param parent parent view group
         * @return prepared item view
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;

            if (itemView == null) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_track, parent, false);
            }

            TextView trackNameTextView = itemView.findViewById(R.id.trackNameTextView);
            TextView trackArtistTextView = itemView.findViewById(R.id.trackArtistTextView);
            TextView trackIdTextView = itemView.findViewById(R.id.trackIdTextView);

            Track track = getItem(position);

            trackNameTextView.setText(getSafeText(track.getName(), "Unknown track"));
            trackArtistTextView.setText(getSafeText(track.getArtistName(), "Unknown artist"));
            trackIdTextView.setText("ID: " + getSafeText(track.getId(), "-"));

            return itemView;
        }

        /**
         * Returns fallback text if value is empty.
         *
         * @param value original value
         * @param fallback fallback value
         * @return safe text
         */
        private String getSafeText(String value, String fallback) {
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }

            return value;
        }
    }
}