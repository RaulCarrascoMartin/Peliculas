package shop.catalog.internal.domain.fake;

import java.util.Collection;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;
import shop.shared.InMemoryRepository;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.repository.MovieRepository;

import java.util.List;

public class MovieRepositoryFake extends InMemoryRepository<Movie> implements MovieRepository {

    @Override
    protected Long idOf(Movie movie) {
        return movie.getId();
    }

    @Override
    protected void assignId(Movie movie, Long id) {
        movie.setId(id);
    }

    @Override
    public PageResult<Movie> search(String title, String genre, Integer year, String director, PageQuery page) {
        List<Movie> filtered = entities.values().stream()
                .filter(p -> title == null || p.getTitle().toLowerCase().contains(title.toLowerCase()))
                .filter(p -> genre == null || genre.equalsIgnoreCase(p.getGenre()))
                .filter(p -> year == null || year.equals(p.getYear()))
                .filter(p -> director == null || director.equalsIgnoreCase(p.getDirector()))
                .toList();
        int from = (int) Math.min(page.offset(), filtered.size());
        int to = Math.min(from + page.size(), filtered.size());
        return new PageResult<>(filtered.subList(from, to), filtered.size(), page);
    }

    @Override
    public List<Movie> findAllById(Collection<Long> ids) {
        List<Movie> found = new java.util.ArrayList<>();
        ids.forEach(id -> findById(id).ifPresent(found::add));
        return found;
    }

    @Override
    public int decrementStock(Long id, int quantity) {
        return findById(id).filter(p -> p.getStock() >= quantity).map(p -> {
            p.setStock(p.getStock() - quantity);
            return 1;
        }).orElse(0);
    }

    @Override
    public int incrementStock(Long id, int quantity) {
        return findById(id).map(p -> {
            p.setStock(p.getStock() + quantity);
            return 1;
        }).orElse(0);
    }
}
