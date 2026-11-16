package shop.rating.internal.domain.repository;

import shop.rating.internal.domain.model.Rating;

import java.util.List;
import java.util.Optional;

public interface RatingRepository {

    Rating save(Rating rating);

    Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId);

    List<Rating> findByMovieId(Long movieId);

    void deleteByMovieId(Long movieId);
}
