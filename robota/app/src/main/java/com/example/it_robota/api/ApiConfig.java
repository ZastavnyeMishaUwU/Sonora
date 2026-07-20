package com.example.it_robota.api;

import android.content.Context;

import com.example.it_robota.R;

/**
 * Stores common Jamendo API configuration values.
 * Provides access to API settings used by network classes.
 */
public class ApiConfig {

    public static final String BASE_URL = "https://api.jamendo.com/v3.0/";
    public static final String DEFAULT_FORMAT = "json";
    public static final String DEFAULT_AUDIO_FORMAT = "mp32";
    public static final int DEFAULT_LIMIT = 10;

    /**
     * Private constructor to prevent creating instances of this utility class.
     */
    private ApiConfig() {
    }

    /**
     * Returns the Jamendo Client ID from application string resources.
     *
     * @param context application or activity context
     * @return Jamendo Client ID
     */
    public static String getClientId(Context context) {
        return context.getString(R.string.jamendo_client_id);
    }
}