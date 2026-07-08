package com.example.it_robota.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.models.Track;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Repository for track-related logic.
 * Connects UI logic with Jamendo API and temporary local favorite storage.
 */
public class TrackRepository {

    private static final String PREF_NAME = "track_repository_prefs";
    private static final String FAVORITE_IDS_KEY = "favorite_track_ids";
    private static final String FAVORITE_TRACK_PREFIX = "favorite_track_";

    private final JamendoApiClient jamendoApiClient;
    private final SharedPreferences sharedPreferences;

    /**
     * Creates a TrackRepository instance.
     *
     * @param context application or activity context
     */
    public TrackRepository(Context context) {
        this.jamendoApiClient = new JamendoApiClient(context);
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Searches tracks through the Jamendo API.
     *
     * @param query search text entered by the user
     * @return list of found tracks
     * @throws Exception if API request or parsing fails
     */
    public List<Track> searchTracks(String query) throws Exception {
        return jamendoApiClient.searchTracks(query);
    }

    /**
     * Loads full details for a selected track by ID.
     *
     * @param trackId Jamendo track ID
     * @return track details
     * @throws Exception if track details cannot be loaded
     */
    public Track getTrackDetails(String trackId) throws Exception {
        Track track = jamendoApiClient.getTrackDetails(trackId);

        if (track != null) {
            track.setFavorite(isTrackFavorite(track.getId()));
        }

        return track;
    }

    /**
     * Returns locally saved favorite tracks.
     *
     * @return list of favorite tracks
     */
    public List<Track> getSavedTracks() {
        List<Track> tracks = new ArrayList<>();
        Set<String> favoriteIds = getFavoriteIds();

        for (String trackId : favoriteIds) {
            String json = sharedPreferences.getString(FAVORITE_TRACK_PREFIX + trackId, null);

            if (json == null || json.trim().isEmpty()) {
                continue;
            }

            try {
                Track track = jsonToTrack(json);
                tracks.add(track);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return tracks;
    }

    /**
     * Saves a track as favorite in temporary local storage.
     *
     * @param track track that should be saved as favorite
     * @throws Exception if track data cannot be saved
     */
    public void saveFavorite(Track track) throws Exception {
        if (track == null || track.getId() == null || track.getId().trim().isEmpty()) {
            throw new Exception("Track is empty.");
        }

        track.setFavorite(true);

        Set<String> favoriteIds = getFavoriteIds();
        favoriteIds.add(track.getId());

        sharedPreferences.edit()
                .putStringSet(FAVORITE_IDS_KEY, favoriteIds)
                .putString(FAVORITE_TRACK_PREFIX + track.getId(), trackToJson(track).toString())
                .apply();
    }

    /**
     * Removes a track from favorite storage.
     *
     * @param trackId track ID that should be removed
     */
    public void removeFavorite(String trackId) {
        if (trackId == null || trackId.trim().isEmpty()) {
            return;
        }

        Set<String> favoriteIds = getFavoriteIds();
        favoriteIds.remove(trackId);

        sharedPreferences.edit()
                .putStringSet(FAVORITE_IDS_KEY, favoriteIds)
                .remove(FAVORITE_TRACK_PREFIX + trackId)
                .apply();
    }

    /**
     * Checks if a track is already saved as favorite.
     *
     * @param trackId track ID
     * @return true if track is favorite, false otherwise
     */
    public boolean isTrackFavorite(String trackId) {
        if (trackId == null || trackId.trim().isEmpty()) {
            return false;
        }

        return getFavoriteIds().contains(trackId);
    }

    /**
     * Reads favorite track IDs from SharedPreferences.
     *
     * @return copied set of favorite track IDs
     */
    private Set<String> getFavoriteIds() {
        Set<String> savedIds = sharedPreferences.getStringSet(FAVORITE_IDS_KEY, new HashSet<>());
        return new HashSet<>(savedIds);
    }

    /**
     * Converts a Track object into JSON for local storage.
     *
     * @param track track object
     * @return JSON object with track fields
     * @throws Exception if JSON creation fails
     */
    private JSONObject trackToJson(Track track) throws Exception {
        JSONObject object = new JSONObject();

        object.put("id", track.getId());
        object.put("name", track.getName());
        object.put("artistName", track.getArtistName());
        object.put("albumName", track.getAlbumName());
        object.put("duration", track.getDuration());
        object.put("audioUrl", track.getAudioUrl());
        object.put("downloadUrl", track.getDownloadUrl());
        object.put("imageUrl", track.getImageUrl());
        object.put("licenseUrl", track.getLicenseUrl());
        object.put("isFavorite", track.isFavorite());
        object.put("localFilePath", track.getLocalFilePath());

        return object;
    }

    /**
     * Converts stored JSON into a Track object.
     *
     * @param json stored track JSON
     * @return restored Track object
     * @throws Exception if JSON parsing fails
     */
    private Track jsonToTrack(String json) throws Exception {
        JSONObject object = new JSONObject(json);

        String localFilePath = null;

        if (!object.isNull("localFilePath")) {
            localFilePath = object.optString("localFilePath", null);
        }

        return new Track(
                object.optString("id", ""),
                object.optString("name", ""),
                object.optString("artistName", ""),
                object.optString("albumName", ""),
                object.optInt("duration", 0),
                object.optString("audioUrl", ""),
                object.optString("downloadUrl", ""),
                object.optString("imageUrl", ""),
                object.optString("licenseUrl", ""),
                object.optBoolean("isFavorite", true),
                localFilePath
        );
    }
}