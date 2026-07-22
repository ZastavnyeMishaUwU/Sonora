package com.example.it_robota.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.LoginActivity;
import com.example.it_robota.R;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.UserEntity;
import com.example.it_robota.repositories.AuthRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private TextView usernameTextView;
    private TextView emailTextView;
    private AuthRepository authRepository;
    private SessionManager sessionManager;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        usernameTextView = findViewById(R.id.usernameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        Button logoutButton = findViewById(R.id.logoutButton);

        sessionManager = new SessionManager(this);
        authRepository = new AuthRepository(
                AppDatabase.getInstance(this).userDao(),
                sessionManager
        );
        executorService = Executors.newSingleThreadExecutor();

        loadUserData();
        logoutButton.setOnClickListener(view -> logout());
    }

    private void loadUserData() {
        executorService.execute(() -> {
            UserEntity user = authRepository.getCurrentUser();
            String username = user == null ? null : user.getUsername();
            String email = user == null ? sessionManager.getCurrentUserEmail() : user.getEmail();

            runOnUiThread(() -> {
                usernameTextView.setText(getValue(username));
                emailTextView.setText(getValue(email));
            });
        });
    }

    private String getValue(String value) {
        return value == null || value.trim().isEmpty()
                ? getString(R.string.settings_no_data)
                : value;
    }

    private void logout() {
        authRepository.logout();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
        super.onDestroy();
    }
}
