package shop.rating.internal.usecase;

import shop.shared.InMemoryRepository;
import shop.rating.internal.domain.MovieMark;
import shop.rating.internal.domain.MovieMarkRepository;
import shop.rating.internal.domain.MarkType;

import java.util.List;
import java.util.Optional;

public class MovieMarkRepositoryFake extends InMemoryRepository<MovieMark>
        implements MovieMarkRepository {

    @Override
    protected Long idOf(MovieMark mark) {
        return mark.getId();
    }

    @Override
    protected void assignId(MovieMark mark, Long id) {
        mark.setId(id);
    }

    @Override
    public Optional<MovieMark> findByUserIdAndMovieIdAndType(Long userId, Long movieId, MarkType type) {
        return entities.values().stream()
                .filter(m -> m.getUserId().equals(userId) && m.getMovieId().equals(movieId)
                        && m.getType() == type)
                .findFirst();
    }

    @Override
    public List<MovieMark> findByUserIdAndType(Long userId, MarkType type) {
        return entities.values().stream()
                .filter(m -> m.getUserId().equals(userId) && m.getType() == type)
                .toList();
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        entities.values().removeIf(m -> m.getMovieId().equals(movieId));
    }
}
