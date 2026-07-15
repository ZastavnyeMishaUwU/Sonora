package com.example.it_robota.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.it_robota.models.User;

@Dao
public interface UserDao {

    /**
     * Inserts a new user into the database.
     */
    @Insert
    void insertUser(User user);

    /**
     * Returns a user by email.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    /**
     * Returns a user by ID.
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(long id);

    /**
     * Checks if a user with the given email exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    boolean checkUserExists(String email);
}