package com.example.it_robota.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_tracks")
public class FavoriteTrackEntity {
    @PrimaryKey
    private String trackId;
    private long userId;
    public FavoriteTrackEntity() {}
    public FavoriteTrackEntity(String trackId, long userId) {
        this.trackId = trackId;
        this.userId = userId;
    }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}