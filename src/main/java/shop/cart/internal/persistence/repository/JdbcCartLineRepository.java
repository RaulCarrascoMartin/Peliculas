package shop.cart.internal.persistence.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.cart.internal.domain.model.CartLine;
import shop.cart.internal.domain.repository.CartLineRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC persistence of cart lines over the {@code lineas_carrito} table of {@code init.sql}. */
public class JdbcCartLineRepository implements CartLineRepository {

    private static final String COLUMNS = "id, carrito_id, pelicula_id";

    private final JdbcClient jdbc;

    public JdbcCartLineRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CartLine save(CartLine line) {
        if (line.getId() == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.sql("insert into lineas_carrito (carrito_id, pelicula_id) values (?, ?)")
                    .param(line.getCartId())
                    .param(line.getMovieId())
                    .update(keys, "id");
            line.setId(keys.getKey().longValue());
        } else {
            jdbc.sql("update lineas_carrito set carrito_id = ?, pelicula_id = ? where id = ?")
                    .param(line.getCartId())
                    .param(line.getMovieId())
                    .param(line.getId())
                    .update();
        }
        return line;
    }

    @Override
    public void delete(CartLine line) {
        jdbc.sql("delete from lineas_carrito where id = ?").param(line.getId()).update();
    }

    @Override
    public List<CartLine> findByCartIdOrderById(Long cartId) {
        return jdbc.sql("select " + COLUMNS + " from lineas_carrito where carrito_id = ? order by id")
                .param(cartId)
                .query(JdbcCartLineRepository::map)
                .list();
    }

    @Override
    public Optional<CartLine> findByCartIdAndMovieId(Long cartId, Long movieId) {
        return jdbc.sql("select " + COLUMNS + " from lineas_carrito where carrito_id = ? and pelicula_id = ?")
                .param(cartId)
                .param(movieId)
                .query(JdbcCartLineRepository::map)
                .optional();
    }

    @Override
    public boolean existsByCartIdAndMovieId(Long cartId, Long movieId) {
        return jdbc.sql("select count(*) from lineas_carrito where carrito_id = ? and pelicula_id = ?")
                .param(cartId)
                .param(movieId)
                .query(Long.class)
                .single() > 0;
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        jdbc.sql("delete from lineas_carrito where pelicula_id = ?").param(movieId).update();
    }

    @Override
    public void deleteByCartId(Long cartId) {
        jdbc.sql("delete from lineas_carrito where carrito_id = ?").param(cartId).update();
    }

    private static CartLine map(ResultSet rs, int row) throws SQLException {
        var l = new CartLine();
        l.setId(rs.getLong("id"));
        l.setCartId(rs.getLong("carrito_id"));
        l.setMovieId(rs.getLong("pelicula_id"));
        return l;
    }
}
