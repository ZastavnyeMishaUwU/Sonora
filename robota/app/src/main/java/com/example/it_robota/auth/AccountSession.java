package com.example.it_robota.auth;

import java.util.Locale;
import java.util.Objects;

/** Immutable owner of an operation; a new login invalidates old UI callbacks. */
public final class AccountSession {
    private final long userId;
    private final String email;
    private final String token;

    public AccountSession(long userId, String email, String token) {
        this.userId = userId;
        this.email = normalizeEmail(email);
        this.token = token == null ? "" : token;
    }

    public long getUserId() { return userId; }
    public String getEmail() { return email; }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AccountSession)) { return false; }
        AccountSession account = (AccountSession) other;
        return userId == account.userId && email.equals(account.email) && token.equals(account.token);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, email, token); }
}
