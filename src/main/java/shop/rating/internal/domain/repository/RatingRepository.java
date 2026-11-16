package shop.rating.internal.domain.repository;

import shop.rating.internal.domain.dto.RatingDto;
import shop.rating.internal.domain.exception.RatingAlreadyExistsException;

import java.util.List;
import java.util.Optional;

/** Rating persistence; the implementation uses JDBC and lives in {@code internal.persistence}. */
public interface RatingRepository {

    /**
     * Inserts the rating (no id) or updates it (with id) and returns it with its id.
     *
     * @throws RatingAlreadyExistsException if the user already has another rating of that movie
     */
    RatingDto save(RatingDto rating);

    Optional<RatingDto> findByUserIdAndMovieId(Long userId, Long movieId);

    /** Ratings of the movie, sorted by id. */
    List<RatingDto> findByMovieId(Long movieId);

    void deleteByMovieId(Long movieId);
}
