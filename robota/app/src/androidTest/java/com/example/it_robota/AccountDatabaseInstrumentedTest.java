package com.example.it_robota;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.it_robota.auth.AuthResult;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.UserEntity;
import com.example.it_robota.repositories.AuthRepository;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AccountDatabaseInstrumentedTest {
    @Test public void registeringSecondAccountPreservesFirstAndGeneratesDistinctIds() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager session = new SessionManager(context);
        com.example.it_robota.auth.AccountSession previous = session.getAccount();
        AppDatabase database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        try {
            AuthRepository auth = new AuthRepository(database.userDao(), session);
            AuthResult first = auth.register("First", " FIRST@EXAMPLE.COM ", "Account pass1!");
            assertTrue(first.isSuccess());
            long firstId = session.getCurrentUserId();
            AuthResult second = auth.register("Second", "second@example.com", "Account pass2!");
            assertTrue(second.isSuccess());
            long secondId = session.getCurrentUserId();
            assertTrue(firstId > 0);
            assertTrue(secondId > 0);
            assertNotEquals(firstId, secondId);
            assertNotNull(database.userDao().getUserByEmail("first@example.com"));
            assertFalse(auth.register("Duplicate", "First@Example.com", "Account pass1!").isSuccess());
            auth.logout();
            assertTrue(auth.login("FIRST@example.com", "Account pass1!").isSuccess());
            assertEquals(firstId, session.getCurrentUserId());
            auth.logout();
            assertTrue(auth.login("second@example.com", "Account pass2!").isSuccess());
            assertEquals(secondId, session.getCurrentUserId());
        } finally {
            database.close();
            session.clearSession();
            if (previous != null) { session.saveSession(previous.getUserId(), previous.getEmail()); }
        }
    }

    @Test public void migrationPreservesKnownOwnersAndQuarantinesAmbiguousZeroIdRecords() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "account-migration-test-" + System.nanoTime() + ".db";
        try {
            try (SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)) {
                old.execSQL("CREATE TABLE users (id INTEGER NOT NULL PRIMARY KEY, username TEXT, email TEXT, passwordHash TEXT, createdAt INTEGER NOT NULL)");
                old.execSQL("CREATE TABLE tracks (id TEXT NOT NULL PRIMARY KEY, name TEXT, artistName TEXT, albumName TEXT, duration INTEGER NOT NULL, audioUrl TEXT, downloadUrl TEXT, imageUrl TEXT, licenseUrl TEXT, isFavorite INTEGER NOT NULL, localFilePath TEXT)");
                old.execSQL("CREATE TABLE favorite_tracks (userId INTEGER NOT NULL, trackId TEXT NOT NULL, PRIMARY KEY(userId, trackId))");
                old.execSQL("CREATE TABLE downloaded_tracks (userId INTEGER NOT NULL, trackId TEXT NOT NULL, localPath TEXT, trackName TEXT, artistName TEXT, PRIMARY KEY(userId, trackId))");
                old.execSQL("INSERT INTO users VALUES(0, 'Legacy', 'last@example.com', 'hash', 1)");
                old.execSQL("INSERT INTO users VALUES(5, 'Known', ' KNOWN@Example.com ', 'hash', 1)");
                old.execSQL("INSERT INTO tracks(id, name, duration, isFavorite) VALUES('old-track', 'Old track', 1, 1)");
                old.execSQL("INSERT INTO favorite_tracks VALUES(0, 'old-track'), (5, 'old-track')");
                old.execSQL("INSERT INTO downloaded_tracks VALUES(0, 'old-track', '/legacy.mp3', 'Old', 'Artist'), (5, 'old-track', '/legacy.mp3', 'Old', 'Artist')");
                old.setVersion(2);
            }
            AppDatabase migrated = Room.databaseBuilder(context, AppDatabase.class, name)
                    .addMigrations(AppDatabase.MIGRATION_2_3).build();
            try {
                assertNotNull(migrated.userDao().getUserById(0));
                assertTrue(migrated.favoriteTrackDao().getFavoriteTracksByAccount(0, "last@example.com").isEmpty());
                assertEquals(1, migrated.favoriteTrackDao().getFavoriteTracksByUser(0).size());
                assertEquals(1, migrated.favoriteTrackDao().getFavoriteTracksByAccount(5, "known@example.com").size());
                assertTrue(migrated.downloadedTrackDao().getDownloadsByAccount(0, "last@example.com").isEmpty());
                assertEquals(1, migrated.downloadedTrackDao().getDownloadedTracks(0).size());
                assertEquals("/legacy.mp3", migrated.downloadedTrackDao()
                        .getDownloadByAccount("old-track", 5, "known@example.com").getLocalPath());
                migrated.userDao().insertUser(new UserEntity(0, "New", "new@example.com", "hash", 2));
                assertTrue(migrated.userDao().getUserByEmail("new@example.com").getId() > 5);
                assertEquals("last@example.com", migrated.userDao().getUserById(0).getEmail());
            } finally {
                migrated.close();
            }
        } finally {
            context.deleteDatabase(name);
        }
    }
}
