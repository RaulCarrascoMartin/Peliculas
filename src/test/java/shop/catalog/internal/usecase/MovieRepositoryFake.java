package shop.catalog.internal.usecase;

import shop.shared.InMemoryRepository;
import shop.catalog.internal.domain.Movie;
import shop.catalog.internal.domain.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
    public Page<Movie> findAll(Pageable pageable) {
        List<Movie> all = List.copyOf(entities.values());
        int from = (int) Math.min(pageable.getOffset(), all.size());
        int hasta = Math.min(from + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(from, hasta), pageable, all.size());
    }

    @Override
    public Page<Movie> search(String title, String genre, Integer year, String director, Pageable pageable) {
        List<Movie> filtered = entities.values().stream()
                .filter(p -> title == null || p.getTitle().toLowerCase().contains(title.toLowerCase()))
                .filter(p -> genre == null || genre.equalsIgnoreCase(p.getGenre()))
                .filter(p -> year == null || year.equals(p.getYear()))
                .filter(p -> director == null || director.equalsIgnoreCase(p.getDirector()))
                .toList();
        int from = (int) Math.min(pageable.getOffset(), filtered.size());
        int hasta = Math.min(from + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(from, hasta), pageable, filtered.size());
    }

    @Override
    public List<Movie> findAllById(Iterable<Long> ids) {
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
