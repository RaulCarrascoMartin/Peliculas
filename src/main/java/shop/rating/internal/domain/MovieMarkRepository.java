package shop.rating.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovieMarkRepository extends JpaRepository<MovieMark, Long> {

    Optional<MovieMark> findByUserIdAndMovieIdAndType(Long userId, Long movieId, MarkType type);

    List<MovieMark> findByUserIdAndType(Long userId, MarkType type);

    void deleteByMovieId(Long movieId);
}
