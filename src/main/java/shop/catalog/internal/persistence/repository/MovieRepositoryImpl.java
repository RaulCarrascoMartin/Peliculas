package shop.catalog.internal.persistence.repository;

import jakarta.inject.Named;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.repository.MovieRepository;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JDBC persistence of movies (and their stock) over the {@code peliculas} table (Flyway migrations). */
@Named
public class MovieRepositoryImpl implements MovieRepository {

    private static final String COLUMNS = "id, titulo, anio, genero, director, valoracion_media, precio, stock";

    /**
     * Column of each sortable property of {@link MovieDto}. Which properties a client may sort by is decided by the
     * service; this table only translates them and, being a closed list, nothing received is ever written into the
     * SQL.
     */
    private static final Map<String, String> COLUMN_OF = Map.of(
            "id", "id",
            "title", "titulo",
            "year", "anio",
            "genre", "genero",
            "director", "director",
            "averageRating", "valoracion_media",
            "price", "precio",
            "stock", "stock");

    private static final RowMapper<MovieDto> MAPPER = (rs, n) -> new MovieDto(
            rs.getLong("id"),
            rs.getString("titulo"),
            rs.getObject("anio", Integer.class),
            rs.getString("genero"),
            rs.getString("director"),
            rs.getBigDecimal("precio"),
            rs.getObject("valoracion_media", Double.class),
            rs.getInt("stock"));

    private final JdbcClient jdbc;

    public MovieRepositoryImpl(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MovieDto save(MovieDto movie) {
        if (movie.id() != null) {
            int updated = columns(jdbc.sql("update peliculas set titulo = :title, anio = :year, genero = :genre, "
                    + "director = :director, precio = :price, stock = :stock "
                    + "where id = :id"), movie)
                    .param("id", movie.id())
                    .update();
            if (updated == 0) {
                throw new MovieNotFoundException(movie.id());
            }
            return movie;
        }
        KeyHolder keys = new GeneratedKeyHolder();
        columns(jdbc.sql("insert into peliculas (titulo, anio, genero, director, valoracion_media, precio, stock) "
                + "values (:title, :year, :genre, :director, :averageRating, :price, :stock)"), movie)
                .update(keys, "id");
        return new MovieDto(keys.getKey().longValue(), movie.title(), movie.year(), movie.genre(), movie.director(),
                movie.price(), movie.averageRating(), movie.stock());
    }

    @Override
    public Optional<MovieDto> findById(Long id) {
        return jdbc.sql("select " + COLUMNS + " from peliculas where id = :id")
                .param("id", id)
                .query(MAPPER)
                .optional();
    }

    @Override
    public boolean existsById(Long id) {
        return jdbc.sql("select count(*) from peliculas where id = :id")
                .param("id", id)
                .query(Long.class)
                .single() > 0;
    }

    @Override
    public void deleteById(Long id) {
        jdbc.sql("delete from peliculas where id = :id").param("id", id).update();
    }

    @Override
    public List<MovieDto> findAllById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + COLUMNS + " from peliculas where id in (:ids)")
                .param("ids", ids)
                .query(MAPPER)
                .list();
    }

    @Override
    public PageResult<MovieDto> search(String title, String genre, Integer year, String director, PageQuery page) {
        var where = new StringBuilder(" where 1 = 1");
        Map<String, Object> params = new LinkedHashMap<>();
        if (title != null) {
            where.append(" and lower(titulo) like lower(concat('%', :title, '%'))");
            params.put("title", title);
        }
        if (genre != null) {
            where.append(" and genero = :genre");
            params.put("genre", genre);
        }
        if (year != null) {
            where.append(" and anio = :year");
            params.put("year", year);
        }
        if (director != null) {
            where.append(" and director = :director");
            params.put("director", director);
        }

        long total = jdbc.sql("select count(*) from peliculas" + where).params(params).query(Long.class).single();
        List<MovieDto> content = jdbc.sql("select " + COLUMNS + " from peliculas" + where + orderBy(page)
                        + " limit :limit offset :offset")
                .params(params)
                .param("limit", page.size())
                .param("offset", page.offset())
                .query(MAPPER)
                .list();
        return new PageResult<>(content, total, page);
    }

    @Override
    public int updateAverageRating(Long id, Double average) {
        return jdbc.sql("update peliculas set valoracion_media = :average where id = :id")
                .param("average", average)
                .param("id", id)
                .update();
    }

    @Override
    public int decrementStock(Long id, int quantity) {
        return jdbc.sql("update peliculas set stock = stock - :quantity where id = :id and stock >= :quantity")
                .param("quantity", quantity)
                .param("id", id)
                .update();
    }

    @Override
    public int incrementStock(Long id, int quantity) {
        return jdbc.sql("update peliculas set stock = stock + :quantity where id = :id")
                .param("quantity", quantity)
                .param("id", id)
                .update();
    }

    private static JdbcClient.StatementSpec columns(JdbcClient.StatementSpec statement, MovieDto movie) {
        return statement
                .param("title", movie.title())
                .param("year", movie.year())
                .param("genre", movie.genre())
                .param("director", movie.director())
                .param("averageRating", movie.averageRating())
                .param("price", movie.price())
                .param("stock", movie.stock());
    }

    /** Always ends with {@code id} so that pages are stable even when the chosen property has repeated values. */
    private static String orderBy(PageQuery page) {
        var order = new StringBuilder(" order by ");
        for (PageQuery.Sort sort : page.sort()) {
            String column = COLUMN_OF.get(sort.property());
            if (column == null) {
                // Not a business answer: the service validates the sort, so reaching this is a programming error.
                throw new IllegalArgumentException("No column for sort property " + sort.property());
            }
            order.append(column).append(sort.ascending() ? " asc, " : " desc, ");
        }
        return order.append("id").toString();
    }
}
