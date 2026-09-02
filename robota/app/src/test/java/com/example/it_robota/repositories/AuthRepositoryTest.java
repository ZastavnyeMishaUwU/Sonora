package com.example.it_robota.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import com.example.it_robota.auth.AuthResult;
import com.example.it_robota.auth.SessionManager;
import com.example.it_robota.auth.validation.PasswordValidator;
import com.example.it_robota.database.UserDao;
import com.example.it_robota.database.UserEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Unit tests for {@link AuthRepository} to verify registration, login, logout, and session state operations.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthRepositoryTest {
    @Mock
    private UserDao userDao;

    @Mock
    private SessionManager sessionManager;

    private AuthRepository authRepository;

    /**
     * Initializes mocks and instantiates the AuthRepository before each test execution.
     */
    @Before
    public void setUp() {
        authRepository = new AuthRepository(userDao, sessionManager);
    }

    /**
     * Verifies that registration fails when the provided password is shorter than 6 characters.
     */
    @Test
    public void register_shortPassword_returnsFailure() {
        AuthResult result = authRepository.register("user", "test@example.com", "123");

        assertFalse(result.isSuccess());
        assertEquals("Password must contain at least 6 characters.", result.getMessage());
        verifyNoInteractions(userDao);
    }

    @Test
    public void register_emptyPassword_returnsFailure() {
        assertRegistrationPasswordFailure(
                "",
                PasswordValidator.EMPTY_MESSAGE
        );
    }

    @Test
    public void register_passwordContainingOnlySpaces_returnsFailure() {
        assertRegistrationPasswordFailure(
                "      ",
                PasswordValidator.WHITESPACE_ONLY_MESSAGE
        );
    }

    @Test
    public void register_passwordStartingWithSpace_returnsFailure() {
        assertRegistrationPasswordFailure(
                " Password1!",
                PasswordValidator.BOUNDARY_SPACE_MESSAGE
        );
    }

    @Test
    public void register_passwordEndingWithSpace_returnsFailure() {
        assertRegistrationPasswordFailure(
                "Password1! ",
                PasswordValidator.BOUNDARY_SPACE_MESSAGE
        );
    }

    @Test
    public void register_passwordWithoutUppercase_returnsFailure() {
        assertRegistrationPasswordFailure(
                "password1!",
                PasswordValidator.UPPERCASE_MESSAGE
        );
    }

    @Test
    public void register_passwordWithoutSpecialCharacter_returnsFailure() {
        assertRegistrationPasswordFailure(
                "Password1",
                PasswordValidator.SPECIAL_CHARACTER_MESSAGE
        );
    }

    /**
     * Verifies that registration fails when the username contains only whitespace characters.
     */
    @Test
    public void register_emptyUsername_returnsFailure() {
        AuthResult result = authRepository.register("  ", "test@example.com", "1234567");

        assertFalse(result.isSuccess());
        assertEquals("Username must not be empty.", result.getMessage());
        verifyNoInteractions(userDao);
    }

    /**
     * Verifies that registration fails when the provided email does not follow standard email formatting.
     */
    @Test
    public void register_invalidEmail_returnsFailure() {
        AuthResult result = authRepository.register("user", "testexample.com", "1234567");

        assertFalse(result.isSuccess());
        assertEquals("Email is not valid.", result.getMessage());
        verifyNoInteractions(userDao);
    }

    /**
     * Verifies that registration fails if the user email already exists in the database.
     */
    @Test
    public void register_UserAlreadyExists_ReturnsFailure() {
        String email = "test@example.com";
        when(userDao.checkUserExists(email)).thenReturn(true);

        AuthResult result = authRepository.register("John", email, "Password123!");

        assertFalse(result.isSuccess());
        assertEquals("User with this email already exists.", result.getMessage());
        verify(userDao, never()).insertUser(any());
    }

    /**
     * Verifies successful registration, ensuring inputs are normalized, credentials are hashed,
     * the user is persisted, and the active session is initialized.
     *
     * @throws Exception if password hashing fails
     */
    @Test
    public void register_Success_NormalizesDataAndSavesSession() throws Exception {
        String rawEmail = "  TEST@Example.com  ";
        String normalizedEmail = "test@example.com";
        String password = "Secure password123!";

        when(userDao.checkUserExists(normalizedEmail)).thenReturn(false);

        UserEntity createdUserInDb = new UserEntity(
                42L,
                "John",
                normalizedEmail,
                hashPassword(password),
                System.currentTimeMillis()
        );
        when(userDao.getUserByEmail(normalizedEmail)).thenReturn(createdUserInDb);

        AuthResult result = authRepository.register("  John  ", rawEmail, password);

        assertTrue(result.isSuccess());
        assertEquals("User registered successfully.", result.getMessage());
        assertNotNull(result.getUser());
        assertEquals(42L, result.getUser().getId());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userDao).insertUser(captor.capture());
        UserEntity insertedUser = captor.getValue();

        assertEquals("John", insertedUser.getUsername());
        assertEquals(normalizedEmail, insertedUser.getEmail());
        assertEquals(hashPassword(password), insertedUser.getPasswordHash());

        verify(sessionManager).saveSession(42L, normalizedEmail);
    }

    /**
     * Verifies that login fails when the password parameter is empty.
     */
    @Test
    public void login_emptyPassword_returnsFailure() {
        AuthResult result = authRepository.login("test@example.com", "");

        assertFalse(result.isSuccess());
        assertEquals("Password must not be empty.", result.getMessage());
        verifyNoInteractions(userDao);
    }

    /**
     * Verifies that login fails when the email format is invalid.
     */
    @Test
    public void login_invalidEmail_returnsFailure() {
        AuthResult result = authRepository.login("testexample.com", "1234567");

        assertFalse(result.isSuccess());
        assertEquals("Email is not valid.", result.getMessage());
        verifyNoInteractions(userDao);
    }

    /**
     * Verifies that login fails when no user record matches the supplied email address.
     */
    @Test
    public void login_UserNotFound_ReturnsFailure() {
        String email = "notfound@example.com";
        when(userDao.getUserByEmail(email)).thenReturn(null);

        AuthResult result = authRepository.login(email, "password123");

        assertFalse(result.isSuccess());
        assertEquals("Email or password is incorrect.", result.getMessage());
        verify(sessionManager, never()).saveSession(anyLong(), any());
    }

    /**
     * Verifies that login fails when the supplied password does not match the stored hash.
     *
     * @throws Exception if password hashing fails during setup
     */
    @Test
    public void login_WrongPassword_ReturnsFailure() throws Exception {
        String email = "test@example.com";
        UserEntity user = new UserEntity(1L, "John", email, hashPassword("correctPassword"), System.currentTimeMillis());
        when(userDao.getUserByEmail(email)).thenReturn(user);

        AuthResult result = authRepository.login(email, "wrongPassword");

        assertFalse(result.isSuccess());
        assertEquals("Email or password is incorrect.", result.getMessage());
        verify(sessionManager, never()).saveSession(anyLong(), any());
    }

    /**
     * Verifies successful login using valid credentials, ensuring session persistence.
     *
     * @throws Exception if hash generation encounters an error
     */
    @Test
    public void login_validCredentials_savesSessionAndReturnsSuccess() throws Exception {
        String email = "test@example.com";
        String password = "password123";
        UserEntity mockUser = new UserEntity(1, "user", email, "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f", System.currentTimeMillis());

        when(userDao.getUserByEmail(email)).thenReturn(mockUser);

        AuthResult result = authRepository.login(email, password);

        assertTrue(result.isSuccess());
        verify(sessionManager).saveSession(1, email);
    }

    /**
     * Verifies that invoking logout clears the current session in SessionManager.
     */
    @Test
    public void logout_CallsSessionManagerClearSession() {
        authRepository.logout();
        verify(sessionManager).clearSession();
    }

    /**
     * Verifies that checking logged-in status accurately delegates and returns state from SessionManager.
     */
    @Test
    public void isUserLoggedIn_ReturnsSessionManagerStatus() {
        when(sessionManager.isLoggedIn()).thenReturn(true);
        assertTrue(authRepository.isUserLoggedIn());

        when(sessionManager.isLoggedIn()).thenReturn(false);
        assertFalse(authRepository.isUserLoggedIn());
    }

    private void assertRegistrationPasswordFailure(
            String password,
            String expectedMessage
    ) {
        AuthResult result = authRepository.register(
                "user",
                "test@example.com",
                password
        );

        assertFalse(result.isSuccess());
        assertEquals(expectedMessage, result.getMessage());
        verifyNoInteractions(userDao);
    }

    /**
     * Helper method to compute SHA-256 password hash for test verification.
     *
     * @param password raw password string
     * @return hexadecimal string representation of the hash
     * @throws Exception if SHA-256 algorithm is not supported
     */
    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(
                password.getBytes(StandardCharsets.UTF_8)
        );
        StringBuilder hash = new StringBuilder();

        for (byte hashByte : hashBytes) {
            hash.append(
                    String.format(
                            Locale.ROOT,
                            "%02x",
                            hashByte
                    )
            );
        }
        return hash.toString();
    }
}
