package com.example.it_robota;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.it_robota.auth.RegisterActivity;
import com.example.it_robota.auth.validation.PasswordValidator;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that registration displays a clear message for every password rule.
 */
@RunWith(AndroidJUnit4.class)
public class PasswordValidationInstrumentedTest {

    @Test
    public void registrationDisplaysEachPasswordValidationMessage() {
        try (ActivityScenario<RegisterActivity> ignored =
                     ActivityScenario.launch(RegisterActivity.class)) {
            onView(withId(R.id.etUsername)).perform(replaceText("Student"));
            onView(withId(R.id.etEmail))
                    .perform(replaceText("student@example.com"));

            assertPasswordMessage("", PasswordValidator.EMPTY_MESSAGE);
            assertPasswordMessage("      ", PasswordValidator.WHITESPACE_ONLY_MESSAGE);
            assertPasswordMessage("A!234", PasswordValidator.MINIMUM_LENGTH_MESSAGE);
            assertPasswordMessage(
                    " Password1!",
                    PasswordValidator.BOUNDARY_SPACE_MESSAGE
            );
            assertPasswordMessage(
                    "Password1! ",
                    PasswordValidator.BOUNDARY_SPACE_MESSAGE
            );
            assertPasswordMessage(
                    "password1!",
                    PasswordValidator.UPPERCASE_MESSAGE
            );
            assertPasswordMessage(
                    "Password1",
                    PasswordValidator.SPECIAL_CHARACTER_MESSAGE
            );
        }
    }

    @Test
    public void interiorSpaceIsNotBlockedBeforeRepositoryValidation() {
        try (ActivityScenario<RegisterActivity> ignored =
                     ActivityScenario.launch(RegisterActivity.class)) {
            onView(withId(R.id.etUsername)).perform(replaceText("Student"));
            // A non-empty invalid email reaches repository validation without creating a user.
            onView(withId(R.id.etEmail)).perform(replaceText("invalid-email"));
            onView(withId(R.id.etPassword)).perform(replaceText("Valid pass1!"));
            onView(withId(R.id.btnRegister)).perform(click());

            long deadline = SystemClock.uptimeMillis() + 5000;
            while (true) {
                try {
                    onView(withId(R.id.tvStatus))
                            .check(matches(withText("Email is not valid.")));
                    break;
                } catch (AssertionError failure) {
                    if (SystemClock.uptimeMillis() >= deadline) {
                        throw failure;
                    }
                    SystemClock.sleep(50);
                }
            }

            onView(withId(R.id.etPassword)).check(matches(withText("Valid pass1!")));
        }
    }

    private void assertPasswordMessage(String password, String expectedMessage) {
        onView(withId(R.id.etPassword)).perform(replaceText(password));
        onView(withId(R.id.btnRegister)).perform(click());
        onView(withId(R.id.tvStatus)).check(matches(withText(expectedMessage)));
    }
}
