package shop.rating.internal.persistence.repository;

import shop.rating.internal.persistence.entity.MovieMarkEntity;

import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.model.MovieMark;
import shop.rating.internal.domain.repository.MovieMarkRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(MovieMarkRepositoryImpl.class)
class MovieMarkRepositoryTest {

    @Autowired
    private MovieMarkRepository repository;

    @Autowired
    private EntityManager em;

    private long count() {
        return em.createQuery("select count(e) from MovieMarkEntity e", Long.class).getSingleResult();
    }

    private <T> T flushed(T saved) {
        em.flush();
        return saved;
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
        assertNotNull(flushed(repository.save(mark(1L, 10L, MarkType.FAVORITA))).getId());
    }

    @Test
    void findsMarkOfAUserByMovieAndType() {
        flushed(repository.save(mark(1L, 10L, MarkType.FAVORITA)));

        var found = repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.FAVORITA).orElseThrow();

        assertEquals(MarkType.FAVORITA, found.getType());
        assertTrue(repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.VISTA).isEmpty());
        assertTrue(repository.findByUserIdAndMovieIdAndType(2L, 10L, MarkType.FAVORITA).isEmpty());
    }

    @Test
    void movieCanBeFavoriteAndWatchedAtOnce() {
        flushed(repository.save(mark(1L, 10L, MarkType.FAVORITA)));
        flushed(repository.save(mark(1L, 10L, MarkType.VISTA)));

        assertEquals(2, count());
    }

    @Test
    void listsUserMarksByType() {
        flushed(repository.save(mark(1L, 10L, MarkType.FAVORITA)));
        flushed(repository.save(mark(1L, 11L, MarkType.FAVORITA)));
        flushed(repository.save(mark(1L, 12L, MarkType.VISTA)));
        flushed(repository.save(mark(2L, 13L, MarkType.FAVORITA)));

        assertEquals(2, repository.findByUserIdAndType(1L, MarkType.FAVORITA).size());
        assertEquals(1, repository.findByUserIdAndType(1L, MarkType.VISTA).size());
    }

    @Test
    void doesNotAllowMarkingSameMovieTwiceWithSameType() {
        flushed(repository.save(mark(1L, 10L, MarkType.VISTA)));

        assertThrows(DataIntegrityViolationException.class,
                () -> flushed(repository.save(mark(1L, 10L, MarkType.VISTA))));
    }

    @Test
    void userMovieAndTypeAreRequired() {
        assertThrows(DataIntegrityViolationException.class,
                () -> flushed(repository.save(mark(null, 10L, MarkType.VISTA))));
    }
}
