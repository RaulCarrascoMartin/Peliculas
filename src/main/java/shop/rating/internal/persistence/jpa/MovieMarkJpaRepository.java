package shop.rating.internal.persistence.jpa;

import shop.rating.internal.persistence.entity.MovieMarkEntity;

import shop.rating.internal.domain.model.MarkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovieMarkJpaRepository extends JpaRepository<MovieMarkEntity, Long> {

    Optional<MovieMarkEntity> findByUserIdAndMovieIdAndType(Long userId, Long movieId, MarkType type);

    List<MovieMarkEntity> findByUserIdAndType(Long userId, MarkType type);

    void deleteByMovieId(Long movieId);
}
