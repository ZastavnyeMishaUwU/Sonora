package com.example.it_robota;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.view.View;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.it_robota.auth.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainBottomNavigationInstrumentedTest {

    private SessionManager sessionManager;
    private boolean wasLoggedIn;
    private long previousUserId;
    private String previousEmail;

    @Before
    public void rememberSession() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
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
    public void loggedOutUserDoesNotSeeBottomNavigation() {
        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.bottomNavigationBar))
                    .check(matches(withEffectiveVisibility(GONE)));
        }
    }

    @Test
    public void loggedInUserSeesFixedBottomNavigation() {
        logInTestUser();

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.bottomNavigationBar)).check(matches(isDisplayed()));

            scenario.onActivity(activity -> {
                ScrollView content = activity.findViewById(R.id.mainContentScrollView);
                View navigation = activity.findViewById(R.id.bottomNavigationBar);
                assertSame(content.getParent(), navigation.getParent());

                int[] positionBeforeScroll = new int[2];
                int[] positionAfterScroll = new int[2];
                navigation.getLocationOnScreen(positionBeforeScroll);
                content.scrollTo(0, 200);
                navigation.getLocationOnScreen(positionAfterScroll);

                assertArrayEquals(positionBeforeScroll, positionAfterScroll);
            });
        }
    }

    @Test
    public void bottomNavigationOpensAllThreeDestinations() {
        logInTestUser();

        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.profileNavButton)).perform(click());
            onView(withId(R.id.usernameText)).check(matches(isDisplayed()));
            pressBack();

            onView(withId(R.id.searchNavButton)).perform(click());
            onView(withId(R.id.etSearchQuery)).check(matches(isDisplayed()));
            pressBack();

            onView(withId(R.id.favoritesNavButton)).perform(click());
            onView(withId(R.id.btnBackFavorites)).check(matches(isDisplayed()));
        }
    }

    private void logInTestUser() {
        sessionManager.saveSession(460046L, "task46-navigation@example.com");
    }
}
