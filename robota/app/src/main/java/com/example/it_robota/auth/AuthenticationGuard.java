package com.example.it_robota.auth;

import android.app.Activity;
import android.content.Intent;

/**
 * Protects screens that require an authenticated user session.
 */
public final class AuthenticationGuard {

    private AuthenticationGuard() {
    }

    /**
     * Keeps an authenticated user on the requested screen and sends a guest to Login.
     *
     * @param activity screen requesting authenticated access
     * @return true when an active session is available
     */
    public static boolean requireLoggedIn(Activity activity) {
        if (new SessionManager(activity).isLoggedIn()) {
            return true;
        }

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        activity.startActivity(intent);
        activity.finish();
        return false;
    }
}
