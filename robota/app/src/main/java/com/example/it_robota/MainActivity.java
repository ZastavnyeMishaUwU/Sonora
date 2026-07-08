package com.example.it_robota;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.models.Track;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText searchEditText;
    private Button searchButton;
    private TextView resultTextView;

    private JamendoApiClient jamendoApiClient;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        resultTextView = findViewById(R.id.resultTextView);

        jamendoApiClient = new JamendoApiClient(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        searchButton.setOnClickListener(v -> searchTracks());
    }

    private void searchTracks() {
        String query = searchEditText.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Enter track or artist name", Toast.LENGTH_SHORT).show();
            return;
        }

        resultTextView.setText("Searching...");

        executorService.execute(() -> {
            try {
                List<Track> tracks = jamendoApiClient.searchTracks(query);

                mainHandler.post(() -> showTracks(tracks));

            } catch (Exception e) {
                e.printStackTrace();

                mainHandler.post(() -> {
                    resultTextView.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Jamendo API error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showTracks(List<Track> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            resultTextView.setText("No tracks found.");
            return;
        }

        StringBuilder result = new StringBuilder();

        result.append("Found tracks: ")
                .append(tracks.size())
                .append("\n\n");

        for (Track track : tracks) {
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
                    .append("\n");

            result.append("Audio URL: ")
                    .append(track.getAudioUrl())
                    .append("\n\n");
        }

        resultTextView.setText(result.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executorService != null) {
            executorService.shutdown();
        }
    }
}