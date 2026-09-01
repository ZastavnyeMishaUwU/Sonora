package com.example.it_robota.repositories;

import android.content.Context;

import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.FavoriteTrackDao;
import com.example.it_robota.database.FavoriteTrackEntity;
import com.example.it_robota.database.TrackEntity;
import com.example.it_robota.models.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for track search, details and user-specific favorite storage.
 */
public class TrackRepository {

    private static final long NO_USER_ID = -1L;

    private final JamendoApiClient jamendoApiClient;
    private final FavoriteTrackDao favoriteTrackDao;
    private final SessionManager sessionManager;

    /**
     * Creates a TrackRepository instance.
     *
     * @param context application or activity context
     */
    public TrackRepository(Context context) {
        Context applicationContext = context.getApplicationContext();
        jamendoApiClient = new JamendoApiClient(applicationContext);
        favoriteTrackDao = AppDatabase.getInstance(applicationContext).favoriteTrackDao();
        sessionManager = new SessionManager(applicationContext);
    }
    public TrackRepository(JamendoApiClient jamendoApiClient, FavoriteTrackDao favoriteTrackDao, SessionManager sessionManager) {
        this.jamendoApiClient = jamendoApiClient;
        this.favoriteTrackDao = favoriteTrackDao;
        this.sessionManager = sessionManager;
    }

    /**
     * Searches tracks through the Jamendo API.
     *
     * @param query search text entered by the user
     * @return list of found tracks
     * @throws Exception if no user is logged in or the API request fails
     */
    public List<Track> searchTracks(String query) throws Exception {
        if (getCurrentUserId() == NO_USER_ID) {
            throw new IllegalStateException("User is not logged in.");
        }

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
     * Returns favorite tracks saved for the active account.
     *
     * @return current user's favorite tracks
     */
    public List<Track> getSavedTracks() {
        long userId = getCurrentUserId();
        List<Track> tracks = new ArrayList<>();
        if (userId == NO_USER_ID) {
            return tracks;
        }

        for (TrackEntity entity : favoriteTrackDao.getFavoriteTracksByUser(userId)) {
            tracks.add(toTrack(entity));
        }
        return tracks;
    }

    /**
     * Saves a track as a favorite for the active account.
     *
     * @param track track to save
     * @throws Exception when the track is invalid or no user is logged in
     */
    public void saveFavorite(Track track) throws Exception {
        if (track == null || isBlank(track.getId())) {
            throw new Exception("Track is empty.");
        }

        long userId = getCurrentUserId();
        if (userId == NO_USER_ID) {
            throw new Exception("User is not logged in.");
        }

        favoriteTrackDao.saveFavorite(
                toTrackEntity(track),
                new FavoriteTrackEntity(userId, track.getId())
        );
        track.setFavorite(true);
    }

    /**
     * Removes a track from the active account's favorites.
     *
     * @param trackId track identifier
     */
    public void removeFavorite(String trackId) {
        long userId = getCurrentUserId();
        if (userId == NO_USER_ID || isBlank(trackId)) {
            return;
        }
        favoriteTrackDao.deleteTrack(trackId, userId);
    }

    /**
     * Checks whether a track is a favorite of the active account.
     *
     * @param trackId track identifier
     * @return true when the current user saved the track
     */
    public boolean isTrackFavorite(String trackId) {
        long userId = getCurrentUserId();
        return userId != NO_USER_ID
                && !isBlank(trackId)
                && favoriteTrackDao.isTrackFavorite(trackId, userId);
    }

    /**
     * Returns the valid active user identifier.
     *
     * @return user identifier, or -1 when no valid session exists
     */
    private long getCurrentUserId() {
        try {
            if (!sessionManager.isLoggedIn()) {
                return NO_USER_ID;
            }
            long userId = sessionManager.getCurrentUserId();
            return userId >= 0L ? userId : NO_USER_ID;
        } catch (ClassCastException exception) {
            sessionManager.clearSession();
            return NO_USER_ID;
        }
    }

    /**
     * Converts a domain track to its Room representation.
     *
     * @param track source track
     * @return database entity
     */
    private TrackEntity toTrackEntity(Track track) {
        TrackEntity entity = new TrackEntity();
        entity.setId(track.getId());
        entity.setName(track.getName());
        entity.setArtistName(track.getArtistName());
        entity.setAlbumName(track.getAlbumName());
        entity.setDuration(track.getDuration());
        entity.setAudioUrl(track.getAudioUrl());
        entity.setDownloadUrl(track.getDownloadUrl());
        entity.setImageUrl(track.getImageUrl());
        entity.setLicenseUrl(track.getLicenseUrl());
        entity.setFavorite(true);
        entity.setLocalFilePath(track.getLocalFilePath());
        return entity;
    }

    /**
     * Converts stored metadata back into a favorite track.
     *
     * @param entity stored track metadata
     * @return favorite track
     */
    private Track toTrack(TrackEntity entity) {
        return new Track(
                entity.getId(),
                entity.getName(),
                entity.getArtistName(),
                entity.getAlbumName(),
                entity.getDuration(),
                entity.getAudioUrl(),
                entity.getDownloadUrl(),
                entity.getImageUrl(),
                entity.getLicenseUrl(),
                true,
                entity.getLocalFilePath()
        );
    }

    /**
     * Checks whether a text value is missing.
     *
     * @param value value to check
     * @return true for null or blank values
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
