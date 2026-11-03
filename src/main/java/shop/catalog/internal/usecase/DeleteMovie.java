package shop.catalog.internal.usecase;

import shop.shared.error.ResourceNotFoundException;
import shop.catalog.internal.domain.MovieRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteMovie {

    private final MovieRepository movies;

    public DeleteMovie(MovieRepository movies) {
        this.movies = movies;
    }

    public void execute(Long id) {
        if (!movies.existsById(id)) {
            throw new ResourceNotFoundException("Película " + id + " no encontrada");
        }
        movies.deleteById(id);
    }
}
