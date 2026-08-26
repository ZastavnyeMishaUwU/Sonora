package com.example.it_robota.downloader;

import android.content.Context;
import android.util.Log;

import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.repositories.AuthRepository;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.DownloadedTrackEntity;
import com.example.it_robota.models.Track;
import com.example.it_robota.storage.LocalFileStorageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Manager responsible for downloading audio files to the device.
 */
public class TrackDownloadManager {

    private static final String TAG = "TrackDownloadManager";

    private final Context context;
    private final AppDatabase database;
    private final AuthRepository authRepository;
    private final SessionManager sessionManager;
    private final LocalFileStorageManager localFileStorageManager;

    /**
     * Creates a TrackDownloadManager instance.
     *
     * @param context application context
     */
    public TrackDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.context);
        this.sessionManager = new SessionManager(this.context);
        this.authRepository = new AuthRepository(database.userDao(), this.sessionManager);
        this.localFileStorageManager = new LocalFileStorageManager(this.context);
    }

    /**
     * Main method to handle the download process.
     * 
     * Required Logic flow:
     * 1. Get track download URL from Track object
     * 2. Create local file destination
     * 3. Download audio stream via HTTP
     * 4. Save file to app-specific music directory
     * 5. Store local file path in Room database
     */
    public void downloadTrack(Track track) throws Exception {
        if (track == null) {
            throw new Exception("Track information is missing.");
        }

        String trackId = track.getId();
        if (trackId == null || trackId.trim().isEmpty()) {
            throw new Exception("Track ID is missing. Cannot download.");
        }

        String downloadUrl = track.getDownloadUrl();
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            throw new Exception("Track download URL is missing. Cannot download.");
        }
        if (!authRepository.isUserLoggedIn()) {
            throw new Exception("User is not logged in. Downloads require an active session.");
        }

        long userId = sessionManager.getCurrentUserId();
        if (userId == -1) {
            throw new Exception("User is not logged in. Downloads require an active session.");
        }

        File localFile = new File(localFileStorageManager.buildFilePath(trackId));
        boolean existingFile = localFileStorageManager.fileExists(localFile.getAbsolutePath())
                && localFile.length() > 0;

        if (!existingFile) {
            File temporaryFile = new File(localFile.getAbsolutePath() + ".part");
            deleteIfPresent(temporaryFile);

            try {
                Log.d(TAG, "Starting track download");
                downloadAudioStream(downloadUrl.trim(), temporaryFile);
                replaceFile(temporaryFile, localFile);
                Log.d(TAG, "Track download completed");
            } catch (Exception exception) {
                deleteIfPresent(temporaryFile);
                throw exception;
            }
        }

        DownloadedTrackEntity entity = new DownloadedTrackEntity(
                userId,
                trackId,
                track.getName(),
                track.getArtistName(),
                localFile.getAbsolutePath()
        );

        try {
            database.downloadedTrackDao().insertDownloadedTrack(entity);
        } catch (Exception exception) {
            if (!existingFile) {
                deleteIfPresent(localFile);
            }
            throw new Exception("Downloaded track could not be saved.", exception);
        }

        track.setLocalFilePath(localFile.getAbsolutePath());
    }

    /**
     * Acceptance Criteria: App can check whether a track is already downloaded.
     */
    public boolean isTrackDownloaded(String trackId) {
        DownloadedTrackEntity entity = getDownloadedTrack(trackId);
        if (entity == null) {
            return false;
        }

        String localPath = entity.getLocalPath();
        if (localFileStorageManager.fileExists(localPath) && new File(localPath).length() > 0) {
            return true;
        }

        database.downloadedTrackDao().deleteDownloadedTrack(
                entity.getTrackId(),
                entity.getUserId()
        );
        return false;
    }

    /**
     * Returns the local file path from Room database.
     * This fulfills the requirement to "store local file path in Room".
     */
    public String getLocalFilePath(String trackId) {
        DownloadedTrackEntity entity = getDownloadedTrack(trackId);
        if (entity == null) {
            return null;
        }

        String localPath = entity.getLocalPath();
        if (!localFileStorageManager.fileExists(localPath) || new File(localPath).length() == 0) {
            database.downloadedTrackDao().deleteDownloadedTrack(
                    entity.getTrackId(),
                    entity.getUserId()
            );
            return null;
        }

        return localPath;
    }

    /**
     * Internal helper to handle the actual network stream (Step 3).
     */
    private void downloadAudioStream(String downloadUrl, File targetFile) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "audio/*,application/octet-stream");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new Exception("Track download failed. Response code: " + responseCode);
            }

            long totalBytes = 0;
            byte[] fileHeader = new byte[3];
            int headerLength = 0;
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    if (headerLength < fileHeader.length) {
                        int headerBytes = Math.min(bytesRead, fileHeader.length - headerLength);
                        System.arraycopy(buffer, 0, fileHeader, headerLength, headerBytes);
                        headerLength += headerBytes;
                    }
                    output.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                output.flush();
            }

            if (totalBytes == 0) {
                throw new Exception("The downloaded audio file is empty.");
            }

            if (!hasMp3Header(fileHeader, headerLength)) {
                throw new Exception("The server did not return a valid MP3 file.");
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Accepts MP3 files beginning with an ID3 tag or an MPEG audio frame.
     */
    private boolean hasMp3Header(byte[] header, int headerLength) {
        if (headerLength < 2) {
            return false;
        }

        boolean hasId3Tag = headerLength >= 3
                && header[0] == 'I'
                && header[1] == 'D'
                && header[2] == '3';
        boolean hasMpegFrame = (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xE0) == 0xE0;

        return hasId3Tag || hasMpegFrame;
    }

    /**
     * Returns the active user's downloaded-track record.
     */
    private DownloadedTrackEntity getDownloadedTrack(String trackId) {
        if (trackId == null || trackId.trim().isEmpty() || !authRepository.isUserLoggedIn()) {
            return null;
        }

        long userId = sessionManager.getCurrentUserId();
        if (userId < 0) {
            return null;
        }

        return database.downloadedTrackDao().getDownloadedTrack(trackId, userId);
    }

    /**
     * Moves a completed temporary download into its final location.
     */
    private void replaceFile(File temporaryFile, File targetFile) throws Exception {
        if (targetFile.exists() && !targetFile.delete()) {
            throw new Exception("The previous downloaded file could not be replaced.");
        }

        if (!temporaryFile.renameTo(targetFile)) {
            throw new Exception("The downloaded file could not be finalized.");
        }
    }

    /**
     * Removes an exact download file when it exists.
     */
    private void deleteIfPresent(File file) {
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Temporary download file could not be deleted");
        }
    }
}
