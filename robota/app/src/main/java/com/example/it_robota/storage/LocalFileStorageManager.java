package com.example.it_robota.storage;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Owns local file operations inside the application's music directory.
 */
public class LocalFileStorageManager {

    private static final String FILE_PREFIX = "track_";
    private static final String FILE_EXTENSION = ".mp3";
    private static final String FALLBACK_DIRECTORY_NAME = "music";

    private final Context context;

    /**
     * Creates a storage manager using application-specific storage.
     *
     * @param context application or activity context
     */
    public LocalFileStorageManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Returns the application-specific directory used for downloaded music.
     *
     * @return music directory
     */
    public File getMusicDirectory() {
        File externalDirectory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File musicDirectory = externalDirectory != null
                ? externalDirectory
                : new File(context.getFilesDir(), FALLBACK_DIRECTORY_NAME);

        if (!musicDirectory.exists()) {
            musicDirectory.mkdirs();
        }

        return musicDirectory;
    }

    /**
     * Builds a consistent local audio file path for a track.
     *
     * @param trackId track identifier
     * @return absolute path for the track file
     */
    public String buildFilePath(String trackId) {
        String safeTrackId = sanitizeTrackId(trackId);
        File trackFile = new File(
                getMusicDirectory(),
                FILE_PREFIX + safeTrackId + FILE_EXTENSION
        );
        return trackFile.getAbsolutePath();
    }

    /**
     * Checks whether a regular file exists at a path.
     *
     * @param filePath absolute file path
     * @return true when the file exists
     */
    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        try {
            File file = new File(filePath);
            return isInsideMusicDirectory(file) && file.isFile();
        } catch (IOException | SecurityException exception) {
            return false;
        }
    }

    /**
     * Deletes a file without failing when it is already missing.
     *
     * @param filePath absolute file path
     * @return true when the file is deleted or already missing
     */
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return true;
        }

        try {
            File file = new File(filePath);

            if (!isInsideMusicDirectory(file)) {
                return false;
            }

            return !file.exists() || file.delete();
        } catch (IOException | SecurityException exception) {
            return false;
        }
    }

    /**
     * Verifies that a file resolves inside the managed music directory.
     *
     * @param file file whose canonical location should be checked
     * @return true when the file is located inside the music directory
     * @throws IOException if a canonical path cannot be resolved
     */
    private boolean isInsideMusicDirectory(File file) throws IOException {
        String directoryPath = getMusicDirectory().getCanonicalPath() + File.separator;
        String filePath = file.getCanonicalPath();
        return filePath.startsWith(directoryPath);
    }

    /**
     * Converts a track identifier into a safe file-name component.
     *
     * @param trackId original track identifier
     * @return sanitized identifier or a fallback value when it is missing
     */
    private String sanitizeTrackId(String trackId) {
        if (trackId == null || trackId.trim().isEmpty()) {
            return "unknown";
        }

        return trackId.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
