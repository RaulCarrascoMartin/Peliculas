package shop.authentication.internal.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.authentication.internal.domain.contract.PasswordHasher;
import shop.authentication.internal.domain.contract.TokenProvider;
import shop.authentication.internal.domain.dto.LoginDto;
import shop.authentication.internal.domain.dto.RegisterUserDto;
import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;
import shop.authentication.internal.domain.exception.IncompleteAdminConfigurationException;
import shop.authentication.internal.domain.exception.LoginFailedException;
import shop.authentication.internal.domain.mapper.UserMapper;
import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.repository.UserRepository;
import shop.shared.error.ConflictException;
import shop.shared.error.InvalidCredentialsException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Authentication rules with the repository, the hasher and the token provider mocked; the mapper is the real one. */
class AuthServiceTest {

    private static final String EMAIL = "ana@correo.com";
    private static final String RAW = "Secreta123";
    private static final String HASH = "hash-of-Secreta123";

    private UserRepository users;
    private PasswordHasher hasher;
    private TokenProvider tokens;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        hasher = mock(PasswordHasher.class);
        tokens = mock(TokenProvider.class);
        service = new AuthServiceImpl(users, hasher, tokens, Mappers.getMapper(UserMapper.class));

        when(hasher.hash(RAW)).thenReturn(HASH);
        when(hasher.matches(RAW, HASH)).thenReturn(true);
        when(users.save(any())).thenAnswer(call -> {
            UserDto u = call.getArgument(0);
            return new UserDto(1L, u.email(), u.passwordHash(), u.role());
        });
    }

    // ---------------------------------------------------------------- register

    @Test
    void registerSavesTheUserWithUserRoleAndTheHashedPassword() {
        service.register(new RegisterUserDto(EMAIL, RAW));

        verify(users).save(new UserDto(null, EMAIL, HASH, Role.USUARIO));
    }

    @Test
    void registerReturnsTheSavedUserWithoutItsPasswordHash() {
        UserDto registered = service.register(new RegisterUserDto(EMAIL, RAW));

        assertEquals(new UserDto(1L, EMAIL, null, Role.USUARIO), registered);
    }

    @Test
    void registerOfAnEmailAlreadyTakenIsRejectedAndSavesNothing() {
        when(users.existsByEmail(EMAIL)).thenReturn(true);

        var ex = assertThrows(EmailAlreadyRegisteredException.class,
                () -> service.register(new RegisterUserDto(EMAIL, "Otra456789")));

        assertInstanceOf(ConflictException.class, ex);
        assertEquals("El email ya está registrado", ex.getMessage());
        verify(users, never()).save(any());
        verifyNoInteractions(hasher);
    }

    @Test
    void registerThatLosesTheRaceLetsTheRepositoryConflictThrough() {
        doThrow(new EmailAlreadyRegisteredException()).when(users).save(any());

        assertThrows(EmailAlreadyRegisteredException.class, () -> service.register(new RegisterUserDto(EMAIL, RAW)));
    }

    // ---------------------------------------------------------------- login

    @Test
    void loginWithTheRightPasswordReturnsTheTokenGeneratedForThatUserWithoutItsHash() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(new UserDto(7L, EMAIL, HASH, Role.USUARIO)));
        when(tokens.generate(new UserDto(7L, EMAIL, null, Role.USUARIO))).thenReturn("token-7-USUARIO");

        assertEquals("token-7-USUARIO", service.login(new LoginDto(EMAIL, RAW)));

        verify(tokens).generate(new UserDto(7L, EMAIL, null, Role.USUARIO));
    }

    @Test
    void adminLoginCarriesItsRoleToTheToken() {
        when(users.findByEmail("root@correo.com"))
                .thenReturn(Optional.of(new UserDto(2L, "root@correo.com", HASH, Role.ADMIN)));
        when(tokens.generate(new UserDto(2L, "root@correo.com", null, Role.ADMIN))).thenReturn("token-2-ADMIN");

        assertEquals("token-2-ADMIN", service.login(new LoginDto("root@correo.com", RAW)));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameErrorAndNoToken() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(new UserDto(7L, EMAIL, HASH, Role.USUARIO)));
        when(users.findByEmail("nadie@correo.com")).thenReturn(Optional.empty());

        var wrongPassword = assertThrows(LoginFailedException.class,
                () -> service.login(new LoginDto(EMAIL, "wrong")));
        var unknownEmail = assertThrows(LoginFailedException.class,
                () -> service.login(new LoginDto("nadie@correo.com", RAW)));

        assertInstanceOf(InvalidCredentialsException.class, wrongPassword);
        assertEquals("Credenciales incorrectas", wrongPassword.getMessage());
        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
        verify(hasher).matches("wrong", HASH);
        verifyNoInteractions(tokens);
        verify(users, never()).save(any());
    }

    // ---------------------------------------------------------------- initial admin

    @Test
    void initialAdminIsCreatedWithAdminRoleAndTheHashedPassword() {
        service.ensureInitialAdmin("root@correo.com", RAW);

        verify(users).save(new UserDto(null, "root@correo.com", HASH, Role.ADMIN));
    }

    @Test
    void withNeitherValueConfiguredNothingHappens() {
        service.ensureInitialAdmin("", "");
        service.ensureInitialAdmin(null, null);
        service.ensureInitialAdmin("  ", null);

        verifyNoInteractions(users, hasher);
    }

    @Test
    void withOnlyOneValueConfiguredItFailsAndCreatesNothing() {
        var onlyEmail = assertThrows(IncompleteAdminConfigurationException.class,
                () -> service.ensureInitialAdmin("root@correo.com", ""));
        assertThrows(IncompleteAdminConfigurationException.class, () -> service.ensureInitialAdmin(null, RAW));
        assertThrows(IllegalStateException.class, () -> service.ensureInitialAdmin("root@correo.com", " "));

        assertTrue(onlyEmail.getMessage().contains("peliculas.admin.email"));
        assertTrue(onlyEmail.getMessage().contains("peliculas.admin.contrasena"));
        verifyNoInteractions(users, hasher);
    }

    @Test
    void existingUserWithThatEmailIsNeverModifiedNorPromoted() {
        when(users.existsByEmail(EMAIL)).thenReturn(true);

        service.ensureInitialAdmin(EMAIL, "Distinta456");

        verify(users, never()).save(any());
        verifyNoInteractions(hasher);
    }
}
