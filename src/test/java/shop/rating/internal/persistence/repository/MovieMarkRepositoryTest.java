package shop.rating.internal.persistence.repository;


import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.model.MovieMark;
import shop.rating.internal.domain.repository.MovieMarkRepository;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcMovieMarkRepository.class)
class MovieMarkRepositoryTest {

    @Autowired
    private MovieMarkRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private long count() {
        return jdbc.sql("select count(*) from marcas_pelicula").query(Long.class).single();
    }


    private MovieMark mark(Long userId, Long movieId, MarkType type) {
        var mark = new MovieMark();
        mark.setUserId(userId);
        mark.setMovieId(movieId);
        mark.setType(type);
        return mark;
    }

    @Test
    void savesMarkAndAssignsId() {
        assertNotNull(repository.save(mark(1L, 10L, MarkType.FAVORITA)).getId());
    }

    @Test
    void findsMarkOfAUserByMovieAndType() {
        repository.save(mark(1L, 10L, MarkType.FAVORITA));

        var found = repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.FAVORITA).orElseThrow();

        assertEquals(MarkType.FAVORITA, found.getType());
        assertTrue(repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.VISTA).isEmpty());
        assertTrue(repository.findByUserIdAndMovieIdAndType(2L, 10L, MarkType.FAVORITA).isEmpty());
    }

    @Test
    void movieCanBeFavoriteAndWatchedAtOnce() {
        repository.save(mark(1L, 10L, MarkType.FAVORITA));
        repository.save(mark(1L, 10L, MarkType.VISTA));

        assertEquals(2, count());
    }

    @Test
    void listsUserMarksByType() {
        repository.save(mark(1L, 10L, MarkType.FAVORITA));
        repository.save(mark(1L, 11L, MarkType.FAVORITA));
        repository.save(mark(1L, 12L, MarkType.VISTA));
        repository.save(mark(2L, 13L, MarkType.FAVORITA));

        assertEquals(2, repository.findByUserIdAndType(1L, MarkType.FAVORITA).size());
        assertEquals(1, repository.findByUserIdAndType(1L, MarkType.VISTA).size());
    }

    @Test
    void doesNotAllowMarkingSameMovieTwiceWithSameType() {
        repository.save(mark(1L, 10L, MarkType.VISTA));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(mark(1L, 10L, MarkType.VISTA)));
    }

    @Test
    void userMovieAndTypeAreRequired() {
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(mark(null, 10L, MarkType.VISTA)));
    }
}
