package com.example.it_robota.api;

import com.example.it_robota.models.Track;

/**
 * Data Transfer Object (DTO) representing a single track from the Jamendo API.
 * Contains raw API response fields and mapping logic to domain models.
 */
public class JamendoTrackDto {

    private String id;
    private String name;
    private String artistName;
    private String albumName;
    private int duration;
    private String audio;
    private String audioDownload;
    private String image;
    private String licenseUrl;

    /**
     * Default empty constructor required for serialization/deserialization
     */
    public JamendoTrackDto() {
    }

    /**
     * Constructor with all fields to easily instantiate track DTOs
     */
    public JamendoTrackDto(String id,
                           String name,
                           String artistName,
                           String albumName,
                           int duration,
                           String audio,
                           String audioDownload,
                           String image,
                           String licenseUrl) {
        this.id = id;
        this.name = name;
        this.artistName = artistName;
        this.albumName = albumName;
        this.duration = duration;
        this.audio = audio;
        this.audioDownload = audioDownload;
        this.image = image;
        this.licenseUrl = licenseUrl;
    }

    /**
     * Converts this API-specific DTO into the clean domain Track model.
     * Sets default values for local UI flags like isFavorite (false) and localPath (null).
     */
    public Track toTrack() {
        return new Track(
                id,
                name,
                artistName,
                albumName,
                duration,
                audio,
                audioDownload,
                image,
                licenseUrl,
                false,
                null
        );
    }

    /**
     * Gets the track ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the track ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the track name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the track name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the artist name
     */
    public String getArtistName() {
        return artistName;
    }

    /**
     * Sets the artist name
     */
    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    /**
     * Gets the album name
     */
    public String getAlbumName() {
        return albumName;
    }

    /**
     * Sets the album name
     */
    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    /**
     * Gets the track duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Sets the track duration
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Gets the streaming audio URL
     */
    public String getAudio() {
        return audio;
    }

    /**
     * Sets the streaming audio URL
     */
    public void setAudio(String audio) {
        this.audio = audio;
    }

    /**
     * Gets the download audio URL
     */
    public String getAudioDownload() {
        return audioDownload;
    }

    /**
     * Sets the download audio URL
     */
    public void setAudioDownload(String audioDownload) {
        this.audioDownload = audioDownload;
    }

    /**
     * Gets the cover image URL
     */
    public String getImage() {
        return image;
    }

    /**
     * Sets the cover image URL
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Gets the license URL
     */
    public String getLicenseUrl() {
        return licenseUrl;
    }

    /**
     * Sets the license URL
     */
    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }
}