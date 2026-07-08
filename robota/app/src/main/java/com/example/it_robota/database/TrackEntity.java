package com.example.it_robota.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity for storing track data in the Room database.
 */
@Entity(tableName = "tracks")
public class TrackEntity {

    @PrimaryKey
    @NonNull
    private String id = "";

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

    /**
     * Creates an empty TrackEntity object.
     */
    public TrackEntity() {
    }

    /**
     * Returns track ID.
     *
     * @return track ID
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Sets track ID.
     *
     * @param id track ID
     */
    public void setId(@NonNull String id) {
        this.id = id;
    }

    /**
     * Returns track name.
     *
     * @return track name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets track name.
     *
     * @param name track name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns artist name.
     *
     * @return artist name
     */
    public String getArtistName() {
        return artistName;
    }

    /**
     * Sets artist name.
     *
     * @param artistName artist name
     */
    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    /**
     * Returns album name.
     *
     * @return album name
     */
    public String getAlbumName() {
        return albumName;
    }

    /**
     * Sets album name.
     *
     * @param albumName album name
     */
    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    /**
     * Returns track duration in seconds.
     *
     * @return track duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Sets track duration in seconds.
     *
     * @param duration track duration
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Returns audio streaming URL.
     *
     * @return audio URL
     */
    public String getAudioUrl() {
        return audioUrl;
    }

    /**
     * Sets audio streaming URL.
     *
     * @param audioUrl audio URL
     */
    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    /**
     * Returns audio download URL.
     *
     * @return download URL
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets audio download URL.
     *
     * @param downloadUrl download URL
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Returns image URL.
     *
     * @return image URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets image URL.
     *
     * @param imageUrl image URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Returns license URL.
     *
     * @return license URL
     */
    public String getLicenseUrl() {
        return licenseUrl;
    }

    /**
     * Sets license URL.
     *
     * @param licenseUrl license URL
     */
    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    /**
     * Returns favorite status.
     *
     * @return true if track is favorite
     */
    public boolean isFavorite() {
        return isFavorite;
    }

    /**
     * Sets favorite status.
     *
     * @param favorite favorite status
     */
    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    /**
     * Returns local file path.
     *
     * @return local file path
     */
    public String getLocalFilePath() {
        return localFilePath;
    }

    /**
     * Sets local file path.
     *
     * @param localFilePath local file path
     */
    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }
}