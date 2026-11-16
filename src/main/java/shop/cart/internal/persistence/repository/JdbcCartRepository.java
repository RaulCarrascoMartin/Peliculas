package shop.cart.internal.persistence.repository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.repository.CartRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/** JDBC persistence of carts over the {@code carritos} table of {@code init.sql}. */
public class JdbcCartRepository implements CartRepository {

    private final JdbcClient jdbc;

    public JdbcCartRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Cart save(Cart cart) {
        return cart.getId() == null ? insert(cart) : update(cart);
    }

    private Cart insert(Cart cart) {
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.sql("insert into carritos (usuario_id, version, actualizado_en) values (?, 0, ?)")
                    .param(cart.getUserId())
                    .param(cart.getUpdatedAt() == null ? null : Timestamp.valueOf(cart.getUpdatedAt()))
                    .update(keys, "id");
        } catch (DataIntegrityViolationException e) {
            if (cart.getUserId() != null) {
                throw new CartAlreadyExistsException(cart.getUserId());
            }
            throw e;
        }
        cart.setId(keys.getKey().longValue());
        cart.setVersion(0L);
        return cart;
    }

    /** Optimistic lock: the update only applies if nobody changed the cart since it was read. */
    private Cart update(Cart cart) {
        int updated = jdbc.sql("update carritos set usuario_id = ?, actualizado_en = ?, version = version + 1 "
                        + "where id = ? and version = ?")
                .param(cart.getUserId())
                .param(cart.getUpdatedAt() == null ? null : Timestamp.valueOf(cart.getUpdatedAt()))
                .param(cart.getId())
                .param(cart.getVersion())
                .update();
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "Cart " + cart.getId() + " was updated or deleted by another transaction");
        }
        cart.setVersion(cart.getVersion() + 1);
        return cart;
    }

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return jdbc.sql("select id, usuario_id, version, actualizado_en from carritos where usuario_id = ?")
                .param(userId)
                .query(JdbcCartRepository::map)
                .optional();
    }

    private static Cart map(ResultSet rs, int row) throws SQLException {
        var c = new Cart();
        c.setId(rs.getLong("id"));
        c.setUserId(rs.getLong("usuario_id"));
        c.setVersion(rs.getLong("version"));
        Timestamp updatedAt = rs.getTimestamp("actualizado_en");
        c.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return c;
    }
}
