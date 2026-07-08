package com.example.it_robota.auth;

import com.example.it_robota.models.User;

/**
 * Represents the result of an authentication action.
 * Contains operation status, message and optional user data.
 */
public class AuthResult {

    private final boolean success;
    private final String message;
    private final User user;

    /**
     * Creates an authentication result.
     *
     * @param success true if operation was successful
     * @param message result message
     * @param user user data or null
     */
    public AuthResult(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    /**
     * Returns whether the authentication action was successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the result message.
     *
     * @return result message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the user related to the result.
     *
     * @return user object or null
     */
    public User getUser() {
        return user;
    }
}