package com.example.it_robota.repositories;

import android.content.Context;

import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.auth.AccountSession;
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
     * @throws Exception if the API request or parsing fails
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
        return getTrackDetails(trackId, sessionManager.getAccount());
    }

    public Track getTrackDetails(String trackId, AccountSession account) throws Exception {
        Track track = jamendoApiClient.getTrackDetails(trackId);
        if (track != null) {
            track.setFavorite(isTrackFavorite(track.getId(), account));
            track.setLocalFilePath(null);
        }
        return track;
    }

    /**
     * Returns favorite tracks saved for the active account.
     *
     * @return current user's favorite tracks
     */
    public List<Track> getSavedTracks() {
        return getSavedTracks(sessionManager.getAccount());
    }

    public List<Track> getSavedTracks(AccountSession account) {
        List<Track> tracks = new ArrayList<>();
        if (!sessionManager.isCurrent(account)) {
            return tracks;
        }

        for (TrackEntity entity : favoriteTrackDao.getFavoriteTracksByAccount(account.getUserId(), account.getEmail())) {
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
        saveFavorite(track, sessionManager.getAccount());
    }

    public void saveFavorite(Track track, AccountSession account) throws Exception {
        if (track == null || isBlank(track.getId())) {
            throw new Exception("Track is empty.");
        }

        if (!sessionManager.isCurrent(account)) {
            throw new Exception("User is not logged in.");
        }

        FavoriteTrackEntity favorite = new FavoriteTrackEntity(account.getUserId(), track.getId());
        favorite.setOwnerEmail(account.getEmail());
        favoriteTrackDao.saveFavorite(toTrackEntity(track), favorite);
        track.setFavorite(true);
    }

    /**
     * Removes a track from the active account's favorites.
     *
     * @param trackId track identifier
     */
    public void removeFavorite(String trackId) {
        removeFavorite(trackId, sessionManager.getAccount());
    }

    public void removeFavorite(String trackId, AccountSession account) {
        if (!sessionManager.isCurrent(account) || isBlank(trackId)) {
            return;
        }
        favoriteTrackDao.deleteForAccount(trackId, account.getUserId(), account.getEmail());
    }

    /**
     * Checks whether a track is a favorite of the active account.
     *
     * @param trackId track identifier
     * @return true when the current user saved the track
     */
    public boolean isTrackFavorite(String trackId) {
        return isTrackFavorite(trackId, sessionManager.getAccount());
    }

    /** Checks a favorite against the captured, still-active account. */
    private boolean isTrackFavorite(String trackId, AccountSession account) {
        return sessionManager.isCurrent(account) && !isBlank(trackId)
                && favoriteTrackDao.isTrackFavoriteForAccount(trackId, account.getUserId(), account.getEmail());
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
        // Track metadata is shared. Download paths belong only in account-owned records.
        entity.setLocalFilePath(null);
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
                null
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
