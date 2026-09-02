package com.example.it_robota;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.auth.LoginActivity;
import com.example.it_robota.auth.RegisterActivity;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.auth.SettingsActivity;
import com.example.it_robota.models.Track;
import com.example.it_robota.musicplayback.PlayerActivity;
import com.example.it_robota.repositories.TrackRepository;
import com.example.it_robota.tracks.DownloadedTracksActivity;
import com.example.it_robota.tracks.FavoritesActivity;
import com.example.it_robota.tracks.SearchActivity;
import com.example.it_robota.tracks.TrackDetailsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Home screen for authenticated navigation and track search.
 */
public class MainActivity extends AppCompatActivity {

    private EditText searchEditText;
    private Button searchButton;
    private View searchCard;
    private Button showDownloadedTracksButton;
    private Button profileNavButton;
    private Button searchNavButton;
    private Button favoritesNavButton;
    private View bottomNavigationBar;
    private ListView tracksListView;
    private Button loginAuthButton;
    private Button registerAuthButton;
    private Button logoutAuthButton;
    private LinearLayout authButtonsLayout;
    private LinearLayout authOnlyContentLayout;
    private SessionManager sessionManager;
    private TrackRepository trackRepository;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean trackDetailsOpening;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        searchCard = findViewById(R.id.cardSearch);
        showDownloadedTracksButton = findViewById(R.id.showDownloadedTracksButton);
        profileNavButton = findViewById(R.id.profileNavButton);
        searchNavButton = findViewById(R.id.searchNavButton);
        favoritesNavButton = findViewById(R.id.favoritesNavButton);
        bottomNavigationBar = findViewById(R.id.bottomNavigationBar);
        tracksListView = findViewById(R.id.tracksListView);
        logoutAuthButton = findViewById(R.id.logoutAuthButton);
        authButtonsLayout = findViewById(R.id.authButtonsLayout);
        authOnlyContentLayout = findViewById(R.id.authOnlyContentLayout);
        loginAuthButton = findViewById(R.id.loginAuthButton);
        registerAuthButton = findViewById(R.id.registerAuthButton);

        sessionManager = new SessionManager(this);
        trackRepository = new TrackRepository(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        searchButton.setOnClickListener(view -> searchTracks());
        showDownloadedTracksButton.setOnClickListener(view ->
                startActivity(new Intent(this, DownloadedTracksActivity.class))
        );
        profileNavButton.setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class))
        );
        searchNavButton.setOnClickListener(view ->
                startActivity(new Intent(this, SearchActivity.class))
        );
        favoritesNavButton.setOnClickListener(view -> openFavorites());
        loginAuthButton.setOnClickListener(view ->
                startActivity(new Intent(this, LoginActivity.class))
        );
        registerAuthButton.setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
        logoutAuthButton.setOnClickListener(view -> {
            sessionManager.clearSession();
            updateAuthButtonsVisibility();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        trackDetailsOpening = false;
        tracksListView.setEnabled(true);
        updateAuthButtonsVisibility();
    }

    private void updateAuthButtonsVisibility() {
        boolean isLoggedIn = sessionManager.isLoggedIn();
        authButtonsLayout.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        logoutAuthButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        searchCard.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        searchButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        authOnlyContentLayout.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        bottomNavigationBar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
    }

    private void openFavorites() {
        startActivity(new Intent(this, FavoritesActivity.class));
    }

    private void searchTracks() {
        if (!sessionManager.isLoggedIn()) {
            tracksListView.setAdapter(null);
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        String query = searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter track or artist name", Toast.LENGTH_SHORT).show();
            return;
        }

        tracksListView.setAdapter(null);
        executorService.execute(() -> {
            try {
                List<Track> tracks = trackRepository.searchTracks(query);
                mainHandler.post(() -> showSearchResults(tracks));
            } catch (Exception exception) {
                mainHandler.post(() -> Toast.makeText(
                        this,
                        "Error: " + exception.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
            }
        });
    }

    private void showSearchResults(List<Track> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            Toast.makeText(this, "No tracks found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> displayItems = new ArrayList<>();
        for (Track track : tracks) {
            String title = track.getName();
            displayItems.add(title == null || title.trim().isEmpty()
                    ? getString(R.string.search_unknown_track)
                    : title);
        }

        tracksListView.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayItems
        ));

        tracksListView.setOnItemClickListener((parent, view, position, id) -> {
            if (trackDetailsOpening) {
                return;
            }
            Track selectedTrack = tracks.get(position);
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra(PlayerActivity.EXTRA_TRACK_ID, selectedTrack.getId());
            startActivity(intent);
        });

        tracksListView.setOnItemLongClickListener((parent, view, position, id) -> {
            openTrackDetails(tracks.get(position));
            return true;
        });
    }

    /** 
    * Prevents queued long-clicks from opening duplicate details screens. 
    */
    private void openTrackDetails(Track track) {
        if (trackDetailsOpening || isFinishing() || isDestroyed()
                || !sessionManager.isLoggedIn()
                || track == null || track.getId() == null || track.getId().trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(this, TrackDetailsActivity.class);
        intent.putExtra(TrackDetailsActivity.EXTRA_TRACK_ID, track.getId());
        trackDetailsOpening = true;
        tracksListView.setEnabled(false);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException exception) {
            trackDetailsOpening = false;
            tracksListView.setEnabled(true);
            Toast.makeText(this, R.string.track_details_open_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}
