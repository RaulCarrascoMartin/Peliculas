package shop.catalog.internal.domain.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.catalog.internal.domain.dto.MovieDetailsDto;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.dto.ReservationDto;
import shop.catalog.internal.domain.model.Movie;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MovieMapperTest {

    private final MovieMapper mapper = Mappers.getMapper(MovieMapper.class);

    private static final MovieDto ALIEN =
            new MovieDto(10L, "Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), 4.5, 3);

    @Test
    void dtoSurvivesTheRoundTripThroughTheModel() {
        Movie model = mapper.toModel(ALIEN);

        assertEquals(10L, model.getId());
        assertEquals("Alien", model.getTitle());
        assertEquals(1979, model.getYear());
        assertEquals("Science fiction", model.getGenre());
        assertEquals("Ridley Scott", model.getDirector());
        assertEquals(new BigDecimal("12.99"), model.getPrice());
        assertEquals(4.5, model.getAverageRating());
        assertEquals(3, model.getStock());
        assertEquals(ALIEN, mapper.toDto(model));
    }

    @Test
    void optionalDataStaysNullInBothDirections() {
        var bare = new MovieDto(null, "Silent", null, null, null, null, null, 0);

        assertEquals(bare, mapper.toDto(mapper.toModel(bare)));
    }

    @Test
    void copyDetailsReplacesTheEditableDataAndLeavesIdAverageAndStock() {
        Movie model = mapper.toModel(ALIEN);

        mapper.copyDetails(
                new MovieDetailsDto("Aliens", 1986, "Action", "James Cameron", new BigDecimal("19.99"), 50), model);

        assertEquals(new MovieDto(10L, "Aliens", 1986, "Action", "James Cameron", new BigDecimal("19.99"), 4.5, 3),
                mapper.toDto(model));
    }

    @Test
    void copyDetailsOverwritesWithNullWhatIsNotGiven() {
        Movie model = mapper.toModel(ALIEN);

        mapper.copyDetails(new MovieDetailsDto("Alien", 1979, "Science fiction", "Ridley Scott", null, null), model);

        assertNull(model.getPrice());
        assertEquals(3, model.getStock());
    }

    @Test
    void reservationTakesTitleAndPriceFromTheMovieAndTheRequestedQuantity() {
        assertEquals(new ReservationDto(10L, "Alien", new BigDecimal("12.99"), 2),
                mapper.toReservation(mapper.toModel(ALIEN), 2));
    }
}
