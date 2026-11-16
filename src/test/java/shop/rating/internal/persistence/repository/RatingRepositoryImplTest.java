package shop.rating.internal.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.rating.internal.domain.dto.RatingDto;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.exception.RatingAlreadyExistsException;
import shop.rating.internal.domain.repository.RatingRepository;
import shop.shared.MariaDbRepositoryTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SQL and row ↔ DTO mapping of ratings against a real MariaDB. */
@Import(RatingRepositoryImpl.class)
class RatingRepositoryImplTest extends MariaDbRepositoryTest {

    @Autowired
    private RatingRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private static RatingDto rating(Long userId, Long movieId, Integer score, String comment) {
        return new RatingDto(null, userId, movieId, score, comment);
    }

    private long count() {
        return jdbc.sql("select count(*) from valoraciones").query(Long.class).single();
    }

    @Test
    void saveWithIdOfAMissingRatingThrowsNotFound() {
        assertThrows(MovieNotFoundException.class, () -> repository.save(new RatingDto(999_999L, 1L, 10L, 5, null)));
    }

    @Test
    void saveAssignsIdAndTheRatingIsFoundWithEveryColumn() {
        RatingDto saved = repository.save(rating(1L, 10L, 4, "Very good"));

        assertNotNull(saved.id());
        assertEquals(new RatingDto(saved.id(), 1L, 10L, 4, "Very good"), saved);
        assertEquals(saved, repository.findByUserIdAndMovieId(1L, 10L).orElseThrow());
    }

    @Test
    void commentIsOptional() {
        repository.save(rating(1L, 10L, 3, null));

        assertNull(repository.findByUserIdAndMovieId(1L, 10L).orElseThrow().comment());
    }

    @Test
    void saveWithIdUpdatesScoreAndCommentWithoutCreatingAnotherRow() {
        RatingDto saved = repository.save(rating(1L, 10L, 2, "Weak"));

        RatingDto updated = repository.save(new RatingDto(saved.id(), 1L, 10L, 5, null));

        assertEquals(new RatingDto(saved.id(), 1L, 10L, 5, null), updated);
        assertEquals(updated, repository.findByUserIdAndMovieId(1L, 10L).orElseThrow());
        assertEquals(1, count());
    }

    @Test
    void findsOnlyTheRatingOfThatUserForThatMovie() {
        repository.save(rating(1L, 10L, 4, "Very good"));
        repository.save(rating(2L, 10L, 2, null));

        assertEquals(4, repository.findByUserIdAndMovieId(1L, 10L).orElseThrow().score());
        assertEquals(2, repository.findByUserIdAndMovieId(2L, 10L).orElseThrow().score());
        assertTrue(repository.findByUserIdAndMovieId(1L, 11L).isEmpty());
        assertTrue(repository.findByUserIdAndMovieId(3L, 10L).isEmpty());
    }

    @Test
    void listsTheRatingsOfAMovieInInsertionOrder() {
        RatingDto first = repository.save(rating(1L, 10L, 4, null));
        RatingDto second = repository.save(rating(2L, 10L, 2, "Meh"));
        repository.save(rating(1L, 11L, 5, null));

        assertEquals(List.of(first, second), repository.findByMovieId(10L));
        assertTrue(repository.findByMovieId(987654L).isEmpty());
    }

    @Test
    void secondRatingOfTheSameUserForTheSameMovieIsTheDomainConflictAndLeavesTheFirstIntact() {
        RatingDto first = repository.save(rating(1L, 10L, 4, null));

        var ex = assertThrows(RatingAlreadyExistsException.class, () -> repository.save(rating(1L, 10L, 5, null)));

        assertTrue(ex.getMessage().contains("10"));
        assertEquals(1, count());
        assertEquals(first, repository.findByUserIdAndMovieId(1L, 10L).orElseThrow());
    }

    @Test
    void userMovieAndScoreAreRequiredAndNothingIsStored() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(rating(null, 10L, 4, null)));
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(rating(1L, null, 4, null)));
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(rating(1L, 10L, null, null)));

        assertEquals(0, count());
    }

    @Test
    void deleteByMovieIdRemovesOnlyTheRatingsOfThatMovie() {
        repository.save(rating(1L, 10L, 4, null));
        repository.save(rating(2L, 10L, 2, null));
        RatingDto other = repository.save(rating(1L, 11L, 5, null));

        repository.deleteByMovieId(10L);
        repository.deleteByMovieId(987654L);

        assertTrue(repository.findByMovieId(10L).isEmpty());
        assertEquals(List.of(other), repository.findByMovieId(11L));
    }
}
