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

    /**
     * Returns an account's favorite tracks, sorted by name without case sensitivity.
     *
     * @param userId database user identifier
     * @param email normalized owner email
     * @return track metadata linked to the account
     */
    @Query("SELECT * FROM tracks WHERE id IN (SELECT trackId FROM favorite_tracks "
            + "WHERE userId = :userId AND ownerEmail = :email) ORDER BY name COLLATE NOCASE")
    List<TrackEntity> getFavoriteTracksByAccount(long userId, String email);

    /**
     * Checks whether the specified account has saved a track.
     *
     * @param trackId track identifier
     * @param userId database user identifier
     * @param email normalized owner email
     * @return true when the account's favorite link exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE trackId = :trackId "
            + "AND userId = :userId AND ownerEmail = :email)")
    boolean isTrackFavoriteForAccount(String trackId, long userId, String email);

    /**
     * Removes one account's favorite link without deleting shared track metadata.
     *
     * @param trackId track identifier
     * @param userId database user identifier
     * @param email normalized owner email
     */
    @Query("DELETE FROM favorite_tracks WHERE trackId = :trackId AND userId = :userId AND ownerEmail = :email")
    void deleteForAccount(String trackId, long userId, String email);

    /**
     * Inserts shared track metadata, replacing the record if its ID already exists.
     *
     * @param track track metadata to store
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrack(TrackEntity track);

    /**
     * Deletes favorite links by user ID without filtering their owner email.
     * Use {@link #deleteForAccount(String, long, String)} for account-scoped removal.
     *
     * @param trackId track identifier
     * @param userId database user identifier
     */
    @Query("DELETE FROM favorite_tracks WHERE trackId = :trackId AND userId = :userId")
    void deleteTrack(String trackId, long userId);

    /**
     * Saves a favorite link, replacing a link with the same user ID, email and track ID.
     *
     * @param favorite account-to-track link with its owner email set
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
     * Returns favorites by user ID, including links with an unassigned owner email.
     * Use {@link #getFavoriteTracksByAccount(long, String)} for an account's visible list.
     *
     * @param userId database user identifier
     * @return matching track metadata, sorted by name
     */
    @Query("SELECT * FROM tracks WHERE id IN (SELECT trackId FROM favorite_tracks WHERE userId = :userId) "
            + "ORDER BY name COLLATE NOCASE")
    List<TrackEntity> getFavoriteTracksByUser(long userId);

    /**
     * Checks for a favorite link by user ID without filtering its owner email.
     *
     * @param trackId track identifier
     * @param userId database user identifier
     * @return true when a matching favorite link exists
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
