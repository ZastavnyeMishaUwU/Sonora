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

    private String firstName;
    private String lastName;

    private String avatarUrl;
    private String bio;
    private String country;
    private String language;
    private String theme;

    private boolean emailVerified;

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
     */
    public User(
            long id,
            String username,
            String email,
            String passwordHash
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /**
     * Creates a User object with required fields and optional name fields.
     *
     * @param id user ID
     * @param username username
     * @param email user email
     * @param passwordHash hashed user password
     * @param firstName optional user first name
     * @param lastName optional user last name
     */
    public User(
            long id,
            String username,
            String email,
            String passwordHash,
            String firstName,
            String lastName
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Creates a User object with all available user fields.
     *
     * @param id user ID
     * @param username username
     * @param email user email
     * @param passwordHash hashed user password
     * @param firstName optional user first name
     * @param lastName optional user last name
     * @param avatarUrl optional avatar image URL
     * @param bio optional user biography
     * @param country optional user country
     * @param language optional application language
     * @param theme optional application theme
     * @param emailVerified whether the email is verified
     */
    public User(
            long id,
            String username,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String avatarUrl,
            String bio,
            String country,
            String language,
            String theme,
            boolean emailVerified
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.country = country;
        this.language = language;
        this.theme = theme;
        this.emailVerified = emailVerified;
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
     * Returns the optional user first name.
     *
     * @return first name or null
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the optional user first name.
     *
     * @param firstName first name or null
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the optional user last name.
     *
     * @return last name or null
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the optional user last name.
     *
     * @param lastName last name or null
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the user avatar URL.
     *
     * @return avatar URL or null
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * Sets the user avatar URL.
     *
     * @param avatarUrl avatar URL or null
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * Returns the user biography.
     *
     * @return biography or null
     */
    public String getBio() {
        return bio;
    }

    /**
     * Sets the user biography.
     *
     * @param bio biography or null
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * Returns the user country.
     *
     * @return country or null
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the user country.
     *
     * @param country country or null
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Returns the application language selected by the user.
     *
     * @return language code or null
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the application language selected by the user.
     *
     * @param language language code or null
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Returns the application theme selected by the user.
     *
     * @return theme or null
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Sets the application theme selected by the user.
     *
     * @param theme theme or null
     */
    public void setTheme(String theme) {
        this.theme = theme;
    }

    /**
     * Reports whether the user email has been verified.
     *
     * @return true when the email is verified
     */
    public boolean isEmailVerified() {
        return emailVerified;
    }

    /**
     * Sets whether the user email has been verified.
     *
     * @param emailVerified true when the email is verified
     */
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}