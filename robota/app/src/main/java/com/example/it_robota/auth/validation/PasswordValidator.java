package com.example.it_robota.auth.validation;

/**
 * Applies reusable password rules for account registration.
 */
public final class PasswordValidator {

    public static final int MINIMUM_LENGTH = 6;

    public static final String EMPTY_MESSAGE =
            "Password must not be empty.";
    public static final String BOUNDARY_SPACE_MESSAGE =
            "Password must not start or end with a space.";
    public static final String MINIMUM_LENGTH_MESSAGE =
            "Password must contain at least 6 characters.";
    public static final String UPPERCASE_MESSAGE =
            "Password must contain at least one uppercase letter.";
    public static final String SPECIAL_CHARACTER_MESSAGE =
            "Password must contain at least one special character.";

    /**
     * Applies every registration rule in a stable, user-friendly order.
     *
     * @param password raw password entered by the user
     * @return the first failed rule or a successful result
     */
    public PasswordValidationResult validate(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordValidationResult.invalid(EMPTY_MESSAGE);
        }

        if (!hasValidSpacePlacement(password)) {
            return PasswordValidationResult.invalid(BOUNDARY_SPACE_MESSAGE);
        }

        if (!hasMinimumLength(password)) {
            return PasswordValidationResult.invalid(MINIMUM_LENGTH_MESSAGE);
        }

        if (!containsUppercaseLetter(password)) {
            return PasswordValidationResult.invalid(UPPERCASE_MESSAGE);
        }

        if (!containsSpecialCharacter(password)) {
            return PasswordValidationResult.invalid(SPECIAL_CHARACTER_MESSAGE);
        }

        return PasswordValidationResult.valid();
    }

    /**
     * Checks the minimum password length.
     *
     * @param password raw password value
     * @return true when the value has enough characters
     */
    public boolean hasMinimumLength(String password) {
        return password != null && password.length() >= MINIMUM_LENGTH;
    }

    /**
     * Allows whitespace inside a password but rejects it at either boundary.
     *
     * @param password raw password value
     * @return true when the first and last symbols are not whitespace
     */
    public boolean hasValidSpacePlacement(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        int firstSymbol = password.codePointAt(0);
        int lastSymbol = password.codePointBefore(password.length());

        return !Character.isWhitespace(firstSymbol)
                && !Character.isWhitespace(lastSymbol);
    }

    /**
     * Checks for at least one uppercase Unicode letter.
     *
     * @param password raw password value
     * @return true when an uppercase letter is present
     */
    public boolean containsUppercaseLetter(String password) {
        return password != null
                && password.codePoints().anyMatch(Character::isUpperCase);
    }

    /**
     * Checks for a non-alphanumeric, non-whitespace symbol.
     * Interior spaces are allowed but do not count as special characters.
     *
     * @param password raw password value
     * @return true when a special character is present
     */
    public boolean containsSpecialCharacter(String password) {
        return password != null
                && password.codePoints().anyMatch(symbol ->
                !Character.isLetterOrDigit(symbol)
                        && !Character.isWhitespace(symbol)
        );
    }
}
