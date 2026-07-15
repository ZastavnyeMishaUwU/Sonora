package com.example.it_robota.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.MainActivity;
import com.example.it_robota.R;

public class RegisterActivity extends AppCompatActivity {

    private AuthRepository authRepository;

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ініціалізуємо репозиторій контекстом Activity
        authRepository = new AuthRepository(this);

        // Ініціалізуємо елементи інтерфейсу (UI)
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvStatus = findViewById(R.id.tvStatus);

        btnRegister.setOnClickListener(v -> performRegistration());
    }

    private void performRegistration() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Очищуємо попередній статус помилки
        showStatus("");

        // Локальна валідація на порожні поля (запобігає крашам)
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

        // Викликаємо логіку реєстрації з репозиторію
        AuthResult result = authRepository.register(username, email, password);

        if (result.isSuccess()) {
            // Успіх! Переходимо на головний екран
            navigateToMainScreen();
        } else {
            // Виводимо конкретну помилку валідації, яку повернув репозиторій
            showStatus(result.getMessage());
        }
    }

    private void showStatus(String message) {
        tvStatus.setText(message);
    }

    private void navigateToMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}