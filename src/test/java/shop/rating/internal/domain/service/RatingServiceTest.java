package shop.rating.internal.domain.service;

import shop.rating.internal.domain.exception.InvalidScoreException;
import shop.rating.internal.domain.exception.MovieNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.rating.internal.domain.fake.MovieCatalogFake;
import shop.rating.internal.domain.fake.RatingRepositoryFake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingServiceTest {

    private RatingRepositoryFake ratings;
    private MovieCatalogFake movies;
    private RatingService service;

    @BeforeEach
    void setUp() {
        ratings = new RatingRepositoryFake();
        movies = new MovieCatalogFake();
        movies.addMovie(10L);
        service = new RatingServiceImpl(ratings, movies);
    }

    @Test
    void ratesAndStoresTheAverageInTheCatalog() {
        service.rate(1L, 10L, 4, null);
        service.rate(2L, 10L, 2, null);

        assertEquals(3.0, movies.averageOf(10L));
    }

    @Test
    void ratingAgainUpdatesInsteadOfAddingOne() {
        service.rate(1L, 10L, 4, "ok");
        service.rate(1L, 10L, 5, "better");

        assertEquals(1, ratings.count());
        assertEquals(5.0, movies.averageOf(10L));
    }

    @Test
    void rejectsScoreOutOfRangeAndUnknownMovie() {
        assertThrows(InvalidScoreException.class, () -> service.rate(1L, 10L, 0, null));
        assertThrows(InvalidScoreException.class, () -> service.rate(1L, 10L, 6, null));
        assertThrows(MovieNotFoundException.class, () -> service.rate(1L, 99L, 3, null));
        assertEquals(0, ratings.count());
    }
}
