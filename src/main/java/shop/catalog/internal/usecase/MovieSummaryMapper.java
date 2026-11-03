package shop.catalog.internal.usecase;

import shop.catalog.api.MovieSummary;
import shop.catalog.internal.domain.Movie;

/** Converts the entity into the record that leaves the use cases: the model never crosses this boundary. */
final class MovieSummaryMapper {

    private MovieSummaryMapper() {
    }

    static MovieSummary toSummary(Movie p) {
        return new MovieSummary(p.getId(), p.getTitle(), p.getYear(), p.getGenre(), p.getDirector(),
                p.getPrice(), p.getAverageRating(), p.getStock());
    }
}
