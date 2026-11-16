package shop.rating.internal.application.usecase;

import shop.rating.internal.application.dto.RatingView;

import jakarta.transaction.Transactional;
import shop.rating.internal.domain.model.Rating;
import shop.rating.internal.domain.service.RatingService;

public class RateMovie {

    private final RatingService ratings;

    public RateMovie(RatingService ratings) {
        this.ratings = ratings;
    }

    /** Creates the user's rating or, if one already existed for that movie, updates it. */
    @Transactional
    public RatingView execute(Long userId, Long movieId, int score, String comment) {
        Rating saved = ratings.rate(userId, movieId, score, comment);
        return new RatingView(saved.getId(), saved.getMovieId(), saved.getScore(), saved.getComment());
    }
}
