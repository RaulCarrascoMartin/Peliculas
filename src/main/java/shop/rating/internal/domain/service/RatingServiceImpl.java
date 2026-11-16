package shop.rating.internal.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.rating.internal.domain.contract.MovieCatalog;
import shop.rating.internal.domain.dto.RateMovieDto;
import shop.rating.internal.domain.dto.RatingDto;
import shop.rating.internal.domain.exception.InvalidScoreException;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.mapper.RatingMapper;
import shop.rating.internal.domain.model.Rating;
import shop.rating.internal.domain.repository.RatingRepository;

@Service
public class RatingServiceImpl implements RatingService {

    static final int MIN_SCORE = 1;
    static final int MAX_SCORE = 5;

    private final RatingRepository ratings;
    private final MovieCatalog movies;
    private final RatingMapper mapper;

    public RatingServiceImpl(RatingRepository ratings, MovieCatalog movies, RatingMapper mapper) {
        this.ratings = ratings;
        this.movies = movies;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public RatingDto rate(RateMovieDto request) {
        if (request.score() < MIN_SCORE || request.score() > MAX_SCORE) {
            throw new InvalidScoreException(MIN_SCORE, MAX_SCORE);
        }
        if (!movies.exists(request.movieId())) {
            throw new MovieNotFoundException(request.movieId());
        }
        Rating rating = ratings.findByUserIdAndMovieId(request.userId(), request.movieId())
                .map(mapper::toModel)
                .orElseGet(() -> newRating(request.userId(), request.movieId()));
        rating.setScore(request.score());
        rating.setComment(request.comment());
        RatingDto saved = ratings.save(mapper.toDto(rating));
        recalculateAverage(request.movieId());
        return saved;
    }

    private static Rating newRating(Long userId, Long movieId) {
        Rating created = new Rating();
        created.setUserId(userId);
        created.setMovieId(movieId);
        return created;
    }

    /** The average of every rating of the movie, the one just saved included, is pushed to the catalog. */
    private void recalculateAverage(Long movieId) {
        double average = ratings.findByMovieId(movieId).stream()
                .mapToInt(RatingDto::score)
                .average()
                .orElseThrow();
        movies.updateAverageRating(movieId, average);
    }
}
