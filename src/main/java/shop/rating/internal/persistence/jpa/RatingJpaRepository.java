package shop.rating.internal.persistence.jpa;

import shop.rating.internal.persistence.entity.RatingEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingJpaRepository extends JpaRepository<RatingEntity, Long> {

    Optional<RatingEntity> findByUserIdAndMovieId(Long userId, Long movieId);

    List<RatingEntity> findByMovieId(Long movieId);

    void deleteByMovieId(Long movieId);
}
