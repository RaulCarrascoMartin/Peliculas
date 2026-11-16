package shop.rating.internal.domain.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.model.MovieMark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MovieMarkMapperTest {

    private final MovieMarkMapper mapper = Mappers.getMapper(MovieMarkMapper.class);

    @Test
    void dtoSurvivesTheRoundTripThroughTheModel() {
        var dto = new MovieMarkDto(5L, 1L, 10L, MarkType.VISTA);

        MovieMark model = mapper.toModel(dto);

        assertEquals(5L, model.getId());
        assertEquals(1L, model.getUserId());
        assertEquals(10L, model.getMovieId());
        assertEquals(MarkType.VISTA, model.getType());
        assertEquals(dto, mapper.toDto(model));
    }

    @Test
    void newMarkWithoutIdKeepsItNull() {
        var dto = new MovieMarkDto(null, 1L, 10L, MarkType.FAVORITA);

        assertNull(mapper.toModel(dto).getId());
        assertEquals(dto, mapper.toDto(mapper.toModel(dto)));
    }
}
