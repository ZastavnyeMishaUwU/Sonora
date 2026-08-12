package com.example.it_robota.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

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

    private String trackName;

    private String artistName;

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
    @Ignore
    public DownloadedTrackEntity(long userId, @NonNull String trackId, String localPath) {
        this(userId, trackId, null, null, localPath);
    }

    /**
     * Creates a downloaded track record with metadata required for offline display.
     *
     * @param userId user identifier
     * @param trackId track identifier
     * @param trackName track name
     * @param artistName artist name
     * @param localPath local audio file path
     */
    @Ignore
    public DownloadedTrackEntity(long userId,
                                 @NonNull String trackId,
                                 String trackName,
                                 String artistName,
                                 String localPath) {
        this.userId = userId;
        this.trackId = trackId;
        this.trackName = trackName;
        this.artistName = artistName;
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
     * Returns the track name stored for offline display.
     *
     * @return track name
     */
    public String getTrackName() {
        return trackName;
    }

    /**
     * Sets the track name stored for offline display.
     *
     * @param trackName track name
     */
    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    /**
     * Returns the artist name stored for offline display.
     *
     * @return artist name
     */
    public String getArtistName() {
        return artistName;
    }

    /**
     * Sets the artist name stored for offline display.
     *
     * @param artistName artist name
     */
    public void setArtistName(String artistName) {
        this.artistName = artistName;
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
