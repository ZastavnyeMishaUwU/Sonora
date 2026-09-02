package com.example.it_robota.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * DAO for working with users in the local Room database.
 */
@Dao
public interface UserDao {

    /**
     * Inserts a user into the local database.
     *
     * @param userEntity user entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertUser(UserEntity userEntity);

    /**
     * Returns a user by email.
     *
     * @param email user email
     * @return found user entity or null
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);

    /**
     * Returns a user by ID.
     *
     * @param id user ID
     * @return found user entity or null
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getUserById(long id);

    /**
     * Checks if a user with this email exists.
     *
     * @param email user email
     * @return true if user exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    boolean checkUserExists(String email);
}
