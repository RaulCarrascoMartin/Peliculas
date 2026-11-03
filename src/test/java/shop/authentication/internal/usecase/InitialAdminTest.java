package shop.authentication.internal.usecase;

import shop.authentication.internal.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InitialAdminTest {

    private UserRepositoryFake repository;
    private CreateAdminIfMissing service;

    @BeforeEach
    void setUp() {
        repository = new UserRepositoryFake();
        service = new CreateAdminIfMissing(repository, new BCryptPasswordEncoder());
    }

    @Test
    void withoutConfigurationCreatesNoUser() throws Exception {
        new InitialAdmin(service, "", "").run();

        assertEquals(0, repository.count());
    }

    @Test
    void withEmailAndPasswordCreatesAdmin() throws Exception {
        new InitialAdmin(service, "root@correo.com", "Secreta123").run();

        assertEquals(Role.ADMIN, repository.findByEmail("root@correo.com").orElseThrow().getRole());
    }

    @Test
    void emailWithoutPasswordIsConfigurationError() {
        assertThrows(IllegalStateException.class,
                () -> new InitialAdmin(service, "root@correo.com", "").run());
        assertEquals(0, repository.count());
    }
}
