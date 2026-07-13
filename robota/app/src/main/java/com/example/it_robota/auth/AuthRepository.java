package com.example.it_robota.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;

import com.example.it_robota.models.User;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Repository for local authentication logic.
 * Handles local user registration using SharedPreferences.
 */
public class AuthRepository {

    private static final String PREF_NAME = "auth_repository_prefs";
    private static final String USER_EMAILS_KEY = "user_emails";
    private static final String USER_PREFIX = "user_";
    private static final String KEY_LOGGED_IN = "loggedIn";
    private static final String KEY_USER_ID = "currentUserId";
    private static final String KEY_USER_EMAIL = "currentUserEmail";

    private final SharedPreferences sharedPreferences;

    /**
     * Creates an AuthRepository instance.
     *
     * @param context application or activity context
     */
    public AuthRepository(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Registers a new local user.
     *
     * @param username username entered by the user
     * @param email email entered by the user
     * @param password password entered by the user
     * @return authentication result with status and message
     */
    public AuthResult register(String username, String email, String password) {
        username = username == null ? "" : username.trim();
        email = normalizeEmail(email);
        password = password == null ? "" : password.trim();

        if (!isUsernameValid(username)) {
            return new AuthResult(false, "Username must not be empty.", null);
        }

        if (!isEmailValid(email)) {
            return new AuthResult(false, "Email is not valid.", null);
        }

        if (!isPasswordValid(password)) {
            return new AuthResult(false, "Password must contain at least 6 characters.", null);
        }

        if (checkUserExists(email)) {
            return new AuthResult(false, "User with this email already exists.", null);
        }

        try {
            User user = new User(
                    System.currentTimeMillis(),
                    username,
                    email,
                    hashPassword(password),
                    System.currentTimeMillis()
            );

            saveUser(user);

            return new AuthResult(true, "User registered successfully.", user);

        } catch (Exception e) {
            e.printStackTrace();
            return new AuthResult(false, "Registration failed.", null);
        }
    }

    /**
     * Logs in a user locally.
     *
     * @param email user email
     * @param password user password
     * @return authentication result
     */
    public AuthResult login(String email, String password) {
        email = normalizeEmail(email);
        password = password == null ? "" : password.trim();

        if (!isEmailValid(email)) {
            return new AuthResult(false, "Invalid email format.", null);
        }
        User user = getUserByEmail(email);
        if (user == null) {
            return new AuthResult(false, "User not found.", null);
        }

        try {
            String hashedPassword = hashPassword(password);
            if (user.getPasswordHash().equals(hashedPassword)) {
                saveSession(user);
                return new AuthResult(true, "Login successful.", user);
            } else {
                return new AuthResult(false, "Invalid password.", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new AuthResult(false, "Login failed.", null);
        }
    }

    /**
     * Checks if username is valid.
     *
     * @param username username value
     * @return true if username is valid
     */
    public boolean isUsernameValid(String username) {
        return username != null && !username.trim().isEmpty();
    }

    /**
     * Checks if email has valid format.
     *
     * @param email email value
     * @return true if email is valid
     */
    public boolean isEmailValid(String email) {
        return email != null
                && !email.trim().isEmpty()
                && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Checks if password is valid.
     *
     * @param password password value
     * @return true if password is valid
     */
    public boolean isPasswordValid(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if logged in
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
    }

    /**
     * Returns current user ID from session.
     *
     * @return user ID or -1 if not logged in
     */
    public long getCurrentUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, -1);
    }

    /**
     * Returns current user email from session.
     *
     * @return user email or null if not logged in
     */
    public String getCurrentUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }
    /**
     * Logs out the current user.
     * Clears only session-related data without deleting user data.
     */
    public void logout() {
        sharedPreferences.edit()
                .remove(KEY_LOGGED_IN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .apply();
    }
    /**
     * Checks if a user with this email already exists.
     *
     * @param email user email
     * @return true if user already exists
     */
    public boolean checkUserExists(String email) {
        String normalizedEmail = normalizeEmail(email);
        return getUserEmails().contains(normalizedEmail);
    }

    /**
     * Returns a user by email.
     *
     * @param email user email
     * @return found user or null
     */
    public User getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail.isEmpty()) {
            return null;
        }

        String json = sharedPreferences.getString(USER_PREFIX + normalizedEmail, null);

        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return jsonToUser(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Saves user session to SharedPreferences.
     *
     * @param user user object
     */
    private void saveSession(User user) {
        sharedPreferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_USER_ID, user.getId())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .apply();
    }

    /**
     * Saves a user to SharedPreferences.
     *
     * @param user user object
     * @throws Exception if user cannot be converted to JSON
     */
    private void saveUser(User user) throws Exception {
        Set<String> emails = getUserEmails();
        emails.add(user.getEmail());

        sharedPreferences.edit()
                .putStringSet(USER_EMAILS_KEY, emails)
                .putString(USER_PREFIX + user.getEmail(), userToJson(user).toString())
                .apply();
    }

    /**
     * Reads all registered user emails.
     *
     * @return copied set of user emails
     */
    private Set<String> getUserEmails() {
        Set<String> savedEmails = sharedPreferences.getStringSet(USER_EMAILS_KEY, new HashSet<>());
        return new HashSet<>(savedEmails);
    }

    /**
     * Converts user data into JSON.
     *
     * @param user user object
     * @return JSON object with user fields
     * @throws Exception if JSON creation fails
     */
    private JSONObject userToJson(User user) throws Exception {
        JSONObject object = new JSONObject();

        object.put("id", user.getId());
        object.put("username", user.getUsername());
        object.put("email", user.getEmail());
        object.put("passwordHash", user.getPasswordHash());
        object.put("createdAt", user.getCreatedAt());

        return object;
    }

    /**
     * Converts stored JSON into a User object.
     *
     * @param json stored user JSON
     * @return restored User object
     * @throws Exception if JSON parsing fails
     */
    private User jsonToUser(String json) throws Exception {
        JSONObject object = new JSONObject(json);

        return new User(
                object.optLong("id", 0),
                object.optString("username", ""),
                object.optString("email", ""),
                object.optString("passwordHash", ""),
                object.optLong("createdAt", 0)
        );
    }

    /**
     * Creates a SHA-256 hash from password.
     *
     * @param password raw password
     * @return hashed password
     * @throws Exception if hashing fails
     */
    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(password.getBytes());

        StringBuilder hexString = new StringBuilder();

        for (byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);

            if (hex.length() == 1) {
                hexString.append('0');
            }

            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Normalizes email value.
     *
     * @param email raw email
     * @return normalized email
     */
    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}