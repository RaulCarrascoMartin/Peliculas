package shop.catalog.api.http.dto;

import java.util.List;

/** Page of movies; the JSON keeps the {@code content} + {@code page} shape the clients already consume. */
public record MoviePageResponse(List<MovieResponse> content, PageInfo page) {

    public record PageInfo(int size, int number, long totalElements, int totalPages) {
    }
}
