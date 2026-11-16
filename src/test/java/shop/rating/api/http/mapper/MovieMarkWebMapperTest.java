package shop.rating.api.http.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.rating.api.http.dto.MarkedMovieResponse;
import shop.rating.internal.domain.dto.CatalogMovieDto;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.model.MarkType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovieMarkWebMapperTest {

    private final MovieMarkWebMapper mapper = Mappers.getMapper(MovieMarkWebMapper.class);

    @Test
    void userMovieAndListBecomeTheDtoOfTheServiceWithoutId() {
        assertEquals(new MovieMarkDto(null, 7L, 10L, MarkType.FAVORITA), mapper.toDto(7L, 10L, MarkType.FAVORITA));
        assertEquals(new MovieMarkDto(null, 8L, 11L, MarkType.VISTA), mapper.toDto(8L, 11L, MarkType.VISTA));
    }

    @Test
    void catalogMoviesBecomeTheResponsesOfTheContractInTheSameOrder() {
        var alien = new CatalogMovieDto(10L, "Alien", 1979, "Horror", "Ridley Scott", 4.5);
        var matrix = new CatalogMovieDto(11L, "Matrix", 1999, "Sci-fi", "Wachowski", null);

        assertEquals(new MarkedMovieResponse(10L, "Alien", 1979, "Horror", "Ridley Scott", 4.5),
                mapper.toResponse(alien));
        assertEquals(List.of(
                new MarkedMovieResponse(10L, "Alien", 1979, "Horror", "Ridley Scott", 4.5),
                new MarkedMovieResponse(11L, "Matrix", 1999, "Sci-fi", "Wachowski", null)),
                mapper.toResponses(List.of(alien, matrix)));
        assertEquals(List.of(), mapper.toResponses(List.of()));
    }
}
