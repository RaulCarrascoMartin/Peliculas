package shop.catalog.internal.domain.service;

import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.model.MovieDetails;
import shop.catalog.internal.domain.repository.MovieRepository;

import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class MovieServiceImpl implements MovieService {

    private final MovieRepository movies;

    public MovieServiceImpl(MovieRepository movies) {
        this.movies = movies;
    }

    @Override
    public Movie create(MovieDetails details) {
        Movie movie = new Movie();
        apply(movie, details);
        movie.setStock(details.stock() == null ? 0 : details.stock());
        return movies.save(movie);
    }

    @Override
    public Movie update(Long id, MovieDetails details) {
        Movie movie = find(id);
        apply(movie, details);
        if (details.stock() != null) {
            movie.setStock(details.stock());
        }
        return movies.save(movie);
    }

    @Override
    public Movie find(Long id) {
        return movies.findById(id).orElseThrow(() -> new MovieNotFoundException(id));
    }

    @Override
    public void delete(Long id) {
        if (!movies.existsById(id)) {
            throw new MovieNotFoundException(id);
        }
        movies.deleteById(id);
    }

    @Override
    public PageResult<Movie> search(String title, String genre, Integer year, String director, PageQuery page) {
        return movies.search(blankToNull(title), blankToNull(genre), year, blankToNull(director), page);
    }

    @Override
    public boolean exists(Long id) {
        return movies.existsById(id);
    }

    @Override
    public void updateAverageRating(Long id, Double average) {
        Movie movie = find(id);
        movie.setAverageRating(average);
        movies.save(movie);
    }

    @Override
    public List<Movie> findAllById(Collection<Long> ids) {
        return movies.findAllById(ids).stream().sorted(Comparator.comparing(Movie::getId)).toList();
    }

    private static void apply(Movie movie, MovieDetails details) {
        movie.setTitle(details.title());
        movie.setYear(details.year());
        movie.setGenre(details.genre());
        movie.setDirector(details.director());
        movie.setPrice(details.price());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
