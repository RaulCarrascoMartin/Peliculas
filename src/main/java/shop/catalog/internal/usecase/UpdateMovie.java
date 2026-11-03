package shop.catalog.internal.usecase;

import shop.shared.error.ResourceNotFoundException;
import shop.catalog.api.MovieSummary;
import shop.catalog.internal.domain.Movie;
import shop.catalog.internal.domain.MovieRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateMovie {

    private final MovieRepository movies;

    public UpdateMovie(MovieRepository movies) {
        this.movies = movies;
    }

    /** Replaces title, year, genre, director, price and (if given) stock; the average rating is not edited by hand. */
    public MovieSummary execute(Long id, MovieData data) {
        Movie existing = movies.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película " + id + " no encontrada"));
        existing.setTitle(data.title());
        existing.setYear(data.year());
        existing.setGenre(data.genre());
        existing.setDirector(data.director());
        existing.setPrice(data.price());

        if (data.stock() != null) {
            existing.setStock(data.stock());
        }
        return MovieSummaryMapper.toSummary(movies.save(existing));
    }
}
