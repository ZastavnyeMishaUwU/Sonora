package com.example.it_robota.auth;

import java.util.Locale;
import java.util.Objects;

/** Immutable owner of an operation; a new login invalidates old UI callbacks. */
public final class AccountSession {
    private final long userId;
    private final String email;
    private final String token;

    /**
     * Captures an account and its login token for use by a background operation.
     *
     * @param userId database user identifier
     * @param email owner email, normalized before storage
     * @param token token identifying this login, or null for a legacy session
     */
    public AccountSession(long userId, String email, String token) {
        this.userId = userId;
        this.email = normalizeEmail(email);
        this.token = token == null ? "" : token;
    }

    /** @return database identifier of the captured user */
    public long getUserId() { return userId; }

    /** @return trimmed, lowercase owner email */
    public String getEmail() { return email; }

    /**
     * Normalizes an email for account ownership comparisons and storage.
     *
     * @param email email to normalize, or null
     * @return trimmed, lowercase email, or an empty string for null
     */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Compares both the account and the login token, so a new login is a different session.
     *
     * @param other value to compare
     * @return true when the user ID, normalized email and token match
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AccountSession)) { return false; }
        AccountSession account = (AccountSession) other;
        return userId == account.userId && email.equals(account.email) && token.equals(account.token);
    }

    /** @return hash of the same account and token fields used by equals */
    @Override
    public int hashCode() { return Objects.hash(userId, email, token); }
}
