package com.example.it_robota.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.it_robota.models.User;

@Dao
public interface UserDao {

    @Insert
    void insertUser(User user);


    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);


    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(long id);


    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    boolean checkUserExists(String email);
}