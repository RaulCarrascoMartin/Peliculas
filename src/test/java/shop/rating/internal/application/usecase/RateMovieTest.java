package shop.rating.internal.application.usecase;

import shop.rating.internal.application.dto.RatingView;
import shop.rating.internal.domain.fake.MovieCatalogFake;
import shop.rating.internal.domain.fake.RatingRepositoryFake;

import shop.shared.error.ResourceNotFoundException;
import shop.shared.error.BusinessValidationException;
import shop.rating.internal.domain.service.RatingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateMovieTest {

    private static final Long MOVIE = 10L;

    private RatingRepositoryFake repository;
    private MovieCatalogFake movies;

    @BeforeEach
    void setUp() {
        repository = new RatingRepositoryFake();
        movies = new MovieCatalogFake();
        movies.addMovie(MOVIE);
    }

    private ForUser serviceFor(Long userId) {
        return new ForUser(new RateMovie(new RatingServiceImpl(repository, movies)), userId);
    }

    /** The rate use case on behalf of one user. */
    private record ForUser(RateMovie rateMovie, Long userId) {
        RatingView execute(Long movieId, int score, String comment) {
            return rateMovie.execute(userId, movieId, score, comment);
        }
    }

    @Test
    void savesRatingOfCurrentUser() {
        var rating = serviceFor(1L).execute(MOVIE, 4, "Very good");

        assertNotNull(rating.id());
        assertEquals(1L, repository.findAll().get(0).getUserId());
        assertEquals(MOVIE, rating.movieId());
        assertEquals(4, rating.score());
        assertEquals("Very good", rating.comment());
        assertEquals(1, repository.count());
    }

    @Test
    void acceptsRangeBoundaries() {
        serviceFor(1L).execute(MOVIE, 1, null);
        serviceFor(2L).execute(MOVIE, 5, null);

        assertEquals(2, repository.count());
    }

    @Test
    void rejectsOutOfRangeScoresWithoutSaving() {
        var service = serviceFor(1L);

        assertThrows(BusinessValidationException.class, () -> service.execute(MOVIE, 0, null));
        assertThrows(BusinessValidationException.class, () -> service.execute(MOVIE, 6, null));
        assertThrows(BusinessValidationException.class, () -> service.execute(MOVIE, -3, null));
        assertEquals(0, repository.count());
    }

    @Test
    void ratingAgainUpdatesPreviousRatingWithoutCreatingAnother() {
        var first = serviceFor(1L).execute(MOVIE, 2, "Weak");

        var second = serviceFor(1L).execute(MOVIE, 5, "Better on second viewing");

        assertEquals(first.id(), second.id());
        assertEquals(1, repository.count());
        assertEquals(5, repository.findAll().get(0).getScore());
        assertEquals("Better on second viewing", repository.findAll().get(0).getComment());
    }

    @Test
    void eachUserHasTheirOwnRating() {
        serviceFor(1L).execute(MOVIE, 2, null);
        serviceFor(2L).execute(MOVIE, 4, null);

        assertEquals(2, repository.count());
    }

    @Test
    void ratingUnknownMovieThrows404WithoutSaving() {
        var service = serviceFor(1L);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(99L, 3, null));
        assertEquals(0, repository.count());
    }

    @Test
    void firstRatingSetsMovieAverage() {
        serviceFor(1L).execute(MOVIE, 4, null);

        assertEquals(4.0, movies.averageOf(MOVIE));
    }

    @Test
    void recalculatesAverageWithAllRatingsOfTheMovie() {
        serviceFor(1L).execute(MOVIE, 5, null);
        serviceFor(2L).execute(MOVIE, 2, null);
        serviceFor(3L).execute(MOVIE, 2, null);

        assertEquals(3.0, movies.averageOf(MOVIE));
    }

    @Test
    void changingRatingRecalculatesAverage() {
        serviceFor(1L).execute(MOVIE, 5, null);
        serviceFor(2L).execute(MOVIE, 1, null);

        serviceFor(2L).execute(MOVIE, 3, null);

        assertEquals(4.0, movies.averageOf(MOVIE));
    }

    @Test
    void averageOnlyCountsRatingsOfThatMovie() {
        movies.addMovie(11L);
        serviceFor(1L).execute(MOVIE, 5, null);
        serviceFor(1L).execute(11L, 1, null);

        assertEquals(5.0, movies.averageOf(MOVIE));
        assertEquals(1.0, movies.averageOf(11L));
    }

    @Test
    void rejectedRatingDoesNotTouchAverage() {
        serviceFor(1L).execute(MOVIE, 4, null);

        assertThrows(BusinessValidationException.class, () -> serviceFor(2L).execute(MOVIE, 9, null));

        assertEquals(4.0, movies.averageOf(MOVIE));
    }

    @Test
    void averageKeepsDecimals() {
        serviceFor(1L).execute(MOVIE, 4, null);
        serviceFor(2L).execute(MOVIE, 3, null);

        assertEquals(3.5, movies.averageOf(MOVIE));
    }

    @Test
    void ratingAgainWithoutCommentClearsPreviousComment() {
        serviceFor(1L).execute(MOVIE, 4, "Very good");

        var updated = serviceFor(1L).execute(MOVIE, 4, null);

        assertNull(updated.comment());
        assertNull(repository.findAll().get(0).getComment());
    }

    @Test
    void ratingUnknownMovieSavesNoAverage() {
        assertThrows(ResourceNotFoundException.class, () -> serviceFor(1L).execute(99L, 3, null));

        assertNull(movies.averageOf(99L));
    }
}
