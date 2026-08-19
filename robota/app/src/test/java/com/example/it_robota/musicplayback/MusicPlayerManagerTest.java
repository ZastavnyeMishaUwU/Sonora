package com.example.it_robota.musicplayback;
import org.junit.Test;
import static org.mockito.Mockito.*;
import android.media.MediaPlayer;

/**
 * Unit test suite for {@link MusicPlayerManager}.
 */
public class MusicPlayerManagerTest {

    /**
     * Verifies that {@link MusicPlayerManager#pause()} invokes {@link MediaPlayer#pause()}
     * when the media player is currently playing audio.
     */
    @Test
    public void pause_whenMediaPlayerIsPlaying_callsPause() {
        MediaPlayer mockPlayer = mock(MediaPlayer.class);
        when(mockPlayer.isPlaying()).thenReturn(true);

        MusicPlayerManager manager =
                new MusicPlayerManager(mockPlayer);
        manager.pause();
        verify(mockPlayer).pause();
    }

    /**
     * Verifies that {@link MusicPlayerManager#pause()} does not invoke {@link MediaPlayer#pause()}
     * when the media player is already paused or stopped.
     */
    @Test
    public void pause_whenMediaPlayerIsNotPlaying_doesNotCallPause() {
        MediaPlayer mockPlayer = mock(MediaPlayer.class);
        when(mockPlayer.isPlaying()).thenReturn(false);

        MusicPlayerManager manager =
                new MusicPlayerManager(mockPlayer);
        manager.pause();
        verify(mockPlayer, never()).pause();
    }
}