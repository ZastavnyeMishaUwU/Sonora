package com.example.it_robota.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

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
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Connect UserDao to DataBase
     */
    public abstract UserDao userDao();

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
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
