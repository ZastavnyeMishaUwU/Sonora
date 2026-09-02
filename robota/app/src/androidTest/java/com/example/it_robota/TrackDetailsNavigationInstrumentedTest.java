package com.example.it_robota;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.models.Track;
import com.example.it_robota.musicplayback.PlayerActivity;
import com.example.it_robota.tracks.TrackDetailsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
* Tests navigation independently of whether the remote track request succeeds. 
*/
@RunWith(AndroidJUnit4.class)
public class TrackDetailsNavigationInstrumentedTest {

    private static final String FIRST_TRACK_ID = "navigation-test-first-track";
    private static final String SECOND_TRACK_ID = "navigation-test-second-track";
    private static final int CLICK_COUNT = 20;

    private Instrumentation instrumentation;
    private Context context;
    private SessionManager sessionManager;
    private Instrumentation.ActivityMonitor detailsMonitor;
    private boolean wasLoggedIn;
    private long previousUserId;
    private String previousEmail;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        sessionManager = new SessionManager(context);
        wasLoggedIn = sessionManager.isLoggedIn();
        previousUserId = sessionManager.getCurrentUserId();
        previousEmail = sessionManager.getCurrentUserEmail();
        sessionManager.saveSession(1900190L, "details-navigation@example.com");
    }

    @After
    public void tearDown() {
        if (detailsMonitor != null) {
            Activity details = detailsMonitor.getLastActivity();
            if (details != null) {
                instrumentation.runOnMainSync(() -> {
                    if (!details.isDestroyed()) {
                        details.finish();
                    }
                });
            }
            instrumentation.removeMonitor(detailsMonitor);
        }
        sessionManager.clearSession();
        if (wasLoggedIn) {
            sessionManager.saveSession(previousUserId, previousEmail);
        }
    }

    @Test
    public void rapidPlayerClicksStartOnlyOneDetailsActivity() {
        monitorDetails(true);
        try (ActivityScenario<PlayerActivity> scenario = launchPlayer(FIRST_TRACK_ID)) {
            scenario.onActivity(activity -> {
                View button = activity.findViewById(R.id.playerDetailsButton);
                // Keep all callbacks in one UI turn, before Android can pause the source.
                for (int i = 0; i < CLICK_COUNT; i++) {
                    button.performClick();
                }
                assertEquals(1, detailsMonitor.getHits());
                assertFalse(button.isEnabled());
            });
        }
    }

    @Test
    public void rapidHomeLongClicksStartOnlyOneDetailsActivity() {
        monitorDetails(true);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                displayHomeTracks(activity, FIRST_TRACK_ID, SECOND_TRACK_ID);
                ListView list = activity.findViewById(R.id.tracksListView);
                for (int i = 0; i < CLICK_COUNT; i++) {
                    longClickTrack(list, i % 2);
                }
                assertEquals(1, detailsMonitor.getHits());
                assertFalse(list.isEnabled());
            });
        }
    }

    @Test
    public void playerCanReopenDetailsAfterBack() {
        monitorDetails(false);
        try (ActivityScenario<PlayerActivity> scenario = launchPlayer(FIRST_TRACK_ID)) {
            for (int i = 0; i < 3; i++) {
                onView(withId(R.id.playerDetailsButton)).check(matches(isEnabled()));
                scenario.onActivity(activity -> {
                    for (int click = 0; click < CLICK_COUNT; click++) {
                        activity.findViewById(R.id.playerDetailsButton).performClick();
                    }
                });
                assertDetailsTrack(FIRST_TRACK_ID);
                // A single Back must return to the player, not another details instance.
                pressBack();
                awaitResumedActivity(PlayerActivity.class);
                onView(withId(R.id.playerDetailsButton)).check(matches(isDisplayed()));
            }
        }
    }

    @Test
    public void homeCanOpenAnotherTrackAfterBack() {
        monitorDetails(false);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                displayHomeTracks(activity, FIRST_TRACK_ID, SECOND_TRACK_ID);
                for (int i = 0; i < CLICK_COUNT; i++) {
                    longClickTrack(activity.findViewById(R.id.tracksListView), i % 2);
                }
            });
            assertDetailsTrack(FIRST_TRACK_ID);
            onView(withId(R.id.btnTrackDetailsBack)).perform(click());
            awaitResumedActivity(MainActivity.class);
            onView(withId(R.id.tracksListView)).check(matches(isEnabled()));

            scenario.onActivity(activity ->
                    longClickTrack(activity.findViewById(R.id.tracksListView), 1));
            assertDetailsTrack(SECOND_TRACK_ID);
            pressBack();
            awaitResumedActivity(MainActivity.class);
            onView(withId(R.id.tracksListView)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void playerCanOpenDetailsAfterRecreation() {
        monitorDetails(false);
        try (ActivityScenario<PlayerActivity> scenario = launchPlayer(FIRST_TRACK_ID)) {
            onView(withId(R.id.playerDetailsButton)).perform(click());
            assertDetailsTrack(FIRST_TRACK_ID);
            pressBack();
            awaitResumedActivity(PlayerActivity.class);
            scenario.recreate();
            onView(withId(R.id.playerDetailsButton)).check(matches(isEnabled())).perform(click());
            assertDetailsTrack(FIRST_TRACK_ID);
            pressBack();
        }
    }

    @Test
    public void guestClicksDoNotOpenDetails() {
        sessionManager.clearSession();
        monitorDetails(true);
        try (ActivityScenario<PlayerActivity> scenario = launchPlayer(FIRST_TRACK_ID)) {
            scenario.onActivity(activity -> {
                for (int i = 0; i < CLICK_COUNT; i++) {
                    activity.findViewById(R.id.playerDetailsButton).performClick();
                }
                assertEquals(0, detailsMonitor.getHits());
            });
        }
    }

    @Test
    public void missingTrackIdDoesNotOpenDetails() {
        monitorDetails(true);
        try (ActivityScenario<PlayerActivity> scenario = launchPlayer(null)) {
            scenario.onActivity(activity -> {
                View button = activity.findViewById(R.id.playerDetailsButton);
                button.performClick();
                assertFalse(button.isEnabled());
                assertEquals(0, detailsMonitor.getHits());
            });
        }
    }

    private ActivityScenario<PlayerActivity> launchPlayer(String trackId) {
        return ActivityScenario.launch(new Intent(context, PlayerActivity.class)
                .putExtra(PlayerActivity.EXTRA_TRACK_ID, trackId));
    }

    private void monitorDetails(boolean blockLaunch) {
        detailsMonitor = instrumentation.addMonitor(
                TrackDetailsActivity.class.getName(), null, blockLaunch);
    }

    private void assertDetailsTrack(String expectedId) {
        Activity details = awaitResumedActivity(TrackDetailsActivity.class);
        onView(withId(R.id.btnTrackDetailsBack)).check(matches(isDisplayed()));
        assertEquals(expectedId, details.getIntent()
                .getStringExtra(TrackDetailsActivity.EXTRA_TRACK_ID));
    }

    /** 
    * Direct click bursts dispatch navigation asynchronously; wait for the new window. 
    */
    private Activity awaitResumedActivity(Class<? extends Activity> activityClass) {
        AtomicReference<Activity> result = new AtomicReference<>();
        long deadline = SystemClock.uptimeMillis() + 5000;
        do {
            instrumentation.runOnMainSync(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)) {
                    if (activityClass.isInstance(activity)
                            && !activity.isFinishing() && activity.hasWindowFocus()) {
                        result.set(activity);
                    }
                }
            });
            if (result.get() != null) {
                return result.get();
            }
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Screen did not become ready: " + activityClass.getSimpleName());
    }

    private void longClickTrack(ListView list, int position) {
        list.getOnItemLongClickListener().onItemLongClick(list, null, position, position);
    }

    private void displayHomeTracks(MainActivity activity, String... ids) {
        Track first = new Track();
        first.setId(ids[0]);
        first.setName("First navigation test track");
        Track second = new Track();
        second.setId(ids[1]);
        second.setName("Second navigation test track");
        try {
            Method method = MainActivity.class.getDeclaredMethod("showSearchResults", List.class);
            method.setAccessible(true);
            method.invoke(activity, Arrays.asList(first, second));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to display navigation test tracks", exception);
        }
    }
}
