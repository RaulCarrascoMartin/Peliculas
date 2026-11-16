package shop.rating.internal.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.exception.MovieAlreadyMarkedException;
import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.repository.MovieMarkRepository;
import shop.shared.MariaDbRepositoryTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SQL and row ↔ DTO mapping of movie marks against a real MariaDB. */
@Import(MovieMarkRepositoryImpl.class)
class MovieMarkRepositoryImplTest extends MariaDbRepositoryTest {

    @Autowired
    private MovieMarkRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private static MovieMarkDto mark(Long userId, Long movieId, MarkType type) {
        return new MovieMarkDto(null, userId, movieId, type);
    }

    private long count() {
        return jdbc.sql("select count(*) from marcas_pelicula").query(Long.class).single();
    }

    @Test
    void saveWithIdOfAMissingMarkThrowsNotFound() {
        assertThrows(MovieNotFoundException.class,
                () -> repository.save(new MovieMarkDto(999_999L, 1L, 10L, MarkType.VISTA)));
    }

    @Test
    void saveAssignsIdAndTheMarkIsFoundWithEveryColumn() {
        MovieMarkDto saved = repository.save(mark(1L, 10L, MarkType.FAVORITA));

        assertNotNull(saved.id());
        assertEquals(new MovieMarkDto(saved.id(), 1L, 10L, MarkType.FAVORITA), saved);
        assertEquals(saved, repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.FAVORITA).orElseThrow());
        assertEquals("FAVORITA", jdbc.sql("select tipo from marcas_pelicula where id = :id")
                .param("id", saved.id()).query(String.class).single());
    }

    @Test
    void findsOnlyTheMarkOfThatUserMovieAndType() {
        repository.save(mark(1L, 10L, MarkType.FAVORITA));

        assertTrue(repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.VISTA).isEmpty());
        assertTrue(repository.findByUserIdAndMovieIdAndType(2L, 10L, MarkType.FAVORITA).isEmpty());
        assertTrue(repository.findByUserIdAndMovieIdAndType(1L, 11L, MarkType.FAVORITA).isEmpty());
    }

    @Test
    void movieCanBeFavoriteAndWatchedAtOnce() {
        repository.save(mark(1L, 10L, MarkType.FAVORITA));
        repository.save(mark(1L, 10L, MarkType.VISTA));

        assertEquals(2, count());
    }

    @Test
    void listsTheMarksOfAUserByTypeInInsertionOrder() {
        MovieMarkDto first = repository.save(mark(1L, 11L, MarkType.FAVORITA));
        MovieMarkDto second = repository.save(mark(1L, 10L, MarkType.FAVORITA));
        MovieMarkDto watched = repository.save(mark(1L, 12L, MarkType.VISTA));
        repository.save(mark(2L, 13L, MarkType.FAVORITA));

        assertEquals(List.of(first, second), repository.findByUserIdAndType(1L, MarkType.FAVORITA));
        assertEquals(List.of(watched), repository.findByUserIdAndType(1L, MarkType.VISTA));
        assertTrue(repository.findByUserIdAndType(3L, MarkType.FAVORITA).isEmpty());
    }

    @Test
    void saveWithIdUpdatesTheRow() {
        MovieMarkDto saved = repository.save(mark(1L, 10L, MarkType.FAVORITA));

        MovieMarkDto updated = repository.save(new MovieMarkDto(saved.id(), 1L, 10L, MarkType.VISTA));

        assertEquals(new MovieMarkDto(saved.id(), 1L, 10L, MarkType.VISTA), updated);
        assertEquals(updated, repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.VISTA).orElseThrow());
        assertEquals(1, count());
    }

    @Test
    void markingTheSameMovieTwiceWithTheSameTypeIsTheDomainConflictAndLeavesOneRow() {
        repository.save(mark(1L, 10L, MarkType.VISTA));

        var ex = assertThrows(MovieAlreadyMarkedException.class,
                () -> repository.save(mark(1L, 10L, MarkType.VISTA)));

        assertTrue(ex.getMessage().contains("10"));
        assertEquals(1, count());
    }

    @Test
    void userAndMovieAreRequiredAndNothingIsStored() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(mark(null, 10L, MarkType.VISTA)));
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(mark(1L, null, MarkType.VISTA)));

        assertEquals(0, count());
    }

    @Test
    void deleteRemovesOnlyThatMarkAndUnknownIdAffectsNothing() {
        MovieMarkDto favorite = repository.save(mark(1L, 10L, MarkType.FAVORITA));
        MovieMarkDto watched = repository.save(mark(1L, 10L, MarkType.VISTA));

        repository.delete(favorite.id());
        repository.delete(987654L);

        assertTrue(repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.FAVORITA).isEmpty());
        assertEquals(watched, repository.findByUserIdAndMovieIdAndType(1L, 10L, MarkType.VISTA).orElseThrow());
        assertEquals(1, count());
    }

    @Test
    void deleteByMovieIdRemovesTheMarksOfThatMovieForEveryUserAndType() {
        repository.save(mark(1L, 10L, MarkType.FAVORITA));
        repository.save(mark(1L, 10L, MarkType.VISTA));
        repository.save(mark(2L, 10L, MarkType.FAVORITA));
        MovieMarkDto other = repository.save(mark(1L, 11L, MarkType.FAVORITA));

        repository.deleteByMovieId(10L);

        assertEquals(List.of(other), repository.findByUserIdAndType(1L, MarkType.FAVORITA));
        assertEquals(1, count());
    }
}
