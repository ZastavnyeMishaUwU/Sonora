package com.example.it_robota;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.auth.SettingsActivity;
import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.models.Track;

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
    private Button settingsButton;

    private ImageView trackImageView;
    private TextView resultTextView;

    private JamendoApiClient jamendoApiClient;

    private ExecutorService executorService;
    private Handler mainHandler;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEditText = findViewById(R.id.searchEditText);
        trackIdEditText = findViewById(R.id.trackIdEditText);

        searchButton = findViewById(R.id.searchButton);
        detailsButton = findViewById(R.id.detailsButton);
        settingsButton = findViewById(R.id.settingsButton);

        trackImageView = findViewById(R.id.trackImageView);
        resultTextView = findViewById(R.id.resultTextView);

        jamendoApiClient = new JamendoApiClient(this);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        searchButton.setOnClickListener(v -> searchTracks());
        detailsButton.setOnClickListener(v -> getTrackDetails());

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    SettingsActivity.class
            );

            startActivity(intent);
        });
    }

    private void searchTracks() {
        String query = searchEditText.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Enter track or artist name", Toast.LENGTH_SHORT).show();
            return;
        }

        trackImageView.setImageDrawable(null);
        resultTextView.setText("Searching tracks...");

        executorService.execute(() -> {
            try {
                List<Track> tracks = jamendoApiClient.searchTracks(query);

                mainHandler.post(() -> showSearchResults(tracks));

            } catch (Exception e) {
                e.printStackTrace();

                mainHandler.post(() -> {
                    resultTextView.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Search error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void getTrackDetails() {
        String trackId = trackIdEditText.getText().toString().trim();

        if (trackId.isEmpty()) {
            Toast.makeText(this, "Enter track ID", Toast.LENGTH_SHORT).show();
            return;
        }

        trackImageView.setImageDrawable(null);
        resultTextView.setText("Loading track details...");

        executorService.execute(() -> {
            try {
                Track track = jamendoApiClient.getTrackDetails(trackId);

                mainHandler.post(() -> showTrackDetails(track));

            } catch (Exception e) {
                e.printStackTrace();

                mainHandler.post(() -> {
                    resultTextView.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Details error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSearchResults(List<Track> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            resultTextView.setText("No tracks found.");
            return;
        }

        StringBuilder result = new StringBuilder();

        result.append("Found tracks: ")
                .append(tracks.size())
                .append("\n\n");

        for (Track track : tracks) {
            result.append("ID: ")
                    .append(track.getId())
                    .append("\n");

            result.append("Title: ")
                    .append(track.getName())
                    .append("\n");

            result.append("Artist: ")
                    .append(track.getArtistName())
                    .append("\n");

            result.append("Album: ")
                    .append(track.getAlbumName())
                    .append("\n");

            result.append("Duration: ")
                    .append(track.getDuration())
                    .append(" sec")
                    .append("\n\n");
        }

        result.append("Copy any ID and paste it into the details field.");

        resultTextView.setText(result.toString());
    }

    private void showTrackDetails(Track track) {
        if (track == null) {
            resultTextView.setText("Track details are empty.");
            return;
        }

        boolean isDownloaded = track.getLocalFilePath() != null
                && !track.getLocalFilePath().isEmpty();

        loadTrackImage(track.getImageUrl());

        String result =
                "Track Details\n\n" +
                        "ID: " + track.getId() + "\n" +
                        "Title: " + track.getName() + "\n" +
                        "Artist: " + track.getArtistName() + "\n" +
                        "Album: " + track.getAlbumName() + "\n" +
                        "Duration: " + track.getDuration() + " sec\n\n" +

                        "Image URL:\n" + track.getImageUrl() + "\n\n" +
                        "License URL:\n" + track.getLicenseUrl() + "\n\n" +
                        "Audio URL:\n" + track.getAudioUrl() + "\n\n" +
                        "Download URL:\n" + track.getDownloadUrl() + "\n\n" +

                        "Favorite: " + track.isFavorite() + "\n" +
                        "Downloaded: " + isDownloaded + "\n" +
                        "Local file path: " + track.getLocalFilePath();

        resultTextView.setText(result);
    }

    private void loadTrackImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            trackImageView.setImageDrawable(null);
            return;
        }

        executorService.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                InputStream inputStream = url.openStream();

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                inputStream.close();

                mainHandler.post(() -> trackImageView.setImageBitmap(bitmap));

            } catch (Exception e) {
                e.printStackTrace();

                mainHandler.post(() -> {
                    trackImageView.setImageDrawable(null);
                    Toast.makeText(this, "Image loading error", Toast.LENGTH_SHORT).show();
                });
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