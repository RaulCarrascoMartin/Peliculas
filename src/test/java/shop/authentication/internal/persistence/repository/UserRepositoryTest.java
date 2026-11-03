package shop.authentication.internal.persistence.repository;

import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.model.User;
import shop.authentication.internal.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Import;
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
@Import(UserRepositoryImpl.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private EntityManager em;

    private <T> T flushed(T saved) {
        em.flush();
        return saved;
    }

    private User user(String email, Role role) {
        var user = new User();
        user.setEmail(email);
        user.setPassword("bcrypt-hash");
        user.setRole(role);
        return user;
    }

    @Test
    void savesUserAndAssignsId() {
        var saved = flushed(repository.save(user("ana@correo.com", Role.USUARIO)));

        assertNotNull(saved.getId());
    }

    @Test
    void findsByEmailWithItsData() {
        flushed(repository.save(user("ana@correo.com", Role.ADMIN)));

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
        flushed(repository.save(user("ana@correo.com", Role.USUARIO)));

        assertTrue(repository.existsByEmail("ana@correo.com"));
        assertFalse(repository.existsByEmail("otra@correo.com"));
    }

    @Test
    void emailIsUnique() {
        flushed(repository.save(user("ana@correo.com", Role.USUARIO)));

        assertThrows(DataIntegrityViolationException.class,
                () -> flushed(repository.save(user("ana@correo.com", Role.USUARIO))));
    }
}
