package com.example.it_robota.auth.validation;

/**
 * Represents the result of validating a password against the registration rules.
 */
public final class PasswordValidationResult {

    private static final PasswordValidationResult VALID_RESULT =
            new PasswordValidationResult(true, null);

    private final boolean valid;
    private final String message;

    private PasswordValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    /**
     * Creates a successful validation result.
     *
     * @return shared successful result
     */
    public static PasswordValidationResult valid() {
        return VALID_RESULT;
    }

    /**
     * Creates a failed validation result with a user-facing explanation.
     *
     * @param message explanation of the failed rule
     * @return failed validation result
     */
    public static PasswordValidationResult invalid(String message) {
        return new PasswordValidationResult(false, message);
    }

    /**
     * Reports whether all password rules passed.
     *
     * @return true when the password is valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the failed-rule message, or null for a valid password.
     *
     * @return validation message or null
     */
    public String getMessage() {
        return message;
    }
}
