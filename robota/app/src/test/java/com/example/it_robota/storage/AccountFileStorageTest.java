package com.example.it_robota.storage;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import android.content.Context;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AccountFileStorageTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void sameTrackHasSeparateFilesForDifferentEmails() {
        LocalFileStorageManager storage = storage();
        assertNotEquals(storage.buildFilePath("first@example.com", "12"),
                storage.buildFilePath("second@example.com", "12"));
    }

    @Test public void normalizedEmailRestoresSamePathWithoutLeakingEmail() {
        LocalFileStorageManager storage = storage();
        String path = storage.buildFilePath("first@example.com", "12");
        assertEquals(path, storage.buildFilePath(" FIRST@EXAMPLE.COM ", "12"));
        assertFalse(path.contains("first@example.com"));
        assertEquals(folder.getRoot(), new File(path).getParentFile());
    }

    @Test public void trackIdsCannotEscapeOrCollideThroughSanitization() {
        LocalFileStorageManager storage = storage();
        String path = storage.buildFilePath("first@example.com", "../../track");
        assertEquals(folder.getRoot(), new File(path).getParentFile());
        assertNotEquals(path, storage.buildFilePath("first@example.com", "______track"));
    }

    private LocalFileStorageManager storage() {
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getExternalFilesDir(any())).thenReturn(folder.getRoot());
        return new LocalFileStorageManager(context);
    }
}
