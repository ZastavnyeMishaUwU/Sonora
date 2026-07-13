package com.example.it_robota.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FavoriteTrackDao {
    /**
     * Inserts a track into the tracks table. Replaces it if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrack(TrackEntity track);
    /**
     * Deletes a specific track from the database using its ID.
     */
    @Query("DELETE FROM tracks WHERE id = :trackId")
    void deleteTrack(String trackId);
    /**
     * Saves a link between a user and their favorite track. Replaces on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavoriteLink(FavoriteTrackEntity favorite);
    /**
     * Retrieves all favorite tracks belonging to a specific user.
     */
    @Query("SELECT * FROM tracks WHERE id IN (SELECT trackId FROM favorite_tracks WHERE userId = :userId)")
    List<TrackEntity> getFavoriteTracksByUser(long userId);
    /**
     * Checks if a specific track is already marked as a favorite by a user.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE trackId = :trackId AND userId = :userId)")
    boolean isTrackFavorite(String trackId, long userId);

}
