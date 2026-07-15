package com.example.it_robota;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.auth.AuthRepository;
import com.example.it_robota.auth.AuthResult;

/**
 * Activity for local user login.
 * Allows an existing user to sign in and opens the main screen after successful login.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView statusTextView;

    private AuthRepository authRepository;

    /**
     * Initializes login screen, checks active session and sets login click listener.
     *
     * @param savedInstanceState saved activity state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authRepository = new AuthRepository(this);

        if (authRepository.isLoggedIn()) {
            openMainScreen();
            return;
        }

        setContentView(R.layout.activity_login);

        initViews();
        setupClickListeners();
    }

    /**
     * Finds all login screen views by ID.
     */
    private void initViews() {
        emailEditText = findViewById(R.id.loginEmailEditText);
        passwordEditText = findViewById(R.id.loginPasswordEditText);
        loginButton = findViewById(R.id.loginButton);
        statusTextView = findViewById(R.id.loginStatusTextView);
    }

    /**
     * Connects login button with login action.
     */
    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> loginUser());
    }

    /**
     * Reads email and password from UI and calls AuthRepository.login().
     */
    private void loginUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        AuthResult result = authRepository.login(email, password);

        statusTextView.setText(result.getMessage());

        if (result.isSuccess()) {
            openMainScreen();
        }
    }

    /**
     * Opens the main/search screen and removes LoginActivity from the back stack.
     */
    private void openMainScreen() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}