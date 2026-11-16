package shop.rating.internal.persistence.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.rating.internal.domain.model.Rating;
import shop.rating.internal.domain.repository.RatingRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC persistence of ratings over the {@code valoraciones} table of {@code init.sql}. */
public class JdbcRatingRepository implements RatingRepository {

    private static final String COLUMNS = "id, usuario_id, pelicula_id, puntuacion, comentario";

    private final JdbcClient jdbc;

    public JdbcRatingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Rating save(Rating rating) {
        if (rating.getId() == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.sql("insert into valoraciones (usuario_id, pelicula_id, puntuacion, comentario) values (?, ?, ?, ?)")
                    .param(rating.getUserId())
                    .param(rating.getMovieId())
                    .param(rating.getScore())
                    .param(rating.getComment())
                    .update(keys, "id");
            rating.setId(keys.getKey().longValue());
        } else {
            jdbc.sql("update valoraciones set usuario_id = ?, pelicula_id = ?, puntuacion = ?, comentario = ? where id = ?")
                    .param(rating.getUserId())
                    .param(rating.getMovieId())
                    .param(rating.getScore())
                    .param(rating.getComment())
                    .param(rating.getId())
                    .update();
        }
        return rating;
    }

    @Override
    public Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId) {
        return jdbc.sql("select " + COLUMNS + " from valoraciones where usuario_id = ? and pelicula_id = ?")
                .param(userId)
                .param(movieId)
                .query(JdbcRatingRepository::map)
                .optional();
    }

    @Override
    public List<Rating> findByMovieId(Long movieId) {
        return jdbc.sql("select " + COLUMNS + " from valoraciones where pelicula_id = ? order by id")
                .param(movieId)
                .query(JdbcRatingRepository::map)
                .list();
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        jdbc.sql("delete from valoraciones where pelicula_id = ?").param(movieId).update();
    }

    private static Rating map(ResultSet rs, int row) throws SQLException {
        var r = new Rating();
        r.setId(rs.getLong("id"));
        r.setUserId(rs.getLong("usuario_id"));
        r.setMovieId(rs.getLong("pelicula_id"));
        r.setScore(rs.getInt("puntuacion"));
        r.setComment(rs.getString("comentario"));
        return r;
    }
}
