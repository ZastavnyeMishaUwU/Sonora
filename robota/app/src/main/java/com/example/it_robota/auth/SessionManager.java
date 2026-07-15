package com.example.it_robota.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages the current user session in SharedPreferences.
 */
public class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_LOGGED_IN = "loggedIn";
    private static final String KEY_CURRENT_USER_ID = "currentUserId";
    private static final String KEY_CURRENT_USER_EMAIL = "currentUserEmail";
    private static final long NO_USER_ID = -1L;

    private final SharedPreferences sharedPreferences;

    /**
     * Creates a session manager using application-scoped preferences.
     *
     * @param context application or activity context
     */
    public SessionManager(Context context) {
        Context applicationContext = context.getApplicationContext();
        sharedPreferences = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Saves the active user's identifier and email.
     *
     * @param userId active user identifier
     * @param email active user email
     */
    public void saveSession(long userId, String email) {
        sharedPreferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_CURRENT_USER_ID, userId)
                .putString(KEY_CURRENT_USER_EMAIL, email)
                .apply();
    }

    /**
     * Removes all values belonging to the current session.
     */
    public void clearSession() {
        sharedPreferences.edit()
                .remove(KEY_LOGGED_IN)
                .remove(KEY_CURRENT_USER_ID)
                .remove(KEY_CURRENT_USER_EMAIL)
                .apply();
    }

    /**
     * Reports whether a user session is currently active.
     *
     * @return true when a user is logged in
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
    }

    /**
     * Returns the active user's identifier.
     *
     * @return user identifier, or -1 when no session exists
     */
    public long getCurrentUserId() {
        return sharedPreferences.getLong(KEY_CURRENT_USER_ID, NO_USER_ID);
    }

    /**
     * Returns the active user's email.
     *
     * @return user email, or null when no session exists
     */
    public String getCurrentUserEmail() {
        return sharedPreferences.getString(KEY_CURRENT_USER_EMAIL, null);
    }
}
