package com.example.it_robota;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.it_robota.auth.LoginActivity;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.auth.SettingsActivity;
import com.example.it_robota.models.Track;
import com.example.it_robota.tracks.DownloadedTracksActivity;
import com.example.it_robota.tracks.FavoritesActivity;
import com.example.it_robota.tracks.SearchActivity;
import com.example.it_robota.tracks.TrackDetailsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class GuestSearchAccessInstrumentedTest {

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
    public void loggedOutUserCanOpenSearchDirectly() {
        try (ActivityScenario<SearchActivity> ignored =
                     ActivityScenario.launch(SearchActivity.class)) {
            onView(withId(R.id.etSearchQuery)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSearch)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void loginScreenOffersGuestSearch() {
        try (ActivityScenario<LoginActivity> ignored =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnContinueAsGuest)).perform(click());
            onView(withId(R.id.etSearchQuery)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBackSearch)).perform(click());
        }
    }

    @Test
    public void guestCanOpenPlayerAndSeesOnlyTrackTitleInSearchResults() {
        try (ActivityScenario<SearchActivity> scenario =
                     ActivityScenario.launch(SearchActivity.class)) {
            scenario.onActivity(activity -> displayTestResult(activity));

            onView(withText(TITLE)).check(matches(isDisplayed()));
            onView(withText(ARTIST)).check(doesNotExist());
            onView(withText(TRACK_ID)).check(doesNotExist());

            onView(withText(TITLE)).perform(click());
            onView(withId(R.id.playPauseButton)).check(matches(isDisplayed()));
            onView(withId(R.id.stopButton)).check(matches(isDisplayed()));
            onView(withId(R.id.playerDetailsButton))
                    .check(matches(withEffectiveVisibility(GONE)));
            onView(withId(R.id.playerBackButton)).perform(click());
        }
    }

    @Test
    public void guestIsRedirectedFromAllAuthenticatedOnlyScreens() {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            assertProtectedScreenRedirectsToLogin(
                    scenario,
                    new Intent(context, TrackDetailsActivity.class)
                            .putExtra(TrackDetailsActivity.EXTRA_TRACK_ID, TRACK_ID)
            );
            assertProtectedScreenRedirectsToLogin(
                    scenario,
                    new Intent(context, FavoritesActivity.class)
            );
            assertProtectedScreenRedirectsToLogin(
                    scenario,
                    new Intent(context, DownloadedTracksActivity.class)
            );
            assertProtectedScreenRedirectsToLogin(
                    scenario,
                    new Intent(context, SettingsActivity.class)
            );
        }
    }

    private void displayTestResult(SearchActivity activity) {
        Track track = new Track();
        track.setId(TRACK_ID);
        track.setName(TITLE);
        track.setArtistName(ARTIST);

        try {
            Method displayResults = SearchActivity.class.getDeclaredMethod(
                    "displayResults",
                    java.util.List.class
            );
            displayResults.setAccessible(true);
            displayResults.invoke(activity, Collections.singletonList(track));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to display the test search result.", exception);
        }
    }

    private void assertProtectedScreenRedirectsToLogin(
            ActivityScenario<LoginActivity> loginScenario,
            Intent protectedScreenIntent
    ) {
        loginScenario.onActivity(activity -> activity.startActivity(protectedScreenIntent));
        onView(withId(R.id.etLoginEmail)).check(matches(isDisplayed()));
    }
}
