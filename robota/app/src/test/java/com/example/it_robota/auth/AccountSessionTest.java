package com.example.it_robota.auth;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public class AccountSessionTest {
    @Test public void emailIsNormalizedConsistently() {
        assertEquals("user@example.com", new AccountSession(1, " USER@Example.com ", "a").getEmail());
    }

    @Test public void accountOrLoginChangesInvalidateOldOperations() {
        AccountSession first = new AccountSession(1, "first@example.com", "a");
        assertNotEquals(first, new AccountSession(2, "second@example.com", "a"));
        assertNotEquals(first, new AccountSession(1, "second@example.com", "a"));
        assertNotEquals(first, new AccountSession(1, "first@example.com", "new-login"));
        assertEquals(first, new AccountSession(1, " FIRST@example.com ", "a"));
    }

    @Test public void snapshotRejectsMissingOrCorruptedOwner() {
        Map<String, Object> values = accountValues();
        SessionManager manager = manager(values);
        assertNotNull(manager.getAccount());
        values.put("currentUserEmail", " ");
        assertNull(manager.getAccount());
        values.put("currentUserEmail", 123);
        assertNull(manager.getAccount());
        values.put("currentUserEmail", "first@example.com");
        values.put("currentUserId", "wrong-type");
        assertNull(manager.getAccount());
        values.put("currentUserId", -1L);
        assertNull(manager.getAccount());
    }

    @Test public void logoutInvalidatesCapturedAccount() {
        Map<String, Object> values = accountValues();
        SessionManager manager = manager(values);
        AccountSession account = manager.getAccount();
        assertTrue(manager.isCurrent(account));
        values.clear();
        assertFalse(manager.isCurrent(account));
        assertFalse(manager.isCurrent(null));
    }

    private Map<String, Object> accountValues() {
        Map<String, Object> values = new HashMap<>();
        values.put("loggedIn", true);
        values.put("currentUserId", 1L);
        values.put("currentUserEmail", "first@example.com");
        values.put("sessionToken", "one-login");
        return values;
    }

    private SessionManager manager(Map<String, Object> values) {
        Context context = mock(Context.class);
        SharedPreferences preferences = mock(SharedPreferences.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(preferences);
        when(preferences.getAll()).thenAnswer(invocation -> new HashMap<>(values));
        return new SessionManager(context);
    }
}
