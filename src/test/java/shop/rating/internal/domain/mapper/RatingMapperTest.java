package shop.rating.internal.domain.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.rating.internal.domain.dto.RatingDto;
import shop.rating.internal.domain.model.Rating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RatingMapperTest {

    private final RatingMapper mapper = Mappers.getMapper(RatingMapper.class);

    @Test
    void dtoSurvivesTheRoundTripThroughTheModel() {
        var dto = new RatingDto(5L, 1L, 10L, 4, "Very good");

        Rating model = mapper.toModel(dto);

        assertEquals(5L, model.getId());
        assertEquals(1L, model.getUserId());
        assertEquals(10L, model.getMovieId());
        assertEquals(4, model.getScore());
        assertEquals("Very good", model.getComment());
        assertEquals(dto, mapper.toDto(model));
    }

    @Test
    void newRatingWithoutIdNorCommentKeepsThemNull() {
        var dto = new RatingDto(null, 1L, 10L, 3, null);

        Rating model = mapper.toModel(dto);

        assertNull(model.getId());
        assertNull(model.getComment());
        assertEquals(dto, mapper.toDto(model));
    }
}
