package com.example.it_robota.downloader;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.database.AppDatabase;
import com.example.it_robota.database.DownloadedTrackDao;
import com.example.it_robota.database.DownloadedTrackEntity;
import com.example.it_robota.models.Track;
import com.example.it_robota.storage.LocalFileStorageManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Verifies download persistence and cleanup against a local HTTP endpoint.
 */
@RunWith(AndroidJUnit4.class)
public class TrackDownloadManagerInstrumentedTest {

    private static final long TEST_USER_ID = 440044L;
    private static final String SUCCESS_TRACK_ID = "task44_success";
    private static final String FAILURE_TRACK_ID = "task44_failure";
    private static final String INVALID_TRACK_ID = "task44_invalid";

    private Context context;
    private SessionManager sessionManager;
    private DownloadedTrackDao downloadedTrackDao;
    private LocalFileStorageManager storageManager;
    private boolean previousLoggedIn;
    private long previousUserId;
    private String previousEmail;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        sessionManager = new SessionManager(context);
        downloadedTrackDao = AppDatabase.getInstance(context).downloadedTrackDao();
        storageManager = new LocalFileStorageManager(context);

        previousLoggedIn = sessionManager.isLoggedIn();
        previousUserId = sessionManager.getCurrentUserId();
        previousEmail = sessionManager.getCurrentUserEmail();

        removeTestTrack(SUCCESS_TRACK_ID);
        removeTestTrack(FAILURE_TRACK_ID);
        removeTestTrack(INVALID_TRACK_ID);
        sessionManager.saveSession(TEST_USER_ID, "download-test@example.com");
    }

    @After
    public void tearDown() {
        removeTestTrack(SUCCESS_TRACK_ID);
        removeTestTrack(FAILURE_TRACK_ID);
        removeTestTrack(INVALID_TRACK_ID);

        if (previousLoggedIn && previousUserId >= 0) {
            sessionManager.saveSession(previousUserId, previousEmail);
        } else {
            sessionManager.clearSession();
        }
    }

    @Test
    public void downloadTrack_savesFileAndRoomRecord() throws Exception {
        byte[] audioBytes = new byte[]{73, 68, 51, 4, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4};
        String downloadUrl = startSingleResponseServer(200, "audio/mpeg", audioBytes);
        Track track = createTrack(SUCCESS_TRACK_ID, downloadUrl);

        TrackDownloadManager downloadManager = new TrackDownloadManager(context);
        downloadManager.downloadTrack(track);

        String localPath = storageManager.buildFilePath(SUCCESS_TRACK_ID);
        File localFile = new File(localPath);
        DownloadedTrackEntity record = downloadedTrackDao.getDownloadedTrack(
                SUCCESS_TRACK_ID,
                TEST_USER_ID
        );

        assertTrue(localFile.isFile());
        assertArrayEquals(audioBytes, readFile(localFile));
        assertNotNull(record);
        assertEquals(localPath, record.getLocalPath());
        assertEquals("Test track", record.getTrackName());
        assertEquals("Test artist", record.getArtistName());
        assertEquals(localPath, track.getLocalFilePath());
        assertTrue(downloadManager.isTrackDownloaded(SUCCESS_TRACK_ID));
    }

    @Test
    public void downloadTrack_serverFailureLeavesNoFileOrRoomRecord() throws Exception {
        String downloadUrl = startSingleResponseServer(
                500,
                "text/plain",
                "download failed".getBytes(StandardCharsets.UTF_8)
        );
        Track track = createTrack(FAILURE_TRACK_ID, downloadUrl);
        TrackDownloadManager downloadManager = new TrackDownloadManager(context);

        assertThrows(Exception.class, () -> downloadManager.downloadTrack(track));

        String localPath = storageManager.buildFilePath(FAILURE_TRACK_ID);
        assertFalse(new File(localPath).exists());
        assertFalse(new File(localPath + ".part").exists());
        assertNull(downloadedTrackDao.getDownloadedTrack(FAILURE_TRACK_ID, TEST_USER_ID));
        assertNull(track.getLocalFilePath());
    }

    @Test
    public void downloadTrack_nonMp3ResponseLeavesNoFileOrRoomRecord() throws Exception {
        String downloadUrl = startSingleResponseServer(
                200,
                "text/html",
                "<html>not an audio file</html>".getBytes(StandardCharsets.UTF_8)
        );
        Track track = createTrack(INVALID_TRACK_ID, downloadUrl);
        TrackDownloadManager downloadManager = new TrackDownloadManager(context);

        assertThrows(Exception.class, () -> downloadManager.downloadTrack(track));

        String localPath = storageManager.buildFilePath(INVALID_TRACK_ID);
        assertFalse(new File(localPath).exists());
        assertFalse(new File(localPath + ".part").exists());
        assertNull(downloadedTrackDao.getDownloadedTrack(INVALID_TRACK_ID, TEST_USER_ID));
    }

    private Track createTrack(String trackId, String downloadUrl) {
        return new Track(
                trackId,
                "Test track",
                "Test artist",
                "Test album",
                1,
                downloadUrl,
                downloadUrl,
                "",
                "",
                false,
                null
        );
    }

    private String startSingleResponseServer(int statusCode,
                                             String contentType,
                                             byte[] body) throws IOException {
        ServerSocket serverSocket = new ServerSocket(
                0,
                1,
                InetAddress.getByName("127.0.0.1")
        );

        Thread serverThread = new Thread(() -> {
            try (ServerSocket server = serverSocket;
                 Socket socket = server.accept();
                 OutputStream output = socket.getOutputStream()) {
                String reason = statusCode >= 200 && statusCode < 300 ? "OK" : "Error";
                String headers = "HTTP/1.1 " + statusCode + " " + reason + "\r\n"
                        + "Content-Type: " + contentType + "\r\n"
                        + "Content-Length: " + body.length + "\r\n"
                        + "Connection: close\r\n\r\n";
                output.write(headers.getBytes(StandardCharsets.US_ASCII));
                output.write(body);
                output.flush();
            } catch (IOException ignored) {
                // The client reports connection failures through the download result.
            }
        }, "track-download-test-server");
        serverThread.start();

        return "http://127.0.0.1:" + serverSocket.getLocalPort() + "/track.mp3";
    }

    private byte[] readFile(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            return output.toByteArray();
        }
    }

    private void removeTestTrack(String trackId) {
        downloadedTrackDao.deleteDownloadedTrack(trackId, TEST_USER_ID);
        storageManager.deleteFile(storageManager.buildFilePath(trackId));
        storageManager.deleteFile(storageManager.buildFilePath(trackId) + ".part");
    }
}
