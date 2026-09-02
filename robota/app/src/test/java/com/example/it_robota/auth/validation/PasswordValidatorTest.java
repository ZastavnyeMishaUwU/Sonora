package com.example.it_robota.auth.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for every reusable registration password rule.
 */
public class PasswordValidatorTest {

    private PasswordValidator validator;

    @Before
    public void setUp() {
        validator = new PasswordValidator();
    }

    @Test
    public void emptyPasswordIsRejectedWithClearMessage() {
        assertInvalid(null, PasswordValidator.EMPTY_MESSAGE);
        assertInvalid("", PasswordValidator.EMPTY_MESSAGE);
    }

    @Test
    public void shortPasswordIsRejectedWithClearMessage() {
        assertInvalid("A!234", PasswordValidator.MINIMUM_LENGTH_MESSAGE);
    }

    @Test
    public void whitespaceOnlyPasswordIsRejectedBeforeOtherRules() {
        assertInvalid(" ", PasswordValidator.WHITESPACE_ONLY_MESSAGE);
        assertInvalid("      ", PasswordValidator.WHITESPACE_ONLY_MESSAGE);
        assertInvalid(" \t\n ", PasswordValidator.WHITESPACE_ONLY_MESSAGE);
    }

    @Test
    public void whitespaceOnlyRuleDoesNotRejectEmptyOrMixedPasswords() {
        assertTrue(validator.isWhitespaceOnly("      "));
        assertFalse(validator.isWhitespaceOnly(null));
        assertFalse(validator.isWhitespaceOnly(""));
        assertFalse(validator.isWhitespaceOnly("Valid pass1!"));
    }

    @Test
    public void leadingSpaceIsRejectedWithClearMessage() {
        assertInvalid(" Password1!", PasswordValidator.BOUNDARY_SPACE_MESSAGE);
    }

    @Test
    public void trailingSpaceIsRejectedWithClearMessage() {
        assertInvalid("Password1! ", PasswordValidator.BOUNDARY_SPACE_MESSAGE);
    }

    @Test
    public void passwordWithoutUppercaseIsRejectedWithClearMessage() {
        assertInvalid("password1!", PasswordValidator.UPPERCASE_MESSAGE);
    }

    @Test
    public void passwordWithoutSpecialCharacterIsRejectedWithClearMessage() {
        assertInvalid("Password1", PasswordValidator.SPECIAL_CHARACTER_MESSAGE);
    }

    @Test
    public void interiorSpacesAreAllowedButDoNotCountAsSpecialCharacters() {
        assertInvalid("Password 1", PasswordValidator.SPECIAL_CHARACTER_MESSAGE);

        PasswordValidationResult result = validator.validate("Valid pass1!");

        assertTrue(result.isValid());
        assertNull(result.getMessage());
    }

    @Test
    public void reusableRuleMethodsReportTheirOwnState() {
        assertTrue(validator.hasMinimumLength("A!2345"));
        assertTrue(validator.hasValidSpacePlacement("A 234!"));
        assertTrue(validator.containsUppercaseLetter("testA!"));
        assertTrue(validator.containsSpecialCharacter("Test!"));

        assertFalse(validator.hasMinimumLength("A!23"));
        assertFalse(validator.hasValidSpacePlacement(" A234!"));
        assertFalse(validator.containsUppercaseLetter("test!"));
        assertFalse(validator.containsSpecialCharacter("Test1"));
    }

    private void assertInvalid(String password, String expectedMessage) {
        PasswordValidationResult result = validator.validate(password);

        assertFalse(result.isValid());
        assertEquals(expectedMessage, result.getMessage());
    }
}
