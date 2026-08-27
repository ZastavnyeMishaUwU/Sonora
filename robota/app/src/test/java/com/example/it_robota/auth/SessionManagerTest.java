package com.example.it_robota.auth;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import android.media.MediaPlayer;
@RunWith(MockitoJUnitRunner.class)
public class SessionManagerTest {
    @Mock
    private Context mockContext;
    @Mock
    private SharedPreferences mockPreferences;
    @Mock
    private SharedPreferences.Editor mockEditor;

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPreferences);
        when(mockPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);

        sessionManager = new SessionManager(mockContext);
    }

    @Test
    public void saveSession_writesUserDataToPreferences() {
        long userId = 42L;
        String email = "test@example.com";

        sessionManager.saveSession(userId, email);

        verify(mockEditor).putBoolean("loggedIn", true);
        verify(mockEditor).putLong("currentUserId", userId);
        verify(mockEditor).putString("currentUserEmail", email);
        verify(mockEditor).apply();
    }

    @Test
    public void clearSession_removesUserDataFromPreferences() {
        sessionManager.clearSession();
        verify(mockEditor).remove("loggedIn");
        verify(mockEditor).remove("currentUserId");
        verify(mockEditor).remove("currentUserEmail");
        verify(mockEditor).apply();
    }

    @Test
    public void isLoggedIn_returnsTrue_whenTrue() {
        when(mockPreferences.getBoolean("loggedIn", false)).thenReturn(true);
        assertTrue(sessionManager.isLoggedIn());
    }

    @Test
    public void isLoggedIn_returnsFalse_whenDefault() {
        when(mockPreferences.getBoolean("loggedIn", false)).thenReturn(false);
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void getCurrentUserId_returnsSavedId() {
        when(mockPreferences.getLong("currentUserId", -1L)).thenReturn(100L);
        assertEquals(100L, sessionManager.getCurrentUserId());
    }

    @Test
    public void getCurrentUserId_whenNoSession_returnsNoUserId() {
        when(mockPreferences.getLong("currentUserId", -1L)).thenReturn(-1L);
        assertEquals(-1L, sessionManager.getCurrentUserId());
    }

    @Test
    public void getCurrentUserEmail_returnsSavedEmail() {
        when(mockPreferences.getString("currentUserEmail", null)).thenReturn("user@mail.com");
        assertEquals("user@mail.com", sessionManager.getCurrentUserEmail());
    }

    @Test
    public void getCurrentUserEmail_whenNoSession_returnsNull() {
        when(mockPreferences.getString("currentUserEmail", null)).thenReturn(null);
        assertNull(sessionManager.getCurrentUserEmail());
    }
}