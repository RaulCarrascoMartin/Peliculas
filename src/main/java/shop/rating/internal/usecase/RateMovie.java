package shop.rating.internal.usecase;

import shop.shared.error.ResourceNotFoundException;
import shop.shared.security.CurrentUser;
import shop.shared.error.BusinessValidationException;
import shop.catalog.api.CatalogApi;
import shop.rating.internal.domain.Rating;
import shop.rating.internal.domain.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateMovie {

    private final RatingRepository ratings;
    private final CatalogApi movies;
    private final CurrentUser currentUser;

    public RateMovie(RatingRepository ratings, CatalogApi movies, CurrentUser currentUser) {
        this.ratings = ratings;
        this.movies = movies;
        this.currentUser = currentUser;
    }

    static final int MIN_SCORE = 1;
    static final int MAX_SCORE = 5;

    /** Creates the current user's rating or, if one already existed for that movie, updates it. */
    @Transactional
    public RatingView execute(Long movieId, int score, String comment) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new BusinessValidationException(
                    "La puntuación debe estar entre " + MIN_SCORE + " y " + MAX_SCORE);
        }
        if (!movies.exists(movieId)) {
            throw new ResourceNotFoundException("Película " + movieId + " no encontrada");
        }
        Long userId = currentUser.id();
        Rating rating = ratings.findByUserIdAndMovieId(userId, movieId)
                .orElseGet(() -> {
                    Rating created = new Rating();
                    created.setUserId(userId);
                    created.setMovieId(movieId);
                    return created;
                });
        rating.setScore(score);
        rating.setComment(comment);
        Rating saved = ratings.save(rating);
        recalculateAverage(movieId);
        return new RatingView(saved.getId(), saved.getMovieId(), saved.getScore(),
                saved.getComment());
    }

    private void recalculateAverage(Long movieId) {
        double media = ratings.findByMovieId(movieId).stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElseThrow();
        movies.updateAverageRating(movieId, media);
    }
}
