package shop.rating.internal.persistence.repository;

import shop.rating.internal.persistence.jpa.RatingJpaRepository;
import shop.rating.internal.persistence.mapper.RatingPersistenceMapper;

import shop.rating.internal.domain.model.Rating;
import shop.rating.internal.domain.repository.RatingRepository;

import java.util.List;
import java.util.Optional;

public class RatingRepositoryImpl implements RatingRepository {

    private final RatingJpaRepository dao;

    public RatingRepositoryImpl(RatingJpaRepository dao) {
        this.dao = dao;
    }

    @Override
    public Rating save(Rating rating) {
        return RatingPersistenceMapper.toDomain(dao.save(RatingPersistenceMapper.toEntity(rating)));
    }

    @Override
    public Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId) {
        return dao.findByUserIdAndMovieId(userId, movieId).map(RatingPersistenceMapper::toDomain);
    }

    @Override
    public List<Rating> findByMovieId(Long movieId) {
        return dao.findByMovieId(movieId).stream().map(RatingPersistenceMapper::toDomain).toList();
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        dao.deleteByMovieId(movieId);
    }
}
