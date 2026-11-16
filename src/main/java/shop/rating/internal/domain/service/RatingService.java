package shop.rating.internal.domain.service;

import shop.rating.internal.domain.dto.RateMovieDto;
import shop.rating.internal.domain.dto.RatingDto;
import shop.rating.internal.domain.exception.InvalidScoreException;
import shop.rating.internal.domain.exception.MovieNotFoundException;

/** Rating rules: score range, one rating per user and movie, and the movie's average. */
public interface RatingService {

    /**
     * Creates the user's rating of the movie or, if one already existed, updates it (score and comment), and
     * recalculates the movie's average in the catalog. Everything happens in one transaction: either the rating and
     * the new average are both stored or neither is.
     *
     * @throws InvalidScoreException  if the score is out of range
     * @throws MovieNotFoundException if the movie does not exist
     */
    RatingDto rate(RateMovieDto request);
}
