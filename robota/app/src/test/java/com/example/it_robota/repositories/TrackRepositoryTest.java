package com.example.it_robota.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

import com.example.it_robota.api.JamendoApiClient;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.database.FavoriteTrackDao;
import com.example.it_robota.database.FavoriteTrackEntity;
import com.example.it_robota.database.TrackEntity;
import com.example.it_robota.models.Track;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link TrackRepository}.
 * Verifies track searching, details retrieval, saving/removing favorites, and session handling logic.
 */
@RunWith(MockitoJUnitRunner.class)
public class TrackRepositoryTest {
    @Mock
    private JamendoApiClient jamendoApiClient;

    @Mock
    private FavoriteTrackDao favoriteTrackDao;

    @Mock
    private SessionManager sessionManager;

    private TrackRepository trackRepository;

    private static final long VALID_USER_ID = 100L;
    private static final String TRACK_ID = "track_123";

    /**
     * Initializes the repository with mock dependencies before each test execution.
     */
    @Before
    public void setUp() {
        trackRepository = new TrackRepository(jamendoApiClient, favoriteTrackDao, sessionManager);
    }
    
    /**
     * Verifies that searchTracks returns the list of tracks provided by JamendoApiClient.
     */
    @Test
    public void searchTracks_returnsListFromApiClient() throws Exception {
        String query = "rock";
        List<Track> expectedTracks = Collections.singletonList(createDummyTrack(TRACK_ID));
        when(jamendoApiClient.searchTracks(query)).thenReturn(expectedTracks);

        List<Track> actualTracks = trackRepository.searchTracks(query);

        assertEquals(expectedTracks, actualTracks);
        verify(jamendoApiClient).searchTracks(query);
    }

    /**
     * Verifies that if a track exists and is saved as a favorite by the active user,
     * the isFavorite flag is set to true.
     */
    @Test
    public void getTrackDetails_whenTrackExistsAndIsFavorite_setsFavoriteTrue() throws Exception {
        Track track = createDummyTrack(TRACK_ID);
        when(jamendoApiClient.getTrackDetails(TRACK_ID)).thenReturn(track);
        when(sessionManager.isLoggedIn()).thenReturn(true);
        when(sessionManager.getCurrentUserId()).thenReturn(VALID_USER_ID);
        when(favoriteTrackDao.isTrackFavorite(TRACK_ID, VALID_USER_ID)).thenReturn(true);

        Track result = trackRepository.getTrackDetails(TRACK_ID);

        assertNotNull(result);
        assertTrue(result.isFavorite());
    }

    /**
     * Verifies that getTrackDetails returns null when the API returns null.
     */
    @Test
    public void getTrackDetails_whenTrackNotFound_returnsNull() throws Exception {
        when(jamendoApiClient.getTrackDetails(TRACK_ID)).thenReturn(null);

        Track result = trackRepository.getTrackDetails(TRACK_ID);

        assertEquals(null, result);
    }

    /**
     * Verifies that an empty list is returned when the user is not logged in,
     * without attempting to query the database.
     */
    @Test
    public void getSavedTracks_whenUserNotLoggedIn_returnsEmptyList() {
        when(sessionManager.isLoggedIn()).thenReturn(false);

        List<Track> tracks = trackRepository.getSavedTracks();

        assertTrue(tracks.isEmpty());
        verify(favoriteTrackDao, never()).getFavoriteTracksByUser(any(Long.class));
    }

    /**
     * Verifies that for a logged-in user, stored database entities (TrackEntity)
     * are correctly mapped to domain models (Track).
     */
    @Test
    public void getSavedTracks_whenUserLoggedIn_returnsMappedTracks() {
        when(sessionManager.isLoggedIn()).thenReturn(true);
        when(sessionManager.getCurrentUserId()).thenReturn(VALID_USER_ID);

        TrackEntity entity = new TrackEntity();
        entity.setId(TRACK_ID);
        entity.setName("Test Song");
        when(favoriteTrackDao.getFavoriteTracksByUser(VALID_USER_ID))
                .thenReturn(Collections.singletonList(entity));

        List<Track> tracks = trackRepository.getSavedTracks();

        assertEquals(1, tracks.size());
        assertEquals(TRACK_ID, tracks.get(0).getId());
        assertTrue(tracks.get(0).isFavorite());
    }


    /**
     * Verifies that passing a null track throws an Exception.
     */
    @Test(expected = Exception.class)
    public void saveFavorite_whenTrackIsNull_throwsException() throws Exception {
        trackRepository.saveFavorite(null);
    }

    /**
     * Verifies that attempting to save a track with a blank ID throws an Exception.
     */
    @Test(expected = Exception.class)
    public void saveFavorite_whenTrackIdIsBlank_throwsException() throws Exception {
        Track track = createDummyTrack("   ");
        trackRepository.saveFavorite(track);
    }

    /**
     * Verifies that attempting to save a favorite when no user is logged in throws an Exception.
     */
    @Test(expected = Exception.class)
    public void saveFavorite_whenUserNotLoggedIn_throwsException() throws Exception {
        Track track = createDummyTrack(TRACK_ID);
        when(sessionManager.isLoggedIn()).thenReturn(false);

        trackRepository.saveFavorite(track);
    }

    /**
     * Verifies successful saving of a track to favorites for a logged-in user.
     */
    @Test
    public void saveFavorite_whenValidTrackAndUser_savesAndSetsFavoriteTrue() throws Exception {
        Track track = createDummyTrack(TRACK_ID);
        when(sessionManager.isLoggedIn()).thenReturn(true);
        when(sessionManager.getCurrentUserId()).thenReturn(VALID_USER_ID);

        trackRepository.saveFavorite(track);

        verify(favoriteTrackDao).saveFavorite(any(TrackEntity.class), any(FavoriteTrackEntity.class));
        assertTrue(track.isFavorite());
    }

    /**
     * Verifies that removing a favorite delegates deletion to DAO when inputs are valid.
     */
    @Test
    public void removeFavorite_whenValidIdAndLoggedIn_deletesFromDao() {
        when(sessionManager.isLoggedIn()).thenReturn(true);
        when(sessionManager.getCurrentUserId()).thenReturn(VALID_USER_ID);

        trackRepository.removeFavorite(TRACK_ID);

        verify(favoriteTrackDao).deleteTrack(TRACK_ID, VALID_USER_ID);
    }

    /**
     * Verifies that attempting to remove a track with a blank ID does nothing.
     */
    @Test
    public void removeFavorite_whenTrackIdIsBlank_doesNothing() {
        trackRepository.removeFavorite("  ");

        verify(favoriteTrackDao, never()).deleteTrack(any(), any(Long.class));
    }

    /**
     * Verifies handling of ClassCastException during user ID retrieval:
     * the session should be cleared and the method should return false.
     */
    @Test
    public void isTrackFavorite_whenClassCastExceptionInSession_clearsSessionAndReturnsFalse() {
        when(sessionManager.isLoggedIn()).thenReturn(true);
        when(sessionManager.getCurrentUserId()).thenThrow(new ClassCastException("Bad cast"));

        boolean isFavorite = trackRepository.isTrackFavorite(TRACK_ID);

        assertFalse(isFavorite);
        verify(sessionManager).clearSession();
    }

    /**
     * Creates a dummy {@link Track} instance for testing purposes.
     */
    private Track createDummyTrack(String id) {
        return new Track(
                id,
                "Song Title",
                "Artist",
                "Album",
                180,
                "http://audio.mp3",
                "http://download.mp3",
                "http://image.jpg",
                "http://license.com",
                false,
                null
        );
    }

}
