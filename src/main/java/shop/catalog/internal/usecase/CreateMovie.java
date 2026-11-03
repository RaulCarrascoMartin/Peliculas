package shop.catalog.internal.usecase;

import shop.catalog.api.MovieSummary;
import shop.catalog.internal.domain.Movie;
import shop.catalog.internal.domain.MovieRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateMovie {

    private final MovieRepository movies;

    public CreateMovie(MovieRepository movies) {
        this.movies = movies;
    }

    public MovieSummary execute(MovieData data) {
        Movie movie = new Movie();
        movie.setTitle(data.title());
        movie.setYear(data.year());
        movie.setGenre(data.genre());
        movie.setDirector(data.director());
        movie.setPrice(data.price());
        movie.setStock(data.stock() == null ? 0 : data.stock());
        return MovieSummaryMapper.toSummary(movies.save(movie));
    }
}
