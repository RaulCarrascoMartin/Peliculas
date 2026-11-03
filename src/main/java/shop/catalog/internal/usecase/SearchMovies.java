package shop.catalog.internal.usecase;

import shop.catalog.api.MovieSummary;
import shop.catalog.internal.domain.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SearchMovies {

    private final MovieRepository movies;

    public SearchMovies(MovieRepository movies) {
        this.movies = movies;
    }

    /** Paged list; null or blank filters are ignored and the given ones are combined with "and". */
    public Page<MovieSummary> execute(String title, String genre, Integer year, String director,
                                          Pageable pageable) {
        return movies.search(blankToNull(title), blankToNull(genre), year, blankToNull(director), pageable)
                .map(MovieSummaryMapper::toSummary);
    }

    private static String blankToNull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
