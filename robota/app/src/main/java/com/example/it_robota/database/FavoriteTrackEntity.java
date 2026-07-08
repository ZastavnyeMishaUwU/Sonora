package com.example.it_robota.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Entity for storing favorite tracks for users.
 */
@Entity(
        tableName = "favorite_tracks",
        primaryKeys = {"userId", "trackId"}
)
public class FavoriteTrackEntity {

    private long userId;

    @NonNull
    private String trackId = "";

    /**
     * Creates an empty FavoriteTrackEntity object.
     */
    public FavoriteTrackEntity() {
    }

    /**
     * Creates a FavoriteTrackEntity object.
     *
     * @param userId user ID
     * @param trackId track ID
     */
    public FavoriteTrackEntity(long userId, @NonNull String trackId) {
        this.userId = userId;
        this.trackId = trackId;
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
}