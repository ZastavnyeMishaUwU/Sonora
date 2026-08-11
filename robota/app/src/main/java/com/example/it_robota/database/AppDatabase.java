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
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

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
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
