package com.example.it_robota.models;

/**
 * Represents a music track inside the application.
 * Stores track metadata, audio links, favorite status and local download information.
 */
public class Track {

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

    /**
     * Creates an empty Track object.
     */
    public Track() {
    }

    /**
     * Creates a Track object with all fields.
     *
     * @param id track ID
     * @param name track name
     * @param artistName artist name
     * @param albumName album name
     * @param duration track duration in seconds
     * @param audioUrl audio streaming URL
     * @param downloadUrl audio download URL
     * @param imageUrl track or album image URL
     * @param licenseUrl license information URL
     * @param isFavorite favorite status
     * @param localFilePath local file path if track is downloaded
     */
    public Track(String id,
                 String name,
                 String artistName,
                 String albumName,
                 int duration,
                 String audioUrl,
                 String downloadUrl,
                 String imageUrl,
                 String licenseUrl,
                 boolean isFavorite,
                 String localFilePath) {
        this.id = id;
        this.name = name;
        this.artistName = artistName;
        this.albumName = albumName;
        this.duration = duration;
        this.audioUrl = audioUrl;
        this.downloadUrl = downloadUrl;
        this.imageUrl = imageUrl;
        this.licenseUrl = licenseUrl;
        this.isFavorite = isFavorite;
        this.localFilePath = localFilePath;
    }

    /**
     * Returns the track ID.
     *
     * @return track ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the track ID.
     *
     * @param id track ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the track name.
     *
     * @return track name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the track name.
     *
     * @param name track name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the artist name.
     *
     * @return artist name
     */
    public String getArtistName() {
        return artistName;
    }

    /**
     * Sets the artist name.
     *
     * @param artistName artist name
     */
    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    /**
     * Returns the album name.
     *
     * @return album name
     */
    public String getAlbumName() {
        return albumName;
    }

    /**
     * Sets the album name.
     *
     * @param albumName album name
     */
    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    /**
     * Returns the track duration in seconds.
     *
     * @return duration in seconds
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Sets the track duration in seconds.
     *
     * @param duration duration in seconds
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Returns the audio streaming URL.
     *
     * @return audio URL
     */
    public String getAudioUrl() {
        return audioUrl;
    }

    /**
     * Sets the audio streaming URL.
     *
     * @param audioUrl audio URL
     */
    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    /**
     * Returns the audio download URL.
     *
     * @return download URL
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets the audio download URL.
     *
     * @param downloadUrl download URL
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Returns the track or album image URL.
     *
     * @return image URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the track or album image URL.
     *
     * @param imageUrl image URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Returns the license information URL.
     *
     * @return license URL
     */
    public String getLicenseUrl() {
        return licenseUrl;
    }

    /**
     * Sets the license information URL.
     *
     * @param licenseUrl license URL
     */
    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    /**
     * Returns whether the track is marked as favorite.
     *
     * @return true if track is favorite, false otherwise
     */
    public boolean isFavorite() {
        return isFavorite;
    }

    /**
     * Sets the favorite status of the track.
     *
     * @param favorite favorite status
     */
    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    /**
     * Returns the local file path if the track is downloaded.
     *
     * @return local file path
     */
    public String getLocalFilePath() {
        return localFilePath;
    }

    /**
     * Sets the local file path for a downloaded track.
     *
     * @param localFilePath local file path
     */
    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    /**
     * Checks whether the track has a valid ID.
     *
     * @return true if track ID is not empty
     */
    public boolean hasId() {
        return id != null && !id.trim().isEmpty();
    }

    /**
     * Checks whether the track has a name.
     *
     * @return true if track name is not empty
     */
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * Checks whether the track has artist information.
     *
     * @return true if artist name is not empty
     */
    public boolean hasArtistName() {
        return artistName != null && !artistName.trim().isEmpty();
    }

    /**
     * Checks whether the track has album information.
     *
     * @return true if album name is not empty
     */
    public boolean hasAlbumName() {
        return albumName != null && !albumName.trim().isEmpty();
    }

    /**
     * Checks whether the track has audio streaming URL.
     *
     * @return true if audio URL is not empty
     */
    public boolean hasAudioUrl() {
        return audioUrl != null && !audioUrl.trim().isEmpty();
    }

    /**
     * Checks whether the track has download URL.
     *
     * @return true if download URL is not empty
     */
    public boolean hasDownloadUrl() {
        return downloadUrl != null && !downloadUrl.trim().isEmpty();
    }

    /**
     * Checks whether the track has image URL.
     *
     * @return true if image URL is not empty
     */
    public boolean hasImageUrl() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    /**
     * Checks whether the track has license URL.
     *
     * @return true if license URL is not empty
     */
    public boolean hasLicenseUrl() {
        return licenseUrl != null && !licenseUrl.trim().isEmpty();
    }

    /**
     * Checks whether the track is downloaded locally.
     *
     * @return true if local file path is not empty
     */
    public boolean isDownloaded() {
        return localFilePath != null && !localFilePath.trim().isEmpty();
    }

    /**
     * Returns readable duration text.
     *
     * @return duration in mm:ss format
     */
    public String getFormattedDuration() {
        if (duration <= 0) {
            return "0:00";
        }

        int minutes = duration / 60;
        int seconds = duration % 60;

        if (seconds < 10) {
            return minutes + ":0" + seconds;
        }

        return minutes + ":" + seconds;
    }

    /**
     * Returns safe track name for UI.
     *
     * @return track name or fallback text
     */
    public String getDisplayName() {
        if (hasName()) {
            return name;
        }

        return "Unknown track";
    }

    /**
     * Returns safe artist name for UI.
     *
     * @return artist name or fallback text
     */
    public String getDisplayArtistName() {
        if (hasArtistName()) {
            return artistName;
        }

        return "Unknown artist";
    }

    /**
     * Returns safe album name for UI.
     *
     * @return album name or fallback text
     */
    public String getDisplayAlbumName() {
        if (hasAlbumName()) {
            return albumName;
        }

        return "Unknown album";
    }

    /**
     * Returns a safe string representation of the track.
     *
     * @return string representation of the track
     */
    @Override
    public String toString() {
        return "Track{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", artistName='" + artistName + '\'' +
                ", albumName='" + albumName + '\'' +
                ", duration=" + duration +
                ", isFavorite=" + isFavorite +
                ", localFilePath='" + localFilePath + '\'' +
                '}';
    }
}