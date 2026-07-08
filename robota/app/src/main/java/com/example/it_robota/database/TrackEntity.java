package com.example.it_robota.database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "tracks")
public class TrackEntity {
    @PrimaryKey
    private String id;
    private String name;
    private String artistName;
    private String albumName;
    private int duration;
    private String audioUrl;
    private String downloadUrl;
    private String imageUrl;
    private String licenseUrl;
    private boolean isFavorite;
    private String localFilePath;
    public TrackEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    public String getAlbumName() { return albumName; }
    public void setAlbumName(String albumName) { this.albumName = albumName; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }
}