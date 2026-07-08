package com.example.it_robota.api;

import android.content.Context;

import com.example.it_robota.R;

public class ApiConfig {

    public static final String BASE_URL = "https://api.jamendo.com/v3.0/";

    public static final String DEFAULT_FORMAT = "json";
    public static final String DEFAULT_AUDIO_FORMAT = "mp32";
    public static final int DEFAULT_LIMIT = 10;

    private ApiConfig() {
    }

    public static String getClientId(Context context) {
        return context.getString(R.string.jamendo_client_id);
    }
}