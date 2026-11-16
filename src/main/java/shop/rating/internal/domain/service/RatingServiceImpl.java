package shop.rating.internal.domain.service;

import shop.rating.internal.domain.contract.MovieCatalog;
import shop.rating.internal.domain.exception.InvalidScoreException;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.model.Rating;
import shop.rating.internal.domain.repository.RatingRepository;

public class RatingServiceImpl implements RatingService {

    static final int MIN_SCORE = 1;
    static final int MAX_SCORE = 5;

    private final RatingRepository ratings;
    private final MovieCatalog movies;

    public RatingServiceImpl(RatingRepository ratings, MovieCatalog movies) {
        this.ratings = ratings;
        this.movies = movies;
    }

    @Override
    public Rating rate(Long userId, Long movieId, int score, String comment) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new InvalidScoreException(MIN_SCORE, MAX_SCORE);
        }
        if (!movies.exists(movieId)) {
            throw new MovieNotFoundException(movieId);
        }
        Rating rating = ratings.findByUserIdAndMovieId(userId, movieId)
                .orElseGet(() -> newRating(userId, movieId));
        rating.setScore(score);
        rating.setComment(comment);
        Rating saved = ratings.save(rating);
        recalculateAverage(movieId);
        return saved;
    }

    private static Rating newRating(Long userId, Long movieId) {
        Rating created = new Rating();
        created.setUserId(userId);
        created.setMovieId(movieId);
        return created;
    }

    private void recalculateAverage(Long movieId) {
        double average = ratings.findByMovieId(movieId).stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElseThrow();
        movies.updateAverageRating(movieId, average);
    }
}
