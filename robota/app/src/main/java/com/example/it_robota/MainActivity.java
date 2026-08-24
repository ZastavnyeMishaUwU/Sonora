package com.example.it_robota;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.auth.LoginActivity;
import com.example.it_robota.auth.RegisterActivity;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.models.Track;
import com.example.it_robota.musicplayback.PlayerActivity;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText searchEditText;
    private EditText trackIdEditText;

    private Button searchButton;
    private Button detailsButton;
    private Button saveFavoriteButton;
    private Button removeFavoriteButton;
    private Button showFavoritesButton;

    private ImageView trackImageView;
    private TextView resultTextView;

    private Button loginAuthButton;
    private Button registerAuthButton;
    private Button logoutAuthButton;
    private LinearLayout authButtonsLayout;
    private LinearLayout authOnlyContentLayout;

    private JamendoApiClient jamendoApiClient;
    private ExecutorService executorService;
    private Handler mainHandler;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        searchEditText = findViewById(R.id.searchEditText);
        trackIdEditText = findViewById(R.id.trackIdEditText);
        searchButton = findViewById(R.id.searchButton);
        detailsButton = findViewById(R.id.detailsButton);
        saveFavoriteButton = findViewById(R.id.saveFavoriteButton);
        removeFavoriteButton = findViewById(R.id.removeFavoriteButton);
        showFavoritesButton = findViewById(R.id.showFavoritesButton);
        trackImageView = findViewById(R.id.trackImageView);
        resultTextView = findViewById(R.id.resultTextView);

        logoutAuthButton = findViewById(R.id.logoutAuthButton);
        authButtonsLayout = findViewById(R.id.authButtonsLayout);
        authOnlyContentLayout = findViewById(R.id.authOnlyContentLayout);
        loginAuthButton = findViewById(R.id.loginAuthButton);
        registerAuthButton = findViewById(R.id.registerAuthButton);

        jamendoApiClient = new JamendoApiClient(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        if (searchButton != null) {
            searchButton.setOnClickListener(v -> searchTracks());
        }

        if (detailsButton != null) {
            detailsButton.setOnClickListener(v -> {
                if (trackIdEditText == null) return;
                String trackId = trackIdEditText.getText().toString().trim();

                if (trackId.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Введіть ID треку", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("TRACK_ID", trackId);
                startActivity(intent);
            });
        }

        if (saveFavoriteButton != null) {
            saveFavoriteButton.setOnClickListener(v -> {
                if (!checkLoggedIn()) return;
                Toast.makeText(this, "Save to favorites clicked", Toast.LENGTH_SHORT).show();
            });
        }

        if (removeFavoriteButton != null) {
            removeFavoriteButton.setOnClickListener(v -> {
                if (!checkLoggedIn()) return;
                Toast.makeText(this, "Remove from favorites clicked", Toast.LENGTH_SHORT).show();
            });
        }

        if (showFavoritesButton != null) {
            showFavoritesButton.setOnClickListener(v -> {
                if (!checkLoggedIn()) return;
                Toast.makeText(this, "Show favorites clicked", Toast.LENGTH_SHORT).show();
            });
        }

        if (loginAuthButton != null) {
            loginAuthButton.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, LoginActivity.class))
            );
        }

        if (registerAuthButton != null) {
            registerAuthButton.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, RegisterActivity.class))
            );
        }

        if (logoutAuthButton != null) {
            logoutAuthButton.setOnClickListener(v -> {
                sessionManager.clearSession();
                updateAuthButtonsVisibility();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthButtonsVisibility();
    }

    private boolean checkLoggedIn() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in to use favorites", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return false;
        }
        return true;
    }

    private void updateAuthButtonsVisibility() {
        boolean isLoggedIn = sessionManager.isLoggedIn();

        if (authButtonsLayout != null) {
            authButtonsLayout.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        }

        if (logoutAuthButton != null) {
            logoutAuthButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        }

        if (authOnlyContentLayout != null) {
            authOnlyContentLayout.setVisibility(View.VISIBLE);
        }
    }

    private void searchTracks() {
        if (searchEditText == null) return;
        String query = searchEditText.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Enter track or artist name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (trackImageView != null) trackImageView.setImageDrawable(null);
        if (resultTextView != null) {
            resultTextView.setVisibility(View.VISIBLE);
            resultTextView.setText("Searching tracks...");
        }

        executorService.execute(() -> {
            try {
                List<Track> tracks = jamendoApiClient.searchTracks(query);
                mainHandler.post(() -> showSearchResults(tracks));
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (resultTextView != null) {
                        resultTextView.setVisibility(View.VISIBLE);
                        resultTextView.setText("Error: " + e.getMessage());
                    }
                    Toast.makeText(this, "Search error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSearchResults(List<Track> tracks) {
        if (resultTextView == null) return;

        resultTextView.setVisibility(View.VISIBLE);

        if (tracks == null || tracks.isEmpty()) {
            resultTextView.setText("No tracks found.");
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("Found tracks: ").append(tracks.size()).append("\n\n");

        for (Track track : tracks) {
            result.append("ID: ").append(track.getId()).append("\n");
            result.append("Title: ").append(track.getName()).append("\n");
            result.append("Artist: ").append(track.getArtistName()).append("\n");
            result.append("Album: ").append(track.getAlbumName()).append("\n");
            result.append("Duration: ").append(track.getDuration()).append(" sec\n\n");
        }

        result.append("Copy any ID and paste it into the details field.");
        resultTextView.setText(result.toString());
    }

    private void showTrackDetails(Track track) {
        if (resultTextView == null) return;

        resultTextView.setVisibility(View.VISIBLE);

        if (track == null) {
            resultTextView.setText("Track details are empty.");
            return;
        }

        boolean isDownloaded = track.getLocalFilePath() != null && !track.getLocalFilePath().isEmpty();
        loadTrackImage(track.getImageUrl());

        String result =
                "Track Details\n\n" +
                        "ID: " + track.getId() + "\n" +
                        "Title: " + track.getName() + "\n" +
                        "Artist: " + track.getArtistName() + "\n" +
                        "Album: " + track.getAlbumName() + "\n" +
                        "Duration: " + track.getDuration() + " sec\n\n" +
                        "Favorite: " + track.isFavorite() + "\n" +
                        "Downloaded: " + isDownloaded;

        resultTextView.setText(result);
    }

    private void loadTrackImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return;

        executorService.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                InputStream inputStream = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                mainHandler.post(() -> {
                    if (trackImageView != null) trackImageView.setImageBitmap(bitmap);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}