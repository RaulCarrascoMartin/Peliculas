package shop.rating.internal.domain.mapper;

import org.mapstruct.Mapper;
import shop.rating.internal.domain.dto.RatingDto;
import shop.rating.internal.domain.model.Rating;

/** DTO ↔ model of ratings. Only transforms data: the rules live in the service. */
@Mapper(componentModel = "spring")
public interface RatingMapper {

    Rating toModel(RatingDto dto);

    RatingDto toDto(Rating rating);
}
