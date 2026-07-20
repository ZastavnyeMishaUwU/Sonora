package com.example.it_robota.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;

import com.example.it_robota.auth.AuthResult;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.UserDao;
import com.example.it_robota.database.UserEntity;
import com.example.it_robota.models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Repository for local authentication logic.
 * Handles user registration, login and active session state.
 */
public class AuthRepository {

    private static final String PREF_NAME = "auth_session_prefs";

    private static final String SESSION_LOGGED_IN_KEY = "session_logged_in";
    private static final String SESSION_USER_ID_KEY = "session_user_id";
    private static final String SESSION_USER_EMAIL_KEY = "session_user_email";

    private final UserDao userDao;
    private final SharedPreferences sharedPreferences;

    /**
     * Creates an AuthRepository instance.
     *
     * @param context application or activity context
     */
    public AuthRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);

        this.userDao = database.userDao();
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Registers a new local user.
     *
     * @param username username entered by the user
     * @param email email entered by the user
     * @param password password entered by the user
     * @return authentication result with status, message and optional user data
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

        try {
            if (userDao.checkUserExists(email)) {
                return new AuthResult(false, "User with this email already exists.", null);
            }

            long currentTime = System.currentTimeMillis();

            User user = new User(
                    currentTime,
                    username,
                    email,
                    hashPassword(password),
                    currentTime
            );

            userDao.insertUser(userToEntity(user));

            return new AuthResult(true, "User registered successfully.", user);

        } catch (Exception e) {
            e.printStackTrace();
            return new AuthResult(false, "Registration failed.", null);
        }
    }

    /**
     * Logs in an existing local user.
     *
     * @param email email entered by the user
     * @param password password entered by the user
     * @return authentication result with status, message and optional user data
     */
    public AuthResult login(String email, String password) {
        email = normalizeEmail(email);
        password = password == null ? "" : password.trim();

        if (!isEmailValid(email)) {
            return new AuthResult(false, "Email is not valid.", null);
        }

        if (password.isEmpty()) {
            return new AuthResult(false, "Password must not be empty.", null);
        }

        try {
            UserEntity userEntity = userDao.getUserByEmail(email);
            User user = entityToUser(userEntity);

            if (user == null) {
                return new AuthResult(false, "User with this email was not found.", null);
            }

            String enteredPasswordHash = hashPassword(password);

            if (!enteredPasswordHash.equals(user.getPasswordHash())) {
                return new AuthResult(false, "Incorrect password.", null);
            }

            saveSession(user);

            return new AuthResult(true, "Login successful.", user);

        } catch (Exception e) {
            e.printStackTrace();
            return new AuthResult(false, "Login failed.", null);
        }
    }

    /**
     * Checks whether a user session is active.
     *
     * @return true if user is logged in
     */
    public boolean isLoggedIn() {
        boolean loggedIn = sharedPreferences.getBoolean(SESSION_LOGGED_IN_KEY, false);
        String email = sharedPreferences.getString(SESSION_USER_EMAIL_KEY, "");

        if (!loggedIn || email == null || email.trim().isEmpty()) {
            return false;
        }

        return userDao.getUserByEmail(email) != null;
    }

    /**
     * Returns the current logged-in user.
     *
     * @return current user or null
     */
    public User getCurrentUser() {
        String email = getCurrentUserEmail();

        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        UserEntity userEntity = userDao.getUserByEmail(email);

        return entityToUser(userEntity);
    }

    /**
     * Returns current user email from session.
     *
     * @return current user email or empty string
     */
    public String getCurrentUserEmail() {
        return sharedPreferences.getString(SESSION_USER_EMAIL_KEY, "");
    }

    /**
     * Returns current user ID from session.
     *
     * @return current user ID or 0
     */
    public long getCurrentUserId() {
        return sharedPreferences.getLong(SESSION_USER_ID_KEY, 0);
    }

    /**
     * Clears the current active session.
     */
    public void logout() {
        sharedPreferences.edit()
                .remove(SESSION_LOGGED_IN_KEY)
                .remove(SESSION_USER_ID_KEY)
                .remove(SESSION_USER_EMAIL_KEY)
                .apply();
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
     * Checks if password is valid for registration.
     *
     * @param password password value
     * @return true if password is valid
     */
    public boolean isPasswordValid(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Checks whether user with this email already exists.
     *
     * @param email user email
     * @return true if user exists
     */
    public boolean checkUserExists(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail.isEmpty()) {
            return false;
        }

        return userDao.checkUserExists(normalizedEmail);
    }

    /**
     * Returns a user by email.
     *
     * @param email user email
     * @return user model or null
     */
    public User getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail.isEmpty()) {
            return null;
        }

        UserEntity userEntity = userDao.getUserByEmail(normalizedEmail);

        return entityToUser(userEntity);
    }

    /**
     * Saves current user session.
     *
     * @param user logged-in user
     */
    private void saveSession(User user) {
        sharedPreferences.edit()
                .putBoolean(SESSION_LOGGED_IN_KEY, true)
                .putLong(SESSION_USER_ID_KEY, user.getId())
                .putString(SESSION_USER_EMAIL_KEY, user.getEmail())
                .apply();
    }

    /**
     * Converts User model to UserEntity for Room database.
     *
     * @param user user model
     * @return user entity
     */
    private UserEntity userToEntity(User user) {
        return new UserEntity(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getCreatedAt()
        );
    }

    /**
     * Converts UserEntity from Room database to User model.
     *
     * @param userEntity user entity
     * @return user model or null
     */
    private User entityToUser(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }

        return new User(
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getEmail(),
                userEntity.getPasswordHash(),
                userEntity.getCreatedAt()
        );
    }

    /**
     * Creates SHA-256 hash from password.
     *
     * @param password raw password
     * @return hashed password
     * @throws Exception if hashing fails
     */
    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));

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