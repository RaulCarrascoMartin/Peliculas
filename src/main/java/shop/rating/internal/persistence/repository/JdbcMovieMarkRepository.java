package shop.rating.internal.persistence.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.model.MovieMark;
import shop.rating.internal.domain.repository.MovieMarkRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC persistence of movie marks over the {@code marcas_pelicula} table of {@code init.sql}. */
public class JdbcMovieMarkRepository implements MovieMarkRepository {

    private static final String COLUMNS = "id, usuario_id, pelicula_id, tipo";

    private final JdbcClient jdbc;

    public JdbcMovieMarkRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MovieMark save(MovieMark mark) {
        if (mark.getId() == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.sql("insert into marcas_pelicula (usuario_id, pelicula_id, tipo) values (?, ?, ?)")
                    .param(mark.getUserId())
                    .param(mark.getMovieId())
                    .param(mark.getType().name())
                    .update(keys, "id");
            mark.setId(keys.getKey().longValue());
        } else {
            jdbc.sql("update marcas_pelicula set usuario_id = ?, pelicula_id = ?, tipo = ? where id = ?")
                    .param(mark.getUserId())
                    .param(mark.getMovieId())
                    .param(mark.getType().name())
                    .param(mark.getId())
                    .update();
        }
        return mark;
    }

    @Override
    public void delete(MovieMark mark) {
        jdbc.sql("delete from marcas_pelicula where id = ?").param(mark.getId()).update();
    }

    @Override
    public Optional<MovieMark> findByUserIdAndMovieIdAndType(Long userId, Long movieId, MarkType type) {
        return jdbc.sql("select " + COLUMNS + " from marcas_pelicula where usuario_id = ? and pelicula_id = ? and tipo = ?")
                .param(userId)
                .param(movieId)
                .param(type.name())
                .query(JdbcMovieMarkRepository::map)
                .optional();
    }

    @Override
    public List<MovieMark> findByUserIdAndType(Long userId, MarkType type) {
        return jdbc.sql("select " + COLUMNS + " from marcas_pelicula where usuario_id = ? and tipo = ? order by id")
                .param(userId)
                .param(type.name())
                .query(JdbcMovieMarkRepository::map)
                .list();
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        jdbc.sql("delete from marcas_pelicula where pelicula_id = ?").param(movieId).update();
    }

    private static MovieMark map(ResultSet rs, int row) throws SQLException {
        var m = new MovieMark();
        m.setId(rs.getLong("id"));
        m.setUserId(rs.getLong("usuario_id"));
        m.setMovieId(rs.getLong("pelicula_id"));
        m.setType(MarkType.valueOf(rs.getString("tipo")));
        return m;
    }
}
