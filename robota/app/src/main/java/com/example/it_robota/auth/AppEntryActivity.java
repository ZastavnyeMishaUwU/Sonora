package com.example.it_robota.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.repositories.AuthRepository;
import com.example.it_robota.tracks.SearchActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Routes application launches to authentication or track search based on session state.
 */
public class AppEntryActivity extends AppCompatActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Starts the session check without displaying an intermediate screen.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolveEntryDestination();
    }

    /**
     * Checks the active session outside the UI thread and selects the destination screen.
     */
    private void resolveEntryDestination() {
        executorService.execute(() -> {
            boolean userLoggedIn = false;

            try {
                AuthRepository authRepository = new AuthRepository(this);
                userLoggedIn = authRepository.isUserLoggedIn();
            } catch (RuntimeException exception) {
                userLoggedIn = false;
            }

            boolean destinationIsSearch = userLoggedIn;
            runOnUiThread(() -> openDestination(destinationIsSearch));
        });
    }

    /**
     * Opens Search for an authenticated user or Login for all other session states.
     *
     * @param destinationIsSearch true when the active session should open Search
     */
    private void openDestination(boolean destinationIsSearch) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Class<?> destination = destinationIsSearch
                ? SearchActivity.class
                : LoginActivity.class;
        Intent intent = new Intent(this, destination);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Releases the routing executor when the entry activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}
