package shop.authentication.internal.domain.service;

import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;
import shop.authentication.internal.domain.exception.LoginFailedException;
import shop.authentication.internal.domain.model.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import shop.authentication.internal.domain.fake.UserRepositoryFake;
import shop.authentication.internal.infrastructure.BcryptPasswordHasher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private UserRepositoryFake users;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = new UserRepositoryFake();
        service = new AuthServiceImpl(users, new BcryptPasswordHasher(new BCryptPasswordEncoder()),
                (id, role) -> "token-" + id + "-" + role);
    }

    @Test
    void registersWithUserRoleHashedPasswordAndRejectsDuplicates() {
        var user = service.register("ana@correo.com", "Secreta123");

        assertEquals(Role.USUARIO, user.getRole());
        assertNotEquals("Secreta123", user.getPassword());
        assertThrows(EmailAlreadyRegisteredException.class, () -> service.register("ana@correo.com", "Otra12345"));
    }

    @Test
    void loginFailsTheSameWayForUnknownEmailAndWrongPassword() {
        service.register("ana@correo.com", "Secreta123");

        assertEquals("token-1-USUARIO", service.login("ana@correo.com", "Secreta123"));
        var wrongPassword = assertThrows(LoginFailedException.class, () -> service.login("ana@correo.com", "no"));
        var unknownEmail = assertThrows(LoginFailedException.class, () -> service.login("x@correo.com", "Secreta123"));
        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    @Test
    void initialAdminIsCreatedOnlyWhenFullyConfiguredAndNeverOverwritten() {
        service.ensureInitialAdmin("", "");
        assertEquals(0, users.count());

        assertThrows(IllegalStateException.class, () -> service.ensureInitialAdmin("root@correo.com", ""));
        assertEquals(0, users.count());

        service.ensureInitialAdmin("root@correo.com", "Secreta123");
        service.ensureInitialAdmin("root@correo.com", "OtraClave123");

        assertEquals(1, users.count());
        assertEquals(Role.ADMIN, users.findByEmail("root@correo.com").orElseThrow().getRole());
    }
}
