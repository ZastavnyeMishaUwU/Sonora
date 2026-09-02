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
import com.example.it_robota.auth.AccountSession;
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
    private static final AccountSession ACCOUNT = new AccountSession(VALID_USER_ID, "first@example.com", "login-1");

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
        when(sessionManager.getAccount()).thenReturn(ACCOUNT);
        when(sessionManager.isCurrent(ACCOUNT)).thenReturn(true);
        when(favoriteTrackDao.isTrackFavoriteForAccount(TRACK_ID, VALID_USER_ID, ACCOUNT.getEmail())).thenReturn(true);

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
        when(sessionManager.getAccount()).thenReturn(null);

        List<Track> tracks = trackRepository.getSavedTracks();

        assertTrue(tracks.isEmpty());
        verify(favoriteTrackDao, never()).getFavoriteTracksByAccount(any(Long.class), any());
    }

    /**
     * Verifies that for a logged-in user, stored database entities (TrackEntity)
     * are correctly mapped to domain models (Track).
     */
    @Test
    public void getSavedTracks_whenUserLoggedIn_returnsMappedTracks() {
        when(sessionManager.getAccount()).thenReturn(ACCOUNT);
        when(sessionManager.isCurrent(ACCOUNT)).thenReturn(true);

        TrackEntity entity = new TrackEntity();
        entity.setId(TRACK_ID);
        entity.setName("Test Song");
        when(favoriteTrackDao.getFavoriteTracksByAccount(VALID_USER_ID, ACCOUNT.getEmail()))
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
        when(sessionManager.getAccount()).thenReturn(null);

        trackRepository.saveFavorite(track);
    }

    /**
     * Verifies successful saving of a track to favorites for a logged-in user.
     */
    @Test
    public void saveFavorite_whenValidTrackAndUser_savesAndSetsFavoriteTrue() throws Exception {
        Track track = createDummyTrack(TRACK_ID);
        when(sessionManager.getAccount()).thenReturn(ACCOUNT);
        when(sessionManager.isCurrent(ACCOUNT)).thenReturn(true);

        trackRepository.saveFavorite(track);

        verify(favoriteTrackDao).saveFavorite(any(TrackEntity.class), any(FavoriteTrackEntity.class));
        assertTrue(track.isFavorite());
    }

    /**
     * Verifies that removing a favorite delegates deletion to DAO when inputs are valid.
     */
    @Test
    public void removeFavorite_whenValidIdAndLoggedIn_deletesFromDao() {
        when(sessionManager.getAccount()).thenReturn(ACCOUNT);
        when(sessionManager.isCurrent(ACCOUNT)).thenReturn(true);

        trackRepository.removeFavorite(TRACK_ID);

        verify(favoriteTrackDao).deleteForAccount(TRACK_ID, VALID_USER_ID, ACCOUNT.getEmail());
    }

    /**
     * Verifies that attempting to remove a track with a blank ID does nothing.
     */
    @Test
    public void removeFavorite_whenTrackIdIsBlank_doesNothing() {
        trackRepository.removeFavorite("  ");

        verify(favoriteTrackDao, never()).deleteForAccount(any(), any(Long.class), any());
    }

    /**
     * Invalid session snapshots must not query another account's favorites.
     */
    @Test
    public void isTrackFavorite_whenSessionIsInvalid_returnsFalse() {
        when(sessionManager.getAccount()).thenReturn(null);

        boolean isFavorite = trackRepository.isTrackFavorite(TRACK_ID);

        assertFalse(isFavorite);
        verify(favoriteTrackDao, never()).isTrackFavoriteForAccount(any(), any(Long.class), any());
    }

    /**
     * Switching accounts changes both components of the ownership query.
     */
    @Test
    public void accountSwitchQueriesTheNewEmailAndId() {
        AccountSession second = new AccountSession(200, "second@example.com", "login-2");
        when(sessionManager.getAccount()).thenReturn(ACCOUNT, second);
        when(sessionManager.isCurrent(ACCOUNT)).thenReturn(true);
        when(sessionManager.isCurrent(second)).thenReturn(true);
        trackRepository.getSavedTracks();
        trackRepository.getSavedTracks();
        verify(favoriteTrackDao).getFavoriteTracksByAccount(VALID_USER_ID, ACCOUNT.getEmail());
        verify(favoriteTrackDao).getFavoriteTracksByAccount(200, second.getEmail());
    }

    @Test
    public void staleAccountCannotRemoveOrSaveAnotherUsersFavorite() throws Exception {
        org.junit.Assert.assertThrows(Exception.class,
                () -> trackRepository.saveFavorite(createDummyTrack(TRACK_ID), ACCOUNT));
        trackRepository.removeFavorite(TRACK_ID, ACCOUNT);
        org.mockito.Mockito.verifyNoInteractions(favoriteTrackDao);
    }

    @Test
    public void sharedMetadataNeverIncludesAnotherUsersLocalPath() throws Exception {
        when(sessionManager.getAccount()).thenReturn(ACCOUNT);
        when(sessionManager.isCurrent(ACCOUNT)).thenReturn(true);
        Track track = createDummyTrack(TRACK_ID);
        track.setLocalFilePath("private-first-account.mp3");
        trackRepository.saveFavorite(track);
        org.mockito.ArgumentCaptor<TrackEntity> metadata = org.mockito.ArgumentCaptor.forClass(TrackEntity.class);
        org.mockito.ArgumentCaptor<FavoriteTrackEntity> owner = org.mockito.ArgumentCaptor.forClass(FavoriteTrackEntity.class);
        verify(favoriteTrackDao).saveFavorite(metadata.capture(), owner.capture());
        org.junit.Assert.assertNull(metadata.getValue().getLocalFilePath());
        assertEquals(ACCOUNT.getEmail(), owner.getValue().getOwnerEmail());
    }

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
