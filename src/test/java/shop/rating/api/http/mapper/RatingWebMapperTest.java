package shop.rating.api.http.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.rating.api.http.dto.RatingRequest;
import shop.rating.api.http.dto.RatingResponse;
import shop.rating.internal.domain.dto.RateMovieDto;
import shop.rating.internal.domain.dto.RatingDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RatingWebMapperTest {

    private final RatingWebMapper mapper = Mappers.getMapper(RatingWebMapper.class);

    @Test
    void requestWithUserAndMovieBecomesTheDtoOfTheService() {
        assertEquals(new RateMovieDto(7L, 10L, 4, "Very good"),
                mapper.toDto(7L, 10L, new RatingRequest(4, "Very good")));
        assertEquals(new RateMovieDto(7L, 10L, 5, null), mapper.toDto(7L, 10L, new RatingRequest(5, null)));
    }

    @Test
    void ratingBecomesTheResponseOfTheContractWithoutTheUser() {
        assertEquals(new RatingResponse(5L, 10L, 4, "Very good"),
                mapper.toResponse(new RatingDto(5L, 7L, 10L, 4, "Very good")));
    }
}
