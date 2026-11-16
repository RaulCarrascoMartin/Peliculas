package shop.rating.internal.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InOrder;
import shop.rating.internal.domain.contract.MovieCatalog;
import shop.rating.internal.domain.dto.RateMovieDto;
import shop.rating.internal.domain.dto.RatingDto;
import shop.rating.internal.domain.exception.InvalidScoreException;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.exception.RatingAlreadyExistsException;
import shop.rating.internal.domain.mapper.RatingMapper;
import shop.rating.internal.domain.repository.RatingRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Rating rules with the repository and the catalog mocked; the MapStruct mapper is the real one. */
class RatingServiceTest {

    private static final Long MOVIE = 10L;
    private static final Long NEW_ID = 50L;

    private RatingRepository ratings;
    private MovieCatalog movies;
    private RatingService service;

    @BeforeEach
    void setUp() {
        ratings = mock(RatingRepository.class);
        movies = mock(MovieCatalog.class);
        service = new RatingServiceImpl(ratings, movies, Mappers.getMapper(RatingMapper.class));
        when(movies.exists(MOVIE)).thenReturn(true);
        when(ratings.save(any())).thenAnswer(call -> {
            RatingDto r = call.getArgument(0);
            return r.id() != null ? r : new RatingDto(NEW_ID, r.userId(), r.movieId(), r.score(), r.comment());
        });
    }

    /** The movie has ratings with these scores once the new one is saved. */
    private void ratingsOfTheMovie(Integer... scores) {
        List<RatingDto> stored = Arrays.stream(scores)
                .map(score -> new RatingDto(null, null, MOVIE, score, null))
                .toList();
        when(ratings.findByMovieId(MOVIE)).thenReturn(stored);
    }

    @Test
    void firstRatingOfTheUserIsSavedWithoutIdAndReturnedWithTheAssignedOne() {
        when(ratings.findByUserIdAndMovieId(1L, MOVIE)).thenReturn(Optional.empty());
        ratingsOfTheMovie(4);

        RatingDto rating = service.rate(new RateMovieDto(1L, MOVIE, 4, "Very good"));

        verify(ratings).save(new RatingDto(null, 1L, MOVIE, 4, "Very good"));
        assertEquals(new RatingDto(NEW_ID, 1L, MOVIE, 4, "Very good"), rating);
    }

    @Test
    void acceptsRangeBoundaries() {
        ratingsOfTheMovie(1, 5);

        service.rate(new RateMovieDto(1L, MOVIE, 1, null));
        service.rate(new RateMovieDto(2L, MOVIE, 5, null));

        verify(ratings).save(new RatingDto(null, 1L, MOVIE, 1, null));
        verify(ratings).save(new RatingDto(null, 2L, MOVIE, 5, null));
    }

    @Test
    void rejectsOutOfRangeScoresWithoutSavingNorTouchingTheAverage() {
        var ex = assertThrows(InvalidScoreException.class, () -> service.rate(new RateMovieDto(1L, MOVIE, 0, null)));
        assertThrows(InvalidScoreException.class, () -> service.rate(new RateMovieDto(1L, MOVIE, 6, null)));
        assertThrows(InvalidScoreException.class, () -> service.rate(new RateMovieDto(1L, MOVIE, -3, null)));
        assertThrows(InvalidScoreException.class, () -> service.rate(new RateMovieDto(1L, MOVIE, 9, null)));

        assertEquals("La puntuación debe estar entre 1 y 5", ex.getMessage());
        verifyNoInteractions(ratings);
        verify(movies, never()).updateAverageRating(anyLong(), anyDouble());
    }

    @Test
    void outOfRangeScoreIsRejectedBeforeLookingForTheMovie() {
        assertThrows(InvalidScoreException.class, () -> service.rate(new RateMovieDto(1L, 99L, 0, null)));

        verifyNoInteractions(movies);
    }

    @Test
    void ratingAgainUpdatesThePreviousRatingKeepingItsId() {
        when(ratings.findByUserIdAndMovieId(1L, MOVIE))
                .thenReturn(Optional.of(new RatingDto(7L, 1L, MOVIE, 2, "Weak")));
        ratingsOfTheMovie(5);

        RatingDto second = service.rate(new RateMovieDto(1L, MOVIE, 5, "Better on second viewing"));

        verify(ratings).save(new RatingDto(7L, 1L, MOVIE, 5, "Better on second viewing"));
        assertEquals(7L, second.id());
        assertEquals(5, second.score());
    }

