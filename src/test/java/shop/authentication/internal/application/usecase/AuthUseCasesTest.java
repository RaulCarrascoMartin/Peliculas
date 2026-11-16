package shop.authentication.internal.application.usecase;

import shop.authentication.internal.domain.fake.UserRepositoryFake;

import shop.authentication.internal.domain.service.AuthServiceImpl;
import shop.authentication.internal.infrastructure.BcryptPasswordHasher;
import shop.authentication.internal.infrastructure.JwtTokenIssuer;
import shop.authentication.internal.domain.model.Role;
import shop.shared.error.ConflictException;
import shop.shared.security.JwtTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.shared.error.InvalidCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthUseCasesTest {

    private final BcryptPasswordHasher encoder = new BcryptPasswordHasher(new BCryptPasswordEncoder());
    private final JwtTokens tokens = new JwtTokens(
            "test-secret-with-at-least-32-bytes-of-length", Duration.ofHours(1));

    private UserRepositoryFake repository;
    private RegisterUser register;
    private Login login;
    private CreateAdminIfMissing createAdmin;

    @BeforeEach
    void setUp() {
        repository = new UserRepositoryFake();
        var service = new AuthServiceImpl(repository, encoder, new JwtTokenIssuer(tokens));
        register = new RegisterUser(service);
        login = new Login(service);
        createAdmin = new CreateAdminIfMissing(service);
    }

    @Test
    void registersUserWithUserRoleAndEncodedPassword() {
        var user = register.execute("ana@correo.com", "Secreta123");

        var saved = repository.findByEmail("ana@correo.com").orElseThrow();
        assertEquals("USUARIO", user.role());
        assertEquals(saved.getId(), user.id());
        assertNotEquals("Secreta123", saved.getPassword());
        assertTrue(encoder.matches("Secreta123", saved.getPassword()));
    }

    @Test
    void rejectsRegistrationOfDuplicateEmail() {
        register.execute("ana@correo.com", "Secreta123");

        assertThrows(ConflictException.class, () -> register.execute("ana@correo.com", "Otra456"));
        assertEquals(1, repository.count());
    }

    @Test
    void successfulLoginIssuesTokenWithIdAndRole() {
        var user = register.execute("ana@correo.com", "Secreta123");

        var data = tokens.parseToken(login.execute("ana@correo.com", "Secreta123"));

        assertEquals(user.id(), data.id());
        assertEquals("USUARIO", data.role());
    }

    @Test
    void adminLoginCarriesTheirRole() {
        register.execute("root@correo.com", "Secreta123");
        repository.findByEmail("root@correo.com").orElseThrow().setRole(Role.ADMIN);

        assertEquals("ADMIN", tokens.parseToken(login.execute("root@correo.com", "Secreta123")).role());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveSameError() {
        register.execute("ana@correo.com", "Secreta123");

        var badPassword = assertThrows(InvalidCredentialsException.class,
                () -> login.execute("ana@correo.com", "wrong"));
        var badEmail = assertThrows(InvalidCredentialsException.class,
                () -> login.execute("nadie@correo.com", "Secreta123"));

        assertEquals(badPassword.getMessage(), badEmail.getMessage());
    }

    @Test
    void createsInitialAdminWithAdminRoleAndEncodedPassword() {
        createAdmin.execute("root@correo.com", "Secreta123");

        var admin = repository.findByEmail("root@correo.com").orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(encoder.matches("Secreta123", admin.getPassword()));
    }

    @Test
    void creatingAdminTwiceDoesNotDuplicateNorChangePassword() {
        createAdmin.execute("root@correo.com", "Secreta123");
        createAdmin.execute("root@correo.com", "Distinta456");

        assertEquals(1, repository.count());
        var admin = repository.findByEmail("root@correo.com").orElseThrow();
        assertTrue(encoder.matches("Secreta123", admin.getPassword()));
    }

    @Test
    void doesNotPromoteUserAlreadyRegisteredWithThatEmail() {
        register.execute("ana@correo.com", "Secreta123");

        createAdmin.execute("ana@correo.com", "Distinta456");

        assertEquals(Role.USUARIO, repository.findByEmail("ana@correo.com").orElseThrow().getRole());
    }
}
