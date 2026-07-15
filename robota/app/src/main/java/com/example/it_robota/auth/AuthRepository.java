package com.example.it_robota.auth;

import android.content.Context;

public class AuthRepository {

    public AuthRepository(Context context) {
        // Конструктор репозиторію
    }

    /**
     * Метод реєстрації користувача.
     * Повертає AuthResult із результатом успішності, повідомленням та об'єктом User (наразі null).
     */
    public AuthResult register(String username, String email, String password) {
        // Тимчасова заглушка: успішна реєстрація для перевірки переходів між екранами
        return new AuthResult(true, "Registration successful", null);
    }
}