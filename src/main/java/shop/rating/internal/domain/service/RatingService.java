package shop.rating.internal.domain.service;

import shop.rating.internal.domain.exception.InvalidScoreException;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.model.Rating;

/** Rating rules: score range, one rating per user and movie, and the movie's average. */
public interface RatingService {

    /**
     * Creates the user's rating of the movie or, if one already existed, updates it, and recalculates the average.
     *
     * @throws InvalidScoreException if the score is out of range
     * @throws MovieNotFoundException if the movie does not exist
     */
    Rating rate(Long userId, Long movieId, int score, String comment);
}
