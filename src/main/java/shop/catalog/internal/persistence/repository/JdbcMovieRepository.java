package shop.catalog.internal.persistence.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.repository.MovieRepository;
import shop.shared.error.BusinessValidationException;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JDBC persistence of movies over the {@code peliculas} table of {@code init.sql}. */
public class JdbcMovieRepository implements MovieRepository {

    private static final String COLUMNS = "id, titulo, anio, genero, director, valoracion_media, precio, stock";

    /** Sortable properties of {@link Movie} and their columns: the sort comes from the client, so it is whitelisted. */
    private static final Map<String, String> SORTABLE = Map.of(
            "id", "id",
            "title", "titulo",
            "year", "anio",
            "genre", "genero",
            "director", "director",
            "averageRating", "valoracion_media",
            "price", "precio",
            "stock", "stock");

    private final JdbcClient jdbc;

    public JdbcMovieRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Movie save(Movie movie) {
        if (movie.getId() == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.sql("insert into peliculas (titulo, anio, genero, director, valoracion_media, precio, stock) "
                            + "values (?, ?, ?, ?, ?, ?, ?)")
                    .param(movie.getTitle())
                    .param(movie.getYear())
                    .param(movie.getGenre())
                    .param(movie.getDirector())
                    .param(movie.getAverageRating())
                    .param(movie.getPrice())
                    .param(movie.getStock())
                    .update(keys, "id");
            movie.setId(keys.getKey().longValue());
        } else {
            jdbc.sql("update peliculas set titulo = ?, anio = ?, genero = ?, director = ?, valoracion_media = ?, "
                            + "precio = ?, stock = ? where id = ?")
                    .param(movie.getTitle())
                    .param(movie.getYear())
                    .param(movie.getGenre())
                    .param(movie.getDirector())
                    .param(movie.getAverageRating())
                    .param(movie.getPrice())
                    .param(movie.getStock())
                    .param(movie.getId())
                    .update();
        }
        return movie;
    }

    @Override
    public Optional<Movie> findById(Long id) {
        return jdbc.sql("select " + COLUMNS + " from peliculas where id = ?")
                .param(id)
                .query(JdbcMovieRepository::map)
                .optional();
    }

    @Override
    public boolean existsById(Long id) {
        return jdbc.sql("select count(*) from peliculas where id = ?").param(id).query(Long.class).single() > 0;
    }

    @Override
    public void deleteById(Long id) {
        jdbc.sql("delete from peliculas where id = ?").param(id).update();
    }

    @Override
    public List<Movie> findAllById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + COLUMNS + " from peliculas where id in (:ids)")
                .param("ids", ids)
                .query(JdbcMovieRepository::map)
                .list();
    }

    @Override
    public PageResult<Movie> search(String title, String genre, Integer year, String director, PageQuery page) {
        var where = new StringBuilder(" where 1 = 1");
        var params = new ArrayList<Object>();
        if (title != null) {
            where.append(" and lower(titulo) like lower(concat('%', ?, '%'))");
            params.add(title);
        }
        if (genre != null) {
            where.append(" and genero = ?");
            params.add(genre);
        }
        if (year != null) {
            where.append(" and anio = ?");
            params.add(year);
        }
        if (director != null) {
            where.append(" and director = ?");
            params.add(director);
        }

        long total = bind(jdbc.sql("select count(*) from peliculas" + where), params).query(Long.class).single();
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(page.size());
        pageParams.add(page.offset());
        List<Movie> content = bind(jdbc.sql("select " + COLUMNS + " from peliculas" + where + orderBy(page)
                + " limit ? offset ?"), pageParams).query(JdbcMovieRepository::map).list();
        return new PageResult<>(content, total, page);
    }

    @Override
    public int decrementStock(Long id, int quantity) {
        return jdbc.sql("update peliculas set stock = stock - ? where id = ? and stock >= ?")
                .param(quantity)
                .param(id)
                .param(quantity)
                .update();
    }

    @Override
    public int incrementStock(Long id, int quantity) {
        return jdbc.sql("update peliculas set stock = stock + ? where id = ?")
                .param(quantity)
                .param(id)
                .update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, List<Object> params) {
        return spec.params(params);
    }

    /** Always ends with {@code id} so that pages are stable even when the chosen property has repeated values. */
    private static String orderBy(PageQuery page) {
        var order = new StringBuilder(" order by ");
        for (PageQuery.Sort sort : page.sort()) {
            String column = SORTABLE.get(sort.property());
            if (column == null) {
                throw new BusinessValidationException("Propiedad de ordenación no válida: " + sort.property());
            }
            order.append(column).append(sort.ascending() ? " asc, " : " desc, ");
        }
        return order.append("id").toString();
    }

    private static Movie map(ResultSet rs, int row) throws SQLException {
        var m = new Movie();
        m.setId(rs.getLong("id"));
        m.setTitle(rs.getString("titulo"));
        m.setYear(rs.getObject("anio", Integer.class));
        m.setGenre(rs.getString("genero"));
        m.setDirector(rs.getString("director"));
        m.setAverageRating(rs.getObject("valoracion_media", Double.class));
        m.setPrice(rs.getBigDecimal("precio"));
        m.setStock(rs.getInt("stock"));
        return m;
    }
}
