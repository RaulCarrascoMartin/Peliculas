package shop.catalog.api.http.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import shop.catalog.api.http.dto.MoviePageResponse;
import shop.catalog.api.http.dto.MovieRequest;
import shop.catalog.api.http.dto.MovieResponse;
import shop.catalog.internal.domain.dto.MovieDetailsDto;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.util.List;

/** DTO and parameters of the HTTP contract ↔ DTO of the catalog domain. */
@Mapper(componentModel = "spring")
public interface MovieWebMapper {

    MovieDetailsDto toDto(MovieRequest request);

    MovieResponse toResponse(MovieDto movie);

    List<MovieResponse> toResponses(List<MovieDto> movies);

    @Mapping(target = "content", source = "content")
    @Mapping(target = "page", source = "result")
    MoviePageResponse toPageResponse(PageResult<MovieDto> result);

    @Mapping(target = "size", source = "query.size")
    @Mapping(target = "number", source = "query.page")
    @Mapping(target = "totalElements", source = "total")
    @Mapping(target = "totalPages", expression = "java(result.totalPages())")
    MoviePageResponse.PageInfo toPageInfo(PageResult<MovieDto> result);

    /** The pagination parameters of the query string as the page request of the domain. */
    default PageQuery toPageQuery(int page, int size, List<String> sort) {
        return new PageQuery(page, size, sort.stream().map(this::toSort).toList());
    }

    /**
     * Format of each {@code sort} value: {@code property} or {@code property,asc|desc}; ascending by default. Spring
     * splits a single {@code sort} parameter by commas, so the direction only arrives here together with its
     * property when the parameter is repeated.
     */
    default PageQuery.Sort toSort(String param) {
        String[] parts = param.split(",");
        boolean descending = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");
        return new PageQuery.Sort(parts[0].trim(), !descending);
    }
}
