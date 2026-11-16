package shop.cart.internal.persistence.repository;

import jakarta.inject.Named;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.cart.internal.domain.dto.CartDto;
import shop.cart.internal.domain.dto.CartLineDto;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.exception.MovieAlreadyInCartException;
import shop.cart.internal.domain.repository.CartRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC persistence of carts over the {@code carritos} and {@code lineas_carrito} tables (Flyway migrations). The
 * lines are children of the cart: this repository inserts, reads and deletes them.
 *
 * <p>The optimistic lock on {@code carritos.version} is hand-made and relies on the REPEATABLE READ isolation
 * configured for the datasource: the transaction keeps the version it read at the beginning, so the conditional
 * update of the loser of two simultaneous checkouts matches no row.
 */
@Named
public class CartRepositoryImpl implements CartRepository {

    private static final RowMapper<CartDto> CART_MAPPER = (rs, n) -> {
        Timestamp updatedAt = rs.getTimestamp("actualizado_en");
        return new CartDto(
                rs.getLong("id"),
                rs.getLong("usuario_id"),
                rs.getLong("version"),
                updatedAt == null ? null : updatedAt.toLocalDateTime());
    };

    private static final RowMapper<CartLineDto> LINE_MAPPER = (rs, n) -> new CartLineDto(
            rs.getLong("id"),
            rs.getLong("carrito_id"),
            rs.getLong("pelicula_id"));

    private final JdbcClient jdbc;

    public CartRepositoryImpl(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CartDto save(CartDto cart) {
        return cart.id() == null ? insert(cart) : update(cart);
    }

    private CartDto insert(CartDto cart) {
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.sql("insert into carritos (usuario_id, version, actualizado_en) values (:userId, 0, :updatedAt)")
                    .param("userId", cart.userId())
                    .param("updatedAt", timestamp(cart.updatedAt()))
                    .update(keys, "id");
        } catch (DataIntegrityViolationException e) {
            if (cart.userId() != null) {
                throw new CartAlreadyExistsException(cart.userId());
            }
            throw e;
        }
        return new CartDto(keys.getKey().longValue(), cart.userId(), 0L, cart.updatedAt());
    }

    /** Optimistic lock: the update only applies if nobody changed the cart since it was read. */
    private CartDto update(CartDto cart) {
        int updated = jdbc.sql("update carritos set usuario_id = :userId, actualizado_en = :updatedAt, "
                        + "version = version + 1 where id = :id and version = :version")
                .param("userId", cart.userId())
                .param("updatedAt", timestamp(cart.updatedAt()))
                .param("id", cart.id())
                .param("version", cart.version())
                .update();
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "Cart " + cart.id() + " was updated or deleted by another transaction");
        }
        return new CartDto(cart.id(), cart.userId(), cart.version() + 1, cart.updatedAt());
    }

    @Override
    public Optional<CartDto> findByUserId(Long userId) {
        return jdbc.sql("select id, usuario_id, version, actualizado_en from carritos where usuario_id = :userId")
                .param("userId", userId)
                .query(CART_MAPPER)
                .optional();
    }

    @Override
    public CartLineDto addLine(CartLineDto line) {
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.sql("insert into lineas_carrito (carrito_id, pelicula_id) values (:cartId, :movieId)")
                    .param("cartId", line.cartId())
                    .param("movieId", line.movieId())
                    .update(keys, "id");
        } catch (DuplicateKeyException e) {
            throw new MovieAlreadyInCartException(line.movieId());
        }
        return new CartLineDto(keys.getKey().longValue(), line.cartId(), line.movieId());
    }

    @Override
    public List<CartLineDto> findLines(Long cartId) {
        return jdbc.sql("select id, carrito_id, pelicula_id from lineas_carrito "
                        + "where carrito_id = :cartId order by id")
                .param("cartId", cartId)
                .query(LINE_MAPPER)
                .list();
    }

    @Override
    public boolean existsLine(Long cartId, Long movieId) {
        return jdbc.sql("select count(*) from lineas_carrito where carrito_id = :cartId and pelicula_id = :movieId")
                .param("cartId", cartId)
                .param("movieId", movieId)
                .query(Long.class)
                .single() > 0;
    }

    @Override
    public boolean removeLine(Long cartId, Long movieId) {
        return jdbc.sql("delete from lineas_carrito where carrito_id = :cartId and pelicula_id = :movieId")
                .param("cartId", cartId)
                .param("movieId", movieId)
                .update() == 1;
    }

    @Override
    public void removeLines(Long cartId) {
        jdbc.sql("delete from lineas_carrito where carrito_id = :cartId").param("cartId", cartId).update();
    }

    private static Timestamp timestamp(LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }
}
