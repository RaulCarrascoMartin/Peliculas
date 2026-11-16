package shop.rating.api.http.mapper;

import org.mapstruct.Mapper;
import shop.rating.api.http.dto.RatingRequest;
import shop.rating.api.http.dto.RatingResponse;
import shop.rating.internal.domain.dto.RateMovieDto;
import shop.rating.internal.domain.dto.RatingDto;

/** DTO of the HTTP contract ↔ DTO of the ratings domain. */
@Mapper(componentModel = "spring")
public interface RatingWebMapper {

    /** The user comes from the token and the movie from the path; the body brings the score and the comment. */
    RateMovieDto toDto(Long userId, Long movieId, RatingRequest request);

    RatingResponse toResponse(RatingDto rating);
}
