package shop.rating.internal.persistence.repository;

import jakarta.inject.Named;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.exception.MovieAlreadyMarkedException;
import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.repository.MovieMarkRepository;

import java.util.List;
import java.util.Optional;

/** JDBC persistence of movie marks over the {@code marcas_pelicula} table (Flyway migrations). */
@Named
public class MovieMarkRepositoryImpl implements MovieMarkRepository {

    private static final RowMapper<MovieMarkDto> MAPPER = (rs, n) -> new MovieMarkDto(
            rs.getLong("id"),
            rs.getLong("usuario_id"),
            rs.getLong("pelicula_id"),
            MarkType.valueOf(rs.getString("tipo")));

    private final JdbcClient jdbc;

    public MovieMarkRepositoryImpl(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MovieMarkDto save(MovieMarkDto mark) {
        try {
            return mark.id() == null ? insert(mark) : update(mark);
        } catch (DuplicateKeyException e) {
            throw new MovieAlreadyMarkedException(mark.movieId(), mark.type());
        }
    }

    private MovieMarkDto insert(MovieMarkDto mark) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into marcas_pelicula (usuario_id, pelicula_id, tipo) values (:userId, :movieId, :type)")
                .param("userId", mark.userId())
                .param("movieId", mark.movieId())
                .param("type", mark.type().name())
                .update(keys, "id");
        return new MovieMarkDto(keys.getKey().longValue(), mark.userId(), mark.movieId(), mark.type());
    }

    private MovieMarkDto update(MovieMarkDto mark) {
        int updated = jdbc.sql("update marcas_pelicula set usuario_id = :userId, pelicula_id = :movieId, tipo = :type "
                        + "where id = :id")
                .param("userId", mark.userId())
                .param("movieId", mark.movieId())
                .param("type", mark.type().name())
                .param("id", mark.id())
                .update();
        if (updated == 0) {
            throw new MovieNotFoundException(mark.movieId());
        }
        return mark;
    }

    @Override
    public void delete(Long id) {
        jdbc.sql("delete from marcas_pelicula where id = :id").param("id", id).update();
    }

    @Override
    public Optional<MovieMarkDto> findByUserIdAndMovieIdAndType(Long userId, Long movieId, MarkType type) {
        return jdbc.sql("select id, usuario_id, pelicula_id, tipo from marcas_pelicula "
                        + "where usuario_id = :userId and pelicula_id = :movieId and tipo = :type")
                .param("userId", userId)
                .param("movieId", movieId)
                .param("type", type.name())
                .query(MAPPER)
                .optional();
    }

    @Override
    public List<MovieMarkDto> findByUserIdAndType(Long userId, MarkType type) {
        return jdbc.sql("select id, usuario_id, pelicula_id, tipo from marcas_pelicula "
                        + "where usuario_id = :userId and tipo = :type order by id")
                .param("userId", userId)
                .param("type", type.name())
                .query(MAPPER)
                .list();
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        jdbc.sql("delete from marcas_pelicula where pelicula_id = :movieId").param("movieId", movieId).update();
    }
}
