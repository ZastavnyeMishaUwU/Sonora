package com.example.it_robota.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Entity for storing downloaded track information.
 */
@Entity(
        tableName = "downloaded_tracks",
        primaryKeys = {"userId", "trackId"}
)
public class DownloadedTrackEntity {

    private long userId;

    @NonNull
    private String trackId = "";

    private String localPath;

    /**
     * Creates an empty DownloadedTrackEntity object.
     */
    public DownloadedTrackEntity() {
    }

    /**
     * Creates a DownloadedTrackEntity object.
     *
     * @param userId user ID
     * @param trackId track ID
     * @param localPath local file path
     */
    public DownloadedTrackEntity(long userId, @NonNull String trackId, String localPath) {
        this.userId = userId;
        this.trackId = trackId;
        this.localPath = localPath;
    }

    /**
     * Returns user ID.
     *
     * @return user ID
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Sets user ID.
     *
     * @param userId user ID
     */
    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * Returns track ID.
     *
     * @return track ID
     */
    @NonNull
    public String getTrackId() {
        return trackId;
    }

    /**
     * Sets track ID.
     *
     * @param trackId track ID
     */
    public void setTrackId(@NonNull String trackId) {
        this.trackId = trackId;
    }

    /**
     * Returns local file path.
     *
     * @return local file path
     */
    public String getLocalPath() {
        return localPath;
    }

    /**
     * Sets local file path.
     *
     * @param localPath local file path
     */
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
}