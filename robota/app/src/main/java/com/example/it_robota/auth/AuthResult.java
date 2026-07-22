package com.example.it_robota.auth;

import com.example.it_robota.database.UserEntity;

/**
 * Represents the result of an authentication action.
 * Contains operation status, message and optional user data.
 */
public class AuthResult {

    private final boolean success;
    private final String message;
    private final UserEntity user;

    /**
     * Creates an authentication result.
     *
     * @param success true if operation was successful
     * @param message result message
     * @param user user data or null
     */
    public AuthResult(
            boolean success,
            String message,
            UserEntity user
    ) {
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
    public UserEntity getUser() {
        return user;
    }
}