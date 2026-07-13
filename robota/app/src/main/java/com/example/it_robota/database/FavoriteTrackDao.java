package com.example.it_robota.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FavoriteTrackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrack(TrackEntity track);

    @Query("DELETE FROM tracks WHERE id = :trackId")
    void deleteTrack(String trackId);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavoriteLink(FavoriteTrackEntity favorite);
    @Query("SELECT * FROM tracks WHERE id IN (SELECT trackId FROM favorite_tracks WHERE userId = :userId)")
    List<TrackEntity> getFavoriteTracksByUser(long userId);
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE trackId = :trackId AND userId = :userId)")
    boolean isTrackFavorite(String trackId, long userId);

}