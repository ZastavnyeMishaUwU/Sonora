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

public class SettingsActivity extends AppCompatActivity {

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private AuthRepository authRepository;
    private SessionManager sessionManager;
    private UserDao userDao;

    private TextView usernameText;
    private TextView emailText;
    private Button logoutButton;

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

    private void loadUserData() {

        long userId = sessionManager.getCurrentUserId();
        String sessionEmail = sessionManager.getCurrentUserEmail();

        if (sessionEmail != null
                && !sessionEmail.trim().isEmpty()) {

            emailText.setText(
                    "Email: " + sessionEmail
            );

        } else {

            emailText.setText(
                    "Email: unavailable"
            );
        }

        if (userId == -1L) {

            usernameText.setText(
                    "Username: unavailable"
            );

            return;
        }

        executorService.execute(() -> {

            UserEntity user = userDao.getUserById(userId);

            runOnUiThread(() -> {

                if (user != null
                        && user.getUsername() != null
                        && !user.getUsername().trim().isEmpty()) {

                    usernameText.setText(
                            "Username: " + user.getUsername()
                    );

                } else {

                    usernameText.setText(
                            "Username: unavailable"
                    );
                }
            });
        });
    }

    private void logout() {

        authRepository.logout();

        Intent intent = new Intent(
                SettingsActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        executorService.shutdownNow();
    }
}