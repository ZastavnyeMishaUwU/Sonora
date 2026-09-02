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
     * Returns download records matching both parts of the account key.
     * This query does not check whether the audio files still exist.
     *
     * @param userId database user identifier
     * @param email normalized owner email
     * @return matching download records
     */
    @Query("SELECT * FROM downloaded_tracks WHERE userId = :userId AND ownerEmail = :email")
    List<DownloadedTrackEntity> getDownloadsByAccount(long userId, String email);

    /**
     * Finds a download belonging to the specified account.
     *
     * @param trackId track identifier
     * @param userId database user identifier
     * @param email normalized owner email
     * @return matching record, or null if none exists
     */
    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId AND userId = :userId "
            + "AND ownerEmail = :email LIMIT 1")
    DownloadedTrackEntity getDownloadByAccount(String trackId, long userId, String email);

    /**
     * Removes only the specified account's record; the audio file is not deleted here.
     *
     * @param trackId track identifier
     * @param userId database user identifier
     * @param email normalized owner email
     */
    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId AND userId = :userId AND ownerEmail = :email")
    void deleteForAccount(String trackId, long userId, String email);

    /**
     * Counts references across all owners, including unassigned legacy records.
     * Used before deleting a file that older records may share.
     *
     * @param path stored local file path
     * @return number of records referencing that path
     */
    @Query("SELECT COUNT(*) FROM downloaded_tracks WHERE localPath = :path")
    int countFileReferences(String path);

    /**
     * Saves a downloaded track record, replacing an existing matching record.
     *
     * @param downloadedTrack downloaded track record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDownloadedTrack(DownloadedTrackEntity downloadedTrack);

    /**
     * Removes records by user ID without filtering their owner email.
     * Use {@link #deleteForAccount(String, long, String)} for account-scoped removal.
     *
     * @param trackId track identifier
     * @param userId user identifier
     */
    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId AND userId = :userId")
    void deleteDownloadedTrack(String trackId, long userId);

    /**
     * Returns records by user ID, including those with an unassigned owner email.
     * Use {@link #getDownloadsByAccount(long, String)} for an account's visible list.
     *
     * @param userId user identifier
     * @return downloaded track records
     */
    @Query("SELECT * FROM downloaded_tracks WHERE userId = :userId")
    List<DownloadedTrackEntity> getDownloadedTracks(long userId);

    /**
     * Returns one record by user ID without checking the session or owner email.
     *
     * @param trackId track identifier
     * @param userId user identifier
     * @return matching record, or null when none exists
     */
    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId AND userId = :userId LIMIT 1")
    DownloadedTrackEntity getDownloadedTrack(String trackId, long userId);

    /**
     * Checks for a record by user ID without checking its owner email or audio file.
     *
     * @param trackId track identifier
     * @param userId user identifier
     * @return true when a matching database record exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_tracks WHERE trackId = :trackId AND userId = :userId)")
    boolean isTrackDownloaded(String trackId, long userId);
}
