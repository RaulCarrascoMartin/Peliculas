package shop.rating.api.http.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import shop.rating.api.http.dto.MarkedMovieResponse;
import shop.rating.internal.domain.dto.CatalogMovieDto;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.model.MarkType;

import java.util.List;

/** DTO of the HTTP contract ↔ DTO of the movie marks domain. */
@Mapper(componentModel = "spring")
public interface MovieMarkWebMapper {

    /** The user comes from the token, the movie from the path and the list from the route. */
    @Mapping(target = "id", ignore = true)
    MovieMarkDto toDto(Long userId, Long movieId, MarkType type);

    MarkedMovieResponse toResponse(CatalogMovieDto movie);

    List<MarkedMovieResponse> toResponses(List<CatalogMovieDto> movies);
}
