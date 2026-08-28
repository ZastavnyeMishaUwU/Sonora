package com.example.it_robota.tracks;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.it_robota.R;
import com.example.it_robota.models.Track;
import com.example.it_robota.musicplayback.PlayerActivity;
import com.example.it_robota.repositories.TrackRepository;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Displays the track search screen and renders results returned by Jamendo.
 */
public class SearchActivity extends AppCompatActivity {

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private TrackRepository trackRepository;
    private TrackAdapter trackAdapter;

    private EditText searchInput;
    private Button searchButton;
    private Button backButton;
    private RecyclerView resultsRecyclerView;
    private ProgressBar progressBar;
    private TextView stateTextView;

    /**
     * Initializes the search screen, repository, list and user actions.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        trackRepository =
                new TrackRepository(this);

        bindViews();
        setupRecyclerView();
        setupActions();

        showEmptyState(
                R.string.search_start_message
        );
    }

    /**
     * Resolves all views used by the search screen.
     */
    private void bindViews() {
        backButton =
                findViewById(R.id.btnBackSearch);

        searchInput =
                findViewById(R.id.etSearchQuery);

        searchButton =
                findViewById(R.id.btnSearch);

        resultsRecyclerView =
                findViewById(R.id.rvSearchResults);

        progressBar =
                findViewById(R.id.searchProgress);

        stateTextView =
                findViewById(R.id.tvSearchState);
    }

    /**
     * Configures the RecyclerView and track adapter.
     */
    private void setupRecyclerView() {
        trackAdapter = new TrackAdapter(
                Collections.emptyList(),
                this::openPlayer
        );

        resultsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        resultsRecyclerView.setAdapter(
                trackAdapter
        );
    }

    /**
     * Configures the search button and keyboard search action.
     */
    private void setupActions() {
        backButton.setOnClickListener(
                view -> finish()
        );

        searchButton.setOnClickListener(
                view -> performSearch()
        );

        searchInput.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId
                            == EditorInfo.IME_ACTION_SEARCH) {

                        performSearch();
                        return true;
                    }

                    return false;
                }
        );
    }

    /**
     * Validates the entered query and starts a search request.
     */
    private void performSearch() {
        String query = searchInput
                .getText()
                .toString()
                .trim();

        if (query.isEmpty()) {
            showEmptyState(
                    R.string.search_query_required
            );

            return;
        }

        searchTracks(query);
    }

    /**
     * Searches tracks outside the UI thread.
     *
     * @param query search text entered by the user
     */
    private void searchTracks(String query) {
        showLoading();

        executorService.execute(() -> {
            try {
                List<Track> tracks =
                        trackRepository.searchTracks(
                                query
                        );

                runOnUiThread(
                        () -> displayResults(tracks)
                );

            } catch (Exception exception) {
                runOnUiThread(
                        this::showSearchError
                );
            }
        });
    }

    /**
     * Displays the returned search results or an empty-state message.
     *
     * @param tracks tracks returned from the repository
     */
    private void displayResults(List<Track> tracks) {
        progressBar.setVisibility(
                View.GONE
        );

        searchButton.setEnabled(true);

        if (tracks == null
                || tracks.isEmpty()) {

            trackAdapter.setTracks(
                    Collections.emptyList()
            );

            resultsRecyclerView.setVisibility(
                    View.GONE
            );

            showEmptyState(
                    R.string.search_no_results
            );

            return;
        }

        stateTextView.setVisibility(
                View.GONE
        );

        resultsRecyclerView.setVisibility(
                View.VISIBLE
        );

        trackAdapter.setTracks(tracks);
    }

    /**
     * Opens the player screen for the selected track.
     *
     * @param track selected track
     */
    private void openPlayer(Track track) {
        if (track == null
                || track.getId() == null
                || track.getId().trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(
                SearchActivity.this,
                PlayerActivity.class
        );

        intent.putExtra(
                PlayerActivity.EXTRA_TRACK_ID,
                track.getId()
        );

        startActivity(intent);
    }

    /**
     * Shows the loading state while a search request is running.
     */
    private void showLoading() {
        searchButton.setEnabled(false);

        progressBar.setVisibility(
                View.VISIBLE
        );

        stateTextView.setVisibility(
                View.GONE
        );

        resultsRecyclerView.setVisibility(
                View.GONE
        );
    }

    /**
     * Shows a search error without crashing the application.
     */
    private void showSearchError() {
        progressBar.setVisibility(
                View.GONE
        );

        searchButton.setEnabled(true);

        resultsRecyclerView.setVisibility(
                View.GONE
        );

        trackAdapter.setTracks(
                Collections.emptyList()
        );

        stateTextView.setText(
                R.string.search_error
        );

        stateTextView.setVisibility(
                View.VISIBLE
        );
    }

    /**
     * Shows a message when results are unavailable or input is invalid.
     *
     * @param messageResource string resource displayed to the user
     */
    private void showEmptyState(int messageResource) {
        progressBar.setVisibility(
                View.GONE
        );

        resultsRecyclerView.setVisibility(
                View.GONE
        );

        stateTextView.setText(
                messageResource
        );

        stateTextView.setVisibility(
                View.VISIBLE
        );
    }

    /**
     * Releases background-thread resources when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        executorService.shutdownNow();
    }
}