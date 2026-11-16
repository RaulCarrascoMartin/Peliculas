package shop.catalog.internal.application.usecase;

import shop.catalog.internal.application.mapper.MovieSummaryMapper;

import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.internal.domain.service.MovieService;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

public class SearchMovies {

    private final MovieService movies;

    public SearchMovies(MovieService movies) {
        this.movies = movies;
    }

    /** Paged list; null or blank filters are ignored and the given ones are combined with "and". */
    public PageResult<MovieSummaryDto> execute(String title, String genre, Integer year, String director,
                                               PageQuery page) {
        return movies.search(title, genre, year, director, page).map(MovieSummaryMapper::toSummary);
    }
}
