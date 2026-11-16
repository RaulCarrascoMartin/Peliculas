package shop.rating.internal.domain.repository;

import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.model.MovieMark;

import java.util.List;
import java.util.Optional;

public interface MovieMarkRepository {

    MovieMark save(MovieMark mark);

    void delete(MovieMark mark);

    Optional<MovieMark> findByUserIdAndMovieIdAndType(Long userId, Long movieId, MarkType type);

    List<MovieMark> findByUserIdAndType(Long userId, MarkType type);

    void deleteByMovieId(Long movieId);
}
