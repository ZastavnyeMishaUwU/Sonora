package com.example.it_robota.downloader;

import android.content.Context;
import android.util.Log;

import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.auth.AccountSession;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.DownloadedTrackEntity;
import com.example.it_robota.models.Track;
import com.example.it_robota.storage.LocalFileStorageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Manager responsible for downloading audio files to the device.
 */
public class TrackDownloadManager {

    private static final String TAG = "TrackDownloadManager";

    private final Context context;
    private final AppDatabase database;
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
        this.localFileStorageManager = new LocalFileStorageManager(this.context);
    }

    /**
     * Downloads a track for the current session and stores its local path in Room.
     * Must be called off the UI thread.
     *
     * @param track track with a download URL
     * @throws Exception if the track, session, download or database write is invalid or fails
     */
    public void downloadTrack(Track track) throws Exception {
        downloadTrack(track, sessionManager.getAccount());
    }

    /**
     * Downloads audio to an account-specific file, reusing an existing nonempty file.
     * The session is checked before starting; a later account switch does not transfer ownership
     * of the download. The supplied track's local path is updated only if that session is current.
     * Must be called off the UI thread.
     *
     * @param track track metadata and download URL
     * @param account session captured before queuing the download
     * @throws Exception if validation, downloading, file handling or database storage fails
     */
    public void downloadTrack(Track track, AccountSession account) throws Exception {
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
        if (!sessionManager.isCurrent(account)) {
            throw new Exception("User is not logged in. Downloads require an active session.");
        }

        long userId = account.getUserId();
        File localFile = new File(localFileStorageManager.buildFilePath(account.getEmail(), trackId));
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
        entity.setOwnerEmail(account.getEmail());

        try {
            database.downloadedTrackDao().insertDownloadedTrack(entity);
        } catch (Exception exception) {
            if (!existingFile) {
                deleteIfPresent(localFile);
            }
            throw new Exception("Downloaded track could not be saved.", exception);
        }

        if (sessionManager.isCurrent(account)) {
            track.setLocalFilePath(localFile.getAbsolutePath());
        }
    }

    /**
     * Checks for a nonempty audio file belonging to the current account.
     * Removes the account's stale record if its file is missing or empty.
     * Must be called off the UI thread.
     *
     * @param trackId track identifier
     * @return true when the account has a record and an available nonempty file
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

        database.downloadedTrackDao().deleteForAccount(
                entity.getTrackId(),
                entity.getUserId(), entity.getOwnerEmail()
        );
        return false;
    }

    /**
     * Resolves an available local file for the current session off the UI thread.
     *
     * @param trackId track identifier
     * @return local path, or null when the account has no available download
     */
    public String getLocalFilePath(String trackId) {
        return getLocalFilePath(trackId, sessionManager.getAccount());
    }

    /**
     * Resolves the captured account's nonempty audio file off the UI thread.
     * Deletes a stale database record when its file is missing or empty.
     *
     * @param trackId track identifier
     * @param account session captured before the lookup
     * @return local path, or null if the session is stale or no usable file is found
     */
    public String getLocalFilePath(String trackId, AccountSession account) {
        DownloadedTrackEntity entity = getDownloadedTrack(trackId, account);
        if (entity == null) {
            return null;
        }

        String localPath = entity.getLocalPath();
        if (!localFileStorageManager.fileExists(localPath) || new File(localPath).length() == 0) {
            database.downloadedTrackDao().deleteForAccount(
                    entity.getTrackId(),
                    entity.getUserId(), entity.getOwnerEmail()
            );
            return null;
        }

        return localPath;
    }

    /**
     * Writes an HTTP response to a temporary file and checks for a nonempty MP3 header.
     * The caller is responsible for removing partial files after a failure.
     *
     * @param downloadUrl audio URL to request
     * @param targetFile temporary file to write
     * @throws Exception if the request, file write or header check fails
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
     * This checks only the prefix, not whether the entire file can be decoded.
     *
     * @param header initial bytes read from the response
     * @param headerLength number of populated bytes in the buffer
     * @return true when the prefix matches an ID3 tag or MPEG frame sync
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
     * Reads a download record for the current session without checking its file.
     *
     * @param trackId track identifier
     * @return matching account-owned record, or null
     */
    private DownloadedTrackEntity getDownloadedTrack(String trackId) {
        return getDownloadedTrack(trackId, sessionManager.getAccount());
    }

    /**
     * Reads a record by user ID and email after checking the captured session.
     *
     * @param trackId track identifier
     * @param account session captured before the lookup
     * @return record, or null for an invalid ID, stale session or missing download
     */
    private DownloadedTrackEntity getDownloadedTrack(String trackId, AccountSession account) {
        if (trackId == null || trackId.trim().isEmpty() || !sessionManager.isCurrent(account)) {
            return null;
        }
        return database.downloadedTrackDao().getDownloadByAccount(trackId, account.getUserId(), account.getEmail());
    }

    /**
     * Reads the captured account's download records off the UI thread.
     * Files are not checked here; the caller must recheck the session before displaying results.
     *
     * @param account session captured before queuing the read
     * @return account-owned records, or an empty list if the session is no longer current
     */
    public List<DownloadedTrackEntity> getDownloadedTracks(AccountSession account) {
        return sessionManager.isCurrent(account)
                ? database.downloadedTrackDao().getDownloadsByAccount(account.getUserId(), account.getEmail())
                : Collections.emptyList();
    }

    /**
     * Removes an account's download off the UI thread, retaining files referenced by other records.
     * Does nothing if the session is stale or no matching record exists.
     *
     * @param trackId track identifier
     * @param account session captured before queuing the removal
     * @throws Exception if an unshared file cannot be deleted or the database operation fails
     */
    public void removeDownload(String trackId, AccountSession account) throws Exception {
        DownloadedTrackEntity track = getDownloadedTrack(trackId, account);
        if (track == null) { return; }
        if (database.downloadedTrackDao().countFileReferences(track.getLocalPath()) <= 1
                && !localFileStorageManager.deleteFile(track.getLocalPath())) {
            throw new Exception("Downloaded file could not be removed.");
        }
        database.downloadedTrackDao().deleteForAccount(trackId, account.getUserId(), account.getEmail());
    }

    /**
     * Moves a completed temporary download into its final location.
     *
     * @param temporaryFile completed download
     * @param targetFile final account-specific audio file
     * @throws Exception if an existing target cannot be deleted or the move fails
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
     * Removes an exact download file when it exists, logging a failed deletion.
     *
     * @param file temporary or newly downloaded file to clean up
     */
    private void deleteIfPresent(File file) {
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Temporary download file could not be deleted");
        }
    }
}
