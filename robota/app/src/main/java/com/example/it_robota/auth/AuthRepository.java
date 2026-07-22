package com.example.it_robota.auth;

import android.content.Context;

public class AuthRepository {

    public AuthRepository(Context context) {
        // Repository constructor
    }

    /**
     * User registration method.
     * Returns AuthResult with success status, message, and User object (currently null).
     */
    public AuthResult register(String username, String email, String password) {
        // Temporary stub: successful registration to test navigation between screens
        return new AuthResult(true, "Registration successful", null);
    }
}