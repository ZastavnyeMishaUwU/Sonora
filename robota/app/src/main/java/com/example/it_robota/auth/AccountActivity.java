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

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        accountSessions = new SessionManager(this);
    }

    /** Call only after all account-owned views and dependencies have been initialized. */
    protected void accountUiReady() { accountUiReady = true; }

    @Override
    protected void onResume() {
        super.onResume();
        if (accountUiReady) {
            accountSessions.addListener(listener);
            observing = true;
            refreshAccount();
        }
    }

    protected final void refreshAccount() {
        accountRevision++;
        displayedAccount = accountSessions.getAccount();
        clearAccountContent();
        loadAccountContent(displayedAccount, accountRevision);
    }

    protected final int accountRevision() { return accountRevision; }

    protected final boolean acceptsResult(AccountSession account, int revision) {
        return observing && !isFinishing() && !isDestroyed() && revision == accountRevision
                && accountSessions.isCurrent(account);
    }

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

    protected abstract void clearAccountContent();
    protected abstract void loadAccountContent(AccountSession account, int revision);

    @Override
    protected void onDestroy() {
        if (observing) { accountSessions.removeListener(listener); }
        observing = false;
        accountRevision++;
        super.onDestroy();
    }
}
