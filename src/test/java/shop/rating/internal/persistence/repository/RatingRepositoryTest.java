package shop.rating.internal.persistence.repository;

import shop.rating.internal.domain.model.Rating;
import shop.rating.internal.domain.repository.RatingRepository;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcRatingRepository.class)
class RatingRepositoryTest {

    @Autowired
    private RatingRepository repository;



    private Rating rating(Long userId, Long movieId, int score, String comment) {
        var rating = new Rating();
        rating.setUserId(userId);
        rating.setMovieId(movieId);
        rating.setScore(score);
        rating.setComment(comment);
        return rating;
    }

    @Test
    void savesRatingAndAssignsId() {
        assertNotNull(repository.save(rating(1L, 10L, 4, "Very good")).getId());
    }

    @Test
    void findsRatingOfAUserForAMovie() {
        repository.save(rating(1L, 10L, 4, "Very good"));
        repository.save(rating(2L, 10L, 2, null));

        var found = repository.findByUserIdAndMovieId(1L, 10L).orElseThrow();

        assertEquals(4, found.getScore());
        assertEquals("Very good", found.getComment());
        assertTrue(repository.findByUserIdAndMovieId(1L, 11L).isEmpty());
    }

    @Test
    void listsRatingsOfAMovie() {
        repository.save(rating(1L, 10L, 4, null));
        repository.save(rating(2L, 10L, 2, null));
        repository.save(rating(1L, 11L, 5, null));

        assertEquals(2, repository.findByMovieId(10L).size());
    }

    @Test
    void commentIsOptional() {
        assertNull(repository.save(rating(1L, 10L, 3, null)).getComment());
    }

    @Test
    void doesNotAllowTwoRatingsBySameUserForSameMovie() {
        repository.save(rating(1L, 10L, 4, null));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(rating(1L, 10L, 5, null)));
    }

    @Test
    void userMovieAndScoreAreRequired() {
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(rating(null, 10L, 4, null)));
    }
}
