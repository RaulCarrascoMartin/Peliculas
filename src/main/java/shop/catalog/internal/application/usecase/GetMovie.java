package shop.catalog.internal.application.usecase;

import shop.catalog.internal.application.mapper.MovieSummaryMapper;

import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.internal.domain.service.MovieService;

public class GetMovie {

    private final MovieService movies;

    public GetMovie(MovieService movies) {
        this.movies = movies;
    }

    public MovieSummaryDto execute(Long id) {
        return MovieSummaryMapper.toSummary(movies.find(id));
    }
}
