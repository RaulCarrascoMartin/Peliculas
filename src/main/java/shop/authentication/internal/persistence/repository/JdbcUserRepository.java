package shop.authentication.internal.persistence.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.model.User;
import shop.authentication.internal.domain.repository.UserRepository;

import java.util.Optional;

/** JDBC persistence of users over the {@code usuarios} table of {@code init.sql}. */
public class JdbcUserRepository implements UserRepository {

    private final JdbcClient jdbc;

    public JdbcUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.sql("insert into usuarios (email, contrasena, rol) values (?, ?, ?)")
                    .param(user.getEmail())
                    .param(user.getPassword())
                    .param(user.getRole().name())
                    .update(keys, "id");
            user.setId(keys.getKey().longValue());
        } else {
            jdbc.sql("update usuarios set email = ?, contrasena = ?, rol = ? where id = ?")
                    .param(user.getEmail())
                    .param(user.getPassword())
                    .param(user.getRole().name())
                    .param(user.getId())
                    .update();
        }
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbc.sql("select id, email, contrasena, rol from usuarios where email = ?")
                .param(email)
                .query((rs, n) -> {
                    var u = new User();
                    u.setId(rs.getLong("id"));
                    u.setEmail(rs.getString("email"));
                    u.setPassword(rs.getString("contrasena"));
                    u.setRole(Role.valueOf(rs.getString("rol")));
                    return u;
                })
                .optional();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jdbc.sql("select count(*) from usuarios where email = ?")
                .param(email)
                .query(Long.class)
                .single() > 0;
    }
}
