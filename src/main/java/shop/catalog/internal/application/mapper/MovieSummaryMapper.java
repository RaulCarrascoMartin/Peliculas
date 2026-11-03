package shop.catalog.internal.application.mapper;

import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.internal.domain.model.Movie;

/** Converts the entity into the record that leaves the use cases: the model never crosses this boundary. */
public final class MovieSummaryMapper {

    private MovieSummaryMapper() {
    }

    public static MovieSummaryDto toSummary(Movie p) {
        return new MovieSummaryDto(p.getId(), p.getTitle(), p.getYear(), p.getGenre(), p.getDirector(),
                p.getPrice(), p.getAverageRating(), p.getStock());
    }
}
