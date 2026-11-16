package shop.catalog.internal.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import shop.catalog.internal.domain.dto.MovieDetailsDto;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.dto.ReservationDto;
import shop.catalog.internal.domain.model.Movie;

/** DTO ↔ model of the catalog domain. Only transforms data: the rules live in the services. */
@Mapper(componentModel = "spring")
public interface MovieMapper {

    Movie toModel(MovieDto dto);

    MovieDto toDto(Movie movie);

    /**
     * Copies the editable data onto the movie. The id and the average rating are not editable, and what a missing
     * stock means is a rule of the service, so those three are left as they were.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "stock", ignore = true)
    void copyDetails(MovieDetailsDto details, @MappingTarget Movie movie);

    /** What was reserved of a movie, with its title and price at that moment. */
    @Mapping(target = "movieId", source = "movie.id")
    @Mapping(target = "title", source = "movie.title")
    @Mapping(target = "unitPrice", source = "movie.price")
    @Mapping(target = "quantity", source = "quantity")
    ReservationDto toReservation(Movie movie, int quantity);
}
