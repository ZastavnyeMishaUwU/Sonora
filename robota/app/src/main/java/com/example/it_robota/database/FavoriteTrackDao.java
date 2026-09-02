package com.example.it_robota.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

/**
 * Provides database access operations for managing favorite tracks.
 * Handles inserting track details, linking tracks to specific users,
 * removing favorites, and checking favorite statuses in the local Room database.
 */
@Dao
public interface FavoriteTrackDao {

    @Query("SELECT * FROM tracks WHERE id IN (SELECT trackId FROM favorite_tracks "
            + "WHERE userId = :userId AND ownerEmail = :email) ORDER BY name COLLATE NOCASE")
    List<TrackEntity> getFavoriteTracksByAccount(long userId, String email);

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE trackId = :trackId "
            + "AND userId = :userId AND ownerEmail = :email)")
    boolean isTrackFavoriteForAccount(String trackId, long userId, String email);

    @Query("DELETE FROM favorite_tracks WHERE trackId = :trackId AND userId = :userId AND ownerEmail = :email")
    void deleteForAccount(String trackId, long userId, String email);

    /**
     * Inserts a track into the tracks table. Replaces it if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrack(TrackEntity track);

    /**
     * Deletes a specific track from the database using its ID.
     */
    @Query("DELETE FROM favorite_tracks WHERE trackId = :trackId AND userId = :userId")
    void deleteTrack(String trackId, long userId);

    /**
     * Saves a link between a user and their favorite track. Replaces on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavoriteLink(FavoriteTrackEntity favorite);

    /**
     * Stores track metadata and the user-specific favorite link atomically.
     *
     * @param track track metadata
     * @param favorite user-to-track favorite link
     */
    @Transaction
    default void saveFavorite(TrackEntity track, FavoriteTrackEntity favorite) {
        insertTrack(track);
        insertFavoriteLink(favorite);
    }

    /**
     * Retrieves all favorite tracks belonging to a specific user.
     */
    @Query("SELECT * FROM tracks WHERE id IN (SELECT trackId FROM favorite_tracks WHERE userId = :userId) "
            + "ORDER BY name COLLATE NOCASE")
    List<TrackEntity> getFavoriteTracksByUser(long userId);

    /**
     * Checks if a specific track is already marked as a favorite by a user.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE trackId = :trackId AND userId = :userId)")
    boolean isTrackFavorite(String trackId, long userId);

    /**
     * Removes stored track metadata after its favorite links have been cleaned up.
     *
     * @param trackId track identifier
     */
    @Query("DELETE FROM tracks WHERE id = :trackId")
    void deleteTrackRecord(String trackId);

}
