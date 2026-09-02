package com.example.it_robota.auth;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Objects;

/** Invalidates account-owned UI and pending results on logout, account change or backgrounding. */
public abstract class AccountActivity extends AppCompatActivity {
    protected SessionManager accountSessions;
    protected AccountSession displayedAccount;
    private int accountRevision;
    private boolean accountUiReady;
    private boolean observing;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (preferences, key) -> {
        if (observing && !isFinishing() && !isDestroyed()
                && !Objects.equals(displayedAccount, accountSessions.getAccount())) {
            refreshAccount();
        }
    };

    /**
     * Initializes session access before the subclass creates its views.
     *
     * @param state previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        accountSessions = new SessionManager(this);
    }

    /**
     * Enables account refreshes once all account-owned views and dependencies are ready.
     * Subclasses must call this after initializing their screen in onCreate.
     */
    protected void accountUiReady() { accountUiReady = true; }

    /** Starts observing session changes and reloads the visible account's data. */
    @Override
    protected void onResume() {
        super.onResume();
        if (accountUiReady) {
            accountSessions.addListener(listener);
            observing = true;
            refreshAccount();
        }
    }

    /**
     * Clears the previous account's UI and starts a new load on the UI thread.
     * Incrementing the revision prevents earlier loads from updating the screen.
     */
    protected final void refreshAccount() {
        accountRevision++;
        displayedAccount = accountSessions.getAccount();
        clearAccountContent();
        loadAccountContent(displayedAccount, accountRevision);
    }

    /**
     * Returns the revision to capture before starting background work.
     *
     * @return current account-content revision
     */
    protected final int accountRevision() { return accountRevision; }

    /**
     * Checks whether a background result still belongs to the visible session and load.
     * Call this on the UI thread before applying the result.
     *
     * @param account session captured when the operation started
     * @param revision content revision captured with the session
     * @return true if the activity is observing the same session and revision
     */
    protected final boolean acceptsResult(AccountSession account, int revision) {
        return observing && !isFinishing() && !isDestroyed() && revision == accountRevision
                && accountSessions.isCurrent(account);
    }

    /** Hides account-owned content and invalidates pending UI results when the screen stops. */
    @Override
    protected void onStop() {
        if (observing) {
            accountSessions.removeListener(listener);
            observing = false;
        }
        accountRevision++;
        displayedAccount = null;
        if (accountUiReady) { clearAccountContent(); }
        super.onStop();
    }

    /**
     * Clears account-owned views and active playback without deleting saved data.
     * Called on the UI thread after the subclass has marked its views ready.
     */
    protected abstract void clearAccountContent();

    /**
     * Starts loading content for a session, or displays the logged-out state.
     * Background callbacks must pass {@link #acceptsResult(AccountSession, int)}.
     *
     * @param account session to display, or null when no valid session is available
     * @param revision revision to check before applying asynchronous results
     */
    protected abstract void loadAccountContent(AccountSession account, int revision);

    /** Removes any remaining listener and invalidates results during destruction. */
    @Override
    protected void onDestroy() {
        if (observing) { accountSessions.removeListener(listener); }
        observing = false;
        accountRevision++;
        super.onDestroy();
    }
}
