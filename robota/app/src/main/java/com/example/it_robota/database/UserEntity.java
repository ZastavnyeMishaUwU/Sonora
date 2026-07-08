package com.example.it_robota.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity for storing local user data in the Room database.
 */
@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    private long id;

    private String username;
    private String email;
    private String passwordHash;
    private long createdAt;

    /**
     * Creates an empty UserEntity object.
     */
    public UserEntity() {
    }

    /**
     * Creates a UserEntity object with all user fields.
     *
     * @param id user ID
     * @param username username
     * @param email user email
     * @param passwordHash hashed password
     * @param createdAt account creation timestamp
     */
    public UserEntity(long id, String username, String email, String passwordHash, long createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    /**
     * Returns user ID.
     *
     * @return user ID
     */
    public long getId() {
        return id;
    }

    /**
     * Sets user ID.
     *
     * @param id user ID
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns username.
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets username.
     *
     * @param username username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns user email.
     *
     * @return user email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets user email.
     *
     * @param email user email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns password hash.
     *
     * @return password hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets password hash.
     *
     * @param passwordHash password hash
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Returns account creation timestamp.
     *
     * @return account creation timestamp
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets account creation timestamp.
     *
     * @param createdAt account creation timestamp
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}