package shop.catalog.api.http.dto;

import shop.shared.pagination.PageResult;

import java.util.List;

/** Page of movies; the JSON keeps the {@code content} + {@code page} shape the clients already consume. */
public record MoviePage(List<MovieResponse> content, PageInfo page) {

    public record PageInfo(int size, int number, long totalElements, int totalPages) {
    }

    public static MoviePage from(PageResult<MovieResponse> result) {
        return new MoviePage(result.content(),
                new PageInfo(result.query().size(), result.query().page(), result.total(), result.totalPages()));
    }
}
