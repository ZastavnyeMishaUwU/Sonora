package com.example.it_robota.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.R;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.UserDao;
import com.example.it_robota.database.UserEntity;
import com.example.it_robota.repositories.AuthRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Displays the current user's account information
 * and provides the logout action.
 */
public class SettingsActivity extends AppCompatActivity {

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private AuthRepository authRepository;
    private SessionManager sessionManager;
    private UserDao userDao;

    private TextView usernameText;
    private TextView emailText;
    private Button logoutButton;

    /*
     * Initializes the settings screen and its account actions.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        AppDatabase database = AppDatabase.getInstance(this);

        sessionManager = new SessionManager(this);
        userDao = database.userDao();

        authRepository = new AuthRepository(
                userDao,
                sessionManager
        );

        usernameText = findViewById(R.id.usernameText);
        emailText = findViewById(R.id.emailText);
        logoutButton = findViewById(R.id.logoutButton);

        loadUserData();

        logoutButton.setOnClickListener(view -> logout());
    }

    /*
     * Loads the current user's username and email.
     * Missing account data is displayed as unavailable
     * instead of causing the activity to crash.
     */
    private void loadUserData() {
        long userId = sessionManager.getCurrentUserId();
        String email = sessionManager.getCurrentUserEmail();

        if (email != null && !email.trim().isEmpty()) {
            emailText.setText("Email: " + email);
        } else {
            emailText.setText("Email: unavailable");
        }

        if (userId <= 0) {
            usernameText.setText("Username: unavailable");
            return;
        }

        /*
         * Reads the user from the Room database outside
         * the main UI thread.
         */
        executorService.execute(() -> {
            UserEntity user = null;

            try {
                user = userDao.getUserById(userId);
            } catch (RuntimeException exception) {
                /*
                 * Missing or corrupted account data should not
                 * crash the settings screen.
                 */
            }

            UserEntity finalUser = user;

            runOnUiThread(() -> {
                if (finalUser != null
                        && finalUser.getUsername() != null
                        && !finalUser.getUsername().trim().isEmpty()) {

                    usernameText.setText(
                            "Username: " + finalUser.getUsername()
                    );
                } else {
                    usernameText.setText("Username: unavailable");
                }
            });
        });
    }

    /*
     * Clears the current session and navigates the user
     * back to the Login screen.
     */
    private void logout() {
        authRepository.logout();

        Intent intent = new Intent(
                SettingsActivity.this,
                LoginActivity.class
        );

        /*
         * Removes authenticated screens from the back stack
         * so the user cannot return to Settings after logout.
         */
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    /*
     * Stops the background executor when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        executorService.shutdownNow();
    }
}
