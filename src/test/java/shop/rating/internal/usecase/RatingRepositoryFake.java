package shop.rating.internal.usecase;

import shop.shared.InMemoryRepository;
import shop.rating.internal.domain.Rating;
import shop.rating.internal.domain.RatingRepository;

import java.util.List;
import java.util.Optional;

public class RatingRepositoryFake extends InMemoryRepository<Rating> implements RatingRepository {

    @Override
    protected Long idOf(Rating rating) {
        return rating.getId();
    }

    @Override
    protected void assignId(Rating rating, Long id) {
        rating.setId(id);
    }

    @Override
    public Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId) {
        return entities.values().stream()
                .filter(v -> v.getUserId().equals(userId) && v.getMovieId().equals(movieId))
                .findFirst();
    }

    @Override
    public List<Rating> findByMovieId(Long movieId) {
        return entities.values().stream().filter(v -> v.getMovieId().equals(movieId)).toList();
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        entities.values().removeIf(v -> v.getMovieId().equals(movieId));
    }
}
