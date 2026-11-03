package shop.authentication.internal.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    private User user(String email, Role role) {
        var user = new User();
        user.setEmail(email);
        user.setPassword("bcrypt-hash");
        user.setRole(role);
        return user;
    }

    @Test
    void savesUserAndAssignsId() {
        var saved = repository.saveAndFlush(user("ana@correo.com", Role.USUARIO));

        assertNotNull(saved.getId());
    }

    @Test
    void findsByEmailWithItsData() {
        repository.saveAndFlush(user("ana@correo.com", Role.ADMIN));

        var found = repository.findByEmail("ana@correo.com").orElseThrow();

        assertEquals("bcrypt-hash", found.getPassword());
        assertEquals(Role.ADMIN, found.getRole());
    }

    @Test
    void findingUnknownEmailReturnsEmpty() {
        assertTrue(repository.findByEmail("nadie@correo.com").isEmpty());
    }

    @Test
    void existsByEmailDistinguishesRegistered() {
        repository.saveAndFlush(user("ana@correo.com", Role.USUARIO));

        assertTrue(repository.existsByEmail("ana@correo.com"));
        assertFalse(repository.existsByEmail("otra@correo.com"));
    }

    @Test
    void emailIsUnique() {
        repository.saveAndFlush(user("ana@correo.com", Role.USUARIO));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(user("ana@correo.com", Role.USUARIO)));
    }
}
