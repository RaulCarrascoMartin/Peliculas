package shop.catalog.internal.application.usecase;

import shop.catalog.internal.application.dto.MovieData;
import shop.catalog.internal.application.mapper.MovieSummaryMapper;

import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.internal.domain.service.MovieService;

public class CreateMovie {

    private final MovieService movies;

    public CreateMovie(MovieService movies) {
        this.movies = movies;
    }

    public MovieSummaryDto execute(MovieData data) {
        return MovieSummaryMapper.toSummary(movies.create(data.toDetails()));
    }
}
