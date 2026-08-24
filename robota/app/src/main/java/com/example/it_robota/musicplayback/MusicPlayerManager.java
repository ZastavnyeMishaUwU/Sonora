package com.example.it_robota.musicplayback;

import android.media.MediaPlayer;
import java.io.IOException;

/**
 * Manager class that encapsulates Android MediaPlayer logic for audio playback.
 * - Handles streaming from remote URLs (such as Jamendo API) asynchronously.
 * - Manages playback states (play, pause, resume, stop) safely.
 * - Prevents UI freezing and memory leaks by properly releasing media resources.
 */
public class MusicPlayerManager {

    /**
    * Internal MediaPlayer instance to handle audio streaming
    */
    private MediaPlayer mediaPlayer;

    /**
    * Constructor initializing the MediaPlayer
    */
    public MusicPlayerManager() {
        this(new MediaPlayer());
    }
    MusicPlayerManager(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    /**
     * Helper method to create and initialize MediaPlayer instance.
     */
    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
    }

    /**
     * Plays a track from a remote audio URL (Jamendo API).
     * - Resets the player to allow safe track switching without crashes.
     * - Prepares the audio asynchronously in the background to prevent UI freezing.
     * - Automatically starts playback once the media source is fully loaded.
     */
    public void play(String url) {
        try {
            initMediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * Pauses the current audio playback
    */
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    /**
     * Resumes playback from the paused state
     */
    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    /**
     * Stops the playback and releases MediaPlayer resources.
     */
    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            release();
        }
    }

    /**
     * Returns current playback position in milliseconds.
     */
    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Returns total track duration in milliseconds.
     */
    public int getDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Seeks to specified time position in milliseconds.
     */
    public void seekTo(int msec) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(msec);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if audio is currently playing
     */
    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    /**
     * Releases MediaPlayer resources to prevent memory leaks when Activity is destroyed
     */
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
