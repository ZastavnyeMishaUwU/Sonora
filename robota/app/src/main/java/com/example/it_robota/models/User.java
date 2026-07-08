package com.example.it_robota.models;

/**
 * Represents a local application user.
 * Used for registration, login and user session logic.
 */
public class User {

    private long id;
    private String username;
    private String email;
    private String passwordHash;
    private long createdAt;

    /**
     * Creates an empty User object.
     */
    public User() {
    }

    /**
     * Creates a User object with all required fields.
     *
     * @param id user ID
     * @param username username
     * @param email user email
     * @param passwordHash hashed user password
     * @param createdAt account creation timestamp
     */
    public User(long id, String username, String email, String passwordHash, long createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    /**
     * Returns the user ID.
     *
     * @return user ID
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the user ID.
     *
     * @param id user ID
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the username.
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the user email.
     *
     * @return user email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user email.
     *
     * @param email user email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the hashed user password.
     *
     * @return hashed password
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the hashed user password.
     *
     * @param passwordHash hashed password
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Returns the account creation timestamp.
     *
     * @return creation timestamp
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the account creation timestamp.
     *
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}