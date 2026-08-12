package com.example.it_robota.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.MainActivity;
import com.example.it_robota.R;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.UserDao;
import com.example.it_robota.repositories.AuthRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Activity for registering new users.
 */
public class RegisterActivity extends AppCompatActivity {

    private AuthRepository authRepository;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private TextView tvStatus;

    /*
     * Activity initialization method
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        AppDatabase db = AppDatabase.getInstance(this);
        UserDao userDao = db.userDao();
        SessionManager sessionManager = new SessionManager(this);

        authRepository = new AuthRepository(
                userDao,
                sessionManager
        );

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvStatus = findViewById(R.id.tvStatus);

        btnRegister.setOnClickListener(
                v -> performRegistration()
        );
    }

    /*
     * Validates user input and calls registration method
     */
    private void performRegistration() {
        String username = etUsername
                .getText()
                .toString()
                .trim();

        String email = etEmail
                .getText()
                .toString()
                .trim();

        String password = etPassword
                .getText()
                .toString();

        showStatus("");

        if (username.isEmpty()) {
            showStatus("Username cannot be empty");
            return;
        }

        if (email.isEmpty()) {
            showStatus("Email cannot be empty");
            return;
        }

        if (password.isEmpty()) {
            showStatus("Password cannot be empty");
            return;
        }

        setLoading(true);

        executorService.execute(() -> {
            try {
                AuthResult result = authRepository.register(
                        username,
                        email,
                        password
                );

                runOnUiThread(() -> {
                    setLoading(false);

                    if (result != null && result.isSuccess()) {
                        navigateToMainScreen();
                    } else if (result != null
                            && result.getMessage() != null
                            && !result.getMessage().trim().isEmpty()) {

                        showStatus(result.getMessage());

                    } else {
                        showStatus("Registration failed");
                    }
                });

            } catch (Exception exception) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showStatus("Registration failed");
                });
            }
        });
    }

    /*
     * Enables or disables registration controls while request is running
     */
    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        etUsername.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);

        if (loading) {
            btnRegister.setText("Registering...");
        } else {
            btnRegister.setText("Register");
        }
    }

    /*
     * Displays status or error message on UI
     */
    private void showStatus(String message) {
        tvStatus.setText(message);
    }

    /*
     * Navigates to MainActivity and clears task stack
     */
    private void navigateToMainScreen() {
        Intent intent = new Intent(
                this,
                MainActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    /*
     * Releases background thread resources
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}