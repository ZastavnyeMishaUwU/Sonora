package com.example.it_robota.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Provides database operations for tracks downloaded by a user.
 */
@Dao
public interface DownloadedTrackDao {

    /**
     * Saves a downloaded track record, replacing an existing matching record.
     *
     * @param downloadedTrack downloaded track record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDownloadedTrack(DownloadedTrackEntity downloadedTrack);

    /**
     * Returns one downloaded track for a user.
     *
     * @param userId user identifier
     * @param trackId track identifier
     * @return downloaded track record or null when it does not exist
     */
    @Query("SELECT * FROM downloaded_tracks WHERE userId = :userId AND trackId = :trackId LIMIT 1")
    DownloadedTrackEntity getDownloadedTrack(long userId, String trackId);

    /**
     * Removes a downloaded track record for a user.
     *
     * @param trackId track identifier
     * @param userId user identifier
     */
    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId AND userId = :userId")
    void deleteDownloadedTrack(String trackId, long userId);

    /**
     * Returns all downloaded track records belonging to a user.
     *
     * @param userId user identifier
     * @return downloaded track records
     */
    @Query("SELECT * FROM downloaded_tracks WHERE userId = :userId")
    List<DownloadedTrackEntity> getDownloadedTracks(long userId);

    /**
     * Checks whether a track has already been downloaded by a user.
     *
     * @param trackId track identifier
     * @param userId user identifier
     * @return true when the track is downloaded
     */
    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_tracks WHERE trackId = :trackId AND userId = :userId)")
    boolean isTrackDownloaded(String trackId, long userId);
}
