package com.example.it_robota;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.models.Track;
import com.example.it_robota.repositories.TrackRepository;
import com.example.it_robota.tracks.SearchActivity;
import com.example.it_robota.tracks.TrackAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class SearchAuthenticationInstrumentedTest {

    private static final String TITLE = "Visible title for task 47";
    private static final String ARTIST = "Artist must not be displayed";
    private static final String TRACK_ID = "internal-track-id-must-not-be-displayed";

    private Context context;
    private SessionManager sessionManager;
    private boolean wasLoggedIn;
    private long previousUserId;
    private String previousEmail;

    @Before
    public void rememberAndClearSession() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        sessionManager = new SessionManager(context);
        wasLoggedIn = sessionManager.isLoggedIn();
        previousUserId = sessionManager.getCurrentUserId();
        previousEmail = sessionManager.getCurrentUserEmail();
        sessionManager.clearSession();
    }

    @After
    public void restoreSession() {
        sessionManager.clearSession();
        if (wasLoggedIn) {
            sessionManager.saveSession(previousUserId, previousEmail);
        }
    }

    @Test
    public void loggedOutUserIsRedirectedFromSearchToLogin() {
        try (ActivityScenario<SearchActivity> ignored =
                     ActivityScenario.launch(SearchActivity.class)) {
            onView(withId(R.id.etLoginEmail)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void loggedOutUserCannotSeeHomeSearchControls() {
        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.cardSearch))
                    .check(matches(withEffectiveVisibility(GONE)));
            onView(withId(R.id.searchButton))
                    .check(matches(withEffectiveVisibility(GONE)));
        }
    }

    @Test
    public void repositoryRejectsSearchWithoutActiveSession() {
        TrackRepository repository = new TrackRepository(context);

        try {
            repository.searchTracks("blocked query");
            fail("Search must be rejected for a logged-out user.");
        } catch (IllegalStateException exception) {
            assertEquals("User is not logged in.", exception.getMessage());
        } catch (Exception exception) {
            fail("Unexpected exception: " + exception.getClass().getSimpleName());
        }
    }

    @Test
    public void searchResultDisplaysOnlyTrackTitle() {
        logInTestUser();

        try (ActivityScenario<SearchActivity> scenario =
                     ActivityScenario.launch(SearchActivity.class)) {
            scenario.onActivity(activity -> {
                Track track = new Track();
                track.setId(TRACK_ID);
                track.setName(TITLE);
                track.setArtistName(ARTIST);

                RecyclerView results = activity.findViewById(R.id.rvSearchResults);
                results.setAdapter(new TrackAdapter(
                        Collections.singletonList(track),
                        selectedTrack -> { }
                ));
                results.setVisibility(View.VISIBLE);
            });

            onView(withText(TITLE)).check(matches(isDisplayed()));
            onView(withText(ARTIST)).check(doesNotExist());
            onView(withText(TRACK_ID)).check(doesNotExist());
        }
    }

    private void logInTestUser() {
        sessionManager.saveSession(470047L, "task47-search@example.com");
    }
}
