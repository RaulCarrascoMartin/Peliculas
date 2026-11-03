package shop.rating.internal.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class MovieMarkRepositoryTest {

    @Autowired
    private MovieMarkRepository repository;

    private MovieMark mark(Long userId, Long movieId, MarkType type) {
        var mark = new MovieMark();
        mark.setUserId(userId);
        mark.setMovieId(movieId);
        mark.setType(type);
        return mark;
    }

    @Test
    void savesMarkAndAssignsId() {
        assertNotNull(repository.saveAndFlush(mark(1L, 10L, MarkType.FAVORITA)).getId());
    }

    @Test
    void findsMarkOfAUserByMovieAndType() {
        repository.saveAndFlush(mark(1L, 10L, MarkType.FAVORITA));

        var found = repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.FAVORITA).orElseThrow();

        assertEquals(MarkType.FAVORITA, found.getType());
        assertTrue(repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.VISTA).isEmpty());
        assertTrue(repository.findByUserIdAndMovieIdAndType(2L, 10L, MarkType.FAVORITA).isEmpty());
    }

    @Test
    void movieCanBeFavoriteAndWatchedAtOnce() {
        repository.saveAndFlush(mark(1L, 10L, MarkType.FAVORITA));
        repository.saveAndFlush(mark(1L, 10L, MarkType.VISTA));

        assertEquals(2, repository.count());
    }

    @Test
    void listsUserMarksByType() {
        repository.saveAndFlush(mark(1L, 10L, MarkType.FAVORITA));
        repository.saveAndFlush(mark(1L, 11L, MarkType.FAVORITA));
        repository.saveAndFlush(mark(1L, 12L, MarkType.VISTA));
        repository.saveAndFlush(mark(2L, 13L, MarkType.FAVORITA));

        assertEquals(2, repository.findByUserIdAndType(1L, MarkType.FAVORITA).size());
        assertEquals(1, repository.findByUserIdAndType(1L, MarkType.VISTA).size());
    }

    @Test
    void doesNotAllowMarkingSameMovieTwiceWithSameType() {
        repository.saveAndFlush(mark(1L, 10L, MarkType.VISTA));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(mark(1L, 10L, MarkType.VISTA)));
    }

    @Test
    void userMovieAndTypeAreRequired() {
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(mark(null, 10L, MarkType.VISTA)));
    }
}
