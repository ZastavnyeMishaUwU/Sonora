package com.example.it_robota.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Main Room database for the Android music application.
 * Stores users, tracks, favorite tracks and downloaded tracks.
 */
@Database(
        entities = {
                UserEntity.class,
                TrackEntity.class,
                FavoriteTrackEntity.class,
                DownloadedTrackEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE users_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "username TEXT, email TEXT, passwordHash TEXT, createdAt INTEGER NOT NULL)");
            database.execSQL("INSERT INTO users_new SELECT id, username, email, passwordHash, createdAt FROM users");
            database.execSQL("DROP TABLE users");
            database.execSQL("ALTER TABLE users_new RENAME TO users");
            database.execSQL("ALTER TABLE favorite_tracks ADD COLUMN ownerEmail TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE downloaded_tracks ADD COLUMN ownerEmail TEXT NOT NULL DEFAULT ''");
            // Old registrations reused ID 0. Its mixed records cannot safely be attributed
            // to the last account, so keep them unassigned rather than expose another user's data.
            for (String table : new String[]{"favorite_tracks", "downloaded_tracks"}) {
                database.execSQL("UPDATE " + table + " SET ownerEmail = COALESCE((SELECT lower(trim(email)) "
                        + "FROM users WHERE users.id = " + table + ".userId), '') WHERE userId > 0");
            }
            database.execSQL("CREATE TABLE favorite_tracks_new (userId INTEGER NOT NULL, "
                    + "ownerEmail TEXT NOT NULL DEFAULT '', trackId TEXT NOT NULL, "
                    + "PRIMARY KEY(userId, ownerEmail, trackId))");
            database.execSQL("INSERT INTO favorite_tracks_new SELECT userId, ownerEmail, trackId FROM favorite_tracks");
            database.execSQL("DROP TABLE favorite_tracks");
            database.execSQL("ALTER TABLE favorite_tracks_new RENAME TO favorite_tracks");
            database.execSQL("CREATE TABLE downloaded_tracks_new (userId INTEGER NOT NULL, "
                    + "ownerEmail TEXT NOT NULL DEFAULT '', trackId TEXT NOT NULL, trackName TEXT, "
                    + "artistName TEXT, localPath TEXT, PRIMARY KEY(userId, ownerEmail, trackId))");
            database.execSQL("INSERT INTO downloaded_tracks_new SELECT userId, ownerEmail, trackId, "
                    + "trackName, artistName, localPath FROM downloaded_tracks");
            database.execSQL("DROP TABLE downloaded_tracks");
            database.execSQL("ALTER TABLE downloaded_tracks_new RENAME TO downloaded_tracks");
        }
    };

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {

        /**
         * Adds offline display metadata while preserving existing downloaded records.
         *
         * @param database database being upgraded
         */
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE downloaded_tracks ADD COLUMN trackName TEXT");
            database.execSQL("ALTER TABLE downloaded_tracks ADD COLUMN artistName TEXT");
        }
    };

    /**
     * Connect UserDao to DataBase
     */
    public abstract UserDao userDao();

    /**
     * Connects DownloadedTrackDao to the database for managing downloaded tracks.
     *
     * @return DownloadedTrackDao instance
     */
    public abstract DownloadedTrackDao downloadedTrackDao();

    /**
     * Connects FavoriteTrackDao to the database for managing user's favorite tracks.
     *
     * @return FavoriteTrackDao instance
     */
    public abstract FavoriteTrackDao favoriteTrackDao();


    private static volatile AppDatabase INSTANCE;

    /**
     * Returns singleton instance of the application database.
     *
     * @param context application or activity context
     * @return AppDatabase instance
     */
    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "jamendo_music_db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
