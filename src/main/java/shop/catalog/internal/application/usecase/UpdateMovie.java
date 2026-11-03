package shop.catalog.internal.application.usecase;

import shop.catalog.internal.application.dto.MovieData;
import shop.catalog.internal.application.mapper.MovieSummaryMapper;

import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.internal.domain.service.MovieService;

public class UpdateMovie {

    private final MovieService movies;

    public UpdateMovie(MovieService movies) {
        this.movies = movies;
    }

    /** Replaces title, year, genre, director, price and (if given) stock; the average rating is not edited by hand. */
    public MovieSummaryDto execute(Long id, MovieData data) {
        return MovieSummaryMapper.toSummary(movies.update(id, data.toDetails()));
    }
}
