package com.example.it_robota.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.FavoriteTrackDao;
import com.example.it_robota.models.Track;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Verifies favorite persistence and account isolation in the Room-backed repository.
 */
@RunWith(AndroidJUnit4.class)
public class TrackRepositoryFavoritesInstrumentedTest {

    private static final long FIRST_USER_ID = 450045L;
    private static final long SECOND_USER_ID = 450046L;
    private static final String TRACK_ID = "favorite_repository_test_track";

    private Context context;
    private SessionManager sessionManager;
    private FavoriteTrackDao favoriteTrackDao;
    private boolean previousLoggedIn;
    private long previousUserId;
    private String previousEmail;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        sessionManager = new SessionManager(context);
        favoriteTrackDao = AppDatabase.getInstance(context).favoriteTrackDao();

        previousLoggedIn = sessionManager.isLoggedIn();
        previousUserId = sessionManager.getCurrentUserId();
        previousEmail = sessionManager.getCurrentUserEmail();

        cleanTestData();
        sessionManager.saveSession(FIRST_USER_ID, "first-favorite-test@example.com");
    }

    @After
    public void tearDown() {
        cleanTestData();
        if (previousLoggedIn && previousUserId >= 0L) {
            sessionManager.saveSession(previousUserId, previousEmail);
        } else {
            sessionManager.clearSession();
        }
    }

    @Test
    public void saveFavorite_persistsMetadataAndSurvivesRepositoryRecreation() throws Exception {
        TrackRepository repository = new TrackRepository(context);
        repository.saveFavorite(createTrack());

        TrackRepository recreatedRepository = new TrackRepository(context);
        List<Track> savedTracks = recreatedRepository.getSavedTracks();

        assertTrue(recreatedRepository.isTrackFavorite(TRACK_ID));
        assertEquals(1, savedTracks.size());
        assertEquals(TRACK_ID, savedTracks.get(0).getId());
        assertEquals("Favorite test track", savedTracks.get(0).getName());
        assertEquals("Favorite test artist", savedTracks.get(0).getArtistName());
        assertTrue(savedTracks.get(0).isFavorite());
    }

    @Test
    public void saveFavorite_sameTrackTwiceDoesNotCreateDuplicates() throws Exception {
        TrackRepository repository = new TrackRepository(context);
        repository.saveFavorite(createTrack());
        repository.saveFavorite(createTrack());

        assertEquals(1, repository.getSavedTracks().size());
    }

    @Test
    public void favorites_areIsolatedByUserAndRemovalAffectsOnlyActiveAccount() throws Exception {
        TrackRepository firstRepository = new TrackRepository(context);
        firstRepository.saveFavorite(createTrack());

        sessionManager.saveSession(SECOND_USER_ID, "second-favorite-test@example.com");
        TrackRepository secondRepository = new TrackRepository(context);
        assertFalse(secondRepository.isTrackFavorite(TRACK_ID));
        assertTrue(secondRepository.getSavedTracks().isEmpty());
        secondRepository.saveFavorite(createTrack());

        sessionManager.saveSession(FIRST_USER_ID, "first-favorite-test@example.com");
        firstRepository.removeFavorite(TRACK_ID);
        assertFalse(firstRepository.isTrackFavorite(TRACK_ID));
        assertTrue(firstRepository.getSavedTracks().isEmpty());

        sessionManager.saveSession(SECOND_USER_ID, "second-favorite-test@example.com");
        assertTrue(secondRepository.isTrackFavorite(TRACK_ID));
        assertEquals(1, secondRepository.getSavedTracks().size());
    }

    @Test
    public void saveFavorite_withoutSessionIsRejected() {
        sessionManager.clearSession();
        TrackRepository repository = new TrackRepository(context);

        assertThrows(Exception.class, () -> repository.saveFavorite(createTrack()));
        assertFalse(repository.isTrackFavorite(TRACK_ID));
        assertTrue(repository.getSavedTracks().isEmpty());
    }

    private Track createTrack() {
        return new Track(
                TRACK_ID,
                "Favorite test track",
                "Favorite test artist",
                "Favorite test album",
                185,
                "https://example.com/stream.mp3",
                "https://example.com/download.mp3",
                "https://example.com/cover.jpg",
                "https://example.com/license",
                false,
                null
        );
    }

    private void cleanTestData() {
        favoriteTrackDao.deleteTrack(TRACK_ID, FIRST_USER_ID);
        favoriteTrackDao.deleteTrack(TRACK_ID, SECOND_USER_ID);
        favoriteTrackDao.deleteTrackRecord(TRACK_ID);
    }
}
