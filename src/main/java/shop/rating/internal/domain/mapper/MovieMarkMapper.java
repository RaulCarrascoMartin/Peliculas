package shop.rating.internal.domain.mapper;

import org.mapstruct.Mapper;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.model.MovieMark;

/** DTO ↔ model of movie marks. Only transforms data: the rules live in the service. */
@Mapper(componentModel = "spring")
public interface MovieMarkMapper {

    MovieMark toModel(MovieMarkDto dto);

    MovieMarkDto toDto(MovieMark mark);
}
