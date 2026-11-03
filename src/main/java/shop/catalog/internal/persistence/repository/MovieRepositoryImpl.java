package shop.catalog.internal.persistence.repository;

import shop.catalog.internal.persistence.entity.MovieEntity;
import shop.catalog.internal.persistence.jpa.MovieJpaRepository;
import shop.catalog.internal.persistence.mapper.MoviePersistenceMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.repository.MovieRepository;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** {@link MovieRepository} over Spring Data JPA: translates between {@link Movie} and {@link MovieEntity}. */
public class MovieRepositoryImpl implements MovieRepository {

    private final MovieJpaRepository dao;

    public MovieRepositoryImpl(MovieJpaRepository dao) {
        this.dao = dao;
    }

    @Override
    public Movie save(Movie movie) {
        return MoviePersistenceMapper.toDomain(dao.save(MoviePersistenceMapper.toEntity(movie)));
    }

    @Override
    public Optional<Movie> findById(Long id) {
        return dao.findById(id).map(MoviePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return dao.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        dao.deleteById(id);
    }

    @Override
    public List<Movie> findAllById(Collection<Long> ids) {
        return dao.findAllById(ids).stream().map(MoviePersistenceMapper::toDomain).toList();
    }

    @Override
    public PageResult<Movie> search(String title, String genre, Integer year, String director, PageQuery page) {
        Sort sort = Sort.by(page.sort().stream()
                .map(s -> new Sort.Order(s.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, s.property()))
                .toList());
        Page<MovieEntity> found = dao.search(title, genre, year, director,
                PageRequest.of(page.page(), page.size(), sort));
        return new PageResult<>(found.getContent().stream().map(MoviePersistenceMapper::toDomain).toList(),
                found.getTotalElements(), page);
    }

    @Override
    public int decrementStock(Long id, int quantity) {
        return dao.decrementStock(id, quantity);
    }

    @Override
    public int incrementStock(Long id, int quantity) {
        return dao.incrementStock(id, quantity);
    }
}
