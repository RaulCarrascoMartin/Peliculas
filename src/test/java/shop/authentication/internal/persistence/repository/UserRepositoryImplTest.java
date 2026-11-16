package shop.authentication.internal.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;
import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.repository.UserRepository;
import shop.shared.MariaDbRepositoryTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SQL and row ↔ DTO mapping of users against a real MariaDB. */
@Import(UserRepositoryImpl.class)
class UserRepositoryImplTest extends MariaDbRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private static UserDto user(String email, Role role) {
        return new UserDto(null, email, "bcrypt-hash", role);
    }

    private int count(String email) {
        return jdbc.sql("select count(*) from usuarios where email = :email").param("email", email)
                .query(Integer.class).single();
    }

    @Test
    void saveAssignsIdAndFindByEmailReturnsEveryColumn() {
        UserDto saved = repository.save(user("ana@correo.com", Role.ADMIN));

        assertNotNull(saved.id());
        UserDto found = repository.findByEmail("ana@correo.com").orElseThrow();
        assertEquals(saved, found);
        assertEquals("ana@correo.com", found.email());
        assertEquals("bcrypt-hash", found.passwordHash());
        assertEquals(Role.ADMIN, found.role());
    }

    @Test
    void everyRoleIsStoredAsTextAndReadBack() {
        repository.save(user("usuario@correo.com", Role.USUARIO));

        assertEquals(Role.USUARIO, repository.findByEmail("usuario@correo.com").orElseThrow().role());
        assertEquals("USUARIO", jdbc.sql("select rol from usuarios where email = :email")
                .param("email", "usuario@correo.com").query(String.class).single());
    }

    @Test
    void eachUserGetsItsOwnId() {
        UserDto first = repository.save(user("ana@correo.com", Role.USUARIO));
        UserDto second = repository.save(user("luis@correo.com", Role.USUARIO));

        assertTrue(second.id() > first.id());
        assertEquals(first.id(), repository.findByEmail("ana@correo.com").orElseThrow().id());
    }

    @Test
    void findingUnknownEmailReturnsEmpty() {
        assertTrue(repository.findByEmail("nadie@correo.com").isEmpty());
    }

    @Test
    void existsByEmailDistinguishesRegistered() {
        repository.save(user("ana@correo.com", Role.USUARIO));

        assertTrue(repository.existsByEmail("ana@correo.com"));
        assertFalse(repository.existsByEmail("otra@correo.com"));
    }

    @Test
    void repeatedEmailBecomesTheDomainConflictAndLeavesTheFirstUserUntouched() {
        UserDto first = repository.save(user("ana@correo.com", Role.USUARIO));

        var ex = assertThrows(EmailAlreadyRegisteredException.class,
                () -> repository.save(new UserDto(null, "ana@correo.com", "another-hash", Role.ADMIN)));

        assertEquals("El email ya está registrado", ex.getMessage());
        assertEquals(1, count("ana@correo.com"));
        assertEquals(first, repository.findByEmail("ana@correo.com").orElseThrow());
    }

    @Test
    void userWithoutMandatoryColumnIsRejectedAndLeavesNoRow() {
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(new UserDto(null, "sin.hash@correo.com", null, Role.USUARIO)));

        assertEquals(0, count("sin.hash@correo.com"));
    }
}
