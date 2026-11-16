package shop.rating.internal.persistence.repository;

import jakarta.inject.Named;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.dto.RatingDto;
import shop.rating.internal.domain.exception.RatingAlreadyExistsException;
import shop.rating.internal.domain.repository.RatingRepository;

import java.util.List;
import java.util.Optional;

/** JDBC persistence of ratings over the {@code valoraciones} table (Flyway migrations). */
@Named
public class RatingRepositoryImpl implements RatingRepository {

    private static final RowMapper<RatingDto> MAPPER = (rs, n) -> new RatingDto(
            rs.getLong("id"),
            rs.getLong("usuario_id"),
            rs.getLong("pelicula_id"),
            rs.getInt("puntuacion"),
            rs.getString("comentario"));

    private final JdbcClient jdbc;

    public RatingRepositoryImpl(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RatingDto save(RatingDto rating) {
        try {
            return rating.id() == null ? insert(rating) : update(rating);
        } catch (DuplicateKeyException e) {
            throw new RatingAlreadyExistsException(rating.movieId());
        }
    }

    private RatingDto insert(RatingDto rating) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into valoraciones (usuario_id, pelicula_id, puntuacion, comentario) "
                        + "values (:userId, :movieId, :score, :comment)")
                .param("userId", rating.userId())
                .param("movieId", rating.movieId())
                .param("score", rating.score())
                .param("comment", rating.comment())
                .update(keys, "id");
        return new RatingDto(keys.getKey().longValue(), rating.userId(), rating.movieId(), rating.score(),
                rating.comment());
    }

    private RatingDto update(RatingDto rating) {
        int updated = jdbc.sql("update valoraciones set usuario_id = :userId, pelicula_id = :movieId, puntuacion = :score, "
                        + "comentario = :comment where id = :id")
                .param("userId", rating.userId())
                .param("movieId", rating.movieId())
                .param("score", rating.score())
                .param("comment", rating.comment())
                .param("id", rating.id())
                .update();
        if (updated == 0) {
            throw new MovieNotFoundException(rating.movieId());
        }
        return rating;
    }

    @Override
    public Optional<RatingDto> findByUserIdAndMovieId(Long userId, Long movieId) {
        return jdbc.sql("select id, usuario_id, pelicula_id, puntuacion, comentario from valoraciones "
                        + "where usuario_id = :userId and pelicula_id = :movieId")
                .param("userId", userId)
                .param("movieId", movieId)
                .query(MAPPER)
                .optional();
    }

    @Override
    public List<RatingDto> findByMovieId(Long movieId) {
        return jdbc.sql("select id, usuario_id, pelicula_id, puntuacion, comentario from valoraciones "
                        + "where pelicula_id = :movieId order by id")
                .param("movieId", movieId)
                .query(MAPPER)
                .list();
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        jdbc.sql("delete from valoraciones where pelicula_id = :movieId").param("movieId", movieId).update();
    }
}
