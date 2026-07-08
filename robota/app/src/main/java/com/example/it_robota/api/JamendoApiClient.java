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

public class JamendoApiClient {

    private static final String TAG = "JamendoApiClient";

    private final Context context;

    public JamendoApiClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Track> searchTracks(String query) throws Exception {
        List<Track> tracks = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return tracks;
        }

        String clientId = ApiConfig.getClientId(context);

        if (clientId == null || clientId.trim().isEmpty()) {
            throw new Exception("Jamendo Client ID is missing. Add it to strings.xml.");
        }

        String encodedQuery = URLEncoder.encode(
                query.trim(),
                StandardCharsets.UTF_8.toString()
        );

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

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject trackObject = resultsArray.optJSONObject(i);

            if (trackObject != null) {
                tracks.add(parseTrack(trackObject));
            }
        }

        return tracks;
    }

    public Track getTrackDetails(String trackId) throws Exception {
        if (trackId == null || trackId.trim().isEmpty()) {
            throw new Exception("Track ID is empty.");
        }

        String clientId = ApiConfig.getClientId(context);

        if (clientId == null || clientId.trim().isEmpty()) {
            throw new Exception("Jamendo Client ID is missing. Add it to strings.xml.");
        }

        String encodedTrackId = URLEncoder.encode(
                trackId.trim(),
                StandardCharsets.UTF_8.toString()
        );

        String requestUrl = ApiConfig.BASE_URL + "tracks/?" +
                "client_id=" + clientId +
                "&format=" + ApiConfig.DEFAULT_FORMAT +
                "&limit=1" +
                "&audioformat=" + ApiConfig.DEFAULT_AUDIO_FORMAT +
                "&id=" + encodedTrackId;

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

    private Track parseTrack(JSONObject trackObject) {
        String id = trackObject.optString("id", "");
        String name = trackObject.optString("name", "");
        String artistName = trackObject.optString("artist_name", "");
        String albumName = trackObject.optString("album_name", "");
        int duration = trackObject.optInt("duration", 0);

        String audioUrl = trackObject.optString("audio", "");
        String downloadUrl = trackObject.optString("audiodownload", "");

        String imageUrl = trackObject.optString("image", "");

        if (imageUrl.isEmpty()) {
            imageUrl = trackObject.optString("album_image", "");
        }

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

    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();

        return result.toString();
    }
}