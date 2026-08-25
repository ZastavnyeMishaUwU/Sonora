package com.example.it_robota.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.R;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.repositories.AuthRepository;
import com.example.it_robota.tracks.SearchActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Displays the login screen and authenticates an existing local user.
 */
public class LoginActivity extends AppCompatActivity {

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private AuthRepository authRepository;

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private TextView statusTextView;
    private ProgressBar progressBar;

    /**
     * Initializes authentication dependencies, views and login actions.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppDatabase database = AppDatabase.getInstance(this);
        SessionManager sessionManager = new SessionManager(this);

        authRepository = new AuthRepository(
                database.userDao(),
                sessionManager
        );

        if (authRepository.isUserLoggedIn()) {
            openSearchScreen();
            return;
        }

        setContentView(R.layout.activity_login);

        bindViews();
        setupActions();
    }

    /**
     * Resolves all views used by the login screen.
     */
    private void bindViews() {
        emailEditText = findViewById(R.id.etLoginEmail);
        passwordEditText = findViewById(R.id.etLoginPassword);
        loginButton = findViewById(R.id.btnLogin);
        registerButton = findViewById(R.id.btnOpenRegister);
        statusTextView = findViewById(R.id.tvLoginStatus);
        progressBar = findViewById(R.id.loginProgress);
    }

    /**
     * Configures login and registration actions.
     */
    private void setupActions() {
        loginButton.setOnClickListener(view -> performLogin());

        registerButton.setOnClickListener(view -> openRegisterScreen());

        passwordEditText.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        performLogin();
                        return true;
                    }

                    return false;
                }
        );
    }

    /**
     * Reads entered credentials and validates empty fields.
     */
    private void performLogin() {
        String email = emailEditText
                .getText()
                .toString()
                .trim();

        String password = passwordEditText
                .getText()
                .toString();

        clearStatus();

        if (email.isEmpty()) {
            showStatus(R.string.login_email_required);
            return;
        }

        if (password.isEmpty()) {
            showStatus(R.string.login_password_required);
            return;
        }

        login(email, password);
    }

    /**
     * Authenticates the user outside the main UI thread.
     *
     * @param email email entered by the user
     * @param password password entered by the user
     */
    private void login(String email, String password) {
        showLoading(true);

        executorService.execute(() -> {
            AuthResult result =
                    authRepository.login(email, password);

            runOnUiThread(() -> handleLoginResult(result));
        });
    }

    /**
     * Processes the authentication result on the main UI thread.
     *
     * @param result authentication result returned by the repository
     */
    private void handleLoginResult(AuthResult result) {
        showLoading(false);

        if (result != null && result.isSuccess()) {
            openSearchScreen();
            return;
        }

        if (result == null
                || result.getMessage() == null
                || result.getMessage().trim().isEmpty()) {
            showStatus(R.string.login_error);
            return;
        }

        statusTextView.setText(result.getMessage());
        statusTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Shows or hides the loading state during authentication.
     *
     * @param loading true while login is running
     */
    private void showLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        loginButton.setEnabled(!loading);
        registerButton.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);
    }

    /**
     * Displays a login validation or authentication message.
     *
     * @param messageResource message string resource
     */
    private void showStatus(int messageResource) {
        statusTextView.setText(messageResource);
        statusTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Clears the previously displayed login status.
     */
    private void clearStatus() {
        statusTextView.setText("");
        statusTextView.setVisibility(View.GONE);
    }

    /**
     * Opens the registration screen.
     */
    private void openRegisterScreen() {
        Intent intent = new Intent(
                this,
                RegisterActivity.class
        );

        startActivity(intent);
    }

    /**
     * Opens the main track search screen and removes login from history.
     */
    private void openSearchScreen() {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Releases the background executor when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}
