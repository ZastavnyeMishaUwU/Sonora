package com.example.it_robota.downloader;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.repositories.AuthRepository;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.DownloadedTrackEntity;
import com.example.it_robota.models.Track;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Manager responsible for downloading audio files to the device.
 */
public class TrackDownloadManager {

    private static final String TAG = "TrackDownloadManager";

    private final Context context;
    private final AppDatabase database;
    private final SessionManager sessionManager;

    /**
     * Creates a TrackDownloadManager instance.
     *
     * @param context application context
     */
    public TrackDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.context);
        this.sessionManager = new SessionManager(this.context);
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
        String downloadUrl = track.getDownloadUrl();
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            throw new Exception("Track download URL is missing. Cannot download.");
        }

        long userId = sessionManager.getCurrentUserId();
        if (userId == -1) {
            throw new Exception("User is not logged in. Downloads require an active session.");
        }

        File musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicDir == null) {
            throw new Exception("External storage for music is not available.");
        }

        if (!musicDir.exists() && !musicDir.mkdirs()) {
            Log.w(TAG, "Directory already exists or could not be created");
        }

        String fileName = "track_" + track.getId() + ".mp3";
        File localFile = new File(musicDir, fileName);

        Log.d(TAG, "Starting stream download from: " + downloadUrl);
        downloadAudioStream(downloadUrl, localFile);
        Log.d(TAG, "Stream saved to: " + localFile.getAbsolutePath());

        DownloadedTrackEntity entity = new DownloadedTrackEntity(
                userId,
                track.getId(),
                localFile.getAbsolutePath()
        );

        database.downloadedTrackDao().insertDownloadedTrack(entity);

        track.setLocalFilePath(localFile.getAbsolutePath());
    }

    /**
     * Acceptance Criteria: App can check whether a track is already downloaded.
     */
    public boolean isTrackDownloaded(String trackId) {
        String localPath = getLocalFilePath(trackId);
        if (localPath == null) return false;
        
        File file = new File(localPath);
        return file.exists();
    }

    /**
     * Returns the local file path from Room database.
     * This fulfills the requirement to "store local file path in Room".
     */
    public String getLocalFilePath(String trackId) {
        long userId = sessionManager.getCurrentUserId();
        if (userId == -1) return null;

        List<DownloadedTrackEntity> tracks = database.downloadedTrackDao()
                .getDownloadedTracks(userId);

        for (DownloadedTrackEntity entity : tracks) {
            if (entity.getTrackId().equals(trackId)) {
                return entity.getLocalPath();
            }
        }

        return null;
    }

    /**
     * Internal helper to handle the actual network stream (Step 3).
     */
    private void downloadAudioStream(String downloadUrl, File targetFile) throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;

        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Server error " + connection.getResponseCode());
            }

            input = connection.getInputStream();
            output = new FileOutputStream(targetFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();

        } finally {
            if (output != null) output.close();
            if (input != null) input.close();
            if (connection != null) connection.disconnect();
        }
    }
}