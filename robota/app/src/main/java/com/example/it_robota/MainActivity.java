package com.example.it_robota;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.it_robota.models.User;

public class MainActivity extends AppCompatActivity {

    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);

        User testUser = new User(
                1,
                "test_user",
                "test@gmail.com",
                "hashed_password_example",
                System.currentTimeMillis()
        );

        resultTextView.setText(
                "Project started successfully\n\n" +
                        "User model test:\n" +
                        "ID: " + testUser.getId() + "\n" +
                        "Username: " + testUser.getUsername() + "\n" +
                        "Email: " + testUser.getEmail() + "\n" +
                        "Created at: " + testUser.getCreatedAt()
        );
    }
}