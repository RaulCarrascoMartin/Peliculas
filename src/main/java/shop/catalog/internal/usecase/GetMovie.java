package shop.catalog.internal.usecase;

import shop.shared.error.ResourceNotFoundException;
import shop.catalog.api.MovieSummary;
import shop.catalog.internal.domain.MovieRepository;
import org.springframework.stereotype.Service;

@Service
public class GetMovie {

    private final MovieRepository movies;

    public GetMovie(MovieRepository movies) {
        this.movies = movies;
    }

    public MovieSummary execute(Long id) {
        return movies.findById(id)
                .map(MovieSummaryMapper::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException("Película " + id + " no encontrada"));
    }
}
