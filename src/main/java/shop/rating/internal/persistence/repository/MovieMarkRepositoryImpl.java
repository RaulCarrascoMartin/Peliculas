package shop.rating.internal.persistence.repository;

import shop.rating.internal.persistence.jpa.MovieMarkJpaRepository;
import shop.rating.internal.persistence.mapper.RatingPersistenceMapper;

import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.model.MovieMark;
import shop.rating.internal.domain.repository.MovieMarkRepository;

import java.util.List;
import java.util.Optional;

public class MovieMarkRepositoryImpl implements MovieMarkRepository {

    private final MovieMarkJpaRepository dao;

    public MovieMarkRepositoryImpl(MovieMarkJpaRepository dao) {
        this.dao = dao;
    }

    @Override
    public MovieMark save(MovieMark mark) {
        return RatingPersistenceMapper.toDomain(dao.save(RatingPersistenceMapper.toEntity(mark)));
    }

    @Override
    public void delete(MovieMark mark) {
        dao.deleteById(mark.getId());
    }

    @Override
    public Optional<MovieMark> findByUserIdAndMovieIdAndType(Long userId, Long movieId, MarkType type) {
        return dao.findByUserIdAndMovieIdAndType(userId, movieId, type).map(RatingPersistenceMapper::toDomain);
    }

    @Override
    public List<MovieMark> findByUserIdAndType(Long userId, MarkType type) {
        return dao.findByUserIdAndType(userId, type).stream().map(RatingPersistenceMapper::toDomain).toList();
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        dao.deleteByMovieId(movieId);
    }
}