    @Test
    void ratingAgainWithoutCommentClearsThePreviousComment() {
        when(ratings.findByUserIdAndMovieId(1L, MOVIE))
                .thenReturn(Optional.of(new RatingDto(7L, 1L, MOVIE, 4, "Very good")));
        ratingsOfTheMovie(4);

        RatingDto updated = service.rate(new RateMovieDto(1L, MOVIE, 4, null));

        assertNull(updated.comment());
        verify(ratings).save(new RatingDto(7L, 1L, MOVIE, 4, null));
    }

    @Test
    void eachUserHasTheirOwnRating() {
        when(ratings.findByUserIdAndMovieId(1L, MOVIE))
                .thenReturn(Optional.of(new RatingDto(7L, 1L, MOVIE, 2, null)));
        when(ratings.findByUserIdAndMovieId(2L, MOVIE)).thenReturn(Optional.empty());
        ratingsOfTheMovie(2, 4);

        service.rate(new RateMovieDto(2L, MOVIE, 4, null));

        verify(ratings).findByUserIdAndMovieId(2L, MOVIE);
        verify(ratings).save(new RatingDto(null, 2L, MOVIE, 4, null));
    }

    @Test
    void ratingUnknownMovieFailsWithoutSavingNorStoringAnAverage() {
        when(movies.exists(99L)).thenReturn(false);

        var ex = assertThrows(MovieNotFoundException.class, () -> service.rate(new RateMovieDto(1L, 99L, 3, null)));

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(ratings);
        verify(movies, never()).updateAverageRating(anyLong(), anyDouble());
    }

    @Test
    void firstRatingSetsTheMovieAverage() {
        ratingsOfTheMovie(4);

        service.rate(new RateMovieDto(1L, MOVIE, 4, null));

        verify(movies).updateAverageRating(MOVIE, 4.0);
    }

    @Test
    void averageIsRecalculatedWithAllTheRatingsOfThatMovieAfterSaving() {
        ratingsOfTheMovie(5, 2, 2);

        service.rate(new RateMovieDto(3L, MOVIE, 2, null));

        InOrder order = inOrder(ratings, movies);
        order.verify(ratings).save(new RatingDto(null, 3L, MOVIE, 2, null));
        order.verify(ratings).findByMovieId(MOVIE);
        order.verify(movies).updateAverageRating(MOVIE, 3.0);
    }

    @Test
    void averageOnlyCountsRatingsOfThatMovie() {
        when(movies.exists(11L)).thenReturn(true);
        when(ratings.findByMovieId(11L)).thenReturn(List.of(new RatingDto(8L, 1L, 11L, 1, null)));
        ratingsOfTheMovie(5);

        service.rate(new RateMovieDto(1L, MOVIE, 5, null));
        service.rate(new RateMovieDto(1L, 11L, 1, null));

        verify(movies).updateAverageRating(MOVIE, 5.0);
        verify(movies).updateAverageRating(11L, 1.0);
    }

    @Test
    void changingARatingRecalculatesTheAverage() {
        when(ratings.findByUserIdAndMovieId(2L, MOVIE))
                .thenReturn(Optional.of(new RatingDto(8L, 2L, MOVIE, 1, null)));
        ratingsOfTheMovie(5, 3);

        service.rate(new RateMovieDto(2L, MOVIE, 3, null));

        verify(movies).updateAverageRating(MOVIE, 4.0);
    }

    @Test
    void averageKeepsDecimals() {
        ratingsOfTheMovie(4, 3);

        service.rate(new RateMovieDto(2L, MOVIE, 3, null));

        verify(movies).updateAverageRating(MOVIE, 3.5);
    }

    @Test
    void collisionWithASimultaneousRatingLeavesTheAverageUntouched() {
        doThrow(new RatingAlreadyExistsException(MOVIE)).when(ratings).save(any());

        assertThrows(RatingAlreadyExistsException.class, () -> service.rate(new RateMovieDto(1L, MOVIE, 4, null)));

        verify(movies, never()).updateAverageRating(anyLong(), anyDouble());
    }
}
