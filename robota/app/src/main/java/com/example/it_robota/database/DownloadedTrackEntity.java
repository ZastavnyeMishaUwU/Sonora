package com.example.it_robota.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_tracks")
public class DownloadedTrackEntity {
    @PrimaryKey
    private String trackId;
    private String localPath;
    public DownloadedTrackEntity() {}
    public DownloadedTrackEntity(String trackId, String localPath) {
        this.trackId = trackId;
        this.localPath = localPath;
    }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
}