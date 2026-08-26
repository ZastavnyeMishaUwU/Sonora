package com.example.it_robota.musicplayback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import android.media.MediaPlayer;

/**
 * Unit test suite for {@link MusicPlayerManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MusicPlayerManagerTest {
    @Mock
    private MediaPlayer mockMediaPlayer;

    private MusicPlayerManager playerManager;

    @Before
    public void setUp() {
        playerManager = new MusicPlayerManager(mockMediaPlayer);
    }

    /**
     * Verifies that {@link MusicPlayerManager#pause()} invokes {@link MediaPlayer#pause()}
     * when the media player is currently playing audio.
     */
    @Test
    public void pause_whenMediaPlayerIsPlaying_callsPause() {
        when(mockMediaPlayer.isPlaying()).thenReturn(true);
        playerManager.pause();
        verify(mockMediaPlayer).pause();
    }

    /**
     * Verifies that {@link MusicPlayerManager#pause()} does not invoke {@link MediaPlayer#pause()}
     * when the media player is already paused or stopped.
     */
    @Test
    public void pause_whenMediaPlayerIsNotPlaying_doesNotCallPause() {
        when(mockMediaPlayer.isPlaying()).thenReturn(false);
        playerManager.pause();
        verify(mockMediaPlayer, never()).pause();
    }

    /**
     * Verifies that {@link MusicPlayerManager#resume()} invokes {@link MediaPlayer#start()}
     * when the media player is currently paused or stopped.
     */
    @Test
    public void resume_whenMediaPlayerIsNotPlaying_doesCallResume() {
        when(mockMediaPlayer.isPlaying()).thenReturn(false);
        playerManager.resume();
        verify(mockMediaPlayer).start();
    }

    /**
     * Verifies that {@link MusicPlayerManager#resume()} does not invoke {@link MediaPlayer#start()}
     * when the media player is already playing.
     */
    @Test
    public void resume_whenMediaPlayerIsPlaying_doesNotCallResume() {
        when(mockMediaPlayer.isPlaying()).thenReturn(true);
        playerManager.resume();
        verify(mockMediaPlayer, never()).start();
    }

    /**
     * Verifies that {@link MusicPlayerManager#stop()} invokes both {@link MediaPlayer#stop()}
     * and {@link MediaPlayer#release()} when the media player is currently playing.
     */
    @Test
    public void stop_whenMediaPlayerIsPlaying_doesCallStopRelease() {
        when(mockMediaPlayer.isPlaying()).thenReturn(true);
        playerManager.stop();
        verify(mockMediaPlayer).stop();
        verify(mockMediaPlayer).release();
    }

    /**
     * Verifies that {@link MusicPlayerManager#stop()} does not invoke {@link MediaPlayer#stop()}
     * but still calls {@link MediaPlayer#release()} when the media player is not playing.
     */
    @Test
    public void stop_whenMediaPlayerIsNotPlaying_doesNotCallStop_doesCallRelease() {
        when(mockMediaPlayer.isPlaying()).thenReturn(false);
        playerManager.stop();
        verify(mockMediaPlayer, never()).stop();
        verify(mockMediaPlayer).release();
    }

    /**
     * Verifies that {@link MusicPlayerManager#isPlaying()} safely returns {@code false}
     * when the internal {@link MediaPlayer} reference is null after calling {@link MusicPlayerManager#release()}.
     */
    @Test
    public void isPlaying_returnsFalse_whenMediaPlayerIsNull() {
        playerManager.release();
        assertFalse(playerManager.isPlaying());
    }

    /**
     * Verifies that {@link MusicPlayerManager#play(String)} correctly resets, configures
     * the data source URL, prepares asynchronously, and attaches an {@link MediaPlayer.OnPreparedListener}.
     */
    @Test
    public void play_preparesMediaPlayerWithCorrectUrl() throws IOException {
        String testUrl = "https://example.com/audio.mp3";
        playerManager.play(testUrl);
        verify(mockMediaPlayer).reset();
        verify(mockMediaPlayer).setDataSource(testUrl);
        verify(mockMediaPlayer).prepareAsync();
        verify(mockMediaPlayer).setOnPreparedListener(any());
    }
}