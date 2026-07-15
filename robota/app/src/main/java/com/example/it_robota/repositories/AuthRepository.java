package com.example.it_robota.repositories;

import com.example.it_robota.auth.AuthResult;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.database.UserDao;
import com.example.it_robota.models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Provides the single entry point for local authentication operations.
 */
public class AuthRepository {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private final UserDao userDao;
    private final SessionManager sessionManager;

    /**
     * Creates a repository backed by persisted users and session storage.
     *
     * @param userDao data access object for local users
     * @param sessionManager storage for the active user session
     */
    public AuthRepository(UserDao userDao, SessionManager sessionManager) {
        this.userDao = userDao;
        this.sessionManager = sessionManager;
    }

    /**
     * Validates registration data and creates a new local user.
     *
     * @param username username entered by the user
     * @param email email entered by the user
     * @param password password entered by the user
     * @return success or failure result suitable for displaying in the UI
     */
    public AuthResult register(String username, String email, String password) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);

        if (normalizedUsername.isEmpty()) {
            return failure("Username must not be empty.");
        }

        if (!isEmailValid(normalizedEmail)) {
            return failure("Email is not valid.");
        }

        if (!isPasswordValid(password)) {
            return failure("Password must contain at least 6 characters.");
        }

        try {
            if (userDao.checkUserExists(normalizedEmail)) {
                return failure("User with this email already exists.");
            }

            long currentTime = System.currentTimeMillis();
            User user = new User(
                    currentTime,
                    normalizedUsername,
                    normalizedEmail,
                    hashPassword(password),
                    currentTime
            );

            userDao.insertUser(user);
            return new AuthResult(true, "User registered successfully.", user);
        } catch (Exception exception) {
            return failure("Registration failed.");
        }
    }

    /**
     * Validates credentials and starts a session for the matching local user.
     *
     * @param email email entered by the user
     * @param password password entered by the user
     * @return success or failure result suitable for displaying in the UI
     */
    public AuthResult login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);

        if (!isEmailValid(normalizedEmail)) {
            return failure("Email is not valid.");
        }

        if (password == null || password.isEmpty()) {
            return failure("Password must not be empty.");
        }

        try {
            User user = userDao.getUserByEmail(normalizedEmail);

            if (user == null || !passwordMatches(password, user.getPasswordHash())) {
                return failure("Email or password is incorrect.");
            }

            sessionManager.saveSession(user.getId(), user.getEmail());
            return new AuthResult(true, "Login successful.", user);
        } catch (Exception exception) {
            return failure("Login failed.");
        }
    }

    /**
     * Clears the active user session.
     */
    public void logout() {
        sessionManager.clearSession();
    }

    /**
     * Reports whether an active user session exists.
     *
     * @return true when a user is currently logged in
     */
    public boolean isUserLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    private AuthResult failure(String message) {
        return new AuthResult(false, message, null);
    }

    private boolean isEmailValid(String email) {
        return !email.isEmpty() && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    private boolean passwordMatches(String password, String passwordHash) throws Exception {
        if (passwordHash == null || passwordHash.isEmpty()) {
            return false;
        }

        byte[] actualHash = hashPassword(password).getBytes(StandardCharsets.UTF_8);
        byte[] expectedHash = passwordHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actualHash, expectedHash);
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();

        for (byte hashByte : hashBytes) {
            hash.append(String.format(Locale.ROOT, "%02x", hashByte));
        }

        return hash.toString();
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
