package com.example.it_robota.api;

import com.example.it_robota.models.Track;

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

    // Default empty constructor required for serialization/deserialization
    public JamendoTrackDto() {
    }

    // Constructor with all fields to easily instantiate track DTOs
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

    /*
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public String getAudioDownload() {
        return audioDownload;
    }

    public void setAudioDownload(String audioDownload) {
        this.audioDownload = audioDownload;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }
}