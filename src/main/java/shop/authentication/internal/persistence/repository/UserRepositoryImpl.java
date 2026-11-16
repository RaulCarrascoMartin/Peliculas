package shop.authentication.internal.persistence.repository;

import jakarta.inject.Named;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;
import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.repository.UserRepository;

import java.util.Optional;

/** JDBC persistence of users over the {@code usuarios} table (Flyway migrations). */
@Named
public class UserRepositoryImpl implements UserRepository {

    private static final RowMapper<UserDto> MAPPER = (rs, n) -> new UserDto(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("contrasena"),
            Role.valueOf(rs.getString("rol")));

    private final JdbcClient jdbc;

    public UserRepositoryImpl(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDto save(UserDto user) {
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.sql("insert into usuarios (email, contrasena, rol) values (:email, :passwordHash, :role)")
                    .param("email", user.email())
                    .param("passwordHash", user.passwordHash())
                    .param("role", user.role().name())
                    .update(keys, "id");
        } catch (DuplicateKeyException e) {
            // the only UNIQUE of the table is the email: two registrations of the same email raced
            throw new EmailAlreadyRegisteredException();
        }
        return new UserDto(keys.getKey().longValue(), user.email(), user.passwordHash(), user.role());
    }

    @Override
    public Optional<UserDto> findByEmail(String email) {
        return jdbc.sql("select id, email, contrasena, rol from usuarios where email = :email")
                .param("email", email)
                .query(MAPPER)
                .optional();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jdbc.sql("select count(*) from usuarios where email = :email")
                .param("email", email)
                .query(Long.class)
                .single() > 0;
    }
}
