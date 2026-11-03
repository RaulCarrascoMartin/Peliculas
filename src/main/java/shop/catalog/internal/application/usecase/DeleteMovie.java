package shop.catalog.internal.application.usecase;

import shop.catalog.internal.domain.service.MovieService;

public class DeleteMovie {

    private final MovieService movies;

    public DeleteMovie(MovieService movies) {
        this.movies = movies;
    }

    public void execute(Long id) {
        movies.delete(id);
    }
}
