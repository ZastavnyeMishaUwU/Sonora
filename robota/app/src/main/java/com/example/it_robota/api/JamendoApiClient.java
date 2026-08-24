package com.example.it_robota.api;

import android.content.Context;
import android.util.Log;

import com.example.it_robota.models.Track;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Client class responsible for executing HTTP requests to the Jamendo API.
 * Provides methods to search for tracks, retrieve track details, and parse JSON responses into domain models.
 */
public class JamendoApiClient {

    private static final String TAG = "JamendoApiClient";

    private final Context context;

    /**
     * Initializes the client with application context.
     */
    public JamendoApiClient(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Searches for music tracks based on the user's query.
     * - Validates the query and API client ID.
     * - Builds the request URL and executes the GET request.
     * - Parses the JSON response into a list of Track objects.
     */
    public List<Track> searchTracks(String query) throws Exception {
        List<Track> tracks = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return tracks;
        }

        String clientId = ApiConfig.getClientId(context);

        if (clientId.trim().isEmpty()) {
            throw new Exception("Jamendo Client ID is missing. Add it to strings.xml.");
        }

        // Safe query encoding to handle spaces and special characters
        String encodedQuery = URLEncoder.encode(
                query.trim(),
                StandardCharsets.UTF_8.name()
        );

        /**
         * Construct the query URL with search parameters
         */
        String requestUrl = ApiConfig.BASE_URL + "tracks/?" +
                "client_id=" + clientId +
                "&format=" + ApiConfig.DEFAULT_FORMAT +
                "&limit=" + ApiConfig.DEFAULT_LIMIT +
                "&audioformat=" + ApiConfig.DEFAULT_AUDIO_FORMAT +
                "&search=" + encodedQuery;

        String response = sendGetRequest(requestUrl);

        JSONObject rootObject = new JSONObject(response);
        checkApiHeader(rootObject);

        JSONArray resultsArray = rootObject.optJSONArray("results");

        if (resultsArray == null) {
            return tracks;
        }

        /**
         * Parse each track object in the results array
         */
        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject trackObject = resultsArray.optJSONObject(i);

            if (trackObject != null) {
                tracks.add(parseTrack(trackObject));
            }
        }

        return tracks;
    }

    /**
     * Retrieves specific details for a single track by its ID.
     * - Validates the track ID and API client ID.
     * - Fetches the single result and returns the parsed Track object.
     */
    public Track getTrackDetails(String trackId) throws Exception {
        if (trackId == null || trackId.trim().isEmpty()) {
            throw new Exception("Track ID is empty.");
        }

        String clientId = ApiConfig.getClientId(context);

        if (clientId.trim().isEmpty()) {
            throw new Exception("Jamendo Client ID is missing. Add it to strings.xml.");
        }

        // Safe query encoding to handle spaces and special characters
        String encodedTrackId = URLEncoder.encode(
                trackId.trim(),
                StandardCharsets.UTF_8.name()
        );

        /**
         * Construct the URL to target a single track ID
         */
        String requestUrl = ApiConfig.BASE_URL + "tracks/?" +
                "client_id=" + clientId +
                "&format=" + ApiConfig.DEFAULT_FORMAT +
                "&limit=1" +
                "&audioformat=" + ApiConfig.DEFAULT_AUDIO_FORMAT +
                "&id[]=" + encodedTrackId;

        String response = sendGetRequest(requestUrl);

        JSONObject rootObject = new JSONObject(response);
        checkApiHeader(rootObject);

        JSONArray resultsArray = rootObject.optJSONArray("results");

        if (resultsArray == null || resultsArray.length() == 0) {
            throw new Exception("Track not found.");
        }

        JSONObject trackObject = resultsArray.optJSONObject(0);

        if (trackObject == null) {
            throw new Exception("Track details are empty.");
        }

        return parseTrack(trackObject);
    }

    /**
     * Sends an HTTP GET request to the specified URL.
     */
    private String sendGetRequest(String requestUrl) throws Exception {
        Log.d(TAG, "Request URL: " + requestUrl);

        HttpURLConnection connection = null;

        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();

            InputStream inputStream;

            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            String response = readStream(inputStream);

            Log.d(TAG, "Response code: " + responseCode);
            Log.d(TAG, "Response body: " + response);

            if (responseCode < 200 || responseCode >= 300) {
                throw new Exception("API request failed. Response code: " + responseCode);
            }

            return response;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Validates the Jamendo API response headers.
     */
    private void checkApiHeader(JSONObject rootObject) throws Exception {
        JSONObject headersObject = rootObject.optJSONObject("headers");

        if (headersObject == null) {
            return;
        }

        String status = headersObject.optString("status", "");
        int code = headersObject.optInt("code", -1);
        String errorMessage = headersObject.optString("error_message", "");

        if (!"success".equals(status) || code != 0) {
            throw new Exception("Jamendo API error: " + errorMessage);
        }
    }

    /**
     * Parses a single JSON track object into the local Track model.
     */
    private Track parseTrack(JSONObject trackObject) {
        String id = trackObject.optString("id", "");
        String name = trackObject.optString("name", "");
        String artistName = trackObject.optString("artist_name", "");
        String albumName = trackObject.optString("album_name", "");
        int duration = trackObject.optInt("duration", 0);

        String audioUrl = trackObject.optString("audio", "");
        String downloadUrl = trackObject.optString("audiodownload", "");
        Log.d(TAG, "id"+id);
        Log.d(TAG, "name"+name);
        Log.d(TAG, "audio"+audioUrl);
        Log.d(TAG, "download"+downloadUrl);

        String imageUrl = trackObject.optString("image", "");

        if (imageUrl.isEmpty()) {
            imageUrl = trackObject.optString("album_image", "");
        }
        Log.d(TAG,"url" + trackObject.optString("audio", "empty"));
        String licenseUrl = trackObject.optString("license_ccurl", "");

        return new Track(
                id,
                name,
                artistName,
                albumName,
                duration,
                audioUrl,
                downloadUrl,
                imageUrl,
                licenseUrl,
                false,
                null
        );
    }

    /**
     * Reads input stream contents and converts them to a single String.
     */
    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }
}