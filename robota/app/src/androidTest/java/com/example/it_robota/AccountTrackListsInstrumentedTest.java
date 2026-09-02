package com.example.it_robota;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.SystemClock;
import android.widget.ListView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.it_robota.auth.AccountActivity;
import com.example.it_robota.auth.AccountSession;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.DownloadedTrackEntity;
import com.example.it_robota.downloader.TrackDownloadManager;
import com.example.it_robota.models.Track;
import com.example.it_robota.repositories.AuthRepository;
import com.example.it_robota.repositories.TrackRepository;
import com.example.it_robota.storage.LocalFileStorageManager;
import com.example.it_robota.tracks.DownloadedTracksActivity;
import com.example.it_robota.tracks.FavoritesActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class AccountTrackListsInstrumentedTest {
    private Context context;
    private AppDatabase database;
    private SessionManager sessions;
    private AuthRepository auth;
    private AccountSession previous;
    private AccountSession first;
    private AccountSession second;
    private String firstTrackId;
    private String secondTrackId;
    private String firstPath;
    private String secondPath;

    @Before public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = AppDatabase.getInstance(context);
        sessions = new SessionManager(context);
        previous = sessions.getAccount();
        auth = new AuthRepository(context);
        String suffix = Long.toString(System.nanoTime());
        firstTrackId = "account-ui-first-" + suffix;
        secondTrackId = "account-ui-second-" + suffix;
        assertTrue(auth.register("First", "first-" + suffix + "@example.com", "Account pass1!").isSuccess());
        first = sessions.getAccount();
        firstPath = seedTrack(first, firstTrackId, "First account track");
        assertTrue(auth.register("Second", "second-" + suffix + "@example.com", "Account pass1!").isSuccess());
        second = sessions.getAccount();
        secondPath = seedTrack(second, secondTrackId, "Second account track");
        login(first);
    }

    @After public void tearDown() {
        for (AccountSession account : new AccountSession[]{first, second}) {
            if (account == null) { continue; }
            database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM favorite_tracks WHERE ownerEmail = ?", new Object[]{account.getEmail()});
            database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM downloaded_tracks WHERE ownerEmail = ?", new Object[]{account.getEmail()});
            database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM users WHERE id = ?", new Object[]{account.getUserId()});
        }
        if (firstTrackId != null) { database.favoriteTrackDao().deleteTrackRecord(firstTrackId); }
        if (secondTrackId != null) { database.favoriteTrackDao().deleteTrackRecord(secondTrackId); }
        LocalFileStorageManager storage = new LocalFileStorageManager(context);
        storage.deleteFile(firstPath);
        storage.deleteFile(secondPath);
        sessions.clearSession();
        if (previous != null) { sessions.saveSession(previous.getUserId(), previous.getEmail()); }
    }

    @Test public void favoritesRefreshOnAccountSwitchLogoutAndLogin() {
        try (ActivityScenario<FavoritesActivity> scenario = ActivityScenario.launch(FavoritesActivity.class)) {
            awaitList(scenario, R.id.lvFavorites, "First account track");
            login(second);
            awaitList(scenario, R.id.lvFavorites, "Second account track");
            auth.logout();
            awaitList(scenario, R.id.lvFavorites, null);
            login(first);
            awaitList(scenario, R.id.lvFavorites, "First account track");
            scenario.recreate();
            awaitList(scenario, R.id.lvFavorites, "First account track");
        }
        assertEquals(1, new TrackRepository(context).getSavedTracks().size());
    }

    @Test public void downloadsRefreshOnAccountSwitchLogoutAndLoginWithoutDeletingFiles() {
        try (ActivityScenario<DownloadedTracksActivity> scenario = ActivityScenario.launch(DownloadedTracksActivity.class)) {
            awaitList(scenario, R.id.downloadedTracksListView, "First account track");
            login(second);
            awaitList(scenario, R.id.downloadedTracksListView, "Second account track");
            auth.logout();
            awaitList(scenario, R.id.downloadedTracksListView, null);
            assertTrue(new java.io.File(firstPath).isFile());
            assertTrue(new java.io.File(secondPath).isFile());
            login(first);
            awaitList(scenario, R.id.downloadedTracksListView, "First account track");
            scenario.recreate();
            awaitList(scenario, R.id.downloadedTracksListView, "First account track");
        }
    }

    @Test public void staleFavoriteLoadCannotReappearAfterAccountSwitch() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<Track> oldTracks = new TrackRepository(context).getSavedTracks();
        try (ActivityScenario<FavoritesActivity> scenario = ActivityScenario.launch(FavoritesActivity.class)) {
            awaitList(scenario, R.id.lvFavorites, "First account track");
            scenario.onActivity(activity -> setField(activity, "trackRepository", new TrackRepository(context) {
                @Override public List<Track> getSavedTracks(AccountSession account) {
                    if (account.getEmail().equals(first.getEmail())) {
                        started.countDown();
                        awaitRelease(release);
                        return oldTracks;
                    }
                    return super.getSavedTracks(account);
                }
            }));
            login(first);
            assertTrue(started.await(5, TimeUnit.SECONDS));
            login(second);
            awaitList(scenario, R.id.lvFavorites, null);
            release.countDown();
            awaitList(scenario, R.id.lvFavorites, "Second account track");
        } finally { release.countDown(); }
    }

    @Test public void staleDownloadLoadCannotReappearAfterLogout() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<DownloadedTrackEntity> oldTracks = new TrackDownloadManager(context).getDownloadedTracks(sessions.getAccount());
        try (ActivityScenario<DownloadedTracksActivity> scenario = ActivityScenario.launch(DownloadedTracksActivity.class)) {
            awaitList(scenario, R.id.downloadedTracksListView, "First account track");
            scenario.onActivity(activity -> setField(activity, "downloadManager", new TrackDownloadManager(context) {
                @Override public List<DownloadedTrackEntity> getDownloadedTracks(AccountSession account) {
                    started.countDown();
                    awaitRelease(release);
                    return oldTracks;
                }
            }));
            login(first);
            assertTrue(started.await(5, TimeUnit.SECONDS));
            auth.logout();
            awaitList(scenario, R.id.downloadedTracksListView, null);
            release.countDown();
            // Wait behind the old load and then drain its main-thread callback.
            java.util.concurrent.atomic.AtomicReference<ExecutorService> executor = new java.util.concurrent.atomic.AtomicReference<>();
            scenario.onActivity(activity -> executor.set((ExecutorService) getField(activity, "executorService")));
            executor.get().submit(() -> {}).get(5, TimeUnit.SECONDS);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            awaitList(scenario, R.id.downloadedTracksListView, null);
        } finally { release.countDown(); }
    }

    private String seedTrack(AccountSession account, String id, String title) throws Exception {
        Track track = new Track();
        track.setId(id);
        track.setName(title);
        new TrackRepository(context).saveFavorite(track);
        String path = new LocalFileStorageManager(context).buildFilePath(account.getEmail(), id);
        try (FileOutputStream output = new FileOutputStream(path)) { output.write(new byte[]{73, 68, 51, 4, 0}); }
        DownloadedTrackEntity download = new DownloadedTrackEntity(account.getUserId(), id, title, "Artist", path);
        download.setOwnerEmail(account.getEmail());
        database.downloadedTrackDao().insertDownloadedTrack(download);
        return path;
    }

    private void login(AccountSession account) {
        assertTrue(auth.login(account.getEmail(), "Account pass1!").isSuccess());
    }

    private <T extends AccountActivity> void awaitList(ActivityScenario<T> scenario, int listId, String title) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        AtomicBoolean matched = new AtomicBoolean();
        do {
            scenario.onActivity(activity -> {
                ListView list = activity.findViewById(listId);
                int count = list.getAdapter().getCount();
                if (title == null) { matched.set(count == 0); return; }
                if (count != 1) { matched.set(false); return; }
                Object track = list.getAdapter().getItem(0);
                String name = track instanceof Track ? ((Track) track).getName()
                        : ((DownloadedTrackEntity) track).getTrackName();
                matched.set(title.equals(name));
            });
            if (matched.get()) { return; }
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("Expected only the active account's track: " + title);
    }

    private void awaitRelease(CountDownLatch release) {
        try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }

    private void setField(Object object, String name, Object value) {
        try { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value); }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private Object getField(Object object, String name) {
        try { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object); }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
}
