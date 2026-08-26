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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.auth.LoginActivity;
import com.example.it_robota.auth.RegisterActivity;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.models.Track;
import com.example.it_robota.musicplayback.PlayerActivity;
import com.example.it_robota.tracks.TrackDetailsActivity;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText searchEditText;


    private Button searchButton;
    private Button saveFavoriteButton;
    private Button removeFavoriteButton;
    private Button showFavoritesButton;

    private ImageView trackImageView;
    private ListView tracksListView;

    private Button loginAuthButton;
    private Button registerAuthButton;
    private Button logoutAuthButton;
    private LinearLayout authButtonsLayout;
    private LinearLayout authOnlyContentLayout;

    private JamendoApiClient jamendoApiClient;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        saveFavoriteButton = findViewById(R.id.saveFavoriteButton);
        removeFavoriteButton = findViewById(R.id.removeFavoriteButton);
        showFavoritesButton = findViewById(R.id.showFavoritesButton);
        tracksListView = findViewById(R.id.tracksListView);
        logoutAuthButton = findViewById(R.id.logoutAuthButton);
        authButtonsLayout = findViewById(R.id.authButtonsLayout);
        authOnlyContentLayout = findViewById(R.id.authOnlyContentLayout);
        loginAuthButton = findViewById(R.id.loginAuthButton);
        registerAuthButton = findViewById(R.id.registerAuthButton);

        jamendoApiClient = new JamendoApiClient(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        searchButton.setOnClickListener(v -> searchTracks());


        saveFavoriteButton.setOnClickListener(v ->
                Toast.makeText(this, "Save to favorites clicked", Toast.LENGTH_SHORT).show()
        );

        removeFavoriteButton.setOnClickListener(v ->
                Toast.makeText(this, "Remove from favorites clicked", Toast.LENGTH_SHORT).show()
        );

        showFavoritesButton.setOnClickListener(v ->
                Toast.makeText(this, "Show favorites clicked", Toast.LENGTH_SHORT).show()
        );

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
                SessionManager sessionManager = new SessionManager(this);
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

    private void updateAuthButtonsVisibility() {
        SessionManager sessionManager = new SessionManager(this);
        boolean isLoggedIn = sessionManager.isLoggedIn();

        if (authButtonsLayout != null) {
            authButtonsLayout.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        }

        if (logoutAuthButton != null) {
            logoutAuthButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        }
        if (authOnlyContentLayout != null) {
            authOnlyContentLayout.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        }
    }

    private void searchTracks() {
        String query = searchEditText.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Enter track or artist name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (trackImageView != null) trackImageView.setImageDrawable(null);
        if (tracksListView != null) tracksListView.setAdapter(null);

        executorService.execute(() -> {
            try {
                List<Track> tracks = jamendoApiClient.searchTracks(query);
                mainHandler.post(() -> showSearchResults(tracks));
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSearchResults(List<Track> tracks) {
        if (tracksListView == null) return;

        if (tracks == null || tracks.isEmpty()) {
            Toast.makeText(this, "No tracks found", Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.List<String> displayItems = new java.util.ArrayList<>();
        for (Track track : tracks) {
            displayItems.add(track.getName() + " — " + track.getArtistName());
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayItems
        );

        tracksListView.setAdapter(adapter);

        tracksListView.setOnItemClickListener((parent, view, position, id) -> {
            Track selectedTrack = tracks.get(position);
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            intent.putExtra("TRACK_ID", selectedTrack.getId());
            startActivity(intent);
        });
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
